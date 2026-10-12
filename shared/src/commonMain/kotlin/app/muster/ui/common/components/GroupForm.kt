package app.muster.ui.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.domain.error.DomainError
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.field_group_name
import org.jetbrains.compose.resources.stringResource

// Shared by New group and Edit group.
@Composable
fun GroupForm(
    screenTitle: String,
    actionText: String,
    name: String,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    submitting: Boolean = false,
    nameError: DomainError? = null,
    error: DomainError? = null,
    canSubmit: Boolean = name.isNotBlank()
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding().fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MusterIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.content_description_back),
                            tint = MusterColors.Ink,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    MusterTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = stringResource(Res.string.field_group_name),
                        enabled = !submitting,
                        error = nameError?.toMessage(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmit() })
                    )
                    if (hint != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MusterColors.Muted
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    if (error != null) {
                        FormError(message = error.toMessage())
                        Spacer(Modifier.height(12.dp))
                    }
                    PrimaryButton(
                        text = actionText,
                        onClick = onSubmit,
                        enabled = canSubmit,
                        loading = submitting
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun GroupFormWithHintPreview() {
    MusterTheme {
        GroupForm(
            screenTitle = "New group",
            actionText = "Create group",
            name = "Westgate Wednesday 7s",
            onNameChange = {},
            onSubmit = {},
            onBack = {},
            hint = "You’ll be its first admin. You can add players once it exists."
        )
    }
}

@Preview
@Composable
private fun GroupFormErrorsPreview() {
    MusterTheme {
        GroupForm(
            screenTitle = "Edit group",
            actionText = "Save",
            name = "W",
            onNameChange = {},
            onSubmit = {},
            onBack = {},
            nameError = DomainError.InvalidName(),
            error = DomainError.Network()
        )
    }
}
