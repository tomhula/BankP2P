package cz.tomashula.bankp2p

import cz.tomashula.bankp2p.command.*
import cz.tomashula.bankp2p.config.FileConfigProvider
import cz.tomashula.bankp2p.data.WriteThroughCachedFileBankStorage
import cz.tomashula.bankp2p.server.TelnetServer
import kotlinx.coroutines.runBlocking

fun main()
{
    val configProvider = FileConfigProvider("bank.conf")
    val config = configProvider.getConfig()
    val bankCode = config.bankCode
    val storage = WriteThroughCachedFileBankStorage(config.fileStorage.storageFilePath, config.fileStorage)
    storage.init()
    val commandProcessor = CommandProcessor()
    commandProcessor.registerCommand(AccountBalanceCmd(storage, bankCode))
    commandProcessor.registerCommand(AccountCreateCmd(storage, bankCode))
    commandProcessor.registerCommand(AccountDepositCmd(storage, bankCode))
    commandProcessor.registerCommand(AccountRemoveCmd(storage, bankCode))
    commandProcessor.registerCommand(AccountWithdrawCmd(storage, bankCode))
    commandProcessor.registerCommand(BankCodeCmd(bankCode))
    commandProcessor.registerCommand(BankNumberOfClientsCmd(storage))
    commandProcessor.registerCommand(BankTotalBalanceCmd(storage))

    val server = TelnetServer(config.server.host, config.server.port) { client, input ->
        commandProcessor.execute(input)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
    })

    runBlocking {
        server.start()
    }
}
