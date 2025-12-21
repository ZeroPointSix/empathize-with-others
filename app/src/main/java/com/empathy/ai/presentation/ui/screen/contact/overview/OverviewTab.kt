package com.empathy.ai.presentation.ui.screen.contact.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.EmotionalBackground

/**
 * 概览标签页组件
 *
 * 联系人详情页的第一个界面，展示关系概况
 *
 * 职责：
 * - 整合动态情感头部、核心标签、最新动态
 * - 提供情感化的背景
 * - 支持滚动和Sticky Header效果
 * - TD-00012: 支持联系人信息编辑
 *
 * @param contact 联系人信息
 * @param topTags 核心标签列表
 * @param latestFact 最新事实记录
 * @param daysSinceFirstMet 相识天数
 * @param onBackClick 返回按钮点击回调
 * @param onViewFactStream 查看事实流回调
 * @param onEditContactInfo 编辑联系人信息回调（TD-00012）
 * @param modifier Modifier
 */
@Composable
fun OverviewTab(
    contact: ContactProfile,
    topTags: List<Fact>,
    latestFact: Fact?,
    daysSinceFirstMet: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onViewFactStream: (() -> Unit)? = null,
    onEditCustomPrompt: (() -> Unit)? = null,
    onEditContactInfo: (() -> Unit)? = null
) {
    val scrollState = rememberLazyListState()
    
    Box(modifier = modifier.fillMaxSize()) {
        // 情感化背景
        EmotionalBackground(relationshipScore = contact.relationshipScore)
        
        // 内容
        Column(modifier = Modifier.fillMaxSize()) {
            // 动态情感头部（Sticky Header）
            DynamicEmotionalHeader(
                contact = contact,
                scrollState = scrollState,
                daysSinceFirstMet = daysSinceFirstMet,
                onBackClick = onBackClick,
                onEditClick = onEditContactInfo
            )
            
            // 可滚动内容
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize()
            ) {
                // 关系分数卡片
                item {
                    RelationshipScoreCard(
                        score = contact.relationshipScore,
                        modifier = Modifier.padding(Dimensions.SpacingMedium)
                    )
                }
                
                // 核心标签速览
                item {
                    TopTagsSection(tags = topTags)
                }
                
                // 最新动态卡片
                item {
                    LatestFactHookCard(
                        latestFact = latestFact,
                        onViewMore = onViewFactStream
                    )
                }
                
                // 专属指令卡片
                if (onEditCustomPrompt != null) {
                    item {
                        CustomPromptCard(
                            contactName = contact.name,
                            onEditClick = onEditCustomPrompt,
                            modifier = Modifier.padding(Dimensions.SpacingMedium)
                        )
                    }
                }
                
                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

/**
 * 关系分数卡片
 *
 * 展示当前关系分数和状态描述
 */
@Composable
private fun RelationshipScoreCard(
    score: Int,
    modifier: Modifier = Modifier
) {
    com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SpacingMedium)
        ) {
            Text(
                text = "关系健康度",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = getScoreDescription(score),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 根据分数获取描述文本
 */
private fun getScoreDescription(score: Int): String {
    return when {
        score >= 81 -> "关系非常亲密，继续保持！"
        score >= 61 -> "关系良好，有进一步发展的空间"
        score >= 31 -> "关系一般，需要更多互动"
        else -> "关系较冷淡，建议主动联系"
    }
}

/**
 * 专属指令卡片
 *
 * 允许用户为特定联系人设置专属的AI指令
 */
@Composable
private fun CustomPromptCard(
    contactName: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "专属指令",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "为 $contactName 设置专属的AI分析指令",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            TextButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("编辑")
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "完整概览页", showBackground = true)
@Composable
private fun PreviewOverviewTab() {
    EmpathyTheme {
        OverviewTab(
            contact = ContactProfile(
                id = "1",
                name = "小明",
                targetGoal = "建立良好关系",
                avatarUrl = "",
                relationshipScore = 85
            ),
            topTags = listOf(
                Fact(
                    key = "兴趣爱好",
                    value = "喜欢吃辣",
                    source = FactSource.MANUAL,
                    timestamp = 1L
                ),
                Fact(
                    key = "兴趣爱好",
                    value = "猫奴",
                    source = FactSource.MANUAL,
                    timestamp = 2L
                ),
                Fact(
                    key = "性格特征",
                    value = "工作狂",
                    source = FactSource.AI_INFERRED,
                    timestamp = 3L
                )
            ),
            latestFact = Fact(
                key = "兴趣爱好",
                value = "喜欢周末去爬山",
                source = FactSource.AI_INFERRED,
                timestamp = System.currentTimeMillis()
            ),
            daysSinceFirstMet = 105,
            onBackClick = {}
        )
    }
}

@Preview(name = "空数据概览页", showBackground = true)
@Composable
private fun PreviewOverviewTabEmpty() {
    EmpathyTheme {
        OverviewTab(
            contact = ContactProfile(
                id = "2",
                name = "新朋友",
                targetGoal = "认识新朋友",
                avatarUrl = "",
                relationshipScore = 50
            ),
            topTags = emptyList(),
            latestFact = null,
            daysSinceFirstMet = 1,
            onBackClick = {}
        )
    }
}

@Preview(name = "低分关系概览页", showBackground = true)
@Composable
private fun PreviewOverviewTabLowScore() {
    EmpathyTheme {
        OverviewTab(
            contact = ContactProfile(
                id = "3",
                name = "老同学",
                targetGoal = "重建联系",
                avatarUrl = "",
                relationshipScore = 25
            ),
            topTags = listOf(
                Fact(
                    key = "禁忌话题",
                    value = "不要提工作",
                    source = FactSource.MANUAL,
                    timestamp = 1L
                )
            ),
            latestFact = null,
            daysSinceFirstMet = 365,
            onBackClick = {}
        )
    }
}
