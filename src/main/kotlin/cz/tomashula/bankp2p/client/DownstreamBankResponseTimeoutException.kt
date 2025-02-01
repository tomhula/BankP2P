package cz.tomashula.bankp2p.client

import kotlin.time.Duration

class DownstreamBankResponseTimeoutException(
    bankCode: String,
    port: Int,
    timeout: Duration
) : BankClientException(bankCode, port, "Downstream bank did not respond in $timeout.")
