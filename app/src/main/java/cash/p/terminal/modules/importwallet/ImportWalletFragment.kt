package cash.p.terminal.modules.importwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.navigateWithTermsAccepted
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.contacts.screen.ConfirmationBottomSheet
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.swap.settings.Caution
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.headline2_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

class ImportWalletFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            val popUpToInclusiveId =
                arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.restoreAccountFragment) ?: R.id.restoreAccountFragment

            setContent {
                ImportWalletScreen(findNavController(), popUpToInclusiveId)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ImportWalletScreen(
    navController: NavController,
    popUpToInclusiveId: Int
) {
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                try {
                    inputStream.bufferedReader().use { br ->
                        //todo validate for json format
                        navController.navigateWithTermsAccepted {
                            navController.slideFromBottom(
                                R.id.restoreLocalFragment,
                                bundleOf(
                                    ManageAccountsModule.popOffOnSuccessKey to popUpToInclusiveId,
                                    "jsonFile" to br.readText()
                                )
                            )
                        }
                    }
                } catch (e: Throwable) {
                    HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                ConfirmationBottomSheet(
                    title = stringResource(R.string.ImportWallet_WarningInvalidJson),
                    text = stringResource(R.string.ImportWallet_WarningInvalidJsonDescription),
                    iconPainter = painterResource(R.drawable.icon_warning_2_20),
                    iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
                    confirmText = stringResource(R.string.ImportWallet_SelectAnotherFile),
                    cautionType = Caution.Type.Warning,
                    cancelText = stringResource(R.string.Button_Cancel),
                    onConfirm = {
                        restoreLauncher.launch(arrayOf("application/json"))
                    },
                    onClose = {
                        coroutineScope.launch { bottomSheetState.hide() }
                    }
                )
            }
        ) {
            Scaffold(
                backgroundColor = ComposeAppTheme.colors.tyler,
                topBar = {
                    AppBar(
                        title = TranslatableString.ResString(R.string.ManageAccounts_ImportWallet),
                        menuItems = listOf(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.Button_Close),
                                icon = R.drawable.ic_close,
                                onClick = { navController.popBackStack() }
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
                    VSpacer(12.dp)
                    ImportOption(
                        title = stringResource(R.string.ImportWallet_BackupFile),
                        description = stringResource(R.string.ImportWallet_BackupFile_Description),
                        icon = R.drawable.ic_download_24,
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json"))
                        }
                    )
                    VSpacer(12.dp)
                    ImportOption(
                        title = stringResource(R.string.ImportWallet_RecoveryPhrase),
                        description = stringResource(R.string.ImportWallet_RecoveryPhrase_Description),
                        icon = R.drawable.ic_edit_24,
                        onClick = {
                            navController.navigateWithTermsAccepted {
                                navController.slideFromBottom(
                                    R.id.restoreAccountFragment,
                                    bundleOf(ManageAccountsModule.popOffOnSuccessKey to popUpToInclusiveId)
                                )
                            }
                        }
                    )
                    VSpacer(12.dp)
                    ImportOption(
                        title = stringResource(R.string.ImportWallet_WatchAddress),
                        description = stringResource(R.string.ImportWallet_WatchAddress_Description),
                        icon = R.drawable.icon_binocule_24,
                        onClick = {
                            navController.slideFromRight(
                                R.id.watchAddressFragment,
                                bundleOf(ManageAccountsModule.popOffOnSuccessKey to popUpToInclusiveId)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportOption(
    title: String,
    description: String,
    icon: Int,
    onClick: () -> Unit
) {
    CellUniversalLawrenceSection {
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalPadding = 24.dp,
            onClick = onClick,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
            HSpacer(16.dp)
            Column {
                headline2_leah(title)
                subhead2_grey(description)
            }
        }
    }
}