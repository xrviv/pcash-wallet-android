package cash.p.terminal.modules.evmfee

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.ethereum.EvmCoinService
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import io.reactivex.disposables.CompositeDisposable

class EvmFeeCellViewModel(
    val feeService: IEvmFeeService,
    val gasPriceService: IEvmGasPriceService,
    val coinService: EvmCoinService
) : ViewModel() {

    private val disposable = CompositeDisposable()

    val feeLiveData = MutableLiveData("")
    val viewStateLiveData = MutableLiveData<ViewState>()
    val loadingLiveData = MutableLiveData<Boolean>()

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
            DataState.Loading -> {
                loadingLiveData.postValue(true)
            }
            is DataState.Error -> {
                loadingLiveData.postValue(false)
                viewStateLiveData.postValue(ViewState.Error(transactionStatus.error))
                feeLiveData.postValue(Translator.getString(R.string.NotAvailable))
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data
                loadingLiveData.postValue(false)

                if (transaction.errors.isNotEmpty()) {
                    viewStateLiveData.postValue(ViewState.Error(transaction.errors.first()))
                } else {
                    viewStateLiveData.postValue(ViewState.Success)
                }

                val fee = coinService.amountData(transactionStatus.data.gasData.fee).getFormatted()
                feeLiveData.postValue(fee)
            }
        }
    }

}
