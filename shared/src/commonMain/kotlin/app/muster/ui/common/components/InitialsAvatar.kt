package app.muster.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme

@Composable
fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fontSize: TextUnit = 14.sp
) {
    AvatarCircle(
        text = initials,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = fontSize, fontWeight = FontWeight.SemiBold),
        color = MusterColors.Secondary,
        size = size,
        modifier = modifier.background(MusterColors.AvatarFill, RoundedCornerShape(percent = 50))
    )
}

// Compose has no built-in dashed border, so it's drawn by hand — same
// technique as MessageState's DashedIconPlaceholder, circular and smaller.
@Composable
fun PendingAvatar(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val stroke = Stroke(
        width = with(density) { 1.5.dp.toPx() },
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
    )
    AvatarCircle(
        text = "@",
        style = LocalTextStyle.current.copy(fontSize = 16.sp),
        color = MusterColors.Hint,
        size = 40.dp,
        modifier = modifier.drawBehind {
            drawOval(color = MusterColors.DashedOutline, style = stroke, size = Size(size.width, size.height))
        }
    )
}

@Composable
private fun AvatarCircle(
    text: String,
    style: TextStyle,
    color: Color,
    size: Dp,
    modifier: Modifier
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Text(text = text, style = style, color = color)
    }
}

@Preview
@Composable
private fun AvatarsPreview() {
    MusterTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(initials = "AD")
                Spacer(Modifier.width(8.dp))
                InitialsAvatar(initials = "AD", size = 34.dp, fontSize = 12.5.sp)
                Spacer(Modifier.width(8.dp))
                PendingAvatar()
            }
        }
    }
}
