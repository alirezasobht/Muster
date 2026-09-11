package app.muster.ui.screens.launch

import app.muster.domain.model.Profile

sealed interface LaunchUiState {
    data object Loading : LaunchUiState
    data object Failed : LaunchUiState
    data object SignedOut : LaunchUiState
    data object NeedsName : LaunchUiState
    data class Ready(val profile: Profile) : LaunchUiState
}
