package cash.p.terminal.modules.managewallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsViewModel

object ManageWalletsModule {

    class Factory : ViewModelProvider.Factory {

        private val restoreSettingsService by lazy {
            RestoreSettingsService(App.restoreSettingsManager, App.zcashBirthdayProvider)
        }

        private val manageWalletsService by lazy {
            ManageWalletsService(
                App.marketKit,
                App.walletManager,
                App.accountManager,
                App.restoreSettingsManager,
                restoreSettingsService
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                RestoreSettingsViewModel::class.java -> {
                    RestoreSettingsViewModel(restoreSettingsService, listOf(restoreSettingsService)) as T
                }
                ManageWalletsViewModel::class.java -> {
                    ManageWalletsViewModel(manageWalletsService, listOf(manageWalletsService)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
