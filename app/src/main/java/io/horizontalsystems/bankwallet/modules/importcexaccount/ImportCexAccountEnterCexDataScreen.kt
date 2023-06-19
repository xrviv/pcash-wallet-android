package cash.p.terminal.modules.importcexaccount

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.utils.ModuleField
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.qrscanner.QRScannerActivity
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellowWithSpinner
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui.compose.components.FormsInputPassword
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.VSpacer
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun ImportCexAccountEnterCexDataScreen(
    cexId: String,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onAccountCreate: () -> Unit,
) {
    val viewModel = viewModel<ImportCexAccountEnterCexDataViewModel>(factory = ImportCexAccountEnterCexDataViewModel.Factory(cexId))

    when (val cex = viewModel.cex) {
        is CexBinance -> {
            ImportBinanceCexAccountScreen(cex, onNavigateBack, onClose, onAccountCreate)
        }

        is CexCoinzix -> {
            ImportCoinzixCexAccountScreen(cex, onNavigateBack, onClose, onAccountCreate)
        }

        else -> Unit
    }
}

@Composable
private fun ImportCoinzixCexAccountScreen(
    cex: CexCoinzix,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onAccountCreate: () -> Unit
) {
    val viewModel = viewModel<EnterCexDataCoinzixViewModel>()

    val intent = Intent(LocalContext.current, HCaptchaActivity::class.java)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val token = data?.extras?.getString("captcha") ?: ""
                    viewModel.onResultCaptchaToken(token)
                }

                Activity.RESULT_CANCELED -> {
                    Log.d("hCaptcha", "hCaptcha failed")
                }
            }
        }

    if (viewModel.accountCreated) {
        LaunchedEffect(Unit) {
            onAccountCreate.invoke()
        }
    }

    var hidePassphrase by remember { mutableStateOf(true) }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.PlainString(cex.name),
                navigationIcon = {
                    HsBackButton(onClick = onNavigateBack)
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
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            InfoText(text = stringResource(R.string.ImportCexAccountConzix_Description))
            VSpacer(height = 20.dp)
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                hint = stringResource(R.string.ImportCexAccountConzix_Email)
            ) {
                viewModel.onEnterEmail(it)
            }
            VSpacer(height = 16.dp)
            FormsInputPassword(
                modifier = Modifier.padding(horizontal = 16.dp),
                hint = stringResource(R.string.Password),
                //state = uiState.passphraseState,
                onValueChange = {
                    viewModel.onEnterPassword(it)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                hide = hidePassphrase,
                onToggleHide = {
                    hidePassphrase = !hidePassphrase
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            ButtonsGroupWithShade {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ButtonPrimaryYellowWithSpinner(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Login),
                        showSpinner = false,
                        enabled = viewModel.loginEnabled,
                        onClick = {
                            launcher.launch(intent)
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    ButtonPrimaryTransparent(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_SignUp),
                        onClick = {
                            //viewModel.onSignUp()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportBinanceCexAccountScreen(
    cex: CexBinance,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onAccountCreate: () -> Unit
) {
    val viewModel = viewModel<EnterCexDataBinanceViewModel>()
    val view = LocalView.current
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val accountCreated = uiState.accountCreated
    val apiKey = uiState.apiKey
    val secretKey = uiState.secretKey
    val errorMessage = uiState.errorMessage
    val connectEnabled = uiState.connectEnabled

    if (accountCreated) {
        LaunchedEffect(Unit) {
            onAccountCreate.invoke()
        }
    }

    LaunchedEffect(errorMessage, uiState) {
        errorMessage?.let {
            HudHelper.showErrorMessage(view, it)
        }
    }

    val qrScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
            viewModel.onScannedData(data)
        }
    }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.PlainString(cex.name),
                navigationIcon = {
                    HsBackButton(onClick = onNavigateBack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_ScanQr),
                        icon = R.drawable.ic_qr_scan_24px,
                        onClick = {
                            qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context))
                        }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClose
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            InfoText(text = stringResource(R.string.ImportCexAccountBinance_Description))
            FormsInput(
                initial = apiKey,
                modifier = Modifier.padding(horizontal = 16.dp),
                hint = stringResource(R.string.ImportCexAccountBinance_ApiKey)
            ) {
                viewModel.onEnterApiKey(it)
            }
            VSpacer(height = 16.dp)
            FormsInput(
                initial = secretKey,
                modifier = Modifier.padding(horizontal = 16.dp),
                hint = stringResource(R.string.ImportCexAccountBinance_SecretKey)
            ) {
                viewModel.onEnterSecretKey(it)
            }

            Spacer(modifier = Modifier.weight(1f))

            ButtonsGroupWithShade {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ButtonPrimaryYellow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Connect),
                        enabled = connectEnabled,
                        onClick = {
                            viewModel.onClickConnect()
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    ButtonPrimaryTransparent(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_GetApiKeys),
                        onClick = {

                        }
                    )
                }
            }
        }
    }
}
