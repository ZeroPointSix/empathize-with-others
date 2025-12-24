package com.empathy.ai.presentation.ui.screen.contact.vault

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.DataStatus
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 数据源信息
 */
data class DataSourceInfo(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val count: Int,
    val status: DataStatus
)

/**
 * 资料库标签页组件
 *
 * 网格布局展示所有数据源
 *
 * 职责：
 * - 2列网格布局
 * - 展示各类数据源卡片
 * - 处理数据源点击导航
 *
 * @param dataSources 数据源列表
 * @param onDataSourceClick 数据源点击回调
 * @param modifier Modifier
 */
@Composable
fun DataVaultTab(
    dataSources: List<DataSourceInfo>,
    modifier: Modifier = Modifier,
    onDataSourceClick: ((DataSourceInfo) -> Unit)? = null
) {
    if (dataSources.isEmpty()) {
        EmptyVaultView(modifier = modifier)
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            // 标题说明
            Text(
                text = "数据来源",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    horizontal = Dimensions.SpacingMedium,
                    vertical = Dimensions.SpacingSmall
                )
            )
            
            Text(
                text = "管理和查看联系人相关的各类数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = Dimensions.SpacingMedium,
                    vertical = Dimensions.SpacingSmall
                )
            )
            
            // 网格布局
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimensions.SpacingMedium),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                items(
                    items = dataSources,
                    key = { it.id }
                ) { dataSource ->
                    DataSourceCard(
                        title = dataSource.title,
                        icon = dataSource.icon,
                        count = dataSource.count,
                        status = dataSource.status,
                        onClick = { onDataSourceClick?.invoke(dataSource) }
                    )
                }
            }
        }
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyVaultView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "导入聊天记录或添加备注后，数据将显示在这里",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

// ========== 预览 ==========

@Preview(name = "完整资料库", showBackground = true)
@Composable
private fun PreviewDataVaultTabFull() {
    EmpathyTheme {
        DataVaultTab(
            dataSources = listOf(
                DataSourceInfo(
                    id = "chat",
                    title = "聊天记录",
                    icon = Icons.Default.Chat,
                    count = 128,
                    status = DataStatus.COMPLETED
                ),
                DataSourceInfo(
                    id = "image",
                    title = "图片",
                    icon = Icons.Default.Image,
                    count = 45,
                    status = DataStatus.PROCESSING
                ),
                DataSourceInfo(
                    id = "voice",
                    title = "语音消息",
                    icon = Icons.Default.Mic,
                    count = 0,
                    status = DataStatus.NOT_AVAILABLE
                ),
                DataSourceInfo(
                    id = "video",
                    title = "视频",
                    icon = Icons.Default.VideoFile,
                    count = 3,
                    status = DataStatus.FAILED
                ),
                DataSourceInfo(
                    id = "note",
                    title = "备注",
                    icon = Icons.Default.Note,
                    count = 12,
                    status = DataStatus.COMPLETED
                )
            )
        )
    }
}

@Preview(name = "空资料库", showBackground = true)
@Composable
private fun PreviewDataVaultTabEmpty() {
    EmpathyTheme {
        DataVaultTab(dataSources = emptyList())
    }
}
