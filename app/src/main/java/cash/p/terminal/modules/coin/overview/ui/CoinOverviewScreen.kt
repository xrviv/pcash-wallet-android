package cash.p.terminal.modules.coin.overview.ui

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.alternativeImageUrl
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.slideFromBottomForResult
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.core.stats.StatEntity
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.modules.coin.CoinLink
import cash.p.terminal.modules.coin.overview.CoinOverviewModule
import cash.p.terminal.modules.coin.overview.CoinOverviewViewModel
import cash.p.terminal.modules.coin.overview.HudMessageType
import cash.p.terminal.modules.coin.ui.CoinScreenTitle
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import cash.p.terminal.modules.managewallets.ManageWalletsModule
import cash.p.terminal.modules.managewallets.ManageWalletsViewModel
import cash.p.terminal.modules.markdown.MarkdownFragment
import cash.p.terminal.modules.zcashconfigure.ZcashConfigure
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.ButtonSecondaryDefault
import cash.p.terminal.ui.compose.components.CellFooter
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.LinkType

@Composable
fun CoinOverviewScreen(
    fullCoin: FullCoin,
    navController: NavController
) {
    val vmFactory by lazy { CoinOverviewModule.Factory(fullCoin) }
    val viewModel = viewModel<CoinOverviewViewModel>(factory = vmFactory)
    val chartViewModel = viewModel<ChartViewModel>(factory = vmFactory)

    val refreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val overview by viewModel.overviewLiveData.observeAsState()
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val chartIndicatorsState = viewModel.chartIndicatorsState

    val view = LocalView.current
    val context = LocalContext.current

    viewModel.showHudMessage?.let {
        when (it.type) {
            HudMessageType.Error -> HudHelper.showErrorMessage(
                contenView = view,
                resId = it.text,
                icon = it.iconRes,
                iconTint = R.color.white
            )

            HudMessageType.Success -> HudHelper.showSuccessMessage(
                contenView = view,
                resId = it.text,
                icon = it.iconRes,
                iconTint = R.color.white
            )
        }

        viewModel.onHudMessageShown()
    }

    val vmFactory1 = remember { ManageWalletsModule.Factory() }
    val manageWalletsViewModel = viewModel<ManageWalletsViewModel>(factory = vmFactory1)
    val restoreSettingsViewModel = viewModel<RestoreSettingsViewModel>(factory = vmFactory1)

    if (restoreSettingsViewModel.openZcashConfigure != null) {
        restoreSettingsViewModel.zcashConfigureOpened()

        navController.slideFromBottomForResult<ZcashConfigure.Result>(R.id.zcashConfigure) {
            if (it.config != null) {
                restoreSettingsViewModel.onEnter(it.config)
            } else {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }
        }
    }


    HSSwipeRefresh(
        refreshing = refreshing,
        onRefresh = {
            viewModel.refresh()
            chartViewModel.refresh()
        },
        content = {
            Crossfade(viewState, label = "") { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        Loading()
                    }
                    ViewState.Success -> {
                        overview?.let { overview ->
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                CoinScreenTitle(
                                    fullCoin.coin.name,
                                    overview.marketCapRank,
                                    fullCoin.coin.imageUrl,
                                    fullCoin.coin.alternativeImageUrl,
                                    fullCoin.iconPlaceholder
                                )

                                Chart(chartViewModel = chartViewModel)

                                Spacer(modifier = Modifier.height(12.dp))

                                CellUniversalLawrenceSection {
                                    RowUniversal(
                                        modifier = Modifier
                                            .height(52.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                    ) {
                                        subhead2_grey(text = stringResource(R.string.CoinPage_Indicators))
                                        Spacer(modifier = Modifier.weight(1f))

                                        if (chartIndicatorsState.hasActiveSubscription) {
                                            if (chartIndicatorsState.enabled) {
                                                ButtonSecondaryDefault(
                                                    title = stringResource(id = R.string.Button_Hide),
                                                    onClick = {
                                                        viewModel.disableChartIndicators()

                                                        stat(page = StatPage.CoinOverview, event = StatEvent.ToggleIndicators(false))
                                                    }
                                                )
                                            } else {
                                                ButtonSecondaryDefault(
                                                    title = stringResource(id = R.string.Button_Show),
                                                    onClick = {
                                                        viewModel.enableChartIndicators()

                                                        stat(page = StatPage.CoinOverview, event = StatEvent.ToggleIndicators(true))
                                                    }
                                                )
                                            }
                                            HSpacer(width = 8.dp)
                                            ButtonSecondaryCircle(
                                                icon = R.drawable.ic_setting_20
                                            ) {
                                                navController.slideFromRight(R.id.indicatorsFragment)

                                                stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.Indicators))
                                            }
                                        }
                                    }
                                }

                                if (overview.marketData.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MarketData(overview.marketData)
                                }

                                if (overview.roi.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Roi(overview.roi)
                                }

                                viewModel.tokenVariants?.let { tokenVariants ->
                                    Spacer(modifier = Modifier.height(24.dp))
                                    TokenVariants(
                                        tokenVariants = tokenVariants,
                                        onClickAddToWallet = {
                                            manageWalletsViewModel.enable(it)

                                            stat(page = StatPage.CoinOverview, event = StatEvent.AddToWallet)
                                        },
                                        onClickRemoveWallet = {
                                            manageWalletsViewModel.disable(it)

                                            stat(page = StatPage.CoinOverview, event = StatEvent.RemoveFromWallet)
                                        },
                                        onClickCopy = {
                                            TextHelper.copyText(it)
                                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)

                                            stat(page = StatPage.CoinOverview, event = StatEvent.Copy(StatEntity.ContractAddress))
                                        },
                                        onClickExplorer = {
                                            LinkHelper.openLinkInAppBrowser(context, it)

                                            stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.ExternalBlockExplorer))
                                        },
                                    )
                                }

                                if (overview.about.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    About(overview.about)
                                }

                                if (overview.links.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Links(overview.links) { onClick(it, context, navController) }
                                }

                                Spacer(modifier = Modifier.height(32.dp))
                                CellFooter(text = stringResource(id = R.string.Market_PoweredByApi))
                            }
                        }

                    }
                    is ViewState.Error -> {
                        ListErrorView(stringResource(id = R.string.BalanceSyncError_Title)) {
                            viewModel.retry()
                            chartViewModel.refresh()
                        }
                    }
                    null -> {}
                }
            }
        },
    )
}

private fun onClick(coinLink: CoinLink, context: Context, navController: NavController) {
    val absoluteUrl = getAbsoluteUrl(coinLink)

    when (coinLink.linkType) {
        LinkType.Guide -> {
            navController.slideFromRight(
                R.id.markdownFragment,
                MarkdownFragment.Input(absoluteUrl, true)
            )
        }
        else -> LinkHelper.openLinkInAppBrowser(context, absoluteUrl)
    }

    when(coinLink.linkType) {
        LinkType.Guide -> stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.Guide))
        LinkType.Website -> stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.ExternalCoinWebsite))
        LinkType.Whitepaper -> stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.ExternalCoinWhitePaper))
        LinkType.Twitter -> stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.ExternalTwitter))
        LinkType.Telegram -> stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.ExternalTelegram))
        LinkType.Reddit -> stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.ExternalReddit))
        LinkType.Github -> stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.ExternalGithub))
    }
}

private fun getAbsoluteUrl(coinLink: CoinLink) = when (coinLink.linkType) {
    LinkType.Twitter -> "https://twitter.com/${coinLink.url}"
    LinkType.Telegram -> "https://t.me/${coinLink.url}"
    else -> coinLink.url
}

@Preview
@Composable
fun LoadingPreview() {
    ComposeAppTheme {
        Loading()
    }
}

@Composable
fun Error(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        subhead2_grey(text = message)
    }
}

@Composable
fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 2.dp
        )
    }
}
