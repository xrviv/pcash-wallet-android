package cash.p.terminal.core.managers

import cash.p.terminal.core.IEnabledWalletStorage
import cash.p.terminal.core.IWalletStorage
import cash.p.terminal.core.customCoinUid
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.entities.EnabledWallet
import cash.p.terminal.entities.Wallet
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

class WalletStorage(
    private val marketKit: MarketKitWrapper,
    private val storage: IEnabledWalletStorage,
) : IWalletStorage {

    override fun wallets(account: Account): List<Wallet> {
        val enabledWallets = storage.enabledWallets(account.id)

        val queries = enabledWallets.mapNotNull { TokenQuery.fromId(it.tokenQueryId) }
        val tokens = marketKit.tokens(queries)

        val blockchainUids = queries.map { it.blockchainType.uid }
        val blockchains = marketKit.blockchains(blockchainUids)

        return enabledWallets.mapNotNull { enabledWallet ->
            val tokenQuery = TokenQuery.fromId(enabledWallet.tokenQueryId) ?: return@mapNotNull null

            tokens.find { it.tokenQuery == tokenQuery }?.let { token ->
                val configuredToken = ConfiguredToken(token)
                return@mapNotNull Wallet(configuredToken, account)
            }

            if (enabledWallet.coinName != null && enabledWallet.coinCode != null && enabledWallet.coinDecimals != null) {
                val coinUid = tokenQuery.customCoinUid
                val blockchain = blockchains.firstOrNull { it.uid == tokenQuery.blockchainType.uid } ?: return@mapNotNull null

                val token = Token(
                    coin = io.horizontalsystems.marketkit.models.Coin(coinUid, enabledWallet.coinName, enabledWallet.coinCode),
                    blockchain = blockchain,
                    type = tokenQuery.tokenType,
                    decimals = enabledWallet.coinDecimals
                )

                val configuredToken = ConfiguredToken(token)

                Wallet(configuredToken, account)
            } else {
                null
            }
        }
    }

    override fun save(wallets: List<Wallet>) {
        val enabledWallets = mutableListOf<EnabledWallet>()

        wallets.forEachIndexed { index, wallet ->

            enabledWallets.add(
                enabledWallet(wallet, index)
            )
        }

        storage.save(enabledWallets)
    }

    override fun delete(wallets: List<Wallet>) {
        storage.delete(wallets.map { enabledWallet(it) })
    }

    override fun handle(newEnabledWallets: List<EnabledWallet>) {
        storage.save(newEnabledWallets)
    }

    override fun clear() {
        storage.deleteAll()
    }

    private fun enabledWallet(wallet: Wallet, index: Int? = null): EnabledWallet {
        return EnabledWallet(
            wallet.token.tokenQuery.id,
            wallet.account.id,
            index,
            wallet.coin.name,
            wallet.coin.code,
            wallet.decimal
        )
    }
}
