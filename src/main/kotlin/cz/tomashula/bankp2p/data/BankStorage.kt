package cz.tomashula.bankp2p.data

interface BankStorage
{
    suspend fun createAccount(): Int

    /**
     * @throws AccountDoesNotExistException if the account does not exist.
     */
    suspend fun deposit(account: Int, amount: Long)

    /**
     * @throws AccountDoesNotExistException if the account does not exist.
     * @throws InsufficientFundsException if the account does not have enough funds.
     */
    suspend fun withdraw(account: Int, amount: Long)

    /**
     * @throws AccountDoesNotExistException if the account does not exist.
     */
    suspend fun balance(account: Int): Long

    /**
     * @throws AccountDoesNotExistException if the account does not exist.
     */
    suspend fun removeAccount(account: Int)
    suspend fun bankTotal(): Long
    suspend fun bankClientCount(): Int
}
