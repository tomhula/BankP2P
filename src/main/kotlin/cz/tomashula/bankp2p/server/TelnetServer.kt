package cz.tomashula.bankp2p.server

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/* CONSIDER: ServerSocketChannel is intended for non-blocking code, however it is used blockingly here.
*   Consider switching to ServerSocket or implementing the ServerSocketChannel properly */
class TelnetServer(
    private val host: String,
    private val port: Int,
    private val clientsThreadPoolSize: Int,
    private val onInput: suspend (ClientSession, String) -> String?
)
{
    @OptIn(DelicateCoroutinesApi::class)
    private val coroutineScope = CoroutineScope(newFixedThreadPoolContext(clientsThreadPoolSize, "TelnetServer clients thread") + SupervisorJob() + CoroutineName("TelnetServer clients"))
    @Volatile
    private var running: Boolean = false
    private lateinit var serverChannel: ServerSocketChannel
    private val sessions: MutableMap<SocketChannel, ClientSession> = ConcurrentHashMap<SocketChannel, ClientSession>()

    init
    {
        require(port in 1..65535) { "BindPort must be between 1 and 65535" }
        require(host.isNotEmpty()) { "BindAddress must not be empty" }
    }

    suspend fun start()
    {
        withContext(CoroutineName("TelnetServer") + Dispatchers.IO) {
            serverChannel = ServerSocketChannel.open()
            serverChannel.bind(InetSocketAddress(host, port))
            serverChannel.configureBlocking(true)
            running = true
            logger.info { "Server started on $host:$port" }

            while (running)
            {
                val clientChannel = serverChannel.accept()
                handleClient(clientChannel)
            }
        }
    }

    fun stop()
    {
        running = false
        sessions.forEach { (channel, clientSession) ->
            logger.info { "Closing client session: $clientSession" }
            channel.close()
        }
        sessions.clear()
        serverChannel.close()
    }

    private fun handleClient(clientChannel: SocketChannel)
    {
        val inetSocketAddress = clientChannel.remoteAddress as InetSocketAddress
        val clientSession = ClientSession(inetSocketAddress.hostString, inetSocketAddress.port)
        sessions[clientChannel] = clientSession
        logger.info { "Client connected: $clientSession" }

        coroutineScope.launch(CoroutineName("Client: $clientSession")) {
            clientChannel.use { channel ->
                val buffer = ByteBuffer.allocate(1024)
                var leftover = ""

                while (channel.isOpen && running)
                {
                    buffer.clear()
                    val bytesRead = channel.read(buffer)

                    if (bytesRead == -1)
                        break

                    buffer.flip()
                    val currentInput = String(buffer.array(), 0, buffer.limit())
                    val fullInput = leftover + currentInput

                    val lines = fullInput.split("\n")
                    leftover = lines.last() // Store incomplete line

                    // Process all complete lines except the last one (which may be incomplete)
                    lines.dropLast(1).forEach { line ->
                        val trimmedLine = line.trim()
                        if (trimmedLine.isNotEmpty()) {
                            val response = onInput(clientSession, trimmedLine)
                            logger.info { "Received input from $clientSession: '$trimmedLine'. Response: '$response'" }
                            if (response != null && channel.isOpen) {
                                channel.write(ByteBuffer.wrap("${response}\n".toByteArray()))
                            }
                        }
                    }
                }
            }
            sessions.remove(clientChannel)
            logger.info { "Client disconnected: $clientSession" }
        }
    }
}
