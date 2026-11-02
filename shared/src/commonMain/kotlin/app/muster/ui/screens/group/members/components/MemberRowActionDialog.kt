package app.muster.ui.screens.group.members.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.muster.ui.common.components.ConfirmDialog
import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.movetotag.MoveToTagActions
import app.muster.ui.screens.group.members.movetotag.MoveToTagSheet
import app.muster.ui.screens.group.members.movetotag.TagOption
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.members_demote_body
import muster.shared.generated.resources.members_demote_cancel
import muster.shared.generated.resources.members_demote_confirm
import muster.shared.generated.resources.members_demote_title
import muster.shared.generated.resources.members_promote_body
import muster.shared.generated.resources.members_promote_cancel
import muster.shared.generated.resources.members_promote_confirm
import muster.shared.generated.resources.members_promote_title
import muster.shared.generated.resources.members_remove_body
import muster.shared.generated.resources.members_remove_cancel
import muster.shared.generated.resources.members_remove_confirm
import muster.shared.generated.resources.members_remove_title
import muster.shared.generated.resources.members_resend_body
import muster.shared.generated.resources.members_resend_cancel
import muster.shared.generated.resources.members_resend_confirm
import muster.shared.generated.resources.members_resend_title
import muster.shared.generated.resources.members_revoke_body
import muster.shared.generated.resources.members_revoke_cancel
import muster.shared.generated.resources.members_revoke_confirm
import muster.shared.generated.resources.members_revoke_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class DialogStrings(
    val title: StringResource,
    val body: StringResource,
    val confirm: StringResource,
    val cancel: StringResource
)

private val PromoteDialog = DialogStrings(
    Res.string.members_promote_title,
    Res.string.members_promote_body,
    Res.string.members_promote_confirm,
    Res.string.members_promote_cancel
)
private val DemoteDialog = DialogStrings(
    Res.string.members_demote_title,
    Res.string.members_demote_body,
    Res.string.members_demote_confirm,
    Res.string.members_demote_cancel
)
private val RemoveDialog = DialogStrings(
    Res.string.members_remove_title,
    Res.string.members_remove_body,
    Res.string.members_remove_confirm,
    Res.string.members_remove_cancel
)
private val RevokeDialog = DialogStrings(
    Res.string.members_revoke_title,
    Res.string.members_revoke_body,
    Res.string.members_revoke_confirm,
    Res.string.members_revoke_cancel
)
private val ResendDialog = DialogStrings(
    Res.string.members_resend_title,
    Res.string.members_resend_body,
    Res.string.members_resend_confirm,
    Res.string.members_resend_cancel
)

private sealed interface PendingMemberAction {

    // Holds the member action function to run on confirm.
    data class ConfirmMemberActionDialog(
        val row: MemberRow,
        val dialog: DialogStrings,
        val onConfirm: () -> Unit
    ) : PendingMemberAction

    data class ConfirmMoveToTagSheet(val row: MemberRow) : PendingMemberAction
}

data class MemberOperations(
    val onPromote: (String) -> Unit = {},
    val onDemote: (String) -> Unit = {},
    val onRemove: (String) -> Unit = {},
    val onRevokeInvitation: (String) -> Unit = {},
    val onResendInvitation: (String) -> Unit = {},
    val onSetMemberTag: (String, String?) -> Unit = { _, _ -> },
    val onCreateTagForMember: (String) -> Unit = {}
)

@Composable
fun rememberMemberRowActionsWithDialogs(
    memberOperations: MemberOperations,
    tagsOptions: List<TagOption>
): (MemberRow) -> MemberRowActions {
    var pendingMemberAction by remember { mutableStateOf<PendingMemberAction?>(null) }

    pendingMemberAction?.let { pending ->
        when (pending) {
            is PendingMemberAction.ConfirmMemberActionDialog -> {
                ConfirmDialog(
                    title = stringResource(pending.dialog.title, pending.row.displayName),
                    body = stringResource(pending.dialog.body),
                    confirmText = stringResource(pending.dialog.confirm),
                    cancelText = stringResource(pending.dialog.cancel),
                    onConfirm = {
                        pendingMemberAction = null
                        pending.onConfirm()
                    },
                    onDismiss = { pendingMemberAction = null }
                )
            }

            is PendingMemberAction.ConfirmMoveToTagSheet -> {
                MoveToTagSheet(
                    memberName = pending.row.displayName,
                    currentTagId = pending.row.tagId,
                    tags = tagsOptions,
                    actions = MoveToTagActions(
                        onPick = {
                            pendingMemberAction = null
                            memberOperations.onSetMemberTag(pending.row.id, it)
                        },
                        onNewTag = {
                            pendingMemberAction = null
                            memberOperations.onCreateTagForMember(pending.row.id)
                        },
                        onDismiss = { pendingMemberAction = null }
                    )
                )
            }
        }
    }

    return remember(memberOperations) {
        { row ->
            fun withConfirmationDialog(
                dialog: DialogStrings,
                call: (String) -> Unit
            ): (String) -> Unit = { id ->
                pendingMemberAction = PendingMemberAction.ConfirmMemberActionDialog(row, dialog) { call(id) }
            }

            MemberRowActions(
                onPromote = withConfirmationDialog(PromoteDialog, memberOperations.onPromote),
                onDemote = withConfirmationDialog(DemoteDialog, memberOperations.onDemote),
                onRemove = withConfirmationDialog(RemoveDialog, memberOperations.onRemove),
                onRevokeInvitation = withConfirmationDialog(RevokeDialog, memberOperations.onRevokeInvitation),
                onResendInvitation = withConfirmationDialog(ResendDialog, memberOperations.onResendInvitation),
                onMoveToTag = { pendingMemberAction = PendingMemberAction.ConfirmMoveToTagSheet(row) }
            )
        }
    }
}
