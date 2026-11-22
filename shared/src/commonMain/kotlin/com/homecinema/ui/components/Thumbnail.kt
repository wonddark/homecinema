package com.homecinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.homecinema.domain.MediaKind
import com.homecinema.ui.theme.LocalAppColors
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.painterResource

/**
 * Video/audio thumbnail loaded with Coil. [model] may be a remote URL or a local absolute file
 * path; local paths are handed to Coil as an okio `Path`, which maps to a `file` URI on both
 * Android and desktop. With no image (or a failed load) a video shows the `skeleton` tile and
 * audio shows `audioThumbBg` with the audio-lines glyph in `accent`.
 *
 * @param dimmed alpha applied to the whole thumbnail (0.55 for failed downloads).
 */
@Composable
fun Thumbnail(
    model: Any?,
    durationSeconds: Long?,
    kind: MediaKind,
    width: Dp,
    height: Dp,
    dimmed: Float = 1f,
    cornerRadius: Dp = 10.dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val data = remember(model) { imageData(model) }
    var failed by remember(data) { mutableStateOf(false) }
    val isAudio = kind == MediaKind.AUDIO
    val placeholder = if (isAudio) colors.audioThumbBg else colors.skeleton

    Box(
        modifier = modifier
            .size(width, height)
            .alpha(dimmed)
            .clip(RoundedCornerShape(cornerRadius))
            .background(placeholder),
    ) {
        if (isAudio) {
            Icon(
                painter = painterResource(AppIcons.AudioLines),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(min(width, height) * 0.4f).align(Alignment.Center),
            )
        }
        if (data != null && !failed) {
            AsyncImage(
                model = data,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (durationSeconds != null && durationSeconds > 0) {
            DurationBadge(durationSeconds, Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp))
        }
    }
}

@Composable
private fun DurationBadge(seconds: Long, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(LocalAppColors.current.durationBadge, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(
            text = formatDuration(seconds),
            // White on the theme-independent dark badge in every palette; no token exists for it.
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun imageData(model: Any?): Any? =
    when {
        model == null -> null
        model is String && model.isBlank() -> null
        model is String && !model.contains("://") -> model.toPath()
        else -> model
    }
