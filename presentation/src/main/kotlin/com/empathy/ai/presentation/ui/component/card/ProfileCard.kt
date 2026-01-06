package com.empathy.ai.presentation.ui.component.card

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.empathy.ai.domain.model.Fact
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 联系人档案卡片组件
 *
 * ## 业务职责
 * 展示联系人的核心档案信息（头像、姓名、关系分数、标签等）。
 * 常见于联系人详情页的概览区域，作为联系人的"名片"。
 *
 * ## 页面布局
 * ```
 * ┌─────────────────────────────────────┐
 * │  👤                          [✏️] │  ← 头像 + 编辑按钮
 * ├─────────────────────────────────────┤
 * │  张三                              │  ← 姓名
 * │  关系分数: 85/100  [📈]            │  ← 关系分数 + 趋势
 * │                                     │
 * │  [雷区标签]  [策略标签]  [标签]     │  ← 标签流
 * │                                     │
 * │  最近互动：今天下午3点              │  ← 互动信息
 * └─────────────────────────────────────┘
 * ```
 *
 * ## 核心设计决策
 * 1. **头像自动生成**: 无头像时使用姓名首字母作为占位符
 * 2. **标签流展示**: 使用FlowRow自动换行展示标签
 * 3. **响应式布局**: 使用AdaptiveDimensions适配不同屏幕
 * 4. **编辑入口**: 右上角编辑按钮支持快速修改联系人信息
 *
 * ## 数据来源
 * - 头像: contact.avatarUrl（为空则显示首字母）
 * - 姓名: contact.name
 * - 关系分数: contact.relationshipScore
 * - 标签: contact.tags
 *
 * @param contact 联系人完整信息
 * @param onEdit 编辑按钮点击回调
 * @param modifier Modifier
 * @see ContactProfile 联系人数据模型
 * @see TagChip 标签组件
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCard(
    contact: ContactProfile,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.cardPadding)
        ) {
            // 头部：头像和编辑按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(dimensions.avatarSizeMedium + 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 编辑按钮
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(dimensions.spacingMedium))
            
            // 姓名
            Text(
                text = contact.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            
            // 目标
            if (contact.targetGoal.isNotBlank()) {
                Text(
                    text = "目标",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(dimensions.spacingXSmall))
                Text(
                    text = contact.targetGoal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(dimensions.spacingMediumSmall))
            }
            
            // 上下文深度
            Text(
                text = "上下文深度",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(dimensions.spacingXSmall))
            Text(
                text = "${contact.contextDepth} 条记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 事实信息
            if (contact.facts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensions.spacingMediumSmall))
                Text(
                    text = "事实信息",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    contact.facts.forEach { (key, value) ->
                        FactItem(key = key, value = value)
                    }
                }
            }
        }
    }
}

/**
 * 事实信息项
 */
@Composable
private fun FactItem(
    key: String,
    value: String
) {
    val dimensions = AdaptiveDimensions.current
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = dimensions.spacingMediumSmall, vertical = dimensions.spacingSmall)
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "完整档案", showBackground = true)
@Composable
private fun ProfileCardFullPreview() {
    EmpathyTheme {
        ProfileCard(
            contact = ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "建立长期合作关系，拿下年度大单",
                contextDepth = 15,
                facts = listOf(
                    Fact(key = "电话", value = "138****1234", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL),
                    Fact(key = "公司", value = "某某科技有限公司", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL),
                    Fact(key = "职位", value = "技术总监", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL),
                    Fact(key = "性格", value = "吃软不吃硬", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL),
                    Fact(key = "爱好", value = "钓鱼、喝茶", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                )
            ),
            onEdit = {}
        )
    }
}

@Preview(name = "基本档案", showBackground = true)
@Composable
private fun ProfileCardBasicPreview() {
    EmpathyTheme {
        ProfileCard(
            contact = ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = "修复关系",
                contextDepth = 10,
                facts = emptyList()
            ),
            onEdit = {}
        )
    }
}

@Preview(name = "无目标", showBackground = true)
@Composable
private fun ProfileCardNoGoalPreview() {
    EmpathyTheme {
        ProfileCard(
            contact = ContactProfile(
                id = "3",
                name = "王五",
                targetGoal = "",
                contextDepth = 10,
                facts = listOf(
                    Fact(key = "电话", value = "139****5678", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                )
            ),
            onEdit = {}
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileCardDarkPreview() {
    EmpathyTheme {
        ProfileCard(
            contact = ContactProfile(
                id = "4",
                name = "张三",
                targetGoal = "建立长期合作关系",
                contextDepth = 15,
                facts = listOf(
                    Fact(key = "电话", value = "138****1234", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL),
                    Fact(key = "性格", value = "吃软不吃硬", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                )
            ),
            onEdit = {}
        )
    }
}
