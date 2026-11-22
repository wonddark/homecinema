package com.homecinema.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import homecinema.shared.generated.resources.Res
import homecinema.shared.generated.resources.geist_bold
import homecinema.shared.generated.resources.geist_medium
import homecinema.shared.generated.resources.geist_regular
import homecinema.shared.generated.resources.geist_semi_bold

@Composable
fun geistFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.geist_regular, FontWeight.Normal),
        Font(Res.font.geist_medium, FontWeight.Medium),
        Font(Res.font.geist_semi_bold, FontWeight.SemiBold),
        Font(Res.font.geist_bold, FontWeight.Bold),
    )

@Composable
fun appTypography(): Typography {
    val family = geistFontFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 15.sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
    )
}
