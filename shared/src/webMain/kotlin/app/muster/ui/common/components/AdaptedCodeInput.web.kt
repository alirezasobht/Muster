package app.muster.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.HtmlElementView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import app.muster.ui.theme.MusterColors
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent

// On Apple platforms, boxes drawn in Compose as in CodeInput, with an
// invisible HTML <input> over them that takes the taps and the typing.
// Every other browser gets CodeInput itself.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun AdaptedCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    length: Int,
    enabled: Boolean,
    onDone: () -> Unit
) {
    if (!isApplePlatform) {
        CodeInput(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            length = length,
            enabled = enabled,
            onDone = onDone
        )
        return
    }

    var focused by remember { mutableStateOf(false) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnDone by rememberUpdatedState(onDone)
    // The <input> sits above the canvas and can't join a screen transition, so
    // it would float over the next screen. Shown only while its screen is on top (see showsHtmlInput).
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateAsState()

    Box(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val active = minOf(value.length, length - 1)
            repeat(length) { index ->
                AdaptedCodeBox(
                    digit = value.getOrNull(index),
                    active = focused && index == active,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        HtmlElementView(
            factory = {
                (document.createElement("input") as HTMLInputElement).apply {
                    type = "text"
                    setAttribute("inputmode", "numeric")
                    setAttribute("autocomplete", "one-time-code")
                    setAttribute("enterkeyhint", "done")
                    maxLength = length
                    style.setProperty("width", "100%")
                    style.setProperty("height", "100%")
                    style.setProperty("padding", "0")
                    style.setProperty("border", "none")
                    style.setProperty("outline", "none")
                    style.setProperty("background", "transparent")
                    // Transparent, not opacity 0: iOS may refuse the keyboard
                    // for an input it considers invisible.
                    style.setProperty("color", "transparent")
                    style.setProperty("caret-color", "transparent")
                    // Under 16px, iOS zooms the page when the field takes focus.
                    style.setProperty("font-size", "16px")
                    addEventListener("input", {
                        val digits = this.value.filter(Char::isDigit).take(length)
                        if (digits != this.value) this.value = digits
                        currentOnValueChange(digits)
                    })
                    addEventListener("focus", { focused = true })
                    addEventListener("blur", { focused = false })
                    addEventListener("keydown", { event ->
                        if ((event as KeyboardEvent).key == "Enter") {
                            event.preventDefault()
                            currentOnDone()
                        }
                    })
                }
            },
            modifier = Modifier.matchParentSize(),
            update = { input ->
                if (input.value != value) input.value = value
                input.disabled = !enabled
                val onScreen = lifecycleState.value.showsHtmlInput()
                input.style.setProperty("visibility", if (onScreen) "visible" else "hidden")
                if (!onScreen) input.blur()
            }
        )
    }
}

@Composable
private fun AdaptedCodeBox(
    digit: Char?,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) MusterColors.Accent else MusterColors.Outline,
                shape = MaterialTheme.shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            digit != null -> Text(
                text = digit.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                ),
                color = MusterColors.Ink
            )
            active -> Box(
                Modifier
                    .size(width = 2.dp, height = 26.dp)
                    .background(MusterColors.Accent)
            )
        }
    }
}
