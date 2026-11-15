package app.tvcleaner.adb

import app.tvcleaner.data.AppItem
import app.tvcleaner.data.Catalog
import app.tvcleaner.data.CatalogEntry
import app.tvcleaner.data.DeviceInfo
import app.tvcleaner.data.LauncherInfo
import app.tvcleaner.data.ScanResult
import app.tvcleaner.data.Tier
import java.io.File

/** Высокоуровневые операции над ТВ: скан, чистка, лаунчеры, настройки. */
class TvOps(private val adb: AdbManager, private val catalog: Catalog) {

    // ---------- информация об устройстве ----------

    suspend fun deviceInfo(): DeviceInfo {
        val props = adb.shell(
            "getprop ro.product.manufacturer; getprop ro.product.model; " +
                "getprop ro.build.version.release; getprop ro.build.version.sdk"
        ).out.lines().map { it.trim() }
        val name = listOfNotNull(props.getOrNull(0), props.getOrNull(1))
            .filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Android TV" }

        val df = adb.shell("df -k /data").out.lines().map { it.trim() }.lastOrNull { it.isNotEmpty() }
        val freeStorageMb = df?.split(Regex("\\s+"))?.let { t ->
            if (t.size >= 4) t[t.size - 3].toLongOrNull()?.div(1024) else null
        }

        val freeRamMb = Regex("MemAvailable:\\s+(\\d+)")
            .find(adb.shell("cat /proc/meminfo").out)?.groupValues?.get(1)?.toLongOrNull()?.div(1024)

        return DeviceInfo(
            name = name,
            androidRelease = props.getOrNull(2).orEmpty(),
            sdk = props.getOrNull(3)?.toIntOrNull() ?: 0,
            freeStorageMb = freeStorageMb,
            freeRamMb = freeRamMb,
        )
    }

    // ---------- скан ----------

    private fun parsePkgs(out: String): Set<String> =
        out.lineSequence().map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }
            .toSet()

    private fun isNeverTouch(pkg: String) =
        catalog.neverTouch.any { rule -> if (rule.endsWith(".")) pkg.startsWith(rule) else pkg == rule }

    private fun matchCatalog(pkg: String): CatalogEntry? =
        catalog.entries.firstOrNull { e -> if (e.prefix) pkg.startsWith(e.pkg) else e.pkg == pkg }

    private suspend fun readCacheSizes(): Map<String, Long> {
        val d = adb.shell("dumpsys diskstats").out
        fun list(label: String): List<String> =
            Regex("(?m)^$label: \\[(.*?)]").find(d)?.groupValues?.get(1)
                ?.split(",")?.map { it.trim().trim('"') }.orEmpty()
        val names = list("Package Names")
        val caches = list("Cache Sizes")
        if (names.isEmpty() || names.size != caches.size) return emptyMap()
        return names.zip(caches).mapNotNull { (n, c) -> c.toLongOrNull()?.let { n to it } }.toMap()
    }

    suspend fun scan(): ScanResult {
        val system = parsePkgs(adb.shell("pm list packages -s").out)
        val third = parsePkgs(adb.shell("pm list packages -3").out)
        val disabled = parsePkgs(adb.shell("pm list packages -d").out)
        val caches = readCacheSizes()
        val currentHome = defaultLauncher()?.substringBefore('/')

        val junk = mutableListOf<AppItem>()
        val optional = mutableListOf<AppItem>()
        val user = mutableListOf<AppItem>()

        for (pkg in (system + third).sorted()) {
            if (isNeverTouch(pkg) || pkg == currentHome) continue
            val isSystem = pkg in system
            val entry = matchCatalog(pkg)
            val cache = caches[pkg]
            val base = AppItem(
                pkg = pkg,
                title = entry?.title ?: pkg,
                tier = entry?.tier,
                note = entry?.note.orEmpty(),
                isSystem = isSystem,
                isDisabled = pkg in disabled,
                cacheBytes = cache,
            )
            when {
                entry?.tier == Tier.JUNK -> junk += base
                entry?.tier == Tier.OPTIONAL -> optional += base
                isSystem && catalog.keywords.any { pkg.lowercase().contains(it) } ->
                    optional += base.copy(
                        tier = Tier.OPTIONAL,
                        note = "Похоже на рекламу или аналитику. Проверь название перед отключением.",
                    )
                !isSystem -> user += base
            }
        }
        return ScanResult(
            junk = junk,
            optional = optional,
            userApps = user.sortedByDescending { it.cacheBytes ?: 0L },
            totalCacheBytes = caches.values.takeIf { it.isNotEmpty() }?.sum(),
        )
    }

    // ---------- чистка ----------

    suspend fun trimCaches(): ShellResult = adb.shell("pm trim-caches 999G")

    suspend fun killBackground(): ShellResult = adb.shell("am kill-all")

    /** Удаление «для пользователя 0» с сохранением данных. Обратимо через install-existing. */
    suspend fun remove(pkg: String): Boolean = adb.shell("pm uninstall -k --user 0 $pkg").text.contains("Success")

    suspend fun disable(pkg: String): Boolean {
        val r = adb.shell("pm disable-user --user 0 $pkg").text
        return r.contains("disabled") && !r.contains("Exception") && !r.contains("Error")
    }

    suspend fun enable(pkg: String): Boolean {
        val r = adb.shell("pm enable $pkg").text
        return r.contains("enabled") && !r.contains("Exception")
    }

    suspend fun restoreRemoved(pkg: String): Boolean {
        val r = adb.shell("cmd package install-existing --user 0 $pkg").text
        return r.contains("installed for user") || r.contains("Success")
    }

    /** Вытягивает APK приложения на телефон. Возвращает имена файлов или пустой список при неудаче. */
    suspend fun pullApks(pkg: String, into: File): List<String> {
        val paths = adb.shell("pm path $pkg").out.lineSequence()
            .map { it.trim() }.filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }.toList()
        if (paths.isEmpty()) return emptyList()
        val names = mutableListOf<String>()
        for (p in paths) {
            val name = p.substringAfterLast('/')
            adb.pull(p, File(into, name))
            names += name
        }
        return names
    }

    // ---------- настройки (анимации и т.п.) ----------

    suspend fun readSetting(key: String): String? {
        val v = adb.shell("settings get global $key").out.trim()
        return if (v.isEmpty() || v == "null") null else v
    }

    suspend fun putSetting(key: String, value: String?) {
        if (value == null) adb.shell("settings delete global $key")
        else adb.shell("settings put global $key $value")
    }

    // ---------- лаунчеры ----------

    suspend fun defaultLauncher(): String? {
        val r = adb.shell("cmd shortcut get-default-launcher").out
        Regex("\\{([^/}]+/[^}]+)}").find(r)?.groupValues?.get(1)?.let { return it }
        val r2 = adb.shell(
            "pm resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
        ).out
        return Regex("([A-Za-z0-9_.]+/[A-Za-z0-9_.$]+)").findAll(r2).lastOrNull()?.value
    }

    suspend fun listLaunchers(): List<LauncherInfo> {
        val query = "-a android.intent.action.MAIN -c android.intent.category.HOME"
        var r = adb.shell("cmd package query-activities --brief $query")
        if (r.out.isBlank()) r = adb.shell("pm query-activities --brief $query")
        val def = defaultLauncher()?.substringBefore('/')
        return Regex("([A-Za-z0-9_.]+)/([A-Za-z0-9_.$]+)").findAll(r.out)
            .map { LauncherInfo(pkg = it.groupValues[1], component = it.value, isDefault = it.groupValues[1] == def) }
            .distinctBy { it.component }
            .toList()
    }

    suspend fun setLauncher(component: String): ShellResult {
        val r = adb.shell("cmd package set-home-activity --user 0 $component")
        return if (r.ok && !r.text.contains("Exception")) r
        else adb.shell("pm set-home-activity $component")
    }
}
