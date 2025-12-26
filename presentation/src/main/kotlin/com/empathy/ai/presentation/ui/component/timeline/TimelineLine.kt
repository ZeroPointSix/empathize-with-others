package com.empathy.ai.presentation.ui.component.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSSeparator

/**
 * 时光轴连接线
 * 
 * 垂直连接线，连接相邻的情绪节点
 * - 宽度2dp
 * - 使用iOSSeparator颜色
 * 
 * @param modifier 修饰符
 * @param height 线条高度
 * 
 * @see TDD-00020 4.2 TimelineLine连接线
 */
@Composable
fun TimelineLine(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp
) {
    Box(
        modifier = modifier
            .width(2.dp)
            .height(height)
            .background(iOSSeparator)
    )
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "默认高度", showBackground = true)
@Composable
private fun TimelineLineDefaultPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TimelineLine()
        }
    }
}

@Preview(name = "短线", showBackground = true)
@Composable
private fun TimelineLineShortPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TimelineLine(height = 40.dp)
        }
    }
}

@Preview(name = "长线", showBackground = true)
@Composable
private fun TimelineLineLongPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TimelineLine(height = 120.dp)
        }
    }
}
