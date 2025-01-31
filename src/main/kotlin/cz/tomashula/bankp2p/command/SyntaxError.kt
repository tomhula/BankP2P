package cz.tomashula.bankp2p.command

class SyntaxError(
    command: Command,
    args: List<String>
) : CommandError(command, args, "Incorrect syntax. Correct syntax: ${command.syntax}")
