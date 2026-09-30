package app.muster.ui.screens.newevent

import app.muster.data.fake.FakeEventRepository
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.CreateEventUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule

class NewEventViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val groupId = "group-1"
    private val futureDate = LocalDate(2099, 1, 1)
    private val futureTime = LocalTime(19, 0)

    private fun viewModel(events: FakeEventRepository = FakeEventRepository()) = NewEventViewModel(groupId, CreateEventUseCase(events))

    private fun NewEventViewModel.fillValidForm() {
        onTitleChange("Weekly 7-a-side")
        onDateChange(futureDate)
        onTimeChange(futureTime)
    }

    @Test
    fun `starts empty`() {
        assertEquals(NewEventUiState(), viewModel().state.value)
    }

    @Test
    fun `creating reports the event`() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidForm()
        viewModel.onCreate()
        assertTrue(viewModel.state.value.creating)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.creating)
        assertEquals("Weekly 7-a-side", state.created?.title)
        assertNull(state.error)
    }

    @Test
    fun `the title and location are trimmed before creating`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events)
        viewModel.onTitleChange("  Weekly 7-a-side  ")
        viewModel.onDateChange(futureDate)
        viewModel.onTimeChange(futureTime)
        viewModel.onLocationChange("  Westgate Pitch 2  ")
        viewModel.onCreate()
        advanceUntilIdle()

        assertEquals(listOf("Weekly 7-a-side"), events.createdEventTitles)
        assertEquals("Westgate Pitch 2", viewModel.state.value.created?.location)
    }

    @Test
    fun `a blank title creates nothing`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events)
        viewModel.onDateChange(futureDate)
        viewModel.onTimeChange(futureTime)
        viewModel.onCreate()
        advanceUntilIdle()

        assertTrue(events.createdEventTitles.isEmpty())
        assertNull(viewModel.state.value.created)
    }

    @Test
    fun `no date or time creates nothing`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events)
        viewModel.onTitleChange("Weekly 7-a-side")
        viewModel.onCreate()
        advanceUntilIdle()

        assertTrue(events.createdEventTitles.isEmpty())
    }

    @Test
    fun `a second tap while creating is ignored`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events)
        viewModel.fillValidForm()
        viewModel.onCreate()
        viewModel.onCreate()
        advanceUntilIdle()

        assertEquals(1, events.createdEventTitles.size)
    }

    @Test
    fun `a past start time is a date-time field error`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events)
        viewModel.onTitleChange("Weekly 7-a-side")
        viewModel.onDateChange(LocalDate(2000, 1, 1))
        viewModel.onTimeChange(futureTime)
        viewModel.onCreate()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.EventStartsInPast>(state.dateTimeError)
        assertNull(state.error)
        assertTrue(events.createdEventTitles.isEmpty())
    }

    @Test
    fun `a failed create surfaces the error`() = runTest {
        val events = FakeEventRepository(createEventError = DomainError.NotSignedIn())
        val viewModel = viewModel(events)
        viewModel.fillValidForm()
        viewModel.onCreate()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.NotSignedIn>(state.error)
        assertNull(state.dateTimeError)
        assertEquals(false, state.creating)
        assertNull(state.created)
    }

    // Retryable: the button is the retry, so it stays live.
    @Test
    fun `a network error leaves canCreate true`() = runTest {
        val events = FakeEventRepository(createEventError = DomainError.Network())
        val viewModel = viewModel(events)
        viewModel.fillValidForm()
        viewModel.onCreate()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.canCreate)
    }

    @Test
    fun `typing clears both errors`() = runTest {
        val events = FakeEventRepository(createEventError = DomainError.Network())
        val viewModel = viewModel(events)
        viewModel.fillValidForm()
        viewModel.onCreate()
        advanceUntilIdle()
        assertIs<DomainError.Network>(viewModel.state.value.error)

        viewModel.onTitleChange("Weekly 7-a-side, take two")
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `changing the date clears a past-start error`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events)
        viewModel.onTitleChange("Weekly 7-a-side")
        viewModel.onDateChange(LocalDate(2000, 1, 1))
        viewModel.onTimeChange(futureTime)
        viewModel.onCreate()
        advanceUntilIdle()
        assertIs<DomainError.EventStartsInPast>(viewModel.state.value.dateTimeError)

        viewModel.onDateChange(futureDate)
        assertNull(viewModel.state.value.dateTimeError)
    }

    // Navigating away and back must not recreate the event.
    @Test
    fun `handling the result resets the screen`() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidForm()
        viewModel.onCreate()
        advanceUntilIdle()

        viewModel.onCreatedHandled()
        assertEquals(NewEventUiState(), viewModel.state.value)
    }
}
