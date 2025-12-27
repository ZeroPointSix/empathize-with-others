package com.empathy.ai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.empathy.ai.presentation.theme.DarkSemanticColors
import com.empathy.ai.presentation.theme.LightSemanticColors
import com.empathy.ai.presentation.theme.LocalSemanticColors
import com.empathy.ai.presentation.theme.ProvideAdaptiveDimensions

// ============================================================
// 颜色定义 (从 presentation 模块复制)
// ============================================================

// Primary - 主色调 (紫色系)
private val PrimaryLight = Color(0xFF6750A4)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFEADDFF)
private val OnPrimaryContainerLight = Color(0xFF21005D)

// Secondary - 辅助色 (紫灰色系)
private val SecondaryLight = Color(0xFF625B71)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFE8DEF8)
private val OnSecondaryContainerLight = Color(0xFF1D192B)

// Tertiary - 第三色 (粉色系)
private val TertiaryLight = Color(0xFF7D5260)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFFFD8E4)
private val OnTertiaryContainerLight = Color(0xFF31111D)

// Error - 错误色 (红色系)
private val ErrorLight = Color(0xFFB3261E)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFF9DEDC)
private val OnErrorContainerLight = Color(0xFF410E0B)

// Background & Surface - 背景和表面
private val BackgroundLight = Color(0xFFFFFBFE)
private val OnBackgroundLight = Color(0xFF1C1B1F)
private val SurfaceLight = Color(0xFFFFFBFE)
private val OnSurfaceLight = Color(0xFF1C1B1F)
private val SurfaceVariantLight = Color(0xFFE7E0EC)
private val OnSurfaceVariantLight = Color(0xFF49454F)

// Outline - 边框
private val OutlineLight = Color(0xFF79747E)
private val OutlineVariantLight = Color(0xFFCAC4D0)

// 深色模式颜色
private val PrimaryDark = Color(0xFFD0BCFF)
private val OnPrimaryDark = Color(0xFF381E72)
private val PrimaryContainerDark = Color(0xFF4F378B)
private val OnPrimaryContainerDark = Color(0xFFEADDFF)

private val SecondaryDark = Color(0xFFCCC2DC)
private val OnSecondaryDark = Color(0xFF332D41)
private val SecondaryContainerDark = Color(0xFF4A4458)
private val OnSecondaryContainerDark = Color(0xFFE8DEF8)

private val TertiaryDark = Color(0xFFEFB8C8)
private val OnTertiaryDark = Color(0xFF492532)
private val TertiaryContainerDark = Color(0xFF633B48)
private val OnTertiaryContainerDark = Color(0xFFFFD8E4)

private val ErrorDark = Color(0xFFF2B8B5)
private val OnErrorDark = Color(0xFF601410)
private val ErrorContainerDark = Color(0xFF8C1D18)
private val OnErrorContainerDark = Color(0xFFF9DEDC)

private val BackgroundDark = Color(0xFF1C1B1F)
private val OnBackgroundDark = Color(0xFFE6E1E5)
private val SurfaceDark = Color(0xFF1C1B1F)
private val OnSurfaceDark = Color(0xFFE6E1E5)
private val SurfaceVariantDark = Color(0xFF49454F)
private val OnSurfaceVariantDark = Color(0xFFCAC4D0)

private val OutlineDark = Color(0xFF938F99)
private val OutlineVariantDark = Color(0xFF49454F)

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
 * App模块的主题包装器
 * 
 * 这是一个临时解决方案，用于解决 presentation 模块的 ThemeKt 类
 * 在运行时无法被找到的问题。
 * 
 * @param darkTheme 是否使用深色模式,默认跟随系统
 * @param dynamicColor 是否使用动态颜色 (Android 12+)
 * @param content 主题内容
 */
@Composable
fun AppTheme(
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
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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
                content = content
            )
        }
    }
}
