package com.empathy.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.empathy.ai.domain.model.AiAdvisorMessageBlock
import com.empathy.ai.domain.model.BlockMetadata
import com.empathy.ai.domain.model.MessageBlockStatus
import com.empathy.ai.domain.model.MessageBlockType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * AI军师消息块数据库实体
 *
 * 存储消息块数据，支持Block-based消息架构。
 * 与AiAdvisorConversationEntity通过外键关联，支持级联删除。
 *
 * 业务背景 (FD-00028):
 * - Block架构支持思考过程展示（DeepSeek R1等模型）
 * - 每个Block独立状态管理，便于流式更新
 * - 支持智能节流更新，减少数据库写入频率
 *
 * 设计决策 (TDD-00028):
 * - 使用外键关联消息表，支持级联删除
 * - 创建message_id索引，优化按消息查询性能
 * - metadata使用JSON格式存储，便于扩展
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see AiAdvisorMessageBlock 领域模型
 */
@Entity(
    tableName = "ai_advisor_message_blocks",
    foreignKeys = [
        ForeignKey(
            entity = AiAdvisorConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("message_id")]
)
data class AiAdvisorMessageBlockEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "type")
    val type: String,  // MessageBlockType.name

    @ColumnInfo(name = "status")
    val status: String,  // MessageBlockStatus.name

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "metadata")
    val metadata: String? = null,  // JSON格式

    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    /**
     * 转换为领域模型
     */
    fun toDomain(): AiAdvisorMessageBlock = AiAdvisorMessageBlock(
        id = id,
        messageId = messageId,
        type = MessageBlockType.valueOf(type),
        status = MessageBlockStatus.valueOf(status),
        content = content,
        metadata = metadata?.let { parseMetadata(it) },
        createdAt = createdAt
    )

    companion object {
        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        /**
         * 从领域模型创建Entity
         */
        fun fromDomain(block: AiAdvisorMessageBlock): AiAdvisorMessageBlockEntity =
            AiAdvisorMessageBlockEntity(
                id = block.id,
                messageId = block.messageId,
                type = block.type.name,
                status = block.status.name,
                content = block.content,
                metadata = block.metadata?.toJson(),
                createdAt = block.createdAt
            )

        /**
         * 解析JSON格式的元数据
         */
        private fun parseMetadata(json: String): BlockMetadata? {
            return try {
                val adapter = moshi.adapter(BlockMetadata::class.java)
                adapter.fromJson(json)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * BlockMetadata扩展函数：转换为JSON
 */
private fun BlockMetadata.toJson(): String {
    val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    val adapter = moshi.adapter(BlockMetadata::class.java)
    return adapter.toJson(this)
}
