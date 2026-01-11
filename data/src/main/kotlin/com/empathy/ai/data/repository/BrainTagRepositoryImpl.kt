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
 * BrainTagRepositoryImpl 实现了策略标签的数据访问层
 *
 * 【架构位置】Clean Architecture Data层
 * 【业务背景】(PRD-00003)联系人画像记忆系统的标签系统
 *   - 红标签(RISK_RED)：雷区标签，提示用户避免的行为或话题
 *   - 绿标签(GREEN)：策略标签，推荐用户采用的行为或话题
 *   - 标签来源于AI分析或用户手动添加
 *
 * 【设计决策】(TDD-00003)
 *   - 简单的CRUD操作，支持Flow响应式查询
 *   - 映射函数私有化，避免暴露转换细节
 *
 * 【关键逻辑】
 *   - TagType降级：未知类型默认降级为RISK_RED，保证系统健壮性
 *   - 响应式查询：getTagsForContact返回Flow，标签变更时自动通知UI
 *
 * 【任务追踪】
 *   - FD-00003/Task-002: 标签系统基础功能
 */
class BrainTagRepositoryImpl @Inject constructor(
    private val dao: BrainTagDao
) : BrainTagRepository {

    override fun getTagsForContact(contactId: String): Flow<List<BrainTag>> {
        return dao.getTagsByContactId(contactId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllRedFlags(): Result<List<BrainTag>> {
        return try {
            val entities = dao.getAllRedFlags()
            val tags = entities.map { it.toDomain() }
            Result.success(tags)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveTag(tag: BrainTag): Result<Long> {
        return try {
            val entity = tag.toEntity()
            val id = dao.insertTag(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTag(id: Long): Result<Unit> {
        return try {
            dao.deleteTag(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新脑标签
     *
     * 【BUG-00066】画像标签编辑功能
     * - 只更新 content 和 type 字段
     * - 保留 contactId、source、isConfirmed 等字段不变
     * - 先验证标签存在，再执行更新
     *
     * @param tag 更新后的标签（只使用 id、content、type 字段）
     * @return 操作结果
     */
    override suspend fun updateTag(tag: BrainTag): Result<Unit> {
        return try {
            // 验证标签存在
            val existingTag = dao.getTagById(tag.id)
                ?: return Result.failure(IllegalArgumentException("标签不存在"))

            // 更新标签（只更新 content 和 type）
            dao.updateTag(
                id = tag.id,
                content = tag.content,
                type = tag.type.name
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ============================================================================
// 私有映射函数：Entity ↔ Domain 转换层
// ============================================================================
//
// 【设计决策】将映射函数设为private extension function，避免暴露转换细节
// 原因：遵循单一职责，Repository只负责调用，不关心转换细节
// 替代方案：Entity/Domain类中的toDomain()/toEntity()方法（当前方案更简洁）
//
// 【健壮性处理】TagType.valueOf失败时默认降级为RISK_RED
// 原因：数据库中可能存在未知类型，降级处理比崩溃更友好

/**
 * toDomain 实体转领域模型
 *
 * 【降级策略】TagType解析失败时默认降级为RISK_RED
 */
private fun BrainTagEntity.toDomain(): BrainTag {
    return BrainTag(
        id = this.id,
        contactId = this.contactId,
        content = this.content,
        type = try {
            TagType.valueOf(this.type)
        } catch (e: IllegalArgumentException) {
            // 降级策略：未知类型归类为风险标签
            TagType.RISK_RED
        },
        source = this.source,
        isConfirmed = this.isConfirmed
    )
}

private fun BrainTag.toEntity(): BrainTagEntity {
    return BrainTagEntity(
        id = this.id,
        contactId = this.contactId,
        content = this.content,
        type = this.type.name,
        source = this.source,
        isConfirmed = this.isConfirmed
    )
}
