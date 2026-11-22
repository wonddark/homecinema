package com.homecinema.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.core.format.QualityOption
import com.homecinema.domain.DownloadState
import com.homecinema.domain.DownloadTask
import com.homecinema.domain.MediaKind
import com.homecinema.ui.components.AppIcons
import com.homecinema.ui.components.DownloadProgressCard
import com.homecinema.ui.components.EmptyState
import com.homecinema.ui.components.FormatToggle
import com.homecinema.ui.components.GlassSurface
import com.homecinema.ui.components.PillButton
import com.homecinema.ui.components.QualityChips
import com.homecinema.ui.components.SectionLabel
import com.homecinema.ui.components.Thumbnail
import com.homecinema.ui.components.UrlInput
import com.homecinema.ui.components.UrlInputStatus
import com.homecinema.ui.components.Wordmark
import com.homecinema.ui.theme.AppShapes
import com.homecinema.ui.theme.LocalAppColors
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.action_download
import homecinema.shared.generated.resources.action_try_again
import homecinema.shared.generated.resources.downloads_empty_body
import homecinema.shared.generated.resources.downloads_empty_title
import homecinema.shared.generated.resources.downloads_in_progress
import homecinema.shared.generated.resources.downloads_section
import homecinema.shared.generated.resources.fetch_blocked_body
import homecinema.shared.generated.resources.fetch_blocked_title
import homecinema.shared.generated.resources.fetch_failed_body
import homecinema.shared.generated.resources.fetch_failed_title
import homecinema.shared.generated.resources.fetch_loading
import homecinema.shared.generated.resources.ffmpeg_hint
import homecinema.shared.generated.resources.history_title
import homecinema.shared.generated.resources.home_paste_link
import homecinema.shared.generated.resources.home_works_with
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The [HomeViewModel] actions the Home screen can trigger. */
@Immutable
data class HomeActions(
    val onInputChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onPaste: (String?) -> Unit,
    val onClear: () -> Unit,
    val onRetryFetch: () -> Unit,
    val onSelectKind: (MediaKind) -> Unit,
    val onSelectQuality: (QualityOption) -> Unit,
    val onDownload: () -> Unit,
    val onPause: (String) -> Unit,
    val onResume: (String) -> Unit,
    val onRetry: (String) -> Unit,
    val onRemove: (String) -> Unit,
) {
    companion object {
        fun from(vm: HomeViewModel) = HomeActions(
            onInputChange = vm::onInputChange,
            onSubmit = vm::onSubmit,
            onPaste = vm::onPaste,
            onClear = vm::onClear,
            onRetryFetch = vm::onRetryFetch,
            onSelectKind = vm::onSelectKind,
            onSelectQuality = vm::onSelectQuality,
            onDownload = vm::onDownload,
            onPause = vm::onPause,
            onResume = vm::onResume,
            onRetry = vm::onRetry,
            onRemove = vm::onRemove,
        )
    }
}

/**
 * Home: header, the "paste a link" download card and the in-progress list. Stateless; the app
 * background and system insets are applied by the root.
 */
@Composable
fun HomeScreen(
    state: HomeState,
    actions: HomeActions,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Header(onOpenHistory)
        DownloadCard(state, actions)
        DownloadsSection(state.tasks, actions)
    }
}

@Composable
private fun Header(onOpenHistory: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark()
        PillButton(
            label = stringResource(Res.string.history_title),
            onClick = onOpenHistory,
            trailingIcon = AppIcons.ArrowRight,
        )
    }
}

@Composable
private fun DownloadCard(state: HomeState, actions: HomeActions) {
    val clipboard = LocalClipboardManager.current
    val fetch = state.fetch
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel(stringResource(Res.string.home_paste_link))
            UrlInput(
                value = state.input,
                status = inputStatus(fetch),
                onValueChange = actions.onInputChange,
                onSubmit = actions.onSubmit,
                onPaste = { actions.onPaste(clipboard.getText()?.text) },
                onClear = actions.onClear,
            )
            AnimatedContent(
                targetState = fetch,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "fetch-state",
            ) { target ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (target) {
                        FetchState.Idle -> PlatformsRow()
                        is FetchState.Fetching -> FetchingContent()
                        is FetchState.Failed -> FailedContent(target.reason, actions.onRetryFetch)
                        is FetchState.Ready -> ReadyContent(target, actions)
                    }
                }
            }
        }
    }
}

private fun inputStatus(fetch: FetchState): UrlInputStatus =
    when (fetch) {
        FetchState.Idle -> UrlInputStatus.Idle
        is FetchState.Failed -> UrlInputStatus.Error
        is FetchState.Fetching, is FetchState.Ready -> UrlInputStatus.Active
    }

@Composable
private fun PlatformsRow() {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.home_works_with),
            color = colors.textMuted,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(AppIcons.Youtube, AppIcons.Instagram, AppIcons.Twitter, AppIcons.Facebook, AppIcons.Globe)
                .forEach { PlatformTile(it) }
        }
    }
}

@Composable
private fun PlatformTile(icon: DrawableResource) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.size(28.dp).background(colors.inputBg, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun FetchingContent() {
    val colors = LocalAppColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(88.dp).height(50.dp).background(colors.skeleton, AppShapes.listThumb))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(170.dp).height(10.dp).background(colors.skeleton, RoundedCornerShape(5.dp)))
            Box(Modifier.width(110.dp).height(8.dp).background(colors.skeletonAlt, RoundedCornerShape(4.dp)))
        }
    }
    Row(
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spinner()
        Text(
            text = stringResource(Res.string.fetch_loading),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Spinner() {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "spinner-angle",
    )
    Icon(
        painter = painterResource(AppIcons.LoaderCircle),
        contentDescription = null,
        tint = LocalAppColors.current.accent,
        modifier = Modifier.size(14.dp).rotate(angle),
    )
}

/** Title and detail lines for a failed fetch. */
@Composable
private fun failedText(reason: FetchFailure): Pair<String, String> =
    when (reason) {
        FetchFailure.SiteBlocked ->
            stringResource(Res.string.fetch_blocked_title) to stringResource(Res.string.fetch_blocked_body)
        FetchFailure.Generic ->
            stringResource(Res.string.fetch_failed_title) to stringResource(Res.string.fetch_failed_body)
    }

@Composable
private fun FailedContent(
    reason: FetchFailure,
    onRetryFetch: () -> Unit,
) {
    val colors = LocalAppColors.current
    val (title, detail) = failedText(reason)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(colors.errorSoft, AppShapes.round),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(AppIcons.CircleAlert),
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AppShapes.toggle)
            .background(colors.control)
            .clickable(role = Role.Button, onClick = onRetryFetch),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(AppIcons.RotateCcw),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(Res.string.action_try_again),
            color = colors.accent,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    PlatformsRow()
}

@Composable
private fun ReadyContent(ready: FetchState.Ready, actions: HomeActions) {
    VideoPreview(ready)
    val audio = ready.options.audio
    if (audio != null) {
        FormatToggle(
            selected = ready.kind,
            videoAvailable = ready.videoAvailable,
            audioLabel = audio.label,
            onSelect = actions.onSelectKind,
        )
    }
    if (ready.kind == MediaKind.VIDEO && ready.videoAvailable) {
        QualityChips(
            options = ready.options.qualities,
            selected = ready.selected,
            onSelect = actions.onSelectQuality,
        )
    }
    if (ready.options.needsFfmpegHint) {
        FfmpegHint()
    }
    DownloadButton(actions.onDownload)
}

@Composable
private fun VideoPreview(ready: FetchState.Ready) {
    val colors = LocalAppColors.current
    val info = ready.info
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(
            model = info.thumbnailUrl,
            durationSeconds = info.duration,
            kind = ready.kind,
            width = 88.dp,
            height = 50.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = info.title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOf(info.extractor, info.uploader).filterNot { it.isNullOrBlank() }.joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FfmpegHint() {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(AppIcons.CircleAlert),
            contentDescription = null,
            tint = colors.warning,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(Res.string.ffmpeg_hint),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun DownloadButton(onDownload: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(AppShapes.input)
            .background(colors.accent)
            .clickable(role = Role.Button, onClick = onDownload),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(AppIcons.ArrowDownToLine),
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(Res.string.action_download),
            color = colors.onAccent,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DownloadsSection(tasks: List<DownloadTask>, actions: HomeActions) {
    val active = tasks.filter { it.state != DownloadState.COMPLETED }
    val highlightedId = active.firstOrNull { it.state == DownloadState.DOWNLOADING }?.id
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (active.isEmpty()) stringResource(Res.string.downloads_section) else stringResource(Res.string.downloads_in_progress),
            color = LocalAppColors.current.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (active.isEmpty()) {
            EmptyState(
                title = stringResource(Res.string.downloads_empty_title),
                body = stringResource(Res.string.downloads_empty_body),
                showSteps = true,
            )
        }
        active.forEach { task ->
            DownloadProgressCard(
                task = task,
                highlighted = task.id == highlightedId,
                onPause = { actions.onPause(task.id) },
                onResume = { actions.onResume(task.id) },
                onRetry = { actions.onRetry(task.id) },
                onRemove = { actions.onRemove(task.id) },
            )
        }
    }
}
