package cash.p.terminal.modules.depositcex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexDepositNetwork
import cash.p.terminal.core.providers.CexProviderManager
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.receive.address.ReceiveAddressModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class DepositAddressViewModel(
    private val cexAsset: CexAsset,
    private val network: CexDepositNetwork?,
    cexProviderManager: CexProviderManager
) : ViewModel() {
    private val cexProvider = cexProviderManager.cexProviderFlow.value

    private var viewState: ViewState = ViewState.Loading
    private var address = ""
    private var amount: BigDecimal? = null
    private var memo: String? = null
    private val networkName = network?.name ?: cexAsset.depositNetworks.firstOrNull()?.name ?: ""
    private val watchAccount = false

    var uiState by mutableStateOf(
        ReceiveAddressModule.UiState(
            viewState = viewState,
            address = address,
            networkName = networkName,
            watchAccount = watchAccount,
            additionalItems = getAdditionalData(),
            amount = amount,
            alertText = getAlertText(memo != null)
        )
    )

    init {
        setInitialData()
    }

    private fun setInitialData() {
        viewState = ViewState.Loading
        emitState()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cexAddress = cexProvider?.getAddress(cexAsset.id, network?.id)
                if (cexAddress == null) {
                    viewState = ViewState.Error(Throwable("No address"))
                } else {
                    if (cexAddress.tag.isNotBlank()) {
                        memo = cexAddress.tag
                    }
                    address = cexAddress.address
                    viewState = ViewState.Success
                }
            } catch (t: Throwable) {
                viewState = ViewState.Error(t)
            }
            emitState()
        }
    }

    private fun emitState() {
        uiState = ReceiveAddressModule.UiState(
            viewState = viewState,
            address = address,
            networkName = networkName,
            watchAccount = watchAccount,
            additionalItems = getAdditionalData(),
            amount = amount,
            alertText = getAlertText(memo != null)
        )
    }

    private fun getAdditionalData(): List<ReceiveAddressModule.AdditionalData> {
        val items = mutableListOf<ReceiveAddressModule.AdditionalData>()

        memo?.let {
            items.add(
                ReceiveAddressModule.AdditionalData.Memo(
                    value = it
                )
            )
        }

        amount?.let {
            items.add(
                ReceiveAddressModule.AdditionalData.Amount(
                    value = it.toString()
                )
            )
        }

        return items
    }

    private fun getAlertText(hasMemo: Boolean): ReceiveAddressModule.AlertText {
        return when {
            hasMemo -> ReceiveAddressModule.AlertText.Critical(
                Translator.getString(R.string.Balance_Receive_AddressMemoAlert)
            )

            else -> ReceiveAddressModule.AlertText.Normal(
                Translator.getString(R.string.Balance_Receive_AddressAlert)
            )
        }
    }

    fun onErrorClick() {
        setInitialData()
    }

    fun setAmount(amount: BigDecimal?) {
        amount?.let {
            if (it <= BigDecimal.ZERO) {
                this.amount = null
                emitState()
                return
            }
        }
        this.amount = amount
        emitState()
    }

    class Factory(private val cexAsset: CexAsset, private val network: CexDepositNetwork?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DepositAddressViewModel(cexAsset, network, App.cexProviderManager) as T
        }
    }
}
