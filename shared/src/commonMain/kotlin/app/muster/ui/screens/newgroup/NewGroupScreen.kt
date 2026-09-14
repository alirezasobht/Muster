package app.muster.ui.screens.newgroup

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.Group
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.MusterTextField
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_create_group
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.field_group_name
import muster.shared.generated.resources.new_group_hint
import muster.shared.generated.resources.new_group_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewGroupRoute(
    onBack: () -> Unit,
    onGroupCreated: (Group) -> Unit,
    viewModel: NewGroupViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) {
        state.created?.let {
            onGroupCreated(it)
            viewModel.onCreatedHandled()
        }
    }
    NewGroupScreen(
        name = state.name,
        onNameChange = viewModel::onNameChange,
        onCreate = viewModel::onCreate,
        onBack = onBack,
        creating = state.creating,
        error = state.error
    )
}

@Composable
fun NewGroupScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    creating: Boolean = false,
    error: DomainError? = null
) {
    val canCreate = name.isNotBlank()

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
                        text = stringResource(Res.string.new_group_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    MusterTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = stringResource(Res.string.field_group_name),
                        enabled = !creating,
                        error = error?.toMessage(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (canCreate) onCreate() })
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.new_group_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MusterColors.Muted
                    )
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton(
                        text = stringResource(Res.string.action_create_group),
                        onClick = onCreate,
                        enabled = canCreate,
                        loading = creating
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NewGroupScreenEmptyPreview() {
    MusterTheme {
        NewGroupScreen(name = "", onNameChange = {}, onCreate = {}, onBack = {})
    }
}

@Preview
@Composable
private fun NewGroupScreenTypedPreview() {
    MusterTheme {
        NewGroupScreen(name = "Westgate Wednesday 7s", onNameChange = {}, onCreate = {}, onBack = {})
    }
}

@Preview
@Composable
private fun NewGroupScreenCreatingPreview() {
    MusterTheme {
        NewGroupScreen(
            name = "Westgate Wednesday 7s",
            onNameChange = {},
            onCreate = {},
            onBack = {},
            creating = true
        )
    }
}

@Preview
@Composable
private fun NewGroupScreenFailedPreview() {
    MusterTheme {
        NewGroupScreen(
            name = "Westgate Wednesday 7s",
            onNameChange = {},
            onCreate = {},
            onBack = {},
            error = DomainError.Network()
        )
    }
}
