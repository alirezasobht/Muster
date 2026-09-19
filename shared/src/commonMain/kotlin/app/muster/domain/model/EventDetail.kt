package app.muster.domain.model

data class EventDetail(
    val event: Event,
    val roster: List<RosterEntry>
)

// Standby players have no event_invitations row, This is invitees only
data class RosterEntry(
    val profileId: String,
    val name: String,
    val status: RsvpStatus
)
