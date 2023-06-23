package cash.p.terminal.modules.depositcex

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah

private val networks = listOf(
    DepositCexModule.NetworkViewItem(
        title = "Fantom",
        imageSource = ImageSource.Local(R.drawable.fantom_erc20),
    ),
    DepositCexModule.NetworkViewItem(
        title = "Gnosis",
        imageSource = ImageSource.Local(R.drawable.gnosis_erc20),
    )
)

@Composable
fun SelectNetworkScreen(
    coinId: String?,
    depositViewModel: DepositViewModel,
    openCoinSelect: () -> Unit,
    openQrCode: () -> Unit,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
) {
    if (depositViewModel.openCoinSelect) {
        openCoinSelect.invoke()
        return
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Cex_ChooseNetwork),
                    navigationIcon = {
                        HsBackButton(onClick = {
                            if (coinId != null)
                                onClose.invoke()
                            else
                                onNavigateBack.invoke()
                        })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onClose
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                networks.let { viewItems ->
                    if (viewItems.isEmpty()) {
                        ListEmptyView(
                            text = stringResource(R.string.EmptyResults),
                            icon = R.drawable.ic_not_found
                        )
                    } else {
                        InfoText(text = stringResource(R.string.Cex_ChooseNetwork_Description))
                        VSpacer(20.dp)
                        CellUniversalLawrenceSection(viewItems) { viewItem ->
                            NetworkCell(
                                viewItem = viewItem,
                                onItemClick = {
                                    openQrCode.invoke()
                                },
                            )
                        }
                        VSpacer(32.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkCell(
    viewItem: DepositCexModule.NetworkViewItem,
    onItemClick: () -> Unit,
) {
    RowUniversal(
        onClick = onItemClick,
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalPadding = 0.dp
    ) {
        Image(
            painter = viewItem.imageSource.painter(),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                .size(32.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                body_leah(
                    text = viewItem.title,
                    maxLines = 1,
                )
            }
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}
