package app.muster.ui.common.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.muster.ui.theme.MusterTheme

// Provides both scopes so previews and tests render shared elements instead
// of falling back to the no-op path.
@Composable
fun SharedTransitionWrapper(content: @Composable () -> Unit) {
    MusterTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalAnimatedVisibilityScope provides this@AnimatedVisibility
                ) {
                    content()
                }
            }
        }
    }
}
