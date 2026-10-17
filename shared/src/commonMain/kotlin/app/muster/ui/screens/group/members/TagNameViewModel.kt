package app.muster.ui.screens.group.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.CreateTagUseCase
import app.muster.domain.usecase.RenameTagUseCase
import app.muster.ui.common.ErrorPresentation
import app.muster.ui.common.presentation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TagNameViewModel(
    private val groupId: String,
    private val createTag: CreateTagUseCase,
    private val renameTag: RenameTagUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<TagNameUiState>(TagNameUiState.Hidden)
    val state: StateFlow<TagNameUiState> = _state.asStateFlow()

    // "New tag…" from Move to tag creates the tag and moves this member in, in one call.
    private var memberToMove: String? = null

    fun onNewTag(memberId: String? = null) {
        memberToMove = memberId
        _state.value = TagNameUiState.Open(mode = TagNameMode.New)
    }

    fun onRename(
        tagId: String,
        currentName: String,
        memberCount: Int
    ) {
        memberToMove = null
        _state.value = TagNameUiState.Open(
            mode = TagNameMode.Rename(tagId = tagId, memberCount = memberCount),
            name = currentName
        )
    }

    fun onNameChange(name: String) {
        val current = _state.value as? TagNameUiState.Open ?: return
        if (current.saving) return
        _state.value = current.copy(name = name, nameError = null, error = null)
    }

    fun onDismiss() {
        val current = _state.value as? TagNameUiState.Open ?: return
        if (!current.saving) _state.value = TagNameUiState.Hidden
    }

    fun onSubmit() {
        val current = _state.value as? TagNameUiState.Open ?: return
        if (current.saving || !current.canSubmit) return

        val name = current.name.trim()
        _state.value = current.copy(saving = true, nameError = null, error = null)
        viewModelScope.launch {
            try {
                when (val mode = current.mode) {
                    is TagNameMode.New -> createTag(groupId, name, listOfNotNull(memberToMove))
                    is TagNameMode.Rename -> renameTag(groupId, mode.tagId, name)
                }
                // The Members tab reloads from the repository's change notification.
                _state.value = TagNameUiState.Hidden
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
