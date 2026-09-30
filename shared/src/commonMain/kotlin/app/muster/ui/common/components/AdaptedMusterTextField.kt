package app.muster.ui.common.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Workaround: Compose for Web text input fails in Apple browsers (no iOS 27
// keyboard, reported on Mac too), so there it types into a real HTML <input>.
// Elsewhere it is MusterTextField. Remove it, and swap its call sites back,
// once Compose fixes the bug.
@Composable
expect fun AdaptedMusterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onAccent: Boolean = false
)
