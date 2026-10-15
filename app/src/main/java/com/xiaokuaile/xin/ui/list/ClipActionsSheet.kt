package com.xiaokuaile.xin.ui.list

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.xiaokuaile.xin.R
import com.xiaokuaile.xin.data.Clip
import com.xiaokuaile.xin.data.ClipKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipActionsSheet(
    clip: Clip,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    fun act(action: () -> Unit) {
        onDismiss()
        action()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
            Text(
                text = clip.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            when (clip.kind) {
                ClipKind.LINK -> ActionItem(Icons.AutoMirrored.Filled.OpenInNew, R.string.open_link) {
                    act { context.launch(Intent(Intent.ACTION_VIEW, Uri.parse(clip.content.trim().withScheme()))) }
                }
                ClipKind.PHONE -> ActionItem(Icons.Filled.Phone, R.string.dial) {
                    act { context.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + clip.content.filter { it.isDigit() || it == '+' }))) }
                }
                ClipKind.EMAIL -> ActionItem(Icons.Filled.Email, R.string.send_email) {
                    act { context.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + clip.content.trim()))) }
                }
                ClipKind.TEXT -> Unit
            }
            ActionItem(Icons.Filled.ContentCopy, R.string.copy) { act(onCopy) }
            ActionItem(
                if (clip.pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                if (clip.pinned) R.string.unpin else R.string.pin,
            ) { act(onTogglePin) }
            ActionItem(Icons.Filled.Edit, R.string.edit) { act(onEdit) }
            ActionItem(Icons.Filled.Share, R.string.share) {
                act {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, clip.content)
                    context.launch(Intent.createChooser(send, null))
                }
            }
            ActionItem(Icons.Filled.Delete, R.string.delete) { act(onDelete) }
        }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: Int, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(stringResource(label)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun String.withScheme(): String =
    if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) this else "https://$this"

private fun Context.launch(intent: Intent) {
    runCatching { startActivity(intent) }
        .onFailure { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show() }
}
