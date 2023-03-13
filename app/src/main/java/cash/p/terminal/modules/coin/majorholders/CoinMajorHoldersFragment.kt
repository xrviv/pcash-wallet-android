package cash.p.terminal.modules.coin.majorholders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.shorten
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.MajorHolderItem
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import cash.p.terminal.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class CoinMajorHoldersFragment : BaseFragment() {

    private val coinUid by lazy {
        requireArguments().getString(COIN_UID_KEY)!!
    }

    private val blockchainUid by lazy {
        requireArguments().getString(BLOCKCHAIN_UID_KEY)!!
    }

    private val blockchainName by lazy {
        requireArguments().getString(BLOCKCHAIN_NAME_KEY)!!
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
                    CoinMajorHoldersScreen(
                        coinUid,
                        blockchainUid,
                        blockchainName,
                        findNavController(),
                    )
                }
            }
        }
    }


    companion object {
        private const val COIN_UID_KEY = "coin_uid_key"
        private const val BLOCKCHAIN_UID_KEY = "blockchain_key"
        private const val BLOCKCHAIN_NAME_KEY = "blockchain_name_key"

        fun prepareParams(coinUid: String, blockchainUid: String, blockchainName: String) =
            bundleOf(
                COIN_UID_KEY to coinUid,
                BLOCKCHAIN_UID_KEY to blockchainUid,
                BLOCKCHAIN_NAME_KEY to blockchainName
            )
    }
}

@Composable
private fun CoinMajorHoldersScreen(
    coinUid: String,
    blockchainUid: String,
    blockchainName: String,
    navController: NavController,
    viewModel: CoinMajorHoldersViewModel = viewModel(
        factory = CoinMajorHoldersModule.Factory(coinUid, blockchainUid)
    )
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.PlainString(blockchainName),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )

            Crossfade(viewModel.uiState.viewState) { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        Loading()
                    }
                    is ViewState.Error -> {
                        ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                        viewModel.uiState.error?.let {
                            SnackbarError(it.getString())
                            viewModel.errorShown()
                        }
                    }
                    ViewState.Success -> {
                        CoinMajorHoldersContent(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinMajorHoldersContent(
    viewModel: CoinMajorHoldersViewModel,
) {
    val uiState = viewModel.uiState

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {

        item {
            HoldersGeneralInfo(uiState.top10Share, uiState.totalHoldersCount)
        }

        item {
            StackedBarChart(
                slices = uiState.chartData,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
            )
        }

        items(uiState.topHolders) {
            TopWalletCell(it)
        }

        item {
            SeeAllButton()
        }
    }
}

@Composable
fun HoldersGeneralInfo(top10Share: String, totalHoldersCount: String) {
    VSpacer(12.dp)
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        headline1_bran(text = top10Share)
        HSpacer(8.dp)
        subhead1_grey(text = stringResource(R.string.CoinPage_MajorHolders_InTopWallets))
    }
    VSpacer(12.dp)
    subhead2_grey(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringResource(R.string.CoinPage_MajorHolders_HoldersNumber, totalHoldersCount)
    )
}

@Composable
fun SeeAllButton() {
    VSpacer(32.dp)
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { }
            ) {
                body_leah(
                    text = stringResource(R.string.Market_SeeAll),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    )
    VSpacer(32.dp)
}

@Composable
private fun TopWalletCell(item: MajorHolderItem) {
    val localView = LocalView.current

    SectionItemBorderedRowUniversalClear(borderTop = true) {
        captionSB_grey(
            text = item.index.toString(),
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            body_leah(text = item.sharePercent)
            VSpacer(1.dp)
            subhead2_grey(text = item.balance)
        }

        ButtonSecondaryDefault(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .height(28.dp),
            title = item.address.shorten(),
            onClick = {
                TextHelper.copyText(item.address)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            }
        )
    }
}
