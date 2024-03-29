package cash.p.terminal.modules.market.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.market.MarketModule
import cash.p.terminal.modules.market.MarketViewItem
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.modules.market.overview.MarketOverviewModule
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.WithTranslatableTitle
import cash.p.terminal.ui.compose.components.ButtonSecondaryToggle
import cash.p.terminal.ui.compose.components.MarketCoinClear

@Composable
fun BoardsView(
    boards: List<MarketOverviewModule.Board>,
    navController: NavController,
    onClickSeeAll: (MarketModule.ListType) -> Unit,
    onSelectTopMarket: (TopMarket, MarketModule.ListType) -> Unit
) {
    val onItemClick: (MarketViewItem) -> Unit = remember {
        {
            navController.slideFromRight(
                R.id.coinFragment,
                CoinFragment.Input(it.coinUid, "market_overview")
            )
        }
    }

    boards.forEach { boardItem ->
        TopBoardHeader(
            title = boardItem.boardHeader.title,
            iconRes = boardItem.boardHeader.iconRes,
            select = boardItem.boardHeader.topMarketSelect,
            onSelect = { topMarket -> onSelectTopMarket(topMarket, boardItem.type) },
            onClickSeeAll = { onClickSeeAll(boardItem.type) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ){
            boardItem.marketViewItems.forEach { coin ->
                MarketCoinWithBackground(coin) { onItemClick.invoke(coin) }
            }

            SeeAllButton { onClickSeeAll(boardItem.type) }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T : WithTranslatableTitle> TopBoardHeader(
    title: Int,
    iconRes: Int,
    select: Select<T>,
    onSelect: (T) -> Unit,
    onClickSeeAll: () -> Unit
) {
    MarketsSectionHeader(
        title = title,
        onClick = onClickSeeAll,
        icon = painterResource(iconRes)
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            ButtonSecondaryToggle(
                select = select,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun MarketCoinWithBackground(
    marketViewItem: MarketViewItem,
    onClick: () -> Unit
) {
    MarketCoinClear(
        marketViewItem.coinName,
        marketViewItem.coinCode,
        marketViewItem.iconUrl,
        marketViewItem.iconPlaceHolder,
        marketViewItem.coinRate,
        marketViewItem.marketDataValue,
        marketViewItem.rank,
        onClick
    )
}
