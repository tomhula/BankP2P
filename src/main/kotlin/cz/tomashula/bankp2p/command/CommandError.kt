package cz.tomashula.bankp2p.command

open class CommandError(
    val command: Command,
    val args: List<String>,
    override val message: String = "Unknown error"
) : RuntimeException()
