# QUICK-REF-BUG-ANALYSIS - 项目BUG系统性分析

> 共情AI助手项目开发过程中遇到的所有BUG深度分析
>
> **版本**: v1.0 | **日期**: 2026-01-06 | **状态**: ✅ 已完成
>
> **覆盖范围**: BUG-00001 ~ BUG-00048（所有版本），共55个文档，80+个具体问题

---

## 目录

1. [BUG统计概览](#1-bug统计概览)
2. [第一类：流式响应状态管理](#2-第一类流式响应状态管理)
3. [第二类：消息关联与重新生成](#3-第二类消息关联与重新生成)
4. [第三类：UI渲染时序](#4-第三类ui渲染时序)
5. [第四类：异步操作时序与并发](#5-第四类异步操作时序与并发)
6. [第五类：数据库迁移Room](#6-第五类数据库迁移room)
7. [第六类：生命周期管理](#7-第六类生命周期管理)
8. [第七类：数据持久化与序列化](#8-第七类数据持久化与序列化)
9. [第八类：Hilt依赖注入](#9-第八类hilt依赖注入)
10. [第九类：UI适配与布局](#10-第九类ui适配与布局)
11. [第十类：数据模型设计](#11-第十类数据模型设计)
12. [第十一类：AI交互问题](#11-第十一类ai交互问题)
13. [第十二类：导航与路由](#12-第十二类导航与路由)
14. [最佳实践清单](#13-最佳实践清单)
15. [经验教训总结](#14-经验教训总结)

---

## 1. BUG统计概览

### 1.1 问题分布统计

| BUG编号范围 | 文档数量 | 问题数量 | 主要问题类型 |
|-------------|----------|----------|--------------|
| BUG-00001 ~ 00015 | 15个 | 25个 | 悬浮窗、联系人管理、提示词系统 |
| BUG-00016 ~ 00031 | 16个 | 30个 | 网络、UI交互、数据模型 |
| BUG-00032 ~ 00048 | 18个 | 25个 | AI军师流式对话、迁移、Hilt |
| **总计** | **49个** | **80+个** | **12大类** |

### 1.2 问题分类分布

```
┌─────────────────────────────────────────────────────────────────┐
│                        问题分类分布                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  UI渲染与布局          ████████████████████████  18个 (22.5%)   │
│                                                                  │
│  状态管理              █████████████████████      15个 (18.8%)   │
│                                                                  │
│  异步操作与并发        ██████████████            10个 (12.5%)   │
│                                                                  │
│  生命周期管理          ████████████              8个 (10.0%)    │
│                                                                  │
│  数据持久化与序列化    ██████████                7个 (8.8%)     │
│                                                                  │
│  数据库迁移            ██████                    5个 (6.3%)     │
│                                                                  │
│  AI交互问题            █████                      4个 (5.0%)    │
│                                                                  │
│  其他                  ████                      3个 (4.1%)     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 严重程度分布

| 严重程度 | 数量 | 占比 | 示例 |
|----------|------|------|------|
| P0 崩溃级 | 15个 | 18.8% | 应用启动崩溃、数据迁移失败 |
| P1 严重级 | 35个 | 43.8% | 功能失效、数据丢失、状态混乱 |
| P2 一般级 | 25个 | 31.3% | UI显示问题、边界情况 |
| P3 轻微级 | 5个 | 6.3% | 文字显示、样式问题 |

---

## 2. 第一类：流式响应状态管理

### 2.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-044 | AI回复完成后显示三个省略号 | 流式完成时 | P1 严重 |
| BUG-045-01 | AI正式回复完成后内容直接消失 | 流式完成时 | P1 严重 |
| BUG-046-01 | AI回复完成后跳转到第一个会话 | 会话切换时 | P0 崩溃 |
| BUG-046-03 | 发送消息后自动跳转到第一个会话 | 消息发送时 | P0 崩溃 |
| BUG-047-01 | AI回复完成后内容变成空白框 | 流式完成时 | P1 严重 |
| BUG-047-02 | AI生成回复时短暂出现两个AI回复框 | 渲染时序 | P1 严重 |
| BUG-048-02 | 停止生成后消息直接消失无重新生成按钮 | stopGeneration | P1 严重 |

### 2.2 根因分析

#### 根因1：StreamingState.Completed处理中直接清空状态

**问题代码**：
```kotlin
when (state) {
    is StreamingState.Completed -> {
        _uiState.update {
            it.copy(
                streamingContent = "",  // 直接清空
                currentStreamingMessageId = null
            )
        }
    }
}
```

**根因分析**：
- 数据库更新是异步操作，需要时间完成
- 直接清空`streamingContent`导致UI闪烁
- 用户看到空白然后突然显示内容
- Flow更新延迟导致状态不一致

#### 根因2：Message和Block双重更新不同步

**数据流**：
```
AI响应 → Block更新(实时) → Message.content更新(完成时)
              ↓
         UI显示Block
              ↓
         延迟显示Message
```

**问题**：
- `getConversationsFlow()`返回的是Message对象
- Block更新了但Message.content没有同步
- UI显示`conversation.content`为空

#### 根因3：流式气泡和数据库消息同时渲染

**时序问题**：
```
T0: StreamingMessageBubbleSimple 显示流式气泡
T1: LazyColumn重组，显示数据库消息
T2: 两个气泡同时存在 → 双气泡
```

### 2.3 解决方案

#### 方案：延迟清空 + 状态保持

```kotlin
// AiAdvisorChatViewModel.kt - StreamingState.Completed处理
when (state) {
    is StreamingState.Completed -> {
        val messageId = currentStreamingMessageId ?: return@collect

        // 1. 先保存完整内容到Message
        _uiState.update {
            it.copy(
                streamingContent = state.fullText,
                currentStreamingMessageId = messageId
            )
        }

        // 2. 延迟清空，确保数据库已更新
        viewModelScope.launch {
            delay(800)

            if (_uiState.value.currentSessionId == sessionId) {
                _uiState.update {
                    it.copy(
                        streamingContent = "",
                        currentStreamingMessageId = null
                    )
                }
            }
        }
    }
}
```

#### 方案：双重更新保障

```kotlin
// SendAdvisorMessageStreamingUseCase.kt - 流式完成时
private suspend fun finalizeStreaming(
    messageId: String,
    blockId: String,
    fullText: String
) {
    // 1. 更新Block状态
    blockDao.updateContentAndStatus(
        blockId = blockId,
        content = fullText,
        status = BlockStatus.SUCCESS
    )

    // 2. 同时更新Message的content字段（关键！）
    messageDao.updateContentAndStatus(
        messageId = messageId,
        content = fullText,
        status = SendStatus.SUCCESS
    )
}
```

#### 方案：消息ID检查避免重复渲染

```kotlin
// AiAdvisorChatScreen.kt - 消息渲染逻辑
LazyColumn(modifier = Modifier.fillMaxSize()) {
    items(conversations, key = { it.id }) { conversation ->
        // 排除正在流式显示的消息
        if (conversation.id == currentStreamingMessageId) {
            return@items  // 跳过，使用流式气泡
        }
        ChatBubble(conversation = conversation)
    }

    // 流式消息（不在列表中显示时）
    currentStreamingMessageId?.let { id ->
        if (conversations.none { it.id == id }) {
            item(key = "streaming_$id") {
                StreamingMessageBubbleSimple(
                    content = streamingContent,
                    isStreaming = true
                )
            }
        }
    }
}
```

### 2.4 验证要点

- [ ] 流式完成后800ms内内容不消失
- [ ] 数据库更新完成后内容仍正确显示
- [ ] 停止生成后保留"[用户已停止生成]"提示
- [ ] 不会出现两个AI回复框

---

## 3. 第二类：消息关联与重新生成

### 3.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-048-01 | 被终止内容被当成用户消息显示 | 重新生成时 | P0 崩溃 |
| BUG-048-02 | 重新生成时出现两个AI对话框 | 删除+生成时序 | P1 严重 |
| BUG-048-03 | 应用重启后无法重新生成 | lastUserInput丢失 | P1 严重 |

### 3.2 根因分析

#### 根因1：lastUserInput内存状态不可靠

**问题场景**：
| 场景 | lastUserInput状态 | 结果 |
|------|-------------------|------|
| 应用重启 | 丢失 | 重新生成失败 |
| ViewModel重建 | 丢失 | 重新生成失败 |
| 会话切换 | 丢失 | 重新生成失败 |
| 正常流程 | 存在 | 正常工作 |

#### 根因2：Flow更新延迟导致查询到旧数据

**时序问题**：
```
T0: deleteAIConversation() 开始
T1: regenerateLastMessage() 开始
T2: getConversationsFlow() 返回旧数据
T3: 查询找到错误的消息
T4: deleteAIConversation() 完成
```

#### 根因3：消息关联关系缺失

**原数据模型**：
```kotlin
@Entity
data class AiAdvisorConversationEntity(
    @PrimaryKey val id: String,
    // 缺少关联字段
    val relatedUserMessageId: String? = null  // 需要添加
)
```

### 3.3 解决方案

#### 方案：添加relatedUserMessageId持久化字段

```kotlin
// domain/model/AiAdvisorConversation.kt
data class AiAdvisorConversation(
    val id: String,
    val contactId: String,
    val sessionId: String,
    val messageType: MessageType,
    val content: String,
    val sendStatus: SendStatus,
    val timestamp: Long,
    // 新增：关联的用户消息ID
    val relatedUserMessageId: String = ""
)
```

#### 方案：三重保障获取用户输入

```kotlin
// AiAdvisorChatViewModel.kt
private suspend fun getUserInputForRegenerate(
    sessionId: String,
    aiMessageId: String
): String? {
    // 保障1：从内存获取（最快）
    lastUserInput?.let { return it }

    // 保障2：通过关联ID查询（最可靠）
    val conversation = messageDao.getById(aiMessageId)
    conversation?.relatedUserMessageId?.let { userId ->
        messageDao.getById(userId)?.content?.let { return it }
    }

    // 保障3：时间戳回退查找（兼容性）
    val timestamp = conversation?.timestamp ?: return null
    return messageDao.findLatestUserMessageBefore(
        sessionId = sessionId,
        beforeTimestamp = timestamp
    )
}
```

#### 方案：skipUserMessage参数

```kotlin
// SendAdvisorMessageStreamingUseCase.kt
operator fun invoke(
    contactId: String,
    sessionId: String,
    userMessage: String,
    skipUserMessage: Boolean = false,  // 新增参数
    relatedUserMessageId: String? = null
): Flow<StreamingState> {
    if (!skipUserMessage) {
        // 只有非重新生成场景才保存用户消息
        saveUserMessage(contactId, sessionId, userMessage)
    }
    // ...
}
```

### 3.4 验证要点

- [ ] 应用重启后仍可正常重新生成
- [ ] 重新生成不会创建重复的用户消息
- [ ] 三重保障机制全部生效
- [ ] 数据库迁移后旧数据仍可正常工作

---

## 4. 第三类：UI渲染时序

### 4.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-018 | 分析模式按钮被遮挡 | 按钮渲染 | P2 一般 |
| BUG-020 | 分析模式按钮可见但被挤压 | 布局空间 | P2 一般 |
| BUG-021 | 复制重新生成按钮未渲染 | findViewById | P1 严重 |
| BUG-022 | 悬浮球拖动后出现双悬浮球 | layoutParams | P1 严重 |
| BUG-032 | 联系人列表与设置页面风格不统一 | UI一致性 | P2 一般 |

### 4.2 根因分析

#### 根因1：Android布局约束机制

**问题代码**：
```kotlin
// ScrollView不支持maxHeight属性
<ScrollView
    android:layout_height="wrap_content"
    android:maxHeight="220dp"  // 无效属性
/>
```

**根因**：
- Android原生ScrollView不支持maxHeight属性
- BUG-00017修复方案存在缺陷
- ScrollView实际高度超过220dp将按钮推出屏幕

#### 根因2：layoutParams引用不一致

**问题代码**：
```kotlin
// FloatingBubbleView.kt
private var layoutParams: WindowManager.LayoutParams  // 变量名冲突！

fun updatePosition() {
    val params = view.layoutParams as LayoutParams  // 系统属性
    // 这里使用的可能是错误的layoutParams
    windowManager.updateViewLayout(view, layoutParams)  // 类字段
}
```

**根因**：
- 变量名`layoutParams`与View.layoutParams系统属性冲突
- WindowManager.updateViewLayout操作错误对象

#### 根因3：findViewById返回null

**问题场景**：
- 布局inflate时机问题
- WindowManager布局参数问题
- ensureButtonsVisible()调用失效

### 4.3 解决方案

#### 方案：自定义MaxHeightScrollView

```kotlin
// MaxHeightScrollView.kt
class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ScrollView(context, attrs) {

    private var maxHeight: Int = 0

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MaxHeightScrollView,
            0, 0
        ).apply {
            maxHeight = getDimensionPixelSize(
                R.styleable.MaxHeightScrollView_maxHeight,
                Int.MAX_VALUE
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val newHeightSpec = MeasureSpec.makeMeasureSpec(
            minOf(heightSize, maxHeight),
            MeasureSpec.AT_MOST
        )
        super.onMeasure(widthMeasureSpec, newHeightSpec)
    }
}
```

#### 方案：重命名避免属性冲突

```kotlin
// FloatingBubbleView.kt
// ❌ 原来的问题代码
private var layoutParams: WindowManager.LayoutParams

// ✅ 修复后的代码
private var windowLayoutParams: WindowManager.LayoutParams

fun updatePosition() {
    windowManager.updateViewLayout(view, windowLayoutParams)
}
```

#### 方案：ViewBinding替代findViewById

```kotlin
// ResultCard.kt
private var binding: FloatingResultCardBinding? = null

private fun ensureViewsInitialized() {
    if (binding == null) {
        binding = FloatingResultCardBinding.bind(cardView)
    }
}
```

### 4.4 验证要点

- [ ] 不会出现双悬浮球
- [ ] 按钮不会被遮挡或挤压
- [ ] 布局inflate完成后视图正常显示
- [ ] UI风格统一

---

## 5. 第四类：异步操作时序与并发

### 5.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-016 | 获取模型列表主线程网络异常 | 协程调度 | P0 崩溃 |
| BUG-019 | 悬浮球点击无响应 | 服务生命周期 | P1 严重 |
| BUG-026 | LazyColumn Key重复导致崩溃 | 唯一标识符 | P0 崩溃 |
| BUG-027 | 事实编辑删除ID不匹配 | 序列化 | P1 严重 |
| BUG-028 | Keystore服务连接丢失 | 系统服务 | P0 崩溃 |

### 5.2 根因分析

#### 根因1：协程withContext范围不完整

**问题代码**：
```kotlin
// ❌ 范围太小
withContext(Dispatchers.IO) {
    response.body?.string()  // 在IO线程读取body
}
response.body?.string()  // 主线程继续读取，抛出异常！

// ✅ 正确做法
val result = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()
    response.body?.string() ?: ""
}
```

**根因**：
- `response.body?.string()`是惰性操作，实际读取在主线程执行
- Android主线程网络限制机制导致StrictMode抛出异常
- withContext范围只包裹了execute()调用

#### 根因2：Moshi序列化跳过默认值字段

**问题代码**：
```kotlin
// Fact.kt
data class Fact(
    val id: String = UUID.randomUUID().toString(),  // 默认值
    val content: String
)

// 序列化后JSON：{"content": "内容"}  // id字段被跳过！
// 反序列化后：Fact(id=新UUID, content="内容")  // id变了！
```

**根因**：
- Moshi跳过有默认值的字段进行序列化
- 每次反序列化时id使用默认值生成新UUID
- 导致同一事实不同时间id不同

#### 根因3：Hilt急切初始化+Keystore服务未就绪

**问题场景**：
- MasterKey.Builder.build()只重试50ms
- 多个类使用by lazy但被Hilt触发
- 主线程同步调用Keystore操作

### 5.3 解决方案

#### 方案：完整包裹IO操作

```kotlin
// AiProviderRepositoryImpl.kt
suspend fun testConnection(provider: AiProvider): Result<Boolean> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(provider.timeoutSeconds, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("${provider.baseUrl}/models")
                .addHeader("Authorization", "Bearer ${provider.apiKey}")
                .build()

            val response = client.newCall(request).execute()  // 全部在IO线程
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

#### 方案：自定义Moshi Adapter

```kotlin
// FactJsonAdapter.kt
class FactJsonAdapter {
    @ToJson
    fun toJson(fact: Fact): String {
        return """{"id":"${fact.id}","content":"${fact.content}"}"""
    }

    @FromJson
    fun fromJson(json: String): Fact {
        val map = Moshi.Builder().build()
            .adapter<Map<String, String>>(Map::class.java)
            .fromJson(json) ?: return Fact()
        return Fact(
            id = map["id"] ?: UUID.randomUUID().toString(),
            content = map["content"] ?: ""
        )
    }
}
```

#### 方案：延迟初始化+重试+降级

```kotlin
// ApiKeyStorage.kt
class ApiKeyStorage @Inject constructor(
    private val context: Context
) {
    private val masterKey by lazy {
        var attempt = 0
        var lastException: Exception? = null
        while (attempt < 3) {
            try {
                return@lazy MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            } catch (e: Exception) {
                lastException = e
                attempt++
                delay(100L * (attempt + 1))  // 递增延迟
            }
        }
        throw lastException!!
    }

    private val prefs by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "api_keys",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // 降级：使用普通SharedPreferences
            context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
        }
    }
}
```

### 5.4 验证要点

- [ ] 真机上网络请求不抛出NetworkOnMainThreadException
- [ ] 重新打开应用后Keystore服务正常工作
- [ ] 事实的ID在序列化前后保持一致
- [ ] Flow收集器正确取消和重启

---

## 6. 第五类：数据库迁移（Room）

### 6.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-048-V5 | Migration v14→v15失败 | 应用升级 | P0 崩溃 |
| BUG-00011 | 旧版本提示词数据迁移 | 应用升级 | P1 严重 |

### 6.2 根因分析

#### 根因1：Entity定义与Migration脚本不匹配

**Entity定义**：
```kotlin
// AiAdvisorConversationEntity.kt
data class AiAdvisorConversationEntity(
    val relatedUserMessageId: String = ""  // 默认为空字符串
)
```

**Migration脚本（错误）**：
```kotlin
// ❌ 默认创建允许NULL的列
database.execSQL(
    "ALTER TABLE ai_advisor_conversations " +
    "ADD COLUMN related_user_message_id TEXT"  // 没有NOT NULL
)
```

**冲突**：Room期望NOT NULL，但SQLite创建了可NULL的列

#### 根因2：SQLite ALTER TABLE的限制

| 特性 | SQLite ALTER TABLE | Room期望 |
|------|-------------------|----------|
| ADD COLUMN | 支持 | - |
| NOT NULL | 需要DEFAULT | 必须指定 |
| 类型 | TEXT | TEXT |

#### 根因3：提示词变量占位符未迁移

**问题**：
- 用户设备上显示旧的带变量占位符的提示词
- `getGlobalPrompt()`读取的是持久化的旧数据
- 数据持久化优先于默认值

### 6.3 解决方案

#### 方案：精确匹配Entity定义

```kotlin
// DatabaseModule.kt
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ✅ 显式指定NOT NULL和DEFAULT
        database.execSQL(
            "ALTER TABLE ai_advisor_conversations " +
            "ADD COLUMN related_user_message_id TEXT NOT NULL DEFAULT ''"
        )
    }
}
```

#### 方案：添加Migration测试

```kotlin
// Migration14To15Test.kt
@RunWith(AndroidJUnit4::class)
class Migration14To15Test {

    @Test
    fun migrate14To15_preservesData() {
        // 1. 在v14插入数据
        val v14Database = createV14Database()
        val oldData = getDataFrom(v14Database)

        // 2. 迁移到v15
        val v15Database = Room.databaseBuilder(
            v14Database.openHelper.readableDatabase,
            AppDatabase::class.java,
            "test_database"
        ).addMigration(AppDatabase.MIGRATION_14_15).build()

        // 3. 验证数据完整性
        val newData = getDataFrom(v15Database)
        assertEquals(oldData.size, newData.size)
    }
}
```

#### 方案：提示词版本迁移

```kotlin
// PromptFileStorage.kt
private const val CURRENT_CONFIG_VERSION = 2

fun getGlobalPrompt(scene: PromptScene): String {
    val json = getJson()
    if (json == null) {
        return DefaultPrompts.getDefaultPrompt(scene)
    }

    val version = try {
        json.optInt("version", 1)
    } catch (e: Exception) {
        1
    }

    // 版本1需要迁移
    if (version < CURRENT_CONFIG_VERSION) {
        val migratedJson = migrateFromV1ToV2(json)
        return migratedJson.optString("content", "")
    }

    return json.optString("content", "")
}

private fun migrateFromV1ToV2(json: JSONObject): JSONObject {
    val content = json.optString("content", "")
    // 检测变量占位符
    if (content.contains("{") && content.contains("}")) {
        // 清空使用系统默认
        json.put("content", "")
    }
    json.put("version", 2)
    saveJson(json)
    return json
}
```

### 6.4 验证要点

- [ ] 迁移后应用正常启动
- [ ] 旧数据保留并正确迁移
- [ ] 新增字段有正确的默认值
- [ ] Migration测试全部通过

---

## 7. 第六类：生命周期管理

### 7.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-00001 | 悬浮窗首次启动不显示 | 应用启动 | P1 严重 |
| BUG-00014 | 最小化后悬浮球直接消失 | 状态保存 | P1 严重 |
| BUG-00029 | 悬浮球最小化消失 | onDestroy | P1 严重 |
| BUG-00031 | 设置页面底部导航栏失效 | 导航参数 | P1 严重 |

### 7.2 根因分析

#### 根因1：生命周期不匹配

**问题场景**：
```
SettingsViewModel生命周期：
├── 用户进入设置页面 → ViewModel创建
├── 用户离开设置页面 → ViewModel销毁
└── 应用启动 → 未触发

悬浮窗服务恢复：
├── 需要在应用启动时恢复
└── 但ViewModel还没创建
```

**解决方案**：
```
应用级服务 → Application.onCreate() 恢复
Activity级服务 → Activity.onCreate() 恢复
ViewModel级服务 → ViewModel.init 恢复
```

#### 根因2：状态保存时机错误

**问题代码**：
```kotlin
// ❌ 在流程最后才保存状态
fun minimize() {
    doSomething()
    doOtherThing()
    saveDisplayMode(DISPLAY_MODE_BUBBLE)  // 太晚！
    // 如果前面崩溃，状态不会保存
}
```

**解决方案**：状态保存移到最前面

```kotlin
// ✅ 在操作前保存状态
fun minimize() {
    saveDisplayMode(DISPLAY_MODE_BUBBLE)  // 立即保存
    doSomething()
    doOtherThing()
}
```

#### 根因3：导航参数遗漏

**问题代码**：
```kotlin
// ❌ NavGraph.kt中调用SettingsScreen时遗漏onNavigate参数
composable(Screen.Settings.route) {
    SettingsScreen(
        uiState = uiState.value,
        onNavigate = { }  // 遗漏！
    )
}

// SettingsScreen.kt
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigate: (NavigationEvent) -> Unit = {}  // 默认空实现
)
```

### 7.3 解决方案

#### 方案：Application级别服务恢复

```kotlin
// EmpathyApplication.kt
class EmpathyApplication : Application() {

    @Inject
    lateinit var floatingWindowPreferences: FloatingWindowPreferences

    override fun onCreate() {
        super.onCreate()

        // 应用启动时恢复悬浮窗服务
        CoroutineScope(Dispatchers.Main).launch {
            restoreFloatingWindowService()
        }
    }

    private suspend fun restoreFloatingWindowService() {
        try {
            val shouldShow = floatingWindowPreferences.shouldShowFloatingWindow()
            if (shouldShow) {
                FloatingWindowService.start(this@EmpathyApplication)
            }
        } catch (e: Exception) {
            Log.e("EmpathyApp", "恢复悬浮窗服务失败", e)
        }
    }
}
```

#### 方案：幂等性保护

```kotlin
// FloatingWindowService.kt
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // 幂等性保护
    if (intent?.getBooleanExtra(EXTRA_RESTARTED, false) == true) {
        return START_STICKY
    }

    when (intent?.action) {
        ACTION_SHOW -> showFloatingView()
        ACTION_MINIMIZE -> minimizeToBubble()
        ACTION_STOP -> stopSelf()
    }

    return START_STICKY
}
```

#### 方案：完整传递导航参数

```kotlin
// NavGraph.kt
composable(Screen.Settings.route) {
    SettingsScreen(
        uiState = uiState.value,
        onNavigate = onNavigate  // ✅ 正确传递
    )
}
```

### 7.4 验证要点

- [ ] 应用首次启动悬浮窗自动显示
- [ ] 应用重启后悬浮窗状态保持
- [ ] 状态保存时机正确（崩溃时也不丢失）
- [ ] 导航参数完整传递

---

## 8. 第七类：数据持久化与序列化

### 8.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-00003 | 用户添加事实不可见 | 数据未持久化 | P1 严重 |
| BUG-00026 | LazyColumn Key重复 | id序列化丢失 | P0 崩溃 |
| BUG-00027 | 事实编辑删除ID不匹配 | Moshi默认值 | P1 严重 |

### 8.2 根因分析

#### 根因1：内存状态未持久化

**问题代码**：
```kotlin
// ❌ 只更新内存状态
fun addFactToStream(fact: Fact) {
    val currentFacts = _uiState.value.facts.toMutableList()
    currentFacts.add(fact)
    _uiState.update { it.copy(facts = currentFacts) }
    // 没有保存到数据库！
}
```

**解决方案**：
```kotlin
// ✅ 保存到数据库
fun addFactToStream(fact: Fact) = viewModelScope.launch {
    val updatedProfile = currentProfile.copy(
        facts = currentProfile.facts + fact
    )
    saveProfileUseCase(updatedProfile)
    _uiState.update { it.copy(facts = updatedProfile.facts) }
}
```

#### 根因2：Fact模型缺少稳定的主键

**问题场景**：
- TopTagsSection使用timestamp作为key
- 同一毫秒创建的多个Fact产生重复key
- Compose LazyColumn强制要求key唯一

### 8.3 解决方案

#### 方案：为Fact添加稳定ID

```kotlin
// Fact.kt
data class Fact(
    val id: String = UUID.randomUUID().toString(),  // 稳定主键
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

// TopTagsSection.kt
LazyColumn {
    items(facts, key = { it.id }) { fact ->
        // 使用fact.id作为key，保证唯一性
    }
}
```

#### 方案：自定义JsonAdapter

```kotlin
// FactListConverter.kt
class FactListConverter {
    private val moshi = Moshi.Builder()
        .add(FactJsonAdapter())
        .build()

    private val type = Types.newParameterizedType(
        List::class.java,
        Fact::class.java
    )
    private val adapter = moshi.adapter<List<Fact>>(type)

    @TypeConverter
    fun fromFactList(facts: List<Fact>?): String {
        return adapter.toJson(facts ?: emptyList())
    }

    @TypeConverter
    fun toFactList(json: String?): List<Fact> {
        if (json.isNullOrEmpty()) return emptyList()
        return adapter.fromJson(json) ?: emptyList()
    }
}
```

### 8.4 验证要点

- [ ] 添加的事实持久化到数据库
- [ ] 重新打开应用后数据仍然存在
- [ ] LazyList的key全局唯一
- [ ] 序列化前后ID保持一致

---

## 9. 第八类：Hilt依赖注入

### 9.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-00034 | Hilt多模块编译错误 | 模块改造 | P0 崩溃 |
| BUG-00035 | 多模块Hilt运行时类找不到 | 构建缓存 | P0 崩溃 |

### 9.2 根因分析

#### 根因1：@AndroidEntryPoint类放在错误模块

**问题场景**：
```
TD-00017 Clean Architecture多模块改造时：
├── presentation模块（com.android.library）
├── @AndroidEntryPoint注解的MainActivity
└── AGP字节码转换只在application模块生效
```

**解决方案**：
```kotlin
// ❌ 错误：Activity放在library模块
// presentation/src/main/kotlin/.../MainActivity.kt
@AndroidEntryPoint
class MainActivity : AppCompatActivity()

// ✅ 正确：Activity放在application模块
// app/src/main/java/com/empathy/ai/ui/MainActivity.kt
@AndroidEntryPoint
class MainActivity : AppCompatActivity()
```

#### 根因2：构建缓存污染

**问题场景**：
- 增量构建后安装APK
- 模块间依赖关系未正确更新
- 旧的DEX文件被复用

**解决方案**：
```bash
# 完全清理构建缓存
./gradlew --stop
taskkill /F /IM java.exe
./gradlew clean assembleDebug --rerun-tasks --no-build-cache
```

### 9.3 解决方案

#### 方案：Activity必须放在Application模块

```kotlin
// app/src/main/java/com/empathy/ai/ui/MainActivity.kt
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    // Activity代码
}

// AndroidManifest.xml
<activity
    android:name=".ui.MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

#### 方案：模块依赖声明

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":presentation"))
    implementation(project(":domain"))
    implementation(project(":data"))
}

// presentation/build.gradle.kts
// 不能有 @AndroidEntryPoint 注解的类
android {
    plugin {
        id("com.android.library")
    }
}
```

### 9.4 验证要点

- [ ] @AndroidEntryPoint注解的类在application模块
- [ ] 修改模块依赖后执行完全重新构建
- [ ] 运行时类找不到错误通过清理缓存解决

---

## 10. 第九类：UI适配与布局

### 10.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-00017 | 添加联系人标签覆盖 | UI不完整 | P1 严重 |
| BUG-00018 | 分析模式按钮被遮挡 | maxHeight | P2 一般 |
| BUG-00020 | 按钮被挤压成一条缝 | 空间分配 | P2 一般 |
| BUG-00032 | 风格不统一 | UI一致性 | P2 一般 |

### 10.2 根因分析

#### 根因1：布局约束机制理解不足

**问题**：
- Android标准ScrollView不支持maxHeight属性
- 输入框占用空间过大
- 结果区域高度过大
- 小屏幕设备空间不足

#### 根因2：设计规范缺失

**问题**：
- 历史原因：联系人列表先于iOS风格组件开发
- 组件复用不足：未使用统一的iOS风格组件库
- 缺乏统一的设计系统文档

### 10.3 解决方案

#### 方案：空间分配优化

```kotlin
// floating_tab_content.xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <!-- 输入框：限制高度 -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_height="wrap_content"
        android:minHeight="120dp"
        android:maxHeight="200dp">

        <com.google.android.material.textfield.TextInputEditText
            android:layout_height="wrap_content"
            android:minLines="1"
            android:maxLines="4"  <!-- 限制行数 -->
            android:ellipsize="end" />
    </com.google.android.material.textfield.TextInputLayout>

    <!-- 结果区域：限制高度 -->
    <MaxHeightScrollView
        android:layout_height="wrap_content"
        app:maxHeight="120dp">  <!-- 自定义属性 -->
        <TextView
            android:layout_height="wrap_content"
            android:maxLines="6"
            android:ellipsize="end" />
    </MaxHeightScrollView>

    <!-- 按钮区域：固定高度 -->
    <LinearLayout
        android:layout_height="48dp"
        android:padding="8dp">
        <Button android:layout_height="match_parent" />
    </LinearLayout>
</LinearLayout>
```

#### 方案：设计系统文档

```markdown
# iOS风格设计规范

## 颜色
- 背景色：#F2F2F7
- 卡片背景：#FFFFFF
- 主色调：#007AFF

## 组件
- LargeTitle：大标题样式
- FormField：表单输入样式
- SettingsItem：设置项样式

## 间距
- 标准间距：16dp
- 分隔线：0.5dp
- 圆角：10dp
```

### 10.4 验证要点

- [ ] 小屏幕设备按钮正常显示
- [ ] 输入框和按钮空间分配合理
- [ ] UI风格统一
- [ ] 滚动区域正确限制高度

---

## 11. 第十类：数据模型设计

### 11.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-00005 | 输入删除导致崩溃 | require断言 | P1 严重 |
| BUG-00007 | 新建联系人标签ID不一致 | ID生成时机 | P1 严重 |
| BUG-00009 | 标签类型不一致 | 数据模型不统一 | P1 严重 |

### 11.2 根因分析

#### 根因1：模型层require断言过于严格

**问题代码**：
```kotlin
// ContactProfile.kt
data class ContactProfile(
    val name: String = "",
    val facts: List<Fact> = emptyList()
) {
    init {
        require(name.isNotBlank()) { "姓名不能为空" }
        // 用户输入过程中name可能为空，触发崩溃
    }
}
```

**解决方案**：验证逻辑分层

```kotlin
// 输入时：提示
@Composable
fun NameInputField(
    name: String,
    onNameChange: (String) -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = { newName ->
            if (newName.length <= 20) {  // 长度限制
                onNameChange(newName)
            }
        },
        isError = name.isBlank() && name.isNotEmpty(),
        supportingText = if (name.isBlank() && name.isNotEmpty()) {
            { Text("姓名不能为空") }
        } else null
    )
}

// 保存时：强制
fun saveContact(profile: ContactProfile) {
    require(profile.name.isNotBlank()) { "保存失败：姓名不能为空" }
    // 持久化
}
```

#### 根因2：ID生成时机不一致

**问题场景**：
```
loadContact('') → contactId = UUID_A
startEdit() → editedProfile.id = UUID_B
BrainTag.contactId = UUID_A
ContactProfile.id = UUID_B  // 不匹配！
```

**解决方案**：统一ID生成时机

```kotlin
// ContactDetailViewModel.kt
fun loadContact(contactId: String?) {
    if (contactId.isNullOrBlank()) {
        // 新建联系人：生成临时ID
        val newId = UUID.randomUUID().toString()
        _uiState.update { it.copy(contactId = newId) }
        loadBrainTags(newId)
    } else {
        // 编辑联系人：使用传入的ID
        _uiState.update { it.copy(contactId = contactId) }
        loadBrainTags(contactId)
    }
}

fun startEdit() {
    val currentId = _uiState.value.contactId
    _uiState.update {
        it.copy(
            editedProfile = it.profile.copy(
                id = currentId  // 使用统一的ID
            )
        )
    }
}
```

### 11.3 验证要点

- [ ] 输入删除不会导致崩溃
- [ ] 新建联系人ID保持一致
- [ ] 验证逻辑分层（输入提示，保存强制）

---

## 12. 第十一类：AI交互问题

### 12.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-00010 | 自定义提示词未生效 | Function Calling | P1 严重 |
| BUG-00012 | 全局提示词与专属提示词重叠 | PromptBuilder | P1 严重 |
| BUG-00015 | 三种模式上下文不共通 | 会话状态 | P1 严重 |
| BUG-00025 | AI响应JSON解析失败 | 容错解析 | P1 严重 |

### 12.2 根因分析

#### 根因1：硬编码覆盖参数

**问题代码**：
```kotlin
// AiRepositoryImpl.kt
when (strategy) {
    AiStrategy.FUNCTION_CALLING -> {
        // ❌ 硬编码忽略systemInstruction
        val systemInstruction = SYSTEM_ANALYZE_FC
        // 完全忽略了传入的systemInstruction参数
    }
}
```

#### 根因2：覆盖语义vs追加语义

**问题**：
```
预期行为：
├── 有专属提示词 → 只使用专属
└── 无专属提示词 → 使用全局

实际行为：
├── 总是追加全局 + 专属
└── 导致指令重复
```

#### 根因3：提示词与DTO定义不一致

**问题**：
```
TagUpdateDto要求action字段
AI提示词未要求action字段
AI返回无action
Moshi严格校验失败
```

### 12.3 解决方案

#### 方案：合并提示词

```kotlin
// PromptBuilder.kt
fun buildSystemInstruction(
    scene: PromptScene,
    contactId: String? = null
): String {
    val globalPrompt = promptRepository.getGlobalPrompt(scene)
    val contactPrompt = contactId?.let {
        promptRepository.getContactPrompt(scene, contactId)
    }

    return when {
        // 专属提示词存在：只使用专属
        !contactPrompt.isNullOrBlank() -> contactPrompt
        // 专属不存在：使用全局
        globalPrompt.isNotBlank() -> globalPrompt
        // 都为空：使用系统默认
        else -> DefaultPrompts.getDefaultPrompt(scene)
    }
}
```

#### 方案：容错解析

```kotlin
// ManualSummaryUseCase.kt
private fun parseTagUpdates(response: String): List<TagUpdate> {
    return try {
        // 严格解析
        moshi.adapter<List<TagUpdate>>()
            .fromJson(response) ?: emptyList()
    } catch (e: Exception) {
        // 容错解析：支持缺失action字段
        parseTagUpdatesWithDefault(response)
    }
}

private fun parseTagUpdatesWithDefault(response: String): List<TagUpdate> {
    // 解析逻辑，支持action默认为ADD
    return try {
        val map = parseJsonToMap(response)
        map["updates"]?.split("\n")?.mapNotNull { line ->
            val match = Regex("""(.+?):(.+)""").find(line)
            match?.let {
                TagUpdate(
                    tag = it.groupValues[1].trim(),
                    action = TagAction.ADD  // 默认值
                )
            }
        } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
```

#### 方案：会话上下文服务

```kotlin
// SessionContextService.kt
class SessionContextService @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    private var currentContext: ConversationContext? = null

    fun updateContext(contactId: String, userMessage: String) {
        currentContext = ConversationContext(
            contactId = contactId,
            lastUserMessage = userMessage,
            timestamp = System.currentTimeMillis()
        )
    }

    fun getContext(): ConversationContext? = currentContext

    fun clear() {
        currentContext = null
    }
}

// PolishDraftUseCase.kt
class PolishDraftUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val sessionContextService: SessionContextService
) {
    suspend operator fun invoke(
        contactId: String,
        draft: String
    ): Result<String> {
        val context = sessionContextService.getContext()

        return if (context != null && context.contactId == contactId) {
            // 使用会话上下文
            aiRepository.polishDraftWithContext(
                contactId = contactId,
                draft = draft,
                context = context
            )
        } else {
            // 无会话上下文，使用普通方法
            aiRepository.polishDraft(contactId, draft)
        }
    }
}
```

### 12.4 验证要点

- [ ] 自定义提示词生效
- [ ] 专属提示词正确覆盖全局
- [ ] 三种模式共享上下文
- [ ] AI返回格式容错处理

---

## 13. 第十二类：导航与路由

### 13.1 问题模式

| BUG编号 | 问题描述 | 触发场景 | 影响程度 |
|---------|----------|----------|----------|
| BUG-00031 | 设置页面底部导航栏失效 | onNavigate参数 | P1 严重 |

### 13.2 根因分析

#### 根因：导航参数遗漏

```kotlin
// ❌ NavGraph.kt
composable(Screen.Settings.route) {
    SettingsScreen(
        uiState = uiState.value,
        onNavigate = { }  // 遗漏！
    )
}

// SettingsScreen.kt
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigate: (NavigationEvent) -> Unit = {}  // 默认空实现
) {
    // 底部导航栏点击无响应
}
```

### 13.3 解决方案

#### 方案：完整传递导航参数

```kotlin
// NavGraph.kt
composable(Screen.Settings.route) {
    SettingsScreen(
        uiState = uiState.value,
        onNavigate = onNavigate  // ✅ 正确传递
    )
}

// SettingsScreen.kt
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigate: (NavigationEvent) -> Unit
) {
    BottomNavigation(
        onNavigate = onNavigate  // ✅ 使用传递的参数
    )
}
```

### 13.4 验证要点

- [ ] 底部导航栏点击有响应
- [ ] 导航事件正确传递
- [ ] 回退栈行为一致

---

## 14. 最佳实践清单

### 14.1 流式响应开发

```markdown
## 流式响应开发检查清单

### 状态管理
- [ ] 流式完成后延迟800ms清空streamingContent
- [ ] 使用单一Flow收集器，避免多Flow冲突
- [ ] 在StreamingState.Completed中同时更新Block和Message
- [ ] stopGeneration后保留"[用户已停止生成]"提示

### 数据同步
- [ ] Message.content在流式完成时同步更新
- [ ] Block状态和Message状态保持一致
- [ ] 使用relatedUserMessageId建立消息关联
```

### 14.2 异步操作

```markdown
## 异步操作检查清单

### 协程使用
- [ ] withContext(Dispatchers.IO)完整包裹所有IO操作
- [ ] 网络请求和响应处理在同一线程上下文
- [ ] 使用try-catch包装异步操作

### 并发控制
- [ ] Flow收集器正确取消和重启
- [ ] 使用launch收集Flow，处理异常
- [ ] 添加状态锁防止重复点击
```

### 14.3 数据持久化

```markdown
## 数据持久化检查清单

### 序列化
- [ ] 为数据模型添加稳定的主键（UUID）
- [ ] 使用自定义JsonAdapter处理默认值字段
- [ ] 序列化前后ID保持一致

### 迁移
- [ ] Room Migration脚本与Entity定义完全匹配
- [ ] ALTER TABLE ADD COLUMN添加NOT NULL约束
- [ ] 编写Migration测试用例
- [ ] 添加数据版本迁移逻辑
```

### 14.4 生命周期管理

```markdown
## 生命周期管理检查清单

### 服务恢复
- [ ] 应用级服务在Application.onCreate()中恢复
- [ ] 状态保存时机在操作前而非操作后
- [ ] 幂等性保护防止重复启动

### 导航
- [ ] 完整传递所有导航回调参数
- [ ] 使用popUpTo和launchSingleTop保持一致行为
- [ ] 端到端导航测试
```

### 14.5 UI开发

```markdown
## UI开发检查清单

### 布局
- [ ] 使用自定义MaxHeightScrollView限制高度
- [ ] 避免与系统属性命名冲突（layoutParams等）
- [ ] 使用ViewBinding替代findViewById
- [ ] 小屏幕设备适配测试

### 渲染
- [ ] 使用消息ID检查避免重复渲染
- [ ] LazyList的key全局唯一
- [ ] LaunchedEffect不依赖频繁变化的状态
```

---

## 15. 经验教训总结

### 15.1 核心教训

#### 教训1：内存状态不可靠

**问题**：依赖`lastUserInput`等内存状态导致边界情况失败

**解决**：
```
❌ 原来：lastUserInput: String = ""
✅ 解决：relatedUserMessageId持久化 + 三重保障机制
```

**原则**：关键数据必须持久化，不能依赖内存状态

#### 教训2：异步操作有时序问题

**问题**：数据库更新和UI状态同步存在延迟

**解决**：
```
❌ 原来：立即清空状态
✅ 解决：延迟清空 + 等待Flow更新 + 状态锁
```

**原则**：异步操作必须考虑时序，添加适当的等待

#### 教训3：Migration必须精确

**问题**：Entity定义与Migration脚本不匹配导致崩溃

**解决**：
```
❌ 原来：ADD COLUMN without NOT NULL
✅ 解决：精确复制Entity约束，添加迁移测试
```

**原则**：Migration脚本必须与Entity定义完全一致

#### 教训4：UI渲染需要精确控制

**问题**：流式气泡和数据库消息同时渲染导致双气泡

**解决**：
```
❌ 原来：不做检查，直接渲染
✅ 解决：消息ID检查 + 延迟清空 + key参数
```

**原则**：UI渲染必须考虑数据更新时序

#### 教训5：协程范围要完整

**问题**：withContext范围不完整导致主线程网络异常

**解决**：
```
❌ 原来：只包裹execute()
✅ 解决：完整包裹网络请求和响应处理
```

**原则**：协程范围要完整包裹所有IO操作

#### 教训6：序列化要控制默认值

**问题**：Moshi跳过默认值字段导致ID丢失

**解决**：
```
❌ 原来：依赖默认值的id字段
✅ 解决：自定义JsonAdapter显式处理id
```

**原则**：持久化ID不能依赖默认值

#### 教训7：生命周期要匹配

**问题**：ViewModel生命周期与应用生命周期不匹配

**解决**：
```
❌ 原来：在SettingsViewModel中恢复服务
✅ 解决：在Application.onCreate()中恢复服务
```

**原则**：服务恢复时机必须与作用域匹配

#### 教训8：设计要有一致性

**问题**：联系人列表与设置页面风格不统一

**解决**：
```
❌ 原来：独立开发，无统一设计
✅ 解决：建立设计系统文档，组件库复用
```

**原则**：建立统一的设计系统，新页面优先复用组件

### 15.2 架构改进建议

```
┌─────────────────────────────────────────────────────────────────┐
│                    推荐架构改进                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. 消息关联使用外键约束                                         │
│     AiAdvisorConversationEntity → relatedUserMessageId          │
│                                                                  │
│  2. 状态管理使用单数据源模式                                     │
│     ViewModel使用单一UiState，所有状态集中管理                   │
│                                                                  │
│  3. 异步操作使用协程超时和取消                                   │
│     withTimeoutOrNull { ... } 防止永久阻塞                      │
│                                                                  │
│  4. 关键路径添加详细日志                                         │
│     Log.d("Regenerate", "step: $step")                         │
│                                                                  │
│  5. 数据模型必须包含稳定主键                                     │
│     val id: String = UUID.randomUUID().toString()               │
│                                                                  │
│  6. UI组件要有一致的设计系统                                     │
│     建立DesignSystem组件库                                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 15.3 测试覆盖建议

| 测试类型 | 覆盖场景 | 测试方法 |
|----------|----------|----------|
| 单元测试 | 重新生成逻辑、提示词构建 | MockRepository |
| 集成测试 | Flow收集、状态管理 | TestScope |
| 迁移测试 | 数据库升级 | RoomMigrationTest |
| UI测试 | 双气泡问题、按钮遮挡 | ComposeTestRule |
| 压力测试 | 快速连续操作 | MonkeyTest |
| 真机测试 | 网络请求、Keystore服务 | ManualTest |

### 15.4 调试技巧

```kotlin
// 添加调试日志
object DebugLogger {
    fun logStreaming(tag: String, state: StreamingState) {
        Log.d("Streaming", "[$tag] state: $state")
    }

    fun logRegenerate(step: String, message: String) {
        Log.d("Regenerate", "[$step] $message")
    }

    fun logMigration(version: Int, sql: String) {
        Log.d("Migration", "v$version: $sql")
    }

    fun logAsyncOperation(operation: String, status: String) {
        Log.d("Async", "[$operation] status: $status")
    }
}
```

---

## 参考文档

| BUG编号 | 文档名 | 主要问题类型 |
|---------|--------|--------------|
| BUG-00001~00015 | 悬浮窗、联系人、提示词 | 生命周期、状态管理 |
| BUG-00016~00031 | 网络、UI、数据模型 | 异步操作、序列化 |
| BUG-00032~00048 | AI军师流式对话、迁移、Hilt | 流式响应、数据库迁移 |

---

**文档版本**: v1.0
**创建日期**: 2026-01-06
**维护者**: Claude (AI Assistant)
**覆盖范围**: BUG-00001 ~ BUG-00048（55个文档，80+问题）
**下次更新**: 重大BUG修复后
