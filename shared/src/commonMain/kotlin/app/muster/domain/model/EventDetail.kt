package app.muster.domain.model

data class EventDetail(
    val event: Event,
    val roster: List<RosterEntry>,
    val standby: List<StandbyEntry>,
    // Null on a locked event when the locker deleted their account or their
    // profile isn't visible.
    val lockedByName: String?
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
