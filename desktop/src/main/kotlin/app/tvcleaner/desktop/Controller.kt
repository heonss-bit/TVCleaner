package app.tvcleaner.desktop

import app.tvcleaner.adb.AdbManager
import app.tvcleaner.adb.NetworkScanner
import app.tvcleaner.adb.TvOps
import app.tvcleaner.data.BackupAction
import app.tvcleaner.data.BackupRecord
import app.tvcleaner.data.BackupStore
import app.tvcleaner.data.Catalog
import app.tvcleaner.data.DeviceInfo
import app.tvcleaner.data.LauncherInfo
import app.tvcleaner.data.ScanResult
import app.tvcleaner.data.Tier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

data class UiState(
    val host: String = "",
    val found: List<String> = emptyList(),
    val connected: Boolean = false,
    val device: DeviceInfo? = null,
    val busy: String? = null,
    val message: String? = null,

    val scan: ScanResult? = null,
    val selected: Set<String> = emptySet(),
    val removeOptional: Boolean = false,
    val report: String? = null,

    val launchers: List<LauncherInfo> = emptyList(),
    val prevLauncher: String? = null,

    val backups: List<BackupRecord> = emptyList(),
)

/** То же, что MainViewModel на телефоне, только без Android. */
class Controller {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val home = File(System.getProperty("user.home"), ".tvcleaner").apply { mkdirs() }
    private val adb = AdbManager(File(home, "adbkey"))
    private val store = BackupStore(File(home, "backups"))

    private val settingsFile = File(home, "settings.properties")
    private val props = Properties().apply {
        if (settingsFile.exists()) runCatching { settingsFile.inputStream().use { load(it) } }
    }

    private fun saveProps() {
        runCatching { settingsFile.outputStream().use { props.store(it, "TV Cleaner") } }
    }

    private val catalog: Catalog = Json { ignoreUnknownKeys = true }.decodeFromString(
        (Controller::class.java.getResourceAsStream("/catalog.json")
            ?: throw IllegalStateException("catalog.json не найден"))
            .bufferedReader().use { it.readText() }
    )
    private val ops = TvOps(adb, catalog)

    private val _ui = MutableStateFlow(
        UiState(
            host = props.getProperty("host", ""),
            backups = store.list(),
            prevLauncher = props.getProperty("prevHome"),
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    val backupFolder: File get() = store.dir

    private fun toast(msg: String) = _ui.update { it.copy(message = msg) }

    fun messageShown() = _ui.update { it.copy(message = null) }

    private fun work(label: String, block: suspend () -> Unit) {
        if (_ui.value.busy != null) return
        scope.launch {
            _ui.update { it.copy(busy = label) }
            try {
                block()
            } catch (e: Exception) {
                toast("Ошибка: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                _ui.update { it.copy(busy = null) }
            }
        }
    }

    private fun refreshBackups() = _ui.update { it.copy(backups = store.list()) }

    fun shutdown() = adb.close()

    // ---------- подключение ----------

    fun setHost(v: String) = _ui.update { it.copy(host = v) }

    fun findTv() = work("Ищу ТВ в сети…") {
        val prefixes = NetworkScanner.subnetPrefixes()
        if (prefixes.isEmpty()) {
            toast("Нет подключения к сети. Подключи компьютер к той же Wi-Fi или кабелю, что и ТВ.")
            return@work
        }
        val found = prefixes.flatMap { NetworkScanner.scan(it) }.distinct()
        _ui.update { s ->
            s.copy(found = found, host = if (found.size == 1 && s.host.isBlank()) found.first() else s.host)
        }
        if (found.isEmpty()) {
            toast("Не нашёл. Включи на ТВ «Отладку по сети» и проверь, что ТВ и компьютер в одной сети.")
        }
    }

    fun connect() = work("Подключаюсь… подтверди доступ на экране ТВ") {
        val raw = _ui.value.host.trim()
        if (raw.isEmpty()) {
            toast("Введи IP-адрес ТВ")
            return@work
        }
        val host = raw.substringBefore(':')
        val port = raw.substringAfter(':', "5555").toIntOrNull() ?: 5555
        val r = adb.connect(host, port)
        if (r.isFailure) {
            toast("Не удалось подключиться: ${r.exceptionOrNull()?.message}")
            return@work
        }
        props.setProperty("host", raw)
        saveProps()
        val info = ops.deviceInfo()
        _ui.update { it.copy(connected = true, device = info, scan = null, selected = emptySet(), report = null) }
        loadLaunchers()
    }

    fun disconnect() {
        adb.close()
        _ui.update { it.copy(connected = false, device = null, scan = null, selected = emptySet(), launchers = emptyList()) }
    }

    // ---------- скан и чистка ----------

    fun scanTv() = work("Сканирую ТВ…") {
        val res = ops.scan()
        val info = ops.deviceInfo()
        _ui.update { s ->
            s.copy(
                scan = res,
                device = info,
                selected = res.junk.filter { !it.isDisabled }.map { it.pkg }.toSet(),
                report = null,
            )
        }
    }

    fun toggle(pkg: String) = _ui.update { s ->
        s.copy(selected = if (pkg in s.selected) s.selected - pkg else s.selected + pkg)
    }

    fun setRemoveOptional(v: Boolean) = _ui.update { it.copy(removeOptional = v) }

    fun trimCaches() = work("Чищу кэш…") {
        val before = ops.deviceInfo().freeStorageMb
        ops.trimCaches()
        val info = ops.deviceInfo()
        val gained = if (before != null && info.freeStorageMb != null) info.freeStorageMb - before else null
        _ui.update {
            it.copy(
                device = info,
                report = "Кэш очищен." + (gained?.let { g -> " Освободилось ≈ $g МБ." } ?: ""),
            )
        }
    }

    fun freeRam() = work("Освобождаю память…") {
        val before = ops.deviceInfo().freeRamMb
        ops.killBackground()
        val info = ops.deviceInfo()
        _ui.update {
            it.copy(
                device = info,
                report = "Фоновые процессы остановлены. Свободно RAM: ${before ?: "?"} → ${info.freeRamMb ?: "?"} МБ.",
            )
        }
    }

    fun setAnimations(scale: String) = work("Меняю анимации…") {
        val keys = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
        val old = keys.associateWith { ops.readSetting(it) }
        val id = "tweak_" + System.currentTimeMillis()
        store.save(
            BackupRecord(
                id = id,
                createdAt = System.currentTimeMillis(),
                device = _ui.value.device?.name.orEmpty(),
                settings = old,
            )
        )
        keys.forEach { ops.putSetting(it, scale) }
        refreshBackups()
        _ui.update { it.copy(report = "Анимации: ×$scale. Прежние значения сохранены в бэкапах.") }
    }

    fun runClean() = work("Делаю бэкап и чищу…") {
        val st = _ui.value
        val scan = st.scan ?: return@work
        val chosen = (scan.junk + scan.optional + scan.userApps).filter { it.pkg in st.selected }
        if (chosen.isEmpty()) {
            toast("Ничего не выбрано")
            return@work
        }
        val before = ops.deviceInfo()
        val id = "clean_" + System.currentTimeMillis()
        val actions = mutableListOf<BackupAction>()
        fun persist() {
            store.save(
                BackupRecord(
                    id = id,
                    createdAt = System.currentTimeMillis(),
                    device = before.name,
                    actions = actions.toList(),
                )
            )
        }

        var removed = 0
        var disabled = 0
        val failed = mutableListOf<String>()

        for (app in chosen) {
            _ui.update { it.copy(busy = "Обрабатываю: ${app.title}") }
            val doRemove = app.tier != Tier.OPTIONAL || st.removeOptional
            if (doRemove) {
                var apks = emptyList<String>()
                if (!app.isSystem) {
                    apks = runCatching { ops.pullApks(app.pkg, store.apkFolder(id, app.pkg)) }.getOrDefault(emptyList())
                    if (apks.isEmpty()) {
                        failed += "${app.title} (не удалось сохранить APK)"
                        continue
                    }
                }
                if (ops.remove(app.pkg)) {
                    actions += BackupAction(app.pkg, app.title, "removed", apks)
                    removed++
                    persist()
                } else failed += app.title
            } else {
                if (ops.disable(app.pkg)) {
                    actions += BackupAction(app.pkg, app.title, "disabled")
                    disabled++
                    persist()
                } else failed += app.title
            }
        }

        ops.trimCaches()
        val after = ops.deviceInfo()
        val fresh = ops.scan()
        refreshBackups()

        val freed = if (before.freeStorageMb != null && after.freeStorageMb != null)
            "\nСвободно места: ${before.freeStorageMb} → ${after.freeStorageMb} МБ." else ""
        _ui.update {
            it.copy(
                scan = fresh,
                device = after,
                selected = emptySet(),
                report = "Удалено: $removed, отключено: $disabled." + freed +
                    (if (failed.isNotEmpty()) "\nНе вышло: ${failed.joinToString()}." else "") +
                    "\nЕсли что-то сломалось — вкладка «Бэкапы» → «Восстановить». Перезагрузи ТВ и проверь.",
            )
        }
    }

    // ---------- лаунчер ----------

    private suspend fun loadLaunchers() {
        val l = ops.listLaunchers()
        _ui.update { it.copy(launchers = l, prevLauncher = props.getProperty("prevHome")) }
    }

    fun refreshLaunchers() = work("Читаю список лаунчеров…") { loadLaunchers() }

    fun setLauncher(l: LauncherInfo) = work("Назначаю лаунчер…") {
        val cur = ops.defaultLauncher()
        if (props.getProperty("prevHome") == null && cur != null && cur != l.component) {
            props.setProperty("prevHome", cur)
            saveProps()
        }
        val r = ops.setLauncher(l.component)
        toast(
            if (r.ok && !r.text.contains("Exception")) "Готово. Нажми «Домой» на пульте ТВ."
            else "Не получилось: ${r.text.take(160)}"
        )
        loadLaunchers()
    }

    fun restorePrevLauncher() = work("Возвращаю прежний лаунчер…") {
        val prev = props.getProperty("prevHome") ?: return@work
        val r = ops.setLauncher(prev)
        if (r.ok && !r.text.contains("Exception")) {
            props.remove("prevHome")
            saveProps()
        }
        toast(if (r.ok) "Прежний лаунчер возвращён." else "Не получилось: ${r.text.take(160)}")
        loadLaunchers()
    }

    fun installLauncher(apk: File) = work("Ставлю APK на ТВ…") {
        adb.install(listOf(apk))
        loadLaunchers()
        toast("APK установлен. Теперь выбери его в списке и нажми «Сделать основным».")
    }

    // ---------- бэкапы ----------

    fun restore(id: String) = work("Восстанавливаю…") {
        val rec = store.get(id) ?: return@work
        var ok = 0
        val failed = mutableListOf<String>()
        for (a in rec.actions) {
            _ui.update { it.copy(busy = "Восстанавливаю: ${a.title}") }
            val done = when (a.kind) {
                "disabled" -> ops.enable(a.pkg)
                else -> {
                    if (a.apkFiles.isNotEmpty()) {
                        val dir = store.apkFolder(rec.id, a.pkg)
                        runCatching { adb.install(a.apkFiles.map { File(dir, it) }) }.isSuccess
                    } else ops.restoreRemoved(a.pkg)
                }
            }
            if (done) ok++ else failed += a.title
        }
        rec.settings.forEach { (k, v) -> ops.putSetting(k, v) }
        store.save(rec.copy(restored = true))
        refreshBackups()
        toast("Восстановлено: $ok" + (if (failed.isNotEmpty()) ". Не вышло: ${failed.joinToString()}" else ""))
    }

    fun deleteBackup(id: String) {
        store.delete(id)
        refreshBackups()
    }

    fun exportBackup(id: String, target: File) = work("Сохраняю бэкап…") {
        val rec = store.get(id) ?: return@work
        target.writeText(store.toJson(rec))
        toast("Бэкап сохранён (без APK-файлов, только список действий).")
    }

    fun importBackup(source: File) = work("Читаю бэкап…") {
        val rec = store.fromJson(source.readText())
        val id = if (store.get(rec.id) == null) rec.id else rec.id + "_imp" + System.currentTimeMillis()
        store.save(rec.copy(id = id, restored = false))
        refreshBackups()
        toast("Бэкап импортирован.")
    }
}
