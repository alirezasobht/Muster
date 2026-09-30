package app.muster.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme

@Composable
fun MusterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    // Inverted for the green ground (1a): white fill, no outline, and the
    // focus ring drops — there is no contrast for it against white on green.
    onAccent: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (onAccent) MusterColors.OnAccent else MusterColors.Muted
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled && !readOnly,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MusterColors.Ink),
            cursorBrush = SolidColor(MusterColors.Accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .then(
                    if (keyboardOptions.keyboardType == KeyboardType.Email) {
                        Modifier.semantics { contentType = ContentType.EmailAddress }
                    } else {
                        Modifier
                    }
                ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            when {
                                readOnly ->
                                    Modifier.background(MusterColors.QuietSurface, MaterialTheme.shapes.medium)
                                onAccent ->
                                    Modifier.background(MusterColors.White, MaterialTheme.shapes.medium)
                                else -> Modifier.border(
                                    width = if (focused) 2.dp else 1.dp,
                                    color = if (focused) MusterColors.Accent else MusterColors.Outline,
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    innerTextField()
                }
            }
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = if (onAccent) MusterColors.OnAccentError else MusterColors.OutText
            )
        }
    }
}

@Preview
@Composable
private fun MusterTextFieldPreview() {
    MusterTheme {
        Surface {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                MusterTextField(
                    value = "alex.doyle@gmail.com",
                    onValueChange = {},
                    label = "Email",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                MusterTextField(
                    value = "alex.doyle@",
                    onValueChange = {},
                    label = "Email",
                    error = "That doesn't look like an email address."
                )
                MusterTextField(
                    value = "alex.doyle@gmail.com",
                    onValueChange = {},
                    label = "Email",
                    readOnly = true
                )
            }
        }
    }
}

@Preview
@Composable
private fun MusterTextFieldOnAccentPreview() {
    MusterTheme {
        Surface(color = MusterColors.Accent) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                MusterTextField(
                    value = "alex.doyle@gmail.com",
                    onValueChange = {},
                    label = "Email",
                    onAccent = true
                )
                MusterTextField(
                    value = "alex.doyle@",
                    onValueChange = {},
                    label = "Email",
                    error = "That doesn't look like an email address.",
                    onAccent = true
                )
            }
        }
    }
}
