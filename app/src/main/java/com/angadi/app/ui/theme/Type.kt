package com.angadi.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.angadi.app.R

private val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex, FontWeight.Thin),
    Font(R.font.google_sans_flex, FontWeight.Light),
    Font(R.font.google_sans_flex, FontWeight.Normal),
    Font(R.font.google_sans_flex, FontWeight.Medium),
    Font(R.font.google_sans_flex, FontWeight.SemiBold),
    Font(R.font.google_sans_flex, FontWeight.Bold),
    Font(R.font.google_sans_flex, FontWeight.ExtraBold),
    Font(R.font.google_sans_flex, FontWeight.Black),
)

private fun TextStyle.withGoogleSans() = copy(fontFamily = GoogleSansFlex)

val Typography = Typography().run {
    copy(
        displayLarge = displayLarge.withGoogleSans(),
        displayMedium = displayMedium.withGoogleSans(),
        displaySmall = displaySmall.withGoogleSans(),
        headlineLarge = headlineLarge.withGoogleSans(),
        headlineMedium = headlineMedium.withGoogleSans(),
        headlineSmall = headlineSmall.withGoogleSans(),
        titleLarge = titleLarge.withGoogleSans(),
        titleMedium = titleMedium.withGoogleSans(),
        titleSmall = titleSmall.withGoogleSans(),
        bodyLarge = bodyLarge.copy(fontFamily = GoogleSansFlex, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = bodyMedium.withGoogleSans(),
        bodySmall = bodySmall.withGoogleSans(),
        labelLarge = labelLarge.withGoogleSans(),
        labelMedium = labelMedium.withGoogleSans(),
        labelSmall = labelSmall.withGoogleSans(),
    )
}
