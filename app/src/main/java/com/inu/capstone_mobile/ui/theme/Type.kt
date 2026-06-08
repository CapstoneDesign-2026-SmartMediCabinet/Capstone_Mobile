package com.inu.capstone_mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.inu.capstone_mobile.R

private val DefaultTypography = Typography()

val PretendardFamily = FontFamily(
    Font(R.font.pretendard_thin, FontWeight.Thin),
    Font(R.font.pretendard_extralight, FontWeight.ExtraLight),
    Font(R.font.pretendard_light, FontWeight.Light),
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
    Font(R.font.pretendard_black, FontWeight.Black)
)

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = PretendardFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = PretendardFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = PretendardFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = PretendardFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = PretendardFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = PretendardFamily),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.ExtraBold
    ),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = PretendardFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = PretendardFamily),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = PretendardFamily),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = PretendardFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = PretendardFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = PretendardFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = PretendardFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = PretendardFamily)
)