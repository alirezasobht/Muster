package app.muster.ui.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AdaptedCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    length: Int,
    enabled: Boolean,
    onDone: () -> Unit
) = CodeInput(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    length = length,
    enabled = enabled,
    onDone = onDone
)
