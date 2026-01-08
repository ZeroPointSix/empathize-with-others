package com.empathy.ai.domain.model

/**
 * 知识查询请求
 *
 * 承载知识查询的请求参数，支持内容验证和截断处理。
 *
 * 业务背景 (PRD-00031):
 * - 悬浮窗新增第4个Tab"快速问答"
 * - 支持用户粘贴/输入需要查询的内容
 * - 限制最大500字符，防止过长输入
 *
 * @property content 用户输入的查询内容
 * @property maxLength 最大长度限制（默认500字符）
 * @property enableNetwork 是否启用联网搜索（默认true）
 * @property timestamp 请求时间戳
 *
 * @see PRD-00031 悬浮窗快速知识回答功能需求
 * @see TDD-00031 悬浮窗快速知识回答功能技术设计
 */
data class KnowledgeQueryRequest(
    val content: String,
    val maxLength: Int = MAX_CONTENT_LENGTH,
    val enableNetwork: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /** 默认最大内容长度 */
        const val MAX_CONTENT_LENGTH = 500
        
        /** 摘要显示的最大长度 */
        private const val SUMMARY_LENGTH = 50
    }
    
    /**
     * 验证请求是否有效
     *
     * @return true 如果内容非空且长度在限制内
     */
    fun isValid(): Boolean = content.isNotBlank() && content.length <= maxLength
    
    /**
     * 获取截断后的内容（如果超出限制）
     *
     * @return 截断后的内容，最多maxLength个字符
     */
    fun getTruncatedContent(): String = 
        if (content.length > maxLength) content.take(maxLength) else content
    
    /**
     * 获取清理后的内容（去除首尾空白）
     *
     * @return 清理后的内容
     */
    fun getCleanedContent(): String = getTruncatedContent().trim()
    
    /**
     * 获取查询内容的摘要（用于日志）
     *
     * @return 内容摘要，超过50字符时显示省略号
     */
    fun getSummary(): String = 
        if (content.length > SUMMARY_LENGTH) "${content.take(SUMMARY_LENGTH)}..." else content
    
    /**
     * 获取验证错误信息
     *
     * @return 错误信息，如果验证通过则返回null
     */
    fun getValidationError(): String? = when {
        content.isBlank() -> "查询内容不能为空"
        content.length > maxLength -> "查询内容超出${maxLength}字符限制"
        else -> null
    }
}
