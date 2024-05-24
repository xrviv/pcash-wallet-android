package cash.p.terminal.modules.market.topcoins

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.modules.market.category.MarketItemWrapper
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await

class MarketTopCoinsService(
    private val marketTopMoversRepository: MarketTopMoversRepository,
    private val currencyManager: CurrencyManager,
    private val favoritesManager: MarketFavoritesManager,
    topMarket: TopMarket = TopMarket.Top100,
    sortingField: SortingField = SortingField.HighestCap,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var syncJob: Job? = null

    private var marketItems: List<MarketItem> = listOf()

    val stateObservable: BehaviorSubject<DataState<List<MarketItemWrapper>>> =
        BehaviorSubject.create()

    val periods = listOf(
        TimeDuration.OneDay,
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
        TimeDuration.ThreeMonths,
    )
    var period: TimeDuration = periods[0]
        private set

    val topMarkets = TopMarket.entries
    var topMarket: TopMarket = topMarket
        private set

    val sortingFields = listOf(
        SortingField.HighestCap,
        SortingField.LowestCap,
        SortingField.TopGainers,
        SortingField.TopLosers,
        )
    var sortingField: SortingField = sortingField
        private set

    fun setSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        sync()
    }

    fun setTopMarket(topMarket: TopMarket) {
        this.topMarket = topMarket
        sync()
    }

    fun setTimeDuration(period: TimeDuration) {
        this.period = period
        sync()
    }

    private fun sync() {
        syncJob?.cancel()
        syncJob = coroutineScope.launch {
            try {
                marketItems = marketTopMoversRepository.get(
                    topMarket.value,
                    sortingField,
                    topMarket.value,
                    currencyManager.baseCurrency,
                    period,
                ).await()

                syncItems()
            } catch (e: Throwable) {
                stateObservable.onNext(DataState.Error(e))
            }
        }
    }

    private fun syncItems() {
        val favorites = favoritesManager.getAll().map { it.coinUid }
        val items =
            marketItems.map { MarketItemWrapper(it, favorites.contains(it.fullCoin.coin.uid)) }
        stateObservable.onNext(DataState.Success(items))
    }

    fun start() {
        coroutineScope.launch {
            favoritesManager.dataUpdatedAsync.asFlow().collect {
                syncItems()
            }
        }

        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                sync()
            }
        }

        sync()
    }

    fun refresh() {
        sync()
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun addFavorite(coinUid: String) {
        favoritesManager.add(coinUid)
    }

    fun removeFavorite(coinUid: String) {
        favoritesManager.remove(coinUid)
    }
}
