package cash.p.terminal.modules.transactionInfo.options

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsFragment
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionView
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewModel
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
import io.horizontalsystems.core.parcelable

class TransactionSpeedUpCancelFragment : BaseComposeFragment() {

    private val logger = AppLogger("tx-speedUp-cancel")
    private val transactionInfoViewModel by navGraphViewModels<TransactionInfoViewModel>(R.id.transactionInfoFragment)
    private val optionType by lazy {
        arguments?.parcelable<TransactionInfoOptionsModule.Type>(
            OPTION_TYPE_KEY
        )!!
    }
    private val transactionHash by lazy { arguments?.getString(TRANSACTION_HASH_KEY)!! }

    private val vmFactory by lazy {
        TransactionInfoOptionsModule.Factory(
            optionType,
            transactionHash,
            transactionInfoViewModel.source
        )
    }
    private val speedUpCancelViewModel by viewModels<TransactionSpeedUpCancelViewModel> { vmFactory }
    private val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(R.id.transactionSpeedUpCancelFragment) { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.transactionSpeedUpCancelFragment) { vmFactory }
    private val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(R.id.transactionSpeedUpCancelFragment) { vmFactory }

    private var snackbarInProcess: CustomSnackbar? = null

    @Composable
    override fun GetContent(navController: NavController) {
        TransactionSpeedUpCancelScreen(
            sendEvmTransactionViewModel = sendEvmTransactionViewModel,
            feeViewModel = feeViewModel,
            nonceViewModel = nonceViewModel,
            parentNavGraphId = R.id.transactionSpeedUpCancelFragment,
            speedUpCancelViewModel = speedUpCancelViewModel,
            navController = navController,
            onSendClick = {
                logger.info("click send button")
                sendEvmTransactionViewModel.send(logger)
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sendEvmTransactionViewModel.sendingLiveData.observe(viewLifecycleOwner) {
            snackbarInProcess = HudHelper.showInProcessMessage(
                requireView(),
                R.string.Send_Sending,
                SnackbarDuration.INDEFINITE
            )
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(
            viewLifecycleOwner
        ) {
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(R.id.transactionInfoFragment, true)
            }, 1200)
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)

            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack()
            }, 1200)
        }

        if (!speedUpCancelViewModel.isTransactionPending) {
            HudHelper.showErrorMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.TransactionInfoOptions_Warning_TransactionInBlock
            )
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(R.id.transactionInfoFragment, true)
            }, 1500)
        }

    }

    companion object {
        private const val OPTION_TYPE_KEY = "option_type_key"
        private const val TRANSACTION_HASH_KEY = "transaction_hash_key"

        fun prepareParams(
            optionType: TransactionInfoOptionsModule.Type,
            transactionHash: String
        ): Bundle {
            return bundleOf(
                OPTION_TYPE_KEY to optionType,
                TRANSACTION_HASH_KEY to transactionHash
            )
        }
    }

}

@Composable
private fun TransactionSpeedUpCancelScreen(
    sendEvmTransactionViewModel: SendEvmTransactionViewModel,
    feeViewModel: EvmFeeCellViewModel,
    nonceViewModel: SendEvmNonceViewModel,
    speedUpCancelViewModel: TransactionSpeedUpCancelViewModel,
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
                                resId = R.id.sendEvmSettingsFragment,
                                args = SendEvmSettingsFragment.prepareParams(parentNavGraphId)
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
                )
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = speedUpCancelViewModel.buttonTitle,
                    onClick = onSendClick,
                    enabled = if (speedUpCancelViewModel.isTransactionPending) enabled else false
                )
            }
        }
    }
}
