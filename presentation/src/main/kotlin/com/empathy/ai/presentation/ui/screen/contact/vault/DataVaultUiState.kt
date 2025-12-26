package com.empathy.ai.presentation.ui.screen.contact.vault

import com.empathy.ai.presentation.ui.component.vault.DataSourceItem
import com.empathy.ai.presentation.ui.component.vault.DataSourceTypes
import com.empathy.ai.presentation.ui.component.vault.DataStatus

/**
 * 数据源类型枚举
 */
enum class DataSourceType(val displayName: String) {
    CHAT("聊天记录"),
    SUMMARY("AI总结"),
    IMAGE("图片"),
    AUDIO("语音"),
    VIDEO("视频"),
    FILE("文件")
}

/**
 * 资料库页面UI状态
 * 
 * @param totalCount 数据总量
 * @param chatCount 聊天记录数量
 * @param summaryCount AI总结数量
 * @param imageCount 图片数量
 * @param audioCount 语音数量
 * @param videoCount 视频数量
 * @param fileCount 文件数量
 * @param chatStatus 聊天记录状态
 * @param summaryStatus AI总结状态
 * @param imageStatus 图片状态
 * @param audioStatus 语音状态
 * @param videoStatus 视频状态
 * @param fileStatus 文件状态
 * @param isLoading 是否加载中
 * @param error 错误信息
 * 
 * @see TDD-00020 8.4 DataVaultTab状态管理
 */
data class DataVaultUiState(
    val totalCount: Int = 0,
    val chatCount: Int = 0,
    val summaryCount: Int = 0,
    val imageCount: Int = 0,
    val audioCount: Int = 0,
    val videoCount: Int = 0,
    val fileCount: Int = 0,
    val chatStatus: DataStatus = DataStatus.COMPLETED,
    val summaryStatus: DataStatus = DataStatus.COMPLETED,
    val imageStatus: DataStatus = DataStatus.COMPLETED,
    val audioStatus: DataStatus = DataStatus.COMPLETED,
    val videoStatus: DataStatus = DataStatus.COMPLETED,
    val fileStatus: DataStatus = DataStatus.COMPLETED,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 计算总数据量
     */
    val calculatedTotal: Int
        get() = chatCount + summaryCount + imageCount + audioCount + videoCount + fileCount
    
    /**
     * 是否有错误
     */
    val hasError: Boolean
        get() = error != null
    
    /**
     * 聊天记录是否可用
     */
    val isChatAvailable: Boolean
        get() = chatStatus == DataStatus.COMPLETED || chatStatus == DataStatus.PROCESSING
    
    /**
     * 格式化的总数量显示
     */
    val formattedTotalCount: String
        get() = formatCount(totalCount)
    
    /**
     * 获取数据来源项列表
     */
    fun getDataSourceItems(): List<DataSourceItem> = listOf(
        DataSourceItem(DataSourceTypes.CHAT, chatCount, chatStatus),
        DataSourceItem(DataSourceTypes.AI_SUMMARY, summaryCount, summaryStatus),
        DataSourceItem(DataSourceTypes.IMAGE, imageCount, imageStatus),
        DataSourceItem(DataSourceTypes.VOICE, audioCount, audioStatus),
        DataSourceItem(DataSourceTypes.VIDEO, videoCount, videoStatus),
        DataSourceItem(DataSourceTypes.FILE, fileCount, fileStatus)
    )
    
    /**
     * 格式化数量显示
     */
    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
}
