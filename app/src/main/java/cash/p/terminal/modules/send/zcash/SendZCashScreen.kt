package cash.p.terminal.modules.send.zcash

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import cash.p.terminal.modules.fee.HSFeeInput
import cash.p.terminal.modules.memo.HSMemoInput
import cash.p.terminal.modules.send.SendConfirmationFragment
import cash.p.terminal.modules.send.SendScreen
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow

@Composable
fun SendZCashScreen(
    navController: NavController,
    viewModel: SendZCashViewModel,
    amountInputModeViewModel: AmountInputModeViewModel
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val addressError = uiState.addressError
    val amountCaution = uiState.amountCaution
    val fee = uiState.fee
    val proceedEnabled = uiState.canBeSend
    val memoIsAllowed = uiState.memoIsAllowed
    val amountInputType = amountInputModeViewModel.inputType

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(factory = AddressParserModule.Factory(wallet.token.blockchainType))
    val amountUnique = paymentAddressViewModel.amountUnique

    ComposeAppTheme {
        val fullCoin = wallet.token.fullCoin
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SendScreen(
            fullCoin = fullCoin,
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

            Spacer(modifier = Modifier.height(12.dp))
            HSAddressInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                tokenQuery = wallet.token.tokenQuery,
                coinCode = wallet.coin.code,
                error = addressError,
                textPreprocessor = paymentAddressViewModel,
                navController = navController
            ) {
                viewModel.onEnterAddress(it)
            }

            if (memoIsAllowed) {
                Spacer(modifier = Modifier.height(12.dp))
                HSMemoInput(
                    maxLength = viewModel.memoMaxLength
                ) {
                    viewModel.onEnterMemo(it)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HSFeeInput(
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fee = fee,
                amountInputType = amountInputType,
                rate = viewModel.coinRate,
                navController = navController
            )

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Send_DialogProceed),
                onClick = {
                    navController.slideFromRight(
                        R.id.sendConfirmation,
                        SendConfirmationFragment.prepareParams(SendConfirmationFragment.Type.ZCash)
                    )
                },
                enabled = proceedEnabled
            )
        }
    }
}
