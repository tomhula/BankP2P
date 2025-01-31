package cz.tomashula.bankp2p.data

class InsufficientFundsException(
    account: Int,
    requiredAmount: Long,
    currentBalance: Long
) : BankStorageException("Account $account has insufficient funds. Required: $requiredAmount, Current balance: $currentBalance")
