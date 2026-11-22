package com.homecinema.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.homecinema.ui.theme.AppColors
import com.homecinema.ui.theme.AppSpacing
import com.homecinema.ui.theme.AppTypography
import kotlin.math.roundToInt

@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(AppSpacing.small),
        modifier = modifier.then(Modifier.fillMaxWidth()),
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f, fill = true),
            color = AppColors.primary,
            trackColor = AppColors.background.copy(alpha = 0.85f),
        )

        Text(
            text = "${(progress * 100).roundToInt()}%",
            style = AppTypography.caption,
            modifier = Modifier.padding(top = AppSpacing.small),
        )
    }
}
