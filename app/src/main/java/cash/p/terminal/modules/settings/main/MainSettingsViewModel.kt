package cash.p.terminal.modules.settings.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.managers.SubscriptionManager
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.modules.settings.main.MainSettingsModule.CounterType
import cash.p.terminal.modules.walletconnect.version1.WC1Manager
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class MainSettingsViewModel(
    private val service: MainSettingsService,
    val companyWebPage: String,
    private val subscriptionManager: SubscriptionManager,
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

    val manageWalletShowAlertLiveData = MutableLiveData(shouldShowAlertForManageWallet(service.allBackedUp, service.hasNonStandardAccount))
    val securityCenterShowAlertLiveData = MutableLiveData(!service.isPinSet)
    val aboutAppShowAlertLiveData = MutableLiveData(!service.termsAccepted)
    val wcCounterLiveData = MutableLiveData<CounterType?>(null)
    val baseCurrencyLiveData = MutableLiveData(service.baseCurrency)
    val languageLiveData = MutableLiveData(service.currentLanguageDisplayName)
    val appVersion by service::appVersion

    var openPersonalSupport by mutableStateOf(false)
        private set

    private var wcSessionsCount = service.walletConnectSessionCount
    private var wc2PendingRequestCount = 0

    init {
        viewModelScope.launch {
            service.termsAcceptedFlow.collect {
                aboutAppShowAlertLiveData.postValue(!it)
            }
        }

        service.backedUpObservable
            .subscribeIO { manageWalletShowAlertLiveData.postValue(shouldShowAlertForManageWallet(it, service.hasNonStandardAccount)) }
            .let { disposables.add(it) }

        service.pinSetObservable
            .subscribeIO { securityCenterShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.baseCurrencyObservable
            .subscribeIO { baseCurrencyLiveData.postValue(it) }
            .let { disposables.add(it) }

        service.walletConnectSessionCountObservable
            .subscribeIO {
                wcSessionsCount = it
                syncCounter()
            }
            .let { disposables.add(it) }

        viewModelScope.launch {
            service.pendingRequestCountFlow.collect {
                wc2PendingRequestCount = it
                syncCounter()
            }
        }
        syncCounter()
        service.start()
    }
    private fun shouldShowAlertForManageWallet(allBackedUp: Boolean, hasNonStandardAccount: Boolean): Boolean {
        return !allBackedUp || hasNonStandardAccount
    }
    // ViewModel

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun getWalletConnectSupportState() : WC1Manager.SupportState {
        return service.getWalletConnectSupportState()
    }

    private fun syncCounter() {
        if (wc2PendingRequestCount > 0) {
            wcCounterLiveData.postValue(CounterType.PendingRequestCounter(wc2PendingRequestCount))
        } else if (wcSessionsCount > 0) {
            wcCounterLiveData.postValue(CounterType.SessionCounter(wcSessionsCount))
        } else {
            wcCounterLiveData.postValue(null)
        }
    }

    fun onPersonalSupportClick() {
        if (subscriptionManager.hasSubscription()){
            openPersonalSupport = true
        } else {
            viewModelScope.launch {
                subscriptionManager.showPremiumFeatureWarning()
            }
        }
    }

    fun personalSupportOpened() {
        openPersonalSupport = false
    }
}
