package cz.tomashula.bankp2p

import ch.qos.logback.classic.Level
import cz.tomashula.bankp2p.command.*
import cz.tomashula.bankp2p.config.FileConfigProvider
import cz.tomashula.bankp2p.data.WriteThroughCachedFileBankStorage
import cz.tomashula.bankp2p.proxy.BankFinder
import cz.tomashula.bankp2p.server.TelnetServer
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main()
{
    val configProvider = FileConfigProvider("bank.conf")
    val config = configProvider.getConfig()
    val bankCode = config.bankCode
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
    rootLogger.level = Level.toLevel(config.logLevel, Level.INFO)
    val bankFinder = BankFinder(config.proxy.scanPortRange, config.proxy.downstreamBankTcpTimeout, config.proxy.downstreamBankResponseTimeout)
    val storage = WriteThroughCachedFileBankStorage(config.fileStorage.storageFilePath, config.fileStorage)
    storage.init()
    val commandProcessor = CommandProcessor()
    commandProcessor.registerCommand(AccountBalanceCmd(storage, bankCode, bankFinder))
    commandProcessor.registerCommand(AccountCreateCmd(storage, bankCode))
    commandProcessor.registerCommand(AccountDepositCmd(storage, bankCode, bankFinder))
    commandProcessor.registerCommand(AccountRemoveCmd(storage, bankCode, bankFinder))
    commandProcessor.registerCommand(AccountWithdrawCmd(storage, bankCode, bankFinder))
    commandProcessor.registerCommand(BankCodeCmd(bankCode))
    commandProcessor.registerCommand(BankNumberOfClientsCmd(storage))
    commandProcessor.registerCommand(BankTotalBalanceCmd(storage))
    commandProcessor.registerCommand(DebugCmd())

    val server = TelnetServer(
        config.server.host,
        config.server.port,
        config.server.clientInactivityTimeout,
        config.server.clientThreadPoolSize
    ) { client, input ->
        commandProcessor.execute(input)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
    })

    runBlocking {
        server.start()
    }
}
