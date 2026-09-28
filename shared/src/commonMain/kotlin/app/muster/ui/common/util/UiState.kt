package app.muster.ui.common.util

interface UiState<S> {
    fun asSuccessOrNull(): S? = null
}
