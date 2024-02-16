package cash.p.terminal.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.getInput
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.modules.pin.ui.PinSet
import kotlinx.parcelize.Parcelize

class SetPinFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()

        PinSet(
            title = stringResource(R.string.PinSet_Title),
            description = stringResource(input?.descriptionResId ?: R.string.PinSet_Info),
            dismissWithSuccess = {
                navController.setNavigationResultX(Result(true))
                navController.popBackStack()
            },
            onBackPress = { navController.popBackStack() }
        )
    }

    @Parcelize
    data class Input(val descriptionResId: Int) : Parcelable

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
