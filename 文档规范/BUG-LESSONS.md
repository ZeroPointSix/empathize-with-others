# BUG经验教训总结

> 共情AI助手项目开发过程中积累的BUG修复经验
>
> **版本**: v1.0 | **日期**: 2026-01-06 | **状态**: ✅ 已发布
>
> **覆盖范围**: BUG-00001 ~ BUG-00048（55个文档，80+个具体问题）

---

## 目录

1. [核心教训](#1-核心教训)
2. [问题分类统计](#2-问题分类统计)
3. [流式响应开发](#3-流式响应开发)
4. [异步操作与协程](#4-异步操作与协程)
5. [数据库与持久化](#5-数据库与持久化)
6. [UI与布局](#6-ui与布局)
7. [生命周期管理](#7-生命周期管理)
8. [依赖注入](#8-依赖注入)
9. [数据模型设计](#9-数据模型设计)
10. [AI交互问题](#10-ai交互问题)
11. [最佳实践检查清单](#11-最佳实践检查清单)
12. [常见错误模式](#12-常见错误模式)
13. [调试技巧](#13-调试技巧)

---

## 1. 核心教训

### 1.1 教训一：内存状态不可靠

**问题描述**：依赖`lastUserInput`等内存状态导致边界情况失败

**错误代码**：
```kotlin
// ❌ 内存状态会在以下场景丢失
private var lastUserInput: String = ""  // 应用重启丢失
// ViewModel重建丢失
// 会话切换丢失
```

**正确做法**：
```kotlin
// ✅ 关键数据必须持久化
data class AiAdvisorConversation(
    val id: String,
    val relatedUserMessageId: String = ""  // 持久化关联
)

// 三重保障机制
private suspend fun getUserInputForRegenerate(...): String? {
    // 1. 内存获取（最快）
    lastUserInput?.let { return it }

    // 2. ID关联查询（最可靠）
    conversation?.relatedUserMessageId?.let { userId ->
        messageDao.getById(userId)?.content?.let { return it }
    }

    // 3. 时间戳回退（兼容性）
    return messageDao.findLatestUserMessageBefore(...)
}
```

**原则**：关键数据必须持久化，不能依赖内存状态

---

### 1.2 教训二：异步操作有时序问题

**问题描述**：数据库更新和UI状态同步存在延迟

**错误代码**：
```kotlin
// ❌ 立即清空状态
fun stopGeneration() {
    _uiState.update {
        it.copy(streamingContent = "", currentStreamingMessageId = null)
    }
    // 数据库更新是异步的，UI出现空白期
}
```

**正确做法**：
```kotlin
// ✅ 延迟清空 + 状态保持
when (state) {
    is StreamingState.Completed -> {
        _uiState.update {
            it.copy(streamingContent = state.fullText, currentStreamingMessageId = messageId)
        }

        viewModelScope.launch {
            delay(800)  // 等待数据库更新

            if (_uiState.value.currentSessionId == sessionId) {
                _uiState.update {
                    it.copy(streamingContent = "", currentStreamingMessageId = null)
                }
            }
        }
    }
}
```

**原则**：异步操作必须考虑时序，添加适当的等待

---

### 1.3 教训三：Migration必须精确

**问题描述**：Entity定义与Migration脚本不匹配导致应用启动崩溃

**错误代码**：
```kotlin
// Entity定义
data class AiAdvisorConversationEntity(
    val relatedUserMessageId: String = ""  // NOT NULL
)

// Migration脚本（错误）
database.execSQL(
    "ALTER TABLE ai_advisor_conversations " +
    "ADD COLUMN related_user_message_id TEXT"  // 默认NULL！
)
```

**正确做法**：
```kotlin
// ✅ 精确匹配Entity定义
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE ai_advisor_conversations " +
            "ADD COLUMN related_user_message_id TEXT NOT NULL DEFAULT ''"
        )
    }
}

// ✅ 添加Migration测试
@RunWith(AndroidJUnit4::class)
class Migration14To15Test {
    @Test
    fun migrate14To15_preservesData() { ... }
}
```

**原则**：Migration脚本必须与Entity定义完全一致

---

### 1.4 教训四：UI渲染需要精确控制

**问题描述**：流式气泡和数据库消息同时渲染导致双气泡

**错误代码**：
```kotlin
// ❌ 不做检查，直接渲染
LazyColumn {
    items(conversations) { conversation ->
        ChatBubble(conversation)  // 可能与流式气泡重复
    }
    item { StreamingMessageBubbleSimple(...) }  // 总是显示
}
```

**正确做法**：
```kotlin
// ✅ 消息ID检查避免重复渲染
LazyColumn {
    items(conversations, key = { it.id }) { conversation ->
        if (conversation.id == currentStreamingMessageId) {
            return@items  // 跳过，使用流式气泡
        }
        ChatBubble(conversation)
    }

    currentStreamingMessageId?.let { id ->
        if (conversations.none { it.id == id }) {
            item(key = "streaming_$id") {
                StreamingMessageBubbleSimple(content = streamingContent)
            }
        }
    }
}
```

**原则**：UI渲染必须考虑数据更新时序

---

### 1.5 教训五：协程范围要完整

**问题描述**：withContext范围不完整导致主线程网络异常

**错误代码**：
```kotlin
// ❌ 范围太小
withContext(Dispatchers.IO) {
    response.body?.string()  // 在IO线程读取body
}
response.body?.string()  // 主线程继续读取，抛出异常！

// ❌ 只包裹execute()
withContext(Dispatchers.IO) {
    client.newCall(request).execute()  // execute()在IO线程
    response.body?.string()  // 但string()是惰性的！
}
```

**正确做法**：
```kotlin
// ✅ 完整包裹网络请求和响应处理
val result = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()
    response.body?.string() ?: ""  // 全部在IO线程
}
```

**原则**：协程范围要完整包裹所有IO操作

---

### 1.6 教训六：序列化要控制默认值

**问题描述**：Moshi跳过默认值字段导致ID丢失

**错误代码**：
```kotlin
// Fact.kt
data class Fact(
    val id: String = UUID.randomUUID().toString(),  // 默认值
    val content: String
)

// 序列化后JSON：{"content": "内容"}  // id字段被跳过！
// 反序列化后：Fact(id=新UUID, content="内容")  // id变了！
```

**正确做法**：
```kotlin
// ✅ 自定义Moshi Adapter
class FactJsonAdapter {
    @ToJson
    fun toJson(fact: Fact): String {
        return """{"id":"${fact.id}","content":"${fact.content}"}"""
    }

    @FromJson
    fun fromJson(json: String): Fact {
        val map = parseJsonToMap(json)
        return Fact(
            id = map["id"] ?: UUID.randomUUID().toString(),
            content = map["content"] ?: ""
        )
    }
}
```

**原则**：持久化ID不能依赖默认值

---

### 1.7 教训七：生命周期要匹配

**问题描述**：ViewModel生命周期与应用生命周期不匹配

**错误场景**：
```
SettingsViewModel生命周期：
├── 用户进入设置页面 → ViewModel创建
├── 用户离开设置页面 → ViewModel销毁
└── 应用启动 → 未触发

悬浮窗服务恢复：
├── 需要在应用启动时恢复
└── 但ViewModel还没创建
```

**正确做法**：
```kotlin
// ✅ Application级别服务恢复
class EmpathyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Main).launch {
            restoreFloatingWindowService()
        }
    }

    private suspend fun restoreFloatingWindowService() {
        val shouldShow = floatingWindowPreferences.shouldShowFloatingWindow()
        if (shouldShow) {
            FloatingWindowService.start(this@EmpathyApplication)
        }
    }
}
```

**原则**：服务恢复时机必须与作用域匹配

---

### 1.8 教训八：设计要有一致性

**问题描述**：联系人列表与设置页面风格不统一

**原因**：
- 历史原因：联系人列表先于iOS风格组件开发
- 组件复用不足：未使用统一的iOS风格组件库
- 缺乏统一的设计系统文档

**正确做法**：
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
```

**原则**：建立统一的设计系统，新页面优先复用组件

---

## 2. 问题分类统计

### 2.1 问题分类分布

| 分类 | 数量 | 占比 | 严重程度 |
|------|------|------|----------|
| UI渲染与布局 | 18个 | 22.5% | P2 一般为主 |
| 状态管理 | 15个 | 18.8% | P1 严重为主 |
| 异步操作与并发 | 10个 | 12.5% | P0/P1 崩溃为主 |
| 生命周期管理 | 8个 | 10.0% | P1 严重为主 |
| 数据持久化与序列化 | 7个 | 8.8% | P1 严重为主 |
| 数据库迁移 | 5个 | 6.3% | P0 崩溃为主 |
| AI交互问题 | 4个 | 5.0% | P1 严重为主 |
| 其他 | 13个 | 16.3% | P2/P3 轻微 |

### 2.2 严重程度分布

| 严重程度 | 数量 | 占比 | 示例 |
|----------|------|------|------|
| P0 崩溃级 | 15个 | 18.8% | 应用启动崩溃、数据迁移失败 |
| P1 严重级 | 35个 | 43.8% | 功能失效、数据丢失、状态混乱 |
| P2 一般级 | 25个 | 31.3% | UI显示问题、边界情况 |
| P3 轻微级 | 5个 | 6.3% | 文字显示、样式问题 |

---

## 3. 流式响应开发

### 3.1 常见问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| AI回复完成后内容直接消失 | streamingContent被过早清空 | 延迟800ms清空 |
| 停止生成后无重新生成按钮 | stopGeneration立即清空状态 | 保留"[用户已停止生成]" |
| 出现两个AI回复框 | 流式气泡和数据库消息同时渲染 | 消息ID检查 |
| 内容变成空白框 | Message和Block双重更新不同步 | 同时更新两者 |

### 3.2 最佳实践

```kotlin
// 1. 流式完成处理
when (state) {
    is StreamingState.Completed -> {
        // 1.1 保持UI状态
        _uiState.update {
            it.copy(streamingContent = state.fullText, currentStreamingMessageId = messageId)
        }

        // 1.2 延迟清空
        viewModelScope.launch {
            delay(800)
            _uiState.update {
                it.copy(streamingContent = "", currentStreamingMessageId = null)
            }
        }

        // 1.3 同时更新Block和Message
        finalizeStreaming(messageId, blockId, fullText)
    }
}

// 2. 双重更新保障
private suspend fun finalizeStreaming(messageId: String, blockId: String, fullText: String) {
    blockDao.updateContentAndStatus(blockId, fullText, BlockStatus.SUCCESS)
    messageDao.updateContentAndStatus(messageId, fullText, SendStatus.SUCCESS)
}

// 3. 消息ID检查
if (conversation.id == currentStreamingMessageId) return@items
```

---

## 4. 异步操作与协程

### 4.1 常见问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| 主线程网络异常 | withContext范围不完整 | 完整包裹IO操作 |
| Flow收集器冲突 | 多个Flow同时运行 | 单一Flow收集器 |
| Keystore服务连接丢失 | 重试次数不足 | 3次重试+递增延迟 |
| 状态锁失效 | 异步操作未等待 | 添加delay等待 |

### 4.2 最佳实践

```kotlin
// 1. 完整包裹IO操作
suspend fun testConnection(provider: AiProvider): Result<Boolean> {
    return withContext(Dispatchers.IO) {
        val request = Request.Builder().url("${provider.baseUrl}/models").build()
        val response = client.newCall(request).execute()
        Result.success(response.isSuccessful)
    }
}

// 2. 单一Flow收集器
private var conversationsJob: Job? = null

fun loadConversations(sessionId: String) {
    conversationsJob?.cancel()
    conversationsJob = viewModelScope.launch {
        repository.getConversationsFlow(sessionId).collect { conversations ->
            _uiState.update { it.copy(conversations = conversations) }
        }
    }
}

// 3. 重试+降级
private val masterKey by lazy {
    var attempt = 0
    var lastException: Exception? = null
    while (attempt < 3) {
        try {
            return@lazy MasterKey.Builder(context).build()
        } catch (e: Exception) {
            lastException = e
            attempt++
            delay(100L * (attempt + 1))  // 递增延迟
        }
    }
    throw lastException!!
}

// 4. 等待异步操作完成
fun deleteAndRegenerate(aiMessageId: String, userInput: String) = viewModelScope.launch {
    deleteAdvisorConversationUseCase(aiMessageId)
    delay(200)  // 等待Flow更新
    sendMessageStreaming(userMessage = userInput, skipUserMessage = true)
}
```

---

## 5. 数据库与持久化

### 5.1 常见问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| Migration失败 | NOT NULL约束不匹配 | 精确匹配Entity定义 |
| ID序列化丢失 | Moshi跳过默认值 | 自定义JsonAdapter |
| 数据未持久化 | 只更新内存状态 | 调用Repository保存 |
| LazyColumn Key重复 | timestamp作为key | 使用UUID作为key |

### 5.2 最佳实践

```kotlin
// 1. Migration精确匹配
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE ai_advisor_conversations " +
            "ADD COLUMN related_user_message_id TEXT NOT NULL DEFAULT ''"
        )
    }
}

// 2. 自定义JsonAdapter
class FactJsonAdapter {
    @ToJson fun toJson(fact: Fact) = """{"id":"${fact.id}","content":"${fact.content}"}"""
    @FromJson fun fromJson(json: String): Fact {
        val map = parseJsonToMap(json)
        return Fact(
            id = map["id"] ?: UUID.randomUUID().toString(),
            content = map["content"] ?: ""
        )
    }
}

// 3. 数据持久化
fun addFactToStream(fact: Fact) = viewModelScope.launch {
    val updatedProfile = currentProfile.copy(facts = currentProfile.facts + fact)
    saveProfileUseCase(updatedProfile)  // 持久化
    _uiState.update { it.copy(facts = updatedProfile.facts) }
}

// 4. 稳定的主键
data class Fact(
    val id: String = UUID.randomUUID().toString(),  // 稳定主键
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## 6. UI与布局

### 6.1 常见问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| 按钮被遮挡 | ScrollView不支持maxHeight | 自定义MaxHeightScrollView |
| 双悬浮球 | layoutParams变量名冲突 | 重命名为windowLayoutParams |
| 按钮未渲染 | findViewById返回null | 使用ViewBinding |
| 风格不统一 | 缺少设计系统 | 建立DesignSystem组件库 |

### 6.2 最佳实践

```kotlin
// 1. 自定义MaxHeightScrollView
class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ScrollView(context, attrs) {
    private var maxHeight: Int = 0
    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView).apply {
            maxHeight = getDimensionPixelSize(R.styleable.MaxHeightScrollView_maxHeight, Int.MAX_VALUE)
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(minOf(heightSize, maxHeight), MeasureSpec.AT_MOST))
    }
}

// 2. 避免与系统属性命名冲突
// ❌ private var layoutParams: WindowManager.LayoutParams
// ✅ private var windowLayoutParams: WindowManager.LayoutParams

// 3. 使用ViewBinding
private var binding: FloatingResultCardBinding? = null
private fun ensureViewsInitialized() {
    if (binding == null) {
        binding = FloatingResultCardBinding.bind(cardView)
    }
}

// 4. 设计系统组件库
@Composable
fun IOSLargeTitleBar(title: String) { ... }
@Composable
fun IOSFormField(...) { ... }
@Composable
fun IOSSettingsItem(...) { ... }
```

---

## 7. 生命周期管理

### 7.1 常见问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| 悬浮窗首次不显示 | ViewModel与应用生命周期不匹配 | Application.onCreate()恢复 |
| 状态丢失 | 状态保存时机错误 | 操作前保存 |
| 导航参数遗漏 | onNavigate未传递 | 完整传递导航参数 |
| 服务重复启动 | 缺少幂等性保护 | 添加状态检查 |

### 7.2 最佳实践

```kotlin
// 1. Application级别服务恢复
class EmpathyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Main).launch { restoreFloatingWindowService() }
    }
}

// 2. 状态保存时机（操作前）
fun minimize() {
    saveDisplayMode(DISPLAY_MODE_BUBBLE)  // ✅ 操作前保存
    doSomething()
    doOtherThing()
}

// 3. 完整传递导航参数
composable(Screen.Settings.route) {
    SettingsScreen(uiState = uiState.value, onNavigate = onNavigate)  // ✅ 传递
}

// 4. 幂等性保护
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.getBooleanExtra(EXTRA_RESTARTED, false) == true) {
        return START_STICKY
    }
    // ...
}
```

---

## 8. 依赖注入

### 8.1 常见问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| 编译错误 | @AndroidEntryPoint在library模块 | 移到application模块 |
| 类找不到 | 构建缓存污染 | 清理缓存重新构建 |

### 8.2 最佳实践

```kotlin
// 1. @AndroidEntryPoint类必须在application模块
// app/src/main/java/com/empathy/ai/ui/MainActivity.kt
@AndroidEntryPoint
class MainActivity : AppCompatActivity()

// 2. 修改模块依赖后完全重新构建
// ./gradlew --stop && taskkill /F /IM java.exe
// ./gradlew clean assembleDebug --rerun-tasks --no-build-cache
```

---

## 9. 数据模型设计

### 9.1 常见问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| 输入删除崩溃 | require断言过于严格 | 验证逻辑分层 |
| ID不一致 | ID生成时机不一致 | 统一ID生成时机 |
| 数据不互通 | Fact和BrainTag是两种模型 | 统一使用Fact |

### 9.2 最佳实践

```kotlin
// 1. 验证逻辑分层
// 输入时：提示
@Composable
fun NameInputField(name: String, onNameChange: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = { newName ->
            if (newName.length <= 20) onNameChange(newName)
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
}

// 2. 统一ID生成时机
fun loadContact(contactId: String?) {
    if (contactId.isNullOrBlank()) {
        val newId = UUID.randomUUID().toString()
        _uiState.update { it.copy(contactId = newId) }
    } else {
        _uiState.update { it.copy(contactId = contactId) }
    }
}

fun startEdit() {
    val currentId = _uiState.value.contactId
    _uiState.update {
        it.copy(editedProfile = it.profile.copy(id = currentId))
    }
}
```

---

## 10. AI交互问题

### 10.1 常见问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| 自定义提示词未生效 | 硬编码覆盖参数 | 合并提示词 |
| 提示词重叠 | 追加而非覆盖语义 | 条件互斥逻辑 |
| 三种模式上下文不共通 | 缺少会话上下文服务 | SessionContextService |
| JSON解析失败 | 缺少容错解析 | 先严格后宽松 |

### 10.2 最佳实践

```kotlin
// 1. 合并提示词（覆盖语义）
fun buildSystemInstruction(scene: PromptScene, contactId: String?): String {
    val globalPrompt = promptRepository.getGlobalPrompt(scene)
    val contactPrompt = contactId?.let { promptRepository.getContactPrompt(scene, it) }

    return when {
        !contactPrompt.isNullOrBlank() -> contactPrompt  // 专属优先
        globalPrompt.isNotBlank() -> globalPrompt        // 全局兜底
        else -> DefaultPrompts.getDefaultPrompt(scene)   // 系统默认
    }
}

// 2. 会话上下文服务
class SessionContextService {
    private var currentContext: ConversationContext? = null
    fun updateContext(contactId: String, userMessage: String) {
        currentContext = ConversationContext(contactId, userMessage, System.currentTimeMillis())
    }
    fun getContext(): ConversationContext? = currentContext
}

// 3. 容错解析
private fun parseTagUpdates(response: String): List<TagUpdate> {
    return try {
        moshi.adapter<List<TagUpdate>>().fromJson(response) ?: emptyList()
    } catch (e: Exception) {
        parseTagUpdatesWithDefault(response)  // 容错
    }
}
```

---

## 11. 最佳实践检查清单

### 11.1 流式响应开发

- [ ] 流式完成后延迟800ms清空streamingContent
- [ ] 使用单一Flow收集器，避免多Flow冲突
- [ ] 在StreamingState.Completed中同时更新Block和Message
- [ ] stopGeneration后保留"[用户已停止生成]"提示
- [ ] 使用relatedUserMessageId建立消息关联

### 11.2 异步操作

- [ ] withContext(Dispatchers.IO)完整包裹所有IO操作
- [ ] 网络请求和响应处理在同一线程上下文
- [ ] Flow收集器正确取消和重启
- [ ] 添加状态锁防止重复点击
- [ ] 使用delay(200)等待Flow更新

### 11.3 数据持久化

- [ ] 为数据模型添加稳定的主键（UUID）
- [ ] 使用自定义JsonAdapter处理默认值字段
- [ ] Room Migration脚本与Entity定义完全匹配
- [ ] ALTER TABLE ADD COLUMN添加NOT NULL约束
- [ ] 编写Migration测试用例

### 11.4 生命周期管理

- [ ] 应用级服务在Application.onCreate()中恢复
- [ ] 状态保存时机在操作前而非操作后
- [ ] 幂等性保护防止重复启动
- [ ] 完整传递所有导航回调参数

### 11.5 UI开发

- [ ] 使用自定义MaxHeightScrollView限制高度
- [ ] 避免与系统属性命名冲突（layoutParams等）
- [ ] 使用ViewBinding替代findViewById
- [ ] LazyList的key全局唯一
- [ ] LaunchedEffect不依赖频繁变化的状态

---

## 12. 常见错误模式

### 12.1 状态管理错误模式

```kotlin
// ❌ 错误模式
class BadViewModel : ViewModel() {
    private var state: String = ""  // 内存状态不可靠
    fun update() { state = "new" }  // 无持久化
}

// ✅ 正确模式
class GoodViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(Data())
    val uiState: StateFlow<Data> = _uiState.asStateFlow()

    fun update() = viewModelScope.launch {
        repository.save(_uiState.value)  // 持久化
        _uiState.update { it.copy() }
    }
}
```

### 12.2 异步操作错误模式

```kotlin
// ❌ 错误模式
suspend fun badAsync() {
    withContext(Dispatchers.IO) {
        doNetworkRequest()  // IO操作
    }
    doSomethingWithResult()  // 主线程执行！
}

// ✅ 正确模式
suspend fun goodAsync(): Result {
    return withContext(Dispatchers.IO) {
        val response = doNetworkRequest()
        doSomethingWithResult(response)  // 全部在IO线程
    }
}
```

### 12.3 序列化错误模式

```kotlin
// ❌ 错误模式
data class BadFact(val id: String = UUID.randomUUID().toString(), val content: String)

// ✅ 正确模式
data class GoodFact(val id: String, val content: String) {
    companion object {
        fun create(content: String) = GoodFact(id = UUID.randomUUID().toString(), content = content)
    }
}
```

---

## 13. 调试技巧

### 13.1 日志分类

```kotlin
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

    fun logLifecycle(method: String, state: String) {
        Log.d("Lifecycle", "[$method] state: $state")
    }
}
```

### 13.2 调试策略

| 场景 | 调试方法 |
|------|----------|
| 流式响应问题 | 添加StreamingState日志 |
| 异步时序问题 | 添加delay(200)观察Flow更新 |
| 数据库迁移 | RoomMigrationTest |
| UI渲染问题 | 添加重组日志 |
| 状态丢失 | 添加生命周期日志 |

### 13.3 常用调试命令

```bash
# AI调试日志
scripts\ai-debug.bat

# 完整AI日志（包含提示词）
scripts\ai-debug-full.bat

# 查看最近错误
scripts\quick-error.bat 100

# Logcat过滤
adb logcat -s "Streaming:Regenerate:Migration:Async:Lifecycle"
```

---

## 参考文档

| 文档路径 | 描述 |
|----------|------|
| `文档/开发文档/BUG/` | 所有BUG分析文档 |
| `文档/开发文档/QUICK-REF/QUICK-REF-BUG-ANALYSIS.md` | BUG系统性分析 |
| `Rules/CORE_PRINCIPLES.md` | 核心开发原则 |

---

**文档版本**: v1.0
**创建日期**: 2026-01-06
**维护者**: Claude (AI Assistant)
**覆盖范围**: BUG-00001 ~ BUG-00048
**下次更新**: 重大BUG修复后
