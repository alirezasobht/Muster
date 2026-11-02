package app.muster.data.mapper

import app.muster.domain.error.DomainError
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class ErrorMapperTest {

    @Test
    fun `a connection failure is a network error`() {
        assertIs<DomainError.Network>(IOException("no route to host").toDomainError())
    }

    @Test
    fun `anything unrecognised is unknown`() {
        assertIs<DomainError.Unknown>(RuntimeException("boom").toDomainError())
    }

    // Mapping an already-mapped error would bury the specific case under
    // Unknown on the second pass.
    @Test
    fun `a DomainError passes through unchanged`() {
        val original = DomainError.InvalidCode()
        assertSame(original, original.toDomainError())
    }

    @Test
    fun `a write on a locked event is EventLocked`() {
        assertIs<DomainError.EventLocked>(raised("event is locked").toDomainError())
    }

    @Test
    fun `a write on a started event is still EventFrozen`() {
        assertIs<DomainError.EventFrozen>(raised("event has already started").toDomainError())
    }

    @Test
    fun `the cause is kept for diagnosis`() {
        val cause = RuntimeException("boom")
        assertSame(cause, cause.toDomainError().cause)
    }

    @Test
    fun `mapErrors returns the value when nothing throws`() = runTest {
        assertEquals(7, mapErrors { 7 })
    }

    @Test
    fun `mapErrors converts what it catches`() {
        assertFailsWith<DomainError.Network> {
            mapErrors { throw IOException("offline") }
        }
    }

    // A cancelled coroutine is not a failure. Mapping it would show an error
    // on screen every time the user navigated away mid-request.
    @Test
    fun `mapErrors lets cancellation through`() {
        assertFailsWith<CancellationException> {
            mapErrors { throw CancellationException("cancelled") }
        }
    }

    // Building a real one needs a Ktor HttpResponse; only code and error
    // are read by the mapper.
    private fun raised(message: String) = mockk<PostgrestRestException> {
        every { code } returns "P0001"
        every { error } returns message
    }
}
