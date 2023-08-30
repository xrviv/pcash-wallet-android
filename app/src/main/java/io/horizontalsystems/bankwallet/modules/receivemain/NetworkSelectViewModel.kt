package cash.p.terminal.modules.receivemain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.order

class NetworkSelectViewModel(
    private val coinUid: String,
    private val walletManager: IWalletManager
) : ViewModel() {

    val coinWallets = walletManager.activeWallets.filter { it.coin.uid == coinUid }.sortedBy { it.token.blockchainType.order }

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NetworkSelectViewModel(coinUid, App.walletManager) as T
        }
    }
}
