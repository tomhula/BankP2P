package cz.tomashula.bankp2p.proxy

import cz.tomashula.bankp2p.client.BankClient
import cz.tomashula.bankp2p.command.BankCodeCmd
import kotlin.time.Duration

class BankFinder(
    var portRange: IntRange,
    var tcpTimout: Duration,
    var responseTimout: Duration
)
{
    private val cache: MutableMap<String, Int> = mutableMapOf()

    /**
     * Finds a running bank on the provided [bankCode] (address) and returns a connected [BankClient] instance, or `null` if no bank was found.
     */
    suspend fun findFirstBank(bankCode: String): BankClient?
    {
        val cachedPort = cache[bankCode]

        if (cachedPort != null)
        {
            val port = cache[bankCode]!!
            val bank = tryBank(bankCode, port)
            if (bank != null)
                return bank
        }

        val portsToScan = if (cachedPort != null) portRange - cachedPort else portRange

        for (port in portsToScan)
        {
            val bank = tryBank(bankCode, port)
            if (bank != null)
                return bank
        }

        return null
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
