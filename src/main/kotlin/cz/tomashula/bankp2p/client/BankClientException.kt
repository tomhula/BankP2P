package cz.tomashula.bankp2p.client

/**
 * Base exception for all exceptions thrown by BankClient.
 */
open class BankClientException(
    val bankCode: String,
    val port: Int,
    override val message: String
) : Exception()
