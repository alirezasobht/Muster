package app.muster.domain.usecase

import app.muster.data.fake.FakeEventRepository
import app.muster.data.fake.FakeMemberRepository
import app.muster.domain.model.Event
import app.muster.domain.model.EventDetail
import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.RosterEntry
import app.muster.domain.model.RsvpStatus
import app.muster.domain.model.StandbyEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class GetInviteeCandidatesUseCaseTest {

    private val eventId = "event-1"
    private val groupId = "group-1"

    private val groupMembers = listOf(
        Member(profileId = "player-1", name = "Alex Doyle", role = GroupRole.Admin),
        Member(profileId = "player-2", name = "Dan Whelan", role = GroupRole.Member),
        Member(profileId = "player-3", name = "Sam Okafor", role = GroupRole.Member),
        Member(profileId = "player-4", name = "Priya Nair", role = GroupRole.Member),
        Member(profileId = "player-5", name = "Leo Fanning", role = GroupRole.Member)
    )

    private fun detail(
        capacity: Int = 3,
        roster: List<RosterEntry> = listOf(RosterEntry("player-1", "Alex Doyle", RsvpStatus.In)),
        standby: List<StandbyEntry> = emptyList()
    ) = EventDetail(
        event = Event(
            id = eventId,
            groupId = groupId,
            title = "Weekly 7-a-side",
            startsAt = Instant.parse("2099-01-01T09:00:00Z"),
            location = "Westgate Pitch 2",
            capacity = capacity,
            inCount = roster.count { it.status == RsvpStatus.In },
            pendingCount = roster.count { it.status == RsvpStatus.Pending },
            myStatus = null
        ),
        roster = roster,
        standby = standby
    )

    private fun useCase(
        events: FakeEventRepository = FakeEventRepository(eventDetail = detail()),
        members: FakeMemberRepository = FakeMemberRepository(members = groupMembers)
    ) = GetInviteeCandidatesUseCase(events, members)

    @Test
    fun `excludes anyone already on the roster or the queue`() = runTest {
        val events = FakeEventRepository(
            eventDetail = detail(
                roster = listOf(RosterEntry("player-1", "Alex Doyle", RsvpStatus.In)),
                standby = listOf(StandbyEntry("player-2", "Dan Whelan"))
            )
        )

        val result = useCase(events = events)(eventId, groupId)

        assertEquals(setOf("player-3", "player-4", "player-5"), result.members.map { it.profileId }.toSet())
    }

    @Test
    fun `an out player is excluded from candidates but frees no slot`() = runTest {
        val events = FakeEventRepository(
            eventDetail = detail(
                capacity = 3,
                roster = listOf(
                    RosterEntry("player-1", "Alex Doyle", RsvpStatus.In),
                    RosterEntry("player-2", "Dan Whelan", RsvpStatus.Out)
                )
            )
        )

        val result = useCase(events = events)(eventId, groupId)

        assertTrue(result.members.none { it.profileId == "player-2" })
        assertEquals(2, result.freeSlots)
    }

    @Test
    fun `capacity, free slots and queue length come from the event`() = runTest {
        val events = FakeEventRepository(
            eventDetail = detail(
                capacity = 3,
                roster = listOf(RosterEntry("player-1", "Alex Doyle", RsvpStatus.In)),
                standby = listOf(StandbyEntry("player-2", "Dan Whelan"))
            )
        )

        val result = useCase(events = events)(eventId, groupId)

        assertEquals(3, result.capacity)
        assertEquals(2, result.freeSlots)
        assertEquals(1, result.queueLength)
    }

    @Test
    fun `every member already on the event leaves no candidates`() = runTest {
        val events = FakeEventRepository(
            eventDetail = detail(roster = groupMembers.map { RosterEntry(it.profileId, it.name, RsvpStatus.In) })
        )

        val result = useCase(events = events)(eventId, groupId)

        assertTrue(result.members.isEmpty())
    }
}
