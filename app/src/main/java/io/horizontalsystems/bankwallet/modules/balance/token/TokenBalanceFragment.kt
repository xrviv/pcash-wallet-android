package cash.p.terminal.modules.balance.token

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.transactions.TransactionsViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable

class TokenBalanceFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            try {
                val wallet = requireArguments().parcelable<Wallet>(WALLET_KEY) ?: throw IllegalStateException("Wallet is Null!")
                val viewModel by viewModels<TokenBalanceViewModel> { TokenBalanceModule.Factory(wallet) }
                val transactionsViewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment)

                setContent {
                    ComposeAppTheme {
                        TokenBalanceScreen(viewModel, transactionsViewModel, findNavController())
                    }
                }

            } catch (t: Throwable) {
                Toast.makeText(
                    App.instance, t.message ?: t.javaClass.simpleName, Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
        }
    }

    companion object {
        private const val WALLET_KEY = "wallet_key"

        fun prepareParams(wallet: Wallet) = bundleOf(
            WALLET_KEY to wallet
        )
    }
}
