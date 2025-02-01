package cz.tomashula.bankp2p.command

class BankNotFoundError(
    command: Command,
    args: List<String>,
    val bankCode: String
) : CommandError(
    command,
    args,
    "Bank $bankCode not found"
)
