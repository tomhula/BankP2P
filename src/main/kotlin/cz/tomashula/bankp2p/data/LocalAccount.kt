package cz.tomashula.bankp2p.data

/**
 * Represents a local bank account and its balance.
 */
data class LocalAccount(
    val number: Int,
    var balance: Long = 0
)
