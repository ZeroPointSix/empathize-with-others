package com.empathy.ai.presentation.ui.screen.contact.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.DataStatus
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSSystemGroupedBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.component.vault.DataSourceGrid
import com.empathy.ai.presentation.ui.component.vault.DataSourceItem
import com.empathy.ai.presentation.ui.component.vault.DataSourceTypes
import com.empathy.ai.presentation.ui.component.vault.DataStatisticsCard
import com.empathy.ai.presentation.ui.component.vault.DataStatus as VaultDataStatus

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
 * 资料库标签页组件 (iOS风格重写)
 *
 * 网格布局展示所有数据源
 *
 * 技术要点:
 * - 数据统计卡片（DataStatisticsCard）
 * - 数据来源网格（DataSourceGrid）
 * - iOS风格背景和卡片
 *
 * @param dataSources 数据源列表
 * @param onDataSourceClick 数据源点击回调
 * @param modifier Modifier
 * 
 * @see TDD-00020 8.4 DataVaultTab资料库页重写
 */
@Composable
fun DataVaultTab(
    dataSources: List<DataSourceInfo>,
    modifier: Modifier = Modifier,
    onDataSourceClick: ((DataSourceInfo) -> Unit)? = null
) {
    // 计算总数据量
    val totalCount = dataSources.sumOf { it.count }
    
    // 转换为新的数据模型
    val dataSourceItems = dataSources.map { source ->
        val config = when (source.id) {
            "chat" -> DataSourceTypes.CHAT
            "ai_summary", "summary" -> DataSourceTypes.AI_SUMMARY
            "image" -> DataSourceTypes.IMAGE
            "voice" -> DataSourceTypes.VOICE
            "video" -> DataSourceTypes.VIDEO
            "file", "note", "folder" -> DataSourceTypes.FILE
            else -> DataSourceTypes.CHAT
        }
        val vaultStatus = when (source.status) {
            DataStatus.COMPLETED -> VaultDataStatus.COMPLETED
            DataStatus.PROCESSING -> VaultDataStatus.PROCESSING
            DataStatus.NOT_AVAILABLE -> VaultDataStatus.NOT_AVAILABLE
            DataStatus.FAILED -> VaultDataStatus.FAILED
        }
        DataSourceItem(config, source.count, vaultStatus)
    }
    
    if (dataSources.isEmpty()) {
        EmptyVaultView(modifier = modifier)
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(iOSSystemGroupedBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 数据统计卡片
            DataStatisticsCard(
                totalCount = totalCount
            )
            
            // 标题
            Text(
                text = "数据来源",
                fontSize = 20.sp,
                color = iOSTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Text(
                text = "管理和查看联系人相关的各类数据",
                fontSize = 14.sp,
                color = iOSTextSecondary
            )
            
            // 数据来源网格
            DataSourceGrid(
                items = dataSourceItems,
                onItemClick = { item ->
                    val originalSource = dataSources.find { 
                        it.id == item.config.id || 
                        (it.id == "note" && item.config.id == "file")
                    }
                    if (originalSource != null) {
                        onDataSourceClick?.invoke(originalSource)
                    }
                }
            )
            
            // 底部间距
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * 空状态视图 (iOS风格)
 */
@Composable
private fun EmptyVaultView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iOSSystemGroupedBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = iOSTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "暂无数据",
                fontSize = 17.sp,
                color = iOSTextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "导入聊天记录或添加备注后，数据将显示在这里",
                fontSize = 14.sp,
                color = iOSTextSecondary,
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
