package cash.p.terminal.modules.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.Account
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal
import java.util.*

object TransactionsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return TransactionsViewModel(
                TransactionsService(
                    TransactionRecordRepository(App.transactionAdapterManager),
                    TransactionsRateRepository(App.currencyManager, App.marketKit),
                    TransactionSyncStateRepository(App.transactionAdapterManager),
                    App.contactsRepository,
                    App.transactionAdapterManager,
                    App.walletManager,
                    TransactionFilterService(),
                    NftMetadataService(App.nftMetadataManager)
                ),
                TransactionViewItemFactory(App.evmLabelManager, App.contactsRepository)
            ) as T
        }
    }
}

data class TransactionLockInfo(
    val lockedUntil: Date,
    val originalAddress: String,
    val amount: BigDecimal?
)

sealed class TransactionStatus {
    object Pending : TransactionStatus()
    class Processing(val progress: Float) : TransactionStatus() //progress in 0.0 .. 1.0
    object Completed : TransactionStatus()
    object Failed : TransactionStatus()
}

data class TransactionWallet(
    val token: Token?,
    val source: TransactionSource,
    val badge: String?
)

data class TransactionSource(
    val blockchain: Blockchain,
    val account: Account,
    val tokenType: TokenType
)
