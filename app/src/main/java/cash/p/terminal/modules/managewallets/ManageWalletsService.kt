package cash.p.terminal.modules.managewallets

import cash.p.terminal.core.Clearable
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.eligibleTokens
import cash.p.terminal.core.isCustom
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.restoreSettingTypes
import cash.p.terminal.core.sortedByFilter
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class ManageWalletsService(
    private val marketKit: MarketKitWrapper,
    private val walletManager: IWalletManager,
    accountManager: IAccountManager,
    private val restoreSettingsService: RestoreSettingsService,
) : Clearable {

    val itemsObservable = PublishSubject.create<List<Item>>()
    var items: List<Item> = listOf()
        private set(value) {
            field = value
            itemsObservable.onNext(value)
        }

    val accountType: AccountType?
        get() = account?.type

    private val account: Account? = accountManager.activeAccount
    private var wallets = setOf<Wallet>()
    private var fullCoins = listOf<FullCoin>()
    private var sortedItems = listOf<Item>()

    private val disposables = CompositeDisposable()

    private var filter: String = ""

    init {
        walletManager.activeWalletsUpdatedObservable
            .subscribeIO {
                handleUpdated(it)
            }
            .let {
                disposables.add(it)
            }

        restoreSettingsService.approveSettingsObservable
            .subscribeIO {
                enable(ConfiguredToken(it.token), it.settings)
            }.let {
                disposables.add(it)
            }

        sync(walletManager.activeWallets)
        syncFullCoins()
        sortItems()
        syncState()
    }

    private fun isEnabled(configuredToken: ConfiguredToken): Boolean {
        return wallets.any { it.configuredToken == configuredToken }
    }

    private fun sync(walletList: List<Wallet>) {
        wallets = walletList.toSet()
    }

    private fun fetchFullCoins(): List<FullCoin> {
        return if (filter.isBlank()) {
            val account = this.account ?: return emptyList()
            val featuredFullCoins = marketKit.fullCoins("", 100).toMutableList()
                .filter { it.eligibleTokens(account.type).isNotEmpty() }

            val featuredCoins = featuredFullCoins.map { it.coin }
            val enabledFullCoins = marketKit.fullCoins(
                coinUids = wallets.filter { !featuredCoins.contains(it.coin) }.map { it.coin.uid }
            )
            val customFullCoins = wallets.filter { it.token.isCustom }.map { it.token.fullCoin }

            featuredFullCoins + enabledFullCoins + customFullCoins
        } else if (isContractAddress(filter)) {
            val tokens = marketKit.tokens(filter)
            val coinUids = tokens.map { it.coin.uid }
            marketKit.fullCoins(coinUids)
        } else {
            marketKit.fullCoins(filter, 20)
        }
    }

    private fun isContractAddress(filter: String) = try {
        AddressValidator.validate(filter)
        true
    } catch (e: AddressValidator.AddressValidationException) {
        false
    }

    private fun syncFullCoins() {
        fullCoins = fetchFullCoins()
    }

    private fun sortItems() {
        fullCoins = fullCoins.sortedByFilter(filter)
        sortedItems = fullCoins
            .map { getItemsForFullCoin(it) }
            .flatten()
            .sortedByDescending { it.enabled }
    }

    private fun getItemsForFullCoin(fullCoin: FullCoin): List<Item> {
        val accountType = account?.type ?: return listOf()

        val items = mutableListOf<Item>()
        fullCoin.eligibleTokens(accountType).forEach { token ->
            items.add(getItemForConfiguredToken(ConfiguredToken(token)))
        }

        return items
    }

    private fun getItemForConfiguredToken(configuredToken: ConfiguredToken): Item {
        val enabled = isEnabled(configuredToken)

        return Item(
                configuredToken = configuredToken,
                enabled = enabled,
                hasInfo = hasInfo(configuredToken.token, enabled)
        )
    }

    private fun hasInfo(token: Token, enabled: Boolean) = when (token.type) {
        is TokenType.Native -> token.blockchainType is BlockchainType.Zcash && enabled
        is TokenType.Derived,
        is TokenType.AddressTyped,
        is TokenType.Eip20,
        is TokenType.Bep2,
        is TokenType.Spl -> true
        else -> false
    }

    private fun syncState() {
        items = sortedItems
    }

    private fun handleUpdated(wallets: List<Wallet>) {
        sync(wallets)

        val newFullCons = fetchFullCoins()
        if (newFullCons.size > fullCoins.size) {
            fullCoins = newFullCons
            sortItems()
        }

        syncState()
    }

    private fun updateSortedItems(configuredToken: ConfiguredToken, enable: Boolean) {
        sortedItems = sortedItems.map { item ->
            if (item.configuredToken == configuredToken) {
                item.copy(enabled = enable)
            } else {
                item
            }
        }
    }

    private fun enable(configuredToken: ConfiguredToken, restoreSettings: RestoreSettings) {
        val account = this.account ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsService.save(restoreSettings, account, configuredToken.token.blockchainType)
        }

        walletManager.save(listOf(Wallet(configuredToken, account)))

        updateSortedItems(configuredToken, true)
    }

    fun setFilter(filter: String) {
        this.filter = filter

        syncFullCoins()
        sortItems()
        syncState()
    }

    fun enable(configuredToken: ConfiguredToken) {
        val account = this.account ?: return

        if (configuredToken.token.blockchainType.restoreSettingTypes.isNotEmpty()) {
            restoreSettingsService.approveSettings(configuredToken.token, account)
        } else {
            enable(configuredToken, RestoreSettings())
        }
    }

    fun disable(configuredToken: ConfiguredToken) {
        wallets.firstOrNull { it.configuredToken == configuredToken }?.let {
            walletManager.delete(listOf(it))
            updateSortedItems(configuredToken, false)
        }
    }

    override fun clear() {
        disposables.clear()
    }

    data class Item(
        val configuredToken: ConfiguredToken,
        val enabled: Boolean,
        val hasInfo: Boolean
    ) {
        val token = configuredToken.token
    }
}
