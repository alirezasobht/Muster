package app.muster.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.UpdateNameUseCase
import app.muster.ui.common.util.updateSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getMyProfile: GetMyProfileUseCase,
    private val updateName: UpdateNameUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    // A one-shot event, not state: "Saved" must not reappear when the screen
    // comes back from the background. Channel rather than SharedFlow so an
    // emission with no collector is buffered instead of dropped.
    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        load()
    }

    fun onRetryLoad() = load()

    fun onNameChange(name: String) {
        _state.updateSuccess { it.copy(name = name, saveError = null) }
    }

    fun onSave() {
        val current = _state.value as? SettingsUiState.Success ?: return
        if (current.name.isBlank() || current.saving || current.isSameName) return
        // Set before launching, not inside: the guard above must see it on a
        // second tap in the same frame, whatever dispatcher is in play.
        _state.updateSuccess { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                val profile = updateName(current.name)
                _state.updateSuccess {
                    it.copy(
                        name = profile.name.orEmpty(),
                        savedName = profile.name.orEmpty(),
                        email = profile.email,
                        saving = false
                    )
                }
                _saved.send(Unit)
            } catch (e: DomainError) {
                _state.updateSuccess { it.copy(saving = false, saveError = e) }
            }
        }
    }

    private fun load() {
        _state.value = SettingsUiState.Loading
        viewModelScope.launch {
            try {
                val profile = getMyProfile()
                _state.value = SettingsUiState.Success(
                    name = profile.name.orEmpty(),
                    savedName = profile.name.orEmpty(),
                    email = profile.email
                )
            } catch (e: DomainError) {
                _state.value = SettingsUiState.Error(e)
            }
        }
    }
}
