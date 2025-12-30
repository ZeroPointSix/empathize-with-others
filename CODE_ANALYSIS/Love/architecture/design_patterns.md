# 设计模式实现分析

> **分析日期**: 2025-12-29
> **分析范围**: 项目中使用的所有设计模式

---

## 目录

1. [Repository模式](#repository模式)
2. [Use Case模式](#use-case模式)
3. [MVVM模式](#mvvm模式)
4. [依赖注入（DI）](#依赖注入di)
5. [单例模式](#单例模式)
6. [工厂模式](#工厂模式)
7. [观察者模式](#观察者模式)
8. [策略模式](#策略模式)

---

## Repository模式

### 定义

Repository模式是一种架构模式，用于将数据访问逻辑与业务逻辑分离。

### 实现方式

#### 接口定义（Domain层）

```kotlin
// domain/repository/ContactRepository.kt
package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ContactProfile
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getAllProfiles(): Flow<List<ContactProfile>>
    suspend fun getProfile(id: String): Result<ContactProfile?>
    suspend fun saveProfile(profile: ContactProfile): Result<Unit>
    suspend fun deleteProfile(id: String): Result<Unit>
}
```

#### 实现提供（Data层）

```kotlin
// data/repository/ContactRepositoryImpl.kt
package com.empathy.ai.data.repository

import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {

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

    // 私有转换方法
    private fun entityToDomain(entity: ContactProfileEntity): ContactProfile {
        return ContactProfile(
            id = entity.id,
            name = entity.name,
            // ...
        )
    }
}
```

#### 接口绑定（DI）

```kotlin
// data/di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        impl: ContactRepositoryImpl
    ): ContactRepository
}
```

### 使用示例

```kotlin
// Domain层UseCase使用接口
class GetAllContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository  // 依赖接口
) {
    operator fun invoke(): Flow<List<ContactProfile>> {
        return contactRepository.getAllProfiles()
    }
}
```

### 优势

1. **依赖倒置**：Domain层定义接口，Data层提供实现
2. **易于测试**：可以Mock Repository进行单元测试
3. **易于替换**：可以轻松切换数据源（如从Room切换到Hilt）
4. **职责分离**：数据访问逻辑集中在Repository中

### 项目中的Repository

| 接口 | 实现 | 文件路径 |
|------|------|----------|
| ContactRepository | ContactRepositoryImpl | domain/repository/ContactRepository.kt |
| AiRepository | AiRepositoryImpl | data/repository/AiRepositoryImpl.kt |
| BrainTagRepository | BrainTagRepositoryImpl | data/repository/BrainTagRepositoryImpl.kt |
| ConversationRepository | ConversationRepositoryImpl | data/repository/ConversationRepositoryImpl.kt |
| DailySummaryRepository | DailySummaryRepositoryImpl | data/repository/DailySummaryRepositoryImpl.kt |
| FailedTaskRepository | FailedTaskRepositoryImpl | data/repository/FailedTaskRepositoryImpl.kt |
| PromptRepository | PromptRepositoryImpl | data/repository/PromptRepositoryImpl.kt |
| TopicRepository | TopicRepositoryImpl | data/repository/TopicRepositoryImpl.kt |
| UserProfileRepository | UserProfileRepositoryImpl | data/repository/UserProfileRepositoryImpl.kt |

---

## Use Case模式

### 定义

Use Case模式（也称Interactor模式）用于封装单一的业务用例，协调多个Repository完成复杂业务逻辑。

### 实现方式

#### 基础UseCase

```kotlin
// domain/usecase/GetAllContactsUseCase.kt
package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    operator fun invoke(): Flow<List<ContactProfile>> {
        return contactRepository.getAllProfiles()
    }
}
```

#### 复杂UseCase（协调多个Repository）

```kotlin
// domain/usecase/AnalyzeChatUseCase.kt（387行）
package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.*
import com.empathy.ai.domain.repository.*
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AnalyzeChatUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val brainTagRepository: BrainTagRepository,
    private val privacyRepository: PrivacyRepository,
    private val aiRepository: AiRepository,
    private val settingsRepository: SettingsRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val conversationRepository: ConversationRepository,
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(
        contactId: String,
        rawScreenContext: List<String>
    ): Result<AnalysisResult> = coroutineScope {
        // 1. 并行加载数据
        val profileDeferred = async { contactRepository.getProfile(contactId) }
        val tagsDeferred = async { brainTagRepository.getTagsForContact(contactId).first() }
        val settingsDeferred = async { settingsRepository.getDataMaskingEnabled() }

        // 2. 等待数据加载
        val profile = profileDeferred.await().getOrNull()
        val brainTags = tagsDeferred.await()
        val dataMaskingEnabled = settingsDeferred.await()

        // 3. 数据脱敏
        val maskedContext = if (dataMaskingEnabled) {
            cleanedContext.map { privacyRepository.maskText(it) }
        } else {
            cleanedContext
        }

        // 4. 构建Prompt
        val systemInstruction = PromptBuilder.buildWithTopic(...)

        // 5. AI推理
        val analysisResult = aiRepository.analyzeChat(...)

        // 6. 保存对话记录
        conversationRepository.saveUserInput(...)

        analysisResult
    }
}
```

### 使用示例

```kotlin
// ViewModel中调用
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase
) : ViewModel() {

    fun analyzeChat(contactId: String, screenContext: List<String>) {
        viewModelScope.launch {
            val result = analyzeChatUseCase(contactId, screenContext)

            result.onSuccess { analysisResult ->
                // 处理成功
            }.onFailure { error ->
                // 处理失败
            }
        }
    }
}
```

### 优势

1. **业务逻辑封装**：业务逻辑集中在UseCase中
2. **可复用**：UseCase可以被多个ViewModel调用
3. **易于测试**：可以Mock Repository进行测试
4. **职责单一**：每个UseCase负责一个业务用例

### 项目中的UseCase

| UseCase | 职责 | 复杂度 |
|---------|------|--------|
| AnalyzeChatUseCase | 分析聊天 | 高（387行，协调7个Repository） |
| PolishDraftUseCase | 润色草稿 | 中 |
| GenerateReplyUseCase | 生成回复 | 中 |
| CheckDraftUseCase | 检查草稿 | 中 |
| GetAllContactsUseCase | 获取所有联系人 | 低 |
| GetContactUseCase | 获取单个联系人 | 低 |

---

## MVVM模式

### 定义

MVVM（Model-View-ViewModel）是一种UI架构模式，用于分离UI逻辑和业务逻辑。

### 架构图

```
┌─────────────────────────────────────┐
│         View (Compose UI)          │
│  - StateFlow.collectAsState()       │
│  - onEvent() 发送事件               │
└──────────────┬──────────────────────┘
               ↓ UI事件
┌─────────────────────────────────────┐
│         ViewModel                   │
│  - private val _uiState: Mutable... │
│  - val uiState: StateFlow<...>      │
│  - fun onEvent(event) {...}         │
└──────────────┬──────────────────────┘
               ↓ 调用
┌─────────────────────────────────────┐
│         UseCase / Model             │
│  - 业务逻辑                         │
│  - 数据访问                         │
└─────────────────────────────────────┘
```

### 实现方式

#### UiState定义

```kotlin
// presentation/viewmodel/ContactListViewModel.kt
data class ContactListUiState(
    val contacts: List<ContactProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)
```

#### UiEvent定义

```kotlin
// presentation/viewmodel/ContactListViewModel.kt
sealed class ContactListUiEvent {
    data class DeleteContact(val contactId: String) : ContactListUiEvent()
    data class SearchContacts(val query: String) : ContactListUiEvent()
    object RefreshContacts : ContactListUiEvent()
    object ClearError : ContactListUiEvent()
}
```

#### ViewModel实现

```kotlin
// presentation/viewmodel/ContactListViewModel.kt
@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getAllContactsUseCase: GetAllContactsUseCase,
    private val deleteContactUseCase: DeleteContactUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactListUiState())
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getAllContactsUseCase().collect { contacts ->
                _uiState.update { currentState ->
                    currentState.copy(
                        contacts = contacts,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onEvent(event: ContactListUiEvent) {
        when (event) {
            is ContactListUiEvent.DeleteContact -> deleteContact(event.contactId)
            is ContactListUiEvent.SearchContacts -> searchContacts(event.query)
            is ContactListUiEvent.RefreshContacts -> loadContacts()
            is ContactListUiEvent.ClearError -> clearError()
        }
    }

    private fun deleteContact(contactId: String) {
        viewModelScope.launch {
            deleteContactUseCase(contactId)
            // 删除后不需要刷新，Flow会自动推送更新
        }
    }
}
```

#### UI实现

```kotlin
// presentation/ui/screen/contact/ContactListScreen.kt
@Composable
fun ContactListScreen(
    modifier: Modifier = Modifier,
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("联系人") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ContactListUiEvent.RefreshContacts) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(uiState.contacts) { contact ->
                ContactCard(
                    contact = contact,
                    onDelete = { viewModel.onEvent(ContactListUiEvent.DeleteContact(it)) }
                )
            }
        }
    }
}
```

### 数据流

```
1. 用户点击删除按钮
   ↓
2. UI调用viewModel.onEvent(ContactListUiEvent.DeleteContact(id))
   ↓
3. ViewModel调用deleteContactUseCase(id)
   ↓
4. UseCase调用Repository删除数据
   ↓
5. Repository返回Result
   ↓
6. ViewModel更新_uiState（不需要，Flow自动推送）
   ↓
7. UI通过collectAsStateWithLifecycle()自动重新渲染
```

### 优势

1. **职责分离**：UI逻辑在View，业务逻辑在ViewModel
2. **响应式**：使用StateFlow自动推送UI更新
3. **易于测试**：可以Mock UseCase测试ViewModel
4. **生命周期感知**：使用collectAsStateWithLifecycle()

### 项目中的ViewModel

| ViewModel | 职责 | 文件路径 |
|-----------|------|----------|
| ContactListViewModel | 联系人列表 | presentation/viewmodel/ContactListViewModel.kt |
| ContactDetailViewModel | 联系人详情 | presentation/viewmodel/ContactDetailViewModel.kt |
| ChatViewModel | 聊天分析 | presentation/viewmodel/ChatViewModel.kt |
| SettingsViewModel | 设置 | presentation/viewmodel/SettingsViewModel.kt |
| AiConfigViewModel | AI配置 | presentation/viewmodel/AiConfigViewModel.kt |

---

## 依赖注入（DI）

### 定义

依赖注入是一种设计模式，用于实现控制反转（IoC），降低组件间的耦合度。

### 实现方式（Hilt）

#### 定义模块

```kotlin
// data/di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }
}
```

#### 绑定接口

```kotlin
// data/di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        impl: ContactRepositoryImpl
    ): ContactRepository
}
```

#### 使用@Qualifier

```kotlin
// app/di/AppDispatcherModule.kt
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppDispatcherModule {

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

// 使用
class SomeRepository @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    // ...
}
```

#### 注入依赖

```kotlin
// ViewModel中注入
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase,
    private val polishDraftUseCase: PolishDraftUseCase
) : ViewModel() {
    // ...
}

// Repository中注入
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    // ...
}

// UseCase中注入
class AnalyzeChatUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val aiRepository: AiRepository
) {
    // ...
}
```

### @Binds vs @Provides

| 特性 | @Binds | @Provides |
|------|--------|-----------|
| **用途** | 绑定接口与实现 | 提供实例 |
| **方式** | 抽象方法 | 具体方法 |
| **性能** | 编译时绑定，更快 | 运行时创建 |
| **代码量** | 更少 | 更多 |
| **适用场景** | 接口绑定 | 第三方库或复杂创建 |

**示例**：

```kotlin
// 使用@Binds（推荐用于接口绑定）
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindContactRepository(
        impl: ContactRepositoryImpl
    ): ContactRepository
}

// 使用@Provides（用于第三方库或复杂创建）
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(...).build()
    }
}
```

### 项目中的DI模块

**Data层（3个模块）**：
- DatabaseModule - Room数据库
- NetworkModule - Retrofit/OkHttp
- RepositoryModule - Repository绑定

**App层（11个模块）**：
- AppDispatcherModule - 协程调度器
- ServiceModule - 服务类
- FloatingWindowModule - 悬浮窗
- SummaryModule - 每日总结
- NotificationModule - 通知
- PersonaModule - 用户画像
- TopicModule - 对话主题
- LoggerModule - 日志
- UserProfileModule - 用户画像
- EditModule - 编辑功能
- FloatingWindowManagerModule - 悬浮窗管理器

---

## 单例模式

### 定义

单例模式确保一个类只有一个实例，并提供全局访问点。

### 实现方式

#### Kotlin object

```kotlin
// domain/service/PrivacyEngine.kt
object PrivacyEngine {
    fun maskHybrid(
        rawText: String,
        privacyMapping: Map<String, String> = emptyMap(),
        enabledPatterns: List<String> = emptyMap()
    ): String {
        // ...
    }
}
```

#### Hilt @Singleton

```kotlin
// data/repository/ContactRepositoryImpl.kt
@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    // ...
}
```

### 优势

1. **资源共享**：共享唯一实例
2. **延迟初始化**：Kotlin object是懒加载的
3. **线程安全**：Hilt @Singleton保证线程安全

### 项目中的单例

| 类型 | 实现方式 | 示例 |
|------|----------|------|
| 纯Kotlin单例 | object | PrivacyEngine |
| 依赖注入单例 | @Singleton | ContactRepositoryImpl |
| 系统服务 | @ApplicationContext | Application Context |

---

## 工厂模式

### 定义

工厂模式用于创建对象，而不暴露创建逻辑。

### 实现方式

#### Repository工厂（通过DI）

```kotlin
// data/di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindContactRepository(
        impl: ContactRepositoryImpl
    ): ContactRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        impl: AiRepositoryImpl
    ): AiRepository
}
```

#### 数据对象工厂

```kotlin
// domain/model/ContactProfile.kt
data class ContactProfile(
    val id: String,
    val name: String,
    val targetGoal: String,
    // ...
) {
    // 工厂方法
    fun copyWithNameEdit(newName: String): ContactProfile {
        return copy(
            name = newName,
            isNameUserModified = true
        )
    }

    fun copyWithGoalEdit(newGoal: String): ContactProfile {
        return copy(
            targetGoal = newGoal,
            isGoalUserModified = true
        )
    }
}
```

---

## 观察者模式

### 定义

观察者模式定义对象间的一对多依赖关系，当一个对象状态改变时，所有依赖者都会收到通知。

### 实现方式

#### Flow（Kotlin Coroutines）

```kotlin
// Repository返回Flow
interface ContactRepository {
    fun getAllProfiles(): Flow<List<ContactProfile>>
}

// ViewModel收集Flow
class ContactListViewModel @Inject constructor(
    private val getAllContactsUseCase: GetAllContactsUseCase
) : ViewModel() {

    init {
        viewModelScope.launch {
            getAllContactsUseCase().collect { contacts ->
                _uiState.update { it.copy(contacts = contacts) }
            }
        }
    }
}

// UI收集StateFlow
@Composable
fun ContactListScreen(
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // UI自动响应数据变化
    LazyColumn {
        items(uiState.contacts) { contact ->
            ContactCard(contact)
        }
    }
}
```

#### StateFlow

```kotlin
// ViewModel定义StateFlow
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun analyzeChat(...) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }

            val result = analyzeChatUseCase(...)

            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }
}

// UI收集StateFlow
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isAnalyzing) {
        CircularProgressIndicator()
    }
}
```

### 优势

1. **响应式**：数据变化自动推送
2. **生命周期感知**：collectAsStateWithLifecycle()自动管理
3. **背压处理**：Flow支持背压策略

---

## 策略模式

### 定义

策略模式定义一系列算法，封装每个算法，并使它们可以互换。

### 实现方式

#### AI服务商策略

```kotlin
// domain/model/AiProvider.kt
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val defaultModelId: String,
    val timeoutMs: Int = 30000
)

// data/repository/AiRepositoryImpl.kt
class AiRepositoryImpl @Inject constructor(
    private val api: OpenAiApi
) : AiRepository {

    override suspend fun analyzeChat(
        provider: AiProvider,  // 不同的provider是不同的策略
        promptContext: String,
        systemInstruction: String
    ): Result<AnalysisResult> {
        val url = buildChatCompletionsUrl(provider.baseUrl)
        val headers = mapOf("Authorization" to "Bearer ${provider.apiKey}")
        val request = ChatRequestDto(
            model = provider.defaultModelId,
            messages = listOf(...)
        )

        val response = api.chatCompletion(url, headers, request)
        return parseAnalysisResult(response)
    }
}
```

#### 脱敏策略

```kotlin
// domain/service/PrivacyEngine.kt
object PrivacyEngine {
    // 策略1：基于映射规则
    fun mask(rawText: String, privacyMapping: Map<String, String>): String {
        // ...
    }

    // 策略2：基于正则表达式
    fun maskByPattern(rawText: String, pattern: Regex, maskTemplate: String): String {
        // ...
    }

    // 策略3：混合策略
    fun maskHybrid(
        rawText: String,
        privacyMapping: Map<String, String> = emptyMap(),
        enabledPatterns: List<String> = emptyList()
    ): String {
        // 组合策略1和策略2
    }
}
```

### 优势

1. **算法族**：可以定义一系列算法
2. **可互换**：算法可以互相替换
3. **易于扩展**：添加新策略不需要修改现有代码

---

## 总结

### 设计模式使用统计

| 设计模式 | 使用频率 | 评级 |
|---------|----------|------|
| Repository | 13个接口+11个实现 | ⭐⭐⭐⭐⭐ |
| Use Case | 38个业务用例 | ⭐⭐⭐⭐⭐ |
| MVVM | 19个ViewModel | ⭐⭐⭐⭐⭐ |
| DI（Hilt） | 14个模块 | ⭐⭐⭐⭐⭐ |
| 单例模式 | 广泛使用 | ⭐⭐⭐⭐⭐ |
| 工厂模式 | 通过DI实现 | ⭐⭐⭐⭐ |
| 观察者模式 | Flow/StateFlow | ⭐⭐⭐⭐⭐ |
| 策略模式 | AI服务商/脱敏策略 | ⭐⭐⭐⭐ |

### 设计模式亮点

1. **Repository模式完美实现**：
   - 接口在domain，实现在data
   - 使用@Binds绑定，高效简洁

2. **Use Case模式运用得当**：
   - 38个业务用例封装
   - 协调多个Repository

3. **MVVM模式响应式**：
   - StateFlow + Compose
   - 单向数据流

4. **依赖注入完善**：
   - 14个Hilt模块
   - @Binds和@Provides使用得当

---

**分析完成时间**：2025-12-29
