package app.muster.ui.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Workaround: Compose for Web text input fails in Apple browsers (no iOS 27
// keyboard, reported on Mac too), so there it types into a real HTML <input>.
// Elsewhere it is CodeInput. Remove it, and swap its call site back, once
// Compose fixes the bug.
@Composable
expect fun AdaptedCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    enabled: Boolean = true,
    onDone: () -> Unit = {}
)
