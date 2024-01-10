package cash.p.terminal.modules.swapxxx

import cash.p.terminal.core.App
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.SwapQuote
import cash.p.terminal.modules.swap.oneinch.OneInchKitHelper
import cash.p.terminal.modules.swap.oneinch.OneInchTradeService
import cash.p.terminal.modules.swap.uniswap.UniswapV2TradeService
import cash.p.terminal.modules.swap.uniswapv3.UniswapV3TradeService
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.DexType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class SwapProvidersManager {
    private val evmKit: EthereumKit by lazy { App.evmBlockchainManager.getEvmKitManager(BlockchainType.Ethereum).evmKitWrapper?.evmKit!! }
    private val oneIncKitHelper by lazy { OneInchKitHelper(evmKit, App.appConfigProvider.oneInchApiKey) }
    private val uniswapKit by lazy { UniswapKit.getInstance(evmKit) }
    private val uniswapV3Kit by lazy { UniswapV3Kit.getInstance(evmKit, DexType.Uniswap) }
    private val pancakeSwapV3Kit by lazy { UniswapV3Kit.getInstance(evmKit, DexType.PancakeSwap) }


    private val allProviders = listOf(
        SwapMainModule.OneInchProvider,
        SwapMainModule.PancakeSwapProvider,
        SwapMainModule.PancakeSwapV3Provider,
        SwapMainModule.QuickSwapProvider,
        SwapMainModule.UniswapProvider,
        SwapMainModule.UniswapV3Provider,
    )
    private val oneInchTradeService = OneInchTradeService(oneIncKitHelper)
    private val uniswapV3TradeService = UniswapV3TradeService(uniswapV3Kit)
    private val uniswapV3TradeService1 = UniswapV3TradeService(pancakeSwapV3Kit)
    private val uniswapV2TradeService = UniswapV2TradeService(uniswapKit)

    suspend fun getQuotes(
        tokenFrom: Token,
        tokenTo: Token,
        amountFrom: BigDecimal,
    ): List<SwapProviderQuote> {
        return coroutineScope {
            val providers = allProviders.filter {
                it.supports(tokenFrom, tokenTo)
            }
            providers
                .map { provider ->
                    async {
                        try {
                            val quote = getTradeService(provider).fetchQuote(tokenFrom, tokenTo, amountFrom)
                            SwapProviderQuote(provider, quote)
                        } catch (e: Throwable) {
                            null
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }

    private fun getTradeService(provider: SwapMainModule.ISwapProvider): SwapMainModule.ISwapTradeService = when (provider) {
        SwapMainModule.OneInchProvider -> oneInchTradeService
        SwapMainModule.UniswapV3Provider -> uniswapV3TradeService
        SwapMainModule.PancakeSwapV3Provider -> uniswapV3TradeService1
        else -> uniswapV2TradeService
    }

}

data class SwapProviderQuote(
    val provider: SwapMainModule.ISwapProvider,
    val quote: SwapQuote
)
