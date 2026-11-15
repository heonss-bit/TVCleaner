package app.tvcleaner.adb

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ShellResult(val out: String, val err: String, val code: Int) {
    val ok: Boolean get() = code == 0
    val text: String get() = (out + "\n" + err).trim()
}

/** Тонкая обёртка над dadb: один активный коннект к ТВ. */
class AdbManager(private val ctx: Context) {

    @Volatile
    private var dadb: Dadb? = null

    val isConnected: Boolean get() = dadb != null

    private val keyPair: AdbKeyPair by lazy {
        val dir = File(ctx.filesDir, "adbkey").apply { mkdirs() }
        val priv = File(dir, "adbkey")
        val pub = File(dir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) AdbKeyPair.generate(priv, pub)
        AdbKeyPair.read(priv, pub)
    }

    /**
     * Подключение. При первом запуске ТВ покажет окно «Разрешить отладку с этого компьютера?»
     * — shell-команда ниже блокируется, пока пользователь не подтвердит.
     */
    suspend fun connect(host: String, port: Int = 5555): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            close()
            val d = Dadb.create(host, port, keyPair)
            val r = d.shell("echo ok")
            check(r.exitCode == 0 && r.output.contains("ok")) { "ТВ не ответил на проверочную команду" }
            dadb = d
        }
    }

    suspend fun shell(cmd: String): ShellResult = withContext(Dispatchers.IO) {
        val d = dadb ?: throw IllegalStateException("Нет подключения к ТВ")
        val r = d.shell(cmd)
        ShellResult(r.output, r.errorOutput, r.exitCode)
    }

    suspend fun pull(remotePath: String, local: File) = withContext(Dispatchers.IO) {
        val d = dadb ?: throw IllegalStateException("Нет подключения к ТВ")
        local.parentFile?.mkdirs()
        d.pull(local, remotePath)
    }

    suspend fun install(apks: List<File>) = withContext(Dispatchers.IO) {
        val d = dadb ?: throw IllegalStateException("Нет подключения к ТВ")
        if (apks.size == 1) d.install(apks.first()) else d.installMultiple(apks)
    }

    fun close() {
        runCatching { dadb?.close() }
        dadb = null
    }
}
