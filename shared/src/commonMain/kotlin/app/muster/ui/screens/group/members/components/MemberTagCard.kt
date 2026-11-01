package app.muster.ui.screens.group.members.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.MemberStatus
import app.muster.ui.screens.group.members.Reorder
import app.muster.ui.screens.group.members.TagSection
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_move_down
import muster.shared.generated.resources.action_move_up
import muster.shared.generated.resources.content_description_tag_options
import muster.shared.generated.resources.tag_action_delete
import muster.shared.generated.resources.tag_action_rename
import muster.shared.generated.resources.tag_empty_hint
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class MemberTagCardActions(
    val onRename: () -> Unit = {},
    val onMoveUp: () -> Unit = {},
    val onMoveDown: () -> Unit = {},
    val onDelete: () -> Unit = {}
)

// End matches the header's, so row menus line up with the card menu.
val TagCardRowPadding = PaddingValues(start = 16.dp, end = 4.dp)

enum class TagCardPosition(val roundsTop: Boolean, val roundsBottom: Boolean) {
    First(roundsTop = true, roundsBottom = false),
    Middle(roundsTop = false, roundsBottom = false),
    Last(roundsTop = false, roundsBottom = true),
    Only(roundsTop = true, roundsBottom = true)
}

private val TagCardCorner = 14.dp

// Pieces of one card are separate items, so each draws only its own slice of the outline: the
// full rounded rect is extended past the open edges and clipped, leaving no seam between items.
fun Modifier.tagCardFrame(position: TagCardPosition): Modifier {
    val shape = RoundedCornerShape(
        topStart = if (position.roundsTop) TagCardCorner else 0.dp,
        topEnd = if (position.roundsTop) TagCardCorner else 0.dp,
        bottomEnd = if (position.roundsBottom) TagCardCorner else 0.dp,
        bottomStart = if (position.roundsBottom) TagCardCorner else 0.dp
    )
    return clip(shape).drawBehind {
        val corner = TagCardCorner.toPx()
        val stroke = 1.dp.toPx()
        val top = if (position.roundsTop) 0f else -corner * 2
        val bottom = if (position.roundsBottom) size.height else size.height + corner * 2
        drawRoundRect(
            color = MusterColors.White,
            topLeft = Offset(0f, top),
            size = Size(size.width, bottom - top),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = MusterColors.Hairline,
            topLeft = Offset(stroke / 2, top + stroke / 2),
            size = Size(size.width - stroke, bottom - top - stroke),
            cornerRadius = CornerRadius(corner - stroke / 2, corner - stroke / 2),
            style = Stroke(width = stroke)
        )
    }
}

// 5a/5b. Rows come from the caller, which owns their menus and confirmations.
@Composable
fun MemberTagCard(
    section: TagSection,
    actions: MemberTagCardActions,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    row: @Composable (MemberRow) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MemberTagCardHeader(section = section, actions = actions, errorMessage = errorMessage)
        if (section.rows.isEmpty()) {
            MemberTagCardEmptyHint()
        } else {
            section.rows.forEachIndexed { index, memberRow ->
                val position = if (index == section.rows.lastIndex) TagCardPosition.Last else TagCardPosition.Middle
                TagCardRowFrame(position = position) { row(memberRow) }
            }
        }
    }
}

// Always the top of the card.
@Composable
fun MemberTagCardHeader(
    section: TagSection,
    actions: MemberTagCardActions,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    Column(modifier = modifier.fillMaxWidth().tagCardFrame(TagCardPosition.First)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MusterColors.Ink
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = section.rows.size.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MusterColors.Muted,
                modifier = Modifier.weight(1f)
            )
            CardMenu(tagId = section.id, reorder = section.reorder, actions = actions)
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MusterColors.OutText,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

// Draws the divider above the row, so the last row also closes the card's rounded bottom.
@Composable
fun TagCardRowFrame(
    position: TagCardPosition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().tagCardFrame(position)) {
        HorizontalDivider(color = MusterColors.Hairline)
        content()
    }
}

// Always the bottom of the card.
@Composable
fun MemberTagCardEmptyHint(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().tagCardFrame(TagCardPosition.Last)) {
        EmptyTagHint(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
    }
}

// Delete is ink like the other items: the palette has no destructive colour.
@Composable
private fun CardMenu(
    tagId: String,
    reorder: Reorder,
    actions: MemberTagCardActions,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    @Composable
    fun Item(
        label: StringResource,
        onClick: () -> Unit,
        enabled: Boolean = true
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(label)) },
            enabled = enabled,
            onClick = {
                expanded = false
                onClick()
            }
        )
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = MusterIcons.MoreVert,
                contentDescription = stringResource(Res.string.content_description_tag_options),
                tint = MusterColors.Muted,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Item(Res.string.tag_action_rename, actions.onRename)
            Item(Res.string.action_move_up, actions.onMoveUp, enabled = reorder.canMoveUp)
            Item(Res.string.action_move_down, actions.onMoveDown, enabled = reorder.canMoveDown)
            Item(Res.string.tag_action_delete, actions.onDelete)
        }
    }
}

@Composable
private fun EmptyTagHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val radius = 10.dp.toPx()
                drawRoundRect(
                    color = MusterColors.DashedOutline,
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                    )
                )
            }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.tag_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MusterColors.Muted,
            textAlign = TextAlign.Center
        )
    }
}

private val PreviewRows = listOf(
    MemberRow(
        id = "1",
        displayName = "Alex Doyle",
        status = MemberStatus.Admin,
        isSelf = true,
        canMoveToTag = true
    ),
    MemberRow(
        id = "2",
        displayName = "Dan Whelan",
        status = MemberStatus.Admin,
        canMoveToTag = true,
        canDemote = true,
        canRemove = true
    ),
    MemberRow(
        id = "3",
        displayName = "Marcus Keane",
        status = MemberStatus.Member,
        canMoveToTag = true,
        canPromote = true,
        canRemove = true
    )
)

@Composable
private fun TagCardPreview(section: TagSection, errorMessage: String? = null) {
    MusterTheme {
        Surface {
            MemberTagCard(section = section, actions = MemberTagCardActions(), errorMessage = errorMessage, modifier = Modifier.padding(16.dp)) {
                MemberRowItem(row = it, memberRowActions = MemberRowActions(), inFlight = false, errorMessage = null, isPending = false, contentPadding = TagCardRowPadding)
            }
        }
    }
}

// 5a
@Preview
@Composable
private fun TagCardWithMembersPreview() {
    TagCardPreview(
        TagSection(id = "t1", name = "Regular", rows = PreviewRows, reorder = Reorder(canMoveUp = false, canMoveDown = true))
    )
}

@Preview
@Composable
private fun TagCardDeleteErrorPreview() {
    TagCardPreview(
        TagSection(id = "t1", name = "Regular", rows = PreviewRows, reorder = Reorder(canMoveUp = false, canMoveDown = true)),
        errorMessage = "Can’t connect. Check your connection and try again."
    )
}

// 5b
@Preview
@Composable
private fun TagCardEmptyPreview() {
    TagCardPreview(
        TagSection(id = "t3", name = "Juniors", rows = emptyList(), reorder = Reorder(canMoveUp = true, canMoveDown = false))
    )
}

// Pieces stacked as separate items must read as one card.
@Preview
@Composable
private fun TagCardPiecesPreview() {
    val reorder = Reorder(canMoveUp = false, canMoveDown = true)
    val withRows = TagSection(id = "t1", name = "Regular", rows = PreviewRows, reorder = reorder)
    val empty = TagSection(id = "t3", name = "Juniors", rows = emptyList(), reorder = reorder)
    MusterTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                MemberTagCardHeader(section = withRows, actions = MemberTagCardActions())
                PreviewRows.forEachIndexed { index, row ->
                    val position = if (index == PreviewRows.lastIndex) TagCardPosition.Last else TagCardPosition.Middle
                    TagCardRowFrame(position) {
                        MemberRowItem(row = row, memberRowActions = MemberRowActions(), inFlight = false, errorMessage = null, isPending = false, contentPadding = TagCardRowPadding)
                    }
                }
                Spacer(Modifier.height(16.dp))
                MemberTagCardHeader(
                    section = withRows,
                    actions = MemberTagCardActions(),
                    errorMessage = "Can’t connect. Check your connection and try again."
                )
                TagCardRowFrame(TagCardPosition.Last) {
                    MemberRowItem(row = PreviewRows.first(), memberRowActions = MemberRowActions(), inFlight = false, errorMessage = null, isPending = false, contentPadding = TagCardRowPadding)
                }
                Spacer(Modifier.height(16.dp))
                MemberTagCardHeader(section = empty, actions = MemberTagCardActions())
                MemberTagCardEmptyHint()
            }
        }
    }
}
