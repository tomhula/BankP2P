package cz.tomashula.bankp2p.server

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

// TODO: Rewrite to non-blocking using Java NIO (ServerSocketChannel or AsynchronousServerSocketChannel)
class TelnetServer(
    private val host: String,
    private val port: Int,
    private val clientInactivityTimeout: Duration,
    private val clientsThreadPoolSize: Int,
    private val onInput: suspend (ClientSession, String) -> String?,
)
{
    @OptIn(DelicateCoroutinesApi::class)
    private val coroutineScope = CoroutineScope(
        newFixedThreadPoolContext(
            clientsThreadPoolSize,
            "TelnetServer clients thread"
        ) + SupervisorJob() + CoroutineName("TelnetServer clients")
    )

    @Volatile
    private var running: Boolean = false
    private lateinit var serverSocket: ServerSocket
    private val sessions: MutableMap<Socket, ClientSession> = ConcurrentHashMap()

    init
    {
        require(port in 1..65535) { "BindPort must be between 1 and 65535" }
        require(host.isNotEmpty()) { "BindAddress must not be empty" }
    }

    suspend fun start()
    {
        withContext(CoroutineName("TelnetServer") + Dispatchers.IO) {
            serverSocket = ServerSocket()
            serverSocket.bind(InetSocketAddress(host, port))
            running = true
            logger.info { "Server started on $host:$port" }

            while (running)
            {
                val clientSocket = serverSocket.accept()
                handleClient(clientSocket)
            }
        }
    }

    fun stop()
    {
        running = false
        sessions.forEach { (clientSocket, clientSession) ->
            logger.info { "Closing client session: $clientSession" }
            clientSocket.close()
        }
        sessions.clear()
        serverSocket.close()
    }

    private fun handleClient(clientSocket: Socket)
    {
        val inetSocketAddress = clientSocket.inetAddress
        val clientSession = ClientSession(inetSocketAddress.hostAddress, clientSocket.port)
        sessions[clientSocket] = clientSession
        logger.info { "Client connected: $clientSession" }

        coroutineScope.launch(CoroutineName("Client: $clientSession")) {
            clientSocket.soTimeout = clientInactivityTimeout.inWholeMilliseconds.toInt()
            clientSocket.use { socket ->
                val reader = socket.getInputStream().bufferedReader()
                val writer = socket.getOutputStream().writer() // No need for BufferedWriter, because responses are sent out immediately

                while (!socket.isClosed && running)
                {
                    val input = try
                    {
                        reader.readLine() ?: break
                    }
                    catch (e: SocketTimeoutException)
                    {
                        if (!socket.isClosed)
                        {
                            writer.write("Took too long, closing connection.\r\n")
                            writer.flush()
                        }
                        break
                    }
                    catch (e: Exception)
                    {
                        logger.error(e) { "Error reading from client: $clientSession" }
                        break
                    }

                    val inputTrimmed = input.trim()

                    val response = onInput(clientSession, inputTrimmed)
                    logger.info { "Request from $clientSession: '$inputTrimmed'. Response: '$response'" }

                    if (response != null && !socket.isClosed)
                    {
                        writer.write("${response}\r\n")
                        writer.flush()
                    }
                }
            }
        }
        sessions.remove(clientSocket)
        logger.info { "Client disconnected: $clientSession" }
    }
}
