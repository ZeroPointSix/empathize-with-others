# Data Local DAO - 数据访问对象模块

[根目录](../../../../../../../CLAUDE.md) > [app](../../../../../) > [data](../../../) > [local](../../) > [dao](../) > **dao**

## 模块职责

Data Local DAO模块包含所有Room数据库的数据访问对象接口，定义了对数据库表的所有CRUD操作。DAO作为Repository实现层的依赖，提供了类型安全的SQL查询方法，是数据访问层的抽象。

## DAO设计原则

### 1. 响应式查询
- 查询操作返回Flow，数据变更自动推送
- UI层能够自动响应数据变化
- 实现响应式编程模型

### 2. 简化写入
- 使用UPSERT模式（Insert + OnConflict.REPLACE）
- 简化上层业务逻辑
- 保证数据一致性

### 3. 类型安全
- 使用Room编译时验证
- SQL参数化防止注入
- 明确的返回类型定义

## 核心DAO接口

### 1. ContactDao - 联系人数据访问
- **文件**: `ContactDao.kt`
- **目标实体**: ContactProfileEntity
- **表名**: profiles

**核心方法**:
```kotlin
@Dao
interface ContactDao {
    // 响应式查询所有联系人
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ContactProfileEntity>>

    // 根据ID查询
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ContactProfileEntity?

    // 插入或更新（UPSERT）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ContactProfileEntity)

    // 批量插入或更新
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<ContactProfileEntity>)

    // 删除联系人
    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    // 搜索联系人
    @Query("SELECT * FROM profiles WHERE name LIKE '%' || :query || '%'")
    fun searchProfiles(query: String): Flow<List<ContactProfileEntity>>

    // 获取联系人数
    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int
}
```

**设计特点**:
- 使用Flow实现响应式数据流
- 搜索功能使用模糊匹配
- 批量操作支持

### 2. BrainTagDao - 标签数据访问
- **文件**: `BrainTagDao.kt`
- **目标实体**: BrainTagEntity
- **表名**: brain_tags

**核心方法**:
```kotlin
@Dao
interface BrainTagDao {
    // 获取联系人的所有标签
    @Query("SELECT * FROM brain_tags WHERE contact_id = :contactId ORDER BY created_at DESC")
    fun getTagsByContactId(contactId: String): Flow<List<BrainTagEntity>>

    // 获取所有雷区标签
    @Query("SELECT * FROM brain_tags WHERE tag_type = 'RISK_RED'")
    suspend fun getAllRedFlags(): List<BrainTagEntity>

    // 获取所有策略标签
    @Query("SELECT * FROM brain_tags WHERE tag_type = 'STRATEGY_GREEN'")
    suspend fun getAllStrategyTags(): List<BrainTagEntity>

    // 获取未确认的AI标签
    @Query("SELECT * FROM brain_tags WHERE source = 'AI_INFERRED' AND is_confirmed = 0")
    suspend fun getUnconfirmedAiTags(): List<BrainTagEntity>

    // 插入标签
    @Insert
    suspend fun insertTag(entity: BrainTagEntity): Long

    // 更新标签确认状态
    @Query("UPDATE brain_tags SET is_confirmed = :isConfirmed WHERE id = :id")
    suspend fun updateTagConfirmation(id: Long, isConfirmed: Boolean)

    // 删除标签
    @Query("DELETE FROM brain_tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    // 删除联系人的所有标签
    @Query("DELETE FROM brain_tags WHERE contact_id = :contactId")
    suspend fun deleteTagsByContactId(contactId: String)

    // 批量确认标签
    @Query("UPDATE brain_tags SET is_confirmed = 1 WHERE id IN (:ids)")
    suspend fun confirmTags(ids: List<Long>)
}
```

**设计特点**:
- 支持按类型查询
- 批量操作优化
- 未确认标签过滤

### 3. AiProviderDao - AI服务商数据访问
- **文件**: `AiProviderDao.kt`
- **目标实体**: AiProviderEntity
- **表名**: ai_providers

**核心方法**:
```kotlin
@Dao
interface AiProviderDao {
    // 获取所有服务商
    @Query("SELECT * FROM ai_providers ORDER BY is_default DESC, created_at ASC")
    fun getAllProviders(): Flow<List<AiProviderEntity>>

    // 获取默认服务商
    @Query("SELECT * FROM ai_providers WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultProvider(): AiProviderEntity?

    // 插入服务商
    @Insert
    suspend fun insertProvider(entity: AiProviderEntity): Long

    // 更新服务商
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProvider(entity: AiProviderEntity)

    // 设置为默认服务商
    @Query("UPDATE ai_providers SET is_default = 0")
    suspend fun clearAllDefaults()

    @Query("UPDATE ai_providers SET is_default = 1 WHERE id = :id")
    suspend fun setAsDefault(id: Long)

    // 删除服务商
    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun deleteProvider(id: Long)
}
```

**设计特点**:
- 确保只有一个默认服务商
- 原子性操作保证一致性

### 4. ConversationLogDao - 对话日志数据访问
- **文件**: `ConversationLogDao.kt`
- **目标实体**: ConversationLogEntity
- **表名**: conversation_logs

**核心方法**:
```kotlin
@Dao
interface ConversationLogDao {
    // 获取联系人最近的对话日志
    @Query("SELECT * FROM conversation_logs WHERE contact_id = :contactId ORDER BY start_time DESC LIMIT :limit")
    suspend fun getLogsByContactId(contactId: String, limit: Int = 10): List<ConversationLogEntity>

    // 获取日期范围内的对话日志
    @Query("SELECT * FROM conversation_logs WHERE contact_id = :contactId AND start_time BETWEEN :startDate AND :endDate ORDER BY start_time ASC")
    suspend fun getLogsByDateRange(
        contactId: String,
        startDate: Long,
        endDate: Long
    ): List<ConversationLogEntity>

    // 插入对话日志
    @Insert
    suspend fun insertLog(entity: ConversationLogEntity): Long

    // 获取最近的对话时间
    @Query("SELECT MAX(end_time) FROM conversation_logs WHERE contact_id = :contactId")
    suspend fun getLastInteractionTime(contactId: String): Long?

    // 获取对话统计
    @Query("SELECT COUNT(*) FROM conversation_logs WHERE contact_id = :contactId AND start_time >= :since")
    suspend fun getConversationCountSince(contactId: String, since: Long): Int

    // 删除旧日志
    @Query("DELETE FROM conversation_logs WHERE end_time < :before")
    suspend fun deleteOldLogs(before: Long)
}
```

**设计特点**:
- 支持日期范围查询
- 提供统计功能
- 数据清理支持

### 5. DailySummaryDao - 每日总结数据访问
- **文件**: `DailySummaryDao.kt`
- **目标实体**: DailySummaryEntity
- **表名**: daily_summaries

**核心方法**:
```kotlin
@Dao
interface DailySummaryDao {
    // 获取联系人的每日总结
    @Query("SELECT * FROM daily_summaries WHERE contact_id = :contactId ORDER BY date DESC LIMIT :days")
    suspend fun getRecentSummaries(contactId: String, days: Int = 30): List<DailySummaryEntity>

    // 根据日期获取总结
    @Query("SELECT * FROM daily_summaries WHERE contact_id = :contactId AND date = :date")
    suspend fun getSummaryByContactAndDate(contactId: String, date: String): DailySummaryEntity?

    // 插入或更新总结
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DailySummaryEntity): Long

    // 获取总结统计
    @Query("SELECT COUNT(*) FROM daily_summaries WHERE contact_id = :contactId AND date BETWEEN :startDate AND :endDate")
    suspend fun getSummaryCountInRange(
        contactId: String,
        startDate: String,
        endDate: String
    ): Int

    // 获取缺失的总结日期
    @Query("""
        SELECT DISTINCT date FROM conversation_logs
        WHERE contact_id = :contactId
        AND datestrftime('%Y-%m-%d', start_time/1000, 'unixepoch')
        NOT IN (SELECT date FROM daily_summaries WHERE contact_id = :contactId)
        ORDER BY date DESC LIMIT 30
    """)
    suspend fun getMissingSummaryDates(contactId: String): List<String>
}
```

**设计特点**:
- 支持批量查询
- 缺失数据检测
- 日期范围统计

### 6. FailedSummaryTaskDao - 失败任务数据访问
- **文件**: `FailedSummaryTaskDao.kt`
- **目标实体**: FailedSummaryTaskEntity
- **表名**: failed_summary_tasks

**核心方法**:
```kotlin
@Dao
interface FailedSummaryTaskDao {
    // 获取失败任务
    @Query("SELECT * FROM failed_summary_tasks ORDER BY created_at DESC")
    fun getAllFailedTasks(): Flow<List<FailedSummaryTaskEntity>>

    // 获取可重试的任务
    @Query("SELECT * FROM failed_summary_tasks WHERE retry_count < :maxRetries AND last_retry_at < :before")
    suspend fun getRetryableTasks(maxRetries: Int = 3, before: Long = System.currentTimeMillis()): List<FailedSummaryTaskEntity>

    // 插入失败任务
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: FailedSummaryTaskEntity): Long

    // 更新重试信息
    @Query("UPDATE failed_summary_tasks SET retry_count = retry_count + 1, last_retry_at = :retryTime WHERE id = :id")
    suspend fun updateRetryInfo(id: Long, retryTime: Long = System.currentTimeMillis())

    // 删除任务
    @Query("DELETE FROM failed_summary_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    // 清理旧任务
    @Query("DELETE FROM failed_summary_tasks WHERE created_at < :before")
    suspend fun deleteOldTasks(before: Long)
}
```

**设计特点**:
- 重试逻辑支持
- 自动清理机制
- 批量处理能力

## DAO使用模式

### 1. 响应式查询
```kotlin
// 在Repository中使用Flow
fun getAllContacts(): Flow<List<Contact>> {
    return contactDao.getAllProfiles()
        .map { entities -> entities.map { it.toDomainModel() } }
}
```

### 2. 事务操作
```kotlin
@Transaction
suspend fun addContactWithTags(contact: ContactProfile, tags: List<BrainTag>) {
    val contactId = contactDao.insertOrUpdate(contact.toEntity())
    tags.forEach { tag ->
        tagDao.insertTag(tag.copy(contactId = contactId).toEntity())
    }
}
```

### 3. 批量操作
```kotlin
suspend fun importContacts(contacts: List<ContactProfile>) {
    contactDao.insertOrUpdateAll(contacts.map { it.toEntity() })
}
```

## 性能优化策略

### 1. 索引使用
- 在查询条件字段上创建索引
- 复合查询使用复合索引
- 避免全表扫描

### 2. 查询优化
- 使用LIMIT限制返回数据量
- 避免N+1查询问题
- 必要时使用JOIN查询

### 3. 批量操作
- 批量插入优于单条插入
- 使用事务保证一致性
- 合理控制事务大小

## 测试策略

### 单元测试
```kotlin
@Test
fun testInsertAndGetContact() = runTest {
    // 插入联系人
    val entity = ContactProfileEntity(
        id = "test-id",
        name = "Test Contact"
    )
    contactDao.insertOrUpdate(entity)

    // 验证插入结果
    val retrieved = contactDao.getProfileById("test-id")
    assertEquals(entity, retrieved)
}
```

### 集成测试
- 测试DAO与数据库的交互
- 验证事务的正确性
- 测试并发访问场景

## 最佳实践

### 1. 命名规范
- 方法名清晰表达意图
- 使用动词开头
- 参数命名有意义

### 2. 错误处理
- 使用suspend函数支持协程
- 合理处理异常
- 提供降级策略

### 3. 文档注释
- 为每个方法添加KDoc
- 说明参数和返回值
- 提供使用示例

## 相关文件清单

- `ContactDao.kt` - 联系人数据访问
- `BrainTagDao.kt` - 标签数据访问
- `AiProviderDao.kt` - AI服务商数据访问
- `ConversationLogDao.kt` - 对话日志数据访问
- `DailySummaryDao.kt` - 每日总结数据访问
- `FailedSummaryTaskDao.kt` - 失败任务数据访问

## 变更记录 (Changelog)

### 2025-12-20 - Claude (模块文档创建)
- **创建data/local/dao模块文档**
- **记录所有DAO接口定义和使用模式**
- **提供性能优化和测试策略**