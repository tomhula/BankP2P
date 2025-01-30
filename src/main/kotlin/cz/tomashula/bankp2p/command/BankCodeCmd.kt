package cz.tomashula.bankp2p.command

class BankCodeCmd(
    private val bankCode: String,
) : Command("BC")
{
    override fun execute(args: List<String>): String
    {
        return bankCode
    }
}
