package cash.p.terminal.modules.pin.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.modules.pin.unlock.PinUnlockModule
import cash.p.terminal.modules.pin.unlock.PinUnlockViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.headline1_leah
import cash.p.terminal.ui.compose.components.headline1_lucian

@Composable
fun PinUnlock(
    showCancelButton: Boolean,
    dismissWithSuccess: () -> Unit,
    onCancelClick: () -> Unit,
    viewModel: PinUnlockViewModel = viewModel(factory = PinUnlockModule.Factory(showCancelButton))
) {
    var showBiometricPrompt by remember { mutableStateOf(viewModel.uiState.fingerScannerEnabled) }
    var showBiometricDisabledAlert by remember { mutableStateOf(false) }

    if (viewModel.uiState.unlocked) {
        dismissWithSuccess.invoke()
        viewModel.unlocked()
    }

    if (showBiometricPrompt) {
        BiometricPromptDialog(
            onSuccess = {
                viewModel.onBiometricsUnlock()
                showBiometricPrompt = false
            },
            onError = { errorCode ->
                if (errorCode == BiometricPrompt.ERROR_LOCKOUT) {
                    showBiometricDisabledAlert = true
                }
                showBiometricPrompt = false
            }
        )
    }

    if (showBiometricDisabledAlert) {
        BiometricDisabledDialog {
            showBiometricDisabledAlert = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ComposeAppTheme.colors.tyler)
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
                        text = viewModel.uiState.title,
                        textAlign = TextAlign.Center
                    )
                }
            },
            enteredCount = viewModel.uiState.enteredCount,
            showCancelButton = viewModel.cancelButtonVisible,
            showShakeAnimation = viewModel.uiState.showShakeAnimation,
            inputState = viewModel.uiState.inputState,
            onShakeAnimationFinish = { viewModel.onShakeAnimationFinish() },
            onCancelClick = onCancelClick
        )

        PinNumpad(
            onNumberClick = { number -> viewModel.onKeyClick(number) },
            onDeleteClick = { viewModel.onDelete() },
            showFingerScanner = viewModel.uiState.fingerScannerEnabled,
            showRandomizer = true,
            showBiometricPrompt = {
                showBiometricPrompt = true
            },
            inputState = viewModel.uiState.inputState
        )
    }
}
