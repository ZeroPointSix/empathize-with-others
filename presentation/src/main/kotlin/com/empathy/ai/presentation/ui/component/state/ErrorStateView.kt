package com.empathy.ai.presentation.ui.component.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 错误状态视图组件
 * 
 * 用于显示加载失败或错误时的占位视图，包含重试按钮
 * 
 * @param message 错误消息文本
 * @param onRetry 重试按钮点击回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 9.7 列表加载错误处理
 */
@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
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
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = iOSRed,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            fontSize = 15.sp,
            color = iOSTextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = iOSBlue,
                contentColor = Color.White
            )
        ) {
            Text(text = "重试")
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "错误状态视图", showBackground = true)
@Composable
private fun ErrorStateViewPreview() {
    EmpathyTheme {
        ErrorStateView(
            message = "加载失败，请检查网络连接",
            onRetry = {}
        )
    }
}

@Preview(name = "错误状态视图-长文本", showBackground = true)
@Composable
private fun ErrorStateViewLongTextPreview() {
    EmpathyTheme {
        ErrorStateView(
            message = "数据加载失败，可能是网络连接问题或服务器暂时不可用，请稍后重试",
            onRetry = {}
        )
    }
}
