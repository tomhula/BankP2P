package cz.tomashula.bankp2p.command

import cz.tomashula.bankp2p.proxy.BankFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class BankRobberyCmd(
    val bankFinder: BankFinder,
    val bankCode: String
) : Command(NAME, NAME)
{
    override suspend fun execute(args: List<String>): String
    {
        checkArgsCount(args, 1)

        val bankAddress = withContext(Dispatchers.IO) {
            InetAddress.getByName(bankCode)
        }

        val targetStr = args[0]
        val target = targetStr.toLongOrNull() ?: throw SyntaxError(this, args, "Target must be a positive integer")
        if (target <= 0) throw SyntaxError(this, args, "Target must be a positive integer")

        val banks = ConcurrentHashMap.newKeySet<BankInfo>()

        bankFinder.findAllBanks(bankAddress) { bankClient ->
            val clientsCountDef = async { bankClient.bankNumberOfClients()?.toIntOrNull() }
            val totalBalanceDef = async { bankClient.bankTotalBalance()?.toLongOrNull() }
            awaitAll(clientsCountDef, totalBalanceDef)
            val clientCount = clientsCountDef.await()
            val totalBalance = totalBalanceDef.await()
            if (clientCount != null && totalBalance != null)
                banks.add(
                    BankInfo(
                        code = bankClient.host,
                        port = bankClient.port,
                        clientCount = clientCount,
                        totalBalance = totalBalance
                    )
                )
        }

        val bankCombinations = withContext(Dispatchers.Default) {
            banks.allCombinations()
        }

        val closestBankCombo = bankCombinations.minByOrNull { comb -> abs(comb.sumOf { it.totalBalance } - target) + comb.sumOf { it.clientCount } }

        if (closestBankCombo == null)
            return "No banks found"

        val robAmount = closestBankCombo.sumOf { it.totalBalance }
        val robClients = closestBankCombo.sumOf { it.clientCount }
        val robBanksString = closestBankCombo.joinToString(separator = ", ") { it.code }

        return "$$robAmount would be stolen from total of $robClients clients from these banks: $robBanksString"
    }

    /* AI PROMPT: Write a Kotlin function with this signature: Iterable<T>.combinations(k: Int, action: (Set<T>) -> Unit): Set<Set<T>> */
    private fun <T> Iterable<T>.combinations(k: Int, action: (Set<T>) -> Unit): Set<Set<T>>
    {
        if (k < 0 || k > this.count()) return emptySet()
        if (k == 0) return setOf(emptySet())

        val inputList = this.toList()
        val result = mutableSetOf<Set<T>>()

        fun backtrack(start: Int, currentCombination: MutableList<T>)
        {
            if (currentCombination.size == k)
            {
                val combinationSet = currentCombination.toSet()
                action(combinationSet)
                result.add(combinationSet)
                return
            }

            for (i in start until inputList.size)
            {
                currentCombination.add(inputList[i])
                backtrack(i + 1, currentCombination)
                currentCombination.removeAt(currentCombination.size - 1)
            }
        }

        backtrack(0, mutableListOf())
        return result
    }

    /* AI PROMPT: Now write a function, that generates all combinations (of all sizes) of an Iterable. */
    fun <T> Iterable<T>.allCombinations(action: (Set<T>) -> Unit = {}): Set<Set<T>>
    {
        val inputList = this.toList()
        val result = mutableSetOf<Set<T>>()

        for (k in 1..inputList.size)
        {
            result.addAll(inputList.combinations(k) { combination ->
                action(combination)
            })
        }

        return result
    }

    companion object
    {
        const val NAME = "BR"
    }

    data class BankInfo(
        val code: String,
        val port: Int,
        val clientCount: Int,
        val totalBalance: Long
    )
}
