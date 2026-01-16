package com.empathy.ai.presentation.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * 浅色配色方案
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

/**
 * 深色配色方案
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

/**
 * Empathy应用主题
 *
 * ## 业务职责
 * 定义应用的整体视觉风格，包括：
 * - 浅色/深色配色方案
 * - Material 3 主题配置
 * - 动态颜色支持（Android 12+）
 * - 语义化颜色（SemanticColors）
 * - 响应式尺寸（AdaptiveDimensions）
 *
 * ## 主题层级
 * ```
 * Theme.kt
 * ├── LightColorScheme  / DarkColorScheme  ← Material 3 配色
 * ├── SemanticColors (Local)                ← 语义化颜色
 * ├── AdaptiveDimensions (Local)           ← 响应式尺寸
 * └── Typography                            ← 字体排版
 * ```
 *
 * ## 颜色系统设计
 * - **Material 3**: 基于Material Design 3的色彩系统
 * - **动态颜色**: Android 12+ 可从壁纸提取主题色
 * - **语义化颜色**: 按功能命名（成功、警告、错误等）
 * - **iOS风格**: 额外的iOS风格颜色（iOSBlue, iOSPurple等）
 *
 * ## 主题选择逻辑
 * ```
 * if (dynamicColor && Android 12+) {
 *     // 动态颜色：跟随系统壁纸主题
 *     colorScheme = dynamicDarkColorScheme / dynamicLightColorScheme
 * } else if (darkTheme) {
 *     // 深色模式：预定义的深色配色
 *     colorScheme = DarkColorScheme
 * } else {
 *     // 浅色模式：预定义的浅色配色
 *     colorScheme = LightColorScheme
 * }
 * ```
 *
 * ## 设计决策
 * 1. **动态颜色默认启用**: 提升用户体验，个性化主题
 * 2. **系统主题跟随**: 默认跟随系统深色模式设置
 * 3. **状态栏颜色同步**: 状态栏颜色随主题变化
 * 4. **CompositionLocal**: 语义化颜色通过Local提供，支持嵌套覆盖
 *
 * ## 关联组件
 * - Color.kt: 颜色值定义
 * - Type.kt: 字体排版定义
 * - AnimationSpec.kt: 动画规格定义
 *
 * @param darkTheme 是否使用深色模式，默认跟随系统设置
 * @param dynamicColor 是否使用动态颜色（Android 12+），默认启用
 * @param content 主题包裹的UI内容
 * @see LightColorScheme 浅色配色方案
 * @see DarkColorScheme 深色配色方案
 * @see SemanticColors 语义化颜色
 */
@Composable
fun EmpathyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 支持动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        // 深色模式
        darkTheme -> DarkColorScheme
        // 浅色模式
        else -> LightColorScheme
    }

    val view = LocalView.current
    val activity = view.context as? ComponentActivity
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            val statusBarStyle = if (darkTheme) {
                SystemBarStyle.dark(colorScheme.surface.toArgb())
            } else {
                SystemBarStyle.light(
                    colorScheme.surface.toArgb(),
                    colorScheme.surfaceVariant.copy(alpha = 0.12f).toArgb()
                )
            }
            val navigationBarStyle = SystemBarStyle.auto(
                lightScrim = colorScheme.background.copy(alpha = 0.9f).toArgb(),
                darkScrim = colorScheme.background.toArgb()
            )
            activity.enableEdgeToEdge(
                statusBarStyle = statusBarStyle,
                navigationBarStyle = navigationBarStyle
            )
        }
    }

    // 选择语义化颜色
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(
        LocalSemanticColors provides semanticColors
    ) {
        // 提供响应式尺寸
        ProvideAdaptiveDimensions {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                content = content
            )
        }
    }
}

/**
 * 向后兼容的主题别名
 * 保留旧名称以避免破坏现有代码
 */
@Composable
fun GiveLoveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    EmpathyTheme(darkTheme, dynamicColor, content)
}
