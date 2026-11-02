package app.muster.ui.screens.group.members.components

import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.MembersTabUiState

internal const val DragSettleMillis = 400L

internal sealed interface ListSection {
    data class Tag(val id: String) : ListSection

    data object Untagged : ListSection

    data object Invited : ListSection
}

internal sealed interface MembersEntry {
    val key: String
}

internal data class HeaderEntry(val section: ListSection) : MembersEntry {
    override val key: String
        get() = when (section) {
            is ListSection.Tag -> "tag_header_${section.id}"
            ListSection.Untagged -> "untagged_header"
            ListSection.Invited -> "invited_header"
        }
}

internal data class RowEntry(val row: MemberRow) : MembersEntry {
    override val key: String get() = row.id
}

// Drop placeholder of a section without rows. It is a swap target for the dragged row but never draggable.
internal data class HintEntry(val section: ListSection) : MembersEntry {
    override val key: String
        get() = when (section) {
            is ListSection.Tag -> "tag_empty_${section.id}"
            else -> "untagged_empty"
        }
}

internal fun buildEntries(
    state: MembersTabUiState.Success,
    dragging: Boolean = false
): List<MembersEntry> {
    val hasTags = state.tags.isNotEmpty()
    return buildList<MembersEntry> {
        state.tags.forEach { section ->
            add(HeaderEntry(ListSection.Tag(section.id)))
            section.rows.forEach { add(RowEntry(it)) }
        }
        if (state.members.isNotEmpty() || (dragging && hasTags)) {
            if (hasTags) add(HeaderEntry(ListSection.Untagged))
            state.members.forEach { add(RowEntry(it)) }
        }
        if (state.pendings.isNotEmpty()) {
            if (hasTags) add(HeaderEntry(ListSection.Invited))
            state.pendings.forEach { add(RowEntry(it)) }
        }
    }.withHints()
}

internal fun List<MembersEntry>.withHints(): List<MembersEntry> {
    val rest = filterNot { it is HintEntry }
    return buildList {
        rest.forEachIndexed { index, entry ->
            add(entry)
            if (entry is HeaderEntry && entry.section != ListSection.Invited && rest.getOrNull(index + 1) !is RowEntry) {
                add(HintEntry(entry.section))
            }
        }
    }
}

internal fun List<MembersEntry>.sectionOf(rowId: String): ListSection {
    val index = indexOfFirst { it.key == rowId }
    if (index < 0) return ListSection.Untagged
    return take(index).filterIsInstance<HeaderEntry>().lastOrNull()?.section ?: ListSection.Untagged
}

internal fun List<MembersEntry>.rowsAfter(headerIndex: Int): List<MemberRow> = drop(headerIndex + 1).takeWhile { it is RowEntry }.map { (it as RowEntry).row }

// Local copy of the list order while a drag is active, so the library can displace rows live.
internal data class DragLayout(
    val entries: List<MembersEntry>,
    val draggedId: String,
    val origin: ListSection,
    val settling: Boolean = false
) {
    fun moved(
        fromKey: Any,
        toKey: Any
    ): DragLayout {
        val from = entries.indexOfFirst { it.key == fromKey }
        val to = entries.indexOfFirst { it.key == toKey }
        if (from < 0 || to < 0 || from == to) return this
        val reordered = entries.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return copy(entries = reordered.withHints())
    }

    fun destination(): ListSection = entries.sectionOf(draggedId)
}
