package com.homecinema.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.action_cancel
import homecinema.shared.generated.resources.confirm_clear_action
import homecinema.shared.generated.resources.confirm_clear_body
import homecinema.shared.generated.resources.confirm_clear_title
import homecinema.shared.generated.resources.confirm_delete_action
import homecinema.shared.generated.resources.confirm_delete_body
import homecinema.shared.generated.resources.confirm_delete_title
import org.jetbrains.compose.resources.stringResource

private data class ConfirmCopy(val title: String, val body: String, val action: String)

@Composable
private fun copyFor(request: ConfirmRequest): ConfirmCopy =
    when (request) {
        is ConfirmRequest.DeleteFile -> ConfirmCopy(
            stringResource(Res.string.confirm_delete_title),
            stringResource(Res.string.confirm_delete_body),
            stringResource(Res.string.confirm_delete_action),
        )
        ConfirmRequest.ClearHistory -> ConfirmCopy(
            stringResource(Res.string.confirm_clear_title),
            stringResource(Res.string.confirm_clear_body),
            stringResource(Res.string.confirm_clear_action),
        )
    }

/** Styled confirm dialog for destructive History actions: Cancel + the action in `error`. */
@Composable
fun ConfirmDialog(request: ConfirmRequest, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        ConfirmCard(request, onConfirm, onDismiss)
    }
}

/** The dialog card on its own, so previews and render checks can show it without a dialog window. */
@Composable
fun ConfirmCard(request: ConfirmRequest, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val copy = copyFor(request)
    Column(
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(AppShapes.menu)
            .background(colors.surfaceMenu)
            .border(1.dp, colors.borderMenu, AppShapes.menu)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = copy.title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = copy.body,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogButton(stringResource(Res.string.action_cancel), colors.control, colors.textPrimary, onDismiss, Modifier.weight(1f))
            DialogButton(copy.action, colors.errorSoft, colors.error, onConfirm, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DialogButton(label: String, background: Color, content: Color, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(AppShapes.toggle)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
