package cash.p.terminal.modules.swap

import cash.p.terminal.R
import cash.p.terminal.core.IAppNumberFormatter
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.modules.swap.SwapXMainModule.PriceImpactLevel
import cash.p.terminal.modules.swap.SwapXMainModule.PriceImpactViewItem
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeOptions
import java.math.BigDecimal

class SwapViewItemHelper(private val numberFormatter: IAppNumberFormatter) {

    fun prices(sellPrice: BigDecimal, buyPrice: BigDecimal, tokenFrom: Token?, tokenTo: Token?): Pair<String?, String?> {
        val primaryPrice: String?
        val secondaryPrice: String?

        val sellPriceStr = price(sellPrice, tokenTo, tokenFrom)
        val buyPriceStr  = price(buyPrice, tokenFrom, tokenTo)
        if (sellPrice > buyPrice) {
            primaryPrice = sellPriceStr
            secondaryPrice = buyPriceStr
        } else {
            primaryPrice = buyPriceStr
            secondaryPrice = sellPriceStr
        }
        return Pair(primaryPrice, secondaryPrice)
    }

    private fun price(price: BigDecimal?, quoteToken: Token?, baseToken: Token?): String? {
        if (price == null || quoteToken == null || baseToken == null)
            return null

        return "1 ${baseToken.coin.code} = ${coinAmount(price, quoteToken.coin.code)}"
    }

    fun priceImpactViewItem(
        trade: SwapXMainModule.SwapData.UniswapData,
        minLevel: PriceImpactLevel = PriceImpactLevel.Normal
    ): PriceImpactViewItem? {

        val priceImpact = trade.data.priceImpact ?: return null
        val impactLevel = trade.priceImpactLevel ?: return null
        if (impactLevel < minLevel) {
            return null
        }

        return PriceImpactViewItem(impactLevel, Translator.getString(R.string.Swap_Percent, priceImpact))
    }

    fun slippage(allowedSlippage: BigDecimal): String? {
        val defaultTradeOptions = TradeOptions()
        return if (allowedSlippage.compareTo(defaultTradeOptions.allowedSlippagePercent) == 0) {
            null
        } else {
            "$allowedSlippage%"
        }
    }

    fun deadline(ttl: Long): String? {
        val defaultTradeOptions = TradeOptions()
        return if (ttl == defaultTradeOptions.ttl) {
            null
        } else {
            Translator.getString(R.string.Duration_Minutes, ttl / 60)
        }
    }

    fun coinAmount(amount: BigDecimal, coinCode: String): String {
        return numberFormatter.formatCoinFull(amount, coinCode, 8)
    }

}
