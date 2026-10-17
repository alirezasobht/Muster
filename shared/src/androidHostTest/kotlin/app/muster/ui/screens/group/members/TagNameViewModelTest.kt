package app.muster.ui.screens.group.members

import app.muster.data.fake.FakeMemberRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.MemberTag
import app.muster.domain.usecase.CreateTagUseCase
import app.muster.domain.usecase.RenameTagUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class TagNameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val casual = MemberTag(id = "t1", name = "Casual", memberIds = listOf("a", "b"))

    private fun viewModel(members: FakeMemberRepository = FakeMemberRepository(memberTags = listOf(casual))) = TagNameViewModel("group-1", CreateTagUseCase(members), RenameTagUseCase(members))

    private fun TagNameViewModel.open() = state.value as TagNameUiState.Open

    @Test
    fun `starts hidden`() {
        assertEquals(TagNameUiState.Hidden, viewModel().state.value)
    }

    @Test
    fun `new opens blank and cannot be submitted`() {
        val viewModel = viewModel()

        viewModel.onNewTag()

        assertEquals(TagNameUiState.Open(mode = TagNameMode.New), viewModel.state.value)
        assertEquals(false, viewModel.open().canSubmit)
    }

    @Test
    fun `rename opens with the current name and member count`() {
        val viewModel = viewModel()

        viewModel.onRename(tagId = "t1", currentName = "Casual", memberCount = 2)

        assertEquals(
            TagNameUiState.Open(mode = TagNameMode.Rename(tagId = "t1", memberCount = 2), name = "Casual"),
            viewModel.state.value
        )
    }

    @Test
    fun `creating adds the tag and closes`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members)
        viewModel.onNewTag()
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        assertTrue(viewModel.open().saving)
        advanceUntilIdle()

        assertEquals(TagNameUiState.Hidden, viewModel.state.value)
        assertEquals(listOf("Regular"), members.tags.map { it.name })
    }

    @Test
    fun `the name is trimmed before saving`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members)
        viewModel.onNewTag()
        viewModel.onNameChange("  Regular  ")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(listOf("Regular"), members.tags.map { it.name })
    }

    @Test
    fun `new from Move to tag creates the tag with that member`() = runTest {
        val members = FakeMemberRepository(memberTags = listOf(casual))
        val viewModel = viewModel(members)
        viewModel.onNewTag(memberId = "a")
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(listOf("a"), members.tags.last().memberIds)
        assertEquals(listOf("b"), members.tags.first().memberIds)
    }

    @Test
    fun `a plain new tag after a move starts with no member`() = runTest {
        val members = FakeMemberRepository(memberTags = listOf(casual))
        val viewModel = viewModel(members)
        viewModel.onNewTag(memberId = "a")
        viewModel.onDismiss()
        viewModel.onNewTag()
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(emptyList(), members.tags.last().memberIds)
    }

    @Test
    fun `renaming changes the name and closes`() = runTest {
        val members = FakeMemberRepository(memberTags = listOf(casual))
        val viewModel = viewModel(members)
        viewModel.onRename(tagId = "t1", currentName = "Casual", memberCount = 2)
        viewModel.onNameChange("Social")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(TagNameUiState.Hidden, viewModel.state.value)
        assertEquals("Social", members.tags.single().name)
    }

    @Test
    fun `a blank name submits nothing`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members)
        viewModel.onNewTag()
        viewModel.onNameChange("   ")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(emptyList(), members.tags)
        assertEquals(false, viewModel.open().saving)
    }

    @Test
    fun `a taken name is a field error and keeps the dialog open`() = runTest {
        val viewModel = viewModel(FakeMemberRepository(createTagError = DomainError.TagNameTaken("Casual")))
        viewModel.onNewTag()
        viewModel.onNameChange("Casual")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.open()
        assertIs<DomainError.TagNameTaken>(state.nameError)
        assertNull(state.error)
        assertEquals(false, state.saving)
        assertEquals("Casual", state.name)
    }

    @Test
    fun `a name that is too long is a field error`() = runTest {
        val viewModel = viewModel(FakeMemberRepository(renameTagError = DomainError.TagNameTooLong()))
        viewModel.onRename(tagId = "t1", currentName = "Casual", memberCount = 2)
        viewModel.onNameChange("x".repeat(31))
        viewModel.onSubmit()
        advanceUntilIdle()

        assertIs<DomainError.TagNameTooLong>(viewModel.open().nameError)
    }

    @Test
    fun `a network failure is a form error and can be retried`() = runTest {
        val members = FakeMemberRepository(createTagError = DomainError.Network())
        val viewModel = viewModel(members)
        viewModel.onNewTag()
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.open()
        assertIs<DomainError.Network>(state.error)
        assertNull(state.nameError)
        assertTrue(state.canSubmit)

        members.createTagError = null
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(TagNameUiState.Hidden, viewModel.state.value)
    }

    @Test
    fun `a terminal failure leaves nothing to submit`() = runTest {
        val viewModel = viewModel(FakeMemberRepository(renameTagError = DomainError.TagListChanged()))
        viewModel.onRename(tagId = "t1", currentName = "Casual", memberCount = 2)
        viewModel.onNameChange("Social")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(false, viewModel.open().canSubmit)
    }

    @Test
    fun `typing clears the errors`() = runTest {
        val viewModel = viewModel(FakeMemberRepository(createTagError = DomainError.TagNameTaken("Casual")))
        viewModel.onNewTag()
        viewModel.onNameChange("Casual")
        viewModel.onSubmit()
        advanceUntilIdle()

        viewModel.onNameChange("Casuals")

        assertNull(viewModel.open().nameError)
    }

    @Test
    fun `dismissing closes the dialog`() {
        val viewModel = viewModel()
        viewModel.onNewTag()

        viewModel.onDismiss()

        assertEquals(TagNameUiState.Hidden, viewModel.state.value)
    }

    @Test
    fun `dismissing while saving does nothing`() = runTest {
        val viewModel = viewModel()
        viewModel.onNewTag()
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()

        viewModel.onDismiss()

        assertTrue(viewModel.open().saving)
        advanceUntilIdle()
    }

    @Test
    fun `submitting twice while saving creates one tag`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members)
        viewModel.onNewTag()
        viewModel.onNameChange("Regular")
        viewModel.onSubmit()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, members.tags.size)
    }
}
