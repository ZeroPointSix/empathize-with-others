package com.empathy.ai.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 屏幕尺寸类型
 * 
 * 根据屏幕宽度划分设备类型
 */
enum class ScreenSizeType {
    /** 小屏手机 (宽度 < 360dp) */
    COMPACT,
    /** 普通手机 (360dp <= 宽度 < 600dp) */
    MEDIUM,
    /** 大屏手机/小平板 (600dp <= 宽度 < 840dp) */
    EXPANDED,
    /** 平板 (宽度 >= 840dp) */
    LARGE
}

/**
 * 响应式尺寸数据类
 * 
 * 包含所有UI组件需要的尺寸值，根据屏幕大小自动调整
 */
data class AdaptiveDimensionsData(
    // ========== 屏幕信息 ==========
    val screenWidth: Dp,
    val screenHeight: Dp,
    val screenSizeType: ScreenSizeType,
    
    // ========== 间距 ==========
    val spacingXSmall: Dp,
    val spacingSmall: Dp,
    val spacingMediumSmall: Dp,
    val spacingMedium: Dp,
    val spacingLarge: Dp,
    val spacingXLarge: Dp,
    
    // ========== 圆角 ==========
    val cornerRadiusSmall: Dp,
    val cornerRadiusMedium: Dp,
    val cornerRadiusLarge: Dp,
    
    // ========== 图标尺寸 ==========
    val iconSizeSmall: Dp,
    val iconSizeMedium: Dp,
    val iconSizeLarge: Dp,
    val iconSizeXLarge: Dp,
    
    // ========== 头像尺寸 ==========
    val avatarSizeSmall: Dp,
    val avatarSizeMedium: Dp,
    val avatarSizeLarge: Dp,
    
    // ========== iOS组件尺寸 ==========
    val iosListItemHeight: Dp,
    val iosNavigationBarHeight: Dp,
    val iosLargeTitleHeight: Dp,
    val iosSearchBarHeight: Dp,
    val iosSwitchWidth: Dp,
    val iosSwitchHeight: Dp,
    val iosSegmentedControlHeight: Dp,
    val iosButtonHeight: Dp,
    val iosCardPadding: Dp,
    val iosIconContainerSize: Dp,
    
    // ========== 滑动操作按钮 ==========
    val swipeActionButtonWidth: Dp,
    val swipeActionTotalWidth: Dp,
    
    // ========== 卡片尺寸 ==========
    val cardMinHeight: Dp,
    val cardPadding: Dp,
    val cardElevation: Dp,
    
    // ========== 头部尺寸 ==========
    val headerHeightExpanded: Dp,
    val headerHeightCollapsed: Dp,
    
    // ========== 进度条 ==========
    val progressBarHeight: Dp,
    
    // ========== 字体缩放因子 ==========
    val fontScale: Float
)

/**
 * CompositionLocal 用于提供响应式尺寸
 */
val LocalAdaptiveDimensions = compositionLocalOf<AdaptiveDimensionsData> {
    error("AdaptiveDimensions not provided. Wrap your content with ProvideAdaptiveDimensions.")
}

/**
 * 根据屏幕配置计算响应式尺寸
 */
@Composable
fun rememberAdaptiveDimensions(): AdaptiveDimensionsData {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    return remember(configuration.screenWidthDp, configuration.screenHeightDp, configuration.fontScale) {
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        val fontScale = configuration.fontScale
        
        // 确定屏幕尺寸类型
        val screenSizeType = when {
            configuration.screenWidthDp < 360 -> ScreenSizeType.COMPACT
            configuration.screenWidthDp < 600 -> ScreenSizeType.MEDIUM
            configuration.screenWidthDp < 840 -> ScreenSizeType.EXPANDED
            else -> ScreenSizeType.LARGE
        }
        
        // 计算缩放因子
        val scaleFactor = when (screenSizeType) {
            ScreenSizeType.COMPACT -> 0.85f
            ScreenSizeType.MEDIUM -> 1.0f
            ScreenSizeType.EXPANDED -> 1.1f
            ScreenSizeType.LARGE -> 1.2f
        }
        
        AdaptiveDimensionsData(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            screenSizeType = screenSizeType,
            
            // 间距 - 根据屏幕大小缩放
            spacingXSmall = (4 * scaleFactor).dp,
            spacingSmall = (8 * scaleFactor).dp,
            spacingMediumSmall = (12 * scaleFactor).dp,
            spacingMedium = (16 * scaleFactor).dp,
            spacingLarge = (24 * scaleFactor).dp,
            spacingXLarge = (32 * scaleFactor).dp,
            
            // 圆角 - 保持一致性，轻微缩放
            cornerRadiusSmall = (8 * scaleFactor).dp,
            cornerRadiusMedium = (12 * scaleFactor).dp,
            cornerRadiusLarge = (16 * scaleFactor).dp,
            
            // 图标尺寸
            iconSizeSmall = (16 * scaleFactor).dp,
            iconSizeMedium = (22 * scaleFactor).dp,
            iconSizeLarge = (32 * scaleFactor).dp,
            iconSizeXLarge = (48 * scaleFactor).dp,
            
            // 头像尺寸
            avatarSizeSmall = (40 * scaleFactor).dp,
            avatarSizeMedium = (56 * scaleFactor).dp,
            avatarSizeLarge = (120 * scaleFactor).dp,
            
            // iOS组件尺寸
            iosListItemHeight = (44 * scaleFactor).dp,
            iosNavigationBarHeight = (44 * scaleFactor).dp,
            iosLargeTitleHeight = (96 * scaleFactor).dp,
            iosSearchBarHeight = (36 * scaleFactor).dp,
            iosSwitchWidth = (51 * scaleFactor).dp,
            iosSwitchHeight = (31 * scaleFactor).dp,
            iosSegmentedControlHeight = (32 * scaleFactor).dp,
            iosButtonHeight = (44 * scaleFactor).dp,
            iosCardPadding = (16 * scaleFactor).dp,
            iosIconContainerSize = (40 * scaleFactor).dp,
            
            // 滑动操作按钮
            swipeActionButtonWidth = (80 * scaleFactor).dp,
            swipeActionTotalWidth = (160 * scaleFactor).dp,
            
            // 卡片尺寸
            cardMinHeight = (80 * scaleFactor).dp,
            cardPadding = (16 * scaleFactor).dp,
            cardElevation = (2 * scaleFactor).dp,
            
            // 头部尺寸
            headerHeightExpanded = (200 * scaleFactor).dp,
            headerHeightCollapsed = (56 * scaleFactor).dp,
            
            // 进度条
            progressBarHeight = (6 * scaleFactor).dp,
            
            // 字体缩放
            fontScale = fontScale
        )
    }
}

/**
 * 提供响应式尺寸的 Composable 包装器
 */
@Composable
fun ProvideAdaptiveDimensions(
    content: @Composable () -> Unit
) {
    val dimensions = rememberAdaptiveDimensions()
    CompositionLocalProvider(
        LocalAdaptiveDimensions provides dimensions,
        content = content
    )
}

/**
 * 便捷访问响应式尺寸
 */
object AdaptiveDimensions {
    val current: AdaptiveDimensionsData
        @Composable
        get() = LocalAdaptiveDimensions.current
}
