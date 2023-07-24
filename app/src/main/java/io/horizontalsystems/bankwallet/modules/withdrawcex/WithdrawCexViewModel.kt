package cash.p.terminal.modules.withdrawcex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexWithdrawNetwork
import cash.p.terminal.core.providers.CoinzixCexProvider
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.coinzixverify.CoinzixVerificationMode
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.fee.FeeItem
import cash.p.terminal.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch
import java.math.BigDecimal

class WithdrawCexViewModel(
    val cexAsset: CexAsset,
    private var network: CexWithdrawNetwork,
    private val xRateService: XRateService,
    private val amountService: CexWithdrawAmountService,
    private val addressService: CexWithdrawAddressService,
    private val cexProvider: CoinzixCexProvider,
    private val contactsRepository: ContactsRepository
) : ViewModel() {
    private val coinUid = cexAsset.coin?.uid

    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    val coinMaxAllowedDecimals = cexAsset.decimals
    val networks = cexAsset.withdrawNetworks
    val networkSelectionEnabled = networks.size > 1

    val blockchainType get() = network.blockchain?.type
    fun hasContacts() = blockchainType?.let { blockchainType ->
        contactsRepository.getContactsFiltered(blockchainType).isNotEmpty()
    } ?: false

    var coinRate by mutableStateOf(coinUid?.let { xRateService.getRate(it) })
        private set

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeItem = FeeItem("", null)
    private var feeFromAmount = false

    var uiState by mutableStateOf(
        WithdrawCexUiState(
            networkName = network.networkName,
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            feeItem = feeItem,
            feeFromAmount = feeFromAmount,
            canBeSend = amountState.canBeSend && addressState.canBeSend
        )
    )
        private set

    init {
        coinUid?.let {
            viewModelScope.launch {
                xRateService.getRateFlow(it).collect {
                    coinRate = it
                }
            }
        }

        viewModelScope.launch {
            amountService.stateFlow.collect {
                handleUpdatedAmountState(it)
            }
        }

        viewModelScope.launch {
            addressService.stateFlow.collect {
                handleUpdatedAddressState(it)
            }
        }
    }

    private fun handleUpdatedAmountState(amountState: CexWithdrawAmountService.State) {
        this.amountState = amountState
        feeItem = feeItem(amountState)

        emitState()
    }

    private fun feeItem(amountState: CexWithdrawAmountService.State): FeeItem {
        val coinAmount = App.numberFormatter.formatCoinFull(
            amountState.fee,
            cexAsset.id,
            coinMaxAllowedDecimals
        )
        val currencyAmount = coinRate?.let { rate ->
            rate.copy(value = amountState.fee.times(rate.value)).getFormattedFull()
        }

        return FeeItem(coinAmount, currencyAmount)
    }

    private fun handleUpdatedAddressState(addressState: CexWithdrawAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = WithdrawCexUiState(
                networkName = network.networkName,
                availableBalance = amountState.availableBalance,
                amountCaution = amountState.amountCaution,
                addressError = addressState.addressError,
                feeItem = feeItem,
                feeFromAmount = feeFromAmount,
                canBeSend = amountState.canBeSend && addressState.canBeSend
            )
        }
    }

    fun onEnterAmount(v: BigDecimal?) {
        amountService.setAmount(v)
    }

    fun onEnterAddress(v: String) {
        addressService.setAddress(Address(v))
    }

    fun onSelectNetwork(network: CexWithdrawNetwork) {
        this.network = network
        amountService.setNetwork(network)
    }

    fun onSelectFeeFromAmount(feeFromAmount: Boolean) {
        this.feeFromAmount = feeFromAmount
        amountService.setFeeFromAmount(feeFromAmount)
    }

    fun getConfirmationData(): ConfirmationData {
        val amount = amountState.amount ?: BigDecimal.ZERO
        val coinAmount = App.numberFormatter.formatCoinFull(
            amount,
            cexAsset.id,
            coinMaxAllowedDecimals
        )
        val currencyAmount = coinRate?.let { rate ->
            rate.copy(value = amount.times(rate.value))
                .getFormattedFull()
        }

        val address = addressState.address!!
        val contact = contactsRepository.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()

        return ConfirmationData(
            assetName = cexAsset.name,
            coinAmount = coinAmount,
            currencyAmount = currencyAmount,
            coinIconUrl = cexAsset.coin?.imageUrl,
            address = address,
            contact = contact,
            blockchainType = null,
            networkName = network.networkName,
            feeItem = feeItem
        )
    }

    suspend fun confirm(): CoinzixVerificationMode.Withdraw {
        return cexProvider.withdraw(
            cexAsset.id,
            network.id,
            addressState.address!!.hex,
            amountState.amount!!
        )
    }
}

data class ConfirmationData(
    val assetName: String,
    val coinAmount: String,
    val currencyAmount: String?,
    val coinIconUrl: String?,
    val address: Address,
    val contact: Contact?,
    val blockchainType: BlockchainType?,
    val networkName: String?,
    val feeItem: FeeItem
)

data class WithdrawCexUiState(
    val networkName: String?,
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val feeItem: FeeItem,
    val feeFromAmount: Boolean,
    val addressError: Throwable?,
    val canBeSend: Boolean
)