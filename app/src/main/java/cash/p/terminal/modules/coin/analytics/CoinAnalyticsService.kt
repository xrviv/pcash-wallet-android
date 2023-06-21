package cash.p.terminal.modules.coin.analytics

import cash.p.terminal.core.App
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.managers.SubscriptionManager
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.DataState
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import retrofit2.HttpException

class CoinAnalyticsService(
    val fullCoin: FullCoin,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val subscriptionManager: SubscriptionManager,
    private val accountManager: IAccountManager,
    appConfigProvider: AppConfigProvider,
) {
    private val disposables = CompositeDisposable()

    val analyticsLink = appConfigProvider.analyticsLink

    private val stateSubject = BehaviorSubject.create<DataState<AnalyticData>>()
    val stateObservable: Observable<DataState<AnalyticData>>
        get() = stateSubject

    val currency: Currency
        get() = currencyManager.baseCurrency

    val auditAddresses: List<String> by lazy {
        fullCoin.tokens.mapNotNull { token ->
            val tokenQuery = token.tokenQuery
            when (val tokenType = tokenQuery.tokenType) {
                is TokenType.Eip20 -> when (tokenQuery.blockchainType) {
                    BlockchainType.Ethereum -> tokenType.address
                    BlockchainType.BinanceSmartChain -> tokenType.address
                    else -> null
                }
                else -> null
            }
        }
    }

    fun blockchains(uids: List<String>): List<Blockchain> {
        return marketKit.blockchains(uids)
    }

    suspend fun start() {
        subscriptionManager.authTokenFlow.collect {
            fetch()
        }
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        disposables.clear()
    }

    private fun fetch() {
        if (!subscriptionManager.hasSubscription()) {
            preview()
        } else {
            stateSubject.onNext(DataState.Loading)

            marketKit.analyticsSingle(fullCoin.coin.uid, currency.code)
                .subscribeIO({ item ->
                    stateSubject.onNext(DataState.Success(AnalyticData(analytics = item)))
                }, {
                    handleError(it)
                }).let {
                    disposables.add(it)
                }
        }
    }

    private fun handleError(error: Throwable) {
        if (error is HttpException && (error.code() == 401 || error.code() == 403)) {
            preview()
        } else {
            stateSubject.onNext(DataState.Error(error))
        }
    }

    private fun preview() {
        val addresses = accountManager.accounts.mapNotNull {
            it.type.evmAddress(App.evmBlockchainManager.getChain(BlockchainType.Ethereum))?.hex
        }

        marketKit.analyticsPreviewSingle(fullCoin.coin.uid, addresses)
            .subscribeIO({ item ->
                stateSubject.onNext(DataState.Success(AnalyticData(analyticsPreview = item)))
            }, {
                stateSubject.onNext(DataState.Error(it))
            }).let {
                disposables.add(it)
            }
    }

    data class AnalyticData(
        val analytics: Analytics? = null,
        val analyticsPreview: AnalyticsPreview? = null
    )

}
