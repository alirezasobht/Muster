package app.muster.ui.screens.group.members.membertagname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.CreateTagUseCase
import app.muster.domain.usecase.RenameTagUseCase
import app.muster.ui.common.ErrorPresentation
import app.muster.ui.common.presentation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface MemberTagNameMode {

    // "New tag…" from Move to tag creates the tag and moves this member in, in one call.
    data class New(val memberId: String? = null) : MemberTagNameMode

    data class Rename(
        val tagId: String,
        val tagName: String,
        val memberCount: Int
    ) : MemberTagNameMode
}

class MemberTagNameDialogViewModel(
    private val groupId: String,
    private val mode: MemberTagNameMode,
    private val createTag: CreateTagUseCase,
    private val renameTag: RenameTagUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(
        when (mode) {
            is MemberTagNameMode.New -> MemberTagNameDialogUiState()
            is MemberTagNameMode.Rename -> MemberTagNameDialogUiState(
                name = mode.tagName,
                isRename = true,
                initialName = mode.tagName,
                memberCount = mode.memberCount
            )
        }
    )
    val state: StateFlow<MemberTagNameDialogUiState> = _state.asStateFlow()

    // The host removes the dialog, and this ViewModel with it, when this fires. Channel rather
    // than SharedFlow so a save that finishes while the collector is away is not lost.
    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    fun onNameChange(name: String) {
        if (_state.value.saving) return
        _state.value = _state.value.copy(name = name, nameError = null, error = null)
    }

    fun onSubmit() {
        val current = _state.value
        if (current.saving || !current.canSubmit) return

        val name = current.name.trim()
        _state.value = current.copy(saving = true, nameError = null, error = null)
        viewModelScope.launch {
            try {
                when (mode) {
                    is MemberTagNameMode.New -> createTag(groupId, name, listOfNotNull(mode.memberId))
                    is MemberTagNameMode.Rename -> renameTag(groupId, mode.tagId, name)
                }
                // Stays saving: the Members tab reloads from the repository's change
                // notification, and the dialog is on its way out.
                _saved.send(Unit)
            } catch (e: DomainError) {
                _state.value = if (e.presentation is ErrorPresentation.Field) {
                    current.copy(saving = false, nameError = e)
                } else {
                    current.copy(saving = false, error = e)
                }
            }
        }
    }
}
