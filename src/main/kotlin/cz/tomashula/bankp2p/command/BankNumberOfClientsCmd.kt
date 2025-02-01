package cz.tomashula.bankp2p.command

import cz.tomashula.bankp2p.data.BankStorage

class BankNumberOfClientsCmd(
    private val storage: BankStorage
) : Command(NAME, NAME)
{
    override suspend fun execute(args: List<String>): String
    {
        checkArgsCount(args, 0)

        return storage.bankClientCount().toString()
    }

    companion object
    {
        private const val NAME = "BN"

        fun build() = NAME
    }
}
