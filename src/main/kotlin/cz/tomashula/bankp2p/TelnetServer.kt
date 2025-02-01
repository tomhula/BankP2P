package cz.tomashula.bankp2p

import cz.tomashula.bankp2p.server.ClientSession
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

/* CONSIDER: ServerSocketChannel is intended for non-blocking code, however it is used blockingly here.
*   Consider switching to ServerSocket or implementing the ServerSocketChannel properly */
class TelnetServer(
    private val host: String,
    private val port: Int,
    private val onInput: (ClientSession, String) -> String?
)
{
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("TelnetServer clients"))
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

                while (channel.isOpen && running)
                {
                    buffer.clear()
                    val bytesRead = channel.read(buffer)

                    /* Connection close check */
                    if (bytesRead == -1)
                        break

                    buffer.flip()
                    val input = String(buffer.array(), 0, buffer.limit()).trim()
                    val response = onInput(clientSession, input)
                    logger.debug { "Received input from $clientSession: '$input'. Response: '$response'" }
                    if (response != null && channel.isOpen)
                        channel.write(ByteBuffer.wrap("${response}\n".toByteArray()))
                }
            }
            sessions.remove(clientChannel)
            logger.info { "Client disconnected: $clientSession" }
        }
    }
}
