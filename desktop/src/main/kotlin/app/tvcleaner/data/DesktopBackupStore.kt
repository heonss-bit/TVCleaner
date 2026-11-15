package app.tvcleaner.data

import kotlinx.serialization.json.Json
import java.io.File

/** Бэкапы на ПК лежат в папке пользователя: ~/.tvcleaner/backups */
class BackupStore(val dir: File) {
    init { dir.mkdirs() }

    private val apkDir = File(dir, "apks").apply { mkdirs() }
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
