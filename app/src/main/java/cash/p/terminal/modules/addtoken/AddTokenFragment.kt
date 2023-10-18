package cash.p.terminal.modules.addtoken

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.addtoken.blockchainselector.AddTokenBlockchainSelectorScreen
import cash.p.terminal.modules.addtoken.blockchainselector.BlockchainSelectorResult
import cash.p.terminal.modules.swap.settings.Caution
import cash.p.terminal.modules.walletconnect.session.ui.TitleValueCell
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui.compose.components.FormsInputStateWarning
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead1_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.delay

class AddTokenFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        AddTokenNavHost(navController)
    }

}

private const val AddTokenPage = "add_token"
private const val BlockchainSelectorPage = "blockchain_selector"

@Composable
private fun AddTokenNavHost(
    fragmentNavController: NavController,
    viewModel: AddTokenViewModel = viewModel(factory = AddTokenModule.Factory())
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AddTokenPage,
    ) {
        composable(AddTokenPage) {
            AddTokenScreen(
                navController = navController,
                closeScreen = { fragmentNavController.popBackStack() },
                viewModel = viewModel
            )
        }
        composablePage(BlockchainSelectorPage) {
            AddTokenBlockchainSelectorScreen(
                blockchains = viewModel.blockchains,
                selectedBlockchain = viewModel.selectedBlockchain,
                navController = navController
            )
        }
    }
}

@Composable
private fun AddTokenScreen(
    navController: NavController,
    closeScreen: () -> Unit,
    viewModel: AddTokenViewModel,
) {
    navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<List<Blockchain>>(BlockchainSelectorResult, emptyList())
        ?.collectAsState()?.value?.let { selectedItems ->
            if (selectedItems.isNotEmpty()) {
                viewModel.onBlockchainSelect(selectedItems.first())
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set<List<Blockchain>>(BlockchainSelectorResult, emptyList())
            }
        }

    val uiState = viewModel.uiState
    val view = LocalView.current

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) {
            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done, SnackbarDuration.LONG)
            delay(300)
            closeScreen.invoke()
        }
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = stringResource(R.string.AddToken_Title),
                navigationIcon = {
                    HsBackButton(onClick = closeScreen)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Add),
                        onClick = viewModel::onAddClick,
                        enabled = uiState.addButtonEnabled
                    )
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                CellUniversalLawrenceSection(
                    listOf {
                        RowUniversal(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = { navController.navigate(BlockchainSelectorPage) }
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_blocks_24),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            body_leah(
                                text = stringResource(R.string.AddToken_Blockchain),
                                modifier = Modifier.weight(1f)
                            )
                            subhead1_grey(
                                text = viewModel.selectedBlockchain.name,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.grey
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    enabled = false,
                    hint = stringResource(R.string.AddToken_AddressOrSymbol),
                    state = getState(uiState.caution, uiState.loading),
                    qrScannerEnabled = true,
                ) {
                    viewModel.onEnterText(it)
                }

                Spacer(modifier = Modifier.height(32.dp))

                uiState.tokenInfo?.let { tokenInfo ->
                    CellUniversalLawrenceSection(
                        listOf(
                            {
                                TitleValueCell(
                                    stringResource(R.string.AddToken_CoinName),
                                    tokenInfo.token.coin.name
                                )
                            }, {
                                TitleValueCell(
                                    stringResource(R.string.AddToken_CoinCode),
                                    tokenInfo.token.coin.code
                                )
                            }, {
                                TitleValueCell(
                                    stringResource(R.string.AddToken_Decimals),
                                    tokenInfo.token.decimals.toString()
                                )
                            }
                        )
                    )
                }
            }
        }
    }
}

private fun getState(caution: Caution?, loading: Boolean) = when (caution?.type) {
    Caution.Type.Error -> DataState.Error(Exception(caution.text))
    Caution.Type.Warning -> DataState.Error(FormsInputStateWarning(caution.text))
    null -> if (loading) DataState.Loading else null
}
