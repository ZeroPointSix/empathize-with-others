package com.empathy.ai.presentation.ui.component.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSOrange
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 风险等级枚举
 * 
 * @param color 对应的颜色
 * @param displayName 显示名称
 */
enum class RiskLevel(
    val color: Color,
    val displayName: String
) {
    HIGH(iOSRed, "高风险"),
    MEDIUM(iOSOrange, "中风险"),
    LOW(Color(0xFFFFD60A), "低风险")
}

/**
 * 雷区标签项组件
 * 
 * 技术要点:
 * - 左侧圆形图标（警告图标）
 * - 右侧标题+风险等级徽标+描述
 * - 背景色使用riskLevel.color.copy(alpha = 0.08f)
 * 
 * @param title 标签标题
 * @param description 标签描述
 * @param riskLevel 风险等级
 * @param onClick 点击回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 5.3 RiskTagItem雷区标签项
 */
@Composable
fun RiskTagItem(
    title: String,
    description: String,
    riskLevel: RiskLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = riskLevel.color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧警告图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = riskLevel.color.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "风险警告",
                tint = riskLevel.color,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 右侧内容
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = iOSTextPrimary
                )
                
                // 风险等级徽标
                RiskLevelBadge(riskLevel = riskLevel)
            }
            
            Text(
                text = description,
                fontSize = 13.sp,
                color = iOSTextSecondary,
                maxLines = 2
            )
        }
    }
}

/**
 * 风险等级徽标
 */
@Composable
private fun RiskLevelBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = riskLevel.color,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = riskLevel.displayName,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "高风险标签", showBackground = true)
@Composable
private fun RiskTagItemHighPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RiskTagItem(
                title = "不喜欢被催促",
                description = "对方非常讨厌被催促做决定，需要给予充足的时间考虑",
                riskLevel = RiskLevel.HIGH,
                onClick = {}
            )
        }
    }
}

@Preview(name = "中风险标签", showBackground = true)
@Composable
private fun RiskTagItemMediumPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RiskTagItem(
                title = "讨厌迟到",
                description = "对时间观念要求较高，约会尽量提前到达",
                riskLevel = RiskLevel.MEDIUM,
                onClick = {}
            )
        }
    }
}

@Preview(name = "低风险标签", showBackground = true)
@Composable
private fun RiskTagItemLowPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RiskTagItem(
                title = "不喜欢惊喜",
                description = "更喜欢提前知道计划，不太喜欢突然的惊喜",
                riskLevel = RiskLevel.LOW,
                onClick = {}
            )
        }
    }
}
