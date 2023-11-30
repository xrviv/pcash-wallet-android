package cash.p.terminal.modules.send

import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.isNative
import cash.p.terminal.modules.amount.AmountValidator
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.BigInteger

class SendAmountAdvancedService(
    private val availableBalance: BigDecimal,
    private val token: Token,
    private val amountValidator: AmountValidator
) {
    private var amount: BigDecimal? = null
    private var amountCaution: HSCaution? = null
    private var evmAmount: BigInteger? = null

    private val _stateFlow = MutableStateFlow(
        State(
            amountCaution = amountCaution,
            availableBalance = availableBalance,
            canBeSend = false,
            evmAmount = evmAmount,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        validateAmount()
        refreshEvmAmount()

        emitState()
    }

    private fun emitState() {
        val tmpAmountCaution = amountCaution

        val canBeSend = evmAmount != null
            && (tmpAmountCaution == null || tmpAmountCaution.isWarning())

        _stateFlow.update {
            State(
                evmAmount = evmAmount,
                amountCaution = amountCaution,
                availableBalance = availableBalance,
                canBeSend = canBeSend
            )
        }
    }

    private fun validateAmount() {
        amountCaution = amountValidator.validate(
            amount,
            token.coin.code,
            availableBalance,
            leaveSomeBalanceForFee = isCoinUsedForFee()
        )
    }

    private fun refreshEvmAmount() {
        val tmpAmount = amount
        evmAmount = if (tmpAmount != null && tmpAmount > BigDecimal.ZERO) {
            tmpAmount.movePointRight(token.decimals).toBigInteger()
        } else {
            null
        }
    }

    private fun isCoinUsedForFee(): Boolean {
        return token.type.isNative
    }

    data class State(
        val amountCaution: HSCaution?,
        val availableBalance: BigDecimal,
        val canBeSend: Boolean,
        val evmAmount: BigInteger?
    )
}
