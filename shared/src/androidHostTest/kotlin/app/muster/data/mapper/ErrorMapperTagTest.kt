package app.muster.data.mapper

import app.muster.domain.error.DomainError
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class ErrorMapperTagTest {

    private fun postgrest(
        code: String?,
        error: String
    ): PostgrestRestException = mockk<PostgrestRestException>(relaxed = true) {
        every { this@mockk.code } returns code
        every { this@mockk.error } returns error
    }

    @Test
    fun `a tag name over the length check is TagNameTooLong`() {
        val mapped = postgrest("23514", "violates check constraint \"group_tags_name_length\"").toDomainError()

        assertIs<DomainError.TagNameTooLong>(mapped)
    }

    @Test
    fun `the profile name check still maps to InvalidName`() {
        val mapped = postgrest("23514", "violates check constraint \"profiles_name_check\"").toDomainError()

        assertIs<DomainError.InvalidName>(mapped)
    }

    @Test
    fun `any other check violation is unknown`() {
        val exception = postgrest("23514", "violates check constraint \"something_else\"")

        val mapped = exception.toDomainError()

        assertIs<DomainError.Unknown>(mapped)
        assertSame(exception, mapped.cause)
    }

    @Test
    fun `a missing tag raised by the database is TagListChanged`() {
        val mapped = postgrest("P0001", "tag not found").toDomainError()

        assertIs<DomainError.TagListChanged>(mapped)
    }

    @Test
    fun `an unrecognised raise exception is still unknown`() {
        assertIs<DomainError.Unknown>(postgrest("P0001", "something unexpected").toDomainError())
    }

    // The unique-index violation becomes TagNameTaken in MemberRepositoryImpl,
    // which knows the submitted name; the mapper alone has nothing to quote.
    @Test
    fun `a tag name unique violation is unknown at the mapper`() {
        val mapped = postgrest("23505", "duplicate key value violates unique constraint \"group_tags_name_unique\"")
            .toDomainError()

        assertIs<DomainError.Unknown>(mapped)
    }
}
