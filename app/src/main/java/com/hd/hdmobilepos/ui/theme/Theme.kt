package com.hd.hdmobilepos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.hd.hdmobilepos.R

private val HyundaiGreen = Color(0xFF005645)
private val HyundaiBrown = Color(0xFFC1A57A)
private val HyundaiBg = Color(0xFFF8F5EE)

private val LightColors = lightColorScheme(
    primary = HyundaiGreen,
    secondary = HyundaiBrown,
    tertiary = Color(0xFF7E6A43),
    background = HyundaiBg,
    surface = Color(0xFFFFFCF6),
    onPrimary = Color.White,
    onSecondary = Color(0xFF1F1A12),
    onBackground = Color(0xFF1C1C1C),
    onSurface = Color(0xFF1C1C1C)
)

private val DarkColors = darkColorScheme(
    primary = HyundaiGreen,
    secondary = HyundaiBrown,
    tertiary = Color(0xFFD9C29E)
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.rounded(),
        displayMedium = base.displayMedium.rounded(),
        displaySmall = base.displaySmall.rounded(),
        headlineLarge = base.headlineLarge.rounded(),
        headlineMedium = base.headlineMedium.rounded(),
        headlineSmall = base.headlineSmall.rounded(),
        titleLarge = base.titleLarge.rounded(),
        titleMedium = base.titleMedium.rounded(),
        titleSmall = base.titleSmall.rounded(),
        bodyLarge = base.bodyLarge.rounded(),
        bodyMedium = base.bodyMedium.rounded(),
        bodySmall = base.bodySmall.rounded(),
        labelLarge = base.labelLarge.rounded(),
        labelMedium = base.labelMedium.rounded(),
        labelSmall = base.labelSmall.rounded()
    )
}

private val NotoSansKrFamily = FontFamily(Font(R.font.noto_sans_kr))

private fun TextStyle.rounded(): TextStyle = copy(fontFamily = NotoSansKrFamily)

@Composable
fun PPOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
