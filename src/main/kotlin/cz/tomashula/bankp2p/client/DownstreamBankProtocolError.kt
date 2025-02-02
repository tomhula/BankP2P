package cz.tomashula.bankp2p.client

class DownstreamBankProtocolError(
    bankCode: String,
    port: Int,
    val requestMessage: String,
    val responseMessage: String
) : BankClientException(bankCode, port, "Downstream bank protocol error. Bank responded with '$responseMessage' to request '$requestMessage'.")
