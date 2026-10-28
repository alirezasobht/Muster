package app.muster.ui.screens.group.members.membertagname

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.FormError
import app.muster.ui.common.components.MusterTextField
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_save
import muster.shared.generated.resources.field_name
import muster.shared.generated.resources.tag_name_admins_hint
import muster.shared.generated.resources.tag_name_cancel
import muster.shared.generated.resources.tag_name_create
import muster.shared.generated.resources.tag_name_members_hint
import muster.shared.generated.resources.tag_name_new_title
import muster.shared.generated.resources.tag_name_rename_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class MemberTagNameActions(
    val onNameChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onDismiss: () -> Unit
)

// The caller composes this only while the dialog is open and removes it on onDismiss.
@Composable
fun MemberTagNameDialogRoute(
    groupId: String,
    mode: MemberTagNameMode,
    onDismiss: () -> Unit,
    viewModel: MemberTagNameDialogViewModel = koinViewModel { parametersOf(groupId, mode) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.saved.collect { onDismiss() } }

    MemberTagNameDialog(
        state = state,
        actions = MemberTagNameActions(
            onNameChange = viewModel::onNameChange,
            onSubmit = viewModel::onSubmit,
            onDismiss = onDismiss
        )
    )
}

// 5n–5r: New tag and Rename tag are the same dialog.
@Composable
fun MemberTagNameDialog(
    state: MemberTagNameDialogUiState,
    actions: MemberTagNameActions
) {
    // While saving, closing it would orphan the in-flight call.
    Dialog(onDismissRequest = { if (!state.saving) actions.onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MusterColors.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                MemberTagNameTitle(isRename = state.isRename)
                Spacer(Modifier.height(16.dp))
                MemberTagNameField(state = state, actions = actions)
                Spacer(Modifier.height(8.dp))
                MemberTagNameHint(isRename = state.isRename, memberCount = state.memberCount)
                Spacer(Modifier.height(20.dp))
                if (state.error != null) {
                    FormError(message = state.error.toMessage())
                    Spacer(Modifier.height(12.dp))
                }
                MemberTagNameButtons(state = state, actions = actions)
            }
        }
    }
}

@Composable
private fun MemberTagNameTitle(isRename: Boolean) {
    Text(
        text = stringResource(if (isRename) Res.string.tag_name_rename_title else Res.string.tag_name_new_title),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MusterColors.Ink
    )
}

@Composable
private fun MemberTagNameField(
    state: MemberTagNameDialogUiState,
    actions: MemberTagNameActions
) {
    val focusRequester = remember { FocusRequester() }
    MusterTextField(
        value = state.name,
        onValueChange = actions.onNameChange,
        label = stringResource(Res.string.field_name),
        enabled = !state.saving,
        error = state.nameError?.toMessage(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) actions.onSubmit() }),
        modifier = Modifier.focusRequester(focusRequester)
    )
    // Rename isn't focused: the field can't place the cursor after the pre-filled name.
    if (!state.isRename) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun MemberTagNameHint(
    isRename: Boolean,
    memberCount: Int
) {
    Text(
        text = if (isRename && memberCount > 0) {
            pluralStringResource(Res.plurals.tag_name_members_hint, memberCount, memberCount)
        } else {
            stringResource(Res.string.tag_name_admins_hint)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MusterColors.Hint
    )
}

@Composable
private fun MemberTagNameButtons(
    state: MemberTagNameDialogUiState,
    actions: MemberTagNameActions
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = actions.onDismiss,
            enabled = !state.saving,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MusterColors.Outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MusterColors.Ink),
            modifier = Modifier.weight(1f).height(52.dp)
        ) {
            Text(text = stringResource(Res.string.tag_name_cancel), style = MaterialTheme.typography.labelLarge)
        }
        PrimaryButton(
            text = stringResource(if (state.isRename) Res.string.action_save else Res.string.tag_name_create),
            onClick = actions.onSubmit,
            enabled = state.canSubmit,
            loading = state.saving,
            modifier = Modifier.weight(1f)
        )
    }
}

private val PreviewActions = MemberTagNameActions(onNameChange = {}, onSubmit = {}, onDismiss = {})

@Composable
private fun MemberTagNamePreview(state: MemberTagNameDialogUiState) {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                MemberTagNameDialog(state = state, actions = PreviewActions)
            }
        }
    }
}

// 5n
@Preview
@Composable
private fun MemberTagNameNewPreview() {
    MemberTagNamePreview(MemberTagNameDialogUiState(name = "Regular"))
}

@Preview
@Composable
private fun MemberTagNameEditPreview() {
    MemberTagNamePreview(MemberTagNameDialogUiState(name = "Regular", initialName = "Regular", isRename = true))
}

@Preview
@Composable
private fun MemberTagNameBlankPreview() {
    MemberTagNamePreview(MemberTagNameDialogUiState())
}

@Preview
@Composable
private fun MemberTagNameSavingPreview() {
    MemberTagNamePreview(MemberTagNameDialogUiState(name = "Regular", saving = true))
}

@Preview
@Composable
private fun MemberTagNameFailedPreview() {
    MemberTagNamePreview(MemberTagNameDialogUiState(name = "Regular", error = DomainError.Network()))
}

// 5r
@Preview
@Composable
private fun MemberTagNameRenamePreview() {
    MemberTagNamePreview(
        MemberTagNameDialogUiState(name = "Casual", isRename = true, initialName = "Casual", memberCount = 3)
    )
}
