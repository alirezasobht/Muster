package app.muster.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme

// The tonal header action: "+ Add" on the event's players (4a), "+ New tag" on the members count line (5a).
@Composable
fun TonalPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(percent = 50),
        color = MusterColors.InFill
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = MusterColors.InText)
        }
    }
}

@Preview
@Composable
private fun TonalPillPreview() {
    MusterTheme {
        Surface {
            TonalPill(text = "+ New tag", onClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
