package com.empathy.ai.presentation.ui.component.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.empathy.ai.presentation.theme.AnimationSpec

/**
 * 视图切换动画组件
 *
 * 用于在不同视图模式之间切换，提供淡入淡出和缩放动画
 *
 * @param targetState 目标状态
 * @param modifier Modifier
 * @param content 内容
 */
@Composable
fun <T> AnimatedViewSwitch(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(animationSpec = tween(AnimationSpec.DurationNormal)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(AnimationSpec.DurationNormal)))
                .togetherWith(
                    fadeOut(animationSpec = tween(AnimationSpec.DurationNormal)) +
                            scaleOut(targetScale = 0.92f, animationSpec = tween(AnimationSpec.DurationNormal))
                )
        },
        label = "ViewSwitchAnimation"
    ) { state ->
        content(state)
    }
}
