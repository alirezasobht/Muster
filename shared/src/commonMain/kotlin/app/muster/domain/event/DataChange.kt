package app.muster.domain.event

/**
 * A write that another screen's data depends on. Add cases as they are needed;
 * a case nobody subscribes to is dead weight.
 */
sealed interface DataChange {
    data object MyGroups : DataChange
    data class Members(val groupId: String) : DataChange
    data class Events(val groupId: String) : DataChange
}
