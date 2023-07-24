package cash.p.terminal.modules.withdrawcex.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.modules.coinzixverify.CoinzixVerificationMode
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.fee.FeeCell
import cash.p.terminal.modules.send.ConfirmAmountCell
import cash.p.terminal.modules.withdrawcex.WithdrawCexViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellowWithSpinner
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.SectionTitleCell
import cash.p.terminal.ui.compose.components.TransactionInfoAddressCell
import cash.p.terminal.ui.compose.components.TransactionInfoCell
import cash.p.terminal.ui.compose.components.TransactionInfoContactCell
import cash.p.terminal.ui.compose.components.VSpacer
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

@Composable
fun WithdrawCexConfirmScreen(
    mainViewModel: WithdrawCexViewModel,
    openVerification: (CoinzixVerificationMode.Withdraw) -> Unit,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    ComposeAppTheme {
        val confirmationData = mainViewModel.getConfirmationData()

        val assetName = confirmationData.assetName
        val coinAmount = confirmationData.coinAmount
        val currencyAmount = confirmationData.currencyAmount
        val coinIconUrl = confirmationData.coinIconUrl
        val address = confirmationData.address
        val contact = confirmationData.contact
        val blockchainType = confirmationData.blockchainType
        val networkName = confirmationData.networkName
        val fee = confirmationData.feeItem

        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Send_Confirmation_Title),
                    navigationIcon = {
                        HsBackButton(onClick = onNavigateBack)
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onClose
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
                ) {
                    VSpacer(12.dp)
                    val topSectionItems = buildList<@Composable () -> Unit> {
                        add {
                            SectionTitleCell(
                                stringResource(R.string.Send_Confirmation_YouSend),
                                assetName,
                                R.drawable.ic_arrow_up_right_12
                            )
                        }
                        add {
                            ConfirmAmountCell(currencyAmount, coinAmount, coinIconUrl)
                        }
                        add {
                            TransactionInfoAddressCell(
                                title = stringResource(R.string.Send_Confirmation_To),
                                value = address.hex,
                                showAdd = false,
                                blockchainType = blockchainType,
                                navController = navController
                            )
                        }
                        contact?.let {
                            add {
                                TransactionInfoContactCell(name = contact.name)
                            }
                        }
                    }

                    CellUniversalLawrenceSection(topSectionItems)

                    VSpacer(16.dp)

                    networkName?.let { networkName ->
                        CellUniversalLawrenceSection(
                            listOf {
                                TransactionInfoCell(
                                    stringResource(R.string.CexWithdraw_Network),
                                    networkName
                                )
                            }
                        )
                    }
                    VSpacer(16.dp)

                    CellUniversalLawrenceSection(
                        listOf {
                            FeeCell(
                                title = stringResource(R.string.FeeSettings_NetworkFee),
                                info = "",
                                value = fee,
                                viewState = null,
                                navController = null
                            )
                        }
                    )
                }

                var confirmEnabled by remember { mutableStateOf(true) }

                ButtonsGroupWithShade {
                    ButtonPrimaryYellowWithSpinner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        title = stringResource(R.string.CexWithdraw_Withdraw),
                        onClick = {
                            coroutineScope.launch {
                                confirmEnabled = false
                                try {
                                    val withdraw = mainViewModel.confirm()
                                    openVerification.invoke(withdraw)
                                } catch (error: Throwable) {

                                    HudHelper.showErrorMessage(view, error.message ?: it.javaClass.simpleName)
                                }
                                confirmEnabled = true
                            }
                        },
                        showSpinner = !confirmEnabled,
                        enabled = confirmEnabled
                    )
                }
            }
        }
    }
}
