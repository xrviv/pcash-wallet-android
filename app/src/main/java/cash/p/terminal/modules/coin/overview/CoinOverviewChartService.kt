package cash.p.terminal.modules.coin.overview

import android.util.Log
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.chart.AbstractChartService
import cash.p.terminal.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.rx2.await
import java.io.IOException

class CoinOverviewChartService(
    private val marketKit: MarketKitWrapper,
    override val currencyManager: CurrencyManager,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Day1

    override var chartIntervals = listOf<HsTimePeriod?>()

    private var updatesSubscriptionKey: String? = null
    private val disposables = CompositeDisposable()

    private var chartStartTime: Long = 0

    override suspend fun start() {
        try {
            chartStartTime = marketKit.chartStartTimeSingle(coinUid).await()
        } catch (e: IOException) {
            Log.e("CoinOverviewChartService", "start error: ", e)
        }

        val now = System.currentTimeMillis() / 1000L
        val mostPeriodSeconds = now - chartStartTime

        chartIntervals = HsTimePeriod.values().filter {
            it.range <= mostPeriodSeconds
        } + listOf<HsTimePeriod?>(null)

        super.start()
    }

    override fun stop() {
        super.stop()
        unsubscribeFromUpdates()
    }

    override fun getAllItems(currency: Currency): Single<ChartPointsWrapper> {
        return getItemsByPeriodType(
            currency = currency,
            periodType = HsPeriodType.ByStartTime(chartStartTime),
            chartInterval = null
        )
    }

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): Single<ChartPointsWrapper> {
        return getItemsByPeriodType(
            currency = currency,
            periodType = HsPeriodType.ByPeriod(chartInterval),
            chartInterval = chartInterval
        )
    }

    private fun getItemsByPeriodType(
        currency: Currency,
        periodType: HsPeriodType,
        chartInterval: HsTimePeriod?
    ): Single<ChartPointsWrapper> {
        val newKey = (chartInterval?.name ?: "All") + currency.code
        if (forceRefresh || newKey != updatesSubscriptionKey) {
            unsubscribeFromUpdates()
            subscribeForUpdates(currency, periodType)
            updatesSubscriptionKey = newKey
        }

        val tmpChartInfo = marketKit.chartInfo(coinUid, currency.code, periodType)
        val tmpLastCoinPrice = marketKit.coinPrice(coinUid, currency.code)

        return Single.just(doGetItems(tmpChartInfo, tmpLastCoinPrice, chartInterval))
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
    }

    private fun subscribeForUpdates(currency: Currency, periodType: HsPeriodType) {
        marketKit.coinPriceObservable(coinUid, currency.code)
            .subscribeIO {
                dataInvalidated()
            }
            .let {
                disposables.add(it)
            }

        marketKit.getChartInfoAsync(coinUid, currency.code, periodType)
            .subscribeIO {
                dataInvalidated()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun doGetItems(
        chartInfo: ChartInfo?,
        lastCoinPrice: CoinPrice?,
        chartInterval: HsTimePeriod?
    ): ChartPointsWrapper {
        if (chartInfo == null || lastCoinPrice == null) return ChartPointsWrapper(chartInterval, listOf())
        val points = chartInfo.points
        if (points.isEmpty()) return ChartPointsWrapper(chartInterval, listOf())

        val items = points
            .mapIndexed { index, chartPoint ->

                ChartPoint(
                    value = chartPoint.value.toFloat(),
                    timestamp = chartPoint.timestamp,
                    indicators = mapOf(
                        Indicator.Volume to chartPoint.extra[ChartPointType.Volume]?.toFloat(),
                    )
                )
            }
            .toMutableList()

        if (lastCoinPrice.timestamp > items.last().timestamp) {
            items.add(ChartPoint(lastCoinPrice.value.toFloat(), timestamp = lastCoinPrice.timestamp))

            if (chartInterval == HsTimePeriod.Day1) {
                val startTimestamp = lastCoinPrice.timestamp - 24 * 60 * 60
                val diff = lastCoinPrice.diff
                if (diff == null) {
                    items.removeIf { it.timestamp < startTimestamp }
                } else {
                    items.removeIf { it.timestamp <= startTimestamp }

                    val startValue = (lastCoinPrice.value * 100.toBigDecimal()) / (diff + 100.toBigDecimal())
                    val startItem = ChartPoint(startValue.toFloat(), startTimestamp)

                    items.add(0, startItem)
                }
            }
        }

        items.removeIf { it.timestamp < chartInfo.startTimestamp }

        return ChartPointsWrapper(chartInterval, items, chartInfo.startTimestamp, chartInfo.endTimestamp, chartInfo.isExpired)
    }

}
