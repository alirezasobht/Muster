package app.muster.domain.model

data class EventDetail(
    val event: Event,
    val roster: List<RosterEntry>,
    val standby: List<StandbyEntry>,
    // Null when locker isn't visible/exist anymore.
    val lockedByName: String?,
    // Null when canceller isn't visible/exist anymore.
    val cancelledByName: String? = null
)

// Standby players have no event_invitations row, This is invitees only
data class RosterEntry(
    val profileId: String,
    val name: String,
    val status: RsvpStatus
)

data class StandbyEntry(
    val profileId: String,
    val name: String
)
