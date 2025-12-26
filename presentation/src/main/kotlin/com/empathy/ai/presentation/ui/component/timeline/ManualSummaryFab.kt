package com.empathy.ai.presentation.ui.component.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSPurple

/**
 * 手动总结悬浮按钮
 * 
 * 用于事实流页面触发AI手动总结功能
 * - 使用iOSPurple作为背景色
 * - 显示AI图标（AutoAwesome）
 * 
 * @param onClick 点击回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 8.2 FactStreamTab手动总结按钮
 */
@Composable
fun ManualSummaryFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = iOSPurple,
        contentColor = Color.White
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "手动总结",
            modifier = Modifier.size(24.dp)
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "手动总结按钮", showBackground = true)
@Composable
private fun ManualSummaryFabPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ManualSummaryFab(onClick = {})
        }
    }
}
