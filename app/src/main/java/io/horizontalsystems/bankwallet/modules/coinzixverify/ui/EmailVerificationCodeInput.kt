package cash.p.terminal.modules.coinzixverify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.entities.DataState
import cash.p.terminal.ui.compose.ColoredTextStyle
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ButtonSecondaryDefault
import cash.p.terminal.ui.compose.components.FormsInputStateWarning

sealed class CodeGetButtonState {
    object Active : CodeGetButtonState()
    class Pending(val secondsLeft: Int) : CodeGetButtonState()
}

@Composable
fun EmailVerificationCodeInput(
    modifier: Modifier = Modifier,
    hint: String,
    state: DataState<Any>? = null,
    singleLine: Boolean = false,
    maxLength: Int? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    actionButtonState: CodeGetButtonState,
    onValueChange: (String) -> Unit,
    actionButtonClick: () -> Unit,
) {

    val borderColor = when (state) {
        is DataState.Error -> {
            if (state.error is FormsInputStateWarning) {
                ComposeAppTheme.colors.yellow50
            } else {
                ComposeAppTheme.colors.red50
            }
        }

        else -> ComposeAppTheme.colors.steel20
    }

    val actionButtonText = when (actionButtonState) {
        CodeGetButtonState.Active -> stringResource(R.string.CexWithdraw_GetCode)
        is CodeGetButtonState.Pending -> stringResource(R.string.CexWithdraw_GetCodeSeconds, actionButtonState.secondsLeft)
    }

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.lawrence),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var textState by rememberSaveable(null, stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue("", TextRange(0)))
            }

            BasicTextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .weight(1f),
                value = textState,
                onValueChange = { textFieldValue ->
                    val text = textFieldValue.text
                    if (maxLength == null || text.length <= maxLength) {
                        textState = textFieldValue
                        onValueChange.invoke(text)
                    } else {
                        // Need to set textState to new instance of TextFieldValue with the same values
                        // Otherwise it getting set to empty string
                        textState = TextFieldValue(text = textState.text, selection = textState.selection)
                    }
                },
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body
                ),
                singleLine = singleLine,
                cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                decorationBox = { innerTextField ->
                    if (textState.text.isEmpty()) {
                        Text(
                            hint,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = ComposeAppTheme.colors.grey50,
                            style = ComposeAppTheme.typography.body
                        )
                    }
                    innerTextField()
                },
                keyboardOptions = keyboardOptions,
            )

            ButtonSecondaryDefault(
                modifier = Modifier.padding(end = 16.dp),
                title = actionButtonText,
                enabled = actionButtonState == CodeGetButtonState.Active,
                onClick = actionButtonClick,
            )
        }
    }
}
