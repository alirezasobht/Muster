package app.muster.ui

import androidx.compose.runtime.Composable
import app.muster.ui.navigation.NavGraph
import app.muster.ui.theme.MusterTheme

@Composable
fun MusterApp() {
    MusterTheme {
        NavGraph()
    }
}
