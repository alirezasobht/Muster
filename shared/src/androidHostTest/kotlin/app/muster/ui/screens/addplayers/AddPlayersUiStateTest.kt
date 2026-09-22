package app.muster.ui.screens.addplayers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddPlayersUiStateTest {

    private val candidates = listOf(
        CandidateRow(id = "1", name = "Alex"),
        CandidateRow(id = "2", name = "Bea"),
        CandidateRow(id = "3", name = "Cal"),
        CandidateRow(id = "4", name = "Dee")
    )

    private fun state(
        selectedIds: List<String> = emptyList(),
        freeSlots: Int = 0,
        queueLength: Int = 0
    ) = AddPlayersUiState.Success(
        candidates = candidates,
        selectedIds = selectedIds,
        freeSlots = freeSlots,
        queueLength = queueLength
    )

    @Test
    fun `inviting is the selection size when it fits within free slots`() {
        assertEquals(2, state(selectedIds = listOf("1", "2"), freeSlots = 3).inviting)
    }

    @Test
    fun `inviting is capped at free slots when the selection overflows them`() {
        assertEquals(2, state(selectedIds = listOf("1", "2", "3"), freeSlots = 2).inviting)
    }

    @Test
    fun `inviting is zero with no free slots`() {
        assertEquals(0, state(selectedIds = listOf("1", "2"), freeSlots = 0).inviting)
    }

    @Test
    fun `standbyPicks is zero when the whole selection is invited`() {
        assertEquals(0, state(selectedIds = listOf("1", "2"), freeSlots = 3).standbyPicks)
    }

    @Test
    fun `standbyPicks is the overflow past free slots`() {
        assertEquals(1, state(selectedIds = listOf("1", "2", "3"), freeSlots = 2).standbyPicks)
    }

    @Test
    fun `standbyPicks is the whole selection with no free slots`() {
        assertEquals(2, state(selectedIds = listOf("1", "2"), freeSlots = 0).standbyPicks)
    }

    @Test
    fun `destinationOf is null for an id that was never checked`() {
        assertNull(state(selectedIds = listOf("1"), freeSlots = 5).destinationOf("2"))
    }

    @Test
    fun `destinationOf is Invited when its check-order index is within free slots`() {
        // Checked in the opposite of candidates' display order — rank comes
        // from selectedIds, not the candidates list.
        val s = state(selectedIds = listOf("2", "1"), freeSlots = 2)
        assertEquals(PickDestination.Invited, s.destinationOf("2"))
        assertEquals(PickDestination.Invited, s.destinationOf("1"))
    }

    @Test
    fun `destinationOf is Standby starting after the existing queue once free slots run out`() {
        val s = state(selectedIds = listOf("1", "2", "3"), freeSlots = 1, queueLength = 5)
        assertEquals(PickDestination.Invited, s.destinationOf("1"))
        assertEquals(PickDestination.Standby(6), s.destinationOf("2"))
        assertEquals(PickDestination.Standby(7), s.destinationOf("3"))
    }

    @Test
    fun `destinationOf numbers standby picks sequentially by check order, not display order`() {
        val s = state(selectedIds = listOf("4", "1"), freeSlots = 0, queueLength = 0)
        assertEquals(PickDestination.Standby(1), s.destinationOf("4"))
        assertEquals(PickDestination.Standby(2), s.destinationOf("1"))
    }
}
