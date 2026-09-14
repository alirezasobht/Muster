package app.muster.ui.screens.launch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.MusterMark
import app.muster.ui.common.components.MusterWordmark
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_sign_out
import muster.shared.generated.resources.launch_failed_body
import muster.shared.generated.resources.launch_failed_title
import muster.shared.generated.resources.launch_try_again
import org.jetbrains.compose.resources.stringResource

// 1q while loading, 1r when `failed`. The mark and wordmark never move; only
// the bottom zone swaps.
@Composable
fun LaunchScreen(
    modifier: Modifier = Modifier,
    failed: Boolean = false,
    onRetry: () -> Unit = {},
    // Null hides Sign out: there is no session to clear.
    onSignOut: (() -> Unit)? = null,
    // Longer than LaunchViewModel's one-second floor, so the spinner only
    // appears when loading genuinely outlasts the pause.
    spinnerDelayMillis: Long = 1200
) {
    Surface(modifier = modifier.fillMaxSize(), color = MusterColors.Accent) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MusterMark(size = 88.dp)
                    MusterWordmark(fontSize = 20)
                }
                Box(
                    modifier = Modifier.height(260.dp).fillMaxWidth(),
                    contentAlignment = if (failed) Alignment.TopCenter else Alignment.Center
                ) {
                    if (failed) {
                        Failure(onRetry = onRetry, onSignOut = onSignOut)
                    } else {
                        MusterSpinner(
                            delayMillis = spinnerDelayMillis,
                            color = MusterColors.White,
                            trackColor = MusterColors.White.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Failure(onRetry: () -> Unit, onSignOut: (() -> Unit)?) {
    Column(
        modifier = Modifier.padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.launch_failed_title),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MusterColors.White
        )
        Text(
            text = stringResource(Res.string.launch_failed_body),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.5.sp,
                lineHeight = 21.75.sp
            ),
            color = MusterColors.OnAccent,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = stringResource(Res.string.launch_try_again),
                onClick = onRetry,
                onAccent = true
            )
            if (onSignOut != null) {
                OutlinedButton(
                    onClick = onSignOut,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.5.dp, MusterColors.White.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MusterColors.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.action_sign_out),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LaunchScreenPreview() {
    MusterTheme { LaunchScreen(spinnerDelayMillis = 0) }
}

@Preview
@Composable
private fun LaunchScreenFailedPreview() {
    MusterTheme { LaunchScreen(failed = true, onSignOut = {}) }
}
