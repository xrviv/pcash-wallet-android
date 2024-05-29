package cash.p.terminal.modules.watchaddress.selectblockchains

import cash.p.terminal.R
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.core.alternativeImageUrl
import cash.p.terminal.core.badge
import cash.p.terminal.core.description
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statAccountType
import cash.p.terminal.entities.AccountType
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.modules.restoreaccount.restoreblockchains.CoinViewItem
import cash.p.terminal.modules.watchaddress.WatchAddressService
import io.horizontalsystems.marketkit.models.Token

class SelectBlockchainsViewModel(
    private val accountType: AccountType,
    private val accountName: String?,
    private val service: WatchAddressService
) : ViewModelUiState<SelectBlockchainsUiState>() {

    private var title: Int = R.string.Watch_Select_Blockchains
    private var coinViewItems = listOf<CoinViewItem<Token>>()
    private var selectedCoins = setOf<Token>()
    private var accountCreated = false

    init {
        val tokens = service.tokens(accountType)

        when (accountType) {
            is AccountType.SolanaAddress,
            is AccountType.TronAddress,
            is AccountType.BitcoinAddress,
            is AccountType.TonAddress,
            is AccountType.Cex,
            is AccountType.Mnemonic,
            is AccountType.EvmPrivateKey -> Unit // N/A
            is AccountType.EvmAddress -> {
                title = R.string.Watch_Select_Blockchains
                coinViewItems = tokens.map {
                    coinViewItemForBlockchain(it)
                }
            }

            is AccountType.HdExtendedKey -> {
                title = R.string.Watch_Select_Coins
                coinViewItems = tokens.map {
                    coinViewItemForToken(it, label = it.badge)
                }
            }
        }

        emitState()
    }

    override fun createState() = SelectBlockchainsUiState(
        title = title,
        coinViewItems = coinViewItems,
        submitButtonEnabled = selectedCoins.isNotEmpty(),
        accountCreated = accountCreated
    )

    private fun coinViewItemForBlockchain(token: Token): CoinViewItem<Token> {
        val blockchain = token.blockchain
        return CoinViewItem(
            item = token,
            imageSource = ImageSource.Remote(blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
            title = blockchain.name,
            subtitle = blockchain.description,
            enabled = false
        )
    }

    private fun coinViewItemForToken(token: Token, label: String?): CoinViewItem<Token> {
        return CoinViewItem(
            item = token,
            imageSource = ImageSource.Remote(token.fullCoin.coin.imageUrl, R.drawable.coin_placeholder, token.fullCoin.coin.alternativeImageUrl),
            title = token.fullCoin.coin.code,
            subtitle = token.fullCoin.coin.name,
            enabled = false,
            label = label
        )
    }

    fun onToggle(token: Token) {
        selectedCoins = if (selectedCoins.contains(token))
            selectedCoins.toMutableSet().also { it.remove(token) }
        else
            selectedCoins.toMutableSet().also { it.add(token) }

        coinViewItems = coinViewItems.map { viewItem ->
            viewItem.copy(enabled = selectedCoins.contains(viewItem.item))
        }

        emitState()
    }

    fun onClickWatch() {
        service.watchTokens(accountType, selectedCoins.toList(), accountName)
        accountCreated = true
        emitState()

        stat(page = StatPage.WatchWallet, event = StatEvent.WatchWallet(accountType.statAccountType))
    }

}

data class SelectBlockchainsUiState(
    val title: Int,
    val coinViewItems: List<CoinViewItem<Token>>,
    val submitButtonEnabled: Boolean,
    val accountCreated: Boolean
)
