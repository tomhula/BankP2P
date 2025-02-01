package cz.tomashula.bankp2p.util

import cz.tomashula.bankp2p.client.BankClient
import cz.tomashula.bankp2p.client.DownstreamBankError
import cz.tomashula.bankp2p.command.BankNotFoundError
import cz.tomashula.bankp2p.command.Command
import cz.tomashula.bankp2p.command.CommandError
import cz.tomashula.bankp2p.proxy.BankFinder

suspend fun Command.executeOnForeignBank(
    args: List<String>,
    bankCode: String,
    bankFinder: BankFinder,
    action: suspend BankClient.() -> String?
): String?
{
    val bankClient = bankFinder.findFirstBank(bankCode) ?: throw BankNotFoundError(this, args, bankCode)

    return try
    {
        bankClient.use { bc ->
            bc.action()
        }
    }
    catch (e: DownstreamBankError)
    {
        throw CommandError(this, args, "Downstream bank error: ${e.message}")
    }
}
