package app.muster.domain.usecase

import app.muster.data.fake.FakeMemberRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.MemberTag
import app.muster.domain.repository.MemberRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class TagUseCasesTest {

    private val groupId = "group-1"

    private class RecordingMembers(
        delegate: MemberRepository = FakeMemberRepository()
    ) : MemberRepository by delegate {
        val sentOrders = mutableListOf<Pair<String, List<String>>>()

        override suspend fun setTagOrder(
            groupId: String,
            tagIds: List<String>
        ) {
            sentOrders += groupId to tagIds
        }
    }

    private fun tag(id: String) = MemberTag(id = id, name = "Tag $id")

    private fun repo(
        vararg ids: String,
        setTagOrderError: DomainError? = null
    ) = FakeMemberRepository(
        memberTags = ids.map { tag(it) },
        setTagOrderError = setTagOrderError
    )

    private fun FakeMemberRepository.tagIds() = tags.map { it.id }

    @Test
    fun `move up swaps a middle tag with the one above`() = runTest {
        val members = repo("a", "b", "c")
        MoveTagUpUseCase(members)(groupId, listOf("a", "b", "c"), "b")
        assertEquals(listOf("b", "a", "c"), members.tagIds())
    }

    @Test
    fun `move up the last tag swaps it with the one above`() = runTest {
        val members = repo("a", "b", "c")
        MoveTagUpUseCase(members)(groupId, listOf("a", "b", "c"), "c")
        assertEquals(listOf("a", "c", "b"), members.tagIds())
    }

    @Test
    fun `move up sends the group and exact reordered ids to the repository`() = runTest {
        val members = RecordingMembers()
        MoveTagUpUseCase(members)(groupId, listOf("a", "b", "c", "d"), "c")
        assertEquals(listOf(groupId to listOf("a", "c", "b", "d")), members.sentOrders)
    }

    @Test
    fun `move up the first tag does nothing and does not call the repository`() = runTest {
        val members = RecordingMembers()
        MoveTagUpUseCase(members)(groupId, listOf("a", "b"), "a")
        assertEquals(emptyList(), members.sentOrders)
    }

    @Test
    fun `move up the first tag ignores a repository error`() = runTest {
        val members = repo("a", "b", setTagOrderError = DomainError.TagListChanged())
        MoveTagUpUseCase(members)(groupId, listOf("a", "b"), "a")
        assertEquals(listOf("a", "b"), members.tagIds())
    }

    @Test
    fun `move up an unknown tag does nothing`() = runTest {
        val members = RecordingMembers()
        MoveTagUpUseCase(members)(groupId, listOf("a", "b"), "zzz")
        assertEquals(emptyList(), members.sentOrders)
    }

    @Test
    fun `move up an unknown tag ignores a repository error`() = runTest {
        val members = repo("a", "b", setTagOrderError = DomainError.TagListChanged())
        MoveTagUpUseCase(members)(groupId, listOf("a", "b"), "zzz")
        assertEquals(listOf("a", "b"), members.tagIds())
    }

    @Test
    fun `move up with an empty list does nothing`() = runTest {
        val members = RecordingMembers()
        MoveTagUpUseCase(members)(groupId, emptyList(), "a")
        assertEquals(emptyList(), members.sentOrders)
    }

    @Test
    fun `move up a single tag does nothing`() = runTest {
        val members = RecordingMembers()
        MoveTagUpUseCase(members)(groupId, listOf("a"), "a")
        assertEquals(emptyList(), members.sentOrders)
    }

    @Test
    fun `move up with two tags swaps them`() = runTest {
        val members = repo("a", "b")
        MoveTagUpUseCase(members)(groupId, listOf("a", "b"), "b")
        assertEquals(listOf("b", "a"), members.tagIds())
    }

    @Test
    fun `move up propagates repository errors`() = runTest {
        val members = repo("a", "b", setTagOrderError = DomainError.TagListChanged())
        assertFailsWith<DomainError.TagListChanged> {
            MoveTagUpUseCase(members)(groupId, listOf("a", "b"), "b")
        }
        assertEquals(listOf("a", "b"), members.tagIds())
    }

    @Test
    fun `move down swaps a middle tag with the one below`() = runTest {
        val members = repo("a", "b", "c")
        MoveTagDownUseCase(members)(groupId, listOf("a", "b", "c"), "b")
        assertEquals(listOf("a", "c", "b"), members.tagIds())
    }

    @Test
    fun `move down the first tag swaps it with the one below`() = runTest {
        val members = repo("a", "b", "c")
        MoveTagDownUseCase(members)(groupId, listOf("a", "b", "c"), "a")
        assertEquals(listOf("b", "a", "c"), members.tagIds())
    }

    @Test
    fun `move down sends the group and exact reordered ids to the repository`() = runTest {
        val members = RecordingMembers()
        MoveTagDownUseCase(members)(groupId, listOf("a", "b", "c", "d"), "b")
        assertEquals(listOf(groupId to listOf("a", "c", "b", "d")), members.sentOrders)
    }

    @Test
    fun `move down the last tag does nothing and does not call the repository`() = runTest {
        val members = RecordingMembers()
        MoveTagDownUseCase(members)(groupId, listOf("a", "b"), "b")
        assertEquals(emptyList(), members.sentOrders)
    }

    @Test
    fun `move down the last tag ignores a repository error`() = runTest {
        val members = repo("a", "b", setTagOrderError = DomainError.TagListChanged())
        MoveTagDownUseCase(members)(groupId, listOf("a", "b"), "b")
        assertEquals(listOf("a", "b"), members.tagIds())
    }

    @Test
    fun `move down an unknown tag does nothing`() = runTest {
        val members = RecordingMembers()
        MoveTagDownUseCase(members)(groupId, listOf("a", "b"), "zzz")
        assertEquals(emptyList(), members.sentOrders)
    }

    @Test
    fun `move down an unknown tag ignores a repository error`() = runTest {
        val members = repo("a", "b", setTagOrderError = DomainError.TagListChanged())
        MoveTagDownUseCase(members)(groupId, listOf("a", "b"), "zzz")
        assertEquals(listOf("a", "b"), members.tagIds())
    }

    @Test
    fun `move down with an empty list does nothing`() = runTest {
        val members = RecordingMembers()
        MoveTagDownUseCase(members)(groupId, emptyList(), "a")
        assertEquals(emptyList(), members.sentOrders)
    }

    @Test
    fun `move down a single tag does nothing`() = runTest {
        val members = RecordingMembers()
        MoveTagDownUseCase(members)(groupId, listOf("a"), "a")
        assertEquals(emptyList(), members.sentOrders)
    }

    @Test
    fun `move down with two tags swaps them`() = runTest {
        val members = repo("a", "b")
        MoveTagDownUseCase(members)(groupId, listOf("a", "b"), "a")
        assertEquals(listOf("b", "a"), members.tagIds())
    }

    @Test
    fun `move down propagates repository errors`() = runTest {
        val members = repo("a", "b", setTagOrderError = DomainError.TagListChanged())
        assertFailsWith<DomainError.TagListChanged> {
            MoveTagDownUseCase(members)(groupId, listOf("a", "b"), "a")
        }
        assertEquals(listOf("a", "b"), members.tagIds())
    }

    @Test
    fun `creating a tag reaches the repository and assigns the member`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(Member("p1", "Pat", GroupRole.Member, tagId = null))
        )
        CreateTagUseCase(members)(groupId, "Goalkeeper", "p1")
        assertEquals(listOf("Goalkeeper"), members.tags.map { it.name })
        assertEquals(members.tags.single().id, members.members.single().tagId)
    }

    @Test
    fun `creating a tag without a member leaves members untouched`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(Member("p1", "Pat", GroupRole.Member, tagId = null))
        )
        CreateTagUseCase(members)(groupId, "Goalkeeper", null)
        assertEquals(1, members.tags.size)
        assertNull(members.members.single().tagId)
    }

    @Test
    fun `creating a tag propagates repository errors`() = runTest {
        val members = FakeMemberRepository(createTagError = DomainError.TagNameTaken("Goalkeeper"))
        assertFailsWith<DomainError.TagNameTaken> {
            CreateTagUseCase(members)(groupId, "Goalkeeper", null)
        }
        assertEquals(emptyList(), members.tags)
    }

    @Test
    fun `renaming a tag reaches the repository`() = runTest {
        val members = repo("a", "b")
        RenameTagUseCase(members)(groupId, "b", "Striker")
        assertEquals(listOf("Tag a", "Striker"), members.tags.map { it.name })
    }

    @Test
    fun `renaming a tag propagates repository errors`() = runTest {
        val members = FakeMemberRepository(
            memberTags = listOf(tag("a")),
            renameTagError = DomainError.TagNameTooLong()
        )
        assertFailsWith<DomainError.TagNameTooLong> {
            RenameTagUseCase(members)(groupId, "a", "Striker")
        }
        assertEquals("Tag a", members.tags.single().name)
    }

    @Test
    fun `deleting a tag reaches the repository`() = runTest {
        val members = repo("a", "b")
        DeleteTagUseCase(members)(groupId, "a")
        assertEquals(listOf("b"), members.tagIds())
    }

    @Test
    fun `deleting a tag propagates repository errors`() = runTest {
        val members = FakeMemberRepository(
            memberTags = listOf(tag("a")),
            deleteTagError = DomainError.TagListChanged()
        )
        assertFailsWith<DomainError.TagListChanged> {
            DeleteTagUseCase(members)(groupId, "a")
        }
        assertEquals(listOf("a"), members.tagIds())
    }

    @Test
    fun `setting a member tag reaches the repository`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(
                Member("p1", "Pat", GroupRole.Member, tagId = null),
                Member("p2", "Sam", GroupRole.Member, tagId = null)
            ),
            memberTags = listOf(tag("a"))
        )
        SetMemberTagUseCase(members)(groupId, "p2", "a")
        assertNull(members.members.first { it.profileId == "p1" }.tagId)
        assertEquals("a", members.members.first { it.profileId == "p2" }.tagId)
    }

    @Test
    fun `clearing a member tag passes null through`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(Member("p1", "Pat", GroupRole.Member, tagId = "a")),
            memberTags = listOf(tag("a"))
        )
        SetMemberTagUseCase(members)(groupId, "p1", null)
        assertNull(members.members.single().tagId)
    }

    @Test
    fun `setting a member tag propagates repository errors`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(Member("p1", "Pat", GroupRole.Member, tagId = null)),
            setMemberTagError = DomainError.TagListChanged()
        )
        assertFailsWith<DomainError.TagListChanged> {
            SetMemberTagUseCase(members)(groupId, "p1", "a")
        }
        assertNull(members.members.single().tagId)
    }

    @Test
    fun `setting the tag order reaches the repository`() = runTest {
        val members = RecordingMembers()
        SetTagOrderUseCase(members)(groupId, listOf("c", "a", "b"))
        assertEquals(listOf(groupId to listOf("c", "a", "b")), members.sentOrders)
    }

    @Test
    fun `setting the tag order reorders the tags`() = runTest {
        val members = repo("a", "b", "c")
        SetTagOrderUseCase(members)(groupId, listOf("c", "a", "b"))
        assertEquals(listOf("c", "a", "b"), members.tagIds())
    }

    @Test
    fun `setting the tag order propagates repository errors`() = runTest {
        val members = repo("a", "b", setTagOrderError = DomainError.TagListChanged())
        assertFailsWith<DomainError.TagListChanged> {
            SetTagOrderUseCase(members)(groupId, listOf("b", "a"))
        }
        assertEquals(listOf("a", "b"), members.tagIds())
    }
}
