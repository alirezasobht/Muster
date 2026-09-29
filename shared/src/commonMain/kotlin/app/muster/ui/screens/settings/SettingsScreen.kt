package app.muster.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.APP_VERSION
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.AdaptedNameField
import app.muster.ui.common.components.ConfirmDialog
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.MusterTextField
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import kotlinx.coroutines.delay
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_save
import muster.shared.generated.resources.action_sign_out
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.field_email
import muster.shared.generated.resources.settings_delete_account
import muster.shared.generated.resources.settings_discard_body
import muster.shared.generated.resources.settings_discard_cancel
import muster.shared.generated.resources.settings_discard_confirm
import muster.shared.generated.resources.settings_discard_title
import muster.shared.generated.resources.settings_load_failed_title
import muster.shared.generated.resources.settings_privacy_policy
import muster.shared.generated.resources.settings_saved
import muster.shared.generated.resources.settings_title
import muster.shared.generated.resources.settings_try_again
import muster.shared.generated.resources.settings_version
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

data class SettingsActions(
    val onNameChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onRetryLoad: () -> Unit,
    val onBack: () -> Unit,
    val onSignOut: () -> Unit,
    val onDeleteAccount: () -> Unit,
    val onOpenPrivacyPolicy: () -> Unit
)

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    deleteAccountViewModel: DeleteAccountViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.saved.collect {
            showSaved = true
            delay(2000)
            showSaved = false
        }
    }

    val requestBack = {
        val current = state
        if (current is SettingsUiState.Success && !current.isSameName) {
            confirmDiscard = true
        } else {
            onBack()
        }
    }

    SettingsScreen(
        state = state,
        actions = SettingsActions(
            onNameChange = viewModel::onNameChange,
            onSave = viewModel::onSave,
            onRetryLoad = viewModel::onRetryLoad,
            onBack = requestBack,
            onSignOut = onSignOut,
            onDeleteAccount = deleteAccountViewModel::onStart,
            onOpenPrivacyPolicy = onOpenPrivacyPolicy
        ),
        showSaved = showSaved
    )

    DeleteAccountRoute(viewModel = deleteAccountViewModel)

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_discard_title),
            body = stringResource(Res.string.settings_discard_body),
            confirmText = stringResource(Res.string.settings_discard_confirm),
            cancelText = stringResource(Res.string.settings_discard_cancel),
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false }
        )
    }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    showSaved: Boolean = false,
    spinnerDelayMillis: Long = 400
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding().fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = MusterIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.content_description_back),
                            tint = MusterColors.Ink,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                when (state) {
                    is SettingsUiState.Loading -> Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        MusterSpinner(delayMillis = spinnerDelayMillis)
                    }
                    is SettingsUiState.Error -> Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorContent(error = state.error, onRetry = actions.onRetryLoad)
                    }
                    is SettingsUiState.Success -> {
                        Spacer(Modifier.height(24.dp))
                        FormContent(state = state, actions = actions, showSaved = showSaved)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: DomainError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.settings_load_failed_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = error.toMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MusterColors.Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry, shape = MaterialTheme.shapes.medium) {
            Text(text = stringResource(Res.string.settings_try_again), color = MusterColors.Ink)
        }
    }
}

@Composable
private fun FormContent(
    state: SettingsUiState.Success,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    showSaved: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        AdaptedNameField(
            value = state.name,
            onValueChange = actions.onNameChange,
            enabled = !state.saving,
            error = state.saveError?.toMessage(),
            onDone = { if (state.name.isNotBlank()) actions.onSave() }
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = stringResource(Res.string.action_save),
            onClick = actions.onSave,
            enabled = state.name.isNotBlank() && !state.isSameName,
            loading = state.saving
        )
        if (showSaved) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.settings_saved),
                style = MaterialTheme.typography.bodySmall,
                color = MusterColors.Muted
            )
        }
        Spacer(Modifier.height(24.dp))
        MusterTextField(
            value = state.email,
            onValueChange = {},
            label = stringResource(Res.string.field_email),
            readOnly = true
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = actions.onSignOut, shape = MaterialTheme.shapes.medium) {
            Text(text = stringResource(Res.string.action_sign_out), color = MusterColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = actions.onOpenPrivacyPolicy, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.settings_privacy_policy),
                color = MusterColors.Ink,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline
            )
        }
        TextButton(onClick = actions.onDeleteAccount, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.settings_delete_account),
                color = MusterColors.Ink,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.settings_version, APP_VERSION),
            style = MaterialTheme.typography.bodySmall,
            color = MusterColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
    }
}

private val PreviewActions = SettingsActions(
    onNameChange = {},
    onSave = {},
    onRetryLoad = {},
    onBack = {},
    onSignOut = {},
    onDeleteAccount = {},
    onOpenPrivacyPolicy = {}
)

@Preview
@Composable
private fun SettingsScreenLoadingPreview() {
    MusterTheme {
        SettingsScreen(
            state = SettingsUiState.Loading,
            actions = PreviewActions,
            spinnerDelayMillis = 0
        )
    }
}

@Preview
@Composable
private fun SettingsScreenLoadFailedPreview() {
    MusterTheme {
        SettingsScreen(
            state = SettingsUiState.Error(DomainError.Network()),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    MusterTheme {
        SettingsScreen(
            state = SettingsUiState.Success(
                name = "Alex Doyle",
                savedName = "Alex Doyle",
                email = "alex.doyle@gmail.com"
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun SettingsScreenDirtyPreview() {
    MusterTheme {
        SettingsScreen(
            state = SettingsUiState.Success(
                name = "Alex D",
                savedName = "Alex Doyle",
                email = "alex.doyle@gmail.com"
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun SettingsScreenSavedPreview() {
    MusterTheme {
        SettingsScreen(
            state = SettingsUiState.Success(
                name = "Alex Doyle",
                savedName = "Alex Doyle",
                email = "alex.doyle@gmail.com"
            ),
            actions = PreviewActions,
            showSaved = true
        )
    }
}

@Preview
@Composable
private fun SettingsScreenSavingPreview() {
    MusterTheme {
        SettingsScreen(
            state = SettingsUiState.Success(
                name = "Alex Doyle",
                savedName = "Alex D",
                email = "alex.doyle@gmail.com",
                saving = true
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun SettingsScreenSaveFailedPreview() {
    MusterTheme {
        SettingsScreen(
            state = SettingsUiState.Success(
                name = "",
                email = "alex.doyle@gmail.com",
                saveError = DomainError.InvalidName()
            ),
            actions = PreviewActions
        )
    }
}
