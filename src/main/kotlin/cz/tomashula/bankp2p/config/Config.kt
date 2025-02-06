package cz.tomashula.bankp2p.config

import java.net.InetAddress
import java.nio.file.Path
import kotlin.time.Duration

data class Config(
    val server: Server,
    val fileStorage: FileStorage,
    val proxy: Proxy,
    val robbery: Robbery,
    val logLevel: String,
    val bankCode: String,
)
{
    data class Server(
        val host: String,
        val port: Int,
        val clientInactivityTimeout: Duration,
        val clientThreadPoolSize: Int,
    )

    data class FileStorage(
        val storageFilePath: Path,
        val minAccountNumber: Int,
        val accountNumberBalanceSeparator: String,
        val deletedAccountChar: Char,
    )

    data class Proxy(
        val downstreamBankResponseTimeout: Duration,
        val downstreamBankTcpTimeout: Duration,
        val scanPortRange: IntRange,
    )

    data class Robbery(
        val scannerThreadPoolSize: Int,
        val scanNetwork: InetAddress,
        val scanNetworkMask: Int
    )
}
