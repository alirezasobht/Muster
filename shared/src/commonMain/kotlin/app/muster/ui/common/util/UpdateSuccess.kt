package app.muster.ui.common.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Reads the latest state at write time. Capturing it before a suspend and
// copying afterwards loses whatever another in-flight call wrote meanwhile.
fun <T, S : T> MutableStateFlow<T>.updateSuccess(block: (S) -> S) where T : UiState<S> {
    update { current -> current.asSuccessOrNull()?.let(block) ?: current }
}
