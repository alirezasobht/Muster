package app.muster.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import app.muster.ui.theme.MusterColors
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent

// On Apple platforms, same look as MusterTextField with an HTML <input> as
// the typing surface. Every other browser gets MusterTextField itself.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun AdaptedMusterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    readOnly: Boolean,
    error: String?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    onAccent: Boolean
) {
    if (!isApplePlatform) {
        MusterTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            error = error,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            onAccent = onAccent
        )
        return
    }

    var focused by remember { mutableStateOf(false) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentKeyboardActions by rememberUpdatedState(keyboardActions)
    // The <input> sits above the canvas and can't join a screen transition, so
    // it would float over the next screen. Shown only while its screen is RESUMED.
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateAsState()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (onAccent) MusterColors.OnAccent else MusterColors.Muted
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .then(
                    when {
                        readOnly ->
                            Modifier.background(MusterColors.QuietSurface, MaterialTheme.shapes.medium)
                        onAccent ->
                            Modifier.background(MusterColors.White, MaterialTheme.shapes.medium)
                        else -> Modifier.border(
                            width = if (focused) 2.dp else 1.dp,
                            color = if (focused) MusterColors.Accent else MusterColors.Outline,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            HtmlElementView(
                factory = {
                    (document.createElement("input") as HTMLInputElement).apply {
                        applyKeyboardOptions(keyboardOptions)
                        style.setProperty("width", "100%")
                        style.setProperty("height", "100%")
                        style.setProperty("padding", "0")
                        style.setProperty("border", "none")
                        style.setProperty("outline", "none")
                        style.setProperty("background", "transparent")
                        style.setProperty("color", "#14171A")
                        style.setProperty("caret-color", "#2F7D4F")
                        style.setProperty("font-family", "'DM Sans', system-ui, sans-serif")
                        // Under 16px, iOS zooms the page when the field takes focus.
                        style.setProperty("font-size", "16px")
                        addEventListener("input", { currentOnValueChange(this.value) })
                        addEventListener("focus", { focused = true })
                        addEventListener("blur", { focused = false })
                        addEventListener("keydown", { event ->
                            if ((event as KeyboardEvent).key == "Enter") {
                                event.preventDefault()
                                currentKeyboardActions.run(keyboardOptions.imeAction)
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { input ->
                    if (input.value != value) input.value = value
                    input.disabled = !enabled || readOnly
                    val onScreen = lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED)
                    input.style.setProperty("visibility", if (onScreen) "visible" else "hidden")
                    if (!onScreen) input.blur()
                }
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = if (onAccent) MusterColors.OnAccentError else MusterColors.OutText
            )
        }
    }
}

private fun HTMLInputElement.applyKeyboardOptions(options: KeyboardOptions) {
    when (options.keyboardType) {
        KeyboardType.Email -> type = "email"
        KeyboardType.Number, KeyboardType.NumberPassword -> {
            type = "text"
            setAttribute("inputmode", "numeric")
        }
        else -> type = "text"
    }
    setAttribute(
        "autocapitalize",
        when (options.capitalization) {
            KeyboardCapitalization.Words -> "words"
            KeyboardCapitalization.Sentences -> "sentences"
            KeyboardCapitalization.Characters -> "characters"
            else -> "none"
        }
    )
    if (options.autoCorrectEnabled == false) {
        setAttribute("autocorrect", "off")
        spellcheck = false
    }
    enterKeyHint(options.imeAction)?.let { setAttribute("enterkeyhint", it) }
}

private fun enterKeyHint(imeAction: ImeAction): String? = when (imeAction) {
    ImeAction.Done -> "done"
    ImeAction.Send -> "send"
    ImeAction.Go -> "go"
    ImeAction.Next -> "next"
    ImeAction.Search -> "search"
    ImeAction.Previous -> "previous"
    else -> null
}

private fun KeyboardActions.run(imeAction: ImeAction) {
    val action = when (imeAction) {
        ImeAction.Done -> onDone
        ImeAction.Send -> onSend
        ImeAction.Go -> onGo
        ImeAction.Next -> onNext
        ImeAction.Search -> onSearch
        ImeAction.Previous -> onPrevious
        else -> null
    }
    action?.invoke(NoDefaultAction)
}

private object NoDefaultAction : KeyboardActionScope {
    override fun defaultKeyboardAction(imeAction: ImeAction) = Unit
}
