package app.tvcleaner.adb

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket

/** Ищет в локальной сети устройства с открытым ADB-портом (по умолчанию 5555). Предполагает маску /24. */
object NetworkScanner {

    fun subnetPrefix(ctx: Context): String? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return null
        val net = cm.activeNetwork ?: return null
        val lp = cm.getLinkProperties(net) ?: return null
        val ip = lp.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress ?: return null
        return ip.substringBeforeLast('.')
    }

    suspend fun scan(prefix: String, port: Int = 5555): List<String> = coroutineScope {
        val gate = Semaphore(64)
        (1..254).map { i ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    val host = "$prefix.$i"
                    try {
                        Socket().use { s ->
                            s.connect(InetSocketAddress(host, port), 500)
                            host
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }
}
