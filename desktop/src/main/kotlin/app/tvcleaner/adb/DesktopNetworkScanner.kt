package app.tvcleaner.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/** Ищет в локальной сети устройства с открытым ADB-портом. Предполагает маску /24. */
object NetworkScanner {

    /** Все домашние подсети компьютера (на Windows бывают виртуальные адаптеры, поэтому их может быть несколько). */
    fun subnetPrefixes(): List<String> {
        val result = mutableListOf<String>()
        val ifaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        for (ni in ifaces) {
            if (!ni.isUp || ni.isLoopback || ni.isVirtual) continue
            for (addr in ni.inetAddresses) {
                if (addr is Inet4Address && addr.isSiteLocalAddress) {
                    result += addr.hostAddress.substringBeforeLast('.')
                }
            }
        }
        return result.distinct().take(3)
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
