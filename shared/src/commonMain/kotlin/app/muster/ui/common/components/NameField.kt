package app.muster.ui.common.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.field_name
import org.jetbrains.compose.resources.stringResource

// Shared by Set name (1c) and Settings (1f). Don't fork it per screen.
@Composable
fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    onDone: () -> Unit = {}
) {
    MusterTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(Res.string.field_name),
        modifier = modifier,
        enabled = enabled,
        error = error,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() })
    )
}

@Preview
@Composable
private fun NameFieldPreview() {
    MusterTheme {
        Surface {
            NameField(value = "Alex Doyle", onValueChange = {}, modifier = Modifier.padding(24.dp))
        }
    }
}
