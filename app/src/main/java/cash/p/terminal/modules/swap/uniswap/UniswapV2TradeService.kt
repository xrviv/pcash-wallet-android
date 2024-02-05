package cash.p.terminal.modules.swap.uniswap

import cash.p.terminal.entities.Address
import cash.p.terminal.modules.swap.SwapMainModule.SwapResultState
import cash.p.terminal.modules.swap.UniversalSwapTradeData
import cash.p.terminal.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class UniswapV2TradeService : IUniswapTradeService {
    private val uniswapKit by lazy { UniswapKit.getInstance() }

    private var swapDataDisposable: Disposable? = null
    private var swapData: SwapData? = null

    override var state: SwapResultState = SwapResultState.NotReady()
        private set(value) {
            field = value
            _stateFlow.update { value }
        }

    override val recipient: Address?
        get() = tradeOptions.recipient
    override val slippage: BigDecimal
        get() = tradeOptions.allowedSlippage
    override val ttl: Long
        get() = tradeOptions.ttl

    private val _stateFlow = MutableStateFlow(state)
    override val stateFlow: StateFlow<SwapResultState>
        get() = _stateFlow

    override var tradeOptions: SwapTradeOptions = SwapTradeOptions()
        set(value) {
            field = value
        }

    override fun stop() {
        clearDisposables()
    }

    override fun updateSwapSettings(recipient: Address?, slippage: BigDecimal?, ttl: Long?) {
        tradeOptions = SwapTradeOptions(
            slippage ?: TradeOptions.defaultAllowedSlippage,
            ttl ?: TradeOptions.defaultTtl,
            recipient
        )
    }

    @Throws
    override fun transactionData(tradeData: UniversalSwapTradeData): TransactionData {
        TODO()
//        return uniswapKit.transactionData(tradeData.getTradeDataV2())
    }

    private fun clearDisposables() {
        swapDataDisposable?.dispose()
        swapDataDisposable = null
    }

    private fun TokenType.isWeth(chain: Chain): Boolean {
        return this is TokenType.Eip20 && address.equals(uniswapKit.etherToken(chain).address.hex, true)
    }

    private fun Token.isWeth(chain: Chain): Boolean = type.isWeth(chain)
    private val Token.isNative: Boolean
        get() = type == TokenType.Native

    private fun isEthWrapping(tokenFrom: Token?, tokenTo: Token?, chain: Chain) =
        when {
            tokenFrom == null || tokenTo == null -> false
            else -> {
                tokenFrom.isNative && tokenTo.isWeth(chain) || tokenTo.isNative && tokenFrom.isWeth(chain)
            }
        }

    sealed class TradeServiceError : Throwable() {
        object WrapUnwrapNotAllowed : TradeServiceError()
    }

}
