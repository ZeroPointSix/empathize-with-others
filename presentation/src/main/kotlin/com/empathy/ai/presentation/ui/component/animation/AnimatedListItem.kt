package com.empathy.ai.presentation.ui.component.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 列表项动画组件
 *
 * 为列表项提供淡入淡出和高度变化的动画效果
 *
 * @param visible 是否可见
 * @param modifier Modifier
 * @param content 内容
 */
@Composable
fun AnimatedListItem(
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        content()
    }
}
