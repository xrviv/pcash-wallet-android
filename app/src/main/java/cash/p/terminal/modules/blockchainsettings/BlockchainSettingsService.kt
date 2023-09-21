package cash.p.terminal.modules.blockchainsettings

import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.modules.blockchainsettings.BlockchainSettingsModule.BlockchainItem
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class BlockchainSettingsService(
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val solanaRpcSourceManager: SolanaRpcSourceManager,
) {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var blockchainItems: List<BlockchainItem> = listOf()
        private set(value) {
            field = value
            blockchainItemsSubject.onNext(value)
        }

    private val blockchainItemsSubject = BehaviorSubject.create<List<BlockchainItem>>()
    val blockchainItemsObservable: Observable<List<BlockchainItem>>
        get() = blockchainItemsSubject


    fun start() {
        btcBlockchainManager.restoreModeUpdatedObservable
            .subscribeIO {
                syncBlockchainItems()
            }.let {
                disposables.add(it)
            }

        btcBlockchainManager.transactionSortModeUpdatedObservable
            .subscribeIO {
                syncBlockchainItems()
            }.let {
                disposables.add(it)
            }

        evmSyncSourceManager.syncSourceObservable
            .subscribeIO {
                syncBlockchainItems()
            }.let {
                disposables.add(it)
            }

        solanaRpcSourceManager.rpcSourceUpdateObservable
            .subscribeIO {
                syncBlockchainItems()
            }.let {
                disposables.add(it)
            }

        syncBlockchainItems()
    }

    fun stop() {
        disposables.clear()
    }

    private fun syncBlockchainItems() {
        val btcBlockchainItems = btcBlockchainManager.allBlockchains.map { blockchain ->
            val restoreMode = btcBlockchainManager.restoreMode(blockchain.type)
            BlockchainItem.Btc(blockchain, restoreMode)
        }

        val evmBlockchainItems = evmBlockchainManager.allBlockchains.map { blockchain ->
            val syncSource = evmSyncSourceManager.getSyncSource(blockchain.type)
            BlockchainItem.Evm(blockchain, syncSource)
        }

        val solanaBlockchainItems = mutableListOf<BlockchainItem>()

        solanaRpcSourceManager.blockchain?.let {
            solanaBlockchainItems.add(BlockchainItem.Solana(it, solanaRpcSourceManager.rpcSource))
        }

        blockchainItems = (btcBlockchainItems + evmBlockchainItems + solanaBlockchainItems).sortedBy { it.order }
    }

}
