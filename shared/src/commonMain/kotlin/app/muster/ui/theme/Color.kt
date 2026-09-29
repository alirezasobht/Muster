package app.muster.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object MusterColors {
    val Accent = Color(0xFF2F7D4F)
    val White = Color(0xFFFFFFFF)

    val Ink = Color(0xFF14171A)
    val Secondary = Color(0xFF4A524D)
    val Muted = Color(0xFF6E7671)
    val Hairline = Color(0xFFE4E7E4)
    val QuietSurface = Color(0xFFF5F7F5)
    val Outline = Color(0xFFD7DCD8)
    val Hint = Color(0xFF8A918C)

    // Dashed border on the Home empty-state icon (1e) only.
    val DashedOutline = Color(0xFFCBD2CD)

    // Initials-avatar fill on member rows (1i/1j).
    val AvatarFill = Color(0xFFF0F2F0)

    val InFill = Color(0xFFE8F2EB)
    val InText = Color(0xFF1F5C39)
    val PendingFill = Color(0xFFECEFF4)
    val PendingText = Color(0xFF3F4C63)
    val OutFill = Color(0xFFFBE9E7)
    val OutText = Color(0xFF9A3324)

    val WarningFill = Color(0xFFFBF2DE)
    val WarningText = Color(0xFF7A5A16)

    // Green ground (1q, 1r, 1a) only. The Out red is unreadable on accent,
    // so errors there use OnAccentError instead.
    val OnAccent = Color(0xFFF0F7F2)
    val OnAccentMuted = Color(0xFFE2EFE6)
    val OnAccentError = Color(0xFFFBE9E7)
}

// Surfaces and surfaceTint are pinned to white/quiet so M3's tonal tinting
// never adds colour to cards, menus or dialogs.
internal val MusterColorScheme = lightColorScheme(
    primary = MusterColors.Accent,
    onPrimary = MusterColors.White,
    primaryContainer = MusterColors.InFill,
    onPrimaryContainer = MusterColors.InText,
    secondary = MusterColors.Accent,
    onSecondary = MusterColors.White,
    secondaryContainer = MusterColors.InFill,
    onSecondaryContainer = MusterColors.InText,
    tertiary = MusterColors.Accent,
    onTertiary = MusterColors.White,
    background = MusterColors.White,
    onBackground = MusterColors.Ink,
    surface = MusterColors.White,
    onSurface = MusterColors.Ink,
    surfaceVariant = MusterColors.QuietSurface,
    onSurfaceVariant = MusterColors.Muted,
    surfaceTint = MusterColors.White,
    surfaceContainerLowest = MusterColors.White,
    surfaceContainerLow = MusterColors.White,
    surfaceContainer = MusterColors.White,
    surfaceContainerHigh = MusterColors.White,
    surfaceContainerHighest = MusterColors.QuietSurface,
    outline = MusterColors.Outline,
    outlineVariant = MusterColors.Hairline,
    error = MusterColors.OutText,
    onError = MusterColors.White,
    errorContainer = MusterColors.OutFill,
    onErrorContainer = MusterColors.OutText
)
