package app.muster.ui.screens.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LaunchRoute(viewModel: LaunchViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchScreen(
        failed = state == LaunchUiState.Failed,
        onRetry = viewModel::onRetry,
        onSignOut = viewModel::onSignOut
    )
}
