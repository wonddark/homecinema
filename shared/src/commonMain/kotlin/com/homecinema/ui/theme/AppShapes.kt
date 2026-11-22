package com.homecinema.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radii from the redesign spec's "Shape & spacing" section. */
object AppShapes {
    val card = RoundedCornerShape(24.dp)
    val input = RoundedCornerShape(16.dp)
    val toggle = RoundedCornerShape(14.dp)
    val togglePill = RoundedCornerShape(10.dp)
    val qualityChip = RoundedCornerShape(18.dp)
    val progressItem = RoundedCornerShape(18.dp)
    val listThumb = RoundedCornerShape(10.dp)
    val pill = RoundedCornerShape(percent = 50)
    val round = CircleShape
    val sheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val menu = RoundedCornerShape(16.dp)
}
