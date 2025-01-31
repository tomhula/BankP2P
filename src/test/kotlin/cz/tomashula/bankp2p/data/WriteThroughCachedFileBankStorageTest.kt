package cz.tomashula.bankp2p.data

import cz.tomashula.bankp2p.config.Config
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.*

internal class WriteThroughCachedFileBankStorageTest
{
    companion object
    {
        val config = Config.FileStorage(
            storageFilePath = Path("bank_storage.txt"),
            minAccountNumber = 10000,
            accountNumberBalanceSeparator = ":",
            deletedAccountChar = 'X'
        )
    }

    private lateinit var storage: WriteThroughCachedFileBankStorage
    private lateinit var tempFile: Path

    @BeforeTest
    fun setUp()
    {
        tempFile = Files.createTempFile("bank_storage", ".txt")
        storage = WriteThroughCachedFileBankStorage(tempFile, config)
        storage.init()
    }

    @AfterTest
    fun tearDown()
    {
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `creating an account returns a valid account ID`() = runBlocking {
        val accountId = storage.createAccount()
        assert(accountId >= 10000)
    }

    @Test
    fun `depositing money increases balance`() = runBlocking {
        val accountId = storage.createAccount()
        storage.deposit(accountId, 1000)
        storage.refresh()
        assertEquals(1000, storage.balance(accountId))
    }

    @Test
    fun `depositing into a non-existent account throws exception`(): Unit = runBlocking {
        assertFailsWith<AccountDoesNotExistException> {
            storage.deposit(99999, 1000)
        }
    }

    @Test
    fun `withdrawing money decreases balance`() = runBlocking {
        val accountId = storage.createAccount()
        storage.deposit(accountId, 1000)
        storage.withdraw(accountId, 500)
        storage.refresh()
        assertEquals(500, storage.balance(accountId))
    }

    @Test
    fun `withdrawing more than balance throws exception`(): Unit = runBlocking {
        val accountId = storage.createAccount()
        storage.deposit(accountId, 500)
        assertFailsWith<InsufficientFundsException> {
            storage.withdraw(accountId, 1000)
        }
    }

    @Test
    fun `withdrawing from non-existent account throws exception`(): Unit = runBlocking {
        assertFailsWith<AccountDoesNotExistException> {
            storage.withdraw(99999, 1000)
        }
    }

    @Test
    fun `removing an account makes it inaccessible`(): Unit = runBlocking {
        val accountId = storage.createAccount()
        storage.removeAccount(accountId)
        storage.refresh()
        assertFailsWith<AccountDoesNotExistException> {
            storage.balance(accountId)
        }
    }

    @Test
    fun `removing an account with non-zero balance throws exception`(): Unit = runBlocking {
        val accountId = storage.createAccount()
        storage.deposit(accountId, 1000)
        storage.refresh()
        assertFailsWith<AccountCannotBeRemovedException> {
            storage.removeAccount(accountId)
        }
    }

    @Test
    fun `bank total reflects deposits and withdrawals`() = runBlocking {
        val acc1 = storage.createAccount()
        val acc2 = storage.createAccount()
        storage.deposit(acc1, 2000)
        storage.deposit(acc2, 1000)
        storage.withdraw(acc1, 500)
        storage.refresh()
        assertEquals(2500, storage.bankTotal())
    }

    @Test
    fun `bank client count reflects number of accounts`() = runBlocking {
        assertEquals(0, storage.bankClientCount())
        val acc1 = storage.createAccount()
        val acc2 = storage.createAccount()
        storage.refresh()
        assertEquals(2, storage.bankClientCount())
    }
}
