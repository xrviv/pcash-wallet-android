package cash.p.terminal.modules.swapxxx.providers

import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.convertedError
import cash.p.terminal.modules.swap.scaleUp
import cash.p.terminal.modules.swapxxx.EvmBlockchainHelper
import cash.p.terminal.modules.swapxxx.ISwapQuote
import cash.p.terminal.modules.swapxxx.SwapQuoteOneInch
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.oneinchkit.OneInchKit
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

object OneInchProvider : ISwapXxxProvider {
    override val id = "oneinch"
    override val title = "1inch"
    override val url = "https://app.1inch.io/"
    override val icon = R.drawable.oneinch
    private val oneInchKit by lazy { OneInchKit.getInstance(App.appConfigProvider.oneInchApiKey) }

    // TODO take evmCoinAddress from oneInchKit
    private val evmCoinAddress = Address("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")

    override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne
        -> true

        else -> false
    }

    override suspend fun fetchQuote(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal): ISwapQuote {
        val evmBlockchainHelper = EvmBlockchainHelper(tokenIn.blockchainType)

        val quote = oneInchKit.getQuoteAsync(
            chain = evmBlockchainHelper.chain,
            fromToken = getTokenAddress(tokenIn),
            toToken = getTokenAddress(tokenOut),
            amount = amountIn.scaleUp(tokenIn.decimals)
        ).onErrorResumeNext {
            Single.error(it.convertedError)
        }.await()

        val amountOut = quote.toTokenAmount.abs().toBigDecimal().movePointLeft(quote.toToken.decimals).stripTrailingZeros()
        return SwapQuoteOneInch(amountOut, listOf(), null, tokenIn.blockchainType)
    }

    private fun getTokenAddress(token: Token) = when (val tokenType = token.type) {
        TokenType.Native -> evmCoinAddress
        is TokenType.Eip20 -> Address(tokenType.address)
        else -> throw IllegalStateException("Unsupported tokenType: $tokenType")
    }
}
