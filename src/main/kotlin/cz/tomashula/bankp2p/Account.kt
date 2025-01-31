package cz.tomashula.bankp2p

data class Account(
    val number: Int,
    val bankCode: String,
)
{
    init
    {
        check(number >= 0) { "Account number must be non-negative" }
    }

    override fun toString() = "$number/$bankCode"

    companion object
    {
        fun parse(account: String): Account?
        {
            return try
            {
                val (number, bankCode) = account.split("/")
                Account(number.toInt(), bankCode)
            }
            catch (e: Exception)
            {
                null
            }
        }
    }
}
