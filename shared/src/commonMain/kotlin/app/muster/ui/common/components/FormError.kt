package app.muster.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme

// Form-error block, DESIGN.md "Two kinds of error" (2b). The action button
// below it is always the retry, so no onAccent variant and no button here.
@Composable
fun FormError(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MusterColors.OutFill, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(1.5.dp, MusterColors.OutText, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
                color = MusterColors.OutText
            )
        }
        Text(
            // 14px matches DESIGN.md exactly; no typography token sits between
            // bodySmall (13) and bodyMedium (15).
            text = message,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MusterColors.OutText
        )
    }
}

@Preview
@Composable
private fun FormErrorPreview() {
    MusterTheme {
        Surface {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FormError(message = "You’re not allowed to create groups. Ask a group admin to invite you.")
                FormError(message = "Couldn’t reach Muster. Check your connection and try again.")
            }
        }
    }
}

@Preview
@Composable
private fun FormErrorOnAccentPreview() {
    MusterTheme {
        Surface(color = MusterColors.Accent) {
            Column(Modifier.padding(24.dp)) {
                FormError(message = "Too many attempts. Try again in 10 minutes.")
            }
        }
    }
}
