package app.muster.ui.screens.group.members

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.common.components.MusterIcons
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

data class TagCardActions(
    val onRename: () -> Unit = {},
    val onMoveUp: () -> Unit = {},
    val onMoveDown: () -> Unit = {},
    val onDelete: () -> Unit = {}
)

// 5a/5b. Rows come from the caller, which owns their menus and confirmations.
@Composable
fun TagCard(
    section: TagSection,
    actions: TagCardActions,
    modifier: Modifier = Modifier,
    row: @Composable (MemberRow) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MusterColors.Hairline),
        color = MusterColors.White
    ) {
        Column {
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
                CardMenu(reorder = section.reorder, actions = actions)
            }
            if (section.rows.isEmpty()) {
                EmptyTagHint(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
            } else {
                section.rows.forEach {
                    HorizontalDivider(color = MusterColors.Hairline)
                    row(it)
                }
            }
        }
    }
}

// Delete is ink like the other items: the palette has no destructive colour.
@Composable
private fun CardMenu(
    reorder: Reorder,
    actions: TagCardActions,
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
        canMoveToTag = true,
        reorder = Reorder(canMoveUp = false, canMoveDown = true)
    ),
    MemberRow(
        id = "2",
        displayName = "Dan Whelan",
        status = MemberStatus.Admin,
        canMoveToTag = true,
        reorder = Reorder(canMoveUp = true, canMoveDown = true),
        canDemote = true,
        canRemove = true
    ),
    MemberRow(
        id = "3",
        displayName = "Marcus Keane",
        status = MemberStatus.Member,
        canMoveToTag = true,
        reorder = Reorder(canMoveUp = true, canMoveDown = false),
        canPromote = true,
        canRemove = true
    )
)

@Composable
private fun TagCardPreview(section: TagSection) {
    MusterTheme {
        Surface {
            TagCard(section = section, actions = TagCardActions(), modifier = Modifier.padding(16.dp)) {
                MemberRowItem(row = it, actions = MemberRowActions(), horizontalPadding = 16.dp)
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

// 5b
@Preview
@Composable
private fun TagCardEmptyPreview() {
    TagCardPreview(
        TagSection(id = "t3", name = "Juniors", rows = emptyList(), reorder = Reorder(canMoveUp = true, canMoveDown = false))
    )
}
