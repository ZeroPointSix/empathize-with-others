# 性能瓶颈分析报告

> 共情AI助手 (Empathy AI Assistant)
> 分析日期: 2026-01-03
> 分析范围: domain, data, presentation, app 模块

---

## 1. 数据库操作瓶颈

### 1.1 N+1 查询风险

**位置**: `ContactRepositoryImpl.kt` (第72-94行)

```kotlin
override suspend fun updateContactFacts(
    contactId: String,
    newFacts: Map<String, String>
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val existingEntity = dao.getProfileById(contactId)  // 查询1
            ?: return@withContext Result.failure(...)

        val existingFacts = factListConverter.toFactList(existingEntity.factsJson)
        // ... 处理逻辑
    }
}
```

**问题描述**:
- 每次更新 Facts 都需要先查询整个联系人实体
- 如果要更新多个字段，会产生多次查询

**影响**:
- 中等 - 对于单次操作影响有限
- 高频调用时累积影响明显

**建议优化**:
- 使用批量更新 API，将查询和更新合并为单个事务
- 考虑使用 Room 的 `@Update` 注解配合 `@Entity` 注解

---

### 1.2 对话记录查询优化

**位置**: `ConversationLogDao.kt` (第117-126行)

```kotlin
@Query("""
    SELECT * FROM (
        SELECT * FROM conversation_logs
        WHERE contact_id = :contactId
        ORDER BY timestamp DESC
        LIMIT :limit
    )
    ORDER BY timestamp ASC
""")
suspend fun getRecentConversations(contactId: String, limit: Int): List<ConversationLogEntity>
```

**问题描述**:
- 使用子查询和双重排序，对于大数据集可能性能不佳
- LIMIT 子查询在外层查询前执行，可能导致索引失效

**影响**:
- 低 - 当前实现已有注释说明，预估 10万条记录下查询时间 < 10ms
- 随着数据增长可能成为瓶颈

**建议优化**:
- 添加复合索引 `(contact_id, timestamp)`
- 考虑使用分页查询替代子查询

---

### 1.3 Facts JSON 序列化开销

**位置**: `FactListConverter.kt`

```kotlin
private fun toFactList(factsJson: String): List<Fact> {
    return try {
        moshi.adapter<List<Fact>>(...).fromJson(factsJson) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
```

**问题描述**:
- 每次读取 Facts 都需要 JSON 反序列化
- 每次保存 Facts 都需要 JSON 序列化
- 如果 Facts 数据量大，序列化/反序列化成为 CPU 热点

**影响**:
- 高 - Facts 是核心业务数据，频繁读写

**建议优化**:
- 考虑使用 Room 的 TypeConverter 直接存储 JSON 字符串
- 对于大型 Facts 列表，考虑分表存储

---

## 2. 网络请求瓶颈

### 2.1 重复的 Token 估算

**位置**: `AiRepositoryImpl.kt` (第204-208行)

```kotlin
private fun estimateTokens(text: String): Int {
    val chineseCount = text.count { it.code in 0x4E00..0x9FFF }
    val otherCount = text.length - chineseCount
    return (chineseCount / 1.5 + otherCount / 4.0).toInt().coerceAtLeast(1)
}
```

**问题描述**:
- Token 估算在每次 AI 调用前执行
- 使用 `count` 和字符遍历，O(n) 复杂度

**影响**:
- 低 - 估算本身开销小，但可在精度要求不高时简化

**建议优化**:
- 使用更简单的字符数估算
- 或缓存估算结果

---

### 2.2 重复的 URL 构建

**位置**: `AiRepositoryImpl.kt` (第735-743行)

```kotlin
private fun buildChatCompletionsUrl(baseUrl: String): String {
    val trimmedUrl = baseUrl.trimEnd('/')
    return when {
        trimmedUrl.endsWith("/v1/chat/completions") -> trimmedUrl
        trimmedUrl.endsWith("/chat/completions") -> ...
        ...
    }
}
```

**问题描述**:
- URL 构建在每次 API 调用前执行
- 字符串操作较多，可缓存

**影响**:
- 低 - URL 构建开销小

**建议优化**:
- 考虑缓存构建后的 URL（以 baseUrl 为 key）

---

### 2.3 缺少请求合并

**位置**: `AiRepositoryImpl.kt`

**问题描述**:
- 多个 AI 请求独立发送，没有请求合并或批处理
- 如果用户快速触发多个分析请求，会产生多个并发请求

**影响**:
- 中等 - 可能导致 API 调用配额快速消耗

**建议优化**:
- 添加请求去重/合并逻辑
- 考虑使用 OkHttp 的拦截器实现请求合并

---

## 3. 集合操作瓶颈

### 3.1 搜索过滤算法

**位置**: `ContactListViewModel.kt` (第204-221行)

```kotlin
private fun performSearch(query: String) {
    val currentState = _uiState.value
    val filteredContacts = currentState.contacts.filter { contact ->
        contact.name.contains(query, ignoreCase = true) ||
        contact.targetGoal.contains(query, ignoreCase = true) ||
        contact.facts.any { fact ->
            fact.key.contains(query, ignoreCase = true) ||
            fact.value.contains(query, ignoreCase = true)
        }
    }
    ...
}
```

**问题描述**:
- O(n * m) 复杂度，其中 n 是联系人数量，m 是平均 Facts 数量
- 每次搜索都遍历所有联系人

**影响**:
- 高 - 联系人数量增长后性能下降明显

**建议优化**:
- 使用搜索索引
- 添加搜索结果缓存
- 考虑分页加载

---

### 3.2 Facts 合并操作

**位置**: `ContactRepositoryImpl.kt` (第274-280行)

```kotlin
private fun mergeFacts(existing: List<Fact>, newFacts: List<Fact>): List<Fact> {
    val factsMap = existing.associateBy { it.key }.toMutableMap()
    newFacts.forEach { fact ->
        factsMap[fact.key] = fact
    }
    return factsMap.values.toList()
}
```

**问题描述**:
- 使用 `associateBy` 创建 Map 是 O(n) 操作
- 整体复杂度 O(n + m)

**影响**:
- 低 - Facts 数量通常较少

**建议优化**:
- 当前实现已经较优，可保持

---

### 3.3 标签过滤

**位置**: `AnalyzeChatUseCase.kt` (第138-139行)

```kotlin
val redTags = brainTags.filter { it.type == TagType.RISK_RED }
val greenTags = brainTags.filter { it.type == TagType.STRATEGY_GREEN }
```

**问题描述**:
- 两次遍历同一列表
- O(2n) 复杂度

**影响**:
- 低 - 标签数量通常较少

**建议优化**:
- 使用单次遍历分组

```kotlin
val (redTags, greenTags) = brainTags.partition { it.type == TagType.RISK_RED }
```

---

## 4. 内存相关瓶颈

### 4.1 ViewModel 状态复制

**位置**: `ContactListViewModel.kt` (第112-119行)

```kotlin
getAllContactsUseCase().collect { contacts ->
    _uiState.update { currentState ->
        currentState.copy(
            isLoading = false,
            contacts = contacts,
            filteredContacts = contacts,
            hasMore = contacts.size >= currentState.pageSize
        )
    }
}
```

**问题描述**:
- `copy()` 方法创建新的 State 对象
- `contacts.toList()` 创建新列表

**影响**:
- 中等 - 每次状态更新都有内存分配
- 对于大列表可能触发 GC

**建议优化**:
- 考虑使用不可变数据类优化
- 使用 `MutableStateFlow` 减少对象创建

---

### 4.2 AI Repository Moshi 实例

**位置**: `AiRepositoryImpl.kt` (第52-54行)

```kotlin
private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
```

**问题描述**:
- 每个 AiRepositoryImpl 实例都创建新的 Moshi 实例
- Moshi 实例创建开销较大

**影响**:
- 中等 - 如果创建多个 Repository 实例

**建议优化**:
- 使用单例 Moshi 实例
- 通过 DI 注入共享实例

---

### 4.3 搜索结果缓存缺失

**位置**: `ContactListViewModel.kt`

**问题描述**:
- 每次输入变化都重新搜索
- 没有缓存上一轮的搜索结果

**影响**:
- 中等 - 用户快速输入时可能产生不必要的搜索

**建议优化**:
- 添加搜索防抖
- 缓存搜索结果供快速切换

---

## 5. 并发相关瓶颈

### 5.1 协程并发控制

**位置**: `SummarizeDailyConversationsUseCase.kt` (第95-102行)

```kotlin
for (contact in allContacts) {
    val result = summarizeForContact(contact.id, yesterday)
    when {
        result.isSuccess && result.getOrNull() == true -> successCount++
        ...
    }
}
```

**问题描述**:
- 联系人总结顺序执行，没有并行处理
- 如果联系人数量多，耗时较长

**影响**:
- 高 - 每日总结可能耗时数分钟

**建议优化**:
- 使用 `coroutineScope` + `async` 并行处理
- 添加并发数量限制

---

### 5.2 缓存锁竞争

**位置**: `PromptFileStorage.kt` (第42-44行, 第59-60行)

```kotlin
@Volatile
private var cachedConfig: GlobalPromptConfig? = null
private val cacheLock = Mutex()

suspend fun readGlobalConfig(): Result<GlobalPromptConfig> = withContext(ioDispatcher) {
    cachedConfig?.let { return@withContext Result.success(it) }
    cacheLock.withLock {
        cachedConfig?.let { return@withContext Result.success(it) }
        ...
    }
}
```

**问题描述**:
- 使用 Mutex 实现同步，线程竞争时可能阻塞
- 双重检查锁定模式

**影响**:
- 低 - 读取操作频繁但锁竞争时间短

**建议优化**:
- 当前实现已较优，可保持

---

### 5.3 数据库事务

**位置**: 多个 Repository 实现

**问题描述**:
- 多个写操作没有使用事务包装
- 失败时可能产生中间状态

**影响**:
- 中等 - 数据一致性风险

**建议优化**:
- 使用 Room 的 `@Transaction` 注解
- 批量操作使用事务

---

## 6. 总结

### 瓶颈严重程度分布

| 级别 | 数量 | 说明 |
|------|------|------|
| 高   | 3    | N+1查询风险、搜索算法复杂度、协程并发 |
| 中   | 6    | Facts序列化、状态复制、事务处理等 |
| 低   | 7    | Token估算、URL构建、合并操作等 |

### 优先级建议

1. **P0 (紧急)**: 搜索过滤算法优化
2. **P1 (重要)**: 协程并行处理、每日总结性能
3. **P2 (一般)**: 数据库查询优化、内存优化
4. **P3 (低优先级)**: 小型优化，如 Token 估算、URL 缓存

---

**报告生成时间**: 2026-01-03
**分析工具**: Claude Code Manual Analysis
