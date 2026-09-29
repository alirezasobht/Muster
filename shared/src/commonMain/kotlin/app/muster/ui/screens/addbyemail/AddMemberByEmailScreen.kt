package app.muster.ui.screens.addbyemail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.ConfirmDialog
import app.muster.ui.common.components.FormError
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.MusterTextField
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_send_invite
import muster.shared.generated.resources.add_by_email_close
import muster.shared.generated.resources.add_by_email_hint
import muster.shared.generated.resources.add_by_email_invite_more
import muster.shared.generated.resources.add_by_email_success_body
import muster.shared.generated.resources.add_by_email_success_title
import muster.shared.generated.resources.content_description_close
import muster.shared.generated.resources.field_email
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddMemberByEmailRoute(
    groupId: String,
    groupName: String,
    onClose: () -> Unit,
    viewModel: AddMemberByEmailViewModel = koinViewModel { parametersOf(groupId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AddMemberByEmailScreen(
        groupName = groupName,
        email = state.email,
        onEmailChange = viewModel::onEmailChange,
        onInvite = viewModel::onInvite,
        onClose = onClose,
        sending = state.sending,
        emailError = state.emailError,
        error = state.error,
        canInvite = state.canInvite,
        invited = state.invited,
        onInviteMore = viewModel::onInviteMore
    )
}

@Composable
fun AddMemberByEmailScreen(
    groupName: String,
    email: String,
    onEmailChange: (String) -> Unit,
    onInvite: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    sending: Boolean = false,
    emailError: DomainError? = null,
    error: DomainError? = null,
    canInvite: Boolean = email.isNotBlank(),
    invited: Boolean = false,
    onInviteMore: () -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding().fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = MusterIcons.Close,
                            contentDescription = stringResource(Res.string.content_description_close),
                            tint = MusterColors.Ink,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    MusterTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = stringResource(Res.string.field_email),
                        enabled = !sending,
                        error = emailError?.toMessage(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (canInvite) onInvite() })
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.add_by_email_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MusterColors.Muted
                    )
                    Spacer(Modifier.height(24.dp))
                    if (error != null) {
                        FormError(message = error.toMessage())
                        Spacer(Modifier.height(12.dp))
                    }
                    PrimaryButton(
                        text = stringResource(Res.string.action_send_invite),
                        onClick = onInvite,
                        enabled = canInvite,
                        loading = sending
                    )
                }
            }
        }
    }

    if (invited) {
        ConfirmDialog(
            title = stringResource(Res.string.add_by_email_success_title),
            body = stringResource(Res.string.add_by_email_success_body),
            confirmText = stringResource(Res.string.add_by_email_invite_more),
            cancelText = stringResource(Res.string.add_by_email_close),
            onConfirm = onInviteMore,
            onDismiss = onClose
        )
    }
}

private const val PREVIEW_GROUP_NAME = "Westgate Wednesday 7s"

@Preview
@Composable
private fun AddMemberByEmailScreenEmptyPreview() {
    MusterTheme {
        AddMemberByEmailScreen(
            groupName = PREVIEW_GROUP_NAME,
            email = "",
            onEmailChange = {},
            onInvite = {},
            onClose = {}
        )
    }
}

@Preview
@Composable
private fun AddMemberByEmailScreenTypedPreview() {
    MusterTheme {
        AddMemberByEmailScreen(
            groupName = PREVIEW_GROUP_NAME,
            email = "priya.n@gmail.com",
            onEmailChange = {},
            onInvite = {},
            onClose = {}
        )
    }
}

@Preview
@Composable
private fun AddMemberByEmailScreenSendingPreview() {
    MusterTheme {
        AddMemberByEmailScreen(
            groupName = PREVIEW_GROUP_NAME,
            email = "priya.n@gmail.com",
            onEmailChange = {},
            onInvite = {},
            onClose = {},
            sending = true
        )
    }
}

// 2b/2c live on the field here — AlreadyInvited/AlreadyMember (step 2) are
// field errors, same slot as this one.
@Preview
@Composable
private fun AddMemberByEmailScreenFieldErrorPreview() {
    MusterTheme {
        AddMemberByEmailScreen(
            groupName = PREVIEW_GROUP_NAME,
            email = "not-an-email",
            onEmailChange = {},
            onInvite = {},
            onClose = {},
            emailError = DomainError.InvalidEmail()
        )
    }
}

// 2c: retryable form error, button stays live.
@Preview
@Composable
private fun AddMemberByEmailScreenFormErrorPreview() {
    MusterTheme {
        AddMemberByEmailScreen(
            groupName = PREVIEW_GROUP_NAME,
            email = "priya.n@gmail.com",
            onEmailChange = {},
            onInvite = {},
            onClose = {},
            error = DomainError.Network()
        )
    }
}

@Preview
@Composable
private fun AddMemberByEmailScreenDialogPreview() {
    MusterTheme {
        AddMemberByEmailScreen(
            groupName = PREVIEW_GROUP_NAME,
            email = "priya.n@gmail.com",
            onEmailChange = {},
            onInvite = {},
            onClose = {},
            invited = true
        )
    }
}
