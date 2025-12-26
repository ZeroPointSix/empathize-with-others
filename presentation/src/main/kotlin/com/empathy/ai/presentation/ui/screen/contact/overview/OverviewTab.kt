package com.empathy.ai.presentation.ui.screen.contact.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSSystemGroupedBackground
import com.empathy.ai.presentation.ui.component.overview.AiPromptSettingsRow
import com.empathy.ai.presentation.ui.component.overview.HealthScoreCardV2
import com.empathy.ai.presentation.ui.component.overview.IdentityCard
import com.empathy.ai.presentation.ui.component.overview.LatestDiscoveryCard
import com.empathy.ai.presentation.ui.component.overview.PastelTagsCard

/**
 * 概览标签页组件 (iOS风格重写 V2)
 *
 * 设计原则：
 * 1. 整齐悬浮的白色圆角卡片列表
 * 2. 身份名片卡：头像居中+白色描边+双层投影
 * 3. Apple Fitness风格关系健康度圆环
 * 4. 糖果色标签胶囊
 * 5. iOS通知风格最新发现卡片
 * 6. 设置项风格AI指令入口
 *
 * @param contact 联系人信息
 * @param topTags 核心标签列表
 * @param latestFact 最新事实记录
 * @param daysSinceFirstMet 相识天数
 * @param onBackClick 返回按钮点击回调
 * @param onViewFactStream 查看事实流回调
 * @param onEditCustomPrompt 编辑专属指令回调
 * @param onEditContactInfo 编辑联系人信息回调
 * @param onTopicClick 主题设置回调
 * @param trendData 趋势数据（最近7天）
 * @param modifier Modifier
 * 
 * @see TDD-00020 8.1 OverviewTab概览页重写
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
    onEditContactInfo: (() -> Unit)? = null,
    onTopicClick: (() -> Unit)? = null,
    trendData: List<Float> = emptyList()
) {
    val scrollState = rememberLazyListState()
    
    LazyColumn(
        state = scrollState,
        modifier = modifier
            .fillMaxSize()
            .background(iOSSystemGroupedBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部间距
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 1. 身份名片卡
        item {
            IdentityCard(
                contact = contact,
                daysSinceFirstMet = daysSinceFirstMet,
                onEditClick = onEditContactInfo
            )
        }
        
        // 2. Apple Fitness风格关系健康度卡片
        item {
            HealthScoreCardV2(
                score = contact.relationshipScore,
                trendData = trendData
            )
        }
        
        // 3. 糖果色核心标签卡片
        if (topTags.isNotEmpty()) {
            item {
                PastelTagsCard(tags = topTags)
            }
        }
        
        // 4. iOS通知风格最新发现卡片
        item {
            LatestDiscoveryCard(
                latestFact = latestFact,
                onViewMore = onViewFactStream
            )
        }
        
        // 5. AI指令设置行
        if (onEditCustomPrompt != null) {
            item {
                AiPromptSettingsRow(onClick = onEditCustomPrompt)
            }
        }
        
        // 底部间距
        item {
            Spacer(modifier = Modifier.height(100.dp))
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
                    key = "性格特点",
                    value = "开朗外向",
                    source = FactSource.MANUAL,
                    timestamp = 2L
                ),
                Fact(
                    key = "工作信息",
                    value = "程序员",
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
            onBackClick = {},
            onEditCustomPrompt = {},
            onEditContactInfo = {},
            trendData = listOf(80f, 82f, 78f, 85f, 83f, 86f, 85f)
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
            onBackClick = {},
            trendData = listOf(48f, 50f, 52f, 49f, 51f, 50f, 50f)
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
            onBackClick = {},
            onEditCustomPrompt = {},
            trendData = listOf(30f, 28f, 25f, 22f, 24f, 26f, 25f)
        )
    }
}
