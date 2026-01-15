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

> 2026-01-15 更新 - 基于实际代码架构扫描
>
> 当前分支: BUG-FIX（Bug修复分支）
> 当前工作: BUG-00073 OPPO真机悬浮球不显示问题修复
> 当前版本: v1.1.0 (versionCode: 10101, dev阶段)

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

**当前版本**: v1.1.0 (versionCode: 10101)
**发布阶段**: dev
**数据库版本**: Room v16

### DI模块分布（基于实际代码）

| DI模块 | 所在模块 | 说明 |
|--------|----------|------|
| **Data模块DI（8个）** |
| DatabaseModule | :data | Room数据库v16、16个迁移脚本、11个DAO |
| NetworkModule | :data | Retrofit、OkHttp、Moshi、SSE流式读取 |
| RepositoryModule | :data | 18个Repository接口绑定 |
| MemoryModule | :data | 内存缓存依赖 |
| PromptModule | :data | 提示词系统依赖 |
| DispatcherModule | :data | 协程调度器 |
| OkHttpClientFactory | :data | OkHttp工厂（代理支持） |
| Qualifiers | :data | 限定符定义 |
| **App模块DI（16个）** |
| LoggerModule | :app | Logger接口绑定 |
| AppDispatcherModule | :app | 应用级协程调度器 |
| ServiceModule | :app | Android服务注入 |
| FloatingWindowModule | :app | 悬浮窗组件 |
| NotificationModule | :app | 通知系统 |
| SummaryModule | :app | 总结功能 |
| EditModule | :app | 编辑功能 |
| PersonaModule | :app | 人设功能 |
| TopicModule | :app | 主题功能 |
| UserProfileModule | :app | 用户画像功能 |
| AiAdvisorModule | :app | AI军师模块 |
| ProxyModule | :app | 代理配置 |
| ApiUsageModule | :app | API用量统计 |
| SystemPromptModule | :app | 系统提示词 |
| FloatingWindowManagerModule | :app | 悬浮窗管理器 |

---

## 包组织结构（基于实际代码扫描）

### :domain 模块 (纯Kotlin)

```
domain/src/main/kotlin/com/empathy/ai/domain/
├── model/                    # 业务模型（96个文件）
│   ├── ActionType.kt
│   ├── AiAdvisorConversation.kt
│   ├── AiAdvisorSession.kt
│   ├── AiProvider.kt
│   ├── ContactProfile.kt
│   ├── BrainTag.kt
│   ├── PromptScene.kt        # 提示词场景（4个核心场景）
│   ├── GlobalPromptConfig.kt # 全局提示词配置（v3）
│   └── ...                   # 共96个模型文件
├── repository/               # 仓库接口（18个文件）
│   ├── AiProviderRepository.kt
│   ├── ContactRepository.kt
│   ├── AiRepository.kt
│   └── ...
├── usecase/                  # 业务用例（51个文件）
│   ├── AnalyzeChatUseCase.kt
│   ├── CreateAdvisorSessionUseCase.kt
│   ├── ManualSummaryUseCase.kt
│   ├── SendAdvisorMessageUseCase.kt
│   └── ...
├── service/                  # 领域服务（2个文件）
│   ├── PrivacyEngine.kt
│   └── SessionContextService.kt
└── util/                     # 工具类（29个文件）
    ├── Logger.kt
    ├── PromptBuilder.kt
    ├── PrivacyEngine.kt
    └── ...
```

### :data 模块 (Android Library)

```
data/src/main/kotlin/com/empathy/ai/data/
├── di/                       # DI模块（8个文件）
│   ├── DatabaseModule.kt     # Room v16 + 16个迁移脚本
│   ├── NetworkModule.kt      # Retrofit + OkHttp
│   ├── RepositoryModule.kt   # 18个Repository绑定
│   └── ...
├── local/                    # 本地存储
│   ├── AppDatabase.kt        # Room数据库 v16
│   ├── dao/                  # 数据访问对象（11个DAO）
│   │   ├── AiProviderDao.kt
│   │   ├── ContactDao.kt
│   │   ├── AiAdvisorDao.kt
│   │   └── ...
│   ├── entity/               # 数据库实体（11个Entity）
│   │   ├── AiProviderEntity.kt
│   │   ├── ContactProfileEntity.kt
│   │   └── ...
│   └── converter/            # 类型转换器
├── remote/                   # 网络层
│   ├── api/OpenAiApi.kt
│   ├── model/                # DTO模型
│   └── SseStreamReader.kt    # SSE流式读取器
├── repository/               # 仓库实现（18个文件）
│   ├── AiProviderRepositoryImpl.kt
│   ├── ContactRepositoryImpl.kt
│   └── ...
├── parser/                   # AI响应解析器（6个文件）
│   ├── AiResponseParser.kt
│   ├── EnhancedJsonCleaner.kt
│   └── ...
└── util/                     # 工具类
```

### :presentation 模块 (Android Library)

```
presentation/src/main/kotlin/com/empathy/ai/presentation/
├── navigation/               # 导航系统（5个文件）
│   ├── NavGraph.kt           # 主导航图
│   ├── NavRoutes.kt          # 路由常量定义（23个路由）
│   ├── BottomNavTab.kt       # 底部Tab枚举
│   ├── NonTabNavGraph.kt     # 非Tab导航容器
│   └── PromptEditorNavigation.kt
├── theme/                    # Compose主题（13个文件）
│   ├── Color.kt
│   ├── Type.kt
│   ├── AnimationSpec.kt
│   ├── Dimensions.kt
│   └── ...
├── viewmodel/                # ViewModel（27个文件）
│   ├── BaseViewModel.kt
│   ├── AiAdvisorChatViewModel.kt
│   ├── ContactDetailViewModel.kt
│   ├── SettingsViewModel.kt
│   └── ...
├── ui/                       # UI组件
│   ├── component/            # 可复用组件（24个子目录）
│   │   ├── animation/        # 动画组件
│   │   ├── button/           # 按钮组件
│   │   ├── card/             # 卡片组件
│   │   ├── dialog/           # 对话框组件（12个）
│   │   ├── ios/              # iOS风格组件（18个）
│   │   ├── navigation/       # 导航组件
│   │   │   └── BottomNavScaffold.kt  # Tab缓存Scaffold
│   │   ├── timeline/         # 时间轴组件（7个）
│   │   └── ...
│   └── screen/               # 功能屏幕（10个子目录）
│       ├── advisor/          # AI军师屏幕（10个文件）
│       ├── aiconfig/         # AI配置屏幕（7个文件）
│       ├── contact/          # 联系人屏幕（42个文件）
│       │   ├── factstream/   # 事实流标签页
│       │   ├── overview/     # 概览标签页
│       │   ├── persona/      # 人设标签页
│       │   └── vault/        # 数据保险库标签页
│       ├── prompt/           # 提示词屏幕（9个文件）
│       └── settings/         # 设置屏幕（7个文件）
└── util/                     # 表现层工具
```

### :app 模块 (Application)

```
app/src/main/java/com/empathy/ai/
├── app/
│   ├── EmpathyApplication.kt # Hilt应用类
│   └── SystemPromptConfigProvider.kt
├── di/                       # 应用级DI模块（16个文件）
│   ├── LoggerModule.kt
│   ├── ServiceModule.kt
│   ├── FloatingWindowModule.kt
│   ├── AiAdvisorModule.kt
│   └── ...
├── notification/             # 通知管理
│   └── AiResultNotificationManager.kt
├── domain/service/           # Android服务
│   └── FloatingWindowService.kt
├── domain/util/              # 工具类
│   ├── ErrorHandler.kt
│   ├── FloatingView.kt
│   └── PerformanceMonitor.kt
├── ui/
│   ├── MainActivity.kt       # 主Activity
│   └── theme/                # 主题配置
└── util/
    └── AndroidFloatingWindowManager.kt
```

---

## 层级职责

### 领域层（:domain - 纯业务逻辑）
- **无 Android 依赖** - 纯Kotlin模块，可独立测试
- **包含**: 96个业务模型、18个仓库接口、51个用例、2个领域服务、29个工具类
- **所有用例返回 `Result<T>` 以实现一致的错误处理**
- **所有 IO 操作都是 `suspend` 函数**

### 数据层（:data - 数据访问）
- **实现领域层的18个仓库接口**
- **Room 数据库v16**：11张表、11个DAO、16个增量迁移脚本
- **Retrofit 用于网络调用**：支持7家AI服务商
- **Moshi JSON解析** + SSE流式读取
- **EncryptedSharedPreferences** 用于敏感数据
- **包含8个DI模块**

### 表现层（:presentation - UI和交互）
- **Jetpack Compose** 用于声明式 UI
- **27个ViewModel**，完整的状态管理
- **5个导航组件**：NavGraph、NavRoutes、BottomNavTab、NonTabNavGraph、PromptEditorNavigation
- **13个主题配置文件**
- **24个子目录的UI组件**（原子-分子-有机体-模板四级架构）
- **只依赖:domain模块，不依赖:data模块**

### 应用层（:app - 应用入口）
- **Hilt Application入口**
- **16个应用级DI模块**
- **FloatingWindowService**：悬浮窗服务
- **AiResultNotificationManager**：通知管理

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
:domain/src/test/           # 领域层单元测试（27个文件）
:data/src/test/             # 数据层单元测试（25个文件）
:data/src/androidTest/      # 数据库迁移测试（6个文件）
:presentation/src/test/     # ViewModel单元测试（62个文件）
:presentation/src/androidTest/  # UI测试（7个文件）
:app/src/test/              # 应用层单元测试（141个文件）
:app/src/androidTest/       # 集成测试（8个文件）
```

---

## 当前实现状态（2026-01-13更新）

### 模块文件统计（基于实际代码架构扫描 - 2026-01-15最新）

| 模块 | 主源码 | 单元测试 | Android测试 | 总计 |
|------|--------|---------|------------|------|
| **:domain** | 213 | 40 | 0 | 253 |
| **:data** | 108 | 23 | 6 | 137 |
| **:presentation** | 285 | 36 | 7 | 328 |
| **:app** | 30 | - | - | 30 |
| **总计** | **636** | **99** | **13** | **748** |

**文件构成详细说明**：
- **主源码**：636个文件
  - domain: 213个（173模型 + 18仓库接口 + 60用例 + 2服务 + 29工具 + 其他）
  - data: 108个（10DAO + 11Entity + 27仓库实现 + 8DI + 6parser + 其他）
  - presentation: 285个（27ViewModel + 5导航 + 12主题 + 93屏幕 + 180+组件 + 其他）
  - app: 30个（14DI + Application + Service + 其他）
- **单元测试**：99个文件
- **Android测试**：13个文件

### 数据库版本历史

| 版本 | 更新内容 | 状态 |
|------|----------|------|
| v1→v15 | 基础功能迭代 | 已完成 |
| v16 | 新增AI军师会话相关表、is_pinned字段 | 已完成 |

**迁移脚本**：16个增量迁移脚本（MIGRATION_1_2 ~ MIGRATION_15_16）

**数据库表**（11张表）：
1. profiles - 联系人档案（含is_pinned字段）
2. brain_tags - 大脑标签
3. ai_providers - AI服务商
4. conversation_logs - 对话记录
5. conversation_topics - 对话主题
6. daily_summaries - 每日总结
7. failed_summary_tasks - 失败任务
8. api_usage_records - API用量记录
9. ai_advisor_sessions - AI军师会话
10. ai_advisor_conversations - AI军师对话
11. ai_advisor_message_blocks - AI军师消息块

### 导航系统架构（23个路由）

**主要路由**：
- CONTACT_LIST - 联系人列表（首页）
- CONTACT_DETAIL_TAB/{id} - 联系人详情标签页
- CREATE_CONTACT - 新建联系人
- CHAT/{id} - AI分析界面
- BRAIN_TAG - 标签管理
- AI_ADVISOR - AI军师入口
- AI_ADVISOR_CHAT/{id} - AI军师对话
- AI_ADVISOR_SESSIONS/{id} - AI军师会话历史
- AI_ADVISOR_CONTACTS - AI军师联系人选择
- SETTINGS - 设置页面
- AI_CONFIG - AI配置
- ADD_PROVIDER - 添加服务商
- EDIT_PROVIDER/{id} - 编辑服务商
- USAGE_STATS - 用量统计
- USER_PROFILE - 用户画像
- SYSTEM_PROMPT_LIST - 系统提示词列表
- SYSTEM_PROMPT_EDIT/{scene} - 系统提示词编辑

**导航特性**：
- 页面转场动画：200ms/150ms
- 底部Tab缓存：BottomNavScaffold实现Tab页面内存缓存
- 导航栈治理：AI军师子页面使用popUpTo清理返回栈，锚点优化（BUG-00069）
- 返回路径优化：自动恢复上一个非AI Tab，增强自适应回退逻辑

### 架构合规性

- **Clean Architecture**: ⭐⭐⭐⭐⭐ (A级，完全合规)
- **模块化**: ⭐⭐⭐⭐⭐ (A级，4模块架构)
- **依赖方向**: ⭐⭐⭐⭐⭐ (A级，严格单向依赖)
- **SOLID原则**: ⭐⭐⭐⭐⭐ (A级，完全遵循)

---

## 组件复用系统（四级架构）

| 层级 | 示例 | 说明 |
|------|------|------|
| 原子组件 | `IosButton`, `IosTextField`, `AvatarView` | 基础UI单元，无业务逻辑 |
| 分子组件 | `IosSearchBar`, `ModernPersonaTab` | 组合原子组件，有简单交互 |
| 有机体组件 | `FactStreamCard`, `EmotionTimelineView` | 复杂业务逻辑，独立功能单元 |
| 模板组件 | `AiAdvisorChatScreen` | 页面级组合，特定场景使用 |

**组件目录组织**：24个子目录，包含animation、button、card、chart、chip、contact、control、dialog、emotion、factstream、filter、input、ios、list、message、navigation、overview、persona、relationship、state、tag、text、timeline、topic、vault

---

**文档版本**: 3.4
**最后更新**: 2026-01-15
**更新内容**:
- 更新当前分支为 BUG-FIX
- 更新当前工作状态（BUG-00073 OPPO真机悬浮球不显示问题）
- 更新文件统计：636个主源码文件，99个测试文件
- 更新代码结构：UseCase从51个增加到60个
- 更新Repository实现：增加到27个实现
- 保持架构统计同步
