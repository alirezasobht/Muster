package app.muster.ui.screens.addplayers

import app.muster.domain.error.DomainError
import app.muster.ui.common.util.UiState
import app.muster.ui.common.util.initials

data class CandidateRow(
    val id: String,
    val name: String,
    val initials: String = name.initials()
)

// tells ui that checked is invite or standby
sealed interface PickDestination {
    data object Invited : PickDestination
    data class Standby(val position: Int) : PickDestination
}

sealed interface AddPlayersUiState : UiState<AddPlayersUiState.Success> {

    data object Loading : AddPlayersUiState

    data class Error(val error: DomainError) : AddPlayersUiState

    data class Success(
        val candidates: List<CandidateRow> = emptyList(),
        // Ordered by when each id was checked, not by candidates' display order.
        val selectedIds: List<String> = emptyList(),
        val capacity: Int = 0,
        val freeSlots: Int = 0,
        val queueLength: Int = 0,
        val adding: Boolean = false,
        val error: DomainError? = null
    ) : AddPlayersUiState {
        override fun asSuccessOrNull(): Success = this
        val isEmpty: Boolean get() = candidates.isEmpty()
        val canConfirm: Boolean get() = selectedIds.isNotEmpty() && !adding

        // how many of the currently checked players will actually get invited
        val inviting: Int get() = minOf(selectedIds.size, freeSlots)
        // how many of the currently checked players will be on standby
        val standbyPicks: Int get() = selectedIds.size - inviting

        fun destinationOf(id: String): PickDestination? {
            val index = selectedIds.indexOf(id)
            if (index == -1) return null
            return if (index < freeSlots) {
                PickDestination.Invited
            } else {
                PickDestination.Standby(queueLength + (index - freeSlots) + 1)
            }
        }
    }
}
