package app.muster.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

internal val MusterShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun MusterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MusterColorScheme,
        typography = musterTypography(),
        shapes = MusterShapes,
        content = content,
    )
}
