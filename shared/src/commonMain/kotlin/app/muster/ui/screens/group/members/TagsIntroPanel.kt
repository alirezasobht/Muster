package app.muster.ui.screens.group.members

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.members_new_tag
import muster.shared.generated.resources.tags_intro_body
import muster.shared.generated.resources.tags_intro_title
import org.jetbrains.compose.resources.stringResource

// 5m: shown to admins while the group has no tags. It holds the only New
// tag button on screen, so the count line's button stays hidden meanwhile.
@Composable
fun TagsIntroPanel(
    onNewTag: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MusterColors.QuietSurface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(Res.string.tags_intro_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MusterColors.Ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.tags_intro_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MusterColors.Secondary
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNewTag,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MusterColors.Accent,
                    contentColor = MusterColors.White
                ),
                contentPadding = PaddingValues(horizontal = 18.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text(text = stringResource(Res.string.members_new_tag), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview
@Composable
private fun TagsIntroPanelPreview() {
    MusterTheme {
        Surface {
            TagsIntroPanel(onNewTag = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
