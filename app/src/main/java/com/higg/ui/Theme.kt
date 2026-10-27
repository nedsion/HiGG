package com.higg.ui

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.higg.R

/*
 * Hệ thống thiết kế HiGG (ui-ux-pro-max: Minimalism & Swiss Style, bảng màu "AI/Chatbot Platform").
 * Mọi màn hình chỉ dùng token từ MaterialTheme, không viết mã màu trực tiếp.
 * tertiary = màu "đã xong" (xanh lá), secondary = xanh ngọc cho tương tác phụ.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF0E7490),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFF047857),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = Color(0xFFFAF5FF),
    onBackground = Color(0xFF1E1B4B),
    surface = Color(0xFFFAF5FF),
    onSurface = Color(0xFF1E1B4B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF5F0FE),
    surfaceContainerHigh = Color(0xFFECEEF9),
    surfaceVariant = Color(0xFFECEEF9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF8B85A8),
    outlineVariant = Color(0xFFDDD6FE),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF1E1048),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = Color(0xFF0F0D1A),
    onBackground = Color(0xFFF5F3FF),
    surface = Color(0xFF0F0D1A),
    onSurface = Color(0xFFF5F3FF),
    surfaceContainerLowest = Color(0xFF0B0914),
    surfaceContainerLow = Color(0xFF17142A),
    surfaceContainer = Color(0xFF1A1730),
    surfaceContainerHigh = Color(0xFF242040),
    surfaceVariant = Color(0xFF262241),
    onSurfaceVariant = Color(0xFFC4C2D9),
    outline = Color(0xFF8F8AAE),
    outlineVariant = Color(0xFF3B3561),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    R.font.inter, weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Inter = FontFamily(
    inter(FontWeight.Normal), inter(FontWeight.Medium), inter(FontWeight.SemiBold), inter(FontWeight.Bold),
)

private val HiggTypography = Typography().let { base ->
    fun TextStyle.inter() = copy(fontFamily = Inter)
    Typography(
        displaySmall = base.displaySmall.inter().copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.inter().copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineSmall = base.headlineSmall.inter().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.inter().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.inter().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.inter().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.inter().copy(lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.inter().copy(lineHeight = 21.sp),
        bodySmall = base.bodySmall.inter(),
        labelLarge = base.labelLarge.inter().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.inter().copy(fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.inter().copy(fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
    )
}

/** Nhịp khoảng cách 8dp. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** Thời lượng chuyển động dùng chung (ms). */
object Motion {
    const val PRESS = 120
    const val STATE = 250
    const val PULSE = 1600
}

@Composable
fun HiggTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = HiggTypography,
        content = content,
    )
}

/** Người dùng tắt hiệu ứng (Tùy chọn nhà phát triển / Trợ năng → tắt hoạt ảnh) thì không chạy hiệu ứng lặp. */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

val ColorScheme.success get() = tertiary
