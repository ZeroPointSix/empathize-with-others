package com.empathy.ai.presentation.ui.component.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.R
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.LocalSemanticColors

/**
 * 已确认标签组件
 *
 * 显示用户已确认的标签，带有实心背景和对勾图标
 *
 * 设计特点：
 * - 实心背景色（根据标签类型）
 * - 白色文字和图标
 * - 对勾图标表示已确认状态
 * - 圆角矩形形状
 *
 * @param tag 标签数据
 * @param onClick 点击回调
 * @param modifier Modifier
 */
@Composable
fun ConfirmedTag(
    tag: BrainTag,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val semanticColors = LocalSemanticColors.current
    val backgroundColor = when (tag.type) {
        TagType.RISK_RED -> semanticColors.riskRed
        TagType.STRATEGY_GREEN -> semanticColors.strategyGreen
    }
    
    // 无障碍描述
    val accessibilityDescription = stringResource(R.string.cd_tag_confirmed, tag.content)
    
    Row(
        modifier = modifier
            .semantics { contentDescription = accessibilityDescription }
            .clip(RoundedCornerShape(Dimensions.CornerRadiusMedium))
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 对勾图标
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "已确认",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        
        // 标签文字
        Text(
            text = tag.content,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}


// ========== 预览 ==========

@Preview(name = "雷区标签", showBackground = true)
@Composable
private fun PreviewConfirmedTagRisk() {
    EmpathyTheme {
        ConfirmedTag(
            tag = BrainTag(
                id = 1,
                contactId = "contact_1",
                content = "不喜欢被催促",
                type = TagType.RISK_RED,
                isConfirmed = true,
                source = "manual"
            )
        )
    }
}

@Preview(name = "策略标签", showBackground = true)
@Composable
private fun PreviewConfirmedTagStrategy() {
    EmpathyTheme {
        ConfirmedTag(
            tag = BrainTag(
                id = 2,
                contactId = "contact_1",
                content = "喜欢收到早安问候",
                type = TagType.STRATEGY_GREEN,
                isConfirmed = true,
                source = "manual"
            )
        )
    }
}
