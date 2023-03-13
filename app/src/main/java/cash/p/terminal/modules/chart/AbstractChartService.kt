package cash.p.terminal.modules.chart

import androidx.annotation.CallSuper
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Currency
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

abstract class AbstractChartService {
    abstract val chartIntervals: List<HsTimePeriod?>

    protected abstract val currencyManager: CurrencyManager
    protected abstract val initialChartInterval: HsTimePeriod
    protected open fun getAllItems(currency: Currency): Single<ChartPointsWrapper> {
        return Single.error(Exception("Not Implemented"))
    }
    protected abstract fun getItems(chartInterval: HsTimePeriod, currency: Currency): Single<ChartPointsWrapper>

    protected var chartInterval: HsTimePeriod? = null
        set(value) {
            field = value
            chartTypeObservable.onNext(Optional.ofNullable(value))
        }

    val currency: Currency
        get() = currencyManager.baseCurrency
    val chartTypeObservable = BehaviorSubject.create<Optional<HsTimePeriod>>()

    val chartPointsWrapperObservable = BehaviorSubject.create<Result<ChartPointsWrapper>>()

    private var fetchItemsDisposable: Disposable? = null
    private val disposables = CompositeDisposable()
    protected var forceRefresh = false

    open suspend fun start() {
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                fetchItems()
            }
            .let {
                disposables.add(it)
            }

        chartInterval = initialChartInterval
        fetchItems()
    }

    protected fun dataInvalidated() {
        fetchItems()
    }

    open fun stop() {
        disposables.clear()
        fetchItemsDisposable?.dispose()
    }

    @CallSuper
    open fun updateChartInterval(chartInterval: HsTimePeriod?) {
        this.chartInterval = chartInterval

        fetchItems()
    }

    fun refresh() {
        forceRefresh = true
        fetchItems()
        forceRefresh = false
    }

    @Synchronized
    private fun fetchItems() {
        val tmpChartInterval = chartInterval
        val itemsSingle = when {
            tmpChartInterval == null -> getAllItems(currency)
            else -> getItems(tmpChartInterval, currency)
        }

        fetchItemsDisposable?.dispose()
        fetchItemsDisposable = itemsSingle
            .subscribeIO({
                chartPointsWrapperObservable.onNext(Result.success(it))
            }, {
                chartPointsWrapperObservable.onNext(Result.failure(it))
            })
    }

}

