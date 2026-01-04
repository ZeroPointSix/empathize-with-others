package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.DailySummaryDao
import com.empathy.ai.data.local.entity.DailySummaryEntity
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.GenerationSource
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.SummaryType
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DailySummaryRepositoryImpl 实现了每日总结的数据访问层
 *
 * 【架构位置】Clean Architecture Data层
 * 【业务背景】(PRD-00003)联系人画像记忆系统的中期记忆层
 *   - 每日对话总结：AI自动生成或用户手动编辑
 *   - 关键事件(KeyEvents)：从对话中提取的标志性事件
 *   - 关系变化趋势：基于多日总结分析关系走向
 *
 * 【设计决策】(TDD-00003)
 *   - 使用Moshi序列化KeyEvents列表为JSON存储
 *   - 支持范围查询：getSummariesInRange获取时间段内的总结
 *   - 支持按类型过滤：DAILY/WEEKLY/MONTHLY等
 *
 * 【关键逻辑】
 *   - toDomain降级：KeyEvents JSON解析失败时返回空列表
 *   - 范围查询：支持统计缺失天数countMissingDatesInRange
 *   - 手动总结：getManualSummaries获取用户编辑的总结
 *
 * 【任务追踪】
 *   - FD-00003/Task-001: 每日总结自动生成
 *   - FD-00003/Task-002: 关键事件提取和存储
 */
@Singleton
class DailySummaryRepositoryImpl @Inject constructor(
    private val dao: DailySummaryDao,
    private val moshi: Moshi
) : DailySummaryRepository {

    private val keyEventsType = Types.newParameterizedType(List::class.java, KeyEvent::class.java)
    private val keyEventsAdapter by lazy { moshi.adapter<List<KeyEvent>>(keyEventsType) }

    override suspend fun saveSummary(
        summary: DailySummary
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val entity = summary.toEntity()
            val id = dao.insert(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSummariesByContact(
        contactId: String
    ): Result<List<DailySummary>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getSummariesByContact(contactId)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSummaryByDate(
        contactId: String,
        date: String
    ): Result<DailySummary?> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getSummaryByDate(contactId, date)
            Result.success(entity?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasSummaryForDate(
        contactId: String,
        date: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val exists = dao.hasSummaryForDate(contactId, date)
            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteByContactId(
        contactId: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.deleteByContactId(contactId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanupOldSummaries(
        beforeTimestamp: Long
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.cleanupOldSummaries(beforeTimestamp)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentSummaries(
        contactId: String,
        days: Int
    ): List<DailySummary> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getRecentSummaries(contactId, days)
            entities.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getSummariesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Result<List<DailySummary>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getSummariesInRange(contactId, startDate, endDate)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSummarizedDatesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val dates = dao.getSummarizedDatesInRange(contactId, startDate, endDate)
            Result.success(dates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSummariesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.deleteSummariesInRange(contactId, startDate, endDate)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getManualSummaries(
        contactId: String
    ): Result<List<DailySummary>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getManualSummaries(contactId)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun countMissingDatesInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.countMissingDatesInRange(contactId, startDate, endDate)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSummariesByType(
        contactId: String,
        summaryType: SummaryType
    ): Result<List<DailySummary>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getSummariesByType(contactId, summaryType.name)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getById(summaryId: Long): DailySummary? = withContext(Dispatchers.IO) {
        dao.getById(summaryId)?.toDomain()
    }

    override suspend fun updateContent(
        summaryId: Long,
        newContent: String,
        modifiedTime: Long,
        originalContent: String
    ): Int = withContext(Dispatchers.IO) {
        dao.updateContent(summaryId, newContent, modifiedTime, originalContent)
    }

    override suspend fun deleteSummary(summaryId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteById(summaryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun DailySummary.toEntity(): DailySummaryEntity {
        return DailySummaryEntity(
            id = id,
            contactId = contactId,
            summaryDate = summaryDate,
            content = content,
            keyEventsJson = keyEventsAdapter.toJson(keyEvents),
            relationshipScore = relationshipScoreChange,
            createdAt = System.currentTimeMillis(),
            startDate = startDate,
            endDate = endDate,
            summaryType = summaryType.name,
            generationSource = generationSource.name,
            conversationCount = conversationCount,
            generatedAt = generatedAt,
            isUserModified = isUserModified,
            lastModifiedTime = lastModifiedTime,
            originalContent = originalContent
        )
    }

    /**
     * toDomain 实体转领域模型
     *
     * 【降级策略】KeyEvents JSON解析失败时返回空列表而非抛出异常
     * 原因：历史数据可能存在格式问题，不应影响用户查看总结内容
     * 设计权衡：牺牲少量功能完整性，换取系统健壮性
     */
    private fun DailySummaryEntity.toDomain(): DailySummary {
        val keyEventsList = try {
            keyEventsAdapter.fromJson(keyEventsJson) ?: emptyList()
        } catch (e: Exception) {
            // 静默降级：解析失败不记录日志，避免日志污染
            emptyList()
        }

        return DailySummary(
            id = id,
            contactId = contactId,
            summaryDate = summaryDate,
            content = content,
            keyEvents = keyEventsList,
            newFacts = emptyList(),
            updatedTags = emptyList(),
            relationshipScoreChange = relationshipScore,
            relationshipTrend = RelationshipTrend.STABLE,
            startDate = startDate,
            endDate = endDate,
            summaryType = try {
                SummaryType.valueOf(summaryType)
            } catch (e: Exception) {
                SummaryType.DAILY
            },
            generationSource = try {
                GenerationSource.valueOf(generationSource)
            } catch (e: Exception) {
                GenerationSource.AUTO
            },
            conversationCount = conversationCount,
            generatedAt = generatedAt,
            isUserModified = isUserModified,
            lastModifiedTime = if (lastModifiedTime > 0) lastModifiedTime else generatedAt,
            originalContent = originalContent
        )
    }
}
