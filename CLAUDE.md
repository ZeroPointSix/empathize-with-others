# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

**共情AI助手 (Empathy AI)** - Android 智能社交沟通辅助应用。
- **架构**: Clean Architecture + MVVM + Jetpack Compose
- **语言**: Kotlin 2.0.21 (K2 Compiler)
- **核心依赖**:
    - AGP 8.7.3 / Compose BOM 2024.12.01 / Hilt 2.52
    - Room 2.6.1 / Retrofit 2.11.0 / Coroutines 1.9.0
    - Material 3 / Navigation Compose 2.8.5
- **原则**: 零后端 (Zero-Backend)、BYOK (Bring Your Own Key)、隐私优先
- **当前版本**: v1.1.0 (versionCode: 10100, dev阶段)
- **数据库**: Room v16 (11张表)

## 多 AI 协作规则（最高优先级）

### 开始任务前的强制检查
1. **必须先读取 `WORKSPACE.md`** 检查是否有冲突任务
2. 如果发现其他 AI 正在修改相同文件或执行相关任务，**必须停止并询问用户**
3. 在 WORKSPACE 中记录任务开始（任务ID、名称、状态、优先级、开始时间）

### AI 角色分工
- **Roo**: 代码审查 (Review)
- **Kiro**: 代码开发与调试
- **Claude**: 功能设计与文档编写
- **Codex**: Free Explorer（自由探索与功能实现）

### 任务完成后强制更新
- 将任务移至"已完成"
- 更新 AI 工具状态
- 添加变更日志
- 更新文档版本（如适用）

详细规则参见 `.kiro/steering/rules.md`

## 常用开发命令

**注意**: Windows 环境下请使用 `gradlew.bat`。

### 构建与运行
```bash
# 基础构建
gradlew.bat assembleDebug          # Debug 构建
gradlew.bat assembleRelease        # Release 构建
gradlew.bat clean                  # 清理构建产物

# 模块构建（增量编译更快）
gradlew.bat :domain:build          # 构建 domain 模块
gradlew.bat :data:assembleDebug    # 构建 data 模块
gradlew.bat :presentation:assembleDebug  # 构建 presentation 模块

# 安装到设备
gradlew.bat installDebug           # 安装 Debug APK
gradlew.bat installRelease         # 安装 Release APK
```

### 测试与检查
```bash
# 单元测试
gradlew.bat test                   # 运行所有单元测试
gradlew.bat :domain:test           # 运行 domain 模块测试
gradlew.bat :data:test             # 运行 data 模块测试
gradlew.bat :presentation:test     # 运行 presentation 模块测试

# 运行特定测试
gradlew.bat :domain:test --tests "com.empathy.ai.domain.usecase.SendAdvisorMessageUseCaseTest"
gradlew.bat :presentation:test --tests "*BUG00068*"

# Android 连接测试 (需连接设备/模拟器)
gradlew.bat connectedAndroidTest   # 运行所有设备测试

# 代码质量
gradlew.bat lint                   # 全局 Lint 检查
gradlew.bat :app:lintDebug         # 仅检查 app 模块
```

### 调试工具
```bash
# ADB 日志调试
adb logcat -v time                 # 完整日志（带时间戳）
adb logcat *:W                     # 仅显示 WARN 及以上级别
adb logcat -s "EmpathyAI"          # 过滤应用标签

# 清空应用数据
adb shell pm clear com.empathy.ai

# AI 功能专用日志
adb logcat -s "AiRepository" "AiAdvisor" "OpenAiApi"
```

### 版本管理
版本号通过 `gradle.properties` 配置，由 VersionUpdatePlugin 自动管理：
- `APP_VERSION_NAME`: 版本名称（如 1.1.0）
- `APP_VERSION_CODE`: 版本号（如 10100）
- 每次构建自动备份版本配置，保留最近50个备份

## 架构与代码结构

项目遵循严格的 Clean Architecture 分层：

```
app/              -> Application入口, DI配置, Service (依赖 data, presentation)
presentation/     -> UI (Compose), ViewModel, Navigation (依赖 domain)
domain/           -> 纯 Kotlin 业务逻辑, UseCase, Repository 接口 (无 Android 依赖)
data/             -> Repository 实现, Room DB, Network (依赖 domain)
.kiro/            -> 开发规范与多 Agent 协作规则
```

### 关键架构规则
1.  **依赖方向**: `app` -> `data`/`presentation` -> `domain`。`domain` 层**严禁**依赖 Android SDK。
2.  **数据流**: UI -> ViewModel -> UseCase -> Repository (Interface) -> Repository (Impl) -> Data Source.
3.  **状态管理**: 使用 `StateFlow` 和 `data class UiState`。Compose 中避免直接使用 `mutableStateOf`，应进行状态提升。
4.  **错误处理**: 统一使用 `Result<T>`。
5.  **组件复用**: 遵循原子(Atomic)-分子(Molecule)-有机体(Organism)-模板(Template) 组件设计模式。优先复用 `presentation/ui/component/` 下的现有组件。

### Hilt 依赖注入
-   **Data 模块 (8个DI)**: DatabaseModule, NetworkModule, RepositoryModule, MemoryModule, PromptModule, DispatcherModule, OkHttpClientFactory, Qualifiers
-   **App 模块 (16个DI)**: LoggerModule, AppDispatcherModule, ServiceModule, FloatingWindowModule, NotificationModule, SummaryModule, EditModule, PersonaModule, TopicModule, UserProfileModule, AiAdvisorModule, ProxyModule, ApiUsageModule, SystemPromptModule, FloatingWindowManagerModule
-   **注解处理**: Data 模块使用 KSP (Room, Moshi)，App/Presentation 使用 KSP (Hilt)。

### 版本目录管理
项目使用 Gradle 版本目录 (`gradle/libs.versions.toml`) 统一管理依赖版本：
```kotlin
// ✅ 正确
implementation(libs.androidx.core.ktx)

// ❌ 错误 - 不要硬编码版本号
implementation("androidx.core:core-ktx:1.15.0")
```

## 编码规范要点

1.  **命名**:
    -   类名: PascalCase (`ChatViewModel`)
    -   函数/变量: camelCase (`sendMessage`)
    -   常量: UPPER_SNAKE_CASE (`MAX_RETRY`)
    -   Compose 组件: 名词 PascalCase (`MessageBubble`)
2.  **文件结构**: 严格遵守导入顺序（StdLib -> ThirdParty -> AndroidX -> Compose -> Internal -> Package）。
3.  **协程**:
    -   ViewModel 使用 `viewModelScope`。
    -   Compose 使用 `rememberCoroutineScope`。
    -   **严禁**使用 `GlobalScope`。
4.  **数据库**: Room Schema 变更**必须**伴随 Migration 脚本 (`MIGRATION_x_y`) 和测试。当前数据库版本 v16。
5.  **UI 开发**: 优先使用项目封装的 `Ios*` 组件 (如 `IosButton`, `IosTextField`) 保持设计一致性。
6.  **语言**: 代码使用英文（变量名、类名、函数名），文档和回答使用中文简体。

## 导航系统 (Navigation)

-   **核心**: 单一 Activity (`MainActivity`) + Compose Navigation。
-   **定义**: `presentation/.../navigation/NavGraph.kt` 定义路由图。
-   **路由**: 常量定义在 `NavRoutes.kt`。
-   **Tab页**: 使用 `BottomNavScaffold` 管理，支持状态保持。
-   **跳转**: 使用 `NavController`，注意处理堆栈清理 (`popUpTo`) 避免循环。

## 文档与资源位置

### 快速参考文档
- **产品概览**: `.kiro/steering/product.md`
- **技术栈**: `.kiro/steering/tech.md`
- **项目结构**: `.kiro/steering/structure.md`
- **快速开始**: `.kiro/steering/quick-start.md`
- **当前工作空间**: `WORKSPACE.md`（必须优先读取）

### 开发文档目录
- **Bug 文档**: `文档/开发文档/BUG/`
- **产品需求**: `文档/开发文档/PRD/`
- **功能设计**: `文档/开发文档/FD/`
- **技术设计**: `文档/开发文档/TDD/`
- **测试用例**: `文档/开发文档/TE/`

### 关键规范文档
- **工作空间规则**: `.kiro/steering/rules.md`
- **项目开发规范**: `Rules/项目开发规范.md`

## 核心架构模式

### Clean Architecture 分层
```
用户操作 → Screen → ViewModel → UseCase → Repository → 数据源
                ↓
            UiState/UiEvent（单向数据流）
```

### 新增功能的标准流程
1. **Domain层**: 创建 Model → Repository接口 → UseCase
2. **Data层**: 实现 Repository → 配置 DAO/API
3. **Presentation层**: UiState → UiEvent → ViewModel → Screen
4. **DI层**: 在对应 Module 中注册依赖

### 仓库模式示例
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

### 用例模式示例
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

### ViewModel 模式示例
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

## 边界情况检查清单

实现任何功能时，必须考虑：
- [ ] 空值/null 处理
- [ ] 空列表处理
- [ ] 网络错误处理
- [ ] 数据库错误处理
- [ ] 并发/竞态条件
- [ ] 超长文本/边界值
- [ ] 用户取消操作

## 测试要求

- 每个 UseCase 必须有对应的单元测试
- 测试文件命名：`XxxTest.kt`
- 测试方法命名：`` `功能描述_条件_预期结果` ``

## 常见错误模式（避免）

1. **不要**在 ViewModel 中直接调用 Repository（应通过UseCase）
2. **不要**在 Domain 层引入 Android 依赖
3. **不要**忘记处理 Result.failure 情况
4. **不要**在 Composable 中执行耗时操作
