package cash.p.terminal.modules.receive.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.requireInput
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper

class UsedAddressesFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        UsedAddressScreen(navController, navController.requireInput())
    }

}

@Composable
fun UsedAddressScreen(
    navController: NavController,
    params: UsedAddressesParams
) {
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(id = R.string.Balance_Receive_UsedAddresses),
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                }

            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            InfoText(text = stringResource(id = R.string.Balance_Receive_UsedAddressesDescriptoin, params.coinName))
            Spacer(Modifier.height(12.dp))

            CellUniversalLawrenceSection(
                buildList {
                    for (item in params.usedAddresses)
                        add {
                            TransactionInfoAddressCell(index = item.index.toString(), address = item.address, explorerUrl = item.explorerUrl)
                        }
                }
            )

            VSpacer(24.dp)
        }
    }
}

@Composable
fun TransactionInfoAddressCell(
    index: String,
    address: String,
    explorerUrl: String
) {
    val view = LocalView.current
    val context = LocalContext.current
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = index)

        HSpacer(16.dp)
        subhead2_leah(
            modifier = Modifier.weight(1f),
            text = address,
            textAlign = TextAlign.Right
        )

        HSpacer(16.dp)
        ButtonSecondaryCircle(
            icon = R.drawable.ic_language,
            onClick = { LinkHelper.openLinkInAppBrowser(context, explorerUrl) }
        )

        HSpacer(16.dp)
        ButtonSecondaryCircle(
            icon = R.drawable.ic_copy_20,
            onClick = {
                TextHelper.copyText(address)
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
            }
        )
    }
}
