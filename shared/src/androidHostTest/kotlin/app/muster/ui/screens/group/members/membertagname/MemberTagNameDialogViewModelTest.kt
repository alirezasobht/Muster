package app.muster.ui.screens.group.members.membertagname

import app.muster.data.fake.FakeMemberRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.MemberTag
import app.muster.domain.usecase.CreateTagUseCase
import app.muster.domain.usecase.RenameTagUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Rule

class MemberTagNameDialogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val casual = MemberTag(id = "t1", name = "Casual", memberIds = listOf("a", "b"))
    private val renameCasual = MemberTagNameMode.Rename(tagId = "t1", tagName = "Casual", memberCount = 2)

    private fun members(
        createError: DomainError? = null,
        renameError: DomainError? = null
    ) = FakeMemberRepository(memberTags = listOf(casual), createTagError = createError, renameTagError = renameError)

    private fun viewModel(
        mode: MemberTagNameMode,
        members: FakeMemberRepository = members()
    ) = MemberTagNameDialogViewModel("group-1", mode, CreateTagUseCase(members), RenameTagUseCase(members))

    private suspend fun MemberTagNameDialogViewModel.hasSaved() = withTimeoutOrNull(1_000) { saved.first() } != null

    @Test
    fun `new starts blank and cannot be submitted`() {
        val state = viewModel(MemberTagNameMode.New()).state.value

        assertEquals(MemberTagNameDialogUiState(), state)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `rename starts with the current name and member count`() {
        val state = viewModel(renameCasual).state.value

        assertEquals(
            MemberTagNameDialogUiState(name = "Casual", isRename = true, initialName = "Casual", memberCount = 2),
            state
        )
    }

    @Test
    fun `rename cannot be submitted until the name changes`() {
        val viewModel = viewModel(renameCasual)
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onNameChange("Social")
        assertTrue(viewModel.state.value.canSubmit)

        viewModel.onNameChange("Casual")
        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `creating adds the tag and reports saved`() = runTest {
        val members = members()
        val viewModel = viewModel(MemberTagNameMode.New(), members)
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        assertTrue(viewModel.state.value.saving)
        advanceUntilIdle()

        assertTrue(viewModel.hasSaved())
        assertEquals(listOf("Casual", "Regular"), members.tags.map { it.name })
    }

    @Test
    fun `the name is trimmed before saving`() = runTest {
        val members = members()
        val viewModel = viewModel(MemberTagNameMode.New(), members)
        viewModel.onNameChange("  Regular  ")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals("Regular", members.tags.last().name)
    }

    @Test
    fun `new from Move to tag creates the tag with that member`() = runTest {
        val members = members()
        val viewModel = viewModel(MemberTagNameMode.New(memberId = "a"), members)
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(listOf("a"), members.tags.last().memberIds)
        assertEquals(listOf("b"), members.tags.first().memberIds)
    }

    @Test
    fun `a plain new tag starts with no member`() = runTest {
        val members = members()
        val viewModel = viewModel(MemberTagNameMode.New(), members)
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(emptyList(), members.tags.last().memberIds)
    }

    @Test
    fun `renaming changes the name and reports saved`() = runTest {
        val members = members()
        val viewModel = viewModel(renameCasual, members)
        viewModel.onNameChange("Social")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(viewModel.hasSaved())
        assertEquals("Social", members.tags.single().name)
    }

    @Test
    fun `a blank name submits nothing`() = runTest {
        val members = members()
        val viewModel = viewModel(MemberTagNameMode.New(), members)
        viewModel.onNameChange("   ")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(listOf("Casual"), members.tags.map { it.name })
        assertFalse(viewModel.state.value.saving)
        assertFalse(viewModel.hasSaved())
    }

    @Test
    fun `an unchanged name submits nothing`() = runTest {
        val members = members()
        val viewModel = viewModel(renameCasual, members)
        viewModel.onSubmit()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.saving)
        assertFalse(viewModel.hasSaved())
    }

    @Test
    fun `a taken name is a field error and keeps the dialog open`() = runTest {
        val viewModel = viewModel(MemberTagNameMode.New(), members(createError = DomainError.TagNameTaken("Casual")))
        viewModel.onNameChange("Casual")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.TagNameTaken>(state.nameError)
        assertNull(state.error)
        assertFalse(state.saving)
        assertEquals("Casual", state.name)
        assertFalse(viewModel.hasSaved())
    }

    @Test
    fun `a name that is too long is a field error`() = runTest {
        val viewModel = viewModel(renameCasual, members(renameError = DomainError.TagNameTooLong()))
        viewModel.onNameChange("x".repeat(31))
        viewModel.onSubmit()
        advanceUntilIdle()

        assertIs<DomainError.TagNameTooLong>(viewModel.state.value.nameError)
    }

    @Test
    fun `a network failure is a form error and can be retried`() = runTest {
        val members = members(createError = DomainError.Network())
        val viewModel = viewModel(MemberTagNameMode.New(), members)
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.Network>(state.error)
        assertNull(state.nameError)
        assertTrue(state.canSubmit)

        members.createTagError = null
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(viewModel.hasSaved())
    }

    @Test
    fun `a terminal failure leaves nothing to submit`() = runTest {
        val viewModel = viewModel(renameCasual, members(renameError = DomainError.TagListChanged()))
        viewModel.onNameChange("Social")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `typing clears the errors`() = runTest {
        val viewModel = viewModel(MemberTagNameMode.New(), members(createError = DomainError.TagNameTaken("Casual")))
        viewModel.onNameChange("Casual")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.nameError)

        viewModel.onNameChange("Casuals")

        assertNull(viewModel.state.value.nameError)
    }

    @Test
    fun `typing while saving is ignored`() = runTest {
        val viewModel = viewModel(MemberTagNameMode.New())
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()

        viewModel.onNameChange("Other")

        assertEquals("Regular", viewModel.state.value.name)
        advanceUntilIdle()
    }

    @Test
    fun `submitting twice while saving creates one tag`() = runTest {
        val members = members()
        val viewModel = viewModel(MemberTagNameMode.New(), members)
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(2, members.tags.size)
    }
}
