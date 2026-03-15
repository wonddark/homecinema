package com.homecinema.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.homecinema.domain.VideoFormat
import com.homecinema.ui.theme.AppColors
import com.homecinema.ui.theme.AppTypography

@Composable
fun FormatSelector(
    formats: List<VideoFormat>,
    selectedFormatId: String?,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = formats.firstOrNull { it.formatId == selectedFormatId }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = selected?.let { "${it.resolution} (${it.formatId})" } ?: "Select format",
                style = AppTypography.body,
                color = AppColors.secondary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            formats.forEach { format ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = buildString {
                                append(format.resolution)
                                append(" [${format.formatId}]")
                                format.size?.let { append(" ~${it / 1_048_576}MB") }
                            },
                            style = AppTypography.body
                        )
                    },
                    onClick = {
                        onFormatSelected(format.formatId)
                        expanded = false
                    }
                )
            }
        }
    }
}
