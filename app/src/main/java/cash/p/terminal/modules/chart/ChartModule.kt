package cash.p.terminal.modules.chart

import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.market.Value
import java.math.BigDecimal

object ChartModule {

    fun createViewModel(
        chartService: AbstractChartService,
        chartNumberFormatter: ChartNumberFormatter
    ): ChartViewModel {
        return ChartViewModel(chartService, chartNumberFormatter)
    }

    interface ChartNumberFormatter {
        fun formatValue(currency: Currency, value: BigDecimal): String
    }

    sealed class ChartHeaderView {
        abstract val value: String

        data class Latest(override val value: String, val diff: Value.Percent) : ChartHeaderView()
        data class Sum(override val value: String) : ChartHeaderView()
    }

}
