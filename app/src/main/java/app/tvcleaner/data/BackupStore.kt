package app.tvcleaner.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** kind: "removed" — удалено для пользователя 0, "disabled" — отключено */
@Serializable
data class BackupAction(
    val pkg: String,
    val title: String,
    val kind: String,
    /** Имена вытянутых с ТВ APK (только для не-системных приложений) */
    val apkFiles: List<String> = emptyList(),
)

@Serializable
data class BackupRecord(
    val id: String,
    val createdAt: Long,
    val device: String,
    val actions: List<BackupAction> = emptyList(),
    /** Прежние значения настроек (settings global). null = ключа не было, при откате удаляем */
    val settings: Map<String, String?> = emptyMap(),
    val restored: Boolean = false,
)

class BackupStore(ctx: Context) {
    private val dir = File(ctx.filesDir, "backups").apply { mkdirs() }
    val apkDir = File(dir, "apks").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    private fun file(id: String) = File(dir, "$id.json")

    fun list(): List<BackupRecord> =
        dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .mapNotNull { runCatching { json.decodeFromString<BackupRecord>(it.readText()) }.getOrNull() }
            .sortedByDescending { it.createdAt }

    fun get(id: String): BackupRecord? = list().firstOrNull { it.id == id }

    fun save(r: BackupRecord) {
        file(r.id).writeText(json.encodeToString(BackupRecord.serializer(), r))
    }

    fun delete(id: String) {
        file(id).delete()
        File(apkDir, id).deleteRecursively()
    }

    fun toJson(r: BackupRecord): String = json.encodeToString(BackupRecord.serializer(), r)

    fun fromJson(text: String): BackupRecord = json.decodeFromString(BackupRecord.serializer(), text)

    fun apkFolder(id: String, pkg: String): File = File(apkDir, "$id/$pkg").apply { mkdirs() }
}
