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
 * 连接Domain层和Data层的桥梁
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
}

// ============================================================================
// 私有映射函数
// ============================================================================

private fun BrainTagEntity.toDomain(): BrainTag {
    return BrainTag(
        id = this.id,
        contactId = this.contactId,
        content = this.content,
        type = try {
            TagType.valueOf(this.type)
        } catch (e: IllegalArgumentException) {
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
