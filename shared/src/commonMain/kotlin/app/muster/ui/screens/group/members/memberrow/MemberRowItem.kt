package app.muster.ui.screens.group.members.memberrow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.MemberStatus
import app.muster.ui.screens.group.members.Reorder
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_move_down
import muster.shared.generated.resources.action_move_up
import muster.shared.generated.resources.content_description_more
import muster.shared.generated.resources.members_action_demote
import muster.shared.generated.resources.members_action_move_to_tag
import muster.shared.generated.resources.members_action_promote
import muster.shared.generated.resources.members_action_remove
import muster.shared.generated.resources.members_action_resend
import muster.shared.generated.resources.members_action_revoke
import muster.shared.generated.resources.members_status_admin
import muster.shared.generated.resources.members_status_invited
import muster.shared.generated.resources.members_status_member
import muster.shared.generated.resources.members_you_badge
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Each takes the row's id: a profile id for members, an invitation id for pending rows.
data class MemberRowActions(
    val onMoveToTag: (id: String) -> Unit = {},
    val onMoveUp: (id: String) -> Unit = {},
    val onMoveDown: (id: String) -> Unit = {},
    val onPromote: (id: String) -> Unit = {},
    val onDemote: (id: String) -> Unit = {},
    val onRemove: (id: String) -> Unit = {},
    val onRevokeInvitation: (id: String) -> Unit = {},
    val onResendInvitation: (id: String) -> Unit = {}
)

@Composable
internal fun MemberRowItem(
    row: MemberRow,
    actions: MemberRowActions,
    modifier: Modifier = Modifier,
    inFlight: Boolean = false,
    errorMessage: String? = null,
    horizontalPadding: Dp = 20.dp
) {
    val isPending = row.status == MemberStatus.Pending

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isPending) PendingAvatar() else InitialsAvatar(row.initials)
            Spacer(Modifier.width(12.dp))
            if (isPending) {
                Text(
                    text = row.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, color = MusterColors.Secondary),
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = row.displayName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (row.isSelf) {
                            Spacer(Modifier.width(8.dp))
                            YouBadge()
                        }
                    }
                    Text(
                        text = stringResource(
                            if (row.status == MemberStatus.Admin) {
                                Res.string.members_status_admin
                            } else {
                                Res.string.members_status_member
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                        color = MusterColors.Muted
                    )
                }
            }
            if (isPending) {
                Spacer(Modifier.width(8.dp))
                InvitedBadge()
            }
            if (row.hasMenu) {
                Spacer(Modifier.width(4.dp))
                RowMenu(row = row, actions = actions, inFlight = inFlight)
            }
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = MusterColors.OutText)
        }
    }
}

// Reads the row's can* flags and reorder as given — which items apply was
// already decided by the ViewModel. Tag items first, then role items (5j).
@Composable
private fun RowMenu(
    row: MemberRow,
    actions: MemberRowActions,
    inFlight: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    @Composable
    fun Item(
        label: StringResource,
        onClick: (id: String) -> Unit,
        enabled: Boolean = true,
        destructive: Boolean = false
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(label),
                    color = if (destructive) MusterColors.OutText else Color.Unspecified
                )
            },
            enabled = enabled,
            onClick = {
                expanded = false
                onClick(row.id)
            }
        )
    }

    Box(modifier = modifier) {
        if (inFlight) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp).padding(8.dp),
                color = MusterColors.Muted,
                strokeWidth = 2.dp
            )
        } else {
            IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = MusterIcons.MoreVert,
                    contentDescription = stringResource(Res.string.content_description_more),
                    tint = MusterColors.Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (row.canMoveToTag) Item(Res.string.members_action_move_to_tag, actions.onMoveToTag)
                row.reorder?.let { reorder ->
                    Item(Res.string.action_move_up, actions.onMoveUp, enabled = reorder.canMoveUp)
                    Item(Res.string.action_move_down, actions.onMoveDown, enabled = reorder.canMoveDown)
                }
                if (row.canMoveToTag && row.hasRoleActions) HorizontalDivider(color = MusterColors.Hairline)
                if (row.canPromote) Item(Res.string.members_action_promote, actions.onPromote)
                if (row.canDemote) Item(Res.string.members_action_demote, actions.onDemote)
                if (row.canRemove) Item(Res.string.members_action_remove, actions.onRemove, destructive = true)
                if (row.canResendInvitation) Item(Res.string.members_action_resend, actions.onResendInvitation)
                if (row.canRevokeInvitation) Item(Res.string.members_action_revoke, actions.onRevokeInvitation)
            }
        }
    }
}

@Composable
private fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(percent = 50),
        color = MusterColors.AvatarFill
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = MusterColors.Secondary
            )
        }
    }
}

// Compose has no built-in dashed border, so it's drawn by hand — same
// technique as MessageState's DashedIconPlaceholder, circular and smaller.
@Composable
private fun PendingAvatar(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val stroke = Stroke(
        width = with(density) { 1.5.dp.toPx() },
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
    )
    Box(
        modifier = modifier.size(40.dp).drawBehind {
            drawOval(color = MusterColors.DashedOutline, style = stroke, size = Size(size.width, size.height))
        },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "@", fontSize = 16.sp, color = MusterColors.Hint)
    }
}

@Composable
private fun YouBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MusterColors.Hint),
        color = MusterColors.White
    ) {
        Text(
            text = stringResource(Res.string.members_you_badge),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MusterColors.Muted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun InvitedBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(1.dp, MusterColors.Outline),
        color = MusterColors.White
    ) {
        Text(
            text = stringResource(Res.string.members_status_invited),
            style = MaterialTheme.typography.labelSmall,
            color = MusterColors.Muted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MemberRowPreview(
    row: MemberRow,
    inFlight: Boolean = false,
    errorMessage: String? = null
) {
    MusterTheme {
        Surface {
            MemberRowItem(row = row, actions = MemberRowActions(), inFlight = inFlight, errorMessage = errorMessage)
        }
    }
}

@Preview
@Composable
private fun MemberRowSelfPreview() {
    MemberRowPreview(
        MemberRow(
            id = "1",
            displayName = "Alex Doyle",
            status = MemberStatus.Admin,
            isSelf = true,
            canMoveToTag = true,
            reorder = Reorder(canMoveUp = false, canMoveDown = true)
        )
    )
}

@Preview
@Composable
private fun MemberRowTaggedPreview() {
    MemberRowPreview(
        MemberRow(
            id = "2",
            displayName = "Sam Okafor",
            status = MemberStatus.Member,
            canMoveToTag = true,
            reorder = Reorder(canMoveUp = true, canMoveDown = true),
            canPromote = true,
            canRemove = true
        )
    )
}

@Preview
@Composable
private fun MemberRowReadOnlyPreview() {
    MemberRowPreview(MemberRow(id = "3", displayName = "Dan Whelan", status = MemberStatus.Admin))
}

@Preview
@Composable
private fun MemberRowPendingPreview() {
    MemberRowPreview(
        MemberRow(
            id = "4",
            displayName = "j.moriarty@outlook.com",
            status = MemberStatus.Pending,
            canRevokeInvitation = true,
            canResendInvitation = true
        )
    )
}

@Preview
@Composable
private fun MemberRowInFlightPreview() {
    MemberRowPreview(
        MemberRow(id = "5", displayName = "Marcus Keane", status = MemberStatus.Member, canPromote = true),
        inFlight = true
    )
}

@Preview
@Composable
private fun MemberRowFailedPreview() {
    MemberRowPreview(
        MemberRow(id = "5", displayName = "Marcus Keane", status = MemberStatus.Member, canPromote = true),
        errorMessage = "Can’t connect. Check your connection and try again."
    )
}
