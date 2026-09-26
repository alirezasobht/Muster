package app.muster.ui.screens.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.muster.SupabaseConfig
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.platform.WebPage
import app.muster.ui.theme.MusterColors
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.settings_privacy_policy
import org.jetbrains.compose.resources.stringResource

// Shows the published page, webApp/src/webMain/resources/privacy.html, so the
// policy has one source.
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
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
                        text = stringResource(Res.string.settings_privacy_policy),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                // .html, not /privacy: the local dev server has no clean URLs.
                WebPage(
                    url = "${SupabaseConfig.WEB_APP_URL}/privacy.html",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
