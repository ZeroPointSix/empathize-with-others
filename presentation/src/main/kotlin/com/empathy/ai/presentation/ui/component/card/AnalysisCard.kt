package com.empathy.ai.presentation.ui.component.card

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * AI分析结果卡片组件
 *
 * ## 业务职责
 * 展示AI对聊天内容的风险分析和回复建议。
 * 是AI分析功能的核心展示组件，用户可直接复制建议内容。
 *
 * ## 页面布局
 * ```
 * ┌─────────────────────────────────────┐
 * │  [✓/!] AI分析建议           [∨/^] │  ← 风险图标 + 展开按钮
 * ├─────────────────────────────────────┤
 * │  分析内容描述...                    │
 * │                                     │
 * │  建议回复：                         │
 * │  "我觉得这个想法很有趣..."         │  ← 可复制内容
 * │                          [📋复制]  │ │
 * └─────────────────────────────────────┘
 * ```
 *
 * ## 核心设计决策
 * 1. **风险等级可视化**: 使用图标（✓/!）和颜色区分风险等级
 * 2. **可展开/收起**: 卡片内容可折叠，节省屏幕空间
 * 3. **一键复制**: 内置复制按钮，快速获取建议内容
 * 4. **Material 3设计**: 使用Card组件，符合Material Design规范
 *
 * ## 风险等级对应
 * - **SAFE (安全)**: 绿色 ✓ 图标，无风险提示
 * - **WARNING (警告)**: 黄色 ! 图标，建议注意
 * - **DANGER (危险)**: 红色 ! 图标，需要谨慎
 *
 * @param analysisResult AI分析结果（包含风险等级和建议内容）
 * @param onCopyReply 复制回复建议的回调
 * @param modifier Modifier
 * @see AnalysisResult 分析结果数据模型
 * @see RiskLevel 风险等级枚举
 */
@Composable
fun AnalysisCard(
    analysisResult: AnalysisResult,
    onCopyReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    
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
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 风险等级图标
                Icon(
                    imageVector = when (analysisResult.riskLevel) {
                        RiskLevel.SAFE -> Icons.Default.CheckCircle
                        RiskLevel.WARNING, RiskLevel.DANGER -> Icons.Default.Warning
                    },
                    contentDescription = "风险等级",
                    modifier = Modifier.size(24.dp),
                    tint = getRiskColor(analysisResult.riskLevel)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 展开/收起按钮
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isExpanded) "收起" else "展开"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 可展开内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    // 话术建议
                    Text(
                        text = "话术建议",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = analysisResult.replySuggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = onCopyReply,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "复制",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 策略分析
                    Text(
                        text = "军师分析",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = analysisResult.strategyAnalysis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 获取风险等级对应的颜色
 * 使用MaterialTheme语义化颜色，自动适配深色模式
 */
@Composable
private fun getRiskColor(riskLevel: RiskLevel): Color {
    return when (riskLevel) {
        RiskLevel.SAFE -> MaterialTheme.colorScheme.tertiary // 使用tertiary表示成功/安全
        RiskLevel.WARNING -> MaterialTheme.colorScheme.secondary // 使用secondary表示警告
        RiskLevel.DANGER -> MaterialTheme.colorScheme.error // 使用error表示危险
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "安全级别", showBackground = true)
@Composable
private fun AnalysisCardSafePreview() {
    EmpathyTheme {
        AnalysisCard(
            analysisResult = AnalysisResult(
                replySuggestion = "好的，我明白了。那我们约个时间详细聊聊这个项目吧。",
                strategyAnalysis = "对方态度积极，可以进一步推进合作。建议主动提出具体的时间安排，展现你的诚意和效率。",
                riskLevel = RiskLevel.SAFE
            ),
            onCopyReply = {}
        )
    }
}

@Preview(name = "警告级别", showBackground = true)
@Composable
private fun AnalysisCardWarningPreview() {
    EmpathyTheme {
        AnalysisCard(
            analysisResult = AnalysisResult(
                replySuggestion = "我理解您的顾虑，我们可以先从小项目开始合作，建立信任。",
                strategyAnalysis = "对方表现出犹豫，可能对合作有所保留。建议降低门槛，提出试探性的合作方案，避免施加压力。",
                riskLevel = RiskLevel.WARNING
            ),
            onCopyReply = {}
        )
    }
}

@Preview(name = "危险级别", showBackground = true)
@Composable
private fun AnalysisCardDangerPreview() {
    EmpathyTheme {
        AnalysisCard(
            analysisResult = AnalysisResult(
                replySuggestion = "抱歉，我理解您现在可能不方便。我们改天再聊吧。",
                strategyAnalysis = "警告：对方明确表示不满或拒绝。建议立即停止当前话题，给对方空间。不要继续追问或解释，这会让情况更糟。",
                riskLevel = RiskLevel.DANGER
            ),
            onCopyReply = {}
        )
    }
}

@Preview(name = "长文本", showBackground = true)
@Composable
private fun AnalysisCardLongTextPreview() {
    EmpathyTheme {
        AnalysisCard(
            analysisResult = AnalysisResult(
                replySuggestion = "王总，我完全理解您的考虑。这个项目确实需要仔细评估。我们可以先安排一次详细的技术交流会，让您的团队充分了解我们的方案和实施计划。同时，我们也准备了几个类似项目的成功案例，可以给您做参考。您看什么时间方便？",
                strategyAnalysis = "对方是决策者，非常谨慎。他需要看到具体的价值和风险控制。建议：1) 提供详细的技术方案和案例支持；2) 展现专业性和可靠性；3) 给予充分的决策时间；4) 避免过度推销，让对方感受到尊重。记住，王总最看重的是长期价值和风险可控。",
                riskLevel = RiskLevel.SAFE
            ),
            onCopyReply = {}
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnalysisCardDarkPreview() {
    EmpathyTheme {
        AnalysisCard(
            analysisResult = AnalysisResult(
                replySuggestion = "好的，我明白了。那我们约个时间详细聊聊这个项目吧。",
                strategyAnalysis = "对方态度积极，可以进一步推进合作。建议主动提出具体的时间安排，展现你的诚意和效率。",
                riskLevel = RiskLevel.SAFE
            ),
            onCopyReply = {}
        )
    }
}
