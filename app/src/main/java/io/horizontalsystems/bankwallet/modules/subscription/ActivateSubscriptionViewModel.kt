package cash.p.terminal.modules.subscription

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.managers.SubscriptionManager
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.toHexString
import cash.p.terminal.entities.Account
import cash.p.terminal.modules.walletconnect.session.v1.WCSessionViewModel
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.net.UnknownHostException

class ActivateSubscriptionViewModel(
    private val marketKit: MarketKitWrapper,
    private val accountManager: IAccountManager,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    private var subscriptionAccount: SubscriptionAccount? = null

    private var fetchingMessage = true
    private var fetchingMessageError: Throwable? = null
    private var subscriptionInfo: SubscriptionInfo? = null
    private var fetchingToken = false
    private var fetchingTokenError: Throwable? = null
    private var fetchingTokenSuccess = false

    var uiState: ActivateSubscription by mutableStateOf(
        ActivateSubscription(
            fetchingMessage = fetchingMessage,
            fetchingMessageError = fetchingMessageError,
            subscriptionInfo = subscriptionInfo,
            fetchingToken = fetchingToken,
            fetchingTokenError = fetchingTokenError,
            fetchingTokenSuccess = fetchingTokenSuccess,
            signButtonState = getSignButtonState(),
        )
    )
        private set

    init {
        fetchMessageToSign()
    }

    private fun fetchMessageToSign() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchingMessage = true
            emitState()

            try {
                val accountsMap = accountManager.accounts.mapNotNull { account ->
                    account.type.evmAddress(App.evmBlockchainManager.getChain(BlockchainType.Ethereum))?.hex?.let { address ->
                        Pair(address, account)
                    }
                }.associateBy({ it.first }, { it.second })

                val addresses = accountsMap.keys.toList()
                val subscriptions = marketKit.subscriptionsSingle(addresses).await()
                val address = subscriptions.maxByOrNull { it.deadline }?.address ?: throw NoSubscription()

                subscriptionAccount = SubscriptionAccount(address, accountsMap[address])

                val messageToSign = marketKit.authGetSignMessage(address).await()

                fetchingMessage = false
                fetchingMessageError = null
                subscriptionInfo = SubscriptionInfo(
                    walletName = accountsMap[address]?.name ?: "--",
                    walletAddress = address,
                    messageToSign = messageToSign
                )

            } catch (e: Throwable) {
                fetchingMessage = false
                fetchingMessageError = if (e is UnknownHostException) {
                    IllegalStateException(Translator.getString(R.string.Hud_Text_NoInternet))
                } else {
                    e
                }
            }

            emitState()
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = ActivateSubscription(
                fetchingMessage = fetchingMessage,
                fetchingMessageError = fetchingMessageError,
                subscriptionInfo = subscriptionInfo,
                fetchingToken = fetchingToken,
                fetchingTokenError = fetchingTokenError,
                fetchingTokenSuccess = fetchingTokenSuccess,
                signButtonState = getSignButtonState()
            )
        }
    }

    private fun getSignButtonState() = when {
        fetchingToken -> WCSessionViewModel.ButtonState.Disabled
        fetchingMessage -> WCSessionViewModel.ButtonState.Hidden
        subscriptionInfo != null -> WCSessionViewModel.ButtonState.Enabled
        else -> WCSessionViewModel.ButtonState.Hidden
    }

    fun sign() {
        val subscriptionInfo = subscriptionInfo ?: return
        val account = subscriptionAccount?.account ?: return
        val address = subscriptionAccount?.address ?: return

        fetchingToken = true
        emitState()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val signature = account.type.sign(subscriptionInfo.messageToSign.toByteArray()) ?: throw IllegalStateException()
                val token = marketKit.authenticate(signature.toHexString(), address).await()
                subscriptionManager.authToken = token
                fetchingTokenSuccess = true
            } catch (t: Throwable) {
                fetchingTokenError = t
            }

            fetchingToken = false
            emitState()
        }
    }

    fun retry() {
        fetchMessageToSign()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActivateSubscriptionViewModel(
                App.marketKit,
                App.accountManager,
                App.subscriptionManager
            ) as T
        }
    }
}

data class SubscriptionAccount(
    val address: String,
    val account: Account?
)

data class SubscriptionInfo(
    val walletName: String,
    val walletAddress: String,
    val messageToSign: String
)

data class ActivateSubscription(
    val fetchingMessage: Boolean,
    val fetchingMessageError: Throwable?,
    val subscriptionInfo: SubscriptionInfo?,
    val fetchingToken: Boolean,
    val fetchingTokenError: Throwable?,
    val fetchingTokenSuccess: Boolean,
    val signButtonState: WCSessionViewModel.ButtonState
)

class NoSubscription : Exception()
