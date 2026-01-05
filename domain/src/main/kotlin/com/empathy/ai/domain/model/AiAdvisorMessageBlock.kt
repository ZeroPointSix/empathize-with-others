package com.empathy.ai.domain.model

import java.util.UUID

/**
 * AI军师消息块
 *
 * 一条消息可以包含多个块（思考+文本），支持复杂内容组合。
 * 参考Cherry Studio的Block-based消息架构设计。
 *
 * 业务背景 (FD-00028):
 * - Block架构支持思考过程展示（DeepSeek R1等模型）
 * - 每个Block独立状态管理，便于流式更新
 * - 支持智能节流更新，减少数据库写入频率
 *
 * 设计决策 (TDD-00028):
 * - 使用UUID作为主键，避免分布式环境下的ID冲突
 * - messageId关联到AiAdvisorConversation，支持级联删除
 * - metadata使用BlockMetadata数据类，类型安全
 *
 * @property id Block唯一标识（UUID）
 * @property messageId 关联的消息ID
 * @property type Block类型（MAIN_TEXT、THINKING、ERROR）
 * @property status Block状态（PENDING、STREAMING、SUCCESS、ERROR）
 * @property content Block内容
 * @property metadata 元数据（如思考耗时）
 * @property createdAt 创建时间
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see MessageBlockType 消息块类型枚举
 * @see MessageBlockStatus 消息块状态枚举
 */
data class AiAdvisorMessageBlock(
    val id: String,
    val messageId: String,
    val type: MessageBlockType,
    val status: MessageBlockStatus,
    val content: String,
    val metadata: BlockMetadata? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * 创建主文本块
         *
         * 用于创建AI回复的主要内容块。
         * 每条AI消息至少包含一个MAIN_TEXT块。
         *
         * @param messageId 关联的消息ID
         * @param content 初始内容，默认为空
         * @param status 初始状态，默认为PENDING
         * @return 新创建的主文本块
         */
        fun createMainTextBlock(
            messageId: String,
            content: String = "",
            status: MessageBlockStatus = MessageBlockStatus.PENDING
        ): AiAdvisorMessageBlock = AiAdvisorMessageBlock(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            type = MessageBlockType.MAIN_TEXT,
            status = status,
            content = content
        )

        /**
         * 创建思考过程块
         *
         * 用于展示AI的推理过程（DeepSeek R1等模型）。
         * 仅在模型支持思考过程时创建。
         *
         * @param messageId 关联的消息ID
         * @param content 初始内容，默认为空
         * @param status 初始状态，默认为PENDING
         * @return 新创建的思考过程块
         */
        fun createThinkingBlock(
            messageId: String,
            content: String = "",
            status: MessageBlockStatus = MessageBlockStatus.PENDING
        ): AiAdvisorMessageBlock = AiAdvisorMessageBlock(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            type = MessageBlockType.THINKING,
            status = status,
            content = content
        )

        /**
         * 创建错误块
         *
         * 用于展示流式响应过程中的错误信息。
         *
         * @param messageId 关联的消息ID
         * @param errorMessage 错误信息
         * @return 新创建的错误块
         */
        fun createErrorBlock(
            messageId: String,
            errorMessage: String
        ): AiAdvisorMessageBlock = AiAdvisorMessageBlock(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            type = MessageBlockType.ERROR,
            status = MessageBlockStatus.ERROR,
            content = errorMessage
        )
    }

    /**
     * 更新内容
     *
     * 创建一个新的Block实例，更新content字段。
     * 用于流式更新场景。
     *
     * @param newContent 新内容
     * @return 更新后的Block实例
     */
    fun withContent(newContent: String): AiAdvisorMessageBlock =
        copy(content = newContent)

    /**
     * 更新状态
     *
     * 创建一个新的Block实例，更新status字段。
     *
     * @param newStatus 新状态
     * @return 更新后的Block实例
     */
    fun withStatus(newStatus: MessageBlockStatus): AiAdvisorMessageBlock =
        copy(status = newStatus)

    /**
     * 更新内容和状态
     *
     * 创建一个新的Block实例，同时更新content和status字段。
     *
     * @param newContent 新内容
     * @param newStatus 新状态
     * @return 更新后的Block实例
     */
    fun withContentAndStatus(
        newContent: String,
        newStatus: MessageBlockStatus
    ): AiAdvisorMessageBlock = copy(content = newContent, status = newStatus)
}

/**
 * 块元数据
 *
 * 存储Block的附加信息，如思考耗时、Token数量等。
 *
 * @property thinkingMs 思考耗时（毫秒），仅THINKING块使用
 * @property tokenCount Token数量，可选
 */
data class BlockMetadata(
    val thinkingMs: Long? = null,
    val tokenCount: Int? = null
)
