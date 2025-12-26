package com.empathy.ai.presentation.ui.component.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSIndigo
import com.empathy.ai.presentation.theme.iOSOrange
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSRed


/**
 * 数据来源配置
 * 
 * @param id 唯一标识
 * @param icon 图标
 * @param title 标题
 * @param subtitle 副标题
 * @param iconColor 图标颜色
 */
data class DataSourceConfig(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val iconColor: Color
)

/**
 * 预定义的数据来源类型
 * 
 * 设计原则 (Material Design 3 糖果色系统):
 * - 每个功能入口使用专属的高饱和度颜色作为视觉锚点
 * - 高饱和度背景色 + 纯白图标 (Solid Color Container + White Icon)
 * - 色彩表达语义，增强信息层级区分度
 */
object DataSourceTypes {
    /** 聊天记录 - 主色蓝，代表沟通和信息流 */
    val CHAT = DataSourceConfig(
        id = "chat",
        icon = Icons.Default.Chat,
        title = "聊天记录",
        subtitle = "文字消息",
        iconColor = iOSBlue
    )
    
    /** AI总结 - 紫色，代表智能和创意 */
    val AI_SUMMARY = DataSourceConfig(
        id = "ai_summary",
        icon = Icons.Default.Psychology,
        title = "AI总结",
        subtitle = "智能分析",
        iconColor = iOSPurple
    )
    
    /** 图片 - 紫色/靛青色，代表视觉内容 */
    val IMAGE = DataSourceConfig(
        id = "image",
        icon = Icons.Default.Image,
        title = "图片",
        subtitle = "照片和截图",
        iconColor = iOSIndigo
    )
    
    /** 语音 - 湖绿色，活跃的媒体类型 */
    val VOICE = DataSourceConfig(
        id = "voice",
        icon = Icons.Default.Mic,
        title = "语音",
        subtitle = "语音消息",
        iconColor = iOSGreen
    )
    
    /** 视频 - 红色，代表录制/播放，视觉重心 */
    val VIDEO = DataSourceConfig(
        id = "video",
        icon = Icons.Default.Videocam,
        title = "视频",
        subtitle = "视频消息",
        iconColor = iOSRed
    )
    
    /** 文件 - 橙色，代表文档和附件 */
    val FILE = DataSourceConfig(
        id = "file",
        icon = Icons.Default.Description,
        title = "文件",
        subtitle = "文档和附件",
        iconColor = iOSOrange
    )
    
    val ALL = listOf(CHAT, AI_SUMMARY, IMAGE, VOICE, VIDEO, FILE)
}

/**
 * 数据来源项数据
 */
data class DataSourceItem(
    val config: DataSourceConfig,
    val count: Int,
    val status: DataStatus,
    val lastUpdated: String? = null
)

/**
 * 数据来源网格组件
 * 
 * 技术要点:
 * - 6种数据来源：聊天记录、AI总结、图片、语音、视频、文件
 * - 2列网格布局，使用chunked(2)分组
 * - 支持各数据源的数量和状态
 * 
 * @param items 数据来源项列表
 * @param onItemClick 项目点击回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 6.3 DataSourceGrid数据网格
 */
@Composable
fun DataSourceGrid(
    items: List<DataSourceItem>,
    onItemClick: (DataSourceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2列网格布局
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    DataSourceGridCard(
                        icon = item.config.icon,
                        title = item.config.title,
                        subtitle = item.lastUpdated ?: item.config.subtitle,
                        count = item.count,
                        status = item.status,
                        iconBackgroundColor = item.config.iconColor,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 如果是奇数个，添加空白占位
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(name = "数据来源网格", showBackground = true)
@Composable
private fun DataSourceGridPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DataSourceGrid(
                items = listOf(
                    DataSourceItem(DataSourceTypes.CHAT, 256, DataStatus.COMPLETED, "今天"),
                    DataSourceItem(DataSourceTypes.AI_SUMMARY, 12, DataStatus.COMPLETED, "昨天"),
                    DataSourceItem(DataSourceTypes.IMAGE, 89, DataStatus.PROCESSING),
                    DataSourceItem(DataSourceTypes.VOICE, 0, DataStatus.NOT_AVAILABLE),
                    DataSourceItem(DataSourceTypes.VIDEO, 0, DataStatus.NOT_AVAILABLE),
                    DataSourceItem(DataSourceTypes.FILE, 5, DataStatus.COMPLETED)
                ),
                onItemClick = {}
            )
        }
    }
}
