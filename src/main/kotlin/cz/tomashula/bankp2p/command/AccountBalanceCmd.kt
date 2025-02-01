package cz.tomashula.bankp2p.command

import cz.tomashula.bankp2p.Account
import cz.tomashula.bankp2p.data.AccountDoesNotExistException
import cz.tomashula.bankp2p.data.BankStorage

class AccountBalanceCmd(
    private val storage: BankStorage,
    private val bankCode: String
) : Command(NAME, "$NAME <account>/<ip>")
{
    override suspend fun execute(args: List<String>): String?
    {
        checkArgsCount(args, 1)

        val accountStr = args[0]
        val account = Account.parse(accountStr) ?: throw SyntaxError(this, args, "Invalid account format")

        if (account.bankCode != this.bankCode)
            throw RuntimeException("Bank proxy not implemented yet")

        return try
        {
            storage.balance(account.number).toString()
        }
        catch (e: AccountDoesNotExistException)
        {
            throw CommandError(this, args, e.message!!)
        }
    }

    companion object
    {
        private const val NAME = "AB"

        fun build(account: Account) = "$NAME $account"
    }
}
