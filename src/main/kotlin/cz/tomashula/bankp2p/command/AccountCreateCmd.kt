package cz.tomashula.bankp2p.command

import cz.tomashula.bankp2p.data.BankStorage

class AccountCreateCmd(
    private val storage: BankStorage,
    private val bankCode: String
) : Command(NAME, NAME)
{
    override suspend fun execute(args: List<String>): String
    {
        checkArgsCount(args, 0)

        return storage.createAccount().toString() + "/" + bankCode
    }

    companion object
    {
        const val NAME = "AC"

        fun build() = NAME
    }
}
