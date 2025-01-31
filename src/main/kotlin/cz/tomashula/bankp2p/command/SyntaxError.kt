package cz.tomashula.bankp2p.command

class SyntaxError(
    command: Command,
    args: List<String>,
    message: String = "Incorrect syntax"
) : CommandError(command, args, "$message. Correct syntax: '${command.syntax}'")
