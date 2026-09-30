package app.muster.ui.screens.signin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.AdaptedCodeInput
import app.muster.ui.common.components.FormError
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_continue
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.enter_code_resend
import muster.shared.generated.resources.enter_code_resend_in
import muster.shared.generated.resources.enter_code_sent_to
import muster.shared.generated.resources.enter_code_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EnterCodeRoute(
    email: String,
    onBack: () -> Unit,
    viewModel: EnterCodeViewModel = koinViewModel { parametersOf(email) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EnterCodeScreen(
        email = state.email,
        code = state.code,
        onCodeChange = viewModel::onCodeChange,
        onContinue = viewModel::onVerify,
        onResend = viewModel::onResend,
        onBack = onBack,
        resendInSeconds = state.resendInSeconds,
        verifying = state.verifying,
        codeError = state.codeError,
        error = state.error,
        canVerify = state.canVerify
    )
}

@Composable
fun EnterCodeScreen(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    onContinue: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    resendInSeconds: Int = 0,
    verifying: Boolean = false,
    codeError: DomainError? = null,
    error: DomainError? = null,
    canVerify: Boolean = code.length == CODE_LENGTH
) {
    val complete = code.length == CODE_LENGTH

    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding()) {
                Box(
                    modifier = Modifier.height(56.dp).padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(onClick = onBack, enabled = !verifying) {
                        Icon(
                            imageVector = MusterIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.content_description_back),
                            tint = MusterColors.Ink
                        )
                    }
                }
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(Res.string.enter_code_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.enter_code_sent_to, email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusterColors.Muted
                    )
                    Spacer(Modifier.height(36.dp))
                    AdaptedCodeInput(
                        value = code,
                        onValueChange = onCodeChange,
                        length = CODE_LENGTH,
                        enabled = !verifying,
                        onDone = { if (complete) onContinue() }
                    )
                    if (codeError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = codeError.toMessage(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MusterColors.OutText
                        )
                    }
                    if (error != null) {
                        Spacer(Modifier.height(16.dp))
                        FormError(message = error.toMessage(), modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton(
                        text = stringResource(Res.string.action_continue),
                        onClick = onContinue,
                        enabled = canVerify,
                        loading = verifying
                    )
                    Spacer(Modifier.height(8.dp))
                    ResendLine(resendInSeconds = resendInSeconds, onResend = onResend)
                }
            }
        }
    }
}

@Composable
private fun ResendLine(
    resendInSeconds: Int,
    onResend: () -> Unit
) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (resendInSeconds > 0) {
            Text(
                text = stringResource(Res.string.enter_code_resend_in, formatSeconds(resendInSeconds)),
                style = style,
                color = MusterColors.Hint,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            Text(
                text = stringResource(Res.string.enter_code_resend),
                style = style.copy(fontWeight = FontWeight.SemiBold),
                color = MusterColors.Ink,
                modifier = Modifier.clickable(onClick = onResend).padding(12.dp)
            )
        }
    }
}

private fun formatSeconds(total: Int): String = "${total / 60}:${(total % 60).toString().padStart(2, '0')}"

@Preview
@Composable
private fun EnterCodeScreenPreview() {
    MusterTheme {
        EnterCodeScreen(
            email = "alex.doyle@gmail.com",
            code = "418",
            onCodeChange = {},
            onContinue = {},
            onResend = {},
            onBack = {},
            resendInSeconds = 42
        )
    }
}

@Preview
@Composable
private fun EnterCodeScreenErrorPreview() {
    MusterTheme {
        EnterCodeScreen(
            email = "alex.doyle@gmail.com",
            code = "418027",
            onCodeChange = {},
            onContinue = {},
            onResend = {},
            onBack = {},
            codeError = DomainError.InvalidCode()
        )
    }
}

@Preview
@Composable
private fun EnterCodeScreenFormErrorPreview() {
    MusterTheme {
        EnterCodeScreen(
            email = "alex.doyle@gmail.com",
            code = "",
            onCodeChange = {},
            onContinue = {},
            onResend = {},
            onBack = {},
            error = DomainError.RateLimited(),
            canVerify = false
        )
    }
}
