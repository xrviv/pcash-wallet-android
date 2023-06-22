package cash.p.terminal.modules.depositcex

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SelectCoinScreen(
    depositViewModel: DepositViewModel,
    onClose: () -> Unit,
    openNetworkSelect: (String) -> Unit,
) {
    val uiState = depositViewModel.uiState

    BackHandler {
        onClose.invoke()
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                SearchBar(
                    title = stringResource(R.string.Cex_ChooseCoin),
                    searchHintText = stringResource(R.string.ManageCoins_Search),
                    onClose = onClose,
                    onSearchTextChanged = { text ->
                        //viewModel.updateFilter(text)
                    }
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                Crossfade(uiState.loading) {
                    if (it) {
                        Loading()
                    } else {
                        uiState.coins?.let { viewItems ->
                            //todo add loading state
                            if (viewItems.isEmpty()) {
                                ListEmptyView(
                                    text = stringResource(R.string.EmptyResults),
                                    icon = R.drawable.ic_not_found
                                )
                            } else {
                                LazyColumn {
                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider(
                                            thickness = 1.dp,
                                            color = ComposeAppTheme.colors.steel10,
                                        )
                                    }
                                    items(viewItems) { viewItem ->
                                        CoinCell(
                                            viewItem = viewItem,
                                            onItemClick = {
                                                depositViewModel.onSelectAsset(viewItem.assetId)
                                                openNetworkSelect.invoke(viewItem.assetId)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinCell(
    viewItem: DepositCexModule.CexCoinViewItem,
    onItemClick: () -> Unit,
) {
    Column {
        RowUniversal(
            onClick = onItemClick,
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalPadding = 0.dp
        ) {
            CoinImage(
                iconUrl = viewItem.coinIconUrl,
                placeholder = viewItem.coinIconPlaceholder,
                modifier = Modifier
                    .padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                    .size(32.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    body_leah(
                        text = viewItem.title,
                        maxLines = 1,
                    )
                }
                subhead2_grey(
                    text = viewItem.subtitle,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )
    }
}