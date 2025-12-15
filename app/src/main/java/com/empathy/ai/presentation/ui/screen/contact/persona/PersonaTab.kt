package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.dialog.TagConfirmationDialog

/**
 * 标签画像标签页组件
 *
 * 整合所有标签分类，提供标签确认/驳回功能
 *
 * 职责：
 * - 按类型分组展示标签
 * - 管理标签确认对话框
 * - 性能优化：只在可见区域启用动画
 *
 * @param riskTags 雷区标签列表
 * @param strategyTags 策略标签列表
 * @param onConfirmTag 确认标签回调
 * @param onRejectTag 驳回标签回调
 * @param modifier Modifier
 */
@Composable
fun PersonaTab(
    riskTags: List<BrainTag>,
    strategyTags: List<BrainTag>,
    onConfirmTag: (BrainTag) -> Unit,
    onRejectTag: (BrainTag) -> Unit,
    modifier: Modifier = Modifier
) {
    // 待确认的标签（用于显示对话框）
    var pendingTag by remember { mutableStateOf<BrainTag?>(null) }
    
    // 列表状态（用于性能优化）
    val listState = rememberLazyListState()
    
    // 计算可见项索引范围（用于控制动画）
    val visibleItemsInfo by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
        }
    }
    
    // 检查是否有标签
    val hasAnyTags = riskTags.isNotEmpty() || strategyTags.isNotEmpty()
    
    if (!hasAnyTags) {
        // 空状态
        EmptyPersonaView(modifier = modifier)
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            // 雷区标签分类
            if (riskTags.isNotEmpty()) {
                item(key = "risk_section") {
                    CategorySection(
                        title = "🚫 雷区标签",
                        tags = riskTags,
                        onTagClick = { tag ->
                            if (!tag.isConfirmed) {
                                pendingTag = tag
                            }
                        },
                        enableAnimation = true  // 可根据可见性优化
                    )
                }
            }
            
            // 策略标签分类
            if (strategyTags.isNotEmpty()) {
                item(key = "strategy_section") {
                    CategorySection(
                        title = "💡 策略标签",
                        tags = strategyTags,
                        onTagClick = { tag ->
                            if (!tag.isConfirmed) {
                                pendingTag = tag
                            }
                        },
                        enableAnimation = true
                    )
                }
            }
            
            // 底部说明
            item(key = "footer") {
                PersonaFooter()
            }
        }
    }
    
    // 标签确认对话框
    pendingTag?.let { tag ->
        TagConfirmationDialog(
            tag = tag,
            onConfirm = {
                onConfirmTag(tag)
                pendingTag = null
            },
            onReject = {
                onRejectTag(tag)
                pendingTag = null
            },
            onDismiss = {
                pendingTag = null
            }
        )
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyPersonaView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "暂无标签",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "与联系人互动后，AI会自动推测标签",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 底部说明
 */
@Composable
private fun PersonaFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.SpacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💡 点击带问号的标签可以确认或驳回",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}


// ========== 预览 ==========

@Preview(name = "完整标签画像", showBackground = true)
@Composable
private fun PreviewPersonaTabFull() {
    EmpathyTheme {
        PersonaTab(
            riskTags = listOf(
                BrainTag(
                    id = 1,
                    contactId = "contact_1",
                    content = "不喜欢被催促",
                    type = TagType.RISK_RED,
                    isConfirmed = true,
                    source = "manual"
                ),
                BrainTag(
                    id = 2,
                    contactId = "contact_1",
                    content = "讨厌加班话题",
                    type = TagType.RISK_RED,
                    isConfirmed = true,
                    source = "manual"
                ),
                BrainTag(
                    id = 3,
                    contactId = "contact_1",
                    content = "可能不喜欢早起",
                    type = TagType.RISK_RED,
                    isConfirmed = false,
                    source = "ai"
                )
            ),
            strategyTags = listOf(
                BrainTag(
                    id = 4,
                    contactId = "contact_1",
                    content = "喜欢收到早安问候",
                    type = TagType.STRATEGY_GREEN,
                    isConfirmed = true,
                    source = "manual"
                ),
                BrainTag(
                    id = 5,
                    contactId = "contact_1",
                    content = "可能喜欢美食话题",
                    type = TagType.STRATEGY_GREEN,
                    isConfirmed = false,
                    source = "ai"
                ),
                BrainTag(
                    id = 6,
                    contactId = "contact_1",
                    content = "可能喜欢旅行",
                    type = TagType.STRATEGY_GREEN,
                    isConfirmed = false,
                    source = "ai"
                )
            ),
            onConfirmTag = {},
            onRejectTag = {}
        )
    }
}

@Preview(name = "只有雷区标签", showBackground = true)
@Composable
private fun PreviewPersonaTabRiskOnly() {
    EmpathyTheme {
        PersonaTab(
            riskTags = listOf(
                BrainTag(
                    id = 1,
                    contactId = "contact_1",
                    content = "不喜欢被催促",
                    type = TagType.RISK_RED,
                    isConfirmed = true,
                    source = "manual"
                )
            ),
            strategyTags = emptyList(),
            onConfirmTag = {},
            onRejectTag = {}
        )
    }
}

@Preview(name = "空状态", showBackground = true)
@Composable
private fun PreviewPersonaTabEmpty() {
    EmpathyTheme {
        PersonaTab(
            riskTags = emptyList(),
            strategyTags = emptyList(),
            onConfirmTag = {},
            onRejectTag = {}
        )
    }
}
