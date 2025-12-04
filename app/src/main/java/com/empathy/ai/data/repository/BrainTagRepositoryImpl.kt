package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.BrainTagDao
import com.empathy.ai.data.local.entity.BrainTagEntity
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.BrainTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 策略标签仓库实现类
 *
 * 连接Domain层和Data层的桥梁,负责:
 * 1. 将Domain层的BrainTag转换为Entity存入数据库
 * 2. 从数据库读取Entity并转换为Domain层的BrainTag
 * 3. 提供响应式查询(Flow)和挂起函数操作
 *
 * 映射规范:
 * - toDomain(): Entity → Domain Model
 * - toEntity(): Domain Model → Entity
 */
class BrainTagRepositoryImpl @Inject constructor(
    private val dao: BrainTagDao
) : BrainTagRepository {

    /**
     * 获取联系人的所有策略标签
     *
     * 提供Flow响应式查询,数据变动时自动推送更新。
     *
     * @param contactId 联系人ID
     * @return 标签列表的Flow
     */
    override fun getTagsForContact(contactId: String): Flow<List<BrainTag>> {
        return dao.getTagsByContactId(contactId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 获取全库的雷区标签
     *
     * 用于全局风控检测或无特定联系人时的通用检测。
     *
     * @return 包含所有雷区标签列表的Result
     */
    override suspend fun getAllRedFlags(): Result<List<BrainTag>> {
        return try {
            val entities = dao.getAllRedFlags()
            val tags = entities.map { it.toDomain() }
            Result.success(tags)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存策略标签
     *
     * @param tag 要保存的标签
     * @return 包含新插入ID的Result
     */
    override suspend fun saveTag(tag: BrainTag): Result<Long> {
        return try {
            val entity = tag.toEntity()
            val id = dao.insertTag(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除标签
     *
     * @param id 标签ID
     * @return 操作结果
     */
    override suspend fun deleteTag(id: Long): Result<Unit> {
        return try {
            dao.deleteTag(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ============================================================================
// 私有映射函数
// ============================================================================

/**
 * Entity → Domain Model 转换
 *
 * 把BrainTagEntity转换为Domain层的BrainTag。
 *
 * 异常处理:
 * - 如果type字段不是有效的TagType枚举值,默认使用RISK_RED(雷区)
 * - 采用保守策略: 宁可误报为雷区,不可漏报警告信息
 * - 确保数据库脏数据不会导致应用崩溃
 *
 * @return Domain层的BrainTag对象
 */
private fun BrainTagEntity.toDomain(): BrainTag {
    return BrainTag(
        id = this.id,
        contactId = this.contactId,
        content = this.content,
        type = try {
            TagType.valueOf(this.type) // 字符串转枚举
        } catch (e: IllegalArgumentException) {
            // 如果数据库中存储的type值无效,使用RISK_RED作为安全默认值
            // 保守策略: 将可疑数据视为雷区,避免错过重要警告
            TagType.RISK_RED
        },
        source = this.source
    )
}

/**
 * Domain Model → Entity 转换
 *
 * 把Domain层的BrainTag转换为BrainTagEntity。
 * 核心:把TagType枚举转换为String(name属性)。
 *
 * @return Data层的BrainTagEntity对象
 */
private fun BrainTag.toEntity(): BrainTagEntity {
    return BrainTagEntity(
        id = this.id,
        contactId = this.contactId,
        content = this.content,
        type = this.type.name, // 枚举转字符串
        source = this.source
    )
}
