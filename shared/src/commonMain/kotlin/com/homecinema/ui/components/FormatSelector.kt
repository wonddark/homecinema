package com.homecinema.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homecinema.domain.VideoFormat
import com.homecinema.ui.theme.AppColors
import com.homecinema.ui.theme.AppTypography
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.check_circle_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun FormatSelector(
    formats: List<VideoFormat>,
    selectedFormatId: String?,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = formats.lastOrNull { it.formatId == selectedFormatId }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = selected?.resolution ?: "Select format",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.secondary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            formats.forEach { format ->
                DropdownMenuItem(
                    leadingIcon =
                        if (format.formatId === selectedFormatId) {
                            {
                                Icon(
                                    painter = painterResource(Res.drawable.check_circle_24px),
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        } else {
                            null
                        },
                    text = {
                        Text(
                            text =
                                buildString {
                                    append(format.resolution)
                                    format.size?.let { append(" ~${it / 1_048_576}MB") }
                                },
                            style = AppTypography.body,
                        )
                    },
                    onClick = {
                        onFormatSelected(format.formatId)
                        expanded = false
                    },
                )
            }
        }
    }
}
