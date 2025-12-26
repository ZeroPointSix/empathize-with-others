package com.empathy.ai.presentation.ui.component.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextTertiary

/**
 * iOS设置项风格的AI指令设置行
 *
 * 设计原则：
 * 1. 紧凑化，高度约60dp
 * 2. 左侧：深紫色"魔法棒"图标
 * 3. 中间：文字"AI 分析指令设置"
 * 4. 右侧：灰色Chevron（向右箭头）
 * 5. 整个区域可点击，无需额外编辑图标
 *
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun AiPromptSettingsRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 左侧图标 - 深紫色魔法棒
                Box(
                    modifier = Modifier
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = iOSPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 中间文字
                Text(
                    text = "AI 分析指令设置",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = iOSTextPrimary
                )
            }
            
            // 右侧箭头
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "进入",
                tint = iOSTextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== Previews ====================

@Preview(name = "AI指令设置行", showBackground = true)
@Composable
private fun AiPromptSettingsRowPreview() {
    EmpathyTheme {
        AiPromptSettingsRow(
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
