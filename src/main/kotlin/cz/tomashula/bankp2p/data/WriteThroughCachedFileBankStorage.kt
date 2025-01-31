package cz.tomashula.bankp2p.data

import cz.tomashula.bankp2p.config.Config
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

class WriteThroughCachedFileBankStorage(
    private val file: Path,
    private val storageConfig: Config.FileStorage
) : BankStorage
{
    private val accountLength = ACCOUNT_NUMBER_MAX_LENGTH + storageConfig.accountNumberBalanceSeparator.length + BALANCE_MAX_LENGTH
    private val lineLength = accountLength + System.lineSeparator().length

    private val accounts: MutableMap<Int, LocalAccount> = mutableMapOf()
    private var currentHighestAccountNumber = storageConfig.minAccountNumber - 1


    /* OPTIMIZE: Can be optimized by locking writing to individual accounts instead of the whole file.
        This can be done by using a map of Mutexes for each account. */
    private val writeMutex = Mutex()

    /** Initializes the bank storage from the file. */
    fun init()
    {
        currentHighestAccountNumber = storageConfig.minAccountNumber - 1

        if (!Files.exists(file))
        {
            Files.createFile(file)
            logger.info { "Created bank storage file '${file.pathString}'" }
            return
        }

        file.readLines().forEach { line ->
            if (line.startsWith(storageConfig.deletedAccountChar))
                return@forEach
            val (accountStr, balanceStr) = line.split(":")
            val accountNumber = accountStr.toInt()
            val balance = balanceStr.toLong()

            if (accountNumber > currentHighestAccountNumber)
                currentHighestAccountNumber = accountNumber

            accounts[accountNumber] = LocalAccount(accountNumber, balance)
        }
    }

    /** Refreshes the cache from the file. */
    fun refresh()
    {
        accounts.clear()
        init()
    }

    private fun fixedLengthAccountString(account: LocalAccount) =
        fixedLengthAccountNumberString(account.number) + storageConfig.accountNumberBalanceSeparator + fixedLengthBalanceString(account.balance)
    private fun padZeroes(number: Long, length: Int) = "%0${length}d".format(number)
    private fun fixedLengthAccountNumberString(account: Int) = padZeroes(account.toLong(), ACCOUNT_NUMBER_MAX_LENGTH)
    private fun fixedLengthBalanceString(balance: Long) = padZeroes(balance, BALANCE_MAX_LENGTH)
    private fun deletedAccountString(account: Int) =
        storageConfig.deletedAccountChar.toString().repeat(ACCOUNT_NUMBER_MAX_LENGTH + 1 + BALANCE_MAX_LENGTH)

    private fun getAccount(accountNumber: Int) =
        accounts[accountNumber] ?: throw AccountDoesNotExistException(accountNumber)

    private fun checkAmountPositive(amount: Long, actionName: String)
    {
        if (amount < 0)
            throw IllegalArgumentException("${actionName.replaceFirstChar { it.uppercaseChar() }} amount must be positive")
    }

    /** WARNING: Not synchronized, must be synchronized by caller */
    private fun updateBalanceThrough(account: LocalAccount)
    {
        RandomAccessFile(file.toFile(), "rw").use { raf ->
            val offset = (account.number - storageConfig.minAccountNumber) * lineLength + ACCOUNT_NUMBER_MAX_LENGTH + storageConfig.accountNumberBalanceSeparator.length
            raf.seek(offset.toLong())
            raf.write(fixedLengthBalanceString(account.balance).toByteArray(CHARSET))
        }
    }

    private fun deleteAccountThrough(account: Int)
    {
        RandomAccessFile(file.toFile(), "rw").use { raf ->
            val offset = (account - storageConfig.minAccountNumber) * lineLength
            raf.seek(offset.toLong())
            raf.write(deletedAccountString(account).toByteArray(CHARSET))
        }
    }

    override suspend fun createAccount() = writeMutex.withLock {
        val account = LocalAccount(++currentHighestAccountNumber)
        accounts[account.number] = account
        file.appendLines(listOf(fixedLengthAccountString(account)), CHARSET)
        account.number
    }

    override suspend fun deposit(accountNumber: Int, amount: Long) = writeMutex.withLock {
        checkAmountPositive(amount, "Deposit")

        val account = getAccount(accountNumber)
        account.balance += amount
        updateBalanceThrough(account)
    }

    override suspend fun withdraw(accountNumber: Int, amount: Long) = writeMutex.withLock {
        checkAmountPositive(amount, "Withdraw")
        val account = getAccount(accountNumber)
        if (account.balance < amount)
            throw InsufficientFundsException(account.number, amount, account.balance)

        account.balance -= amount
        updateBalanceThrough(account)
    }

    override suspend fun balance(accountNumber: Int) = writeMutex.withLock {
        accounts[accountNumber]?.balance ?: throw AccountDoesNotExistException(accountNumber)
    }

    override suspend fun removeAccount(accountNumber: Int) = writeMutex.withLock {
        val account = getAccount(accountNumber)
        if (account.balance != 0L)
            throw AccountCannotBeRemovedException(accountNumber, account.balance)
        accounts.remove(accountNumber)
        deleteAccountThrough(accountNumber)
    }

    override suspend fun bankTotal() = writeMutex.withLock {
        accounts.values.sumOf { it.balance }
    }

    override suspend fun bankClientCount() = writeMutex.withLock {
        accounts.size
    }

    companion object
    {
        private val CHARSET = Charsets.US_ASCII
        private const val ACCOUNT_NUMBER_MAX_LENGTH = Int.MAX_VALUE.toString().length
        private const val BALANCE_MAX_LENGTH = Long.MAX_VALUE.toString().length
    }
}
