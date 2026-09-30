package app.muster.ui.common.components

import kotlinx.browser.window

// iPhone, iPad and Mac, in any browser. iPadOS in desktop mode reports
// itself as a Mac, so "Macintosh" covers it too.
internal val isApplePlatform: Boolean by lazy {
    val userAgent = window.navigator.userAgent
    listOf("iPhone", "iPad", "iPod", "Macintosh").any { it in userAgent }
}
