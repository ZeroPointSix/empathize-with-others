package com.empathy.ai.presentation.ui.component.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary


/**
 * 数据统计卡片组件 (Material Design 3 风格优化)
 * 
 * 设计原则:
 * - 数字使用Black字重，成为绝对的视觉中心
 * - 右侧图标添加微投影(Glow)，营造"浮"在卡片上的效果
 * - 卡片圆角20dp，与数据来源卡片保持一致
 * - 装饰性背景圆形增加视觉层次
 * 
 * 技术要点:
 * - 装饰性背景圆形（模糊效果）
 * - 显示数据总量（大字体数字+单位）
 * - 右侧图标（BarChart图标，蓝色背景+微投影）
 * - 卡片圆角20dp
 * 
 * @param totalCount 数据总量
 * @param unit 单位（如：条）
 * @param modifier 修饰符
 * 
 * @see TDD-00020 6.1 DataStatisticsCard统计卡片
 */
@Composable
fun DataStatisticsCard(
    totalCount: Int,
    unit: String = "条",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        // 增大圆角到20dp，与数据来源卡片保持一致
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：数据统计
                Column {
                    Text(
                        text = "数据总量",
                        fontSize = 14.sp,
                        color = iOSTextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // 数字使用Black字重，成为绝对的视觉中心
                        Text(
                            text = totalCount.toString(),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = iOSTextPrimary
                        )
                        Text(
                            text = unit,
                            fontSize = 16.sp,
                            color = iOSTextSecondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                    }
                }
                
                // 右侧：图标（添加微投影效果）
                Box(
                    modifier = Modifier
                        // 添加同色系微投影，营造"浮"在卡片上的效果
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = iOSBlue.copy(alpha = 0.3f),
                            spotColor = iOSBlue.copy(alpha = 0.3f)
                        )
                        .size(52.dp)
                        .background(
                            color = iOSBlue,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "数据统计",
                        // 纯白图标，与高饱和度背景形成对比
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "数据统计卡片", showBackground = true)
@Composable
private fun DataStatisticsCardPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DataStatisticsCard(totalCount = 1234)
        }
    }
}

@Preview(name = "数据统计卡片-小数量", showBackground = true)
@Composable
private fun DataStatisticsCardSmallPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DataStatisticsCard(totalCount = 42)
        }
    }
}
