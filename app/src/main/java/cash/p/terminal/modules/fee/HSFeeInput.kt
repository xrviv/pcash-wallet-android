package cash.p.terminal.modules.fee

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import java.math.BigDecimal

@Composable
fun HSFeeInput(
    coinCode: String,
    coinDecimal: Int,
    fiatDecimal: Int,
    fee: BigDecimal?,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    onClick: (() -> Unit)? = null,
) {
    CellUniversalLawrenceSection(
        listOf {
            HSFeeInputRaw(
                coinCode = coinCode,
                coinDecimal = coinDecimal,
                fiatDecimal = fiatDecimal,
                fee = fee,
                amountInputType = amountInputType,
                rate = rate,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            )
        })
}
@Composable
fun HSFeeInputRaw(
    coinCode: String,
    coinDecimal: Int,
    fiatDecimal: Int,
    fee: BigDecimal?,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val viewModel = viewModel<FeeInputViewModel>(
        factory = FeeInputModule.Factory(
            coinCode,
            coinDecimal,
            fiatDecimal
        )
    )
    val formatted = viewModel.formatted

    LaunchedEffect(fee, amountInputType, rate) {
        viewModel.fee = fee
        viewModel.amountInputType = amountInputType
        viewModel.rate = rate
        viewModel.refreshFormatted()
    }

    FeeCell(
        title = stringResource(R.string.Send_Fee),
        info = "",
        value = formatted,
        viewState = null,
        navController = null
    )
}
