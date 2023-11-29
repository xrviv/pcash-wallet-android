package cash.p.terminal.modules.market.topplatforms

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.sortedByDescendingNullLast
import cash.p.terminal.modules.market.sortedByNullLast
import io.horizontalsystems.marketkit.models.TopPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext

class TopPlatformsRepository(
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val apiTag: String,
) {
    private var itemsCache: List<TopPlatform>? = null

    suspend fun get(
        sortingField: SortingField,
        timeDuration: TimeDuration,
        forceRefresh: Boolean,
        limit: Int? = null,
    ) = withContext(Dispatchers.IO) {
        val currentCache = itemsCache

        val items = if (forceRefresh || currentCache == null) {
            marketKit.topPlatformsSingle(currencyManager.baseCurrency.code, apiTag).await()
        } else {
            currentCache
        }

        itemsCache = items

        val topPlatformsByPeriod = getTopPlatformItems(items, timeDuration)

        topPlatformsByPeriod.sort(sortingField).let { sortedList ->
            limit?.let { sortedList.take(it) } ?: sortedList
        }
    }

    companion object {
        fun getTopPlatformItems(
                topPlatforms: List<TopPlatform>,
                timeDuration: TimeDuration
        ): List<TopPlatformItem> {
            return topPlatforms.map { platform ->
                val prevRank = when (timeDuration) {
                    TimeDuration.OneDay -> platform.rank1D
                    TimeDuration.SevenDay -> platform.rank1W
                    TimeDuration.ThirtyDay -> platform.rank1M
                }

                val rankDiff = if (prevRank == platform.rank || prevRank == null) {
                    null
                } else {
                    prevRank - platform.rank
                }

                val marketCapDiff = when (timeDuration) {
                    TimeDuration.OneDay -> platform.change1D
                    TimeDuration.SevenDay -> platform.change1W
                    TimeDuration.ThirtyDay -> platform.change1M
                }

                TopPlatformItem(
                        Platform(platform.blockchain.uid, platform.blockchain.name),
                        platform.rank,
                        platform.protocols,
                        platform.marketCap,
                        rankDiff,
                        marketCapDiff
                )
            }
        }
    }

    fun List<TopPlatformItem>.sort(sortingField: SortingField) = when (sortingField) {
        SortingField.HighestCap -> sortedByDescendingNullLast { it.marketCap }
        SortingField.LowestCap -> sortedByNullLast { it.marketCap }
        SortingField.TopGainers -> sortedByDescendingNullLast { it.changeDiff }
        SortingField.TopLosers -> sortedByNullLast { it.changeDiff }
        else -> this
    }

}
