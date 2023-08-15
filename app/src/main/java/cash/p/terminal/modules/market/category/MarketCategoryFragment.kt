package cash.p.terminal.modules.market.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.coin.overview.ui.Chart
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.topcoins.SelectorDialogState
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable

class MarketCategoryFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val factory = MarketCategoryModule.Factory(
            arguments?.parcelable(categoryKey)!!
        )

        val chartViewModel by viewModels<ChartViewModel> { factory }

        val viewModel by viewModels<MarketCategoryViewModel> { factory }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    CategoryScreen(
                        viewModel,
                        chartViewModel,
                        { findNavController().popBackStack() },
                        { coinUid -> onCoinClick(coinUid) }
                    )
                }
            }
        }
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)

        findNavController().slideFromRight(R.id.coinFragment, arguments)
    }

    companion object {
        const val categoryKey = "coin_category"
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(
    viewModel: MarketCategoryViewModel,
    chartViewModel: ChartViewModel,
    onCloseButtonClick: () -> Unit,
    onCoinClick: (String) -> Unit,
) {
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    val viewItemState by viewModel.viewStateLiveData.observeAsState(ViewState.Loading)
    val viewItems by viewModel.viewItemsLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val selectorDialogState by viewModel.selectorDialogStateLiveData.observeAsState()

    val interactionSource = remember { MutableInteractionSource() }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(interactionSource, onCloseButtonClick)

            HSSwipeRefresh(
                refreshing = isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                Crossfade(viewItemState) { state ->
                    when (state) {
                        ViewState.Loading -> {
                            Loading()
                        }
                        is ViewState.Error -> {
                            ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                        }
                        ViewState.Success -> {
                            viewItems?.let {
                                val header by viewModel.headerLiveData.observeAsState()
                                val menu by viewModel.menuLiveData.observeAsState()

                                CoinList(
                                    items = it,
                                    scrollToTop = scrollToTopAfterUpdate,
                                    onAddFavorite = { uid -> viewModel.onAddFavorite(uid) },
                                    onRemoveFavorite = { uid -> viewModel.onRemoveFavorite(uid) },
                                    onCoinClick = onCoinClick,
                                    preItems = {
                                        header?.let {
                                            item {
                                                DescriptionCard(it.title, it.description, it.icon)
                                            }
                                        }
                                        item {
                                            Chart(chartViewModel = chartViewModel)
                                        }
                                        menu?.let {
                                            stickyHeader {
                                                HeaderSorting(borderTop = true, borderBottom = true) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        SortMenu(
                                                            it.sortingFieldSelect.selected.titleResId,
                                                            viewModel::showSelectorMenu
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier.padding(
                                                            start = 8.dp,
                                                            end = 16.dp
                                                        )
                                                    ) {
                                                        ButtonSecondaryToggle(
                                                            select = it.marketFieldSelect,
                                                            onSelect = viewModel::onSelectMarketField
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                                if (scrollToTopAfterUpdate) {
                                    scrollToTopAfterUpdate = false
                                }
                            }
                        }
                    }
                }
            }
        }
        //Dialog
        when (val option = selectorDialogState) {
            is SelectorDialogState.Opened -> {
                AlertGroup(
                    R.string.Market_Sort_PopupTitle,
                    option.select,
                    { selected ->
                        scrollToTopAfterUpdate = true
                        viewModel.onSelectSortingField(selected)
                    },
                    { viewModel.onSelectorDialogDismiss() }
                )
            }
            SelectorDialogState.Closed,
            null -> {}
        }
    }
}

