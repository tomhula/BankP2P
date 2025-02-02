package cz.tomashula.bankp2p.command

import cz.tomashula.bankp2p.Account
import cz.tomashula.bankp2p.data.AccountCannotBeRemovedException
import cz.tomashula.bankp2p.data.AccountDoesNotExistException
import cz.tomashula.bankp2p.data.BankStorage
import cz.tomashula.bankp2p.proxy.BankFinder
import cz.tomashula.bankp2p.util.executeOnForeignBank

class AccountRemoveCmd(
    private val storage: BankStorage,
    private val bankCode: String,
    private val bankFinder: BankFinder
) : Command(NAME, "$NAME <account>/<ip>")
{
    override suspend fun execute(args: List<String>): String?
    {
        checkArgsCount(args, 1)

        val accountStr = args[0]
        val account = Account.parse(accountStr) ?: throw SyntaxError(this, args, "Invalid account format")

        return if (account.bankCode == this.bankCode)
        try
        {
            storage.removeAccount(account.number).toString()
            null
        }
        catch (e: AccountDoesNotExistException)
        {
            throw CommandError(this, args, e.message!!)
        }
        catch (e: AccountCannotBeRemovedException)
        {
            throw CommandError(this, args, e.message!!)
        }
        else
            executeOnForeignBank(args, account.bankCode, bankFinder) {
                accountRemove(account)
            }
    }

    companion object
    {
        const val NAME = "AR"

        fun build(account: Account) = "$NAME $account"
    }
}
