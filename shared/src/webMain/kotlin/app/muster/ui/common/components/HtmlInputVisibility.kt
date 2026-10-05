package app.muster.ui.common.components

import androidx.lifecycle.Lifecycle

// STARTED, not RESUMED: iOS Safari doesn't give the window focus back after
// the page returns from the background, so Compose leaves the lifecycle at
// STARTED and a RESUMED check would hide the input for good. The cost is that
// a screen leaving a transition keeps its input a moment longer.
internal fun Lifecycle.State.showsHtmlInput(): Boolean = isAtLeast(Lifecycle.State.STARTED)
