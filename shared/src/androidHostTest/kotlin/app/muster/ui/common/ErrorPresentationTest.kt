package app.muster.ui.common

import app.muster.domain.error.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorPresentationTest {

    @Test
    fun `field errors are about the contents of a field`() {
        assertEquals(ErrorPresentation.Field, DomainError.InvalidEmail().presentation)
        assertEquals(ErrorPresentation.Field, DomainError.InvalidCode().presentation)
        assertEquals(ErrorPresentation.Field, DomainError.InvalidName().presentation)
    }

    @Test
    fun `network and unknown errors are retryable form errors`() {
        assertEquals(
            ErrorPresentation.Form(ErrorPresentation.Severity.Retryable),
            DomainError.Network().presentation
        )
        assertEquals(
            ErrorPresentation.Form(ErrorPresentation.Severity.Retryable),
            DomainError.Unknown(RuntimeException("boom")).presentation
        )
    }

    @Test
    fun `permission and rate-limit errors are terminal form errors`() {
        assertEquals(
            ErrorPresentation.Form(ErrorPresentation.Severity.Terminal),
            DomainError.RateLimited().presentation
        )
        assertEquals(
            ErrorPresentation.Form(ErrorPresentation.Severity.Terminal),
            DomainError.NotAllowedToCreateGroups().presentation
        )
        assertEquals(
            ErrorPresentation.Form(ErrorPresentation.Severity.Terminal),
            DomainError.InvitationNotPending().presentation
        )
        assertEquals(
            ErrorPresentation.Form(ErrorPresentation.Severity.Terminal),
            DomainError.NotSignedIn().presentation
        )
    }

    @Test
    fun `tag name errors are field errors`() {
        assertEquals(ErrorPresentation.Field, DomainError.TagNameTaken("Regular").presentation)
        assertEquals(ErrorPresentation.Field, DomainError.TagNameTooLong().presentation)
    }

    @Test
    fun `a changed tag list is a terminal form error`() {
        assertEquals(
            ErrorPresentation.Form(ErrorPresentation.Severity.Terminal),
            DomainError.TagListChanged().presentation
        )
    }

    @Test
    fun `only a changed tag list among the tag errors is terminal`() {
        assertTrue(DomainError.TagListChanged().isTerminal)
        assertFalse(DomainError.TagNameTaken("Regular").isTerminal)
        assertFalse(DomainError.TagNameTooLong().isTerminal)
    }
}
