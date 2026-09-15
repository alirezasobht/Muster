package app.muster.ui.common

import app.muster.domain.error.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
