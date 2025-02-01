package cz.tomashula.bankp2p.command

class BankCodeCmd(
    private val bankCode: String,
) : Command(NAME, NAME)
{
    override suspend fun execute(args: List<String>): String
    {
        checkArgsCount(args, 0)
        
        return bankCode
    }

    companion object
    {
        const val NAME = "BC"

        fun build() = NAME
    }
}
