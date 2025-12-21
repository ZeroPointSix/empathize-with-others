package com.empathy.ai.presentation.ui.screen.contact.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.AsyncImage
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.RelationshipColors

/**
 * 动态情感头部组件
 *
 * 实现Sticky Header效果，随滚动平滑收缩
 *
 * 职责：
 * - 展示联系人头像、姓名、相识时长
 * - 根据滚动位置动态调整高度和元素大小
 * - 背景光晕根据关系分数变化
 * - TD-00012: 支持编辑联系人信息
 *
 * 技术要点：
 * - 使用derivedStateOf优化滚动监听，减少重组
 * - 使用lerp实现平滑的尺寸过渡
 * - 背景使用垂直渐变，与EmotionalBackground协调
 *
 * @param contact 联系人信息
 * @param scrollState 滚动状态，用于计算收缩进度
 * @param daysSinceFirstMet 相识天数
 * @param onBackClick 返回按钮点击回调
 * @param onEditClick 编辑按钮点击回调（TD-00012）
 * @param modifier Modifier
 */
@Composable
fun DynamicEmotionalHeader(
    contact: ContactProfile,
    scrollState: LazyListState,
    daysSinceFirstMet: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: (() -> Unit)? = null
) {
    // 计算滚动进度 (0.0 ~ 1.0)
    val scrollProgress by remember {
        derivedStateOf {
            val offset = scrollState.firstVisibleItemScrollOffset.toFloat()
            val maxScroll = 200f
            (offset / maxScroll).coerceIn(0f, 1f)
        }
    }
    
    // 动态计算头部高度
    val headerHeight by remember {
        derivedStateOf {
            lerp(
                start = Dimensions.HeaderHeightExpanded,
                stop = Dimensions.HeaderHeightCollapsed,
                fraction = scrollProgress
            )
        }
    }
    
    // 动态计算头像尺寸
    val avatarSize by remember {
        derivedStateOf {
            lerp(
                start = Dimensions.AvatarSizeLarge,
                stop = Dimensions.AvatarSizeSmall,
                fraction = scrollProgress
            )
        }
    }
    
    // 动态计算背景透明度
    val backgroundAlpha by remember {
        derivedStateOf {
            1f - scrollProgress
        }
    }
    
    // 获取情感化颜色
    val emotionalColors = remember(contact.relationshipScore) {
        RelationshipColors.getColorsByScore(contact.relationshipScore)
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(
                Brush.verticalGradient(
                    colors = emotionalColors.map { it.copy(alpha = backgroundAlpha) }
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            // 返回按钮
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            
            // 头像
            AsyncImage(
                model = contact.avatarUrl?.ifEmpty { null },
                contentDescription = "${contact.name}的头像",
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
            
            // 姓名和相识时长
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = contact.name,
                    style = if (scrollProgress < 0.5f) {
                        MaterialTheme.typography.headlineMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // 相识时长（收缩时隐藏）
                if (scrollProgress < 0.7f) {
                    Text(
                        text = "已相识 $daysSinceFirstMet 天",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f * (1f - scrollProgress))
                    )
                }
            }
            
            // TD-00012: 编辑按钮
            if (onEditClick != null) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑联系人信息",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "展开状态", showBackground = true)
@Composable
private fun PreviewDynamicEmotionalHeaderExpanded() {
    EmpathyTheme {
        DynamicEmotionalHeader(
            contact = ContactProfile(
                id = "1",
                name = "小明",
                targetGoal = "建立良好关系",
                avatarUrl = "",
                relationshipScore = 85
            ),
            scrollState = rememberLazyListState(),
            daysSinceFirstMet = 105,
            onBackClick = {}
        )
    }
}

@Preview(name = "良好关系", showBackground = true)
@Composable
private fun PreviewDynamicEmotionalHeaderGood() {
    EmpathyTheme {
        DynamicEmotionalHeader(
            contact = ContactProfile(
                id = "2",
                name = "小红",
                targetGoal = "保持友好联系",
                avatarUrl = "",
                relationshipScore = 70
            ),
            scrollState = rememberLazyListState(),
            daysSinceFirstMet = 50,
            onBackClick = {}
        )
    }
}
