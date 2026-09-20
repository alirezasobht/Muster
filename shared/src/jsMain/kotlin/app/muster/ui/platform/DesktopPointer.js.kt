package app.muster.ui.platform

import kotlin.js.js

actual fun hasDesktopPointer(): Boolean =
    js("window.matchMedia('(hover: hover) and (pointer: fine)').matches")