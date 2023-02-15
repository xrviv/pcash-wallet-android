package cash.p.terminal.modules.send.evm

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendEthereumAdapter
import cash.p.terminal.core.Warning
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.send.SendAmountAdvancedService
import cash.p.terminal.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import cash.p.terminal.modules.swap.uniswap.UniswapModule
import cash.p.terminal.modules.xrate.XRateService
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode


data class SendEvmData(
    val transactionData: TransactionData,
    val additionalInfo: AdditionalInfo? = null,
    val warnings: List<Warning> = listOf()
) {
    sealed class AdditionalInfo : Parcelable {
        @Parcelize
        class Send(val info: SendInfo) : AdditionalInfo()

        @Parcelize
        class Uniswap(val info: UniswapInfo) : AdditionalInfo()

        @Parcelize
        class OneInchSwap(val info: OneInchSwapInfo) : AdditionalInfo()

        @Parcelize
        class WalletConnectRequest(val info: WalletConnectInfo) : AdditionalInfo()

        val sendInfo: SendInfo?
            get() = (this as? Send)?.info

        val uniswapInfo: UniswapInfo?
            get() = (this as? Uniswap)?.info

        val oneInchSwapInfo: OneInchSwapInfo?
            get() = (this as? OneInchSwap)?.info

        val walletConnectInfo: WalletConnectInfo?
            get() = (this as? WalletConnectRequest)?.info
    }

    @Parcelize
    data class SendInfo(
        val domain: String?,
        val nftShortMeta: NftShortMeta? = null
    ) : Parcelable

    @Parcelize
    data class NftShortMeta(
        val nftName: String,
        val previewImageUrl: String?
    ) : Parcelable

    @Parcelize
    data class WalletConnectInfo(
        val dAppName: String?
    ) : Parcelable

    @Parcelize
    data class UniswapInfo(
        val estimatedOut: BigDecimal,
        val estimatedIn: BigDecimal,
        val slippage: String? = null,
        val deadline: String? = null,
        val recipientDomain: String? = null,
        val price: String? = null,
        val priceImpact: UniswapModule.PriceImpactViewItem? = null,
        val gasPrice: String? = null,
    ) : Parcelable

    @Parcelize
    data class OneInchSwapInfo(
        val tokenFrom: Token,
        val tokenTo: Token,
        val amountFrom: BigDecimal,
        val estimatedAmountTo: BigDecimal,
        val slippage: BigDecimal,
        val recipient: Address?,
        val price: String? = null
    ) : Parcelable
}

object SendEvmModule {

    const val transactionDataKey = "transactionData"
    const val additionalInfoKey = "additionalInfo"
    const val blockchainTypeKey = "blockchainType"
    const val backButtonKey = "backButton"
    const val sendNavGraphIdKey = "sendNavGraphId_key"

    @Parcelize
    data class TransactionDataParcelable(
        val toAddress: String,
        val value: BigInteger,
        val input: ByteArray
    ) : Parcelable {
        constructor(transactionData: TransactionData) : this(
            transactionData.to.hex,
            transactionData.value,
            transactionData.input
        )
    }


    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        val adapter by lazy {
            App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                EvmKitWrapperHoldingViewModel::class.java -> {
                    EvmKitWrapperHoldingViewModel(adapter.evmKitWrapper) as T
                }
                SendEvmViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val amountService = SendAmountAdvancedService(
                        adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
                        wallet.token,
                        amountValidator
                    )
                    val addressService = SendEvmAddressService()
                    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

                    SendEvmViewModel(
                        wallet,
                        wallet.token,
                        adapter,
                        xRateService,
                        amountService,
                        addressService,
                        coinMaxAllowedDecimals
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
