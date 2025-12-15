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

## 包组织结构

```
com.empathy.ai/
├── app/                                    # ✅ 应用入口
│   └── EmpathyApplication.kt           # Hilt 应用类
│
├── domain/                                 # ✅ 领域层（纯 Kotlin，无 Android 依赖）
│   ├── model/                            # ✅ 业务实体
│   │   ├── AnalysisResult.kt
│   │   ├── BrainTag.kt
│   │   ├── ChatMessage.kt
│   │   ├── ContactProfile.kt
│   │   ├── SafetyCheckResult.kt
│   │   ├── AiProvider.kt
│   │   ├── AiModel.kt
│   │   ├── ActionType.kt
│   │   ├── FloatingWindowError.kt
│   │   ├── MinimizedRequestInfo.kt
│   │   ├── MinimizeError.kt
│   │   └── ExtractedData.kt
│   ├── repository/                        # ✅ 仓库接口
│   │   ├── AiRepository.kt
│   │   ├── BrainTagRepository.kt
│   │   ├── ContactRepository.kt
│   │   ├── PrivacyRepository.kt
│   │   └── SettingsRepository.kt
│   ├── usecase/                          # ✅ 业务逻辑用例
│   │   ├── AnalyzeChatUseCase.kt
│   │   ├── CheckDraftUseCase.kt
│   │   ├── FeedTextUseCase.kt
│   │   ├── SaveProfileUseCase.kt
│   │   ├── GetAllContactsUseCase.kt
│   │   ├── GetContactUseCase.kt
│   │   ├── DeleteContactUseCase.kt
│   │   ├── DeleteBrainTagUseCase.kt
│   │   ├── SaveBrainTagUseCase.kt
│   │   ├── SaveProviderUseCase.kt
│   │   ├── DeleteProviderUseCase.kt
│   │   ├── GetProvidersUseCase.kt
│   │   └── TestConnectionUseCase.kt
│   ├── service/                          # ✅ 领域服务
│   │   ├── PrivacyEngine.kt
│   │   ├── RuleEngine.kt
│   │   └── FloatingWindowService.kt
│   └── util/                            # ✅ 领域工具类
│       ├── ErrorHandler.kt
│       ├── ErrorMapper.kt
│       ├── FallbackStrategy.kt
│       ├── FloatingView.kt
│       ├── FloatingWindowManager.kt
│       ├── OperationExecutor.kt
│       ├── PerformanceMonitor.kt
│       ├── PerformanceTracker.kt
│       ├── RetryConfig.kt
│       └── WeChatDetector.kt
│
├── data/                                   # ✅ 数据层（实现）
│   ├── local/                          # ✅ 本地存储
│   │   ├── AppDatabase.kt              # Room 数据库配置
│   │   ├── ApiKeyStorage.kt
│   │   ├── FloatingWindowPreferences.kt
│   │   ├── converter/                # ✅ Room 类型转换器
│   │   │   └── RoomTypeConverters.kt
│   │   ├── dao/                    # ✅ 数据访问对象
│   │   │   ├── AiProviderDao.kt
│   │   │   ├── BrainTagDao.kt
│   │   │   └── ContactDao.kt
│   │   └── entity/                 # ✅ 数据库实体
│   │       ├── AiProviderEntity.kt
│   │       ├── BrainTagEntity.kt
│   │       └── ContactProfileEntity.kt
│   ├── remote/                         # ✅ 网络层
│   │   ├── api/                    # ✅ Retrofit API 接口
│   │   │   └── OpenAiApi.kt
│   │   └── model/                  # ✅ DTO（数据传输对象）
│   │       ├── ChatRequestDto.kt
│   │       ├── ChatResponseDto.kt
│   │       └── MessageDto.kt
│   ├── repository/                     # ✅ 仓库实现
│   │   ├── AiRepositoryImpl.kt
│   │   ├── BrainTagRepositoryImpl.kt
│   │   ├── ContactRepositoryImpl.kt
│   │   ├── PrivacyRepositoryImpl.kt
│   │   ├── AiProviderRepositoryImpl.kt
│   │   └── settings/
│   │       └── SettingsRepositoryImpl.kt
│   └── parser/                         # ✅ AI响应解析器
│       ├── AiResponseParser.kt
│       ├── EnhancedJsonCleaner.kt
│       ├── FallbackHandler.kt
│       ├── FieldMapper.kt
│       └── JsonCleaner.kt
│
├── presentation/                            # ✅ 表现层
│   ├── navigation/                     # ✅ 导航系统
│   │   ├── NavGraph.kt
│   │   └── NavRoutes.kt
│   ├── theme/                          # ✅ Compose 主题
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── ui/                             # ✅ UI 组件
│   │   ├── MainActivity.kt
│   │   ├── component/               # ✅ 可复用组件
│   │   │   ├── button/
│   │   │   │   ├── PrimaryButton.kt
│   │   │   │   └── SecondaryButton.kt
│   │   │   ├── card/
│   │   │   │   ├── AnalysisCard.kt
│   │   │   │   ├── ProfileCard.kt
│   │   │   │   └── ProviderCard.kt
│   │   │   ├── chip/
│   │   │   │   └── TagChip.kt
│   │   │   ├── dialog/
│   │   │   │   ├── AddContactDialog.kt
│   │   │   │   ├── AddTagDialog.kt
│   │   │   │   ├── DeleteTagConfirmDialog.kt
│   │   │   │   ├── PermissionRequestDialog.kt
│   │   │   │   └── ProviderFormDialog.kt
│   │   │   ├── input/
│   │   │   │   ├── ContactSearchBar.kt
│   │   │   │   ├── CustomTextField.kt
│   │   │   │   └── TagSearchBar.kt
│   │   │   ├── list/
│   │   │   │   └── ContactListItem.kt
│   │   │   ├── message/
│   │   │   │   └── MessageBubble.kt
│   │   │   └── state/
│   │   │       ├── EmptyView.kt
│   │   │       ├── ErrorView.kt
│   │   │       └── LoadingIndicator.kt
│   │   └── screen/               # ✅ 功能屏幕
│   │       ├── aiconfig/
│   │       │   ├── AiConfigScreen.kt
│   │       │   ├── AiConfigUiState.kt
│   │       │   └── AiConfigUiEvent.kt
│   │       ├── chat/
│   │       │   ├── ChatScreen.kt
│   │       │   ├── ChatUiState.kt
│   │       │   └── ChatUiEvent.kt
│   │       ├── contact/
│   │       │   ├── ContactListScreen.kt
│   │       │   ├── ContactListUiState.kt
│   │       │   ├── ContactListUiEvent.kt
│   │       │   ├── ContactDetailScreen.kt
│   │       │   ├── ContactDetailUiState.kt
│   │       │   └── ContactDetailUiEvent.kt
│   │       ├── settings/
│   │       │   ├── SettingsScreen.kt
│   │       │   ├── SettingsUiState.kt
│   │       │   └── SettingsUiEvent.kt
│   │       └── tag/
│   │           ├── BrainTagScreen.kt
│   │           ├── BrainTagUiState.kt
│   │           └── BrainTagUiEvent.kt
│   └── viewmodel/                    # ✅ ViewModel
│       ├── BaseViewModel.kt
│       ├── AiConfigViewModel.kt
│       ├── BrainTagViewModel.kt
│       ├── ChatViewModel.kt
│       ├── ContactDetailViewModel.kt
│       ├── ContactListViewModel.kt
│       └── SettingsViewModel.kt
│
└── di/                              # ✅ 依赖注入
    ├── DatabaseModule.kt
    ├── NetworkModule.kt
    ├── RepositoryModule.kt
    └── ServiceModule.kt
```

## 层级职责

### 领域层（纯业务逻辑）
- **✅ 无 Android 依赖** - 可以在不依赖 Android 框架的情况下进行测试
- **✅ 包含业务模型、仓库接口、用例和领域服务**
- **✅ 所有用例返回 `Result<T>` 以实现一致的错误处理**
- **✅ 所有 IO 操作都是 `suspend` 函数**

### 数据层（数据访问）
- **✅ 实现领域层的仓库接口**
- **✅ Room 数据库用于本地存储，支持 Flow**
- **✅ Retrofit 用于网络调用，使用 Moshi JSON 解析**
- **✅ EncryptedSharedPreferences 用于敏感数据（API 密钥）**
- **✅ 实体使用 `snake_case` 作为数据库列名，`camelCase` 作为 Kotlin 属性名**

### 表现层（UI 和交互）
- **✅ Jetpack Compose 用于声明式 UI**
- **✅ 使用 Hilt 注入的 ViewModel**
- **✅ StateFlow 用于 UI 状态管理**
- **✅ UiState 和 UiEvent 密封类用于类型安全的状态/事件处理**

### DI 层（依赖注入）
- **✅ Hilt 模块用于提供依赖**
- **✅ 为数据库、网络和仓库分别创建模块**

## 命名规范

### 文件
- **PascalCase** 用于所有 Kotlin 文件：`ContactProfile.kt`
- **Entity 后缀** 用于数据库实体：`ContactProfileEntity.kt`
- **ViewModel 后缀**：`ChatViewModel.kt`
- **UiState 后缀**：`ChatUiState.kt`
- **UiEvent 后缀**：`ChatUiEvent.kt`
- **UseCase 后缀**：`AnalyzeChatUseCase.kt`

### 数据库
- **表名**：`snake_case` 复数形式：`contact_profiles`、`brain_tags`、`ai_providers`
- **列名**：`snake_case`：`contact_id`、`tag_type`
- **始终使用 `@ColumnInfo(name = "...")` 来解耦 Kotlin 名称和 SQL**

### Kotlin
- **属性**：`camelCase`：`contactId`、`tagType`
- **常量**：`UPPER_SNAKE_CASE`：`MAX_RETRY_COUNT`
- **Composable**：`PascalCase`：`ChatScreen`、`MessageBubble`

## 关键模式

### 仓库模式
```kotlin
// 领域层定义接口
interface ContactRepository {
    fun getAllProfiles(): Flow<List<ContactProfile>>
    suspend fun insertProfile(profile: ContactProfile): Result<Unit>
}

// 数据层实现
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    // 实现包含 Entity <-> Domain 映射
}
```

### 用例模式
```kotlin
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
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    fun onEvent(event: ChatUiEvent) { /* 处理事件 */ }
}
```

## 测试结构

```
test/
└── com/empathy/ai/
    ├── data/
    │   ├── local/converter/
    │   └── repository/
    ├── domain/
    │   ├── service/
    │   ├── usecase/
    │   └── util/
    ├── integration/
    ├── presentation/
    │   ├── integration/
    ├── viewmodel/
    └── performance/
```

测试文件镜像源代码结构，使用 `Test` 后缀：`AnalyzeChatUseCaseTest.kt`

## 文档

所有架构和开发文档位于 `docs/`：
- `00-项目概述/` - 项目概览
- `01-架构设计/` - 架构设计（数据层、服务层、UI 层）
- `02-开发指南/` - 开发指南
- `03-测试文档/` - 测试文档

## 当前实现状态

### ✅ 完全实现的模块
- **领域层**: 100%完成，所有模型、接口、用例、服务完整实现
- **数据层**: 100%完成，Room数据库、网络层、仓库实现完整
- **表现层**: 100%完成，UI组件、屏幕、ViewModel完整实现
- **依赖注入**: 100%完成，Hilt模块完整配置
- **测试覆盖**: 99.1% (113/114测试通过)
- **联系人画像记忆系统UI**: 100%完成，四标签页架构完整实现
  - ✅ 概览标签页：关系进展追踪、最新事实钩子卡片
  - ✅ 事实流标签页：时间线视图、列表视图切换
  - ✅ 标签画像标签页：标签确认/驳回功能
  - ✅ 资料库标签页：联系人详细信息展示
  - ✅ 情感化背景：根据关系分数变化的动态背景
  - ✅ 性能优化：自动降级机制、内存管理

### ⚠️ 部分实现/待验证模块
- **AI响应解析**: AiResponseParser接口已定义，但实现可能不完整
- **媒体处理**: FeedTextUseCase已实现，但AiRepositoryImpl中transcribeMedia方法未实现
  - 代码架构已设计：ExtractedData模型、AiRepository接口定义
  - ❌ 实际实现：AiRepositoryImpl.transcribeMedia直接返回未实现异常
  - 需要集成：FFmpeg音视频处理、ASR语音识别、OCR文字识别
- **规则引擎**: RuleEngine功能完整，但与实际业务流程的集成状态不明
- **悬浮窗服务**: FloatingWindowService代码庞大，需要验证与UI层的实际集成
- **无障碍服务**: WeChatDetector等工具类存在，但实际集成状态不明