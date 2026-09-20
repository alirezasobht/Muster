package app.muster.ui.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme

object MusterIcons {

    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes("M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"),
            fill = SolidColor(Color.Black)
        ).build()
    }

    val MoreVert: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(
                "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2z" +
                    "M12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z" +
                    "M12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z"
            ),
            fill = SolidColor(Color.Black)
        ).build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(
                "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"
            ),
            fill = SolidColor(Color.Black)
        ).build()
    }

    val DragHandle: ImageVector by lazy {
        ImageVector.Builder(
            name = "DragHandle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(
                "M11,18c0,1.1 -0.9,2 -2,2s-2,-0.9 -2,-2 0.9,-2 2,-2 2,0.9 2,2z" +
                    "M9,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z" +
                    "M9,4C7.9,4 7,4.9 7,6s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z" +
                    "M15,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2z" +
                    "M15,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z" +
                    "M15,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z"
            ),
            fill = SolidColor(Color.Black)
        ).build()
    }

    val Refresh: ImageVector by lazy {
        ImageVector.Builder(
            name = "Refresh",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(
                "M17.65,6.35C16.2,4.9 14.21,4 12,4" +
                        "C7.58,4 4,7.58 4,12" +
                        "S7.58,20 12,20" +
                        "C15.73,20 18.84,17.45 19.73,14" +
                        "H17.65C16.83,16.33 14.61,18 12,18" +
                        "C8.69,18 6,15.31 6,12" +
                        "S8.69,6 12,6" +
                        "C13.66,6 15.14,6.69 16.22,7.78" +
                        "L13,11H20V4L17.65,6.35Z"
            ),
            fill = SolidColor(Color.Black),
        ).build()
    }
}

@Preview
@Composable
private fun IconsPreview() {
    MusterTheme {
        Surface {
            Column {
                IconPreview(imageVector = MusterIcons.ArrowBack)
                IconPreview(imageVector = MusterIcons.MoreVert)
                IconPreview(imageVector = MusterIcons.Close)
                IconPreview(imageVector = MusterIcons.DragHandle)
                IconPreview(imageVector = MusterIcons.Refresh)
            }
        }
    }
}

@Composable
private fun IconPreview(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MusterColors.Ink,
        modifier = Modifier.padding(16.dp)
    )
}
