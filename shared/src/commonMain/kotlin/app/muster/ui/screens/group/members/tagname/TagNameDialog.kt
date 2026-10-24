package app.muster.ui.screens.group.members.tagname

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

data class TagNameActions(
    val onNameChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onDismiss: () -> Unit
)

// 5n–5r: New tag and Rename tag are the same dialog.
@Composable
fun TagNameDialog(
    state: TagNameDialogUiState,
    actions: TagNameActions
) {
    if (state !is TagNameDialogUiState.Open) return
    val rename = state.mode as? TagNameMode.Rename

    // While saving, closing it would orphan the in-flight call.
    Dialog(onDismissRequest = { if (!state.saving) actions.onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MusterColors.White) {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(
                        if (rename == null) Res.string.tag_name_new_title else Res.string.tag_name_rename_title
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MusterColors.Ink
                )
                Spacer(Modifier.height(16.dp))
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (rename == null || rename.memberCount == 0) {
                        stringResource(Res.string.tag_name_admins_hint)
                    } else {
                        pluralStringResource(Res.plurals.tag_name_members_hint, rename.memberCount, rename.memberCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MusterColors.Hint
                )
                Spacer(Modifier.height(20.dp))
                if (state.error != null) {
                    FormError(message = state.error.toMessage())
                    Spacer(Modifier.height(12.dp))
                }
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
                        text = stringResource(if (rename == null) Res.string.tag_name_create else Res.string.action_save),
                        onClick = actions.onSubmit,
                        enabled = state.canSubmit,
                        loading = state.saving,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Rename isn't focused: the field can't place the cursor after the pre-filled name.
            if (rename == null) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        }
    }
}

private val PreviewActions = TagNameActions(onNameChange = {}, onSubmit = {}, onDismiss = {})

@Composable
private fun TagNamePreview(state: TagNameDialogUiState) {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                TagNameDialog(state = state, actions = PreviewActions)
            }
        }
    }
}

// 5n
@Preview
@Composable
private fun TagNameNewPreview() {
    TagNamePreview(TagNameDialogUiState.Open(mode = TagNameMode.New, name = "Regular"))
}

@Preview
@Composable
private fun TagNameBlankPreview() {
    TagNamePreview(TagNameDialogUiState.Open(mode = TagNameMode.New))
}

@Preview
@Composable
private fun TagNameSavingPreview() {
    TagNamePreview(TagNameDialogUiState.Open(mode = TagNameMode.New, name = "Regular", saving = true))
}

@Preview
@Composable
private fun TagNameFailedPreview() {
    TagNamePreview(TagNameDialogUiState.Open(mode = TagNameMode.New, name = "Regular", error = DomainError.Network()))
}

// 5r
@Preview
@Composable
private fun TagNameRenamePreview() {
    TagNamePreview(
        TagNameDialogUiState.Open(mode = TagNameMode.Rename(tagId = "t1", memberCount = 3), name = "Casual")
    )
}
