package cash.p.terminal.modules.restorelocal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import cash.p.terminal.R
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.managers.EncryptDecryptManager
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.AccountOrigin
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.EnabledWallet
import cash.p.terminal.modules.backuplocal.BackupLocalModule
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestoreLocalViewModel(
    private val backupJsonString: String?,
    private val accountManager: IAccountManager,
    private val accountFactory: IAccountFactory,
    private val walletManager: IWalletManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    fileName: String?,
) : ViewModel() {

    private var passphrase = ""
    private var passphraseState: DataState.Error? = null
    private var showButtonSpinner = false
    private var walletBackup: BackupLocalModule.WalletBackup? = null
    private val encryptDecryptManager = EncryptDecryptManager()
    private var parseError: Exception? = null
    private var accountType: AccountType? = null
    private var manualBackup = false
    private var restored = false

    val accountName by lazy {
        fileName?.let { name ->
            return@lazy name
                .replace(".json", "")
                .replace("UW_Backup_", "")
                .replace("_", " ")
        }
        accountFactory.getNextAccountName()
    }

    var uiState by mutableStateOf(
        RestoreLocalModule.UiState(
            passphraseState = null,
            showButtonSpinner = showButtonSpinner,
            parseError = parseError,
            accountType = accountType,
            manualBackup = manualBackup,
            restored = restored
        )
    )
        private set

    init {
        viewModelScope.launch {
            try {
                val gson = GsonBuilder()
                    .disableHtmlEscaping()
                    .enableComplexMapKeySerialization()
                    .create()
                walletBackup = gson.fromJson(backupJsonString, BackupLocalModule.WalletBackup::class.java)
                manualBackup = walletBackup?.manualBackup ?: false
            } catch (e: Exception) {
                parseError = e
                syncState()
            }
        }
    }

    fun onChangePassphrase(v: String) {
        passphrase = v
        passphraseState = null
        syncState()
    }

    fun onImportClick() {
        val backup = walletBackup ?: return
        showButtonSpinner = true
        syncState()
        viewModelScope.launch(Dispatchers.IO) {
            val kdfParams = backup.crypto.kdfparams
            val key = EncryptDecryptManager.getKey(passphrase, kdfParams) ?: return@launch
            val enabledWallets = backup.enabledWallets
            if (EncryptDecryptManager.passwordIsCorrect(backup.crypto.mac, backup.crypto.ciphertext, key)) {
                val decrypted = encryptDecryptManager.decrypt(backup.crypto.ciphertext, key, backup.crypto.cipherparams.iv)
                try {
                    val type = BackupLocalModule.getAccountTypeFromData(backup.type, decrypted)
                    if (type is AccountType.Cex) {
                        restoreCexAccount(type)
                        return@launch
                    } else if (!enabledWallets.isNullOrEmpty()) {
                        restoreWithWallets(type, enabledWallets)
                        return@launch
                    } else {
                        accountType = type
                    }
                } catch (e: IllegalStateException) {
                    parseError = e
                }
            } else {
                passphraseState = DataState.Error(Exception(Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword)))
            }
            showButtonSpinner = false
            withContext(Dispatchers.Main) {
                syncState()
            }
        }
    }

    private fun restoreWithWallets(
        type: AccountType,
        enabledWalletBackups: List<BackupLocalModule.EnabledWalletBackup>
    ) {
        val account = accountFactory.account(accountName, type, AccountOrigin.Restored, false, true)
        accountManager.save(account)

        val enabledWallets = enabledWalletBackups.map {
            EnabledWallet(
                tokenQueryId = it.tokenQueryId,
                accountId = account.id,
                coinName = it.coinName,
                coinCode = it.coinCode,
                coinDecimals = it.decimals
            )
        }
        walletManager.saveEnabledWallets(enabledWallets)

        enabledWalletBackups.forEach { backup ->
            TokenQuery.fromId(backup.tokenQueryId)?.let { tokenQuery ->
                if (!backup.settings.isNullOrEmpty()) {
                    val restoreSettings = RestoreSettings()
                    backup.settings.forEach { (restoreSettingType, value) ->
                        restoreSettings[restoreSettingType] = value
                    }
                    restoreSettingsManager.save(restoreSettings, account, tokenQuery.blockchainType)
                }
            }
        }

        restored = true
        syncState()
    }

    private fun restoreCexAccount(accountType: AccountType) {
        val account = accountFactory.account(accountName, accountType, AccountOrigin.Restored, true, true)
        accountManager.save(account)
        restored = true
        syncState()
    }

    fun onSelectCoinsShown() {
        accountType = null
        syncState()
    }

    private fun syncState() {
        uiState = RestoreLocalModule.UiState(
            passphraseState = passphraseState,
            showButtonSpinner = showButtonSpinner,
            parseError = parseError,
            accountType = accountType,
            manualBackup = manualBackup,
            restored = restored
        )
    }

}
