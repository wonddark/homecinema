package com.homecinema.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homecinema.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.painterResource

/** Hook logo mark followed by "GrabThat" + accent "Video". */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(AppIcons.Hook),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(30.dp),
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.textPrimary)) { append("GrabThat") }
                withStyle(SpanStyle(color = colors.accent)) { append("Video") }
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = (-0.4).sp,
        )
    }
}
