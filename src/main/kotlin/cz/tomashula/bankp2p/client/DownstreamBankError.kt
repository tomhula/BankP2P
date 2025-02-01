package cz.tomashula.bankp2p.client

class DownstreamBankError(
    bankCode: String,
    port: Int,
    val command: String,
    downStreamBankErrorMessage: String?
) : BankClientException(bankCode, port, downStreamBankErrorMessage)
