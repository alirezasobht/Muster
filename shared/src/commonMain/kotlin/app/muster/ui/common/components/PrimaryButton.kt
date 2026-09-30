package app.muster.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    // Inverted for the green ground (1a, 1r): white fill, dark green label.
    onAccent: Boolean = false
) {
    val container = if (onAccent) MusterColors.White else MusterColors.Accent
    val content = if (onAccent) MusterColors.InText else MusterColors.White
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            // Loading is disabled too, but must stay full colour behind the spinner.
            disabledContainerColor = container.copy(alpha = if (loading) 1f else 0.4f),
            disabledContentColor = content
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = content,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview
@Composable
private fun PrimaryButtonPreview() {
    MusterTheme {
        Surface {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton(text = "Send code", onClick = {})
                PrimaryButton(text = "Continue", onClick = {}, enabled = false)
                PrimaryButton(text = "Continue", onClick = {}, loading = true)
            }
        }
    }
}

@Preview
@Composable
private fun PrimaryButtonOnAccentPreview() {
    MusterTheme {
        Surface(color = MusterColors.Accent) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton(text = "Send code", onClick = {}, onAccent = true)
                PrimaryButton(text = "Send code", onClick = {}, enabled = false, onAccent = true)
            }
        }
    }
}
