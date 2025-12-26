package com.empathy.ai.presentation.ui.component.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.theme.iOSTextTertiary

/**
 * 空状态视图组件
 * 
 * 用于显示列表或内容为空时的占位视图
 * 
 * @param message 显示的消息文本
 * @param modifier 修饰符
 * 
 * @see TDD-00020 9.7 列表加载错误处理
 */
@Composable
fun EmptyStateView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            tint = iOSTextTertiary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            fontSize = 15.sp,
            color = iOSTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "空状态视图", showBackground = true)
@Composable
private fun EmptyStateViewPreview() {
    EmpathyTheme {
        EmptyStateView(message = "暂无数据")
    }
}

@Preview(name = "空状态视图-长文本", showBackground = true)
@Composable
private fun EmptyStateViewLongTextPreview() {
    EmpathyTheme {
        EmptyStateView(message = "暂无事实记录，点击右下角按钮添加第一条记录")
    }
}
