package app.muster.domain.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Repositories announce writes here; ViewModels subscribe to the changes their
 * screen cares about.
 *
 * SharedFlow, not Channel: this is a broadcast, and an emission with no
 * listener is meant to be dropped. A screen that does not exist yet loads
 * fresh when it is created, so it has nothing to catch up on.
 *
 * Repositories notify, not ViewModels. Every caller of a write gets the
 * announcement, so a new call site cannot forget to make one.
 */
class DataChanges {

    private val _changes = MutableSharedFlow<DataChange>(extraBufferCapacity = 16)
    val changes: Flow<DataChange> = _changes.asSharedFlow()

    fun notify(change: DataChange) {
        _changes.tryEmit(change)
    }
}
