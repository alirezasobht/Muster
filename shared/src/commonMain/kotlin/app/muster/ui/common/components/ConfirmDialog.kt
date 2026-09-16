package app.muster.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    confirmLoading: Boolean = false,
    errorMessage: String? = null
) {
    AlertDialog(
        // While loading, this dialog is the only way to see the result — closing
        // it (back, tap-outside) would orphan the in-flight call.
        onDismissRequest = { if (!confirmLoading) onDismiss() },
        title = { Text(title) },
        text = {
            Column {
                Text(body)
                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    FormError(message = errorMessage)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !confirmLoading) {
                if (confirmLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = if (destructive) MusterColors.Ink else MaterialTheme.colorScheme.primary
                    )
                } else {
                    // DESIGN.md: destructive buttons are ink, not accent — there
                    // is no destructive colour in the palette.
                    Text(confirmText, color = if (destructive) MusterColors.Ink else Color.Unspecified)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !confirmLoading) { Text(cancelText) }
        }
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                ConfirmDialog(
                    title = "Discard changes?",
                    body = "Your name hasn’t been saved yet.",
                    confirmText = "Discard",
                    cancelText = "Keep editing",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmDialogDestructivePreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                ConfirmDialog(
                    title = "Archive this group?",
                    body = "It disappears for everyone, including you, and its events stop accepting changes. This can’t be undone in the app.",
                    confirmText = "Archive",
                    cancelText = "Cancel",
                    onConfirm = {},
                    onDismiss = {},
                    destructive = true
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmDialogLoadingPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                ConfirmDialog(
                    title = "Leave this group?",
                    body = "You’ll lose access to this group, and any RSVPs you’ve made will be removed.",
                    confirmText = "Leave",
                    cancelText = "Cancel",
                    onConfirm = {},
                    onDismiss = {},
                    confirmLoading = true
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmDialogErrorPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                ConfirmDialog(
                    title = "Leave this group?",
                    body = "You’ll lose access to this group, and any RSVPs you’ve made will be removed.",
                    confirmText = "Leave",
                    cancelText = "Cancel",
                    onConfirm = {},
                    onDismiss = {},
                    errorMessage = "A group needs at least one admin. Promote someone else first."
                )
            }
        }
    }
}
