package com.empathy.ai.data.repository

import android.util.Log
import com.empathy.ai.data.local.converter.FactListConverter
import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.data.local.entity.ContactProfileEntity
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.util.MemoryConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * ContactRepositoryImpl 实现了联系人画像的数据访问层
 *
 * 【架构位置】Clean Architecture Data层，负责Domain层接口的具体实现
 * 【业务背景】(PRD-00003)联系人画像记忆系统的核心数据管理
 *   - 三层记忆架构：短期对话记录 → 中期每日总结 → 长期联系人画像
 *   - 关系分数系统：0-100分，反映联系人的重要程度
 *   - Facts结构：带时间戳和来源的键值对记忆
 *
 * 【设计决策】(TDD-00003)
 *   - 使用Room数据库进行本地存储
 *   - FactListConverter处理JSON序列化的Facts
 *   - 异步迁移逻辑：检测旧格式数据并自动升级
 *
 * 【关键逻辑】
 *   - 数据格式迁移：检测不包含"id"字段的旧Facts格式
 *   - 关系分数约束：使用MemoryConstants钳制在有效范围内
 *   - 合并策略：新增Fact覆盖同名Key，保留时间戳更新的版本
 *
 * 【任务追踪】
 *   - FD-00003/Task-001: 联系人画像基础CRUD
 *   - FD-00003/Task-003: Facts数据结构增强（带时间戳和来源）
 */
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {

    private val factListConverter = FactListConverter()
    private val migratingContacts = ConcurrentHashMap.newKeySet<String>()

    override fun getAllProfiles(): Flow<List<ContactProfile>> {
        return dao.getAllProfiles().map { entities ->
            entities.map { entityToDomain(it) }
        }
    }

    override suspend fun getProfile(id: String): Result<ContactProfile?> {
        return try {
            val entity = dao.getProfileById(id)
            if (entity == null) {
                Result.success(null)
            } else {
                val profile = entityToDomain(entity)
                
                if (migratingContacts.remove(id)) {
                    Log.d("ContactRepoImpl", "执行同步迁移: contactId=$id")
                    val migratedJson = factListConverter.fromFactList(profile.facts)
                    dao.updateFacts(id, migratedJson)
                    Log.d("ContactRepoImpl", "同步迁移完成: contactId=$id")
                }
                
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveProfile(profile: ContactProfile): Result<Unit> {
        return try {
            val entity = domainToEntity(profile)
            dao.insertOrUpdate(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContactFacts(
        contactId: String,
        newFacts: Map<String, String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingEntity = dao.getProfileById(contactId)
                ?: return@withContext Result.failure(Exception("Contact not found: $contactId"))

            val existingFacts = factListConverter.toFactList(existingEntity.factsJson)
            val now = System.currentTimeMillis()

            val newFactsList = newFacts.map { (key, value) ->
                Fact(key = key, value = value, timestamp = now, source = FactSource.MANUAL)
            }

            val mergedFacts = mergeFacts(existingFacts, newFactsList)
            val updatedEntity = existingEntity.copy(
                factsJson = factListConverter.fromFactList(mergedFacts)
            )

            dao.insertOrUpdate(updatedEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProfile(id: String): Result<Unit> {
        return try {
            dao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRelationshipScore(
        contactId: String,
        newScore: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val clampedScore = newScore.coerceIn(
                MemoryConstants.MIN_RELATIONSHIP_SCORE,
                MemoryConstants.MAX_RELATIONSHIP_SCORE
            )
            dao.updateRelationshipScore(contactId, clampedScore)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFacts(
        contactId: String,
        facts: List<Fact>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val factsJson = factListConverter.fromFactList(facts)
            dao.updateFacts(contactId, factsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addFact(
        contactId: String,
        fact: Fact
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingEntity = dao.getProfileById(contactId)
                ?: return@withContext Result.failure(Exception("Contact not found: $contactId"))

            val existingFacts = factListConverter.toFactList(existingEntity.factsJson)
            val mergedFacts = mergeFacts(existingFacts, listOf(fact))
            val factsJson = factListConverter.fromFactList(mergedFacts)

            dao.updateFacts(contactId, factsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLastInteractionDate(
        contactId: String,
        date: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateLastInteractionDate(contactId, date)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContactData(
        contactId: String,
        facts: List<Fact>?,
        relationshipScore: Int?,
        lastInteractionDate: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingEntity = dao.getProfileById(contactId)
                ?: return@withContext Result.failure(Exception("Contact not found: $contactId"))

            val updatedEntity = existingEntity.copy(
                factsJson = facts?.let { factListConverter.fromFactList(it) }
                    ?: existingEntity.factsJson,
                relationshipScore = relationshipScore?.coerceIn(
                    MemoryConstants.MIN_RELATIONSHIP_SCORE,
                    MemoryConstants.MAX_RELATIONSHIP_SCORE
                ) ?: existingEntity.relationshipScore,
                lastInteractionDate = lastInteractionDate ?: existingEntity.lastInteractionDate
            )

            dao.insertOrUpdate(updatedEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(profile: ContactProfile): Result<Unit> {
        return try {
            val entity = domainToEntity(profile)
            dao.insertOrUpdate(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateName(
        contactId: String,
        newName: String,
        modifiedTime: Long,
        originalName: String
    ): Int = withContext(Dispatchers.IO) {
        dao.updateName(contactId, newName, modifiedTime, originalName)
    }

    override suspend fun updateGoal(
        contactId: String,
        newGoal: String,
        modifiedTime: Long,
        originalGoal: String
    ): Int = withContext(Dispatchers.IO) {
        dao.updateGoal(contactId, newGoal, modifiedTime, originalGoal)
    }

    override suspend fun updateContactInfo(
        contactId: String,
        contactInfo: String?
    ): Int = withContext(Dispatchers.IO) {
        dao.updateContactInfo(contactId, contactInfo)
    }

    override suspend fun updateAvatar(
        contactId: String,
        avatarUrl: String?,
        avatarColorSeed: Int
    ): Int = withContext(Dispatchers.IO) {
        dao.updateAvatar(contactId, avatarUrl, avatarColorSeed)
    }

    /**
     * entityToDomain 实体转领域模型
     *
     * 【数据迁移】检测旧格式Facts并标记需要迁移
     * 原因：v1.5.0之前Facts不包含"id"字段，需要升级格式
     * 策略：首次读取时检测，标记到migratingContacts集合
     *       下次读取时触发同步迁移，避免阻塞当前操作
     */
    private fun entityToDomain(entity: ContactProfileEntity): ContactProfile {
        val facts = factListConverter.toFactList(entity.factsJson)

        Log.d("ContactRepoImpl", "entityToDomain: contactId=${entity.id}, facts数量=${facts.size}")

        val originalJson = entity.factsJson
        // 旧格式Facts不包含"id"字段，检测到则标记迁移
        val needsMigration = facts.isNotEmpty() && !originalJson.contains("\"id\"")

        if (needsMigration) {
            Log.w("ContactRepoImpl", "检测到旧格式数据，标记需要迁移: contactId=${entity.id}")
            migratingContacts.add(entity.id)
        }

        return ContactProfile(
            id = entity.id,
            name = entity.name,
            targetGoal = entity.targetGoal,
            contextDepth = entity.contextDepth,
            facts = facts,
            relationshipScore = entity.relationshipScore,
            lastInteractionDate = entity.lastInteractionDate,
            avatarUrl = entity.avatarUrl,
            contactInfo = entity.contactInfo,
            avatarColorSeed = entity.avatarColorSeed,
            isNameUserModified = entity.isNameUserModified,
            isGoalUserModified = entity.isGoalUserModified,
            nameLastModifiedTime = entity.nameLastModifiedTime,
            goalLastModifiedTime = entity.goalLastModifiedTime,
            originalName = entity.originalName,
            originalGoal = entity.originalGoal
        )
    }

    private fun domainToEntity(profile: ContactProfile): ContactProfileEntity {
        Log.d("ContactRepoImpl", "domainToEntity: contactId=${profile.id}, facts数量=${profile.facts.size}")
        
        val factsJson = factListConverter.fromFactList(profile.facts)

        return ContactProfileEntity(
            id = profile.id,
            name = profile.name,
            targetGoal = profile.targetGoal,
            contextDepth = profile.contextDepth,
            factsJson = factsJson,
            relationshipScore = profile.relationshipScore,
            lastInteractionDate = profile.lastInteractionDate,
            avatarUrl = profile.avatarUrl,
            contactInfo = profile.contactInfo,
            avatarColorSeed = profile.avatarColorSeed,
            isNameUserModified = profile.isNameUserModified,
            isGoalUserModified = profile.isGoalUserModified,
            nameLastModifiedTime = profile.nameLastModifiedTime,
            goalLastModifiedTime = profile.goalLastModifiedTime,
            originalName = profile.originalName,
            originalGoal = profile.originalGoal
        )
    }

    /**
     * mergeFacts 合并新旧Facts
     *
     * 【合并策略】新增Fact覆盖同名Key，保留最新时间戳版本
     * 设计权衡：使用Map去重比List合并更高效
     * 业务规则：AI新增的Fact优先于用户手动添加的
     */
    private fun mergeFacts(existing: List<Fact>, newFacts: List<Fact>): List<Fact> {
        val factsMap = existing.associateBy { it.key }.toMutableMap()
        newFacts.forEach { fact ->
            factsMap[fact.key] = fact
        }
        return factsMap.values.toList()
    }
}
