package cash.p.terminal.modules.coin.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.modules.chart.ChartCurrencyValueFormatterSignificant
import cash.p.terminal.modules.chart.ChartModule
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.modules.coin.*
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketInfoOverview

object CoinOverviewModule {

    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return when (modelClass) {
                CoinOverviewViewModel::class.java -> {
                    val currency = App.currencyManager.baseCurrency
                    val service = CoinOverviewService(
                        fullCoin,
                        App.marketKit,
                        App.currencyManager,
                        App.appConfigProvider,
                        App.languageManager
                    )

                    CoinOverviewViewModel(
                        service,
                        CoinViewFactory(currency, App.numberFormatter),
                        App.walletManager,
                        App.accountManager
                    ) as T
                }
                ChartViewModel::class.java -> {
                    val chartService = CoinOverviewChartService(App.marketKit, App.currencyManager, fullCoin.coin.uid)
                    val chartNumberFormatter = ChartCurrencyValueFormatterSignificant()
                    ChartModule.createViewModel(chartService, chartNumberFormatter) as T
                }
                else -> throw IllegalArgumentException()
            }
        }

    }
}

data class CoinOverviewItem(
    val coinCode: String,
    val marketInfoOverview: MarketInfoOverview,
    val guideUrl: String?,
)

data class TokenVariant(
    val value: String,
    val copyValue: String?,
    val imgUrl: String,
    val explorerUrl: String?,
    val name: String?,
    val configuredToken: ConfiguredToken,
    val canAddToWallet: Boolean,
    val inWallet: Boolean,
) {
}

data class CoinOverviewViewItem(
    val roi: List<RoiViewItem>,
    val categories: List<String>,
    val links: List<CoinLink>,
    val about: String,
    val marketData: MutableList<CoinDataItem>
)