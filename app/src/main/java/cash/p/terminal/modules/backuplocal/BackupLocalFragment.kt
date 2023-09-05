package cash.p.terminal.modules.backuplocal

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.modules.backuplocal.password.LocalBackupPasswordScreen
import cash.p.terminal.modules.backuplocal.terms.LocalBackupTermsScreen
import io.horizontalsystems.core.findNavController

class BackupLocalFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        BackupLocalNavHost(findNavController(), requireArguments().getString(ACCOUNT_ID_KEY))
    }

    companion object {
        private const val ACCOUNT_ID_KEY = "coin_uid_key"
        fun prepareParams(accountId: String): Bundle {
            return bundleOf(ACCOUNT_ID_KEY to accountId)
        }
    }
}

@Composable
private fun BackupLocalNavHost(fragmentNavController: NavController, accountId: String?) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "terms_page",
    ) {
        composable("terms_page") {
            LocalBackupTermsScreen(fragmentNavController, navController)
        }
        composablePage("password_page") {
            LocalBackupPasswordScreen(
                fragmentNavController,
                navController,
                accountId
            )
        }
    }
}
