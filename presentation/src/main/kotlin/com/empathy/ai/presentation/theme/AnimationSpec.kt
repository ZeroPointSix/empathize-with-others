package com.empathy.ai.presentation.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween

/**
 * 全局动画规范
 *
 * 定义应用中所有动画的标准参数，确保动画体验一致
 * 遵循Material Design 3的动画指南
 */
object AnimationSpec {
    // ========== 动画时长 ==========
    
    /**
     * 快速动画时长 (150ms)
     * 用于小元素的快速响应，如按钮点击、开关切换
     */
    const val DurationFast = 150
    
    /**
     * 标准动画时长 (300ms)
     * 用于大多数UI过渡，如页面切换、卡片展开
     */
    const val DurationNormal = 300
    
    /**
     * 慢速动画时长 (500ms)
     * 用于需要强调的过渡，如重要状态变化
     */
    const val DurationSlow = 500
    
    /**
     * 颜色过渡时长 (1000ms)
     * 用于背景光晕等需要平滑过渡的颜色变化
     */
    const val DurationColorTransition = 1000
    
    /**
     * 呼吸动画时长 (2000ms)
     * 用于AI推测标签的呼吸效果
     */
    const val DurationBreathing = 2000
    
    // ========== 缓动曲线 ==========
    
    /**
     * 标准缓动曲线
     * 快速开始，慢速结束，适合大多数UI过渡
     */
    val EasingStandard = FastOutSlowInEasing
    
    /**
     * 线性缓动曲线
     * 匀速运动，适合循环动画
     */
    val EasingLinear = LinearEasing
    
    // ========== 预定义动画规范 ==========
    
    /**
     * 颜色过渡动画规范
     * 用于背景光晕的颜色变化
     */
    val ColorTransition = tween<Float>(
        durationMillis = DurationColorTransition,
        easing = EasingStandard
    )
    
    /**
     * 视图切换动画规范
     * 用于时光轴↔清单列表的切换
     */
    val ViewTransition = tween<Float>(
        durationMillis = DurationNormal,
        easing = EasingStandard
    )
    
    /**
     * 呼吸动画规范
     * 用于AI推测标签的透明度变化
     */
    val BreathingAnimation = infiniteRepeatable<Float>(
        animation = tween(
            durationMillis = DurationBreathing,
            easing = EasingLinear
        ),
        repeatMode = RepeatMode.Reverse
    )
    
    /**
     * 快速淡入动画规范
     */
    val FastFadeIn = tween<Float>(
        durationMillis = DurationFast,
        easing = EasingStandard
    )
    
    /**
     * 快速淡出动画规范
     */
    val FastFadeOut = tween<Float>(
        durationMillis = DurationFast,
        easing = EasingStandard
    )

    // ========== Phase 2: 交互动效系统扩展 ==========

    /**
     * 页面进入动画时长 (300ms)
     */
    const val DurationPageEnter = 300

    /**
     * 页面退出动画时长 (250ms)
     */
    const val DurationPageExit = 250

    /**
     * 弹簧阻尼比 (0.8f)
     * 用于由于用户交互引起的弹性动画
     */
    const val SpringDampingRatio = 0.8f

    /**
     * 弹簧刚度 (300f)
     */
    const val SpringStiffness = 300f

    /**
     * 页面进入转场 (Forward Enter)
     * 从右侧滑入 + 淡入
     */
    val PageEnterTransition = androidx.compose.animation.slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(DurationPageEnter, easing = EasingStandard)
    ) + androidx.compose.animation.fadeIn(
        animationSpec = tween(DurationPageEnter)
    )

    /**
     * 页面退出转场 (Forward Exit)
     * 向左侧滑出1/3 + 淡出
     */
    val PageExitTransition = androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(DurationPageExit, easing = EasingStandard)
    ) + androidx.compose.animation.fadeOut(
        animationSpec = tween(DurationPageExit)
    )

    /**
     * 页面返回进入转场 (Back Enter)
     * 从左侧1/3滑入 + 淡入
     */
    val PagePopEnterTransition = androidx.compose.animation.slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(DurationPageEnter, easing = EasingStandard)
    ) + androidx.compose.animation.fadeIn(
        animationSpec = tween(DurationPageEnter)
    )

    /**
     * 页面返回退出转场 (Back Exit)
     * 向右侧滑出 + 淡出
     */
    val PagePopExitTransition = androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(DurationPageExit, easing = EasingStandard)
    ) + androidx.compose.animation.fadeOut(
        animationSpec = tween(DurationPageExit)
    )

    /**
     * 简单的淡入转场
     */
    val FadeInTransition = androidx.compose.animation.fadeIn(
        animationSpec = tween(DurationNormal)
    )

    /**
     * 简单的淡出转场
     */
    val FadeOutTransition = androidx.compose.animation.fadeOut(
        animationSpec = tween(DurationNormal)
    )
}
