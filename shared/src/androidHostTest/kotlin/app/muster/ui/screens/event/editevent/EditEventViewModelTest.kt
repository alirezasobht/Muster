package app.muster.ui.screens.event.editevent

import app.muster.data.fake.FakeEventRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.MusterTimeZone
import app.muster.domain.usecase.UpdateEventUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import org.junit.Rule

class EditEventViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val startDate = LocalDate(2099, 1, 7)
    private val startTime = LocalTime(19, 0)

    private val initial = EditEventInitial(
        eventId = "event-1",
        groupId = "group-1",
        title = "Weekly 7-a-side",
        startsAt = LocalDateTime(startDate, startTime).toInstant(MusterTimeZone),
        location = "Westgate Pitch 2"
    )

    private fun viewModel(events: FakeEventRepository = FakeEventRepository()) = EditEventViewModel(initial, UpdateEventUseCase(events))

    @Test
    fun `starts with the event's values in Sydney time and nothing to save`() {
        val state = viewModel().state.value
        assertEquals("Weekly 7-a-side", state.title)
        assertEquals(startDate, state.date)
        assertEquals(startTime, state.time)
        assertEquals("Westgate Pitch 2", state.location)
        assertFalse(state.canSave)
    }

    @Test
    fun `changing any field enables save`() {
        val viewModel = viewModel()
        viewModel.onTimeChange(LocalTime(20, 0))
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun `changing a field back disables save again`() {
        val viewModel = viewModel()
        viewModel.onLocationChange("Westgate Pitch 3")
        viewModel.onLocationChange("Westgate Pitch 2 ")
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `a blank title cannot be saved`() {
        val viewModel = viewModel()
        viewModel.onTitleChange("   ")
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `saving trims and reports saved`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events)
        viewModel.onTitleChange("  Weekly 7-a-side, late  ")
        viewModel.onTimeChange(LocalTime(20, 0))
        viewModel.onLocationChange("   ")
        viewModel.onSave()
        assertTrue(viewModel.state.value.saving)
        advanceUntilIdle()

        val updated = events.updatedEvents.single()
        assertEquals("Weekly 7-a-side, late", updated.title)
        assertEquals(LocalDateTime(startDate, LocalTime(20, 0)).toInstant(MusterTimeZone), updated.startsAt)
        assertNull(updated.location)
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun `a second tap while saving is ignored`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events)
        viewModel.onTitleChange("Weekly 7-a-side, late")
        viewModel.onSave()
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(1, events.updatedEvents.size)
    }

    @Test
    fun `a past start time is a date-time field error`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events)
        viewModel.onDateChange(LocalDate(2000, 1, 1))
        viewModel.onSave()
        advanceUntilIdle()

        assertIs<DomainError.EventStartsInPast>(viewModel.state.value.dateTimeError)
        assertNull(viewModel.state.value.error)
        assertTrue(events.updatedEvents.isEmpty())
    }

    // The event started while the form was open: retrying can't help.
    @Test
    fun `a frozen event is a terminal form error`() = runTest {
        val viewModel = viewModel(FakeEventRepository(updateEventError = DomainError.EventFrozen()))
        viewModel.onTitleChange("Weekly 7-a-side, late")
        viewModel.onSave()
        advanceUntilIdle()

        assertIs<DomainError.EventFrozen>(viewModel.state.value.error)
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `a network error leaves save live`() = runTest {
        val viewModel = viewModel(FakeEventRepository(updateEventError = DomainError.Network()))
        viewModel.onTitleChange("Weekly 7-a-side, late")
        viewModel.onSave()
        advanceUntilIdle()

        assertIs<DomainError.Network>(viewModel.state.value.error)
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun `changing the date clears a past-start error`() = runTest {
        val viewModel = viewModel()
        viewModel.onDateChange(LocalDate(2000, 1, 1))
        viewModel.onSave()
        advanceUntilIdle()

        viewModel.onDateChange(LocalDate(2099, 1, 14))
        assertNull(viewModel.state.value.dateTimeError)
    }
}
