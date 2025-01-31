package cz.tomashula.bankp2p.command

abstract class Command(val name: String, val syntax: String)
{
    /**
     * Executes the command with the given [args].
     * @return the command's result.
     */
    abstract suspend fun execute(args: List<String>): String?
}

fun Command.checkArgsCount(args: List<String>, expected: Int)
{
    if (args.size != expected)
        throw SyntaxError(this, args, "Expected $expected arguments")
}
