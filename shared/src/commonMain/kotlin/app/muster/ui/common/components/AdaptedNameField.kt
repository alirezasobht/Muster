package app.muster.ui.common.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.field_name
import org.jetbrains.compose.resources.stringResource

// NameField on AdaptedMusterTextField, for Set name and Settings. Goes when
// AdaptedMusterTextField does; keep its options in step with NameField.
@Composable
fun AdaptedNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    onDone: () -> Unit = {}
) {
    AdaptedMusterTextField(
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
