package com.empathy.ai.presentation.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
 * ROM 类型枚举
 * 
 * 用于识别不同厂商的 ROM，以便进行字体渲染补偿
 */
enum class RomType {
    MIUI,           // 小米 MIUI / HyperOS
    EMUI,           // 华为 EMUI / HarmonyOS
    COLOR_OS,       // OPPO ColorOS
    FUNTOUCH_OS,    // vivo FuntouchOS / OriginOS
    ONE_UI,         // 三星 One UI
    OXYGEN_OS,      // 一加 OxygenOS
    AOSP            // 原生 Android
}

/**
 * 响应式尺寸数据类
 * 
 * 包含所有UI组件需要的尺寸值，根据屏幕大小自动调整
 * 
 * @Stable 注解标记此类为稳定类，减少不必要的重组
 */
@Stable
data class AdaptiveDimensionsData(
    // ========== 屏幕信息 ==========
    val screenWidth: Dp,
    val screenHeight: Dp,
    val screenSizeType: ScreenSizeType,
    val density: Float,
    val romType: RomType,
    
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
    val fontScale: Float,
    
    // ========== 响应式字体尺寸 (BUG-00036 新增) ==========
    /** 极小字体 - 用于辅助说明文字 (10sp 基准) */
    val fontSizeXSmall: TextUnit,
    /** 小字体 - 用于标签、时间戳 (12sp 基准) */
    val fontSizeCaption: TextUnit,
    /** 正文字体 - 用于列表项副标题、说明文字 (14sp 基准) */
    val fontSizeBody: TextUnit,
    /** 副标题字体 - 用于列表项标题 (16sp 基准) */
    val fontSizeSubtitle: TextUnit,
    /** 标题字体 - iOS 导航栏标准字体 (17sp 基准) */
    val fontSizeTitle: TextUnit,
    /** 大标题字体 - 用于页面标题 (20sp 基准) */
    val fontSizeHeadline: TextUnit,
    /** 超大标题字体 - iOS Large Title (34sp 基准) */
    val fontSizeLargeTitle: TextUnit
)

/**
 * CompositionLocal 用于提供响应式尺寸
 */
val LocalAdaptiveDimensions = compositionLocalOf<AdaptiveDimensionsData> {
    error("AdaptiveDimensions not provided. Wrap your content with ProvideAdaptiveDimensions.")
}

/**
 * ROM 类型检测工具
 * 
 * 检测当前设备的 ROM 类型，用于字体渲染补偿
 */
private object RomDetector {
    /**
     * 检测当前 ROM 类型
     */
    fun detectRomType(): RomType {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") || 
            manufacturer.contains("redmi") || brand.contains("redmi") -> RomType.MIUI
            
            manufacturer.contains("huawei") || brand.contains("huawei") ||
            manufacturer.contains("honor") || brand.contains("honor") -> RomType.EMUI
            
            manufacturer.contains("oppo") || brand.contains("oppo") ||
            manufacturer.contains("realme") || brand.contains("realme") -> RomType.COLOR_OS
            
            manufacturer.contains("vivo") || brand.contains("vivo") ||
            manufacturer.contains("iqoo") || brand.contains("iqoo") -> RomType.FUNTOUCH_OS
            
            manufacturer.contains("samsung") || brand.contains("samsung") -> RomType.ONE_UI
            
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> RomType.OXYGEN_OS
            
            else -> RomType.AOSP
        }
    }
    
    /**
     * 获取 ROM 特定的字体缩放补偿因子
     * 
     * 不同 ROM 的字体渲染引擎存在差异，需要进行补偿
     */
    fun getFontScaleCompensation(romType: RomType): Float {
        return when (romType) {
            RomType.MIUI -> 0.98f        // MIUI 字体略大，补偿缩小
            RomType.EMUI -> 1.0f         // EMUI 标准
            RomType.COLOR_OS -> 0.99f    // ColorOS 略大
            RomType.FUNTOUCH_OS -> 1.0f  // FuntouchOS 标准
            RomType.ONE_UI -> 1.02f      // One UI 略小，补偿放大
            RomType.OXYGEN_OS -> 1.0f    // OxygenOS 标准
            RomType.AOSP -> 1.0f         // 原生 Android 标准
        }
    }
}

/**
 * 根据屏幕配置计算响应式尺寸
 * 
 * BUG-00036 优化：
 * - 添加响应式字体尺寸
 * - 添加字体缩放边界限制（0.85-1.5）
 * - 添加高密度屏幕补偿因子
 * - 添加 ROM 厂商字体渲染补偿
 */
@Composable
fun rememberAdaptiveDimensions(): AdaptiveDimensionsData {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    return remember(configuration.screenWidthDp, configuration.screenHeightDp, configuration.fontScale, density.density) {
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        val systemFontScale = configuration.fontScale
        val screenDensity = density.density
        
        // 检测 ROM 类型
        val romType = RomDetector.detectRomType()
        
        // 确定屏幕尺寸类型
        val screenSizeType = when {
            configuration.screenWidthDp < 360 -> ScreenSizeType.COMPACT
            configuration.screenWidthDp < 600 -> ScreenSizeType.MEDIUM
            configuration.screenWidthDp < 840 -> ScreenSizeType.EXPANDED
            else -> ScreenSizeType.LARGE
        }
        
        // 计算屏幕缩放因子
        val scaleFactor = when (screenSizeType) {
            ScreenSizeType.COMPACT -> 0.85f
            ScreenSizeType.MEDIUM -> 1.0f
            ScreenSizeType.EXPANDED -> 1.1f
            ScreenSizeType.LARGE -> 1.2f
        }
        
        // ========== BUG-00036: 字体缩放边界限制 ==========
        // 限制系统字体缩放在合理范围内，防止超大/超小字体导致布局问题
        val clampedFontScale = systemFontScale.coerceIn(0.85f, 1.5f)
        
        // ========== BUG-00036: 高密度屏幕补偿因子 ==========
        // 高密度屏幕（>480dpi）的 dp 转换可能导致字体过小，需要补偿
        val densityCompensation = when {
            screenDensity > 4.0f -> 0.92f   // xxxhdpi 以上（>640dpi），略微缩小避免过大
            screenDensity > 3.5f -> 0.95f   // xxxhdpi（560-640dpi），轻微缩小
            screenDensity > 3.0f -> 0.98f   // xxhdpi（480-560dpi），微调
            screenDensity < 1.5f -> 1.05f   // hdpi 以下（<240dpi），略微放大
            else -> 1.0f                     // 正常密度
        }
        
        // ========== BUG-00036: ROM 厂商字体渲染补偿 ==========
        val romCompensation = RomDetector.getFontScaleCompensation(romType)
        
        // ========== 综合字体缩放因子 ==========
        // fontScaleFactor = 屏幕缩放 * 系统字体缩放(限制后) * 密度补偿 * ROM补偿
        val fontScaleFactor = scaleFactor * clampedFontScale * densityCompensation * romCompensation
        
        AdaptiveDimensionsData(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            screenSizeType = screenSizeType,
            density = screenDensity,
            romType = romType,
            
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
            
            // 字体缩放（原始系统值，供调试使用）
            fontScale = systemFontScale,
            
            // ========== BUG-00036: 响应式字体尺寸 ==========
            // 使用综合字体缩放因子计算，确保在各种设备和设置下都能正确显示
            fontSizeXSmall = (10 * fontScaleFactor).sp,
            fontSizeCaption = (12 * fontScaleFactor).sp,
            fontSizeBody = (14 * fontScaleFactor).sp,
            fontSizeSubtitle = (16 * fontScaleFactor).sp,
            fontSizeTitle = (17 * fontScaleFactor).sp,      // iOS 导航栏标准
            fontSizeHeadline = (20 * fontScaleFactor).sp,
            fontSizeLargeTitle = (34 * fontScaleFactor).sp  // iOS Large Title
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
