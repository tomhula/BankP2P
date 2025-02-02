package cz.tomashula.bankp2p.client

/**
 * Exception thrown when downstream bank returns an error (ER message).
 */
class DownstreamBankError(
    bankCode: String,
    port: Int,
    val command: String,
    downStreamBankErrorMessage: String
) : BankClientException(bankCode, port, downStreamBankErrorMessage)
