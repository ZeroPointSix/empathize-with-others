package com.empathy.ai.presentation.ui.component.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 状态角标组件
 * 
 * 8dp圆形指示器，颜色根据DataStatus确定
 * 
 * @param status 数据状态
 * @param modifier 修饰符
 * 
 * @see TDD-00020 6.2 StatusBadge状态角标
 */
@Composable
fun StatusBadge(
    status: DataStatus,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(
                color = status.color,
                shape = CircleShape
            )
    )
}

@Preview(name = "状态角标-已完成", showBackground = true)
@Composable
private fun StatusBadgeCompletedPreview() {
    EmpathyTheme {
        StatusBadge(status = DataStatus.COMPLETED)
    }
}

@Preview(name = "状态角标-处理中", showBackground = true)
@Composable
private fun StatusBadgeProcessingPreview() {
    EmpathyTheme {
        StatusBadge(status = DataStatus.PROCESSING)
    }
}
