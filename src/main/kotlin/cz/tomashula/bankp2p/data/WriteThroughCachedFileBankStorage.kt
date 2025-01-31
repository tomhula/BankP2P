package cz.tomashula.bankp2p.data

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

class WriteThroughCachedFileBankStorage(
    private val file: Path
) : BankStorage
{
    private val accounts: MutableMap<Int, Long> = mutableMapOf()
    private var maxAccountNumber = MIN_ACCOUNT_NUMBER - 1


    /* OPTIMIZE: Can be optimized by locking writing to individual accounts instead of the whole file.
        This can be done by using a map of Mutexes for each account. */
    private val writeMutex = Mutex()

    /** Initializes the bank storage from the file. */
    fun init()
    {
        if (!Files.exists(file))
        {
            Files.createFile(file)
            logger.info { "Created bank storage file '${file.pathString}'" }
            return
        }

        file.readLines().forEach { line ->
            if (line.startsWith(DELETED_ACCOUNT_CHAR))
                return@forEach
            val (accountStr, balanceStr) = line.split(":")
            val account = accountStr.toInt()
            val balance = balanceStr.toLong()

            if (account > maxAccountNumber)
                maxAccountNumber = account

            accounts[account] = balance
        }
    }

    /** Reloads the bank storage from the file. */
    fun reload()
    {
        accounts.clear()
        maxAccountNumber = MIN_ACCOUNT_NUMBER - 1
        init()
    }

    private fun fixedLengthAccountString(account: Int, balance: Long) =
        fixedLengthAccountNumberString(account) + SEPARATOR + fixedLengthBalanceString(balance)
    private fun padZeroes(number: Long, length: Int) = "%0${length}d".format(number)
    private fun fixedLengthAccountNumberString(account: Int) = padZeroes(account.toLong(), ACCOUNT_NUMBER_MAX_LENGTH)
    private fun fixedLengthBalanceString(balance: Long) = padZeroes(balance, BALANCE_MAX_LENGTH)
    private fun deletedAccountString(account: Int) =
        DELETED_ACCOUNT_CHAR.toString().repeat(ACCOUNT_NUMBER_MAX_LENGTH + 1 + BALANCE_MAX_LENGTH)

    private fun checkAccountExists(account: Int)
    {
        if (account !in accounts)
            throw AccountDoesNotExistException(account)
    }

    private fun checkAmountPositive(amount: Long, actionName: String)
    {
        if (amount < 0)
            throw IllegalArgumentException("${actionName.replaceFirstChar { it.uppercaseChar() }} amount must be positive")
    }

    /** WARNING: Not synchronized, must be synchronized by caller */
    private fun updateBalanceThrough(account: Int, balance: Long)
    {
        RandomAccessFile(file.toFile(), "rw").use { raf ->
            val offset = (account - MIN_ACCOUNT_NUMBER) * LINE_LENGTH + ACCOUNT_NUMBER_MAX_LENGTH + SEPARATOR.length
            raf.seek(offset.toLong())
            raf.write(fixedLengthBalanceString(balance).toByteArray(CHARSET))
        }
    }

    private fun deleteAccountThrough(account: Int)
    {
        RandomAccessFile(file.toFile(), "rw").use { raf ->
            val offset = (account - MIN_ACCOUNT_NUMBER) * LINE_LENGTH
            raf.seek(offset.toLong())
            raf.write(deletedAccountString(account).toByteArray(CHARSET))
        }
    }

    override suspend fun createAccount() = writeMutex.withLock {
        val account = ++maxAccountNumber
        val balance = 0L
        accounts[account] = balance
        file.appendLines(listOf(fixedLengthAccountString(account, balance)), CHARSET)
        account
    }

    override suspend fun deposit(account: Int, amount: Long) = writeMutex.withLock {
        checkAccountExists(account)
        checkAmountPositive(amount, "Deposit")

        val oldBalance = accounts[account]!!
        val newBalance = oldBalance + amount
        accounts[account] = newBalance
        updateBalanceThrough(account, newBalance)
    }

    override suspend fun withdraw(account: Int, amount: Long) = writeMutex.withLock {
        checkAccountExists(account)
        checkAmountPositive(amount, "Withdraw")
        val oldBalance = accounts[account]!!
        if (oldBalance < amount)
            throw InsufficientFundsException(account, amount, oldBalance)

        val newBalance = oldBalance - amount
        accounts[account] = newBalance
        updateBalanceThrough(account, newBalance)
    }

    override suspend fun balance(account: Int) = writeMutex.withLock {
        if (account !in accounts)
            throw AccountDoesNotExistException(account)

        accounts[account]!!
    }

    override suspend fun removeAccount(account: Int) = writeMutex.withLock {
        checkAccountExists(account)
        accounts.remove(account)
        deleteAccountThrough(account)
    }

    override suspend fun bankTotal() = writeMutex.withLock {
        accounts.values.sum()
    }

    override suspend fun bankClientCount() = writeMutex.withLock {
        accounts.size
    }

    companion object
    {
        private val CHARSET = Charsets.US_ASCII
        private const val ACCOUNT_NUMBER_MAX_LENGTH = Int.MAX_VALUE.toString().length
        private const val BALANCE_MAX_LENGTH = Long.MAX_VALUE.toString().length
        private const val SEPARATOR = ":"
        private const val DELETED_ACCOUNT_CHAR = '-'
        private const val ACCOUNT_LENGTH = ACCOUNT_NUMBER_MAX_LENGTH + SEPARATOR.length + BALANCE_MAX_LENGTH
        private val LINE_LENGTH = ACCOUNT_LENGTH + System.lineSeparator().length
        private const val MIN_ACCOUNT_NUMBER = 10000
    }
}
