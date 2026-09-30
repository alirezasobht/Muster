package app.muster.domain.model

data class EventDetail(
    val event: Event,
    val roster: List<RosterEntry>,
    val standby: List<StandbyEntry>
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
