package cash.p.terminal.modules.swapxxx.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.swapxxx.SwapConfirmViewModel

class SwapTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapTransactionSettingsScreen(navController)
    }
}

@Composable
fun SwapTransactionSettingsScreen(navController: NavController) {
    val viewModel = viewModel<SwapConfirmViewModel>(
        viewModelStoreOwner = navController.previousBackStackEntry!!,
    )

    val sendTransactionService = viewModel.getSendTransactionService()

    sendTransactionService.GetContent(navController)
}
