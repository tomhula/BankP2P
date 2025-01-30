package cz.tomashula.bankp2p.command

class CommandError(
    val command: Command,
    val args: List<String>,
    message: String = "Unknown error"
) : RuntimeException(message)
