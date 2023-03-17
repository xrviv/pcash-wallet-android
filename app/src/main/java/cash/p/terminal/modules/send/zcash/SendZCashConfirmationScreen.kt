package cash.p.terminal.modules.send.zcash

import androidx.compose.runtime.*
import androidx.navigation.NavController
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.send.SendConfirmationScreen
import cash.p.terminal.ui.compose.DisposableLifecycleCallbacks

@Composable
fun SendZCashConfirmationScreen(
    navController: NavController,
    sendViewModel: SendZCashViewModel,
    amountInputModeViewModel: AmountInputModeViewModel
) {
    var confirmationData by remember { mutableStateOf(sendViewModel.getConfirmationData()) }
    var refresh by remember { mutableStateOf(false) }

    DisposableLifecycleCallbacks(
        onResume = {
            if (refresh) {
                confirmationData = sendViewModel.getConfirmationData()
            }
        },
        onPause = {
            refresh = true
        }
    )

    SendConfirmationScreen(
        navController = navController,
        coinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        fiatMaxAllowedDecimals = sendViewModel.fiatMaxAllowedDecimals,
        amountInputType = amountInputModeViewModel.inputType,
        rate = sendViewModel.coinRate,
        feeCoinRate = sendViewModel.coinRate,
        sendResult = sendViewModel.sendResult,
        coin = confirmationData.coin,
        blockchainType = sendViewModel.blockchainType,
        feeCoin = confirmationData.feeCoin,
        amount = confirmationData.amount,
        address = confirmationData.address,
        contact = confirmationData.contact,
        fee = confirmationData.fee,
        lockTimeInterval = confirmationData.lockTimeInterval,
        memo = confirmationData.memo,
        onClickSend = sendViewModel::onClickSend
    )
}