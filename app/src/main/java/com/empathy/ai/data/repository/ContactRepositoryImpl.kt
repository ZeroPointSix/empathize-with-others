package com.empathy.ai.data.repository

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
import javax.inject.Inject

/**
 * 联系人画像仓库实现类
 *
 * 这是连接Domain层(纯Kotlin)和Data层(Android/SQL)的桥梁,是最关键的胶水层。
 */
class ContactRepositoryImpl @Inject constructor(
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
            Result.success(entity?.let { entityToDomain(it) })
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
    // 私有映射函数
    // ============================================================================

    private fun entityToDomain(entity: ContactProfileEntity): ContactProfile {
        val facts = factListConverter.toFactList(entity.factsJson)

        return ContactProfile(
            id = entity.id,
            name = entity.name,
            targetGoal = entity.targetGoal,
            contextDepth = entity.contextDepth,
            facts = facts,
            relationshipScore = entity.relationshipScore,
            lastInteractionDate = entity.lastInteractionDate,
            avatarUrl = entity.avatarUrl
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
            avatarUrl = profile.avatarUrl
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

