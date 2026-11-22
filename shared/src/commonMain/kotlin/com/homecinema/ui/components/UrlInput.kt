package com.homecinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.painterResource

/** Visual emphasis of the URL field: neutral, accent while fetching/ready, error after a failed fetch. */
enum class UrlInputStatus { Idle, Active, Error }

/**
 * Link field with a leading link icon. Shows the accent **Paste** button while empty and idle,
 * otherwise a round clear (×) button. IME "Go" calls [onSubmit].
 */
@Composable
fun UrlInput(
    value: String,
    status: UrlInputStatus,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val (borderWidth, borderColor, iconTint) = when (status) {
        UrlInputStatus.Idle -> Triple(1.dp, colors.border, colors.textTertiary)
        UrlInputStatus.Active -> Triple(1.5.dp, colors.accent, colors.accent)
        UrlInputStatus.Error -> Triple(1.5.dp, colors.error, colors.error)
    }
    val showPaste = value.isEmpty() && status == UrlInputStatus.Idle
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = colors.textPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colors.inputBg, AppShapes.input)
            .border(borderWidth, borderColor, AppShapes.input)
            .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(AppIcons.Link2),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Paste a video link…",
                            style = textStyle,
                            color = colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        if (showPaste) {
            PasteButton(onClick = onPaste)
        } else {
            RoundIconButton(
                icon = AppIcons.X,
                tint = colors.textSecondary,
                onClick = onClear,
                contentDescription = "Clear link",
                size = 32.dp,
                iconSize = 14.dp,
            )
        }
    }
}

@Composable
private fun PasteButton(onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(AppShapes.togglePill)
            .background(colors.accent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Paste",
            color = colors.onAccent,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
