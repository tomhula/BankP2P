package cz.tomashula.bankp2p.config

import java.nio.file.Path

data class Config(
    val server: Server,
    val fileStorage: FileStorage,
    val logLevel: String,
    val bankCode: String,
)
{
    data class Server(
        val host: String,
        val port: Int
    )

    data class FileStorage(
        val storageFilePath: Path,
        val minAccountNumber: Int,
        val accountNumberBalanceSeparator: String,
        val deletedAccountChar: Char,
    )
}
