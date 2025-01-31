package cz.tomashula.bankp2p.data

class AccountDoesNotExistException(account: Int) : BankStorageException("Account $account does not exist")
