package cz.tomashula.bankp2p.command

import cz.tomashula.bankp2p.data.BankStorage

class BankTotalBalanceCmd(
    private val storage: BankStorage
) : Command(NAME, NAME)
{
    override suspend fun execute(args: List<String>): String
    {
        checkArgsCount(args, 0)

        return storage.bankTotal().toString()
    }

    companion object
    {
        private const val NAME = "BA"
    }
}
