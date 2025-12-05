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
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 联系人档案卡片组件
 *
 * 用于展示联系人的详细档案信息
 *
 * @param contact 联系人信息
 * @param onEdit 编辑回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCard(
    contact: ContactProfile,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 头部：头像和编辑按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(64.dp)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 姓名
            Text(
                text = contact.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 目标
            if (contact.targetGoal.isNotBlank()) {
                Text(
                    text = "目标",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.targetGoal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 上下文深度
            Text(
                text = "上下文深度",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${contact.contextDepth} 条记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 事实信息
            if (contact.facts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "事实信息",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                facts = mapOf(
                    "电话" to "138****1234",
                    "公司" to "某某科技有限公司",
                    "职位" to "技术总监",
                    "性格" to "吃软不吃硬",
                    "爱好" to "钓鱼、喝茶"
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
                facts = emptyMap()
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
                facts = mapOf(
                    "电话" to "139****5678"
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
                facts = mapOf(
                    "电话" to "138****1234",
                    "性格" to "吃软不吃硬"
                )
            ),
            onEdit = {}
        )
    }
}
