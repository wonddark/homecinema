package com.homecinema.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homecinema.ui.theme.AppColors
import com.homecinema.ui.theme.AppSpacing
import com.homecinema.ui.theme.AppTypography
import kotlin.math.roundToInt

@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.small),
            color = AppColors.primary,
            trackColor = AppColors.surface
        )
        Text(
            text = "${(progress * 100).roundToInt()}%",
            style = AppTypography.caption,
            modifier = Modifier.padding(top = AppSpacing.small)
        )
    }
}
