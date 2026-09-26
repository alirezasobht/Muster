package app.muster.ui.common.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AdaptedMusterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    readOnly: Boolean,
    error: String?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    onAccent: Boolean
) = MusterTextField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    enabled = enabled,
    readOnly = readOnly,
    error = error,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    onAccent = onAccent
)
