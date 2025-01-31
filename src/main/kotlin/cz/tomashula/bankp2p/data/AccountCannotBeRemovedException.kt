package cz.tomashula.bankp2p.data

class AccountCannotBeRemovedException(account: Int, balance: Long) : BankStorageException("Cannot remove account '$account', because it has a balance of $balance")
