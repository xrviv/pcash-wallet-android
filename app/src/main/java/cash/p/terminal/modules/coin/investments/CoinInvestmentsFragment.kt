package cash.p.terminal.modules.coin.investments

import android.os.Parcelable
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.requireInput
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.investments.CoinInvestmentsModule.FundViewItem
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellSingleLineClear
import cash.p.terminal.ui.compose.components.CellSingleLineLawrenceSection
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.body_jacob
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead1_grey
import cash.p.terminal.ui.compose.components.subhead2_remus
import cash.p.terminal.ui.helpers.LinkHelper
import kotlinx.parcelize.Parcelize

class CoinInvestmentsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()
        CoinInvestmentsScreen(
            viewModel = viewModel(
                factory = CoinInvestmentsModule.Factory(input.coinUid)
            ),
            onClickNavigation = {
                navController.popBackStack()
            },
            onClickFundUrl = {
                LinkHelper.openLinkInAppBrowser(requireContext(), it)
            }
        )
    }

    @Parcelize
    data class Input(val coinUid: String) : Parcelable
}

@Composable
private fun CoinInvestmentsScreen(
    viewModel: CoinInvestmentsViewModel,
    onClickNavigation: () -> Unit,
    onClickFundUrl: (url: String) -> Unit
) {
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewItems by viewModel.viewItemsLiveData.observeAsState()

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(R.string.CoinPage_FundsInvested),
            navigationIcon = {
                HsBackButton(onClick = onClickNavigation)
            }
        )

        HSSwipeRefresh(
            refreshing = isRefreshing,
            onRefresh = viewModel::refresh
        ) {
            Crossfade(viewState) { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        Loading()
                    }

                    is ViewState.Error -> {
                        ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                    }

                    ViewState.Success -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            viewItems?.forEach { viewItem ->
                                item {
                                    CoinInvestmentHeader(viewItem.amount, viewItem.info)

                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                item {
                                    CellSingleLineLawrenceSection(viewItem.fundViewItems) { fundViewItem ->
                                        CoinInvestmentFund(fundViewItem) { onClickFundUrl(fundViewItem.url) }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }

                    null -> {}
                }
            }
        }
    }
}

@Composable
fun CoinInvestmentHeader(amount: String, info: String) {
    CellSingleLineClear(borderTop = true) {
        body_jacob(
            modifier = Modifier.weight(1f),
            text = amount,
        )

        subhead1_grey(text = info)
    }
}

@Composable
fun CoinInvestmentFund(fundViewItem: FundViewItem, onClick: () -> Unit) {
    val hasWebsiteUrl = fundViewItem.url.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick, enabled = hasWebsiteUrl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinImage(
            iconUrl = fundViewItem.logoUrl,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(24.dp)
        )
        body_leah(
            modifier = Modifier.weight(1f),
            text = fundViewItem.name,
            overflow = TextOverflow.Ellipsis
        )
        if (fundViewItem.isLead) {
            subhead2_remus(
                modifier = Modifier.padding(horizontal = 6.dp),
                text = stringResource(R.string.CoinPage_CoinInvestments_Lead),
            )
        }
        if (hasWebsiteUrl) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "arrow icon"
            )
        }
        Spacer(Modifier.width(16.dp))
    }
}
