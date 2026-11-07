package app.muster.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(percent = 50),
        color = MusterColors.AvatarFill
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = fontSize, fontWeight = FontWeight.SemiBold),
                color = MusterColors.Secondary
            )
        }
    }
}

@Preview
@Composable
private fun InitialsAvatarPreview() {
    MusterTheme {
        Surface {
            InitialsAvatar(initials = "AD")
        }
    }
}

@Preview
@Composable
private fun InitialsAvatarSmallPreview() {
    MusterTheme {
        Surface {
            InitialsAvatar(initials = "AD", size = 34.dp, fontSize = 12.5.sp)
        }
    }
}
