package cash.p.terminal.modules.settings.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App

object MainSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = MainSettingsService(
                App.backupManager,
                App.languageManager,
                App.systemInfoManager,
                App.currencyManager,
                App.termsManager,
                App.pinComponent,
                App.wc1SessionManager,
                App.wc2SessionManager,
                App.wc1Manager,
                App.accountManager
            )
            val viewModel = MainSettingsViewModel(
                service,
                App.appConfigProvider.companyWebPageLink,
                App.subscriptionManager,
            )

            return viewModel as T
        }
    }

    sealed class CounterType {
        class SessionCounter(val number: Int) : CounterType()
        class PendingRequestCounter(val number: Int) : CounterType()
    }

}
