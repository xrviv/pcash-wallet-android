package cash.p.terminal.modules.swap.confirmation.oneinch

import cash.p.terminal.core.EvmError
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.evmfee.FeeSettingsError
import cash.p.terminal.modules.evmfee.GasData
import cash.p.terminal.modules.evmfee.GasDataError
import cash.p.terminal.modules.evmfee.GasPriceInfo
import cash.p.terminal.modules.evmfee.IEvmFeeService
import cash.p.terminal.modules.evmfee.IEvmGasPriceService
import cash.p.terminal.modules.evmfee.Transaction
import cash.p.terminal.modules.swap.SwapMainModule.OneInchSwapParameters
import cash.p.terminal.modules.swap.oneinch.OneInchKitHelper
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.oneinchkit.Swap
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class OneInchFeeService(
    private val oneInchKitHelper: OneInchKitHelper,
    private val evmKit: EthereumKit,
    private val gasPriceService: IEvmGasPriceService,
    parameters: OneInchSwapParameters,
) : IEvmFeeService {
    private val disposable = CompositeDisposable()
    private var gasPriceInfoDisposable: Disposable? = null

    private var retryDelayTimeInSeconds = 3L
    private var retryDisposable: Disposable? = null

    private val evmBalance: BigInteger
        get() = evmKit.accountState?.balance ?: BigInteger.ZERO

    var parameters: OneInchSwapParameters = parameters
        private set

    override var transactionStatus: DataState<Transaction> = DataState.Error(GasDataError.NoTransactionData)
        private set(value) {
            field = value
            transactionStatusSubject.onNext(value)
        }
    private val transactionStatusSubject = PublishSubject.create<DataState<Transaction>>()
    override val transactionStatusObservable: Observable<DataState<Transaction>> = transactionStatusSubject

    init {
        val gasPriceServiceState = gasPriceService.state
        sync(gasPriceServiceState)
        if (gasPriceServiceState.dataOrNull == null) {
            gasPriceService.stateObservable
                .subscribeIO {
                    sync(it)
                }
                .let { disposable.add(it) }
        }
    }

    override fun reset() {
        gasPriceService.setRecommended()
    }

    override fun clear() {
        disposable.clear()
        gasPriceInfoDisposable?.dispose()
        retryDisposable?.dispose()
    }

    private fun sync(gasPriceServiceState: DataState<GasPriceInfo>) {
        when (gasPriceServiceState) {
            is DataState.Error -> {
                transactionStatus = gasPriceServiceState
            }
            DataState.Loading -> {
                transactionStatus = DataState.Loading
            }
            is DataState.Success -> {
                sync(gasPriceServiceState.data)
            }
        }
    }

    private fun sync(gasPriceInfo: GasPriceInfo) {
        gasPriceInfoDisposable?.dispose()
        retryDisposable?.dispose()

        val chain: Chain = TODO()
        val receiveAddress: Address = TODO()

        oneInchKitHelper.getSwapAsync(
            chain = chain,
            receiveAddress = receiveAddress,
            fromToken = parameters.tokenFrom,
            toToken = parameters.tokenTo,
            fromAmount = parameters.amountFrom,
            recipient = parameters.recipient?.hex,
            slippagePercentage = parameters.slippage.toFloat(),
            gasPrice = gasPriceInfo.gasPrice
        )
            .subscribeIO({ swap ->
                sync(swap, gasPriceInfo)
            }, { error ->
                onError(error, gasPriceInfo)
            })
            .let { gasPriceInfoDisposable = it }
    }

    private fun sync(swap: Swap, gasPriceInfo: GasPriceInfo) {
        val swapTx = swap.transaction
        val gasData = GasData(
            gasLimit = swapTx.gasLimit,
            gasPrice = gasPriceInfo.gasPrice
        )

        parameters = parameters.copy(
            amountTo = swap.toTokenAmount.toBigDecimal().movePointLeft(swap.toToken.decimals).stripTrailingZeros()
        )

        val transactionData = TransactionData(swapTx.to, swapTx.value, swapTx.data)
        val transaction = Transaction(transactionData, gasData, gasPriceInfo.default, gasPriceInfo.warnings, gasPriceInfo.errors)

        transactionStatus = if (transaction.totalAmount > evmBalance) {
            DataState.Success(
                transaction.copy(
                    warnings = gasPriceInfo.warnings,
                    errors = gasPriceInfo.errors + FeeSettingsError.InsufficientBalance
                )
            )
        } else {
            DataState.Success(transaction)
        }
    }

    private fun onError(error: Throwable, gasPriceInfo: GasPriceInfo) {
        parameters = parameters.copy(amountTo = BigDecimal.ZERO)
        transactionStatus = DataState.Error(error)

        if (error is EvmError.CannotEstimateSwap) {
            retryDisposable = Single.timer(retryDelayTimeInSeconds, TimeUnit.SECONDS)
                .subscribeIO {
                    sync(gasPriceInfo)
                }
        }
    }
}
