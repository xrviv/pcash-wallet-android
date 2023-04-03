package cash.p.terminal.modules.restoreaccount.restoreprivatekey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.restoreaccount.restoreblockchains.RestoreBlockchainsFragment
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreByMenu
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*

@Composable
fun RestorePrivateKey(
    navController: NavController,
    popUpToInclusiveId: Int,
    restoreMenuViewModel: RestoreMenuViewModel,
) {
    val viewModel = viewModel<RestorePrivateKeyViewModel>(factory = RestorePrivateKeyModule.Factory())

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.Restore_Advanced_Title),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Next),
                        onClick = {
                            viewModel.resolveAccountType()?.let { accountType ->
                                navController.slideFromRight(
                                    R.id.restoreSelectCoinsFragment,
                                    bundleOf(
                                        RestoreBlockchainsFragment.ACCOUNT_NAME_KEY to viewModel.accountName,
                                        RestoreBlockchainsFragment.ACCOUNT_TYPE_KEY to accountType,
                                        ManageAccountsModule.popOffOnSuccessKey to popUpToInclusiveId,
                                    )
                                )
                            }
                        }
                    )
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            HeaderText(stringResource(id = R.string.ManageAccount_Name))
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = viewModel.accountName,
                pasteEnabled = false,
                hint = viewModel.defaultName,
                onValueChange = viewModel::onEnterName
            )
            Spacer(Modifier.height(32.dp))

            RestoreByMenu(restoreMenuViewModel)

            Spacer(Modifier.height(32.dp))

            FormsInputMultiline(
                modifier = Modifier.padding(horizontal = 16.dp),
                hint = stringResource(id = R.string.Restore_PrivateKeyHint),
                state = viewModel.inputState,
                qrScannerEnabled = true,
            ) {
                viewModel.onEnterPrivateKey(it)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
