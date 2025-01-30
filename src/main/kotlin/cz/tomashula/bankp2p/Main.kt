package cz.tomashula.bankp2p

import cz.tomashula.bankp2p.command.BankCodeCmd
import cz.tomashula.bankp2p.command.CommandParser
import java.util.*

fun main()
{
    val scanner = Scanner(System.`in`)
    val commandParser = CommandParser()
    commandParser.registerCommand(BankCodeCmd("10.20.30.40"))

    while (scanner.hasNextLine())
    {
        val line = scanner.nextLine()
        println(commandParser.execute(line))
    }
}
