package app.muster.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.ConfirmDialog
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.delete_account_body
import muster.shared.generated.resources.delete_account_cancel
import muster.shared.generated.resources.delete_account_confirm
import muster.shared.generated.resources.delete_account_sole_admin_body
import muster.shared.generated.resources.delete_account_sole_admin_confirm
import muster.shared.generated.resources.delete_account_sole_admin_title
import muster.shared.generated.resources.delete_account_sole_admin_warning
import muster.shared.generated.resources.delete_account_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteAccountRoute(viewModel: DeleteAccountViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeleteAccountDialog(
        state = state,
        onConfirm = viewModel::onConfirm,
        onDismiss = viewModel::onDismiss
    )
}

@Composable
fun DeleteAccountDialog(
    state: DeleteAccountUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        is DeleteAccountUiState.Hidden -> Unit
        is DeleteAccountUiState.Confirm -> ConfirmDialog(
            title = stringResource(Res.string.delete_account_title),
            body = stringResource(Res.string.delete_account_body),
            confirmText = stringResource(Res.string.delete_account_confirm),
            cancelText = stringResource(Res.string.delete_account_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            confirmLoading = state.deleting,
            errorMessage = state.error?.toMessage()
        )
        is DeleteAccountUiState.SoleAdmin -> ConfirmDialog(
            title = stringResource(Res.string.delete_account_sole_admin_title),
            body = stringResource(Res.string.delete_account_sole_admin_body) +
                state.groupNames.joinToString(separator = "", prefix = "\n") { "\n• $it" },
            confirmText = stringResource(Res.string.delete_account_sole_admin_confirm),
            cancelText = stringResource(Res.string.delete_account_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            confirmLoading = state.deleting,
            // A failed call replaces the warning; the list above still says what's at stake.
            errorMessage = state.error?.toMessage()
                ?: stringResource(Res.string.delete_account_sole_admin_warning)
        )
    }
}

@Preview
@Composable
private fun DeleteAccountConfirmPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                DeleteAccountDialog(
                    state = DeleteAccountUiState.Confirm(),
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun DeleteAccountConfirmFailedPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                DeleteAccountDialog(
                    state = DeleteAccountUiState.Confirm(error = DomainError.Network()),
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun DeleteAccountSoleAdminPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                DeleteAccountDialog(
                    state = DeleteAccountUiState.SoleAdmin(
                        groupNames = listOf("Thursday Five-a-side", "Sunday League")
                    ),
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun DeleteAccountSoleAdminDeletingPreview() {
    MusterTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                DeleteAccountDialog(
                    state = DeleteAccountUiState.SoleAdmin(
                        groupNames = listOf("Thursday Five-a-side"),
                        deleting = true
                    ),
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}
