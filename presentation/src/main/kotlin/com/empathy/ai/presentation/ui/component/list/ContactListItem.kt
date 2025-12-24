package com.empathy.ai.presentation.ui.component.list

import android.content.res.Configuration
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 联系人列表项组件
 *
 * 用于在列表中展示联系人基本信息
 *
 * @param contact 联系人信息
 * @param onClick 点击事件回调
 * @param modifier 修饰符
 * @param tagCount 标签数量，用于显示标签统计
 */
@Composable
fun ContactListItem(
    contact: ContactProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tagCount: Int = 0
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像 (首字母)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 联系人信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 姓名
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 目标
                if (contact.targetGoal.isNotBlank()) {
                    Text(
                        text = contact.targetGoal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 标签数量
                if (tagCount > 0) {
                    Text(
                        text = "$tagCount 个标签",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 箭头图标
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "查看详情",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "基本联系人", showBackground = true)
@Composable
private fun ContactListItemBasicPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "建立良好的合作关系"
            ),
            onClick = {},
            tagCount = 3
        )
    }
}

@Preview(name = "无目标", showBackground = true)
@Composable
private fun ContactListItemNoGoalPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = ""
            ),
            onClick = {},
            tagCount = 0
        )
    }
}

@Preview(name = "长文本", showBackground = true)
@Composable
private fun ContactListItemLongTextPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "3",
                name = "王总经理",
                targetGoal = "通过建立深厚的信任关系，最终达成长期战略合作伙伴关系"
            ),
            onClick = {},
            tagCount = 15
        )
    }
}

@Preview(name = "单字名", showBackground = true)
@Composable
private fun ContactListItemSingleCharPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "4",
                name = "王",
                targetGoal = "修复关系"
            ),
            onClick = {},
            tagCount = 2
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactListItemDarkPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "5",
                name = "张三",
                targetGoal = "建立良好的合作关系"
            ),
            onClick = {},
            tagCount = 3
        )
    }
}
