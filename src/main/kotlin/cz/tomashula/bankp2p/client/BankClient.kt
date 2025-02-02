package cz.tomashula.bankp2p.client

import cz.tomashula.bankp2p.Account
import cz.tomashula.bankp2p.command.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.time.Duration

class BankClient(
    val host: String,
    val port: Int,
    val tcpTimeout: Duration,
    val bankResponseTimeout: Duration
) : AutoCloseable
{
    private lateinit var clientSocket: Socket
    private lateinit var printWriter: PrintWriter
    private lateinit var bufferedReader: BufferedReader

    suspend fun connect()
    {
        withContext(Dispatchers.IO) {
            clientSocket = Socket()
            clientSocket.connect(InetSocketAddress(host, port), tcpTimeout.inWholeMilliseconds.toInt())
            clientSocket.soTimeout = bankResponseTimeout.inWholeMilliseconds.toInt()
            printWriter = PrintWriter(clientSocket.getOutputStream(), true)
            bufferedReader = clientSocket.getInputStream().bufferedReader()
        }
    }

    suspend fun request(command: String): String? = withContext(Dispatchers.IO) {
        printWriter.println(command)
        val response = try
        {
            bufferedReader.readLine()
        }
        catch (e: SocketTimeoutException)
        {
            throw DownstreamBankResponseTimeoutException(host, port, bankResponseTimeout)
        }

        val responseCodeMessage = response.trim().split(Regex("\\s+"), limit = 2)

        if (responseCodeMessage.size != 2)
            throw DownstreamBankProtocolError(host, port, command, response)

        val (responseCode, responseMessage) = responseCodeMessage

        if (responseCode.uppercase() == "ER")
            throw DownstreamBankError(host, port, command, responseMessage)

        responseMessage.ifBlank { null }
    }

    suspend fun bankCode() = request(BankCodeCmd.build())

    suspend fun accountDeposit(account: Account, amount: Long) = request(AccountDepositCmd.build(account, amount))

    suspend fun accountWithdraw(account: Account, amount: Long) = request(AccountWithdrawCmd.build(account, amount))

    suspend fun accountBalance(account: Account) = request(AccountBalanceCmd.build(account))

    suspend fun accountRemove(account: Account) = request(AccountRemoveCmd.build(account))

    suspend fun bankTotalBalance() = request(BankTotalBalanceCmd.build())

    suspend fun bankNumberOfClients() = request(BankNumberOfClientsCmd.build())

    override fun close()
    {
        clientSocket.close()
    }
}
