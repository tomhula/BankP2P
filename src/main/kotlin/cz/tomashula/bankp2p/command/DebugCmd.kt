package cz.tomashula.bankp2p.command

class DebugCmd : Command(NAME, "DE <sub-command> [args]")
{
    override suspend fun execute(args: List<String>): String
    {
        val subCommand = args.firstOrNull() ?: throw SyntaxError(this, args)

        return when (subCommand)
        {
            "thread" -> thread()
            else -> throw CommandError(this, args, "Subcommand '$subCommand' does not exist.")
        }
    }

    private fun thread(): String
    {
        return Thread.currentThread().name
    }

    companion object
    {
        const val NAME = "DE"
    }
}