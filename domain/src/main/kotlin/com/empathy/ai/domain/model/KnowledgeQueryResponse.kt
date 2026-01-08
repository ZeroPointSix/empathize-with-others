package com.empathy.ai.domain.model

/**
 * 知识查询响应
 *
 * 承载知识查询的AI返回结果，支持Markdown格式内容和相关推荐。
 *
 * 业务背景 (PRD-00031):
 * - 在悬浮窗内展示Markdown格式的知识解释
 * - 显示信息来源（联网获取时显示）
 * - 推荐相关的知识话题或延伸阅读（3-5条）
 *
 * @property title 解释标题（可选，自动提取或生成）
 * @property content 解释内容（Markdown格式）
 * @property source 信息来源（联网获取时显示）
 * @property sourceTime 信息时间（联网获取时显示）
 * @property isFromNetwork 是否来自联网搜索
 * @property recommendations 相关推荐列表
 *
 * @see PRD-00031 悬浮窗快速知识回答功能需求
 * @see TDD-00031 悬浮窗快速知识回答功能技术设计
 */
data class KnowledgeQueryResponse(
    val title: String? = null,
    val content: String,
    val source: String? = null,
    val sourceTime: String? = null,
    val isFromNetwork: Boolean = false,
    val recommendations: List<Recommendation> = emptyList()
) {
    /**
     * 获取用于复制的文本
     *
     * @return 纯文本内容，不包含来源信息
     */
    fun getCopyableText(): String = content
    
    /**
     * 获取用于显示的完整内容
     *
     * @return 格式化的显示内容，包含标题和来源
     */
    fun getDisplayContent(): String = buildString {
        if (!title.isNullOrBlank()) {
            appendLine("## $title")
            appendLine()
        }
        append(content)
        if (!source.isNullOrBlank()) {
            appendLine()
            appendLine()
            append("📅 来源：$source")
            if (!sourceTime.isNullOrBlank()) {
                append(" • $sourceTime")
            }
        }
    }
    
    /**
     * 是否有推荐内容
     *
     * @return true 如果有推荐项
     */
    fun hasRecommendations(): Boolean = recommendations.isNotEmpty()
    
    /**
     * 获取来源标签文本
     *
     * @return 格式化的来源标签
     */
    fun getSourceLabel(): String = when {
        isFromNetwork && !source.isNullOrBlank() -> "来源：$source"
        isFromNetwork -> "来源：联网搜索"
        else -> "来源：AI知识库"
    }
    
    /**
     * 获取来源图标
     *
     * @return 来源对应的图标emoji
     */
    fun getSourceIcon(): String = if (isFromNetwork) "🌐" else "🤖"
    
    /**
     * 内容是否为空
     *
     * @return true 如果内容为空或仅包含空白字符
     */
    fun isEmpty(): Boolean = content.isBlank()
    
    companion object {
        /**
         * 创建空响应（用于错误情况）
         *
         * @return 空的知识查询响应
         */
        fun empty(): KnowledgeQueryResponse = KnowledgeQueryResponse(
            title = null,
            content = "",
            source = null,
            sourceTime = null,
            isFromNetwork = false,
            recommendations = emptyList()
        )
        
        /**
         * 创建AI兜底响应
         *
         * @param content 响应内容
         * @param title 可选的标题
         * @param recommendations 可选的推荐列表
         * @return AI本地知识生成的响应
         */
        fun fromAiFallback(
            content: String, 
            title: String? = null,
            recommendations: List<Recommendation> = emptyList()
        ): KnowledgeQueryResponse = KnowledgeQueryResponse(
            title = title,
            content = content,
            source = "AI知识库",
            sourceTime = null,
            isFromNetwork = false,
            recommendations = recommendations
        )
        
        /**
         * 创建联网搜索响应
         *
         * @param content 响应内容
         * @param title 可选的标题
         * @param source 信息来源
         * @param sourceTime 信息时间
         * @param recommendations 可选的推荐列表
         * @return 联网搜索生成的响应
         */
        fun fromNetworkSearch(
            content: String,
            title: String? = null,
            source: String? = null,
            sourceTime: String? = null,
            recommendations: List<Recommendation> = emptyList()
        ): KnowledgeQueryResponse = KnowledgeQueryResponse(
            title = title,
            content = content,
            source = source,
            sourceTime = sourceTime,
            isFromNetwork = true,
            recommendations = recommendations
        )
    }
}
