package cz.tomashula.bankp2p

data class Account(
    val number: Int,
    val bankCode: String,
)
{
    override fun toString() = "$number/$bankCode"

    companion object
    {
        fun parse(account: String): Account
        {
            val (number, bankCode) = account.split("/")
            return Account(number.toInt(), bankCode)
        }
    }
}
