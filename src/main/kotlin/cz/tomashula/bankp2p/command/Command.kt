package cz.tomashula.bankp2p.command

abstract class Command(val name: String)
{
    /**
     * Executes the command with the given [args].
     * @return the command's result.
     */
    abstract suspend fun execute(args: List<String>): String
}
