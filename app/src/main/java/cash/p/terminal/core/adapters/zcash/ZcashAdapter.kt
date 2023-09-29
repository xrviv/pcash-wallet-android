package cash.p.terminal.core.adapters.zcash

import android.content.Context
import cash.z.ecc.android.sdk.CloseableSynchronizer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.fromHex
import cash.z.ecc.android.sdk.model.*
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import cash.p.terminal.core.*
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.entities.AccountOrigin
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.Wallet
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.math.max

class ZcashAdapter(
    context: Context,
    private val wallet: Wallet,
    restoreSettings: RestoreSettings,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendZcashAdapter {

    private val confirmationsThreshold = 10
    private val decimalCount = 8
    private val network: ZcashNetwork = ZcashNetwork.Mainnet
    private val feeChangeHeight: Long = 1_077_550
    private val lightWalletEndpoint = LightWalletEndpoint.defaultForNetwork(network)

    private val synchronizer: CloseableSynchronizer
    private val transactionsProvider: ZcashTransactionsProvider

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val accountType = (wallet.account.type as? AccountType.Mnemonic) ?: throw UnsupportedAccountException()
    private val seed = accountType.seed

    private val zcashAccount = Account.DEFAULT

    override val receiveAddress: String

    override val isMainNet: Boolean = true

    init {
        val birthday: BlockHeight?
        val walletInitMode: WalletInitMode

        when (wallet.account.origin) {
            AccountOrigin.Created -> {
                birthday = runBlocking {
                    BlockHeight.ofLatestCheckpoint(context, network)
                }
                walletInitMode = WalletInitMode.NewWallet
            }
            AccountOrigin.Restored -> {
                birthday = restoreSettings.birthdayHeight
                    ?.let { height ->
                        max(network.saplingActivationHeight.value, height)
                    }
                    ?.let {
                        BlockHeight.new(network, it)
                    }
                walletInitMode = WalletInitMode.RestoreWallet
            }
        }

        synchronizer = Synchronizer.newBlocking(
            context = context,
            zcashNetwork = network,
            alias = getValidAliasFromAccountId(wallet.account.id),
            lightWalletEndpoint = lightWalletEndpoint,
            seed = seed,
            birthday = birthday,
            walletInitMode = walletInitMode
        )

        receiveAddress = runBlocking { synchronizer.getSaplingAddress(zcashAccount) }
        transactionsProvider = ZcashTransactionsProvider(receiveAddress, synchronizer as SdkSynchronizer)
        synchronizer.onProcessorErrorHandler = ::onProcessorError
        synchronizer.onChainErrorHandler = ::onChainError
    }

    private fun defaultFee(height: Long? = null): Zatoshi {
        val value = if (height == null || height > feeChangeHeight) 1_000L else 10_000L
        return Zatoshi(value)
    }

    private var syncState: AdapterState = AdapterState.Syncing()
        set(value) {
            if (value != field) {
                field = value
                adapterStateUpdatedSubject.onNext(Unit)
            }
        }

    override fun start() {
        subscribe(synchronizer as SdkSynchronizer)
    }

    override fun stop() {
        synchronizer.close()
    }

    override fun refresh() {
    }

    override val debugInfo: String
        get() = ""

    override val balanceState: AdapterState
        get() = syncState

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceData: BalanceData
        get() = BalanceData(balance, balanceLocked)

    private val balance: BigDecimal
        get() {
            val walletBalance = synchronizer.saplingBalances.value ?: return BigDecimal.ZERO
            return walletBalance.available.convertZatoshiToZec(decimalCount)
        }

    private val balanceLocked: BigDecimal
        get() {
            val walletBalance = synchronizer.saplingBalances.value ?: return BigDecimal.ZERO
            return walletBalance.pending.convertZatoshiToZec(decimalCount)
        }

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val explorerTitle: String
        get() = "blockchair.com"

    override val transactionsState: AdapterState
        get() = syncState

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val lastBlockInfo: LastBlockInfo?
        get() = synchronizer.latestHeight?.value?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = lastBlockUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>> {
        val fromParams = from?.let {
            val transactionHash = it.transactionHash.fromHex().reversedArray()
            Triple(transactionHash, it.timestamp, it.transactionIndex)
        }

        return transactionsProvider.getTransactions(fromParams, transactionType, limit)
            .map { transactions ->
                transactions.map {
                    getTransactionRecord(it)
                }
            }
    }

    override fun getTransactionRecordsFlowable(token: Token?, transactionType: FilterTransactionType): Flowable<List<TransactionRecord>> {
        return transactionsProvider.getNewTransactionsFlowable(transactionType).map { transactions ->
            transactions.map { getTransactionRecord(it) }
        }
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/zcash/transaction/$transactionHash"

    override val availableBalance: BigDecimal
        get() {
            val available = synchronizer.saplingBalances.value?.available ?: Zatoshi(0)
            val defaultFee = defaultFee()

            return if (available <= defaultFee) {
                BigDecimal.ZERO
            } else {
                available.minus(defaultFee)
                    .convertZatoshiToZec(decimalCount)
            }
        }

    override val fee: BigDecimal
        get() = defaultFee().convertZatoshiToZec(decimalCount)

    override suspend fun validate(address: String): ZCashAddressType {
        if (address == receiveAddress) throw ZcashError.SendToSelfNotAllowed
        return when (synchronizer.validateAddress(address)) {
            is AddressType.Invalid -> throw ZcashError.InvalidAddress
            is AddressType.Transparent -> ZCashAddressType.Transparent
            is AddressType.Shielded -> ZCashAddressType.Shielded
            AddressType.Unified -> ZCashAddressType.Unified
        }
    }

    override suspend fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger): Long {
        val spendingKey = DerivationTool.getInstance().deriveUnifiedSpendingKey(seed, network, zcashAccount)
        logger.info("call synchronizer.sendToAddress")
        return synchronizer.sendToAddress(spendingKey, amount.convertZecToZatoshi(), address, memo)
    }

    // Subscribe to a synchronizer on its own scope and begin responding to events
    @OptIn(FlowPreview::class)
    private fun subscribe(synchronizer: SdkSynchronizer) {
        // Note: If any of these callback functions directly touch the UI, then the scope used here
        //       should not live longer than that UI or else the context and view tree will be
        //       invalid and lead to crashes. For now, we use a scope that is cancelled whenever
        //       synchronizer.stop is called.
        //       If the scope of the view is required for one of these, then consider using the
        //       related viewModelScope instead of the synchronizer's scope.
        //       synchronizer.coroutineScope cannot be accessed until the synchronizer is started
        val scope = synchronizer.coroutineScope
        synchronizer.transactions.distinctUntilChanged().collectWith(scope, transactionsProvider::onTransactions)
        synchronizer.status.collectWith(scope, ::onStatus)
        synchronizer.progress.collectWith(scope, ::onDownloadProgress)
        synchronizer.saplingBalances.collectWith(scope, ::onBalance)
        synchronizer.processorInfo.collectWith(scope, ::onProcessorInfo)
    }

    private fun onProcessorError(error: Throwable?): Boolean {
        error?.printStackTrace()
        return true
    }

    private fun onChainError(errorHeight: BlockHeight, rewindHeight: BlockHeight) {
    }

    private fun onStatus(status: Synchronizer.Status) {
        syncState = when (status) {
            Synchronizer.Status.STOPPED -> AdapterState.NotSynced(Exception("stopped"))
            Synchronizer.Status.DISCONNECTED -> AdapterState.NotSynced(Exception("disconnected"))
            Synchronizer.Status.SYNCING -> AdapterState.Syncing()
            Synchronizer.Status.SYNCED -> AdapterState.Synced
            else -> syncState
        }
    }

    private fun onDownloadProgress(progress: PercentDecimal) {
        syncState = AdapterState.Syncing(progress.toPercentage())
    }

    private fun onProcessorInfo(processorInfo: CompactBlockProcessor.ProcessorInfo) {
        syncState = AdapterState.Syncing()
        lastBlockUpdatedSubject.onNext(Unit)
    }

    private fun onBalance(balance: WalletBalance?) {
        balanceUpdatedSubject.onNext(Unit)
    }

    private fun getTransactionRecord(transaction: ZcashTransaction): TransactionRecord {
        val transactionHashHex = transaction.transactionHash.toReversedHex()

        return if (transaction.isIncoming) {
            BitcoinIncomingTransactionRecord(
                token = wallet.token,
                uid = transactionHashHex,
                transactionHash = transactionHashHex,
                transactionIndex = transaction.transactionIndex,
                blockHeight = transaction.minedHeight?.toInt(),
                confirmationsThreshold = confirmationsThreshold,
                timestamp = transaction.timestamp,
                fee = defaultFee(transaction.minedHeight).convertZatoshiToZec(decimalCount),
                failed = transaction.failed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = Zatoshi(transaction.value).convertZatoshiToZec(decimalCount),
                from = null,
                memo = transaction.memo,
                source = wallet.transactionSource
            )
        } else {
            BitcoinOutgoingTransactionRecord(
                token = wallet.token,
                uid = transactionHashHex,
                transactionHash = transactionHashHex,
                transactionIndex = transaction.transactionIndex,
                blockHeight = transaction.minedHeight?.toInt(),
                confirmationsThreshold = confirmationsThreshold,
                timestamp = transaction.timestamp,
                fee = defaultFee(transaction.minedHeight).convertZatoshiToZec(decimalCount),
                failed = transaction.failed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = Zatoshi(transaction.value).convertZatoshiToZec(decimalCount).negate(),
                to = transaction.toAddress,
                sentToSelf = false,
                memo = transaction.memo,
                source = wallet.transactionSource
            )
        }
    }

    enum class ZCashAddressType{
        Shielded, Transparent, Unified
    }

    sealed class ZcashError : Exception() {
        object InvalidAddress : ZcashError()
        object SendToSelfNotAllowed : ZcashError()
    }

    companion object {
        private const val ALIAS_PREFIX = "zcash_"

        private fun getValidAliasFromAccountId(accountId: String): String {
            return ALIAS_PREFIX + accountId.replace("-", "_")
        }

        fun clear(accountId: String) {
            runBlocking {
                Synchronizer.erase(App.instance, ZcashNetwork.Mainnet, getValidAliasFromAccountId(accountId))
            }
        }
    }
}
