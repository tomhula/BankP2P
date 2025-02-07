package cz.tomashula.bankp2p.command

import cz.tomashula.bankp2p.Account
import cz.tomashula.bankp2p.data.AccountDoesNotExistException
import cz.tomashula.bankp2p.data.BankStorage
import cz.tomashula.bankp2p.data.InsufficientFundsException
import cz.tomashula.bankp2p.proxy.BankFinder
import cz.tomashula.bankp2p.util.executeOnForeignBank

class AccountWithdrawCmd(
    private val storage: BankStorage,
    private val bankCode: String,
    private val bankFinder: BankFinder
) : Command(NAME, "$NAME <account>/<bankCode> <amount>")
{
    override suspend fun execute(args: List<String>): String?
    {
        checkArgsCount(args, 2)

        val (accountStr, amountStr) = args

        val account = Account.parse(accountStr) ?: throw SyntaxError(this, args, "Invalid account format")
        val amount = amountStr.toLongOrNull() ?: throw SyntaxError(this, args, "Amount must be a positive integer")

        if (amount < 0)
            throw SyntaxError(this, args, "Amount must be a positive integer")


        return if (account.bankCode == this.bankCode)
            try
            {
                storage.withdraw(account.number, amount).toString()
                null
            }
            catch (e: AccountDoesNotExistException)
            {
                throw CommandError(this, args, e.message!!)
            }
            catch (e: InsufficientFundsException)
            {
                throw CommandError(this, args, e.message!!)
            }
        else
            executeOnForeignBank(args, account.bankCode, bankFinder) {
                accountWithdraw(account, amount)
            }
    }

    companion object
    {
        const val NAME = "AW"

        fun build(account: Account, amount: Long): String
        {
            check(account.number >= 0) { "Account number must be non-negative" }
            check(amount >= 0) { "Amount must be non-negative" }

            return "$NAME $account $amount"
        }
    }
}
