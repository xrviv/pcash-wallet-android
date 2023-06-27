package cash.p.terminal.modules.balance.cex

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.balance.AccountViewItem
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.ui.BalanceSortingSelector
import cash.p.terminal.modules.balance.ui.BalanceTitleRow
import cash.p.terminal.modules.balance.ui.TotalBalanceRow
import cash.p.terminal.modules.balance.ui.Wallets
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.depositcex.DepositCexFragment
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun BalanceForAccountCex(navController: NavController, accountViewItem: AccountViewItem) {
    val viewModel = viewModel<BalanceCexViewModel>(factory = BalanceModule.FactoryCex())
    val uiState = viewModel.uiState
    val totalState = viewModel.totalUiState

    val context = LocalContext.current

    val activeScreen = uiState.isActiveScreen
    if (activeScreen) {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = {
                        BalanceTitleRow(navController, accountViewItem.name)
                    }
                )
            }
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues)) {

                TotalBalanceRow(
                    totalState = totalState,
                    onClickTitle = {
                        viewModel.toggleBalanceVisibility()
                        HudHelper.vibrate(context)
                    },
                    onClickSubtitle = {
                        viewModel.toggleTotalType()
                        HudHelper.vibrate(context)
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 16.dp)
                ) {
                    ButtonPrimaryYellow(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.Balance_Withdraw),
                        onClick = {
                            navController.slideFromBottom(R.id.withdrawCexFragment)
                        },
                    )

                    HSpacer(width = 8.dp)

                    ButtonPrimaryDefault(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.Balance_Deposit),
                        onClick = {
                            navController.slideFromRight(R.id.depositCexFragmentChooseAsset)
                        }
                    )
                }

                HeaderSorting(borderTop = true) {
                    BalanceSortingSelector(
                        sortType = uiState.sortType,
                        sortTypes = viewModel.sortTypes,
                        onSelectSortType = viewModel::onSelectSortType
                    )
                }

                Wallets(
                    items = uiState.viewItems,
                    key = { it.assetId },
                    accountId = accountViewItem.id,
                    sortType = uiState.sortType,
                    refreshing = uiState.isRefreshing,
                    onRefresh = viewModel::onRefresh
                ) { item ->
                    BalanceCardCex(navController, item) {
                        viewModel.onClickItem(item)
                    }
                }
            }
        }
    }
}


@Composable
fun BalanceCardCex(
    navController: NavController,
    viewItem: BalanceCexViewItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        CellMultilineClear(height = 64.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WalletIconCex(viewItem.coinIconUrl, viewItem.coinIconPlaceholder)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(weight = 1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            body_leah(
                                text = viewItem.coinCode,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!viewItem.badge.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ComposeAppTheme.colors.jeremy)
                                ) {
                                    Text(
                                        modifier = Modifier.padding(
                                            start = 4.dp,
                                            end = 4.dp,
                                            bottom = 1.dp
                                        ),
                                        text = viewItem.badge,
                                        color = ComposeAppTheme.colors.bran,
                                        style = ComposeAppTheme.typography.microSB,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(24.dp))
                        if (viewItem.primaryValue.visible) {
                            Text(
                                text = viewItem.primaryValue.value,
                                color = if (viewItem.primaryValue.dimmed) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.leah,
                                style = ComposeAppTheme.typography.headline2,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                        ) {
                            if (viewItem.exchangeValue.visible) {
                                Row {
                                    Text(
                                        text = viewItem.exchangeValue.value,
                                        color = if (viewItem.exchangeValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                                        style = ComposeAppTheme.typography.subhead2,
                                        maxLines = 1,
                                    )
                                    Text(
                                        modifier = Modifier.padding(start = 4.dp),
                                        text = RateText(viewItem.diff),
                                        color = RateColor(viewItem.diff),
                                        style = ComposeAppTheme.typography.subhead2,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.padding(start = 16.dp),
                        ) {
                            if (viewItem.secondaryValue.visible) {
                                Text(
                                    text = viewItem.secondaryValue.value,
                                    color = if (viewItem.secondaryValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                                    style = ComposeAppTheme.typography.subhead2,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        ExpandableContentCex(viewItem, navController)
    }
}


@Composable
private fun WalletIconCex(iconUrl: String?, placeholder: Int) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        CoinImage(
            iconUrl = iconUrl,
            placeholder = placeholder,
            modifier = Modifier
                .size(32.dp)
        )
    }
}


@Composable
private fun ExpandableContentCex(
    viewItem: BalanceCexViewItem,
    navController: NavController
) {
    AnimatedVisibility(
        visible = viewItem.expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            Divider(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 6.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            ButtonsRowCex(viewItem, navController)
        }
    }
}


@Composable
private fun ButtonsRowCex(viewItem: BalanceCexViewItem, navController: NavController) {

    Row(
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.Balance_Withdraw),
            enabled = viewItem.withdrawEnabled,
            onClick = {
                navController.slideFromBottom(R.id.withdrawCexFragment)
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        ButtonPrimaryDefault(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.Balance_Deposit),
            enabled = viewItem.depositEnabled,
            onClick = {
                navController.slideFromRight(R.id.depositCexFragment, DepositCexFragment.args(viewItem.cexAsset))
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        ButtonPrimaryCircle(
            icon = R.drawable.ic_chart_24,
            contentDescription = stringResource(R.string.Coin_Info),
            enabled = viewItem.coinUid != null,
            onClick = {
                viewItem.coinUid?.let { coinUid ->
                    navController.slideFromRight(
                        R.id.coinFragment,
                        CoinFragment.prepareParams(coinUid)
                    )
                }
            },
        )
    }
}

