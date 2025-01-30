package cz.tomashula.bankp2p.command

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class CommandParser
{
    private val commands = mutableMapOf<String, Command>()

    fun registerCommand(command: Command)
    {
        commands[command.name] = command
    }

    /**
     * Returns the entire output of the matched command.
     */
    fun execute(input: String): String
    {
        val parts = input.trim().split("\\s+".toRegex())
        val commandName = parts.firstOrNull()
        val args = if (parts.size > 1) parts.subList(1, parts.size) else emptyList()

        val command = commands[commandName] ?: return buildError("Unknown command '$commandName'")

        try
        {
            val commandResult = command.execute(args)
            return "${command.name} $commandResult"
        }
        catch (e: CommandError)
        {
            return buildError(e.message ?: "Unknown error") // Shouldn't be null
        }
        catch (e: Exception)
        {
            logger.error(e) { "Error while executing command ${command.name}" }
            return buildError("Internal error")
        }
    }

    private fun buildError(message: String): String = "ER $message"
}
