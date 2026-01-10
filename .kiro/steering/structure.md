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

## 多模块架构 (TD-00017 Clean Architecture模块化改造)

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
| AiAdvisorModule | :app | AI军师模块 |
| ProxyModule | :app | 代理配置 |
| ApiUsageModule | :app | API用量统计 |
| SystemPromptModule | :app | 系统提示词 |
| FloatingWindowManagerModule | :app | 悬浮窗管理器 |

---

## 包组织结构

### :domain 模块 (纯Kotlin)

```
domain/src/main/kotlin/com/empathy/ai/domain/
├── model/                    # 业务实体
│   ├── ActionType.kt
│   ├── AiModel.kt
│   ├── AiProvider.kt
│   ├── AiResult.kt
│   ├── AnalysisResult.kt
│   ├── AppError.kt
│   ├── BrainTag.kt
│   ├── ChatMessage.kt
│   ├── ContactProfile.kt
│   ├── PromptScene.kt        # 提示词场景（4个核心场景）
│   ├── GlobalPromptConfig.kt # 全局提示词配置（v3）
│   ├── AiAdvisorSession.kt   # AI军师会话模型
│   ├── AiAdvisorConversation.kt  # AI军师对话模型
│   ├── AiAdvisorMessageBlock.kt  # AI军师消息块模型
│   └── ...
├── repository/               # 仓库接口
│   ├── AiProviderRepository.kt
│   ├── AiRepository.kt
│   ├── BrainTagRepository.kt
│   ├── ContactRepository.kt
│   ├── ConversationRepository.kt
│   ├── DailySummaryRepository.kt
│   ├── PromptRepository.kt
│   ├── AiAdvisorRepository.kt
│   └── ...                   # 其他仓库接口
├── usecase/                  # 业务用例
│   ├── AnalyzeChatUseCase.kt
│   ├── PolishDraftUseCase.kt
│   ├── GenerateReplyUseCase.kt
│   ├── ManualSummaryUseCase.kt
│   ├── SendAdvisorMessageUseCase.kt
│   ├── CreateAdvisorSessionUseCase.kt
│   ├── GetAdvisorSessionsUseCase.kt
│   ├── GetAdvisorConversationsUseCase.kt
│   ├── DeleteAdvisorConversationUseCase.kt
│   └── ...                   # 其他UseCase
├── service/                  # 领域服务
│   ├── PrivacyEngine.kt
│   └── SessionContextService.kt
└── util/                     # 领域工具类
    ├── Logger.kt
    ├── PromptBuilder.kt
    ├── PromptSanitizer.kt
    ├── PromptValidator.kt
    ├── PromptVariableResolver.kt
    ├── IdentityPrefixHelper.kt
    ├── PerformanceMetrics.kt
    └── ...                   # 其他工具类
```

### :data 模块 (Android Library)

```
data/src/main/kotlin/com/empathy/ai/data/
├── di/                       # DI模块
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   ├── MemoryModule.kt
│   ├── PromptModule.kt
│   ├── DispatcherModule.kt
│   ├── OkHttpClientFactory.kt
│   └── Qualifiers.kt
├── local/                    # 本地存储
│   ├── AppDatabase.kt        # Room数据库 v16
│   ├── ApiKeyStorage.kt
│   ├── FloatingWindowPreferences.kt
│   ├── PrivacyPreferences.kt
│   ├── PromptFileStorage.kt
│   ├── PromptFileBackup.kt
│   ├── AiAdvisorPreferences.kt
│   ├── ProxyPreferences.kt
│   ├── DeveloperModePreferences.kt
│   ├── SystemPromptStorage.kt
│   ├── DefaultPrompts.kt
│   ├── dao/                  # 数据访问对象
│   │   ├── AiProviderDao.kt
│   │   ├── BrainTagDao.kt
│   │   ├── ContactDao.kt
│   │   ├── ConversationLogDao.kt
│   │   ├── DailySummaryDao.kt
│   │   ├── FailedSummaryTaskDao.kt
│   │   ├── AiAdvisorDao.kt
│   │   ├── AiAdvisorMessageBlockDao.kt
│   │   ├── ConversationTopicDao.kt
│   │   └── ApiUsageDao.kt
│   ├── entity/               # 数据库实体
│   │   ├── AiProviderEntity.kt
│   │   ├── BrainTagEntity.kt
│   │   ├── ContactProfileEntity.kt
│   │   ├── ConversationLogEntity.kt
│   │   ├── DailySummaryEntity.kt
│   │   ├── FailedSummaryTaskEntity.kt
│   │   ├── AiAdvisorSessionEntity.kt
│   │   ├── AiAdvisorConversationEntity.kt
│   │   ├── AiAdvisorMessageBlockEntity.kt
│   │   ├── ConversationTopicEntity.kt
│   │   └── ApiUsageEntity.kt
│   └── converter/
│       ├── FactListConverter.kt
│       └── RoomTypeConverters.kt
├── remote/                   # 网络层
│   ├── api/OpenAiApi.kt
│   └── model/
│       ├── ChatRequestDto.kt
│       ├── ChatResponseDto.kt
│       ├── MessageDto.kt
│       ├── ModelsResponseDto.kt
│       └── AiSummaryResponseDto.kt
├── repository/               # 仓库实现
│   ├── AiProviderRepositoryImpl.kt
│   ├── AiRepositoryImpl.kt
│   ├── BrainTagRepositoryImpl.kt
│   ├── ContactRepositoryImpl.kt
│   ├── ConversationRepositoryImpl.kt
│   ├── DailySummaryRepositoryImpl.kt
│   ├── PromptRepositoryImpl.kt
│   ├── AiAdvisorRepositoryImpl.kt
│   ├── ApiUsageRepositoryImpl.kt
│   ├── PrivacyRepositoryImpl.kt
│   ├── TopicRepositoryImpl.kt
│   ├── UserProfileRepositoryImpl.kt
│   ├── SystemPromptRepositoryImpl.kt
│   ├── DeveloperModeRepositoryImpl.kt
│   ├── FailedTaskRepositoryImpl.kt
│   └── settings/
│       └── SettingsRepositoryImpl.kt
├── parser/                   # AI响应解析
│   ├── AiResponseParser.kt
│   ├── AiSummaryResponseParserImpl.kt
│   ├── EnhancedJsonCleaner.kt
│   ├── FallbackHandler.kt
│   ├── FieldMapper.kt
│   └── JsonCleaner.kt
└── util/                     # 数据层工具
    ├── AndroidLogger.kt
    ├── DebugLogger.kt
    ├── AiResponseCleaner.kt
    ├── ApiErrorHandler.kt
    └── BlockUpdateManager.kt
```

### :presentation 模块 (Android Library)

```
presentation/src/main/kotlin/com/empathy/ai/presentation/
├── navigation/               # 导航系统
│   ├── NavGraph.kt
│   ├── NavRoutes.kt
│   └── PromptEditorNavigation.kt
├── theme/                    # Compose主题
│   ├── Color.kt
│   ├── Theme.kt
│   ├── Type.kt
│   ├── AnimationSpec.kt
│   ├── Dimensions.kt
│   ├── Spacing.kt
│   ├── CategoryColorPalette.kt
│   ├── RelationshipColors.kt
│   ├── SemanticColors.kt
│   ├── AvatarColors.kt
│   ├── EmotionColors.kt
│   ├── MacaronTagColors.kt
│   └── CategoryBarColors.kt
├── ui/                       # UI组件
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
│   │   ├── topic/
│   │   ├── filter/
│   │   ├── tag/
│   │   ├── chart/
│   │   ├── timeline/
│   │   ├── persona/
│   │   ├── vault/
│   │   ├── animation/
│   │   ├── text/
│   │   ├── contact/
│   │   ├── factstream/
│   │   ├── ios/
│   │   └── navigation/
│   ├── floating/             # 悬浮窗组件
│   │   ├── FloatingBubbleView.kt
│   │   ├── FloatingViewV2.kt
│   │   ├── TabSwitcher.kt
│   │   ├── ResultCard.kt
│   │   └── RefinementOverlay.kt
│   └── screen/               # 功能屏幕
│       ├── MainScreen.kt
│       ├── aiconfig/
│       ├── chat/
│       ├── advisor/          # AI军师模块
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
├── viewmodel/                # ViewModel
│   ├── BaseViewModel.kt
│   ├── AiAdvisorEntryViewModel.kt
│   ├── AiAdvisorChatViewModel.kt
│   ├── AiConfigViewModel.kt
│   ├── BrainTagViewModel.kt
│   ├── ChatViewModel.kt
│   ├── ContactDetailTabViewModel.kt
│   ├── ContactDetailViewModel.kt
│   ├── ContactListViewModel.kt
│   ├── ContactSelectViewModel.kt
│   ├── ManualSummaryViewModel.kt
│   ├── PromptEditorViewModel.kt
│   ├── SessionHistoryViewModel.kt
│   ├── SettingsViewModel.kt
│   ├── TopicViewModel.kt
│   ├── UserProfileViewModel.kt
│   ├── UsageStatsViewModel.kt
│   └── DeveloperModeViewModel.kt
└── util/                     # 表现层工具
    ├── FilterTypeIcons.kt
    ├── FloatingWindowManagerStub.kt
    ├── ImageLoaderConfig.kt
    ├── DebugLogger.kt
    ├── ErrorMessageMapper.kt
    └── AdaptiveAnimationConfig.kt
```

### :app 模块 (Application)

```
app/src/main/java/com/empathy/ai/
├── app/
│   └── EmpathyApplication.kt  # Hilt应用类
│   └── SystemPromptConfigProvider.kt
├── di/                       # 应用级DI模块
│   ├── AppDispatcherModule.kt
│   ├── LoggerModule.kt
│   ├── ServiceModule.kt
│   ├── FloatingWindowModule.kt
│   ├── NotificationModule.kt
│   ├── SummaryModule.kt
│   ├── EditModule.kt
│   ├── PersonaModule.kt
│   ├── TopicModule.kt
│   ├── UserProfileModule.kt
│   ├── AiAdvisorModule.kt
│   ├── ProxyModule.kt
│   ├── ApiUsageModule.kt
│   ├── SystemPromptModule.kt
│   └── FloatingWindowManagerModule.kt
├── notification/
│   └── AiResultNotificationManager.kt
├── service/
│   └── FloatingWindowService.kt
└── domain/
    ├── service/
    │   └── FloatingWindowService.kt
    └── util/
        ├── ErrorHandler.kt
        ├── FloatingView.kt
        ├── FloatingViewDebugLogger.kt
        └── PerformanceMonitor.kt
└── util/
    └── AndroidFloatingWindowManager.kt
```

---

## 层级职责

### 领域层（:domain - 纯业务逻辑）
- **无 Android 依赖** - 纯Kotlin模块，可独立测试
- **包含业务模型、仓库接口、用例、领域服务和工具类**
- **所有用例返回 `Result<T>` 以实现一致的错误处理**
- **所有 IO 操作都是 `suspend` 函数**

### 数据层（:data - 数据访问）
- **实现领域层的仓库接口**
- **Room 数据库用于本地存储，支持 Flow**
- **Retrofit 用于网络调用，使用 Moshi JSON 解析**
- **EncryptedSharedPreferences 用于敏感数据（API 密钥）**
- **包含数据层DI模块**

### 表现层（:presentation - UI和交互）
- **Jetpack Compose 用于声明式 UI**
- **使用 Hilt 注入的 ViewModel**
- **StateFlow 用于 UI 状态管理**
- **UiState 和 UiEvent 密封类用于类型安全的状态/事件处理**
- **只依赖:domain模块，不依赖:data模块**

### 应用层（:app - 应用入口）
- **Hilt Application入口**
- **Android服务（FloatingWindowService）**
- **应用级DI模块**
- **聚合所有模块依赖**

---

## 命名规范

### 文件
- **PascalCase** 用于所有 Kotlin 文件
- **Entity 后缀** 用于数据库实体
- **ViewModel 后缀**
- **UiState 后缀**
- **UiEvent 后缀**
- **UseCase 后缀**

### 数据库
- **表名**：`snake_case` 复数形式
- **列名**：`snake_case`
- **始终使用 `@ColumnInfo(name = "...")` 来解耦 Kotlin 名称和 SQL**

### Kotlin
- **属性**：`camelCase`
- **常量**：`UPPER_SNAKE_CASE`
- **Composable**：`PascalCase`

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

测试文件镜像源代码结构，使用 `Test` 后缀

---

## 当前实现状态

### 完全实现的模块
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
| **:presentation** | 280 | 59 | 7 | 346 |
| **:app** | 27 | 141 | 26 | 194 |
| **总计** | **574** | **268** | **39** | **881** |

**项目整体统计**：
- 总Kotlin文件数：881个
- 主源码文件：574个
- 测试文件：307个（268单元 + 39 Android）

### 数据库版本历史

| 版本 | 更新内容 | 状态 |
|------|----------|------|
| v12 | 新增api_usage_records表，AI用量统计 | 已完成 |
| v13-v15 | 迭代优化 | 已完成 |
| v16 | 新增AI军师会话相关表 | 已完成 |

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

### 进行中的问题修复（2026-01-10）

| Bug ID | 问题描述 | 状态 |
|--------|----------|------|
| BUG-00058 | 新建会话功能失效问题 | 已修复，测试用例已验证 |
| BUG-00059 | 中断生成后重新生成消息角色错乱问题 | 已修复，测试用例已验证 |
| BUG-00060 | 会话管理增强需求 | 已修复，测试用例已验证 |
| BUG-00061 | 会话历史跳转失败问题 | 已修复，测试用例已验证 |
| BUG-00064 | AI手动总结功能未生效问题 | 已修复，测试用例已验证 |
| BUG-00065 | 联系人搜索功能优化 | 进行中 |

**文档版本**: 2.19
**最后更新**: 2026-01-10
**更新内容**:
- 更新模块测试文件统计（基于实际代码架构扫描）
- 更新BUG-00064状态为已修复
- 更新进行中的BUG修复列表
