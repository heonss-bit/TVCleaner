package app.tvcleaner.data

import kotlinx.serialization.Serializable

/** JUNK = полный шлак, безопасно убирать. OPTIONAL = можно отключить, но что-то может перестать работать. */
@Serializable
enum class Tier { JUNK, OPTIONAL }

@Serializable
data class CatalogEntry(
    val pkg: String,
    val title: String,
    val tier: Tier,
    val note: String,
    /** true — pkg это префикс (com.vendor.), а не точное имя пакета */
    val prefix: Boolean = false,
)

@Serializable
data class Catalog(
    val version: Int,
    /** Никогда не трогаем и не показываем. Если строка заканчивается на '.', это префикс. */
    val neverTouch: List<String>,
    /** Подстроки в имени системного пакета, по которым подозреваем рекламу/телеметрию */
    val keywords: List<String>,
    val entries: List<CatalogEntry>,
)

data class AppItem(
    val pkg: String,
    val title: String,
    val tier: Tier?,
    val note: String,
    val isSystem: Boolean,
    val isDisabled: Boolean,
    val cacheBytes: Long?,
)

data class DeviceInfo(
    val name: String,
    val androidRelease: String,
    val sdk: Int,
    val freeStorageMb: Long?,
    val freeRamMb: Long?,
)

data class ScanResult(
    val junk: List<AppItem>,
    val optional: List<AppItem>,
    val userApps: List<AppItem>,
    val totalCacheBytes: Long?,
)

data class LauncherInfo(
    val pkg: String,
    val component: String,
    val isDefault: Boolean,
)
