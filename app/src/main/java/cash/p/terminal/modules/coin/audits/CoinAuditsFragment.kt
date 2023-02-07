package cash.p.terminal.modules.coin.audits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import cash.p.terminal.ui.helpers.LinkHelper

class CoinAuditsFragment : BaseFragment() {

    private val viewModel by viewModels<CoinAuditsViewModel> {
        CoinAuditsModule.Factory(requireArguments().getStringArrayList(ADDRESSES_KEY)!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    CoinAuditsScreen(viewModel,
                        onClickNavigation = {
                            findNavController().popBackStack()
                        },
                        onClickReportUrl = {
                            LinkHelper.openLinkInAppBrowser(requireContext(), it)
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val ADDRESSES_KEY = "addresses_key"

        fun prepareParams(addresses: List<String>) = bundleOf(ADDRESSES_KEY to addresses)
    }
}

@Composable
private fun CoinAuditsScreen(
    viewModel: CoinAuditsViewModel,
    onClickNavigation: () -> Unit,
    onClickReportUrl: (url: String) -> Unit
) {
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewItems by viewModel.viewItemsLiveData.observeAsState()

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(R.string.CoinPage_Audits),
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
                        if (viewItems?.isEmpty() == true) {
                            ListEmptyView(
                                text = stringResource(R.string.CoinPage_Audits_Empty),
                                icon = R.drawable.ic_not_available
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                viewItems?.forEach { viewItem ->
                                    item {
                                        CoinAuditHeader(viewItem.name, viewItem.logoUrl)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    item {
                                        CellMultilineLawrenceSection(viewItem.auditViewItems) { auditViewItem ->
                                            CoinAudit(auditViewItem) { auditViewItem.reportUrl?.let { onClickReportUrl(it)} }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier.height(32.dp))
                                    CellFooter(text = stringResource(id = R.string.CoinPage_Audits_PoweredBy))
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
private fun NoAudits() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        subhead2_grey(text = stringResource(R.string.CoinPage_Audits_Empty))
    }
}

@Composable
fun CoinAuditHeader(name: String, logoUrl: String) {
    CellSingleLineClear(borderTop = true) {
        CoinImage(
            iconUrl = logoUrl,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        body_leah(text = name)
    }
}

@Composable
fun CoinAudit(auditViewItem: CoinAuditsModule.AuditViewItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick, enabled = auditViewItem.reportUrl != null),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            body_leah(text = auditViewItem.date ?: "")
            subhead2_grey(
                text = auditViewItem.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        subhead2_grey(text = auditViewItem.issues.getString())

        if (auditViewItem.reportUrl != null) {
            Image(painterResource(id = R.drawable.ic_arrow_right), contentDescription = "")
        }
    }
}
