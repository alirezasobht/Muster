package app.muster.ui.screens.group.members.movetotag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.move_to_tag_new
import muster.shared.generated.resources.move_to_tag_none
import muster.shared.generated.resources.move_to_tag_subtitle
import muster.shared.generated.resources.move_to_tag_title
import org.jetbrains.compose.resources.stringResource

data class TagOption(
    val id: String,
    val name: String,
    val memberCount: Int
)

data class MoveToTagActions(
    // Null removes the tag.
    val onPick: (tagId: String?) -> Unit,
    val onNewTag: () -> Unit,
    val onDismiss: () -> Unit
)

// 5k: a single choice that applies on tap. A sheet rather than a dialog because the tag list is unbounded.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToTagSheet(
    memberName: String,
    // Null when the member has no tag yet.
    currentTagId: String?,
    tags: List<TagOption>,
    actions: MoveToTagActions
) {
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MusterColors.White,
        scrimColor = MusterColors.Ink.copy(alpha = 0.32f),
        dragHandle = { DragHandle() }
    ) {
        MoveToTagContent(memberName, currentTagId, tags, actions)
    }
}

@Composable
private fun DragHandle() {
    Box(
        Modifier
            .padding(top = 8.dp, bottom = 10.dp)
            .size(width = 36.dp, height = 4.dp)
            .background(MusterColors.Outline, CircleShape)
    )
}

@Composable
private fun MoveToTagContent(
    memberName: String,
    currentTagId: String?,
    tags: List<TagOption>,
    actions: MoveToTagActions
) {
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(Res.string.move_to_tag_title, memberName),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MusterColors.Ink
            )
            Text(
                text = stringResource(Res.string.move_to_tag_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MusterColors.Muted
            )
        }
        tags.forEach { tag ->
            ChoiceRow(
                label = tag.name,
                selected = tag.id == currentTagId,
                count = tag.memberCount,
                onClick = { if (tag.id == currentTagId) actions.onDismiss() else actions.onPick(tag.id) }
            )
        }
        ChoiceRow(
            label = stringResource(Res.string.move_to_tag_none),
            selected = currentTagId == null,
            labelColor = MusterColors.Secondary,
            onClick = { if (currentTagId == null) actions.onDismiss() else actions.onPick(null) }
        )
        HorizontalDivider(color = MusterColors.Hairline, modifier = Modifier.padding(vertical = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .selectable(selected = false, role = Role.Button, onClick = actions.onNewTag)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleLarge,
                color = MusterColors.Ink,
                modifier = Modifier.width(20.dp)
            )
            Text(
                text = stringResource(Res.string.move_to_tag_new),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MusterColors.Ink
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    count: Int? = null,
    labelColor: Color = MusterColors.Ink
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, if (selected) MusterColors.Accent else MusterColors.DashedOutline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(10.dp).background(MusterColors.Accent, CircleShape))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                color = MusterColors.Muted
            )
        }
    }
}

private val PreviewActions = MoveToTagActions(onPick = {}, onNewTag = {}, onDismiss = {})

private val PreviewTags = listOf(
    TagOption("t1", "Regulars", 6),
    TagOption("t2", "Casual", 4),
    TagOption("t3", "Juniors", 0)
)

// ModalBottomSheet opens in its own window and animates in, so previews draw its content directly.
@Composable
private fun MoveToTagPreview(currentTagId: String?) {
    MusterTheme {
        Box(Modifier.fillMaxSize().background(MusterColors.Ink.copy(alpha = 0.32f)), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MusterColors.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DragHandle()
                MoveToTagContent("Sam Okafor", currentTagId, PreviewTags, PreviewActions)
            }
        }
    }
}

// 5k
@Preview
@Composable
private fun MoveToTagTaggedPreview() {
    MoveToTagPreview(currentTagId = "t2")
}

@Preview
@Composable
private fun MoveToTagUntaggedPreview() {
    MoveToTagPreview(currentTagId = null)
}
