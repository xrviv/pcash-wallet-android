package cash.p.terminal.modules.evmfee

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.ethereum.EvmCoinService
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.fee.FeeItem
import io.reactivex.disposables.CompositeDisposable

class EvmFeeCellViewModel(
    val feeService: IEvmFeeService,
    val gasPriceService: IEvmGasPriceService,
    val coinService: EvmCoinService
) : ViewModel() {

    private val disposable = CompositeDisposable()

    val feeLiveData = MutableLiveData<FeeItem?>()
    val viewStateLiveData = MutableLiveData<ViewState>()

    init {
        syncTransactionStatus(feeService.transactionStatus)
        feeService.transactionStatusObservable
            .subscribe { syncTransactionStatus(it) }
            .let { disposable.add(it) }
    }

    override fun onCleared() {
        disposable.clear()
    }

    private fun syncTransactionStatus(transactionStatus: DataState<Transaction>) {
        when (transactionStatus) {
            DataState.Loading -> {}
            is DataState.Error -> {
                viewStateLiveData.postValue(ViewState.Error(transactionStatus.error))
                feeLiveData.postValue(null)
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data

                if (transaction.errors.isNotEmpty()) {
                    viewStateLiveData.postValue(ViewState.Error(transaction.errors.first()))
                } else {
                    viewStateLiveData.postValue(ViewState.Success)
                }

                val feeAmountData = coinService.amountData(transactionStatus.data.gasData.fee)
                val feeViewItem = FeeItem(
                    primary = feeAmountData.primary.getFormattedPlain(),
                    secondary = feeAmountData.secondary?.getFormattedPlain()
                )
                feeLiveData.postValue(feeViewItem)
            }
        }
    }
}
