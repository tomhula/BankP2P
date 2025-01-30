package cz.tomashula.bankp2p.command

class BankCodeCmd(
    private val bankCode: String,
) : Command(NAME)
{
    override fun execute(args: List<String>): String
    {
        return bankCode
    }

    companion object
    {
        private const val NAME = "BC"

        fun build() = NAME
    }
}
