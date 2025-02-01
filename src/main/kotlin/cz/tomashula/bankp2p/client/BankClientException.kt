package cz.tomashula.bankp2p.client

open class BankClientException(
    val bankCode: String,
    val port: Int,
    message: String?
) : Exception(message)
