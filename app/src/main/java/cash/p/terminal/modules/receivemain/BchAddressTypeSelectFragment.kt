package cash.p.terminal.modules.receivemain

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.receive.address.ReceiveAddressFragment
import io.horizontalsystems.core.helpers.HudHelper

class BchAddressTypeSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val coinUid = arguments?.getString("coinUid")
        val popupDestinationId = arguments?.getInt(
            ReceiveAddressFragment.POPUP_DESTINATION_ID_KEY
        )
        
        if (coinUid == null) {
            HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)
            navController.popBackStack()
        } else {
            val viewModel = viewModel<BchAddressTypeSelectViewModel>(
                factory = BchAddressTypeSelectViewModel.Factory(coinUid)
            )
            AddressFormatSelectScreen(
                navController,
                viewModel.items,
                stringResource(R.string.Balance_Receive_AddressFormat_RecommendedAddressType),
                popupDestinationId
            )
        }
    }

    companion object {
        fun prepareParams(coinUid: String, popupDestinationId: Int?): Bundle {
            return bundleOf(
                "coinUid" to coinUid,
                ReceiveAddressFragment.POPUP_DESTINATION_ID_KEY to popupDestinationId
            )
        }
    }
}
