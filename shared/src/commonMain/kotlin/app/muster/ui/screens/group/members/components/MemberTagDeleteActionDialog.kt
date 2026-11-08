package app.muster.ui.screens.group.members.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.muster.ui.common.components.ConfirmDialog
import app.muster.ui.screens.group.members.TagSection
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.tag_delete_body
import muster.shared.generated.resources.tag_delete_body_empty
import muster.shared.generated.resources.tag_delete_cancel
import muster.shared.generated.resources.tag_delete_confirm
import muster.shared.generated.resources.tag_delete_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun rememberTagDeleteAction(onDeleteTag: (String) -> Unit): (TagSection) -> Unit {
    var pendingTagDelete by remember { mutableStateOf<TagSection?>(null) }

    pendingTagDelete?.let { tag ->
        ConfirmDialog(
            title = stringResource(Res.string.tag_delete_title, tag.name),
            body = if (tag.rows.isEmpty()) {
                stringResource(Res.string.tag_delete_body_empty)
            } else {
                pluralStringResource(Res.plurals.tag_delete_body, tag.rows.size, tag.rows.size)
            },
            confirmText = stringResource(Res.string.tag_delete_confirm),
            cancelText = stringResource(Res.string.tag_delete_cancel),
            onConfirm = {
                pendingTagDelete = null
                onDeleteTag(tag.id)
            },
            onDismiss = { pendingTagDelete = null },
            destructive = true
        )
    }

    return { pendingTagDelete = it }
}
