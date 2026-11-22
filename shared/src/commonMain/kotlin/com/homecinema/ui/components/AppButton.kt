package com.homecinema.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.homecinema.ui.theme.AppColors
import com.homecinema.ui.theme.AppSpacing
import com.homecinema.ui.theme.AppTypography

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape = ShapeDefaults.ExtraLarge,
    icon: @Composable (() -> Unit)? = null,
) {
    if (text.isNotBlank()) {
        Button(
            onClick = onClick,
            modifier = modifier.then(Modifier.padding(horizontal = AppSpacing.medium)),
            enabled = enabled,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = AppColors.textPrimary,
                    disabledContainerColor = AppColors.surface,
                    disabledContentColor = AppColors.textSecondary,
                ),
            shape = shape,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp),
                    )
                } else {
                    icon?.let { it() }
                }

                Text(
                    text = text,
                    style = AppTypography.body,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier) {
            icon?.let { it() }
        }
    }
}
