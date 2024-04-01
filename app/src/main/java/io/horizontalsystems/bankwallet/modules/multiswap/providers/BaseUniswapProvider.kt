package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.modules.multiswap.EvmBlockchainHelper
import cash.p.terminal.modules.multiswap.ISwapFinalQuote
import cash.p.terminal.modules.multiswap.ISwapQuote
import cash.p.terminal.modules.multiswap.SwapFinalQuoteEvm
import cash.p.terminal.modules.multiswap.SwapQuoteUniswap
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.settings.SwapSettingDeadline
import cash.p.terminal.modules.multiswap.settings.SwapSettingRecipient
import cash.p.terminal.modules.multiswap.settings.SwapSettingSlippage
import cash.p.terminal.modules.multiswap.ui.SwapDataFieldAllowance
import cash.p.terminal.modules.multiswap.ui.SwapDataFieldRecipient
import cash.p.terminal.modules.multiswap.ui.SwapDataFieldRecipientExtended
import cash.p.terminal.modules.multiswap.ui.SwapDataFieldSlippage
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

abstract class BaseUniswapProvider : EvmSwapProvider() {
    private val uniswapKit by lazy { UniswapKit.getInstance() }

    final override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote {
        val blockchainType = tokenIn.blockchainType

        val settingRecipient = SwapSettingRecipient(settings, blockchainType)
        val settingSlippage = SwapSettingSlippage(settings, TradeOptions.defaultAllowedSlippage)
        val settingDeadline = SwapSettingDeadline(settings, TradeOptions.defaultTtl)

        val tradeOptions = TradeOptions(
            allowedSlippagePercent = settingSlippage.valueOrDefault(),
            ttl = settingDeadline.valueOrDefault(),
            recipient = settingRecipient.getEthereumKitAddress(),
        )

        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)
        val swapData = swapDataSingle(tokenIn, tokenOut, evmBlockchainHelper).await()
        val tradeData = uniswapKit.bestTradeExactIn(swapData, amountIn, tradeOptions)
        val routerAddress = uniswapKit.routerAddress(evmBlockchainHelper.chain)
        val allowance = getAllowance(tokenIn, routerAddress)
        val fields = buildList {
            settingRecipient.value?.let {
                add(SwapDataFieldRecipient(it))
            }
            settingSlippage.value?.let {
                add(SwapDataFieldSlippage(it))
            }
            if (allowance != null && allowance < amountIn) {
                add(SwapDataFieldAllowance(allowance, tokenIn))
            }
        }

        return SwapQuoteUniswap(
            fields,
            listOf(settingRecipient, settingSlippage, settingDeadline),
            tokenIn,
            tokenOut,
            amountIn,
            actionApprove(allowance, amountIn, routerAddress, tokenIn),
            tradeData
        )
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
    ): ISwapFinalQuote {
        val blockchainType = tokenIn.blockchainType
        val evmBlockchainHelper = EvmBlockchainHelper(blockchainType)

        val swapQuote = fetchQuote(tokenIn, tokenOut, amountIn, swapSettings) as SwapQuoteUniswap

        val transactionData = evmBlockchainHelper.receiveAddress?.let { receiveAddress ->
            uniswapKit.transactionData(receiveAddress, evmBlockchainHelper.chain, swapQuote.tradeData)
        } ?: throw Exception("No Receive Address")

        val settingRecipient = SwapSettingRecipient(swapSettings, blockchainType)
        val settingSlippage = SwapSettingSlippage(swapSettings, TradeOptions.defaultAllowedSlippage)
        val slippage = settingSlippage.valueOrDefault()

        val amountOut = swapQuote.amountOut
        val amountOutMin = amountOut - amountOut / BigDecimal(100) * slippage

        val fields = buildList {
            settingRecipient.value?.let {
                add(SwapDataFieldRecipientExtended(it, tokenOut.blockchainType))
            }
            settingSlippage.value?.let {
                add(SwapDataFieldSlippage(it))
            }
        }

        return SwapFinalQuoteEvm(
            tokenIn,
            tokenOut,
            amountIn,
            amountOut,
            amountOutMin,
            SendTransactionData.Evm(transactionData, null),
            swapQuote.priceImpact,
            fields
        )
    }


    private fun swapDataSingle(
        tokenIn: Token,
        tokenOut: Token,
        evmBlockchainHelper: EvmBlockchainHelper
    ): Single<SwapData> {
        return try {
            val chain = evmBlockchainHelper.chain

            uniswapKit.swapData(
                evmBlockchainHelper.getRpcSourceHttp(),
                chain,
                uniswapToken(tokenIn, chain),
                uniswapToken(tokenOut, chain)
            )
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    @Throws
    private fun uniswapToken(token: Token?, chain: Chain) = when (val tokenType = token?.type) {
        TokenType.Native -> uniswapKit.etherToken(chain)
        is TokenType.Eip20 -> {
            uniswapKit.token(Address(tokenType.address), token.decimals)
        }

        else -> throw Exception("Invalid coin for swap: $token")
    }
}
