package com.empathy.ai.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * 全局尺寸常量
 *
 * 定义应用中所有UI元素的标准尺寸，确保视觉一致性
 * 遵循Material Design 3的8dp网格系统
 */
object Dimensions {
    // ========== 头部尺寸 ==========
    
    /**
     * 头部展开状态高度
     * 用于联系人详情页的动态情感头部
     */
    val HeaderHeightExpanded = 200.dp
    
    /**
     * 头部收缩状态高度
     * 滚动后的Sticky Header高度
     */
    val HeaderHeightCollapsed = 56.dp
    
    // ========== 头像尺寸 ==========
    
    /**
     * 大头像尺寸
     * 用于详情页头部展开状态
     */
    val AvatarSizeLarge = 120.dp
    
    /**
     * 小头像尺寸
     * 用于详情页头部收缩状态
     */
    val AvatarSizeSmall = 40.dp
    
    /**
     * 中等头像尺寸
     * 用于列表项
     */
    val AvatarSizeMedium = 56.dp
    
    // ========== 间距 ==========
    
    /**
     * 极小间距
     */
    val SpacingXSmall = 4.dp
    
    /**
     * 小间距
     */
    val SpacingSmall = 8.dp
    
    /**
     * 中小间距 (12dp)
     * 用于列表项间距、表单字段间距、按钮内边距
     * 填补8dp和16dp之间的间隙
     */
    val SpacingMediumSmall = 12.dp
    
    /**
     * 中等间距
     */
    val SpacingMedium = 16.dp
    
    /**
     * 大间距
     */
    val SpacingLarge = 24.dp
    
    /**
     * 极大间距
     */
    val SpacingXLarge = 32.dp
    
    // ========== 圆角 ==========
    
    /**
     * 小圆角
     * 用于标签、按钮等小元素
     */
    val CornerRadiusSmall = 8.dp
    
    /**
     * 中等圆角
     * 用于卡片、对话框等中等元素
     */
    val CornerRadiusMedium = 16.dp
    
    /**
     * 大圆角
     * 用于底部弹窗等大元素
     */
    val CornerRadiusLarge = 24.dp
    
    // ========== 卡片尺寸 ==========
    
    /**
     * 卡片最小高度
     */
    val CardMinHeight = 80.dp
    
    /**
     * 卡片内边距
     */
    val CardPadding = 16.dp
    
    // ========== 图标尺寸 ==========
    
    /**
     * 小图标尺寸
     */
    val IconSizeSmall = 16.dp
    
    /**
     * 中等图标尺寸
     */
    val IconSizeMedium = 24.dp
    
    /**
     * 大图标尺寸
     */
    val IconSizeLarge = 32.dp
    
    /**
     * 超大图标尺寸
     */
    val IconSizeXLarge = 48.dp
    
    // ========== 时间线尺寸 ==========
    
    /**
     * 时间线节点尺寸
     */
    val TimelineNodeSize = 32.dp
    
    /**
     * 时间线线条宽度
     */
    val TimelineLineWidth = 2.dp
}
