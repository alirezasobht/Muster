package app.muster.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_sign_out
import muster.shared.generated.resources.home_signed_in_as
import org.jetbrains.compose.resources.stringResource

// Placeholder. Replaced by the real Home (1d); sign-out moves to Settings (1f).
@Composable
fun HomeScreen(
    name: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding().padding(24.dp)) {
                Text(text = "Muster", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.home_signed_in_as, name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusterColors.Muted
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onSignOut, shape = MaterialTheme.shapes.medium) {
                    Text(text = stringResource(Res.string.action_sign_out), color = MusterColors.Ink)
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    MusterTheme { HomeScreen(name = "Alex Doyle", onSignOut = {}) }
}
