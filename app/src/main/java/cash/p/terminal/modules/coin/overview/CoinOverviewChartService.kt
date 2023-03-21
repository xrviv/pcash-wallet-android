package cash.p.terminal.modules.coin.overview

import android.util.Log
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.chart.AbstractChartService
import cash.p.terminal.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.ChartViewType
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
    override val hasVolumes = true
    override val initialChartInterval = HsTimePeriod.Day1

    override var chartIntervals = listOf<HsTimePeriod?>()
    override val chartViewType = ChartViewType.Line

    private var updatesSubscriptionKey: String? = null
    private val disposables = CompositeDisposable()

    private var chartStartTime: Long = 0
    private val cache = mutableMapOf<String, ChartInfo>()

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
        val newKey = currency.code
        if (forceRefresh || newKey != updatesSubscriptionKey) {
            unsubscribeFromUpdates()
            subscribeForUpdates(currency)
            updatesSubscriptionKey = newKey
        }

        return chartInfoCached(currency, periodType)
            .map {
                val tmpLastCoinPrice = marketKit.coinPrice(coinUid, currency.code)

                doGetItems(it, tmpLastCoinPrice, chartInterval)
            }
    }

    private fun chartInfoCached(
        currency: Currency,
        periodType: HsPeriodType
    ): Single<ChartInfo> {
        val cacheKey = currency.code + periodType.serialize()
        val cached = cache[cacheKey]
        return if (cached != null) {
            Single.just(cached)
        } else {
            marketKit.chartInfoSingle(coinUid, currency.code, periodType)
                .doOnSuccess {
                    cache[cacheKey] = it
                }
        }
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
    }

    private fun subscribeForUpdates(currency: Currency) {
        marketKit.coinPriceObservable(coinUid, currency.code)
            .subscribeIO {
                dataInvalidated()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun doGetItems(
        chartInfo: ChartInfo,
        lastCoinPrice: CoinPrice?,
        chartInterval: HsTimePeriod?
    ): ChartPointsWrapper {
        val points = chartInfo.points
        if (lastCoinPrice == null || points.isEmpty()) return ChartPointsWrapper(listOf())

        val items = points
            .map { chartPoint ->
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

        return ChartPointsWrapper(items, chartInfo.startTimestamp, chartInfo.endTimestamp, chartInfo.isExpired)
    }

}
