package app.muster.ui.screens.setname

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.Profile
import app.muster.ui.common.components.NameField
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_continue
import muster.shared.generated.resources.set_name_subtitle
import muster.shared.generated.resources.set_name_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SetNameRoute(
    onNameSet: (Profile) -> Unit,
    viewModel: SetNameViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        state.saved?.let {
            onNameSet(it)
            viewModel.onSavedHandled()
        }
    }
    SetNameScreen(
        name = state.name,
        onNameChange = viewModel::onNameChange,
        onContinue = viewModel::onContinue,
        saving = state.saving,
        error = state.error
    )
}

@Composable
fun SetNameScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    saving: Boolean = false,
    error: DomainError? = null
) {
    val canContinue = name.isNotBlank()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding()) {
                Spacer(Modifier.height(56.dp))
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(Res.string.set_name_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.set_name_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusterColors.Muted
                    )
                    Spacer(Modifier.height(36.dp))
                    NameField(
                        value = name,
                        onValueChange = onNameChange,
                        enabled = !saving,
                        error = error?.toMessage(),
                        onDone = { if (canContinue) onContinue() },
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton(
                        text = stringResource(Res.string.action_continue),
                        onClick = onContinue,
                        enabled = canContinue,
                        loading = saving
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SetNameScreenPreview() {
    MusterTheme {
        SetNameScreen(name = "Alex Doyle", onNameChange = {}, onContinue = {})
    }
}
