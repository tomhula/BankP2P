package cz.tomashula.bankp2p.command

open class CommandError(
    val command: Command,
    val args: List<String>,
    message: String = "Unknown error"
) : RuntimeException(message)
