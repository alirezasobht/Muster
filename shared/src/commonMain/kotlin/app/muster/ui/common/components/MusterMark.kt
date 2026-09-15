package app.muster.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import app.muster.ui.common.util.SharedTransitionKeys
import app.muster.ui.common.util.sharedBoundsOrNone
import app.muster.ui.common.util.sharedElementOrNone
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme

// The white "M" tile. 88 dp on launch (1q, 1r), 44 dp on sign-in (1a).
// Corner radius and glyph size scale with it, per the frames.
@Composable
fun MusterMark(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .sharedElementOrNone(SharedTransitionKeys.MARK)
            .size(size)
            .background(MusterColors.White, RoundedCornerShape(0.295f * size)),
        contentAlignment = Alignment.Center
    ) {
        val glyph = (size.value * 0.5f).sp
        Text(
            text = "M",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = glyph,
                lineHeight = glyph,
                letterSpacing = -0.03f * glyph.value.sp
            ),
            color = MusterColors.Accent
        )
    }
}

@Composable
fun MusterWordmark(fontSize: Int, modifier: Modifier = Modifier) {
    Text(
        text = "MUSTER",
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.17f * fontSize.sp
        ),
        color = MusterColors.White,
        modifier = modifier.sharedBoundsOrNone(SharedTransitionKeys.WORDMARK)
    )
}

@Preview
@Composable
private fun MusterMarkPreview() {
    MusterTheme {
        Surface(color = MusterColors.Accent) {
            Row(
                modifier = Modifier.size(width = 260.dp, height = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MusterMark(size = 88.dp)
                MusterMark(size = 44.dp)
                MusterWordmark(fontSize = 15)
            }
        }
    }
}
