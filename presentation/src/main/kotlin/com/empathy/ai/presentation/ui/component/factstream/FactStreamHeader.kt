package com.empathy.ai.presentation.ui.component.factstream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue

/**
 * 事实流页面顶部导航栏
 * 
 * 设计规范：
 * - 标题"事实流"居中，加粗
 * - 右上角"+"号图标，iOS标准添加操作位置
 * - 简洁清爽，节省屏幕空间
 * 
 * @param onAddClick 添加按钮点击回调
 * @param modifier 修饰符
 */
@Composable
fun FactStreamHeader(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧占位，保持标题居中
        Box(modifier = Modifier.size(44.dp))
        
        // 中间标题
        Text(
            text = "事实流",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        
        // 右侧添加按钮
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加事实",
                tint = iOSBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "事实流导航栏", showBackground = true)
@Composable
private fun FactStreamHeaderPreview() {
    EmpathyTheme {
        FactStreamHeader(onAddClick = {})
    }
}
