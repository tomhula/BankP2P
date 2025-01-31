package cz.tomashula.bankp2p.data

interface BankStorage
{
    suspend fun createAccount(): Int

    /**
     * @throws AccountDoesNotExistException if the account does not exist.
     */
    suspend fun deposit(accountNumber: Int, amount: Long)

    /**
     * @throws AccountDoesNotExistException if the account does not exist.
     * @throws InsufficientFundsException if the account does not have enough funds.
     */
    suspend fun withdraw(accountNumber: Int, amount: Long)

    /**
     * @throws AccountDoesNotExistException if the account does not exist.
     */
    suspend fun balance(accountNumber: Int): Long

    /**
     * @throws AccountDoesNotExistException if the account does not exist.
     */
    suspend fun removeAccount(accountNumber: Int)
    suspend fun bankTotal(): Long
    suspend fun bankClientCount(): Int
}
