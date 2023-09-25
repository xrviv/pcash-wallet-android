package cash.p.terminal.modules.pin.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.modules.pin.unlock.PinConfirmViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.headline1_leah
import cash.p.terminal.ui.compose.components.headline1_lucian

@Composable
fun PinConfirm(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = viewModel<PinConfirmViewModel>(factory = PinConfirmViewModel.Factory())

    if (viewModel.uiState.unlocked) {
        onSuccess.invoke()
        viewModel.unlocked()
    }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Unlock_Title),
                navigationIcon = {
                    HsBackButton(onClick = onCancel)
                },
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            PinTopBlock(
                modifier = Modifier.weight(1f),
                title = {
                    val error = viewModel.uiState.error
                    if (error != null) {
                        headline1_lucian(
                            text = error,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        headline1_leah(
                            text = stringResource(R.string.Unlock_Passcode),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                enteredCount = viewModel.uiState.enteredCount,
                showShakeAnimation = viewModel.uiState.showShakeAnimation,
                inputState = viewModel.uiState.inputState,
                onShakeAnimationFinish = { viewModel.onShakeAnimationFinish() }
            )

            PinNumpad(
                onNumberClick = { number -> viewModel.onKeyClick(number) },
                onDeleteClick = { viewModel.onDelete() },
                showRandomizer = true,
                inputState = viewModel.uiState.inputState
            )
        }
    }
}