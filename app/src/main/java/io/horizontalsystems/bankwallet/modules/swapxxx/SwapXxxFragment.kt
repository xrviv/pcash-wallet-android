package cash.p.terminal.modules.swapxxx

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.slideFromBottomForResult
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.ui.compose.ColoredTextStyle
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellowWithSpinner
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_grey
import cash.p.terminal.ui.compose.components.body_grey50
import cash.p.terminal.ui.compose.components.headline1_grey
import cash.p.terminal.ui.compose.components.subhead1_jacob
import cash.p.terminal.ui.compose.components.subhead1_leah
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class SwapXxxFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapXxxScreen(navController)
    }
}

@Composable
fun SwapXxxScreen(navController: NavController) {
    val viewModel = viewModel<SwapXxxViewModel>(factory = SwapXxxViewModel.Factory())
    val uiState = viewModel.uiState

    Yyy(
        uiState = uiState,
        onClickClose = navController::popBackStack,
        onClickCoinFrom = {
            val dex = SwapMainModule.Dex(
                blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
                provider = SwapMainModule.OneInchProvider
            )
            navController.slideFromBottomForResult<SwapMainModule.CoinBalanceItem>(
                R.id.selectSwapCoinDialog,
                dex
            ) {
                viewModel.onSelectTokenFrom(it.token)
            }
        },
        onClickCoinTo = {
            val dex = SwapMainModule.Dex(
                blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
                provider = SwapMainModule.OneInchProvider
            )
            navController.slideFromBottomForResult<SwapMainModule.CoinBalanceItem>(
                R.id.selectSwapCoinDialog,
                dex
            ) {
                viewModel.onSelectTokenTo(it.token)
            }
        },
        onSwitchPairs = viewModel::onSwitchPairs,
        onEnterAmount = viewModel::onEnterAmount
    )
}

@Composable
private fun Yyy(
    uiState: SwapXxxUiState,
    onClickClose: () -> Unit,
    onClickCoinFrom: () -> Unit,
    onClickCoinTo: () -> Unit,
    onSwitchPairs: () -> Unit,
    onEnterAmount: (BigDecimal?) -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClickClose
                    )
                ),
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(modifier = Modifier.padding(it)) {
            VSpacer(height = 12.dp)
            SwapInput(
                coinAmountHint = uiState.coinAmountHint,
                currencyAmountHint = uiState.currencyAmountHint,
                spendingCoinAmount = uiState.spendingCoinAmount,
                spendingCurrencyAmount = uiState.spendingCurrencyAmount,
                onSwitchPairs = onSwitchPairs,
                receivingCoinAmount = uiState.receivingCoinAmount,
                receivingCurrencyAmount = uiState.receivingCurrencyAmount,
                onValueChange = onEnterAmount,
                onClickCoinFrom = onClickCoinFrom,
                onClickCoinTo = onClickCoinTo,
                tokenFrom = uiState.tokenFrom,
                tokenTo = uiState.tokenTo
            )

            VSpacer(height = 24.dp)
            if (uiState.calculating) {
                ButtonPrimaryYellowWithSpinner(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Alert_Loading),
                    enabled = false,
                    onClick = { /*TODO*/ }
                )
            } else {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Swap_Proceed),
                    enabled = uiState.swapEnabled,
                    onClick = { /*TODO*/ }
                )
            }
        }
    }
}

@Composable
private fun SwapInput(
    coinAmountHint: String,
    currencyAmountHint: String,
    spendingCoinAmount: BigDecimal?,
    spendingCurrencyAmount: String,
    onSwitchPairs: () -> Unit,
    receivingCoinAmount: BigDecimal?,
    receivingCurrencyAmount: String,
    onValueChange: (BigDecimal?) -> Unit,
    onClickCoinFrom: () -> Unit,
    onClickCoinTo: () -> Unit,
    tokenFrom: Token?,
    tokenTo: Token?,
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding()
        ) {
            Xxx(
                coinAmountHint = coinAmountHint,
                currencyAmountHint = currencyAmountHint,
                coinAmount = spendingCoinAmount,
                currencyAmount = spendingCurrencyAmount,
                onValueChange = onValueChange,
                token = tokenFrom,
                onClickCoin = onClickCoinFrom
            )
            Xxx(
                coinAmountHint = coinAmountHint,
                currencyAmountHint = currencyAmountHint,
                coinAmount = receivingCoinAmount,
                currencyAmount = receivingCurrencyAmount,
                onValueChange = { },
                enabled = false,
                token = tokenTo,
                onClickCoin = onClickCoinTo
            )
        }
        Divider(
            modifier = Modifier.align(Alignment.Center),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
        ButtonSecondaryCircle(
            modifier = Modifier.align(Alignment.Center),
            icon = R.drawable.ic_arrow_down_20,
            onClick = onSwitchPairs
        )
    }
}

@Composable
private fun Xxx(
    coinAmountHint: String,
    currencyAmountHint: String,
    coinAmount: BigDecimal?,
    currencyAmount: String,
    onValueChange: (BigDecimal?) -> Unit,
    enabled: Boolean = true,
    token: Token?,
    onClickCoin: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AmountInput(coinAmount, coinAmountHint, onValueChange, enabled)
            VSpacer(height = 3.dp)
            if (currencyAmount.isNotBlank()) {
                body_grey(text = currencyAmount)
            } else {
                body_grey50(text = currencyAmountHint)
            }
        }
        HSpacer(width = 8.dp)
        Row(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClickCoin,
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinImage(
                iconUrl = token?.coin?.imageUrl,
                placeholder = token?.iconPlaceholder,
                modifier = Modifier.size(32.dp)
            )
            HSpacer(width = 8.dp)
            if (token != null) {
                subhead1_leah(text = token.coin.code)
            } else {
                subhead1_jacob(text = stringResource(R.string.Swap_TokenSelectorTitle))
            }
            HSpacer(width = 8.dp)
            Icon(
                painter = painterResource(R.drawable.ic_arrow_big_down_20),
                contentDescription = "",
                tint = ComposeAppTheme.colors.grey
            )
        }
    }
}

@Composable
private fun AmountInput(
    value: BigDecimal?,
    hint: String,
    onValueChange: (BigDecimal?) -> Unit,
    enabled: Boolean,
) {
    var text by remember(value) {
        mutableStateOf(value?.toPlainString() ?: "")
    }
    BasicTextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = {
            try {
                val amount = if (it.isBlank()) {
                    null
                } else {
                    it.toBigDecimal()
                }
                text = it
                onValueChange.invoke(amount)
            } catch (e: Exception) {

            }
        },
        enabled = enabled,
        textStyle = ColoredTextStyle(
            color = ComposeAppTheme.colors.leah, textStyle = ComposeAppTheme.typography.headline1
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
        decorationBox = { innerTextField ->
            if (text.isEmpty()) {
                headline1_grey(text = hint)
            }
            innerTextField()
        },
    )
}

//@Preview
//@Composable
//fun SwapInputPreview() {
//    ComposeAppTheme(darkTheme = true) {
//        SwapInput(
//            coinAmountHint = "0",
//            currencyAmountHint = "$0",
//            spendingCoinAmount = BigDecimal.ZERO,
//            spendingCurrencyAmount = "$123.30",
//            onSwitchPairs = {},
//            receivingCoinAmount = BigDecimal(12),
//            receivingCurrencyAmount = "$123",
//            onValueChange = {},
//            onClickCoinFrom = {},
//            onClickCoinTo = {},
//            tokenFrom = null,
//            tokenTo = null
//        )
//    }
//}

@Preview
@Composable
fun ScreenPreview() {
    ComposeAppTheme(darkTheme = true) {
        val uiState = SwapXxxUiState(
            coinAmountHint = "0",
            currencyAmountHint = "$0",
            spendingCoinAmount = BigDecimal.ZERO,
            spendingCurrencyAmount = "$123.30",
            receivingCoinAmount = BigDecimal(12),
            receivingCurrencyAmount = "$123",
            tokenFrom = null,
            tokenTo = null,
            calculating = false,
            swapEnabled = false
        )
        Yyy(
            uiState = uiState,
            onClickClose = { /*TODO*/ },
            onClickCoinFrom = { /*TODO*/ },
            onClickCoinTo = { /*TODO*/ },
            onSwitchPairs = { /*TODO*/ },
            onEnterAmount = {}
        )
    }
}
