package cash.p.terminal.core.factories

import android.content.Context
import cash.p.terminal.core.IAdapter
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.adapters.BinanceAdapter
import cash.p.terminal.core.adapters.BitcoinAdapter
import cash.p.terminal.core.adapters.BitcoinCashAdapter
import cash.p.terminal.core.adapters.DashAdapter
import cash.p.terminal.core.adapters.ECashAdapter
import cash.p.terminal.core.adapters.Eip20Adapter
import cash.p.terminal.core.adapters.EvmAdapter
import cash.p.terminal.core.adapters.EvmTransactionsAdapter
import cash.p.terminal.core.adapters.LitecoinAdapter
import cash.p.terminal.core.adapters.SolanaAdapter
import cash.p.terminal.core.adapters.SolanaTransactionConverter
import cash.p.terminal.core.adapters.SolanaTransactionsAdapter
import cash.p.terminal.core.adapters.SplAdapter
import cash.p.terminal.core.adapters.Trc20Adapter
import cash.p.terminal.core.adapters.TronAdapter
import cash.p.terminal.core.adapters.TronTransactionConverter
import cash.p.terminal.core.adapters.TronTransactionsAdapter
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.core.managers.BinanceKitManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.core.managers.TronKitManager
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.transactions.TransactionSource
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class AdapterFactory(
    private val context: Context,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val binanceKitManager: BinanceKitManager,
    private val solanaKitManager: SolanaKitManager,
    private val tronKitManager: TronKitManager,
    private val backgroundManager: BackgroundManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val coinManager: ICoinManager,
    private val evmLabelManager: EvmLabelManager,
    private val localStorage: ILocalStorage,
) {

    private fun getEvmAdapter(wallet: Wallet): IAdapter? {
        val blockchainType = evmBlockchainManager.getBlockchain(wallet.token)?.type ?: return null
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType).getEvmKitWrapper(
            wallet.account,
            blockchainType
        )

        return EvmAdapter(evmKitWrapper, coinManager)
    }

    private fun getEip20Adapter(wallet: Wallet, address: String): IAdapter? {
        val blockchainType = evmBlockchainManager.getBlockchain(wallet.token)?.type ?: return null
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType).getEvmKitWrapper(wallet.account, blockchainType)
        val baseToken = evmBlockchainManager.getBaseToken(blockchainType) ?: return null

        return Eip20Adapter(context, evmKitWrapper, address, baseToken, coinManager, wallet, evmLabelManager)
    }

    private fun getSplAdapter(wallet: Wallet, address: String): IAdapter? {
        val solanaKitWrapper = solanaKitManager.getSolanaKitWrapper(wallet.account)

        return SplAdapter(solanaKitWrapper, wallet, address)
    }

    private fun getTrc20Adapter(wallet: Wallet, address: String): IAdapter {
        val tronKitWrapper = tronKitManager.getTronKitWrapper(wallet.account)

        return Trc20Adapter(tronKitWrapper, address, wallet)
    }

    fun getAdapter(wallet: Wallet) = when (val tokenType = wallet.token.type) {
        is TokenType.Derived -> {
            when (wallet.token.blockchainType) {
                BlockchainType.Bitcoin -> {
                    val syncMode = btcBlockchainManager.syncMode(BlockchainType.Bitcoin, wallet.account.origin)
                    BitcoinAdapter(wallet, syncMode, backgroundManager, tokenType.derivation)
                }
                BlockchainType.Litecoin -> {
                    val syncMode = btcBlockchainManager.syncMode(BlockchainType.Litecoin, wallet.account.origin)
                    LitecoinAdapter(wallet, syncMode, backgroundManager, tokenType.derivation)
                }
                else -> null
            }
        }
        is TokenType.AddressTyped -> {
            if (wallet.token.blockchainType == BlockchainType.BitcoinCash) {
                val syncMode = btcBlockchainManager.syncMode(BlockchainType.BitcoinCash, wallet.account.origin)
                BitcoinCashAdapter(wallet, syncMode, backgroundManager, tokenType.type)
            }
            else null
        }
        TokenType.Native -> when (wallet.token.blockchainType) {
            BlockchainType.ECash -> {
                val syncMode = btcBlockchainManager.syncMode(BlockchainType.ECash, wallet.account.origin)
                ECashAdapter(wallet, syncMode, backgroundManager)
            }
            BlockchainType.Dash -> {
                val syncMode = btcBlockchainManager.syncMode(BlockchainType.Dash, wallet.account.origin)
                DashAdapter(wallet, syncMode, backgroundManager)
            }
            BlockchainType.Zcash -> {
                ZcashAdapter(context, wallet, restoreSettingsManager.settings(wallet.account, wallet.token.blockchainType), localStorage)
            }
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                getEvmAdapter(wallet)
            }

            BlockchainType.BinanceChain -> {
                getBinanceAdapter(wallet, "BNB")
            }

            BlockchainType.Solana -> {
                val solanaKitWrapper = solanaKitManager.getSolanaKitWrapper(wallet.account)
                SolanaAdapter(solanaKitWrapper)
            }
            BlockchainType.Tron -> {
                TronAdapter(tronKitManager.getTronKitWrapper(wallet.account))
            }

            else -> null
        }
        is TokenType.Eip20 -> {
            if (wallet.token.blockchainType == BlockchainType.Tron) {
                getTrc20Adapter(wallet, tokenType.address)
            } else {
                getEip20Adapter(wallet, tokenType.address)
            }
        }
        is TokenType.Bep2 -> getBinanceAdapter(wallet, tokenType.symbol)
        is TokenType.Spl -> getSplAdapter(wallet, tokenType.address)
        is TokenType.Unsupported -> null
    }

    private fun getBinanceAdapter(
        wallet: Wallet,
        symbol: String
    ): BinanceAdapter? {
        val query = TokenQuery(BlockchainType.BinanceChain, TokenType.Native)
        return coinManager.getToken(query)?.let { feeToken ->
            BinanceAdapter(binanceKitManager.binanceKit(wallet), symbol, feeToken, wallet)
        }
    }

    fun evmTransactionsAdapter(source: TransactionSource, blockchainType: BlockchainType): ITransactionsAdapter? {
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType).getEvmKitWrapper(source.account, blockchainType)
        val baseCoin = evmBlockchainManager.getBaseToken(blockchainType) ?: return null
        val syncSource = evmSyncSourceManager.getSyncSource(blockchainType)

        return EvmTransactionsAdapter(evmKitWrapper, baseCoin, coinManager, source, syncSource.transactionSource, evmLabelManager)
    }

    fun solanaTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val solanaKitWrapper = solanaKitManager.getSolanaKitWrapper(source.account)
        val baseToken = coinManager.getToken(TokenQuery(BlockchainType.Solana, TokenType.Native)) ?: return null
        val solanaTransactionConverter = SolanaTransactionConverter(coinManager, source, baseToken, solanaKitWrapper)

        return SolanaTransactionsAdapter(solanaKitWrapper, solanaTransactionConverter)
    }

    fun tronTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val tronKitWrapper = tronKitManager.getTronKitWrapper(source.account)
        val baseToken = coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: return null
        val tronTransactionConverter = TronTransactionConverter(coinManager, tronKitWrapper, source, baseToken, evmLabelManager)

        return TronTransactionsAdapter(tronKitWrapper, tronTransactionConverter)
    }

    fun unlinkAdapter(wallet: Wallet) {
        when (val blockchainType = wallet.transactionSource.blockchain.type) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne -> {
                val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchainType)
                evmKitManager.unlink(wallet.account)
            }
            BlockchainType.BinanceChain -> {
                binanceKitManager.unlink(wallet.account)
            }
            BlockchainType.Solana -> {
                solanaKitManager.unlink(wallet.account)
            }
            BlockchainType.Tron -> {
                tronKitManager.unlink(wallet.account)
            }
            else -> Unit
        }
    }

    fun unlinkAdapter(transactionSource: TransactionSource) {
        when (val blockchainType = transactionSource.blockchain.type) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne -> {
                val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchainType)
                evmKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Solana -> {
                solanaKitManager.unlink(transactionSource.account)
            }
            BlockchainType.Tron -> {
                tronKitManager.unlink(transactionSource.account)
            }
            else -> Unit
        }
    }
}
