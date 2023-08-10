package cash.p.terminal.modules.settings.appstatus

import cash.p.terminal.core.AppLog
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.IAdapterManager
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.adapters.BaseTronAdapter
import cash.p.terminal.core.adapters.BitcoinBaseAdapter
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountType
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.marketkit.models.BlockchainType
import java.util.Date

class AppStatusService(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val marketKit: MarketKitWrapper
) {

    val status: LinkedHashMap<String, Any>
        get() {
            val status = LinkedHashMap<String, Any>()

            status["App Info"] = getAppInfo()
            status["Version History"] = getVersionHistory()
            status["Wallets Status"] = getWalletsStatus()
            status["Blockchain Status"] = getBlockchainStatus()
            status["App Log"] = AppLog.getLog()
            status["Market Last Sync Timestamps"] = getMarketLastSyncTimestamps()

            return status
        }

    private fun getAppInfo(): Map<String, Any> {
        val appInfo = LinkedHashMap<String, Any>()
        appInfo["Current Time"] = Date()
        appInfo["App Version"] = systemInfoManager.appVersion
        appInfo["Device Model"] = systemInfoManager.deviceModel
        appInfo["OS Version"] = systemInfoManager.osVersion

        return appInfo
    }

    private fun getVersionHistory(): Map<String, Any> {
        val versions = LinkedHashMap<String, Date>()

        localStorage.appVersions.sortedBy { it.timestamp }.forEach { version ->
            versions[version.version] = Date(version.timestamp)
        }
        return versions
    }

    private fun getWalletsStatus(): Map<String, Any> {
        val wallets = LinkedHashMap<String, Any>()

        for (account in accountManager.accounts) {
            val title = account.name

            wallets[title] = getAccountDetails(account)
        }
        return wallets
    }

    private fun getAccountDetails(account: Account): LinkedHashMap<String, Any> {
        val accountDetails = LinkedHashMap<String, Any>()

        accountDetails["Origin"] = account.origin.value

        val accountType = account.type
        if (accountType is AccountType.Mnemonic) {
            accountDetails["Mnemonic"] = accountType.words.count()
        }
        return accountDetails
    }

    private fun getBlockchainStatus(): Map<String, Any> {
        val blockchainStatus = LinkedHashMap<String, Any>()

        blockchainStatus.putAll(getBitcoinForkStatuses())
        getTronStatus()?.let {
            blockchainStatus.putAll(it)
        }

        return blockchainStatus
    }

    private fun getTronStatus(): Map<String, Any>? {
        val wallet = walletManager.activeWallets.firstOrNull { it.token.blockchainType == BlockchainType.Tron }
        return wallet?.let {
            (adapterManager.getAdapterForWallet(wallet) as? BaseTronAdapter)?.statusInfo?.let {
                mapOf("Tron" to it)
            }
        }
    }

    private fun getBitcoinForkStatuses(): Map<String, Any> {
        val bitcoinChainStatus = LinkedHashMap<String, Any>()
        val blockchainTypesToDisplay = listOf(BlockchainType.Bitcoin, BlockchainType.BitcoinCash, BlockchainType.Dash, BlockchainType.Litecoin, BlockchainType.ECash)

        walletManager.activeWallets
                .filter { blockchainTypesToDisplay.contains(it.token.blockchainType) }
                .sortedBy { it.token.coin.name }
                .forEach { wallet ->
                    (adapterManager.getAdapterForWallet(wallet) as? BitcoinBaseAdapter)?.let { adapter ->
                        val settings = wallet.configuredToken.coinSettings
                        val settingsValue = settings.derivation?.value
                                ?: settings.bitcoinCashCoinType?.value
                        val statusTitle = "${wallet.token.coin.name}${settingsValue?.let { "-$it" } ?: ""}"
                        bitcoinChainStatus[statusTitle] = adapter.statusInfo
                    }
                }
        return bitcoinChainStatus
    }

    private fun getMarketLastSyncTimestamps(): Map<String, Any> {
        val syncInfo = marketKit.syncInfo()
        val info = LinkedHashMap<String, Any>()
        info["Coins"] = syncInfo.coinsTimestamp ?: ""
        info["Blockchains"] = syncInfo.blockchainsTimestamp ?: ""
        info["Tokens"] = syncInfo.tokensTimestamp ?: ""

        return info
    }

}
