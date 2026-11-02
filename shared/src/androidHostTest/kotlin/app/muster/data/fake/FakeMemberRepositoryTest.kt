package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.MemberTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class FakeMemberRepositoryTest {

    private val alex = Member(profileId = "a", name = "Alex", role = GroupRole.Admin, tagId = null)
    private val bea = Member(profileId = "b", name = "Bea", role = GroupRole.Member, tagId = "t1")
    private val regular = MemberTag(id = "t1", name = "Regular")
    private val casual = MemberTag(id = "t2", name = "Casual")

    private fun repository(dataChanges: DataChanges? = null) = FakeMemberRepository(
        members = listOf(alex, bea),
        memberTags = listOf(regular, casual),
        dataChanges = dataChanges,
        latency = 0
    )

    @Test
    fun `listMembers includes the tags`() = runTest {
        val listing = repository().listMembers("g")

        assertEquals(listOf(regular, casual), listing.memberTags)
    }

    @Test
    fun `createTag adds a tag with a generated id`() = runTest {
        val repository = repository()

        repository.createTag("g", "Late", memberId = null)

        assertEquals(listOf("t1", "t2", "tag-1"), repository.tags.map { it.id })
        assertEquals("Late", repository.tags.last().name)
        assertEquals(listOf(null, "t1"), repository.listMembers("g").members.map { it.tagId })
    }

    @Test
    fun `createTag with a member assigns the new tag to that member`() = runTest {
        val repository = repository()

        repository.createTag("g", "Late", memberId = "a")

        val members = repository.listMembers("g").members
        assertEquals("tag-1", members.first { it.profileId == "a" }.tagId)
        assertEquals("t1", members.first { it.profileId == "b" }.tagId)
    }

    @Test
    fun `renameTag changes only that tag's name`() = runTest {
        val repository = repository()

        repository.renameTag("g", "t2", "Drop-in")

        assertEquals(listOf("Regular", "Drop-in"), repository.tags.map { it.name })
    }

    @Test
    fun `deleteTag removes the tag`() = runTest {
        val repository = repository()

        repository.deleteTag("g", "t1")

        assertEquals(listOf("t2"), repository.tags.map { it.id })
    }

    @Test
    fun `deleteTag clears the tag on its members and keeps them listed`() = runTest {
        val repository = repository()

        repository.deleteTag("g", "t1")

        val members = repository.listMembers("g").members
        assertEquals(listOf("a", "b"), members.map { it.profileId })
        assertEquals(listOf(null, null), members.map { it.tagId })
    }

    @Test
    fun `deleteTag leaves members of other tags untouched`() = runTest {
        val repository = repository()
        repository.setMemberTag("g", "a", "t2")

        repository.deleteTag("g", "t1")

        val members = repository.listMembers("g").members
        assertEquals("t2", members.first { it.profileId == "a" }.tagId)
        assertNull(members.first { it.profileId == "b" }.tagId)
    }

    @Test
    fun `tag ids stay unique after a delete followed by a create`() = runTest {
        val repository = repository()

        repository.createTag("g", "Late", memberId = null)
        repository.createTag("g", "Early", memberId = null)
        repository.deleteTag("g", "tag-1")
        repository.createTag("g", "Newer", memberId = null)

        val ids = repository.tags.map { it.id }
        assertEquals(ids.distinct(), ids)
        assertEquals(listOf("t1", "t2", "tag-2", "tag-3"), ids)
    }

    @Test
    fun `setMemberTag assigns and clears a member's tag`() = runTest {
        val repository = repository()

        repository.setMemberTag("g", "a", "t2")
        assertEquals("t2", repository.listMembers("g").members.first { it.profileId == "a" }.tagId)

        repository.setMemberTag("g", "a", null)
        assertNull(repository.listMembers("g").members.first { it.profileId == "a" }.tagId)
    }

    @Test
    fun `setTagOrder reorders the tags`() = runTest {
        val repository = repository()

        repository.setTagOrder("g", listOf("t2", "t1"))

        assertEquals(listOf("t2", "t1"), repository.tags.map { it.id })
    }

    @Test
    fun `setTagOrder puts tags missing from the list last`() = runTest {
        val repository = repository()

        repository.setTagOrder("g", listOf("t2"))

        assertEquals(listOf("t2", "t1"), repository.tags.map { it.id })
    }

    @Test
    fun `tag writes announce a members change for the group`() = runTest {
        val dataChanges = DataChanges()
        val seen = mutableListOf<DataChange>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dataChanges.changes.collect { seen += it }
        }
        val repository = repository(dataChanges)

        repository.createTag("g", "Late", memberId = null)
        repository.renameTag("g", "t1", "Core")
        repository.deleteTag("g", "t2")
        repository.setMemberTag("g", "a", "t1")
        repository.setTagOrder("g", listOf("t1"))

        assertEquals(List(5) { DataChange.Members("g") }, seen)
    }

    @Test
    fun `tag writes work without DataChanges`() = runTest {
        val repository = repository(dataChanges = null)

        repository.renameTag("g", "t1", "Core")

        assertEquals("Core", repository.tags.first().name)
    }

    @Test
    fun `a configured error is thrown and nothing changes`() = runTest {
        val error = DomainError.TagListChanged()
        val dataChanges = DataChanges()
        val seen = mutableListOf<DataChange>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dataChanges.changes.collect { seen += it }
        }
        val repository = repository(dataChanges)
        repository.createTagError = error
        repository.renameTagError = error
        repository.deleteTagError = error
        repository.setMemberTagError = error
        repository.setTagOrderError = error

        assertFailsWith<DomainError.TagListChanged> { repository.createTag("g", "Late", "a") }
        assertFailsWith<DomainError.TagListChanged> { repository.renameTag("g", "t1", "Core") }
        assertFailsWith<DomainError.TagListChanged> { repository.deleteTag("g", "t1") }
        assertFailsWith<DomainError.TagListChanged> { repository.setMemberTag("g", "a", "t1") }
        assertFailsWith<DomainError.TagListChanged> { repository.setTagOrder("g", listOf("t2", "t1")) }

        assertEquals(listOf(regular, casual), repository.tags)
        assertEquals(listOf(alex, bea), repository.listMembers("g").members)
        assertEquals(emptyList(), seen)
    }
}
