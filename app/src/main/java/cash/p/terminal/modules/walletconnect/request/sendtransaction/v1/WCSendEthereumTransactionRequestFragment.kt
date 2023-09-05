package cash.p.terminal.modules.walletconnect.request.sendtransaction.v1

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.walletconnect.WalletConnectViewModel
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCRequestModule
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import cash.p.terminal.modules.walletconnect.request.sendtransaction.ui.SendEthRequestScreen
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WCSendEthereumTransactionRequestFragment : BaseComposeFragment() {
    private val logger = AppLogger("wallet-connect")
    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.wcSessionFragment)
    val vmFactory by lazy {
        WCRequestModule.Factory(
            baseViewModel.sharedSendEthereumTransactionRequest!!,
            baseViewModel.service,
            baseViewModel.dAppName
        )
    }
    private val viewModel by viewModels<WCSendEthereumTransactionRequestViewModel> { vmFactory }
    private val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(R.id.wcSendEthereumTransactionRequestFragment) { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.wcSendEthereumTransactionRequestFragment) { vmFactory }
    private val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(R.id.wcSendEthereumTransactionRequestFragment) { vmFactory }

    @Composable
    override fun GetContent() {
        val cacheNoneViewModel = nonceViewModel //Needed in SendEvmSettingsFragment
        SendEthRequestScreen(
            findNavController(),
            viewModel,
            sendEvmTransactionViewModel,
            feeViewModel,
            logger,
            R.id.wcSendEthereumTransactionRequestFragment
        ) { close() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
            close()
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) { transactionHash ->
            viewModel.approve(transactionHash)
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            close()
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }

    }

    private fun close() {
        baseViewModel.sharedSendEthereumTransactionRequest = null
        findNavController().popBackStack()
    }

}
