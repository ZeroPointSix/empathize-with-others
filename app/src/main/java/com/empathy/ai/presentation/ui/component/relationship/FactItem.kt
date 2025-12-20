package com.empathy.ai.presentation.ui.component.relationship

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 事实条目组件
 *
 * 显示单条事实信息，包括来源标识
 *
 * @param fact 事实数据
 * @param onDelete 删除回调，为null时不显示删除按钮
 * @param modifier Modifier
 */
@Composable
fun FactItem(
    fact: Fact,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 来源图标
            Icon(
                imageVector = if (fact.source == FactSource.MANUAL) {
                    Icons.Default.Person
                } else {
                    Icons.Default.AutoAwesome
                },
                contentDescription = if (fact.source == FactSource.MANUAL) "手动添加" else "AI推断",
                tint = if (fact.source == FactSource.MANUAL) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fact.key,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fact.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fact.formatDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 删除按钮
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 事实列表组件
 *
 * 显示事实列表，按来源分组
 *
 * @param facts 事实列表
 * @param onDeleteFact 删除事实回调
 * @param isEditMode 是否为编辑模式
 * @param modifier Modifier
 */
@Composable
fun FactList(
    facts: List<Fact>,
    onDeleteFact: ((Fact) -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (facts.isEmpty()) {
        Text(
            text = "暂无事实记录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    // 按来源分组
    val manualFacts = facts.filter { it.source == FactSource.MANUAL }
    val aiFacts = facts.filter { it.source == FactSource.AI_INFERRED }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 手动添加的事实
        if (manualFacts.isNotEmpty()) {
            Text(
                text = "手动记录 (${manualFacts.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            manualFacts.forEach { fact ->
                FactItem(
                    fact = fact,
                    onDelete = if (isEditMode && onDeleteFact != null) {
                        { onDeleteFact(fact) }
                    } else null
                )
            }
        }

        // AI推断的事实
        if (aiFacts.isNotEmpty()) {
            if (manualFacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = "AI推断 (${aiFacts.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            aiFacts.forEach { fact ->
                FactItem(
                    fact = fact,
                    onDelete = if (isEditMode && onDeleteFact != null) {
                        { onDeleteFact(fact) }
                    } else null
                )
            }
        }
    }
}

// ==================== Previews ====================

@Preview(showBackground = true)
@Composable
private fun FactItemManualPreview() {
    EmpathyTheme {
        FactItem(
            fact = Fact(
                key = "职业",
                value = "产品经理",
                timestamp = System.currentTimeMillis(),
                source = FactSource.MANUAL
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FactItemAiPreview() {
    EmpathyTheme {
        FactItem(
            fact = Fact(
                key = "性格特点",
                value = "外向、热情、善于沟通",
                timestamp = System.currentTimeMillis(),
                source = FactSource.AI_INFERRED
            ),
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FactListPreview() {
    EmpathyTheme {
        FactList(
            facts = listOf(
                Fact(key = "职业", value = "产品经理", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(key = "爱好", value = "摄影、旅行", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(key = "性格", value = "外向热情", timestamp = System.currentTimeMillis(), source = FactSource.AI_INFERRED)
            ),
            isEditMode = true,
            onDeleteFact = {}
        )
    }
}
