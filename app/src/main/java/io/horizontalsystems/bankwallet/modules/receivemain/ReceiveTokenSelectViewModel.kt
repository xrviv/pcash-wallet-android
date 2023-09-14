package cash.p.terminal.modules.receivemain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.eligibleTokens
import cash.p.terminal.core.isDefault
import cash.p.terminal.core.utils.Utils
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.Wallet
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch

class ReceiveTokenSelectViewModel(
    private val walletManager: IWalletManager,
    private val activeAccount: Account,
    private val fullCoinsProvider: FullCoinsProvider
) : ViewModel() {
    private var fullCoins: List<FullCoin> = listOf()

    var uiState by mutableStateOf(
        ReceiveTokenSelectUiState(
            fullCoins = fullCoins,
        )
    )

    init {
        fullCoinsProvider.setActiveWallets(walletManager.activeWallets)

        refreshItems()
        emitState()
    }

    fun updateFilter(q: String) {
        viewModelScope.launch {
            fullCoinsProvider.setQuery(q)
            refreshItems()

            emitState()
        }
    }

    private fun refreshItems() {
        fullCoins = fullCoinsProvider.getItems()
    }


    private fun emitState() {
        viewModelScope.launch {
            uiState = ReceiveTokenSelectUiState(
                fullCoins = fullCoins,
            )
        }
    }

    fun getCoinForReceiveType(fullCoin: FullCoin): CoinForReceiveType? {
        val eligibleTokens = fullCoin.eligibleTokens(activeAccount.type)

        return when {
            eligibleTokens.isEmpty() -> null
            eligibleTokens.size == 1 -> {
                CoinForReceiveType.Single(getOrCreateWallet(eligibleTokens.first()))
            }

            eligibleTokens.all { it.type is TokenType.Derived } -> {
                val activeWallets =
                    walletManager.activeWallets.filter { it.coin == fullCoin.coin }

                when {
                    activeWallets.isEmpty() -> {
                        eligibleTokens.find { it.type.isDefault }?.let { default ->
                            CoinForReceiveType.Single(createWallet(default))
                        }
                    }

                    activeWallets.size == 1 -> {
                        CoinForReceiveType.Single(activeWallets.first())
                    }

                    else -> {
                        CoinForReceiveType.MultipleDerivations
                    }
                }
            }

            eligibleTokens.all { it.type is TokenType.AddressTyped } -> {
                val activeWallets =
                    walletManager.activeWallets.filter { it.coin == fullCoin.coin }

                when {
                    activeWallets.isEmpty() -> {
                        eligibleTokens.find { it.type.isDefault }?.let { default ->
                            CoinForReceiveType.Single(createWallet(default))
                        }
                    }

                    activeWallets.size == 1 -> {
                        CoinForReceiveType.Single(activeWallets.first())
                    }

                    else -> {
                        CoinForReceiveType.MultipleAddressTypes
                    }
                }
            }

            else -> CoinForReceiveType.MultipleBlockchains
        }
    }

    private fun getOrCreateWallet(token: Token): Wallet {
        return walletManager
            .activeWallets
            .find { it.token == token }
            ?: createWallet(token)
    }

    private fun createWallet(token: Token): Wallet {
        val wallet = Wallet(token, activeAccount)

        walletManager.save(listOf(wallet))
        return wallet
    }

    class Factory(private val activeAccount: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val fullCoinsProvider = FullCoinsProvider(
                App.marketKit,
                activeAccount
            )
            return ReceiveTokenSelectViewModel(
                App.walletManager,
                activeAccount,
                fullCoinsProvider
            ) as T
        }
    }
}

sealed interface CoinForReceiveType {
    data class Single(val wallet: Wallet) : CoinForReceiveType
    object MultipleDerivations : CoinForReceiveType
    object MultipleAddressTypes : CoinForReceiveType
    object MultipleBlockchains : CoinForReceiveType
}

data class ReceiveTokenSelectUiState(
    val fullCoins: List<FullCoin>
)
