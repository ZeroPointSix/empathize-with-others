package com.empathy.ai.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 品牌主色定义
 * 
 * 设计原则:
 * - 浅色模式使用较深的颜色作为Primary
 * - 深色模式使用较浅的颜色作为Primary
 * - 确保对比度符合WCAG AA标准 (4.5:1)
 */

// ============================================================
// 浅色模式颜色 (Light Mode Colors)
// ============================================================

// Primary - 主色调 (紫色系)
val PrimaryLight = Color(0xFF6750A4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEADDFF)
val OnPrimaryContainerLight = Color(0xFF21005D)

// Secondary - 辅助色 (紫灰色系)
val SecondaryLight = Color(0xFF625B71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE8DEF8)
val OnSecondaryContainerLight = Color(0xFF1D192B)

// Tertiary - 第三色 (粉色系)
val TertiaryLight = Color(0xFF7D5260)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD8E4)
val OnTertiaryContainerLight = Color(0xFF31111D)

// Error - 错误色 (红色系)
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

// Background & Surface - 背景和表面
val BackgroundLight = Color(0xFFFFFBFE)
val OnBackgroundLight = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFFFBFE)
val OnSurfaceLight = Color(0xFF1C1B1F)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val OnSurfaceVariantLight = Color(0xFF49454F)

// Outline - 边框
val OutlineLight = Color(0xFF79747E)
val OutlineVariantLight = Color(0xFFCAC4D0)

// ============================================================
// 深色模式颜色 (Dark Mode Colors)
// ============================================================

// Primary - 主色调 (紫色系,更亮)
val PrimaryDark = Color(0xFFD0BCFF)
val OnPrimaryDark = Color(0xFF381E72)
val PrimaryContainerDark = Color(0xFF4F378B)
val OnPrimaryContainerDark = Color(0xFFEADDFF)

// Secondary - 辅助色 (紫灰色系,更亮)
val SecondaryDark = Color(0xFFCCC2DC)
val OnSecondaryDark = Color(0xFF332D41)
val SecondaryContainerDark = Color(0xFF4A4458)
val OnSecondaryContainerDark = Color(0xFFE8DEF8)

// Tertiary - 第三色 (粉色系,更亮)
val TertiaryDark = Color(0xFFEFB8C8)
val OnTertiaryDark = Color(0xFF492532)
val TertiaryContainerDark = Color(0xFF633B48)
val OnTertiaryContainerDark = Color(0xFFFFD8E4)

// Error - 错误色 (红色系,更亮)
val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

// Background & Surface - 背景和表面 (深色)
val BackgroundDark = Color(0xFF1C1B1F)
val OnBackgroundDark = Color(0xFFE6E1E5)
val SurfaceDark = Color(0xFF1C1B1F)
val OnSurfaceDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF49454F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)

// Outline - 边框
val OutlineDark = Color(0xFF938F99)
val OutlineVariantDark = Color(0xFF49454F)

// ============================================================
// 语义化颜色 (Semantic Colors)
// ============================================================

// Success - 成功色 (绿色系)
val SuccessLight = Color(0xFF4CAF50)
val SuccessDark = Color(0xFF81C784)

// Warning - 警告色 (橙色系)
val WarningLight = Color(0xFFFF9800)
val WarningDark = Color(0xFFFFB74D)

// Info - 信息色 (蓝色系)
val InfoLight = Color(0xFF2196F3)
val InfoDark = Color(0xFF64B5F6)

// ============================================================
// 品牌色 (Brand Colors)
// ============================================================

// 暖金色 - 用于主要操作按钮、光标等
val BrandWarmGold = Color(0xFFF5A623)

// 暖橙色 - 用于警告状态（如字数接近限制）
val BrandWarmOrange = Color(0xFFFF9500)

// 琥珀色 - 用于强调和高亮
val BrandWarmAmber = Color(0xFFFFCC00)
