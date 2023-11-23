package cash.p.terminal.modules.send.evm

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.address.AddressParserModule
import cash.p.terminal.modules.address.AddressParserViewModel
import cash.p.terminal.modules.address.HSAddressInput
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.amount.HSAmountInput
import cash.p.terminal.modules.availablebalance.AvailableBalance
import cash.p.terminal.modules.send.SendScreen
import cash.p.terminal.modules.send.evm.confirmation.SendEvmConfirmationModule
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.helpers.HudHelper
import java.math.BigDecimal

@Composable
fun SendEvmScreen(
    title: String,
    navController: NavController,
    viewModel: SendEvmViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int,
    prefilledAmount: BigDecimal? = null,
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val addressError = uiState.addressError
    val amountCaution = uiState.amountCaution
    val proceedEnabled = uiState.canBeSend
    val amountInputType = amountInputModeViewModel.inputType

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token.blockchainType, prefilledAmount)
    )
    val amountUnique = paymentAddressViewModel.amountUnique
    val view = LocalView.current

    ComposeAppTheme {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SendScreen(
            title = title,
            onCloseClick = { navController.popBackStack() }
        ) {
            AvailableBalance(
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                availableBalance = availableBalance,
                amountInputType = amountInputType,
                rate = viewModel.coinRate
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAmountInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                focusRequester = focusRequester,
                availableBalance = availableBalance,
                caution = amountCaution,
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                onClickHint = {
                    amountInputModeViewModel.onToggleInputType()
                },
                onValueChange = {
                    viewModel.onEnterAmount(it)
                },
                inputType = amountInputType,
                rate = viewModel.coinRate,
                amountUnique = amountUnique
            )

            if (uiState.showAddressInput) {
                Spacer(modifier = Modifier.height(12.dp))
                HSAddressInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = uiState.prefilledAddress,
                    tokenQuery = wallet.token.tokenQuery,
                    coinCode = wallet.coin.code,
                    error = addressError,
                    textPreprocessor = paymentAddressViewModel,
                    navController = navController
                ) {
                    viewModel.onEnterAddress(it)
                }
            }
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Send_DialogProceed),
                onClick = {
                    if (viewModel.hasConnection()) {
                        viewModel.getSendData()?.let {
                            navController.slideFromRight(
                                R.id.sendEvmConfirmationFragment,
                                SendEvmConfirmationModule.prepareParams(it, R.id.sendXFragment, sendEntryPointDestId)
                            )
                        }
                    } else {
                        HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                    }
                },
                enabled = proceedEnabled
            )
        }
    }
}
