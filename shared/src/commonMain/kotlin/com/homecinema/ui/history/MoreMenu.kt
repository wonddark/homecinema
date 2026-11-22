package com.homecinema.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.core.settings.ThemePreference
import com.homecinema.ui.components.AppIcons
import com.homecinema.ui.text.themeLabel
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.action_more
import homecinema.shared.generated.resources.menu_clear_history
import homecinema.shared.generated.resources.menu_close
import homecinema.shared.generated.resources.menu_open_folder
import homecinema.shared.generated.resources.menu_selected
import homecinema.shared.generated.resources.menu_theme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val ItemShape = RoundedCornerShape(10.dp)

/**
 * The More menu with its `scrimMenu` backdrop and the active More button drawn above it, laid
 * out to line up with [Header]. Tapping the scrim dismisses.
 */
@Composable
internal fun MoreMenuOverlay(
    theme: ThemePreference,
    onDismiss: () -> Unit,
    onTheme: (ThemePreference) -> Unit,
    onOpenDownloadsFolder: () -> Unit,
    onClearHistory: () -> Unit,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrimMenu)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(Res.string.menu_close),
                onClick = onDismiss,
            ),
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Row(modifier = Modifier.height(HEADER_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            HeaderButton(AppIcons.Ellipsis, stringResource(Res.string.action_more), onDismiss, active = true)
        }
        Spacer(Modifier.height(8.dp))
        MoreMenu(
            theme = theme,
            onTheme = onTheme,
            onOpenDownloadsFolder = onOpenDownloadsFolder,
            onClearHistory = onClearHistory,
        )
    }
}

/**
 * The dropdown card: Theme (expands an inline list of the five themes), Open downloads folder,
 * divider, Clear history.
 */
@Composable
fun MoreMenu(
    theme: ThemePreference,
    onTheme: (ThemePreference) -> Unit,
    onOpenDownloadsFolder: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyThemeExpanded: Boolean = false,
) {
    val colors = LocalAppColors.current
    var themeExpanded by rememberSaveable { mutableStateOf(initiallyThemeExpanded) }
    Column(
        modifier = modifier
            .width(236.dp)
            .shadow(24.dp, AppShapes.menu, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(AppShapes.menu)
            .background(colors.surfaceMenu)
            .border(1.dp, colors.borderMenu, AppShapes.menu)
            .padding(6.dp),
    ) {
        MenuItem(
            icon = AppIcons.Palette,
            label = stringResource(Res.string.menu_theme),
            onClick = { themeExpanded = !themeExpanded },
            trailing = {
                Text(
                    text = themeLabel(theme),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                )
                Icon(
                    painter = painterResource(AppIcons.ChevronRight),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(14.dp).rotate(if (themeExpanded) 90f else 0f),
                )
            },
        )
        if (themeExpanded) {
            ThemePreference.entries.forEach { option ->
                ThemeOption(option, selected = option == theme, onClick = { onTheme(option) })
            }
        }
        MenuItem(AppIcons.FolderOpen, stringResource(Res.string.menu_open_folder), onOpenDownloadsFolder)
        Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            HorizontalDivider(thickness = 1.dp, color = colors.borderMenu)
        }
        MenuItem(AppIcons.Trash2, stringResource(Res.string.menu_clear_history), onClearHistory, destructive = true)
    }
}

@Composable
private fun MenuItem(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val colors = LocalAppColors.current
    val iconTint = if (destructive) colors.error else colors.textChip
    val labelColor = if (destructive) colors.error else colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ItemShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = labelColor,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        trailing()
    }
}

/** A theme choice in the expanded Theme list, indented to the menu labels; the current one is checked. */
@Composable
private fun ThemeOption(option: ThemePreference, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ItemShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(start = 42.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = themeLabel(option),
            modifier = Modifier.weight(1f),
            color = if (selected) colors.textPrimary else colors.textChip,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (selected) {
            Icon(
                painter = painterResource(AppIcons.Check),
                contentDescription = stringResource(Res.string.menu_selected),
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
