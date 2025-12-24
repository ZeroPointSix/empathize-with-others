package com.empathy.ai.presentation.ui.screen.contact.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.DataStatus
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.state.StatusBadge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VideoFile

/**
 * 数据源卡片组件
 *
 * 用于资料库网格展示单个数据源
 *
 * 设计特点：
 * - 正方形卡片（1:1比例）
 * - 居中图标
 * - 底部标题和数量
 * - 右上角状态角标
 *
 * @param title 数据源标题
 * @param icon 数据源图标
 * @param count 数据数量
 * @param status 数据状态
 * @param onClick 点击回调
 * @param modifier Modifier
 */
@Composable
fun DataSourceCard(
    title: String,
    icon: ImageVector,
    count: Int,
    status: DataStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(Dimensions.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 主内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimensions.SpacingMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                // 图标
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 数量
                Text(
                    text = "${count}条",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // 状态角标（右上角）
            StatusBadge(
                status = status,
                size = 20.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
            )
        }
    }
}

// ========== 预览 ==========

@Preview(name = "聊天记录", showBackground = true)
@Composable
private fun PreviewDataSourceCardChat() {
    EmpathyTheme {
        DataSourceCard(
            title = "聊天记录",
            icon = Icons.Default.Chat,
            count = 128,
            status = DataStatus.COMPLETED,
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
        )
    }
}

@Preview(name = "图片", showBackground = true)
@Composable
private fun PreviewDataSourceCardImage() {
    EmpathyTheme {
        DataSourceCard(
            title = "图片",
            icon = Icons.Default.Image,
            count = 45,
            status = DataStatus.PROCESSING,
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
        )
    }
}

@Preview(name = "语音", showBackground = true)
@Composable
private fun PreviewDataSourceCardVoice() {
    EmpathyTheme {
        DataSourceCard(
            title = "语音消息",
            icon = Icons.Default.Mic,
            count = 0,
            status = DataStatus.NOT_AVAILABLE,
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
        )
    }
}

@Preview(name = "视频", showBackground = true)
@Composable
private fun PreviewDataSourceCardVideo() {
    EmpathyTheme {
        DataSourceCard(
            title = "视频",
            icon = Icons.Default.VideoFile,
            count = 3,
            status = DataStatus.FAILED,
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
        )
    }
}
