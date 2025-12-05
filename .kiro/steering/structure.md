# 项目结构

## 语言规范

**所有文档和回答必须使用中文。** 代码注释、变量名、类名等保持英文，但所有说明文档、开发指南和与开发者的沟通必须使用中文。

## 架构模式

**Clean Architecture + MVVM** with strict layer separation and dependency rules.

## 包组织结构

```
com.empathy.ai/
├── app/                          # 应用入口
│   └── EmpathyApplication.kt     # Hilt 应用类
│
├── domain/                       # ✅ 领域层（纯 Kotlin，无 Android 依赖）
│   ├── model/                    # 业务实体
│   │   ├── AnalysisResult.kt
│   │   ├── BrainTag.kt
│   │   ├── ChatMessage.kt
│   │   ├── ContactProfile.kt
│   │   └── SafetyCheckResult.kt
│   ├── repository/               # 仓库接口
│   │   ├── AiRepository.kt
│   │   ├── BrainTagRepository.kt
│   │   ├── ContactRepository.kt
│   │   ├── PrivacyRepository.kt
│   │   └── SettingsRepository.kt
│   ├── usecase/                  # 业务逻辑用例
│   │   ├── AnalyzeChatUseCase.kt
│   │   ├── CheckDraftUseCase.kt
│   │   ├── FeedTextUseCase.kt
│   │   └── SaveProfileUseCase.kt
│   └── service/                  # 领域服务
│       ├── PrivacyEngine.kt
│       └── RuleEngine.kt
│
├── data/                         # 数据层（实现）
│   ├── local/                    # 本地存储
│   │   ├── AppDatabase.kt
│   │   ├── converter/            # Room 类型转换器
│   │   ├── dao/                  # 数据访问对象
│   │   └── entity/               # 数据库实体
│   ├── remote/                   # 网络层
│   │   ├── api/                  # Retrofit API 接口
│   │   └── model/                # DTO（数据传输对象）
│   └── repository/               # 仓库实现
│       └── settings/
│
├── presentation/                 # 表现层
│   ├── theme/                    # Compose 主题
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── ui/                       # UI 组件
│   │   ├── MainActivity.kt
│   │   └── screen/               # 功能屏幕
│   │       ├── chat/
│   │       └── contact/
│   └── viewmodel/                # ViewModel
│       ├── ChatViewModel.kt
│       ├── ContactDetailViewModel.kt
│       └── ContactListViewModel.kt
│
└── di/                           # 依赖注入
    ├── DatabaseModule.kt
    ├── NetworkModule.kt
    └── RepositoryModule.kt
```

## 层级职责

### 领域层（纯业务逻辑）
- **无 Android 依赖** - 可以在不依赖 Android 框架的情况下进行测试
- 包含业务模型、仓库接口、用例和领域服务
- 所有用例返回 `Result<T>` 以实现一致的错误处理
- 所有 IO 操作都是 `suspend` 函数

### 数据层（数据访问）
- 实现领域层的仓库接口
- Room 数据库用于本地存储，支持 Flow
- Retrofit 用于网络调用，使用 Moshi JSON 解析
- EncryptedSharedPreferences 用于敏感数据（API 密钥）
- 实体使用 `snake_case` 作为数据库列名，`camelCase` 作为 Kotlin 属性名

### 表现层（UI 和交互）
- Jetpack Compose 用于声明式 UI
- 使用 Hilt 注入的 ViewModel
- StateFlow 用于 UI 状态管理
- UiState 和 UiEvent 密封类用于类型安全的状态/事件处理

### DI 层（依赖注入）
- Hilt 模块用于提供依赖
- 为数据库、网络和仓库分别创建模块

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
- 始终使用 `@ColumnInfo(name = "...")` 来解耦 Kotlin 名称和 SQL

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
    │   └── local/converter/
    └── domain/
        ├── service/              # 服务测试
        └── usecase/              # 用例测试
```

测试文件镜像源代码结构，使用 `Test` 后缀：`AnalyzeChatUseCaseTest.kt`

## 文档

所有架构和开发文档位于 `docs/`：
- `00-项目概述/` - 项目概览
- `01-架构设计/` - 架构设计（数据层、服务层、UI 层）
- `02-开发指南/` - 开发指南
- `03-测试文档/` - 测试文档
