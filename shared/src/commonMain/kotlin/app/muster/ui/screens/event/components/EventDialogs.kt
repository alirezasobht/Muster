package app.muster.ui.screens.event.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.ConfirmDialog
import app.muster.ui.common.toMessage
import app.muster.ui.screens.event.EventDialogKind
import app.muster.ui.screens.event.EventDialogState
import app.muster.ui.screens.event.EventSummary
import app.muster.ui.screens.event.RosterAction
import app.muster.ui.screens.event.RosterRow
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.event_cancel_event_body
import muster.shared.generated.resources.event_cancel_event_body_no_location
import muster.shared.generated.resources.event_cancel_event_confirm
import muster.shared.generated.resources.event_cancel_event_dismiss
import muster.shared.generated.resources.event_cancel_event_title
import muster.shared.generated.resources.event_cancel_invite_body
import muster.shared.generated.resources.event_cancel_invite_cancel
import muster.shared.generated.resources.event_cancel_invite_confirm
import muster.shared.generated.resources.event_cancel_invite_title
import muster.shared.generated.resources.event_delete_body
import muster.shared.generated.resources.event_delete_body_no_location
import muster.shared.generated.resources.event_delete_cancel
import muster.shared.generated.resources.event_delete_confirm
import muster.shared.generated.resources.event_delete_title
import muster.shared.generated.resources.event_lock_body
import muster.shared.generated.resources.event_lock_cancel
import muster.shared.generated.resources.event_lock_confirm
import muster.shared.generated.resources.event_lock_title
import muster.shared.generated.resources.event_remove_from_event_body
import muster.shared.generated.resources.event_remove_from_event_cancel
import muster.shared.generated.resources.event_remove_from_event_confirm
import muster.shared.generated.resources.event_remove_from_event_title
import muster.shared.generated.resources.event_remove_from_list_body
import muster.shared.generated.resources.event_remove_from_list_cancel
import muster.shared.generated.resources.event_remove_from_list_confirm
import muster.shared.generated.resources.event_remove_from_list_title
import muster.shared.generated.resources.event_resend_invite_body
import muster.shared.generated.resources.event_resend_invite_cancel
import muster.shared.generated.resources.event_resend_invite_confirm
import muster.shared.generated.resources.event_resend_invite_title
import muster.shared.generated.resources.event_unlock_body
import muster.shared.generated.resources.event_unlock_cancel
import muster.shared.generated.resources.event_unlock_confirm
import muster.shared.generated.resources.event_unlock_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EventDialogs(
    pendingConfirmation: PendingRosterConfirmation?,
    onPendingClosed: () -> Unit,
    dialog: EventDialogState?,
    summary: EventSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    pendingConfirmation?.let { pending ->
        ConfirmDialog(
            title = stringResource(pending.dialog.title, pending.row.name),
            body = stringResource(pending.dialog.body),
            confirmText = stringResource(pending.dialog.confirm),
            cancelText = stringResource(pending.dialog.cancel),
            onConfirm = {
                onPendingClosed()
                pending.onConfirm()
            },
            onDismiss = onPendingClosed
        )
    }

    dialog?.let {
        EventConfirmation(
            dialog = it,
            summary = summary,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

// Stays open across the call: it owns the loading state and, on failure,
// the error — tapping the same button again is the retry.
@Composable
private fun EventConfirmation(
    dialog: EventDialogState,
    summary: EventSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val errorMessage = dialog.error?.toMessage()
    when (dialog.kind) {
        EventDialogKind.Lock -> ConfirmDialog(
            title = stringResource(Res.string.event_lock_title),
            body = stringResource(Res.string.event_lock_body),
            confirmText = stringResource(Res.string.event_lock_confirm),
            cancelText = stringResource(Res.string.event_lock_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            confirmLoading = dialog.inFlight,
            errorMessage = errorMessage
        )

        EventDialogKind.Unlock -> ConfirmDialog(
            title = stringResource(Res.string.event_unlock_title),
            body = stringResource(Res.string.event_unlock_body),
            confirmText = stringResource(Res.string.event_unlock_confirm),
            cancelText = stringResource(Res.string.event_unlock_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            confirmLoading = dialog.inFlight,
            errorMessage = errorMessage
        )

        EventDialogKind.Cancel -> ConfirmDialog(
            title = stringResource(Res.string.event_cancel_event_title),
            body = if (summary.location.isBlank()) {
                stringResource(Res.string.event_cancel_event_body_no_location, summary.title, summary.date)
            } else {
                stringResource(Res.string.event_cancel_event_body, summary.title, summary.date, summary.location)
            },
            confirmText = stringResource(Res.string.event_cancel_event_confirm),
            cancelText = stringResource(Res.string.event_cancel_event_dismiss),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            confirmLoading = dialog.inFlight,
            errorMessage = errorMessage
        )

        EventDialogKind.Delete -> ConfirmDialog(
            title = stringResource(Res.string.event_delete_title),
            body = if (summary.location.isBlank()) {
                stringResource(Res.string.event_delete_body_no_location, summary.title, summary.date)
            } else {
                stringResource(Res.string.event_delete_body, summary.title, summary.date, summary.location)
            },
            confirmText = stringResource(Res.string.event_delete_confirm),
            cancelText = stringResource(Res.string.event_delete_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            confirmLoading = dialog.inFlight,
            errorMessage = errorMessage
        )
    }
}

internal data class DialogStrings(
    val title: StringResource,
    val body: StringResource,
    val confirm: StringResource,
    val cancel: StringResource
)

private val CancelInviteDialog = DialogStrings(
    Res.string.event_cancel_invite_title,
    Res.string.event_cancel_invite_body,
    Res.string.event_cancel_invite_confirm,
    Res.string.event_cancel_invite_cancel
)
private val RemoveFromEventDialog = DialogStrings(
    Res.string.event_remove_from_event_title,
    Res.string.event_remove_from_event_body,
    Res.string.event_remove_from_event_confirm,
    Res.string.event_remove_from_event_cancel
)
private val RemoveFromListDialog = DialogStrings(
    Res.string.event_remove_from_list_title,
    Res.string.event_remove_from_list_body,
    Res.string.event_remove_from_list_confirm,
    Res.string.event_remove_from_list_cancel
)
private val ResendInviteDialog = DialogStrings(
    Res.string.event_resend_invite_title,
    Res.string.event_resend_invite_body,
    Res.string.event_resend_invite_confirm,
    Res.string.event_resend_invite_cancel
)

internal fun RosterAction.confirmationDialog(): DialogStrings? = when (this) {
    RosterAction.CancelInvite -> CancelInviteDialog
    RosterAction.RemoveFromEvent -> RemoveFromEventDialog
    RosterAction.RemoveFromList -> RemoveFromListDialog
    RosterAction.ResendInvite -> ResendInviteDialog
    RosterAction.SetIn, RosterAction.SetOut -> null
}

internal data class PendingRosterConfirmation(
    val row: RosterRow,
    val dialog: DialogStrings,
    val onConfirm: () -> Unit
)

@Preview
@Composable
private fun EventConfirmationLockPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                EventConfirmation(
                    dialog = EventDialogState(EventDialogKind.Lock),
                    summary = PreviewSummary,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun EventConfirmationUnlockPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                EventConfirmation(
                    dialog = EventDialogState(EventDialogKind.Unlock),
                    summary = PreviewSummary,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun EventConfirmationCancelPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                EventConfirmation(
                    dialog = EventDialogState(EventDialogKind.Cancel),
                    summary = PreviewSummary,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun EventConfirmationCancelNoLocationPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                EventConfirmation(
                    dialog = EventDialogState(EventDialogKind.Cancel),
                    summary = PreviewSummary.copy(location = ""),
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun EventConfirmationDeletePreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                EventConfirmation(
                    dialog = EventDialogState(EventDialogKind.Delete),
                    summary = PreviewSummary,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun EventConfirmationDeleteInFlightPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                EventConfirmation(
                    dialog = EventDialogState(EventDialogKind.Delete, inFlight = true),
                    summary = PreviewSummary,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun EventConfirmationDeleteErrorPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                EventConfirmation(
                    dialog = EventDialogState(EventDialogKind.Delete, error = DomainError.Network()),
                    summary = PreviewSummary,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun EventDialogsRosterConfirmationPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                EventDialogs(
                    pendingConfirmation = PendingRosterConfirmation(
                        row = PreviewRoster[3],
                        dialog = CancelInviteDialog,
                        onConfirm = {}
                    ),
                    onPendingClosed = {},
                    dialog = null,
                    summary = PreviewSummary,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}
