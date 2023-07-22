package cash.p.terminal.modules.settings.support

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.InfoTextBody
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader

class PremiumFeatureWarningDialog : BaseComposableBottomSheetFragment() {

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
                PremiumFeatureWarningScreen(
                    App.appConfigProvider.analyticsLink
                ) { close() }
            }
        }
    }
}

@Composable
private fun PremiumFeatureWarningScreen(
    analyticsLink: String,
    onCloseClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.icon_24_lock),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
            title = stringResource(R.string.Settings_PersonalSupport),
            onCloseClick = onCloseClick
        ) {
            InfoTextBody(
                text = stringResource(R.string.Settings_PersonalSupport_PremiumFeatureWarning)
            )
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                title = stringResource(R.string.Button_LearnMore),
                onClick = {
                    uriHandler.openUri(analyticsLink)
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
