package com.empathy.ai.data.repository

import com.empathy.ai.data.local.converter.FactListConverter
import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.data.local.entity.ContactProfileEntity
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.util.MemoryConstants
import com.empathy.ai.presentation.viewmodel.BatchDeleteDebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 联系人画像仓库实现类 - 调试版本
 *
 * 添加了调试日志来追踪可能的误删除操作
 */
class ContactRepositoryImplWithDebug @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {

    private val factListConverter = FactListConverter()

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
                
                // BUG-00027修复：如果标记需要迁移，同步完成迁移
                if (migratingContacts.remove(id)) {
                    com.empathy.ai.domain.util.DebugLogger.d(
                        "ContactRepoImpl",
                        "执行同步迁移: contactId=$id"
                    )
                    val migratedJson = factListConverter.fromFactList(profile.facts)
                    dao.updateFacts(id, migratedJson)
                    com.empathy.ai.domain.util.DebugLogger.d(
                        "ContactRepoImpl",
                        "同步迁移完成: contactId=$id"
                    )
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

            // 将Map转换为Fact列表并合并
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
        // 记录危险操作
        BatchDeleteDebugLogger.logDeleteProfileCall(
            id, 
            Thread.currentThread().stackTrace.joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
        )
        
        return try {
            dao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================================
    // 记忆系统扩展方法实现
    // ============================================================================

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
            // 记录updateFacts调用
            BatchDeleteDebugLogger.logUpdateFacts(contactId, facts.size)
            
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

    // ============================================================================
    // 编辑追踪扩展方法实现（v10）
    // ============================================================================

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

    // ============================================================================
    // 私有映射函数
    // ============================================================================

    /**
     * 需要迁移的联系人ID集合（用于避免重复迁移）
     * 使用线程安全的Set
     */
    private val migratingContacts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun entityToDomain(entity: ContactProfileEntity): ContactProfile {
        val facts = factListConverter.toFactList(entity.factsJson)
        
        // BUG-00027修复：检测旧格式数据并标记需要迁移
        val originalJson = entity.factsJson
        val needsMigration = facts.isNotEmpty() && !originalJson.contains("\"id\":")
        
        if (needsMigration) {
            com.empathy.ai.domain.util.DebugLogger.w(
                "ContactRepoImpl",
                "检测到旧格式数据（无id字段），标记需要迁移: contactId=${entity.id}"
            )
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
            // v10 编辑追踪字段映射
            isNameUserModified = entity.isNameUserModified,
            isGoalUserModified = entity.isGoalUserModified,
            nameLastModifiedTime = entity.nameLastModifiedTime,
            goalLastModifiedTime = entity.goalLastModifiedTime,
            originalName = entity.originalName,
            originalGoal = entity.originalGoal
        )
    }

    private fun domainToEntity(profile: ContactProfile): ContactProfileEntity {
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
            // v10 编辑追踪字段映射
            isNameUserModified = profile.isNameUserModified,
            isGoalUserModified = profile.isGoalUserModified,
            nameLastModifiedTime = profile.nameLastModifiedTime,
            goalLastModifiedTime = profile.goalLastModifiedTime,
            originalName = profile.originalName,
            originalGoal = profile.originalGoal
        )
    }

    /**
     * 合并Facts列表
     * 如果key相同，使用新的Fact替换旧的
     */
    private fun mergeFacts(existing: List<Fact>, newFacts: List<Fact>): List<Fact> {
        val factsMap = existing.associateBy { it.key }.toMutableMap()
        newFacts.forEach { fact ->
            factsMap[fact.key] = fact
        }
        return factsMap.values.toList()
    }
}