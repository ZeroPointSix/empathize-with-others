package com.empathy.ai.presentation.ui.component.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard

/**
 * 图文时刻卡片组件
 *
 * 展示照片和简短描述，类似朋友圈动态
 *
 * @param item 图文时刻数据
 * @param onClick 点击回调
 * @param modifier Modifier
 */
@Composable
fun PhotoMomentCard(
    item: TimelineItem.PhotoMoment,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column {
            // 图片
            AsyncImage(
                model = item.photoUrl,
                contentDescription = item.description,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )
            
            // 描述
            Column(modifier = Modifier.padding(Dimensions.SpacingMedium)) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 标签
                if (item.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.tags.joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "图文卡片", showBackground = true)
@Composable
private fun PreviewPhotoMomentCard() {
    EmpathyTheme {
        PhotoMomentCard(
            item = TimelineItem.PhotoMoment(
                id = "1",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.DATE,
                photoUrl = "",
                description = "今天一起去了新开的咖啡店，环境很不错",
                tags = listOf("约会", "咖啡")
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
