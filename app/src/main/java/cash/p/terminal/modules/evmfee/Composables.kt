package cash.p.terminal.modules.evmfee

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import cash.p.terminal.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*

@Composable
fun Eip1559FeeSettings(
    viewModel: Eip1559FeeSettingsViewModel,
    navController: NavController
) {
    val feeViewItem by viewModel.feeViewItemLiveData.observeAsState()
    val feeViewItemState by viewModel.feeViewItemStateLiveData.observeAsState()
    val feeViewItemLoading by viewModel.feeViewItemLoadingLiveData.observeAsState(false)
    val currentBaseFee by viewModel.currentBaseFeeLiveData.observeAsState()
    val maxBaseFeeSlider by viewModel.baseFeeSliderViewItemLiveData.observeAsState()
    val maxPriorityFeeSlider by viewModel.priorityFeeSliderViewItemLiveData.observeAsState()
    val cautions by viewModel.cautionsLiveData.observeAsState(listOf())

    val settingsViewItems = mutableListOf<@Composable () -> Unit>()
    var maxBaseFee by remember { mutableStateOf(0L) }
    var maxPriorityFee by remember { mutableStateOf(1L) }
    var valueChanged by remember { mutableStateOf(false) }

    Column {
        Spacer(modifier = Modifier.height(12.dp))

        CellSingleLineLawrenceSection(
            listOf(
                {
                    MaxFeeCell(
                        title = stringResource(R.string.FeeSettings_MaxFee),
                        value = feeViewItem?.fee ?: "",
                        loading = feeViewItemLoading,
                        viewState = feeViewItemState,
                        navController = navController
                    )
                },
                {
                    FeeInfoCell(
                        title = stringResource(R.string.FeeSettings_GasLimit),
                        value = feeViewItem?.gasLimit,
                        infoTitle = Translator.getString(R.string.FeeSettings_GasLimit),
                        infoText = Translator.getString(R.string.FeeSettings_GasLimit_Info),
                        navController = navController
                    )
                },
                {
                    FeeCell(title = stringResource(R.string.FeeSettings_CurrentBaseFee), value = currentBaseFee)
                }
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        maxBaseFeeSlider?.let { baseFeeSlider ->
            maxPriorityFeeSlider?.let { priorityFeeSlider ->
                maxBaseFee = baseFeeSlider.initialSliderValue

                settingsViewItems.add {
                    FeeInfoCell(
                        title = stringResource(R.string.FeeSettings_MaxBaseFee),
                        value = baseFeeSlider.scaledString(maxBaseFee),
                        infoTitle = Translator.getString(R.string.FeeSettings_MaxBaseFee),
                        infoText = Translator.getString(R.string.FeeSettings_MaxBaseFee_Info),
                        navController = navController
                    )
                }

                settingsViewItems.add {
                    HsSlider(
                        value = baseFeeSlider.initialSliderValue,
                        onValueChange = { maxBaseFee = it },
                        valueRange = baseFeeSlider.range.first..baseFeeSlider.range.last,
                        onValueChangeFinished = { viewModel.onSelectGasPrice(baseFeeSlider.wei(maxBaseFee), priorityFeeSlider.wei(maxPriorityFee)) }
                    )
                }

                settingsViewItems.add {
                    FeeInfoCell(
                        title = stringResource(R.string.FeeSettings_MaxMinerTips),
                        value = if (valueChanged) priorityFeeSlider.scaledString(maxPriorityFee) else priorityFeeSlider.initialValueScaledString,
                        infoTitle = Translator.getString(R.string.FeeSettings_MaxMinerTips),
                        infoText = Translator.getString(R.string.FeeSettings_MaxMinerTips_Info),
                        navController = navController
                    )
                }

                settingsViewItems.add {
                    HsSlider(
                        value = priorityFeeSlider.initialSliderValue,
                        onValueChange = {
                            valueChanged = true
                            maxPriorityFee = it
                        },
                        valueRange = priorityFeeSlider.range.first..priorityFeeSlider.range.last,
                        onValueChangeFinished = {
                            viewModel.onSelectGasPrice(baseFeeSlider.wei(maxBaseFee), priorityFeeSlider.wei(maxPriorityFee))
                        }
                    )
                }
            }
        }

        CellSingleLineLawrenceSection(settingsViewItems)

        Cautions(cautions)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ButtonsGroupWithShade(
    ButtonsContent: @Composable (() -> Unit)
) {
    Column(
        modifier = Modifier.offset(y = -(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(ComposeAppTheme.colors.transparent, ComposeAppTheme.colors.tyler)
                    )
                )
        )
        Box(
            modifier = Modifier
                .background(ComposeAppTheme.colors.tyler)
                .padding(bottom = 8.dp) // With 24dp offset actual padding will be 32dp
        ) {
            ButtonsContent()
        }
    }
}

@Composable
fun LegacyFeeSettings(
    viewModel: LegacyFeeSettingsViewModel,
    navController: NavController
) {
    val feeViewItem by viewModel.feeViewItemLiveData.observeAsState()
    val feeViewItemState by viewModel.feeViewItemStateLiveData.observeAsState()
    val feeViewItemLoading by viewModel.feeViewItemLoadingLiveData.observeAsState(false)
    val sliderViewItem by viewModel.sliderViewItemLiveData.observeAsState()
    val cautions by viewModel.cautionsLiveData.observeAsState(listOf())

    val settingsViewItems = mutableListOf<@Composable () -> Unit>()
    var selectedGasPrice by remember { mutableStateOf(1L) }

    Column {
        Spacer(modifier = Modifier.height(12.dp))

        settingsViewItems.add {
            MaxFeeCell(
                title = stringResource(R.string.FeeSettings_MaxFee),
                value = feeViewItem?.fee ?: "",
                loading = feeViewItemLoading,
                viewState = feeViewItemState,
                navController = navController
            )
        }

        settingsViewItems.add {
            FeeInfoCell(
                title = stringResource(R.string.FeeSettings_GasLimit),
                value = feeViewItem?.gasLimit,
                infoTitle = Translator.getString(R.string.FeeSettings_GasLimit),
                infoText = Translator.getString(R.string.FeeSettings_GasLimit_Info),
                navController = navController
            )
        }

        selectedGasPrice = sliderViewItem?.initialSliderValue ?: 0

        sliderViewItem?.let { slider ->
            settingsViewItems.add {
                FeeInfoCell(
                    title = stringResource(R.string.FeeSettings_GasPrice),
                    value = slider.scaledString(selectedGasPrice),
                    infoTitle = Translator.getString(R.string.FeeSettings_GasPrice),
                    infoText = Translator.getString(R.string.FeeSettings_GasPrice_Info),
                    navController = navController
                )
            }

            settingsViewItems.add {
                HsSlider(
                    value = slider.initialSliderValue,
                    onValueChange = { selectedGasPrice = it },
                    valueRange = slider.range.first..slider.range.last,
                    onValueChangeFinished = { viewModel.onSelectGasPrice(slider.wei(selectedGasPrice)) }
                )
            }
        }

        CellSingleLineLawrenceSection(settingsViewItems)

        Cautions(cautions)

        Spacer(modifier = Modifier.height(32.dp))
    }

}

@Composable
fun Cautions(cautions: List<CautionViewItem>) {
    val modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        cautions.forEach { caution ->

            when (caution.type) {
                CautionViewItem.Type.Error -> {
                    TextImportantError(
                        modifier = modifier,
                        text = caution.text,
                        title = caution.title,
                        icon = R.drawable.ic_attention_20
                    )
                }
                CautionViewItem.Type.Warning -> {
                    TextImportantWarning(
                        modifier = modifier,
                        text = caution.text,
                        title = caution.title,
                        icon = R.drawable.ic_attention_20
                    )
                }
            }
        }
    }
}

@Composable
fun FeeCell(title: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead2_grey(text = title)
        subhead1_leah(
            modifier = Modifier.weight(1f),
            text = value ?: "",
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FeeInfoCell(
    title: String,
    value: String?,
    infoTitle: String,
    infoText: String,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                navController.slideFromBottom(
                    R.id.feeSettingsInfoDialog,
                    FeeSettingsInfoDialog.prepareParams(infoTitle, infoText)
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.padding(end = 16.dp),
            painter = painterResource(id = R.drawable.ic_info_20), contentDescription = ""
        )
        subhead1_grey(text = title)
        subhead1_leah(
            modifier = Modifier.weight(1f),
            text = value ?: "",
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EvmFeeCell(
    title: String,
    value: String?,
    loading: Boolean,
    viewState: ViewState?,
    highlightEditButton: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    CellUniversalLawrenceSection(
        listOf {
            HSFeeCell(
                title = title,
                value = value,
                loading = loading,
                viewState = viewState,
                highlightEditButton = highlightEditButton,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            )
        })
}

@Composable
fun HSFeeCell(
    title: String,
    value: String?,
    loading: Boolean,
    viewState: ViewState?,
    highlightEditButton: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = if (enabled) onClick else null,
    ) {
        subhead2_grey(text = title)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ComposeAppTheme.colors.grey,
                    strokeWidth = 1.5.dp
                )
            } else {
                val color = if (viewState is ViewState.Error) {
                    ComposeAppTheme.colors.lucian
                } else if (value == null) {
                    ComposeAppTheme.colors.grey50
                } else {
                    ComposeAppTheme.colors.leah
                }

                Text(
                    text = value ?: stringResource(R.string.NotAvailable),
                    style = ComposeAppTheme.typography.subhead1,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (enabled) {
            Box(modifier = Modifier.padding(start = 8.dp)) {
                val tintColor = if (highlightEditButton)
                    ComposeAppTheme.colors.jacob
                else
                    ComposeAppTheme.colors.grey

                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.ic_edit_20),
                    colorFilter = ColorFilter.tint(tintColor),
                    contentDescription = ""
                )
            }
        }
    }
}


@Composable
fun MaxFeeCell(
    title: String,
    value: String,
    loading: Boolean,
    viewState: ViewState?,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                navController.slideFromBottom(
                    R.id.feeSettingsInfoDialog,
                    FeeSettingsInfoDialog.prepareParams(
                        Translator.getString(R.string.FeeSettings_MaxFee),
                        Translator.getString(R.string.FeeSettings_MaxFee_Info)
                    )
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.padding(end = 16.dp),
            painter = painterResource(id = R.drawable.ic_info_20), contentDescription = ""
        )
        subhead2_grey(text = title)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ComposeAppTheme.colors.grey,
                    strokeWidth = 1.5.dp
                )
            } else {
                Text(
                    text = value,
                    style = ComposeAppTheme.typography.subhead1,
                    color = if (viewState is ViewState.Error) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.leah,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
