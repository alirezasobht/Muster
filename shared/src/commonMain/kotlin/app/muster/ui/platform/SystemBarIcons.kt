package app.muster.ui.platform

import androidx.compose.runtime.Composable

// Colour of the status and navigation bar icons: dark on a light screen,
// white on the green ones. A no-op where the platform has no such bars.
@Composable
expect fun SystemBarIcons(dark: Boolean)
