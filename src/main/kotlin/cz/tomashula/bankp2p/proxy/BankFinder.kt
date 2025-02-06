package cz.tomashula.bankp2p.proxy

import cz.tomashula.bankp2p.client.BankClient
import cz.tomashula.bankp2p.command.BankCodeCmd
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.net.InetAddress
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

class BankFinder(
    var portRange: IntRange,
    var tcpTimout: Duration,
    var responseTimout: Duration,
    var scanNetwork: InetAddress,
    var scanSubnetMask: Int,
    val threadPoolSize: Int
)
{
    private val coroutineContext = newFixedThreadPoolContext(threadPoolSize, "$scanNetwork/$scanSubnetMask scan thread")
    private val cache: MutableMap<String, Int> = mutableMapOf()

    /**
     * Finds a running bank on the provided [bankCode] (address) and returns a connected [BankClient] instance, or `null` if no bank was found.
     */
    // OPTIMIZE: Try all ports at once, and once one succeeds, cancel the rest.
    suspend fun findFirstBank(bankCode: String): BankClient?
    {
        val cachedPort = cache[bankCode]

        val portsToScanOrdered = if (cachedPort != null)
            listOf(cachedPort) + portRange.toList() - cachedPort // Just makes the cached port first in the list else
        else
            portRange.toList()

        for (port in portsToScanOrdered)
        {
            val bank = tryBank(bankCode, port)
            if (bank != null)
                return bank
        }

        return null
    }

    suspend fun findAllBanks(exclude: InetAddress, operation: suspend CoroutineScope.(BankClient) -> Unit): Int
    {
        val ipsToScan = generateIpsInNetwork(scanNetwork, scanSubnetMask).filter { it != exclude }

        val count = withContext(coroutineContext) {
            val jobs = ipsToScan.map { ip ->
                async {
                    logger.debug { "Scanning $ip" }
                    val bankClient = findFirstBank(ip.hostAddress)
                    if (bankClient != null)
                    {
                        operation(bankClient)
                        bankClient.close()
                        true
                    }
                    else false
                }
            }
            jobs.awaitAll().count { it == true }
        }

        return count
    }

    // ChatGPT prompt: https://chatgpt.com/share/67a0fc02-91bc-800e-8476-a225fed1ee3b
    private fun generateIpsInNetwork(address: InetAddress, subnetMask: Int): List<InetAddress>
    {
        val addresses = mutableListOf<InetAddress>()
        val baseAddress = address.address
        val mask = (0xFFFFFFFF shl (32 - subnetMask)).toInt()
        val addrInt = baseAddress.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
        val networkAddr = addrInt and mask
        val broadcastAddr = networkAddr or (mask.inv() and 0xFFFFFFFF.toInt())

        for (i in (networkAddr + 1) until broadcastAddr)
        {
            val ipBytes = ByteArray(4)
            for (j in 3 downTo 0)
                ipBytes[j] = (i shr (8 * (3 - j)) and 0xFF).toByte()
            addresses.add(InetAddress.getByAddress(ipBytes))
        }
        return addresses
    }

    private suspend fun tryBank(bankCode: String, port: Int): BankClient?
    {
        val bankClient = BankClient(bankCode, port, tcpTimout, responseTimout)

        try
        {
            bankClient.connect()
            val response = bankClient.request(BankCodeCmd.build())
            if (response != bankCode)
            {
                bankClient.close()
                return null
            }

            cache[bankCode] = port
            return bankClient
        }
        catch (e: Exception)
        {
            bankClient.close()
            return null
        }
    }
}
