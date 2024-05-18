package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.AdapterState
import cash.p.terminal.core.IAdapterManager
import cash.p.terminal.core.IBalanceAdapter
import cash.p.terminal.core.ServiceState
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class TokenBalanceService(
    private val adapterManager: IAdapterManager,
) : ServiceState<TokenBalanceService.State>() {
    private var token: Token? = null
    private var amount: BigDecimal? = null
    private var adapter: IBalanceAdapter? = null
    private var balance: BigDecimal? = null
    private var error: Throwable? = null

    override fun createState() = State(
        balance = balance,
        error = error
    )

    fun setToken(token: Token?) {
        this.token = token

        refreshAvailableBalance()
        validate()

        emitState()
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        validate()

        emitState()
    }

    private fun validate() {
        error = null

        val amount = amount ?: return

        error = when (adapter?.balanceState) {
            null -> TokenNotEnabled()
            is AdapterState.SearchingTxs -> WalletSyncing()
            is AdapterState.Syncing -> WalletSyncing()
            is AdapterState.NotSynced -> WalletNotSynced()
            AdapterState.Synced -> {
                if (amount > balance) {
                    SwapError.InsufficientBalanceFrom
                } else {
                    null
                }
            }
        }
    }

    private fun refreshAvailableBalance() {
        adapter = token?.let { adapterManager.getAdapterForToken(it) as? IBalanceAdapter }
        balance = adapter?.balanceData?.available
    }

    data class State(val balance: BigDecimal?, val error: Throwable?)
}

class TokenNotEnabled : Exception()
class WalletSyncing : Exception()
class WalletNotSynced : Exception()
