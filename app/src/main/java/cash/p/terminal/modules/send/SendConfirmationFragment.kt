package cash.p.terminal.modules.send

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.send.binance.SendBinanceConfirmationScreen
import cash.p.terminal.modules.send.binance.SendBinanceViewModel
import cash.p.terminal.modules.send.bitcoin.SendBitcoinConfirmationScreen
import cash.p.terminal.modules.send.bitcoin.SendBitcoinViewModel
import cash.p.terminal.modules.send.solana.SendSolanaConfirmationScreen
import cash.p.terminal.modules.send.solana.SendSolanaViewModel
import cash.p.terminal.modules.send.tron.SendTronConfirmationScreen
import cash.p.terminal.modules.send.tron.SendTronViewModel
import cash.p.terminal.modules.send.zcash.SendZCashConfirmationScreen
import cash.p.terminal.modules.send.zcash.SendZCashViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable
import kotlinx.parcelize.Parcelize

class SendConfirmationFragment : BaseFragment() {
    val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment)

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
                val arguments = requireArguments()
                val sendEntryPointDestId = arguments.getInt(sendEntryPointDestIdKey)
                when (arguments.parcelable<Type>(typeKey)) {
                    Type.Bitcoin -> {
                        val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment)

                        SendBitcoinConfirmationScreen(
                            findNavController(),
                            sendBitcoinViewModel,
                            amountInputModeViewModel,
                            sendEntryPointDestId
                        )
                    }
                    Type.Bep2 -> {
                        val sendBinanceViewModel by navGraphViewModels<SendBinanceViewModel>(R.id.sendXFragment)

                        SendBinanceConfirmationScreen(
                            findNavController(),
                            sendBinanceViewModel,
                            amountInputModeViewModel,
                            sendEntryPointDestId
                        )
                    }
                    Type.ZCash -> {
                        val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment)

                        SendZCashConfirmationScreen(
                            findNavController(),
                            sendZCashViewModel,
                            amountInputModeViewModel,
                            sendEntryPointDestId
                        )
                    }
                    Type.Tron -> {
                        val sendTronViewModel by navGraphViewModels<SendTronViewModel>(R.id.sendXFragment)
                        SendTronConfirmationScreen(
                            findNavController(),
                            sendTronViewModel,
                            amountInputModeViewModel,
                            sendEntryPointDestId
                        )
                    }
                    Type.Solana -> {
                        val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment)

                        SendSolanaConfirmationScreen(
                            findNavController(),
                            sendSolanaViewModel,
                            amountInputModeViewModel,
                            sendEntryPointDestId
                        )
                    }
                    null -> Unit
                }
            }
        }
    }

    @Parcelize
    enum class Type : Parcelable {
        Bitcoin, Bep2, ZCash, Solana, Tron
    }

    companion object {
        private const val typeKey = "typeKey"
        private const val sendEntryPointDestIdKey = "sendEntryPointDestIdKey"

        fun prepareParams(type: Type, sendEntryPointDestId: Int) = bundleOf(
            typeKey to type,
            sendEntryPointDestIdKey to sendEntryPointDestId,
        )
    }
}
