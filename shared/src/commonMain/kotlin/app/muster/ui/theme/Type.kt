package app.muster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.dm_sans_bold
import muster.shared.generated.resources.dm_sans_medium
import muster.shared.generated.resources.dm_sans_regular
import muster.shared.generated.resources.dm_sans_semibold
import org.jetbrains.compose.resources.Font

@Composable
private fun dmSans() = FontFamily(
    Font(Res.font.dm_sans_regular, FontWeight.Normal),
    Font(Res.font.dm_sans_medium, FontWeight.Medium),
    Font(Res.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.dm_sans_bold, FontWeight.Bold)
)

@Composable
internal fun musterTypography(): Typography {
    val family = dmSans()
    fun style(size: Int, weight: FontWeight, lineHeight: Double, tracking: Double = 0.0) =
        TextStyle(
            fontFamily = family,
            fontSize = size.sp,
            fontWeight = weight,
            lineHeight = lineHeight.sp,
            letterSpacing = tracking.sp
        )

    return Typography(
        // Screen titles — "Sign in", "Enter your code" (1a–1c)
        headlineLarge = style(30, FontWeight.Bold, 36.0, tracking = -0.6),
        // App bar titles (1d, 1f)
        titleLarge = style(20, FontWeight.Bold, 26.0, tracking = -0.2),
        // Card and group names (1d)
        titleMedium = style(18, FontWeight.SemiBold, 24.0),
        // Field text, list rows
        bodyLarge = style(16, FontWeight.Normal, 24.0),
        // Subtitles under screen titles
        bodyMedium = style(15, FontWeight.Normal, 22.5),
        // Footnotes and hints
        bodySmall = style(13, FontWeight.Normal, 19.5),
        // Primary and outlined buttons
        labelLarge = style(16, FontWeight.SemiBold, 20.0),
        // Field labels
        labelMedium = style(13, FontWeight.Medium, 18.0),
        // Status badges
        labelSmall = style(12, FontWeight.SemiBold, 16.0)
    )
}
