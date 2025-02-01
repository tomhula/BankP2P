package cz.tomashula.bankp2p.proxy

import cz.tomashula.bankp2p.client.BankClient
import cz.tomashula.bankp2p.command.BankCodeCmd
import kotlin.time.Duration

class BankFinder
{
    private val cache: MutableMap<String, Int> = mutableMapOf()

    /**
     * Finds a running bank on the provided [bankCode] (address) and returns a connected [BankClient] instance, or `null` if no bank was found.
     */
    suspend fun findFirstBank(bankCode: String, portRange: IntRange, tcpTimout: Duration, responseTimout: Duration): BankClient?
    {
        for (port in portRange)
        {
            val client = BankClient(bankCode, port, tcpTimout, responseTimout)

            try
            {
                client.connect()
                val response = client.request(BankCodeCmd.build())
                if (response != bankCode)
                    continue

                cache[bankCode] = port
                return client
            }
            catch (e: Exception)
            {
                continue
            }
        }

        return null
    }
}
