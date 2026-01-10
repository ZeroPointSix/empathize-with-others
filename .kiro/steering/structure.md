# 项目结构

## 🔴 必读文档（开始工作前必须阅读）

**在开始任何工作之前，请务必先阅读以下文档：**

1. **[Rules/RulesReadMe.md](../../Rules/RulesReadMe.md)** - 项目通用规则和文档规范
2. **[WORKSPACE.md](../../WORKSPACE.md)** - 当前工作状态和任务协调

---

## 语言规范

**所有文档和回答必须使用中文。** 代码注释、变量名、类名等保持英文，但所有说明文档、开发指南和与开发者的沟通必须使用中文。

## 架构模式

**Clean Architecture + MVVM** with strict layer separation and dependency rules.

## 🆕 多模块架构 (TD-00017 Clean Architecture模块化改造)

> 2025-12-25 更新 - 项目已完成Clean Architecture多模块改造

### 模块结构

```
:domain/        # 纯Kotlin模块 - 领域层（无Android依赖）
:data/          # Android Library - 数据层（依赖:domain）
:presentation/  # Android Library - 表现层（依赖:domain）
:app/           # Application - 应用入口（依赖所有模块）
```

### 模块依赖关系

```
                    ┌─────────────┐
                    │    :app     │
                    │ (Application)│
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────────┐
    │  :data   │    │ :domain  │    │:presentation │
    │(Library) │    │ (Kotlin) │    │  (Library)   │
    └────┬─────┘    └──────────┘    └──────┬───────┘
         │               ▲                  │
         └───────────────┴──────────────────┘
```

### 模块职责

| 模块 | 类型 | 职责 | 依赖 |
|------|------|------|------|
| `:domain` | Kotlin Library | 业务模型、Repository接口、UseCase、领域服务、工具类 | 仅kotlinx.coroutines |
| `:data` | Android Library | Room数据库、Retrofit网络、Repository实现、DI模块 | :domain |
| `:presentation` | Android Library | Compose UI、ViewModel、Navigation、Theme | :domain |
| `:app` | Application | 应用入口、Android服务、应用级DI模块 | :domain, :data, :presentation |

### DI模块分布

| DI模块 | 所在模块 | 说明 |
|--------|----------|------|
| DatabaseModule | :data | Room数据库和DAO |
| NetworkModule | :data | Retrofit和OkHttp |
| RepositoryModule | :data | Repository接口绑定 |
| MemoryModule | :data | 记忆系统依赖 |
| PromptModule | :data | 提示词系统依赖 |
| DispatcherModule | :data | 协程调度器 |
| LoggerModule | :app | Logger接口绑定 |
| AppDispatcherModule | :app | 应用级协程调度器 |
| ServiceModule | :app | 领域服务 |
| FloatingWindowModule | :app | 悬浮窗UseCase |
| NotificationModule | :app | 通知系统 |
| SummaryModule | :app | 总结系统 |
| EditModule | :app | 编辑功能 |
| PersonaModule | :app | 画像功能 |
| TopicModule | :app | 主题功能 |
| UserProfileModule | :app | 用户画像功能 |

---

## 包组织结构

### :domain 模块 (纯Kotlin)

```
domain/src/main/kotlin/com/empathy/ai/domain/
├── model/                    # ✅ 业务实体（183个模型）
│   ├── ActionType.kt
│   ├── AiModel.kt
│   ├── AiProvider.kt
│   ├── AiResult.kt
│   ├── AnalysisResult.kt
│   ├── AppError.kt
│   ├── BrainTag.kt
│   ├── ChatMessage.kt
│   ├── ContactProfile.kt
│   ├── PromptScene.kt        # 提示词场景（已优化为4个核心场景）
│   ├── GlobalPromptConfig.kt # 全局提示词配置（v3）
│   ├── AiAdvisorSession.kt   # AI军师会话模型（v16新增）
│   └── ...                   # 其他模型
├── repository/               # ✅ 仓库接口（13+个）
│   ├── AiProviderRepository.kt
│   ├── AiRepository.kt
│   ├── BrainTagRepository.kt
│   ├── ContactRepository.kt
│   ├── ConversationRepository.kt
│   ├── DailySummaryRepository.kt
│   ├── FailedTaskRepository.kt
│   ├── FloatingWindowPreferencesRepository.kt
│   ├── PrivacyRepository.kt
│   ├── PromptRepository.kt
│   ├── SettingsRepository.kt
│   ├── TopicRepository.kt
│   ├── UserProfileRepository.kt
│   └── AiAdvisorRepository.kt   # AI军师仓库接口（v16新增）
├── usecase/                  # ✅ 业务用例（38个）
│   ├── AnalyzeChatUseCase.kt
│   ├── PolishDraftUseCase.kt
│   ├── GenerateReplyUseCase.kt
│   ├── ManualSummaryUseCase.kt
│   ├── AiAdvisorUseCases.kt    # AI军师相关UseCase（v16新增）
│   └── ...                  # 其他UseCase
├── service/                  # ✅ 领域服务（2个）
│   ├── PrivacyEngine.kt
│   └── SessionContextService.kt
└── util/                     # ✅ 领域工具类（29个）
    ├── Logger.kt             # 日志接口（无Android依赖）
    ├── PromptBuilder.kt
    ├── PromptSanitizer.kt
    ├── PromptValidator.kt
    ├── PromptVariableResolver.kt
    ├── IdentityPrefixHelper.kt # 身份前缀工具类
    ├── PerformanceMetrics.kt   # 性能指标工具类
    └── ...                  # 其他工具类
```

### :data 模块 (Android Library)

```
data/src/main/kotlin/com/empathy/ai/data/
├── di/                       # ✅ DI模块（7个）
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   ├── MemoryModule.kt
│   ├── PromptModule.kt
│   ├── DispatcherModule.kt
│   └── Qualifiers.kt
├── local/                    # ✅ 本地存储
│   ├── AppDatabase.kt        # Room数据库 v16
│   ├── ApiKeyStorage.kt
│   ├── FloatingWindowPreferences.kt
│   ├── PromptFileStorage.kt
│   ├── PromptFileBackup.kt
│   ├── UserProfileBackupManager.kt
│   ├── UserProfileCache.kt
│   ├── converter/
│   │   ├── FactListConverter.kt
│   │   └── RoomTypeConverters.kt
│   ├── dao/                  # ✅ 数据访问对象（8个）
│   │   ├── AiProviderDao.kt
│   │   ├── BrainTagDao.kt
│   │   ├── ContactDao.kt
│   │   ├── ConversationLogDao.kt
│   │   ├── ConversationTopicDao.kt
│   │   ├── DailySummaryDao.kt
│   │   ├── FailedSummaryTaskDao.kt
│   │   └── AiAdvisorDao.kt     # AI军师DAO（v16新增）
│   └── entity/               # ✅ 数据库实体（11个）
│       ├── AiProviderEntity.kt
│       ├── BrainTagEntity.kt
│       ├── ContactProfileEntity.kt
│       ├── ConversationLogEntity.kt
│       ├── ConversationTopicEntity.kt
│       ├── DailySummaryEntity.kt
│       ├── FailedSummaryTaskEntity.kt
│       ├── AiAdvisorSessionEntity.kt      # AI军师会话实体（v16新增）
│       ├── AiAdvisorConversationEntity.kt # AI军师对话实体（v16新增）
│       ├── AiAdvisorMessageBlockEntity.kt # AI军师消息块实体（v16新增）
│       └── ApiUsageRecordEntity.kt        # API用量统计实体（v12新增）
├── remote/                   # ✅ 网络层
│   ├── api/OpenAiApi.kt
│   └── model/
│       ├── ChatRequestDto.kt
│       ├── ChatResponseDto.kt
│       ├── MessageDto.kt
│       ├── ModelsResponseDto.kt
│       └── AiSummaryResponseDto.kt
├── repository/               # ✅ 仓库实现（14个）
│   ├── AiProviderRepositoryImpl.kt
│   ├── AiRepositoryImpl.kt
│   ├── BrainTagRepositoryImpl.kt
│   ├── ContactRepositoryImpl.kt
│   ├── ConversationRepositoryImpl.kt
│   ├── DailySummaryRepositoryImpl.kt
│   ├── FailedTaskRepositoryImpl.kt
│   ├── PrivacyRepositoryImpl.kt
│   ├── PromptRepositoryImpl.kt
│   ├── TopicRepositoryImpl.kt
│   ├── UserProfileRepositoryImpl.kt
│   ├── ProviderCompatibility.kt
│   └── settings/
│       └── SettingsRepositoryImpl.kt
│   └── advisor/
│       └── AiAdvisorRepositoryImpl.kt   # AI军师仓库实现（v16新增）
├── parser/                   # ✅ AI响应解析
│   ├── AiResponseParser.kt
│   ├── AiSummaryResponseParserImpl.kt
│   ├── EnhancedJsonCleaner.kt
│   ├── FallbackHandler.kt
│   ├── FieldMapper.kt
│   └── JsonCleaner.kt
└── util/                     # ✅ 数据层工具（3个）
    ├── AndroidLogger.kt      # Logger接口实现
    ├── DebugLogger.kt
    └── AiResponseCleaner.kt
```

### :presentation 模块 (Android Library)

```
presentation/src/main/kotlin/com/empathy/ai/presentation/
├── di/                       # ✅ DI模块（1个）
│   └── (Hilt组件级模块)
├── navigation/               # ✅ 导航系统
│   ├── NavGraph.kt
│   ├── NavRoutes.kt
│   └── PromptEditorNavigation.kt
├── theme/                    # ✅ Compose主题
│   ├── Color.kt
│   ├── Theme.kt
│   ├── Type.kt
│   ├── AnimationSpec.kt
│   ├── Dimensions.kt
│   ├── CategoryColorPalette.kt
│   ├── RelationshipColors.kt
│   └── SemanticColors.kt
├── ui/                       # ✅ UI组件（280个）
│   ├── MainActivity.kt
│   ├── component/            # 可复用组件
│   │   ├── MaxHeightScrollView.kt
│   │   ├── button/
│   │   ├── card/
│   │   ├── chip/
│   │   ├── control/
│   │   ├── dialog/
│   │   ├── emotion/
│   │   ├── input/
│   │   ├── list/
│   │   ├── message/
│   │   ├── relationship/
│   │   ├── state/
│   │   └── topic/
│   ├── floating/             # 悬浮窗组件
│   │   ├── FloatingBubbleView.kt
│   │   ├── FloatingViewV2.kt
│   │   ├── TabSwitcher.kt
│   │   ├── ResultCard.kt
│   │   └── RefinementOverlay.kt
│   └── screen/               # 功能屏幕
│       ├── aiconfig/
│       ├── chat/
│       ├── advisor/          # AI军师模块（v16新增）
│       │   ├── AiAdvisorChatScreen.kt
│       │   ├── AiAdvisorScreen.kt
│       │   ├── ContactSelectScreen.kt
│       │   └── SessionHistoryScreen.kt
│       ├── contact/
│       │   ├── ContactListScreen.kt
│       │   ├── ContactDetailScreen.kt
│       │   ├── ContactDetailTabScreen.kt
│       │   ├── overview/
│       │   ├── factstream/
│       │   ├── persona/
│       │   ├── summary/
│       │   └── vault/
│       ├── prompt/
│       ├── settings/
│       ├── tag/
│       └── userprofile/
├── viewmodel/                # ✅ ViewModel（17个）
│   ├── BaseViewModel.kt
│   ├── AiAdvisorEntryViewModel.kt
│   ├── AiAdvisorChatViewModel.kt      # AI军师聊天ViewModel（v16新增）
│   ├── AiConfigViewModel.kt
│   ├── BrainTagViewModel.kt
│   ├── ChatViewModel.kt
│   ├── ContactDetailTabViewModel.kt
│   ├── ContactDetailViewModel.kt
│   ├── ContactListViewModel.kt
│   ├── ContactSelectViewModel.kt
│   ├── ManualSummaryViewModel.kt
│   ├── PromptEditorViewModel.kt
│   ├── SessionHistoryViewModel.kt     # 会话历史ViewModel（v16新增）
│   ├── SettingsViewModel.kt
│   ├── TopicViewModel.kt
│   └── UserProfileViewModel.kt
└── util/                     # ✅ 表现层工具（3个）
    ├── FilterTypeIcons.kt
    ├── FloatingWindowManagerStub.kt
    ├── ImageLoaderConfig.kt
    └── DebugLogger.kt
```

### :app 模块 (Application)

```
app/src/main/java/com/empathy/ai/
├── app/
│   └── EmpathyApplication.kt # Hilt应用类
├── di/                       # ✅ 应用级DI模块（9个）
│   ├── AppDispatcherModule.kt
│   ├── LoggerModule.kt       # Logger绑定
│   ├── ServiceModule.kt
│   ├── FloatingWindowModule.kt
│   ├── NotificationModule.kt
│   ├── SummaryModule.kt
│   ├── EditModule.kt
│   ├── PersonaModule.kt
│   ├── TopicModule.kt
│   └── UserProfileModule.kt
├── notification/
│   └── AiResultNotificationManager.kt
├── service/
│   └── FloatingWindowService.kt
├── domain/
│   ├── service/
│   │   └── FloatingWindowService.kt
│   └── util/
│       ├── ErrorHandler.kt
│       ├── FloatingView.kt
│       ├── FloatingViewDebugLogger.kt
│       └── PerformanceMonitor.kt
└── util/
    └── AndroidFloatingWindowManager.kt
```

---

## 层级职责

### 领域层（:domain - 纯业务逻辑）
- **✅ 无 Android 依赖** - 纯Kotlin模块，可独立测试
- **✅ 包含业务模型、仓库接口、用例、领域服务和工具类**
- **✅ 所有用例返回 `Result<T>` 以实现一致的错误处理**
- **✅ 所有 IO 操作都是 `suspend` 函数**

### 数据层（:data - 数据访问）
- **✅ 实现领域层的仓库接口**
- **✅ Room 数据库用于本地存储，支持 Flow**
- **✅ Retrofit 用于网络调用，使用 Moshi JSON 解析**
- **✅ EncryptedSharedPreferences 用于敏感数据（API 密钥）**
- **✅ 包含数据层DI模块（DatabaseModule、NetworkModule、RepositoryModule等）**

### 表现层（:presentation - UI和交互）
- **✅ Jetpack Compose 用于声明式 UI**
- **✅ 使用 Hilt 注入的 ViewModel**
- **✅ StateFlow 用于 UI 状态管理**
- **✅ UiState 和 UiEvent 密封类用于类型安全的状态/事件处理**
- **✅ 只依赖:domain模块，不依赖:data模块**

### 应用层（:app - 应用入口）
- **✅ Hilt Application入口**
- **✅ Android服务（FloatingWindowService）**
- **✅ 应用级DI模块（ServiceModule、NotificationModule等）**
- **✅ 聚合所有模块依赖**

---

## 命名规范

### 文件
- **PascalCase** 用于所有 Kotlin 文件：`ContactProfile.kt`
- **Entity 后缀** 用于数据库实体：`ContactProfileEntity.kt`
- **ViewModel 后缀**：`ChatViewModel.kt`
- **UiState 后缀**：`ChatUiState.kt`
- **UiEvent 后缀**：`ChatUiEvent.kt`
- **UseCase 后缀**：`AnalyzeChatUseCase.kt`

### 数据库
- **表名**：`snake_case` 复数形式：`contact_profiles`、`brain_tags`
- **列名**：`snake_case`：`contact_id`、`tag_type`
- **始终使用 `@ColumnInfo(name = "...")` 来解耦 Kotlin 名称和 SQL**

### Kotlin
- **属性**：`camelCase`：`contactId`、`tagType`
- **常量**：`UPPER_SNAKE_CASE`：`MAX_RETRY_COUNT`
- **Composable**：`PascalCase`：`ChatScreen`、`MessageBubble`

---

## 关键模式

### 仓库模式
```kotlin
// :domain模块定义接口
interface ContactRepository {
    fun getAllProfiles(): Flow<List<ContactProfile>>
    suspend fun insertProfile(profile: ContactProfile): Result<Unit>
}

// :data模块实现
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    // 实现包含 Entity <-> Domain 映射
}
```

### 用例模式
```kotlin
// :domain模块
class AnalyzeChatUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(...): Result<AnalysisResult> {
        // 业务逻辑，使用 Result 包装
    }
}
```

### ViewModel 模式
```kotlin
// :presentation模块
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onEvent(event: ChatUiEvent) { /* 处理事件 */ }
}
```

---

## 测试结构

```
:domain/src/test/           # 领域层单元测试（纯JVM）
:data/src/test/             # 数据层单元测试
:data/src/androidTest/      # 数据库迁移测试
:presentation/src/test/     # ViewModel单元测试
:app/src/test/              # 应用层单元测试
:app/src/androidTest/       # 集成测试和UI测试
```

测试文件镜像源代码结构，使用 `Test` 后缀：`AnalyzeChatUseCaseTest.kt`

---

## 当前实现状态

### ✅ 完全实现的模块
- **:domain模块**: 100%完成
  - 业务模型
  - Repository接口
  - UseCase
  - 领域服务
  - 工具类
  - 无Android依赖
- **:data模块**: 100%完成
  - Room数据库v16
  - DI模块
  - DAO
  - Entity
  - Repository实现
  - Parser
  - 完整的工具类
- **:presentation模块**: 100%完成
  - UI组件
  - ViewModel
  - Navigation系统
  - Theme系统
- **:app模块**: 100%完成
  - 应用级DI模块
  - Android服务
  - 应用入口

### 模块文件统计（2026-01-10最新扫描）

| 模块 | 主源码 | 单元测试 | Android测试 | 总计 |
|------|--------|---------|------------|------|
| **:domain** | 183 | 43 | 0 | 226 |
| **:data** | 84 | 25 | 6 | 115 |
| **:presentation** | 280 | 57 | 7 | 344 |
| **:app** | 27 | 141 | 26 | 194 |
| **总计** | **574** | **266** | **39** | **879** |

**项目整体统计**：
- 总Kotlin文件数：879个
- 主源码文件：574个
- 测试文件：305个（266单元 + 39 Android）

### 数据库版本历史

| 版本 | 更新内容 | 状态 |
|------|----------|------|
| v12 | 新增api_usage_records表，AI用量统计 | 已完成 |
| v13-v15 | 迭代优化 | 已完成 |
| v16 | 新增AI军师会话相关表（ai_advisor_sessions, ai_advisor_conversations, ai_advisor_message_blocks） | 已完成 |

### AI军师模块（v16新增）

AI军师（心语助手）是一个独立的对话模块，提供：

- **会话管理**：创建、切换、删除会话
- **对话历史**：支持Markdown渲染的消息展示
- **流式响应**：支持打字机效果的消息生成
- **重新生成**：中断后可重新生成消息
- **联系人关联**：可选择联系人进行针对性对话

### 架构合规性
- **Clean Architecture**: ⭐⭐⭐⭐⭐ (A级，完全合规)
- **模块化**: ⭐⭐⭐⭐⭐ (A级，4模块架构)
- **依赖方向**: ⭐⭐⭐⭐⭐ (A级，严格单向依赖)

---

### 🔄 进行中的问题修复（2026-01-10）

| Bug ID | 问题描述 | 状态 |
|--------|----------|------|
| BUG-00058 | 新建会话功能失效问题 | 已修复，测试用例已验证 |
| BUG-00059 | 中断生成后重新生成消息角色错乱问题 | 已修复，测试用例已验证 |
| BUG-00060 | 会话管理增强需求 | 已修复，测试用例已验证 |
| BUG-00061 | 会话历史跳转失败问题 | 已修复，测试用例已验证 |
| BUG-00062 | AI军师会话管理功能增强 | 已识别，待实现 |
| BUG-00063 | 联系人搜索功能优化 | 已识别，待实现 |

**文档版本**: 2.17
**最后更新**: 2026-01-10
**更新内容**:
- 添加BUG-00064 AI手动总结功能修复到已解决列表
