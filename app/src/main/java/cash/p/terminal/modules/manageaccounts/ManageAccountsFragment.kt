package cash.p.terminal.modules.manageaccounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.navigateWithTermsAccepted
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.backupalert.BackupAlert
import cash.p.terminal.modules.manageaccount.ManageAccountModule
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule.ActionViewItem
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.body_jacob
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_lucian
import io.horizontalsystems.core.findNavController

class ManageAccountsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ManageAccountsScreen(findNavController(), arguments?.getParcelable(ManageAccountsModule.MODE)!!)
            }
        }
    }
}

@Composable
fun ManageAccountsScreen(navController: NavController, mode: ManageAccountsModule.Mode) {
    BackupAlert(navController)

    val viewModel = viewModel<ManageAccountsViewModel>(factory = ManageAccountsModule.Factory(mode))

    val viewItems = viewModel.viewItems
    val finish = viewModel.finish
    val isCloseButtonVisible = viewModel.isCloseButtonVisible

    if (finish) {
        navController.popBackStack()
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            var menuItems: List<MenuItem> = listOf()
            var navigationIcon: @Composable (() -> Unit)? = null

            if (isCloseButtonVisible) {
                menuItems = listOf(MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = { navController.popBackStack() }
                ))
            } else {
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            }
            AppBar(
                title = TranslatableString.ResString(R.string.ManageAccounts_Title),
                navigationIcon = navigationIcon,
                menuItems = menuItems
            )

            LazyColumn(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))

                    viewItems?.let { (regularAccounts, watchAccounts) ->
                        if (regularAccounts.isNotEmpty()) {
                            AccountsSection(regularAccounts, viewModel, navController)
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        if (watchAccounts.isNotEmpty()) {
                            AccountsSection(watchAccounts, viewModel, navController)
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    val args = when (mode) {
                        ManageAccountsModule.Mode.Manage -> ManageAccountsModule.prepareParams(R.id.manageAccountsFragment, false)
                        ManageAccountsModule.Mode.Switcher -> ManageAccountsModule.prepareParams(R.id.manageAccountsFragment, true)
                    }

                    val actions = listOf(
                        ActionViewItem(R.drawable.ic_plus, R.string.ManageAccounts_CreateNewWallet) {
                            navController.navigateWithTermsAccepted {
                                navController.slideFromRight(R.id.createAccountFragment, args)
                            }
                        },
                        ActionViewItem(R.drawable.ic_download_20, R.string.ManageAccounts_ImportWallet) {
                            navController.slideFromBottom(R.id.importWalletFragment, args)
                        },
                        ActionViewItem(R.drawable.icon_binocule_20, R.string.ManageAccounts_WatchAddress) {
                            navController.slideFromRight(R.id.watchAddressFragment, args)
                        }
                    )
                    CellUniversalLawrenceSection(actions) {
                        RowUniversal(
                            onClick = it.callback
                        ) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                painter = painterResource(id = it.icon),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                            body_jacob(text = stringResource(id = it.title))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun AccountsSection(accounts: List<AccountViewItem>, viewModel: ManageAccountsViewModel, navController: NavController) {
    CellUniversalLawrenceSection(items = accounts) { accountViewItem ->
        RowUniversal(
            onClick = { viewModel.onSelect(accountViewItem) }
        ) {
            if (accountViewItem.selected) {
                Icon(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    painter = painterResource(id = R.drawable.ic_radion),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.jacob
                )
            } else {
                Icon(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    painter = painterResource(id = R.drawable.ic_radioff),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                body_leah(text = accountViewItem.title)
                if (accountViewItem.backupRequired) {
                    subhead2_lucian(text = stringResource(id = R.string.ManageAccount_BackupRequired_Title))
                } else if (accountViewItem.migrationRequired) {
                    subhead2_lucian(text = stringResource(id = R.string.ManageAccount_MigrationRequired_Title))
                } else {
                    subhead2_grey(
                        text = accountViewItem.subtitle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
            if (accountViewItem.isWatchAccount) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_binocule_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }

            val icon: Int
            val iconTint: Color
            if (
                accountViewItem.backupRequired
                || accountViewItem.migrationRequired
                || accountViewItem.migrationRecommended
            ) {
                icon = R.drawable.icon_warning_2_20
                iconTint = ComposeAppTheme.colors.lucian
            } else {
                icon = R.drawable.ic_more2_20
                iconTint = ComposeAppTheme.colors.leah
            }

            ButtonSecondaryCircle(
                modifier = Modifier.padding(horizontal = 16.dp),
                icon = icon,
                tint = iconTint
            ) {
                navController.slideFromRight(
                    R.id.manageAccountFragment,
                    ManageAccountModule.prepareParams(accountViewItem.accountId)
                )
            }
        }
    }
}
