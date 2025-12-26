package com.empathy.ai.presentation.theme

import androidx.compose.ui.unit.Dp

/**
 * 间距规范类型别名
 * 
 * 提供更简洁的API，同时保持与Dimensions的兼容性
 * 基于8dp网格系统 (8dp Grid System)
 * 
 * 使用示例:
 * ```kotlin
 * // 页面边距
 * Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
 * 
 * // 列表项间距
 * LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.md))
 * 
 * // 卡片内边距
 * Card { Column(Modifier.padding(AppSpacing.lg)) { ... } }
 * ```
 * 
 * 间距规范对照表:
 * | 别名 | 数值 | 用途 |
 * |------|------|------|
 * | xs   | 4dp  | 图标与文字间距、标签内边距 |
 * | sm   | 8dp  | 相关元素间距、卡片内边距 |
 * | md   | 12dp | 列表项间距、表单字段间距 |
 * | lg   | 16dp | 页面边距、Section间距 |
 * | xl   | 24dp | 主要区域分隔、模块间距 |
 * | xxl  | 32dp | 特殊强调、空状态页面边距 |
 */
object AppSpacing {
    /**
     * 超小间距 (4dp)
     * 用于: 图标与文字间距、标签内边距、紧凑元素间距
     */
    val xs: Dp = Dimensions.SpacingXSmall    // 4dp
    
    /**
     * 小间距 (8dp)
     * 用于: 相关元素间距、卡片内边距、按钮组间距
     */
    val sm: Dp = Dimensions.SpacingSmall     // 8dp
    
    /**
     * 中间距 (12dp)
     * 用于: 列表项间距、表单字段间距、按钮内边距
     */
    val md: Dp = Dimensions.SpacingMediumSmall // 12dp
    
    /**
     * 大间距 (16dp)
     * 用于: 页面边距、Section间距、卡片外边距
     */
    val lg: Dp = Dimensions.SpacingMedium    // 16dp
    
    /**
     * 超大间距 (24dp)
     * 用于: 主要区域分隔、模块间距
     */
    val xl: Dp = Dimensions.SpacingLarge     // 24dp
    
    /**
     * 特大间距 (32dp)
     * 用于: 特殊强调、空状态页面边距
     */
    val xxl: Dp = Dimensions.SpacingXLarge   // 32dp
}
