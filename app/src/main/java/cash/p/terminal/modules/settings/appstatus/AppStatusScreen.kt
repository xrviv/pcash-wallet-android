package cash.p.terminal.modules.settings.appstatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.settings.appstatus.AppStatusModule.BlockContent
import cash.p.terminal.modules.settings.appstatus.AppStatusModule.BlockData
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.subhead1_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
import io.horizontalsystems.core.helpers.HudHelper


@Composable
fun AppStatusScreen(
    navController: NavController
) {
    val viewModel = viewModel<AppStatusViewModel>(factory = AppStatusModule.Factory())
    val uiState = viewModel.uiState
    val clipboardManager = LocalClipboardManager.current
    val localView = LocalView.current

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.Settings_AppStatus),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Alert_Copy),
                            onClick = {
                                uiState.appStatusAsText?.let {
                                    clipboardManager.setText(AnnotatedString(it))
                                    HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                                }
                            },
                        )
                    )
                )
            }
        ) {
            Column(Modifier.padding(it)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    uiState.blockViewItems.forEach { blockData ->
                        StatusBlock(
                            sectionTitle = blockData.title,
                            contentItems = blockData.content
                        )
                    }
                    VSpacer(32.dp)
                }
            }
        }
    }
}

@Composable
private fun StatusBlock(
    sectionTitle: String?,
    contentItems: List<BlockContent>
) {
    if (sectionTitle != null) {
        VSpacer(24.dp)
    } else {
        VSpacer(12.dp)
    }

    sectionTitle?.let {
        InfoText(text = it.uppercase())
    }
    CellUniversalLawrenceSection(contentItems) { item ->
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            when (item) {
                is BlockContent.Header -> {
                    subhead2_leah(
                        text = item.title,
                    )
                }

                is BlockContent.Text -> {
                    subhead2_grey(
                        text = item.text,
                    )
                }

                is BlockContent.TitleValue -> {
                    subhead2_grey(
                        modifier = Modifier.weight(1f),
                        text = item.title,
                    )
                    subhead1_leah(
                        modifier = Modifier.padding(start = 8.dp),
                        text = item.value,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun StatusBlockPreview() {
    val testBlocks = listOf(
        BlockData(
            title = "Status",
            content = listOf(
                BlockContent.Header("Header"),
                BlockContent.TitleValue("Title", "Value"),
                BlockContent.TitleValue("Title 2", "Value 2"),
                BlockContent.Text("So then I thought what if I use chat GPT, save a link to my home screen on my phone, and start a new thread on it called MyFitness app. For anyone not familiar with chat GPT you can essentially give it prompts and get it to give outputs that provide information you need. Not always 100% correct but you can give it feedback to adjust as needed."),
            )
        ),
        BlockData(
            title = null,
            content = listOf(
                BlockContent.TitleValue("Title", "Value"),
                BlockContent.TitleValue("Title 2", "Value 2"),
                BlockContent.Text("So then I thought what if I use chat GPT, save a link to my home screen on my phone, and start a new thread on it called MyFitness app. For anyone not familiar with chat GPT you can essentially give it prompts and get it to give outputs that provide information you need. Not always 100% correct but you can give it feedback to adjust as needed."),
            )
        ),
    )
    ComposeAppTheme {
        testBlocks.forEach {
            StatusBlock(
                it.title,
                it.content
            )
        }
    }
}