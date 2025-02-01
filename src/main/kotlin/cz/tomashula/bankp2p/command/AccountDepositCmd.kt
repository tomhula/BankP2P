package cz.tomashula.bankp2p.command

import cz.tomashula.bankp2p.Account
import cz.tomashula.bankp2p.data.AccountDoesNotExistException
import cz.tomashula.bankp2p.data.BankStorage

class AccountDepositCmd(
    private val storage: BankStorage,
    private val bankCode: String
) : Command(NAME, "$NAME <account>/<bankCode> <amount>")
{
    override suspend fun execute(args: List<String>): String?
    {
        checkArgsCount(args, 2)

        val (accountStr, amountStr) = args
        val account = Account.parse(accountStr) ?: throw SyntaxError(this, args, "Invalid account format")

        val amount = amountStr.toLongOrNull() ?: throw SyntaxError(this, args, "Amount must be a positive integer")

        if (account.bankCode != this.bankCode)
            throw RuntimeException("Bank proxy not implemented yet")

        try
        {
            storage.deposit(account.number, amount)
        }
        catch (e: AccountDoesNotExistException)
        {
            throw CommandError(this, args, e.message!!)
        }

        return null
    }

    companion object
    {
        const val NAME = "AD"

        fun build(account: Account, amount: Long): String
        {
            check(amount >= 0) { "Amount must be non-negative" }

            return "$NAME $account $amount"
        }
    }
}
