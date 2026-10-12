package app.muster.ui.screens.group.members.components

import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.MemberStatus
import app.muster.ui.screens.group.members.MembersTabUiState
import app.muster.ui.screens.group.members.Reorder
import app.muster.ui.screens.group.members.TagSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MembersListModelTest {

    private fun row(
        id: String,
        status: MemberStatus = MemberStatus.Member
    ) = MemberRow(id = id, displayName = "Name $id", status = status)

    private fun tag(
        id: String,
        vararg rows: String
    ) = TagSection(
        id = id,
        name = "Tag $id",
        rows = rows.map { row(it) },
        reorder = Reorder(canMoveUp = false, canMoveDown = false)
    )

    private fun state(
        tags: List<TagSection> = emptyList(),
        members: List<String> = emptyList(),
        pendings: List<String> = emptyList()
    ) = MembersTabUiState.Success(
        tags = tags,
        members = members.map { row(it) },
        pendings = pendings.map { row(it, MemberStatus.Pending) }
    )

    private fun keys(entries: List<MembersEntry>) = entries.map { it.key }

    private fun layout(
        entries: List<MembersEntry>,
        draggedId: String
    ) = DragLayout(entries = entries, draggedId = draggedId, origin = entries.sectionOf(draggedId))

    private val t1 = ListSection.Tag("t1")
    private val t2 = ListSection.Tag("t2")

    // t1 [a, b], t2 [], untagged [u]
    private fun draggingWithEmptyTag() = buildEntries(state(tags = listOf(tag("t1", "a", "b"), tag("t2")), members = listOf("u")), dragging = true)

    // t1 [a], t2 [c], untagged [u]
    private fun draggingSingles() = buildEntries(state(tags = listOf(tag("t1", "a"), tag("t2", "c")), members = listOf("u")), dragging = true)

    @Test
    fun `entry keys per kind`() {
        assertEquals("tag_header_t1", HeaderEntry(t1).key)
        assertEquals("untagged_header", HeaderEntry(ListSection.Untagged).key)
        assertEquals("invited_header", HeaderEntry(ListSection.Invited).key)
        assertEquals("x", RowEntry(row("x")).key)
        assertEquals("tag_empty_t1", HintEntry(t1).key)
        assertEquals("untagged_empty", HintEntry(ListSection.Untagged).key)
    }

    @Test
    fun `drag settle delay is positive`() {
        assertTrue(DRAG_SETTLE_MILLIS > 0)
    }

    @Test
    fun `without tags there are no headers`() {
        val entries = buildEntries(state(members = listOf("m1", "m2"), pendings = listOf("p1")))

        assertEquals(listOf("m1", "m2", "p1"), keys(entries))
        assertTrue(entries.all { it is RowEntry })
    }

    @Test
    fun `without tags and without people there are no entries`() {
        assertTrue(buildEntries(state()).isEmpty())
    }

    @Test
    fun `without tags dragging adds nothing`() {
        assertTrue(buildEntries(state(), dragging = true).isEmpty())
        assertEquals(listOf("m1"), keys(buildEntries(state(members = listOf("m1")), dragging = true)))
    }

    @Test
    fun `tags come in order each with header then rows`() {
        val entries = buildEntries(
            state(
                tags = listOf(tag("t1", "a", "b"), tag("t2", "c")),
                members = listOf("m"),
                pendings = listOf("p")
            )
        )

        assertEquals(
            listOf("tag_header_t1", "a", "b", "tag_header_t2", "c", "untagged_header", "m", "invited_header", "p"),
            keys(entries)
        )
        assertEquals(HeaderEntry(t1), entries[0])
        assertEquals(RowEntry(row("a")), entries[1])
    }

    @Test
    fun `row entries keep the pending rows`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a")), pendings = listOf("p")))

        assertEquals(MemberStatus.Pending, (entries.last() as RowEntry).row.status)
    }

    @Test
    fun `untagged header is omitted when there are no untagged members`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a")), pendings = listOf("p")))

        assertEquals(listOf("tag_header_t1", "a", "invited_header", "p"), keys(entries))
    }

    @Test
    fun `invited header is omitted when there are no pendings`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a")), members = listOf("m")))

        assertEquals(listOf("tag_header_t1", "a", "untagged_header", "m"), keys(entries))
    }

    @Test
    fun `empty tag gets a hint`() {
        val entries = buildEntries(state(tags = listOf(tag("t1"), tag("t2", "c"))))

        assertEquals(listOf("tag_header_t1", "tag_empty_t1", "tag_header_t2", "c"), keys(entries))
        assertEquals(HintEntry(t1), entries[1])
    }

    @Test
    fun `last empty tag gets a hint at the end`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a"), tag("t2"))))

        assertEquals(listOf("tag_header_t1", "a", "tag_header_t2", "tag_empty_t2"), keys(entries))
    }

    @Test
    fun `untagged hint appears only while dragging with tags and no untagged members`() {
        val s = state(tags = listOf(tag("t1", "a")))

        assertEquals(listOf("tag_header_t1", "a"), keys(buildEntries(s)))
        assertEquals(
            listOf("tag_header_t1", "a", "untagged_header", "untagged_empty"),
            keys(buildEntries(s, dragging = true))
        )
    }

    @Test
    fun `untagged hint sits before the invited header`() {
        val s = state(tags = listOf(tag("t1", "a")), pendings = listOf("p"))

        assertEquals(
            listOf("tag_header_t1", "a", "untagged_header", "untagged_empty", "invited_header", "p"),
            keys(buildEntries(s, dragging = true))
        )
    }

    @Test
    fun `dragging with untagged members adds no hint`() {
        val s = state(tags = listOf(tag("t1", "a")), members = listOf("m"))

        assertEquals(buildEntries(s), buildEntries(s, dragging = true))
        assertTrue(buildEntries(s, dragging = true).none { it is HintEntry })
    }

    @Test
    fun `invited section never gets a hint`() {
        val alone = listOf<MembersEntry>(HeaderEntry(ListSection.Invited)).withHints()
        val beforeAnother = listOf<MembersEntry>(HeaderEntry(ListSection.Invited), HeaderEntry(t1)).withHints()

        assertEquals(listOf("invited_header"), keys(alone))
        assertEquals(listOf("invited_header", "tag_header_t1", "tag_empty_t1"), keys(beforeAnother))
    }

    @Test
    fun `withHints adds a hint after a header with nothing after it`() {
        val entries = listOf<MembersEntry>(HeaderEntry(t1), HeaderEntry(ListSection.Untagged)).withHints()

        assertEquals(listOf("tag_header_t1", "tag_empty_t1", "untagged_header", "untagged_empty"), keys(entries))
    }

    @Test
    fun `withHints strips stale hints and never keeps both a row and a hint`() {
        val stale = listOf<MembersEntry>(HeaderEntry(t1), HintEntry(t1), RowEntry(row("a")), HeaderEntry(t2), HintEntry(t2))

        val entries = stale.withHints()

        assertEquals(listOf("tag_header_t1", "a", "tag_header_t2", "tag_empty_t2"), keys(entries))
        assertEquals(1, entries.count { it is HintEntry })
    }

    @Test
    fun `withHints is idempotent`() {
        val once = draggingWithEmptyTag()

        assertEquals(once, once.withHints())
    }

    @Test
    fun `withHints drops a hint misplaced among rows`() {
        val entries = listOf<MembersEntry>(HeaderEntry(t1), RowEntry(row("a")), HintEntry(t1), RowEntry(row("b"))).withHints()

        assertEquals(listOf("tag_header_t1", "a", "b"), keys(entries))
    }

    @Test
    fun `sectionOf finds the section of a tagged row`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a", "b"), tag("t2", "c")), members = listOf("m"), pendings = listOf("p")))

        assertEquals(t1, entries.sectionOf("a"))
        assertEquals(t1, entries.sectionOf("b"))
        assertEquals(t2, entries.sectionOf("c"))
    }

    @Test
    fun `sectionOf finds untagged and invited rows`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a")), members = listOf("m"), pendings = listOf("p")))

        assertEquals(ListSection.Untagged, entries.sectionOf("m"))
        assertEquals(ListSection.Invited, entries.sectionOf("p"))
    }

    @Test
    fun `sectionOf an unknown id is untagged`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a"))))

        assertEquals(ListSection.Untagged, entries.sectionOf("nope"))
        assertEquals(ListSection.Untagged, emptyList<MembersEntry>().sectionOf("a"))
    }

    @Test
    fun `sectionOf a row before any header is untagged`() {
        val entries = buildEntries(state(members = listOf("m"), pendings = listOf("p")))

        assertEquals(ListSection.Untagged, entries.sectionOf("m"))
        assertEquals(ListSection.Untagged, entries.sectionOf("p"))
    }

    @Test
    fun `sectionOf resolves a hint key to its own section`() {
        val entries = draggingWithEmptyTag()

        assertEquals(t2, entries.sectionOf("tag_empty_t2"))
    }

    @Test
    fun `rowsAfter returns the rows up to the next header`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a", "b"), tag("t2", "c")), members = listOf("m")))

        assertEquals(listOf("a", "b"), entries.rowsAfter(entries.indexOfFirst { it.key == "tag_header_t1" }).map { it.id })
        assertEquals(listOf("c"), entries.rowsAfter(entries.indexOfFirst { it.key == "tag_header_t2" }).map { it.id })
        assertEquals(listOf("m"), entries.rowsAfter(entries.indexOfFirst { it.key == "untagged_header" }).map { it.id })
    }

    @Test
    fun `rowsAfter a header with a hint is empty`() {
        val entries = buildEntries(state(tags = listOf(tag("t1"), tag("t2", "c"))))

        assertTrue(entries.rowsAfter(0).isEmpty())
    }

    @Test
    fun `rowsAfter the last index is empty`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a"))))

        assertTrue(entries.rowsAfter(entries.lastIndex).isEmpty())
    }

    @Test
    fun `moved down within a section`() {
        val moved = layout(draggingWithEmptyTag(), "a").moved("a", "b")

        assertEquals(
            listOf("tag_header_t1", "b", "a", "tag_header_t2", "tag_empty_t2", "untagged_header", "u"),
            keys(moved.entries)
        )
        assertEquals(t1, moved.destination())
    }

    @Test
    fun `moved up within a section`() {
        val moved = layout(draggingWithEmptyTag(), "b").moved("b", "a")

        assertEquals(
            listOf("tag_header_t1", "b", "a", "tag_header_t2", "tag_empty_t2", "untagged_header", "u"),
            keys(moved.entries)
        )
        assertEquals(t1, moved.destination())
    }

    @Test
    fun `moved down onto a header lands in the section below`() {
        val moved = layout(draggingWithEmptyTag(), "b").moved("b", "tag_header_t2")

        assertEquals(
            listOf("tag_header_t1", "a", "tag_header_t2", "b", "untagged_header", "u"),
            keys(moved.entries)
        )
        assertEquals(t2, moved.destination())
    }

    @Test
    fun `moved up onto a header lands in the section above`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a", "b")), members = listOf("u")))

        val moved = layout(entries, "u").moved("u", "untagged_header")

        assertEquals(listOf("tag_header_t1", "a", "b", "u", "untagged_header", "untagged_empty"), keys(moved.entries))
        assertEquals(t1, moved.destination())
    }

    @Test
    fun `moved up onto a row of another section`() {
        val entries = buildEntries(state(tags = listOf(tag("t1", "a", "b")), members = listOf("u")))

        val moved = layout(entries, "u").moved("u", "b")

        assertEquals(listOf("tag_header_t1", "a", "u", "b", "untagged_header", "untagged_empty"), keys(moved.entries))
        assertEquals(t1, moved.destination())
    }

    @Test
    fun `moved onto a hint fills the empty section and the hint disappears`() {
        val moved = layout(draggingWithEmptyTag(), "b").moved("b", "tag_empty_t2")

        assertEquals(
            listOf("tag_header_t1", "a", "tag_header_t2", "b", "untagged_header", "u"),
            keys(moved.entries)
        )
        assertTrue(moved.entries.none { it is HintEntry })
        assertEquals(t2, moved.destination())
    }

    @Test
    fun `moved up onto a hint leaves a hint in the emptied section`() {
        val moved = layout(draggingWithEmptyTag(), "u").moved("u", "tag_empty_t2")

        assertEquals(
            listOf("tag_header_t1", "a", "b", "tag_header_t2", "u", "untagged_header", "untagged_empty"),
            keys(moved.entries)
        )
        assertEquals(t2, moved.destination())
    }

    @Test
    fun `moving the last row out of a tag adds a hint there`() {
        val moved = layout(draggingSingles(), "a").moved("a", "c")

        assertEquals(
            listOf("tag_header_t1", "tag_empty_t1", "tag_header_t2", "c", "a", "untagged_header", "u"),
            keys(moved.entries)
        )
        assertEquals(t2, moved.destination())
    }

    @Test
    fun `unknown keys leave the layout unchanged`() {
        val l = layout(draggingSingles(), "a")

        assertSame(l, l.moved("nope", "c"))
        assertSame(l, l.moved("a", "nope"))
        assertSame(l, l.moved("nope", "nope2"))
    }

    @Test
    fun `moving onto itself leaves the layout unchanged`() {
        val l = layout(draggingSingles(), "a")

        assertSame(l, l.moved("a", "a"))
    }

    @Test
    fun `moved keeps dragged id origin and settling`() {
        val l = layout(draggingSingles(), "a").copy(settling = true)

        val moved = l.moved("a", "c")

        assertEquals("a", moved.draggedId)
        assertEquals(t1, moved.origin)
        assertTrue(moved.settling)
    }

    @Test
    fun `destination equals origin before any move`() {
        val l = layout(draggingSingles(), "a")

        assertEquals(l.origin, l.destination())
    }

    @Test
    fun `dropping back into the same section reverts to origin`() {
        val l = layout(draggingWithEmptyTag(), "a").moved("a", "b")

        assertEquals(l.origin, l.destination())
    }

    @Test
    fun `dropping into another section differs from origin`() {
        val l = layout(draggingWithEmptyTag(), "a").moved("a", "tag_empty_t2")

        assertEquals(t1, l.origin)
        assertNotEquals(l.origin, l.destination())
    }

    @Test
    fun `full drag simulation across sections and back`() {
        val start = layout(draggingSingles(), "a")
        assertEquals(t1, start.origin)

        val inT2 = start.moved("a", "c")
        assertEquals(t2, inT2.destination())
        assertEquals(
            listOf("tag_header_t1", "tag_empty_t1", "tag_header_t2", "c", "a", "untagged_header", "u"),
            keys(inT2.entries)
        )

        val backOnHint = inT2.moved("a", "tag_empty_t1")
        assertEquals(t1, backOnHint.destination())
        assertEquals(backOnHint.origin, backOnHint.destination())
        assertEquals(keys(start.entries), keys(backOnHint.entries))

        val inUntagged = backOnHint.moved("a", "u")
        assertEquals(ListSection.Untagged, inUntagged.destination())
        assertNotEquals(inUntagged.origin, inUntagged.destination())
        assertEquals(
            listOf("tag_header_t1", "tag_empty_t1", "tag_header_t2", "c", "untagged_header", "u", "a"),
            keys(inUntagged.entries)
        )
        assertFalse(inUntagged.entries.any { it is HintEntry && it.section == ListSection.Untagged })
    }
}
