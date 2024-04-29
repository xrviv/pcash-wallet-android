package cash.p.terminal.modules.swap.confirmation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsFragment
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionView
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.MenuItem
import io.horizontalsystems.core.CustomSnackbar
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

abstract class BaseSwapConfirmationFragment : BaseComposeFragment() {

    protected abstract val logger: AppLogger
    protected abstract val sendEvmTransactionViewModel: SendEvmTransactionViewModel
    protected abstract val feeViewModel: EvmFeeCellViewModel
    protected abstract val nonceViewModel: SendEvmNonceViewModel
    protected abstract val navGraphId: Int
    protected abstract val swapEntryPointDestId: Int

    private var snackbarInProcess: CustomSnackbar? = null

    @Composable
    override fun GetContent(navController: NavController) {
        BaseSwapConfirmationScreen(
            sendEvmTransactionViewModel = sendEvmTransactionViewModel,
            feeViewModel = feeViewModel,
            nonceViewModel = nonceViewModel,
            parentNavGraphId = navGraphId,
            navController = navController,
            onSendClick = {
                logger.info("click swap button")
                sendEvmTransactionViewModel.send(logger)

                stat(page = StatPage.SwapConfirmation, event = StatEvent.Send)
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sendEvmTransactionViewModel.sendingLiveData.observe(viewLifecycleOwner) {
            snackbarInProcess = HudHelper.showInProcessMessage(
                requireView(),
                R.string.Swap_Swapping,
                SnackbarDuration.INDEFINITE
            )
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) {
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(swapEntryPointDestId, true)
            }, 1200)
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)

            findNavController().popBackStack()
        }
    }
}

@Composable
private fun BaseSwapConfirmationScreen(
    sendEvmTransactionViewModel: SendEvmTransactionViewModel,
    feeViewModel: EvmFeeCellViewModel,
    nonceViewModel: SendEvmNonceViewModel,
    parentNavGraphId: Int,
    navController: NavController,
    onSendClick: () -> Unit
) {
    val enabled by sendEvmTransactionViewModel.sendEnabledLiveData.observeAsState(false)

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Send_Confirmation_Title),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
                        icon = R.drawable.ic_manage_2,
                        tint = ComposeAppTheme.colors.jacob,
                        onClick = {
                            navController.slideFromBottom(
                                R.id.sendEvmSettingsFragment,
                                SendEvmSettingsFragment.Input(parentNavGraphId)
                            )
                        }
                    )
                )
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                SendEvmTransactionView(
                    sendEvmTransactionViewModel,
                    feeViewModel,
                    nonceViewModel,
                    navController,
                    StatPage.SwapConfirmation
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Swap),
                    onClick = onSendClick,
                    enabled = enabled
                )
            }
        }
    }
}
