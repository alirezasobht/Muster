package app.muster.ui.screens.addplayers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.AddPlayersUseCase
import app.muster.domain.usecase.GetInviteeCandidatesUseCase
import app.muster.ui.common.util.updateSuccess
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddPlayersViewModel(
    private val eventId: String,
    private val groupId: String,
    private val getInviteeCandidates: GetInviteeCandidatesUseCase,
    private val addPlayers: AddPlayersUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<AddPlayersUiState>(AddPlayersUiState.Loading)
    val state: StateFlow<AddPlayersUiState> = _state.asStateFlow()

    private val _exit = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val exit: SharedFlow<Unit> = _exit.asSharedFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onToggle(profileId: String) = _state.updateSuccess { current ->
        if (current.adding) {
            current
        } else {
            val selected = current.selectedIds
            current.copy(selectedIds = if (profileId in selected) selected - profileId else selected + profileId)
        }
    }

    fun onConfirm() {
        val current = _state.value as? AddPlayersUiState.Success ?: return
        if (!current.canConfirm) return
        _state.updateSuccess { it.copy(adding = true, error = null) }
        viewModelScope.launch {
            try {
                addPlayers(eventId, groupId, current.selectedIds)
                _state.updateSuccess { it.copy(adding = false) }
                _exit.emit(Unit)
            } catch (e: DomainError) {
                _state.updateSuccess { it.copy(adding = false, error = e) }
            }
        }
    }

    private fun load() {
        _state.value = AddPlayersUiState.Loading
        viewModelScope.launch {
            try {
                _state.value = fetchSuccess()
            } catch (e: DomainError) {
                _state.value = AddPlayersUiState.Error(e)
            }
        }
    }

    private suspend fun fetchSuccess(): AddPlayersUiState.Success {
        val result = getInviteeCandidates(eventId, groupId)
        return AddPlayersUiState.Success(
            candidates = result.members
                .map { CandidateRow(id = it.profileId, name = it.name) }
                .sortedBy { it.name.lowercase() },
            capacity = result.capacity,
            freeSlots = result.freeSlots,
            queueLength = result.queueLength
        )
    }
}
