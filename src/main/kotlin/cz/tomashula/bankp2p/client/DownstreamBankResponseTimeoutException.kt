package cz.tomashula.bankp2p.client

import kotlin.time.Duration


/**
 * Exception thrown when downstream bank does not respond in time.
 * This is after a connection is already established, but the bank takes too long to respond to a command.
 */
class DownstreamBankResponseTimeoutException(
    bankCode: String,
    port: Int,
    timeout: Duration
) : BankClientException(bankCode, port, "Downstream bank did not respond in $timeout.")
