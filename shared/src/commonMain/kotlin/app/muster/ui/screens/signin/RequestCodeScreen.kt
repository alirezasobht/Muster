package app.muster.ui.screens.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.FormError
import app.muster.ui.common.components.MusterMark
import app.muster.ui.common.components.MusterTextField
import app.muster.ui.common.components.MusterWordmark
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.field_email
import muster.shared.generated.resources.request_code_footer
import muster.shared.generated.resources.request_code_send
import muster.shared.generated.resources.request_code_subtitle
import muster.shared.generated.resources.request_code_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RequestCodeRoute(
    onCodeSent: (String) -> Unit,
    viewModel: RequestCodeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.sentTo) {
        state.sentTo?.let {
            onCodeSent(it)
            viewModel.onSentToHandled()
        }
    }
    RequestCodeScreen(
        email = state.email,
        onEmailChange = viewModel::onEmailChange,
        onSendCode = viewModel::onSendCode,
        sending = state.sending,
        emailError = state.emailError,
        error = state.error,
        canSend = state.canSend
    )
}

@Composable
fun RequestCodeScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    onSendCode: () -> Unit,
    modifier: Modifier = Modifier,
    sending: Boolean = false,
    emailError: DomainError? = null,
    error: DomainError? = null,
    canSend: Boolean = email.isNotBlank()
) {
    Surface(modifier = modifier.fillMaxSize(), color = MusterColors.Accent) {
        PhoneWidth(surround = Color.Transparent) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 24.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MusterMark(size = 44.dp)
                    MusterWordmark(fontSize = 15)
                }
                Spacer(Modifier.height(44.dp))
                Text(
                    text = stringResource(Res.string.request_code_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MusterColors.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.request_code_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusterColors.OnAccent
                )
                Spacer(Modifier.height(36.dp))
                MusterTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = stringResource(Res.string.field_email),
                    enabled = !sending,
                    error = emailError?.toMessage(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSendCode() }),
                    onAccent = true
                )
                Spacer(Modifier.height(24.dp))
                if (error != null) {
                    FormError(message = error.toMessage())
                    Spacer(Modifier.height(12.dp))
                }
                PrimaryButton(
                    text = stringResource(Res.string.request_code_send),
                    onClick = onSendCode,
                    enabled = canSend,
                    loading = sending,
                    onAccent = true
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.request_code_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusterColors.OnAccentMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
private fun RequestCodeScreenPreview() {
    MusterTheme {
        RequestCodeScreen(email = "alex.doyle@gmail.com", onEmailChange = {}, onSendCode = {})
    }
}

@Preview
@Composable
private fun RequestCodeScreenErrorPreview() {
    MusterTheme {
        RequestCodeScreen(
            email = "alex.doyle@",
            onEmailChange = {},
            onSendCode = {},
            emailError = DomainError.InvalidEmail()
        )
    }
}

@Preview
@Composable
private fun RequestCodeScreenOfflinePreview() {
    MusterTheme {
        RequestCodeScreen(
            email = "alex.doyle@gmail.com",
            onEmailChange = {},
            onSendCode = {},
            error = DomainError.Network()
        )
    }
}

// 2a: terminal form error, button greys.
@Preview
@Composable
private fun RequestCodeScreenRateLimitedPreview() {
    MusterTheme {
        RequestCodeScreen(
            email = "alex.doyle@gmail.com",
            onEmailChange = {},
            onSendCode = {},
            error = DomainError.RateLimited(),
            canSend = false
        )
    }
}
