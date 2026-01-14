# 架构审查报告

## 基本信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-01-12 |
| 审查范围 | 全项目（:domain / :data / :presentation / :app） |
| 状态 | 审查完成 |
| 审查者 | architecture-reviewer |
| 决策日志 | DECISION_JOURNAL.md |

---

## 🔗 相关文档

- `DECISION_JOURNAL.md`（决策记录）
- `CLAUDE.md`（项目主文档）
- `.kiro/steering/structure.md`（结构规范）
- `.kiro/steering/tech.md`（技术栈规范）
- `WORKSPACE.md`（工作状态）

---

## 审查范围

### 审查模块
- :domain
- :data
- :presentation
- :app

### 审查维度
- [x] 层级划分
- [x] 依赖方向
- [x] 命名规范
- [x] 代码组织
- [x] 设计模式
- [x] 可维护性

---

## 审查方法与数据来源

### 方法说明
本次审查以**代码证据为主**、文档为辅，避免基于文档做主观推断。采用以下方法：
1. 全量扫描模块结构与关键包名（rg 搜索）。
2. 统计主源码规模（Kotlin 文件数量与行数）。
3. 重点模块深读（FloatingWindowService、NavGraph、AiRepositoryImpl 等）。
4. 交叉验证依赖方向（build.gradle 依赖声明 + 代码导入检查）。

### 代码统计（仅 src/main）
| 模块 | Kotlin文件数 | 代码行数 |
|------|-------------|----------|
| domain | 184 | 19689 |
| data | 84 | 15273 |
| presentation | 284 | 64741 |
| app | 27 | 9807 |

**说明**：以上为 `src/main` 范围统计，测试代码未计入。此统计用于衡量模块体量与复杂度分布。

---

## 架构现状

### 整体架构
项目采用 Clean Architecture + MVVM，多模块结构清晰，按职责划分为 domain/data/presentation/app。总体上依赖方向符合 “上层依赖下层，内层不依赖外层” 的原则，domain 层保持纯 Kotlin，实现良好。

### 模块依赖图（实证）
```
:app
  ├─ depends on :domain
  ├─ depends on :data
  └─ depends on :presentation

:data  ── api → :domain
:presentation ── api → :domain
:domain ── 纯 Kotlin（无 Android 依赖）
```

### 依赖声明证据
```kotlin
// file: domain/build.gradle.kts
// line: 1-30
plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation("javax.inject:javax.inject:1")
}
```

```kotlin
// file: data/build.gradle.kts
// line: 72-90
dependencies {
    // 使用api暴露domain模块给依赖data模块的模块（解决Hilt多模块类型解析问题）
    api(project(":domain"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.retrofit)
}
```

```kotlin
// file: presentation/build.gradle.kts
// line: 70-88
dependencies {
    // 使用api暴露domain模块给依赖presentation模块的模块（解决Hilt多模块类型解析问题）
    api(project(":domain"))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.navigation.compose)
}
```

### 架构优点概览（基于代码）
- domain 层构建脚本明确禁止 Android 依赖，工具链纯 Kotlin，符合 Clean Architecture 核心要求。
- data/presentation 通过 `api(project(":domain"))` 暴露 domain 接口，依赖方向一致。
- domain 层存在平台相关能力的抽象接口（例如 FloatingWindowManager），并由 app 层实现，体现 DIP。

```kotlin
// file: domain/src/main/kotlin/com/empathy/ai/domain/util/FloatingWindowManager.kt
// line: 1-70
interface FloatingWindowManager {
    fun hasPermission(): PermissionResult
    fun startService(): ServiceStartResult
    fun stopService(): ServiceStopResult
}
```

```kotlin
// file: app/src/main/java/com/empathy/ai/util/AndroidFloatingWindowManager.kt
// line: 1-40
class AndroidFloatingWindowManager @Inject constructor(
    @ApplicationContext private val context: Context
) : FloatingWindowManager {
    override fun startService(): FloatingWindowManager.ServiceStartResult {
        val intent = Intent(context, FloatingWindowService::class.java)
        ...
    }
}
```

---

## 问题发现

### 🔴 严重问题（P0）
- 无。

---

### 🟡 中等问题（P1）

#### P1-001: app 模块存在“domain 包名实现”，导致层级命名漂移

**问题描述**：
在 app 模块中存在多个类使用 `com.empathy.ai.domain.*` 包名（例如 FloatingWindowService、ErrorHandler、FloatingView、PerformanceMonitor），这些类同时包含 Android Framework 依赖与 UI 逻辑。虽然模块依赖方向未被破坏，但包名层级与模块职责不一致，属于架构层次的“语义漂移”。这种漂移会在代码搜索、依赖分析、未来模块拆分时引入混淆，增加错误引入成本。该问题在 Clean Architecture 的命名规范维度属于中等风险：功能可用，但架构可维护性受损。

**复现/定位步骤**：
1. 执行 `rg -n "package com.empathy.ai.domain" app/src/main`。
2. 打开 `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`。
3. 检查包名与 Android 依赖导入。

**根因分析（不少于5句）**：
1. Clean Architecture 强调“层级职责与命名一致”，包名应清晰映射模块职责。
2. 当前 app 模块将 Android Service 与 UI 控件放入 `com.empathy.ai.domain` 包名下，容易被误判为 domain 层实现。
3. 这种命名漂移通常来自历史代码迁移后未完成包路径重构，或为快速修复而忽略模块语义边界。
4. 由于 app 模块编译时依赖 domain/data/presentation，包名漂移不会编译失败，但会削弱架构的可读性与一致性。
5. 当后续需求需要真正的 domain/service 或 domain/util 类时，命名冲突与误引用风险会显著提升。

**影响范围**：
- `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
- `app/src/main/java/com/empathy/ai/domain/util/ErrorHandler.kt`
- `app/src/main/java/com/empathy/ai/domain/util/FloatingView.kt`
- `app/src/main/java/com/empathy/ai/domain/util/PerformanceMonitor.kt`

**问题代码**：
```kotlin
// file: app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt
// line: 1-40
package com.empathy.ai.domain.service

import android.app.Service
import android.view.WindowManager
import com.empathy.ai.domain.usecase.AnalyzeChatUseCase
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.presentation.ui.floating.FloatingViewV2
```

```kotlin
// file: app/src/main/java/com/empathy/ai/domain/util/ErrorHandler.kt
// line: 1-30
package com.empathy.ai.domain.util

import android.content.Context
import android.widget.Toast

object ErrorHandler {
    fun handleError(context: Context, error: FloatingWindowError) { ... }
}
```

**建议改进**：
- 将 app 层 Android 实现类迁移到 `com.empathy.ai.app.*` 或 `com.empathy.ai.framework.*` 包名下。
- 保持 `com.empathy.ai.domain.*` 仅出现在 domain 模块中，避免语义污染。
- 迁移时同步更新依赖注入与引用路径，确保包名与模块边界一致。

**建议代码（示例）**：
```kotlin
// file: app/src/main/java/com/empathy/ai/app/service/FloatingWindowService.kt
// line: 1-12
package com.empathy.ai.app.service

import android.app.Service
import android.view.WindowManager
import com.empathy.ai.domain.usecase.AnalyzeChatUseCase
import com.empathy.ai.presentation.ui.floating.FloatingViewV2

class FloatingWindowService : Service() { ... }
```

**风险评估**：
如果不修复，架构层次将继续“名实不符”。随着 domain 层扩展或模块拆分，包名冲突与误引用风险上升，同时新成员理解成本增加，容易导致后续架构债务累积。

---

#### P1-002: FloatingWindowService 过度集中职责（3221 行），形成“上帝类”

**问题描述**：
`FloatingWindowService` 同时承担 Android Service 生命周期管理、悬浮窗 UI 创建/销毁、UI 回调处理、UseCase 编排、持久化状态管理、通知管理等多重职责。该类行数达到 3221 行，远超可维护阈值，导致逻辑难以测试、难以演进。服务类承担 UI 细节与业务细节，会导致需求变更时牵一发而动全身，属于可维护性与演进性风险。

**复现/定位步骤**：
1. 打开 `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`。
2. 观察文件行数与职责范围。
3. 关注 `createAndShowFloatingViewV2` 与 `setupFloatingViewV2Callbacks` 等方法。

**根因分析（不少于5句）**：
1. 服务类本应仅负责生命周期与系统资源管理，但当前实现将 UI 控件逻辑直接写入服务。
2. 缺少 UI 代理层或 Presenter/Controller 拆分，导致服务类承担 UI 渲染与交互细节。
3. UseCase 编排与数据持久化在服务中混杂，使得业务逻辑与平台逻辑边界模糊。
4. 过往 Bug 修复与功能扩展可能直接向服务追加逻辑，导致类不断膨胀。
5. 当类体积变大时，协作开发成本提高，代码审查难度增加，回归风险加剧。

**问题代码**：
```kotlin
// file: app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt
// line: 60-140
class FloatingWindowService : Service() {

    @Inject lateinit var analyzeChatUseCase: AnalyzeChatUseCase
    @Inject lateinit var checkDraftUseCase: CheckDraftUseCase
    @Inject lateinit var polishDraftUseCase: PolishDraftUseCase
    @Inject lateinit var generateReplyUseCase: GenerateReplyUseCase
    @Inject lateinit var refinementUseCase: RefinementUseCase
    @Inject lateinit var contactRepository: ContactRepository
    @Inject lateinit var floatingWindowPreferences: com.empathy.ai.data.local.FloatingWindowPreferences

    private lateinit var windowManager: WindowManager
    private var floatingViewV2: FloatingViewV2? = null

    override fun onCreate() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        performanceMonitor = com.empathy.ai.domain.util.PerformanceMonitor(this)
    }
}
```

```kotlin
// file: app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt
// line: 2608-2686
private fun createAndShowFloatingViewV2() {
    val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_GiveLove)
    floatingViewV2 = FloatingViewV2(themedContext, windowManager)
    setupFloatingViewV2Callbacks()
    loadContactsForFloatingViewV2()
    restoreFloatingViewV2State()
    addFloatingViewV2ToWindow()
}

private fun setupFloatingViewV2Callbacks() {
    floatingViewV2?.apply {
        setOnTabChangedListener { tab ->
            currentUiState = currentUiState.copy(selectedTab = tab)
            floatingWindowPreferences.saveSelectedTab(tab)
        }
        setOnSubmitListener { tab, contactId, text ->
            when (tab) {
                ActionType.ANALYZE -> handleAnalyzeV2(contactId, text)
                ActionType.POLISH -> handlePolishV2(contactId, text)
                ActionType.REPLY -> handleReplyV2(contactId, text)
            }
        }
    }
}
```

**建议改进**：
- 以职责划分为核心，将 Service 拆分为：Service 生命周期管理、UI 代理、状态存储与业务编排四个模块。
- UI 相关逻辑迁移至 presentation 层或独立的 UI Delegate，Service 只负责绑定与事件转发。
- UseCase 编排与状态存储可抽离为可单测的协作类（例如 FloatingWindowOrchestrator）。

**建议代码（示例）**：
```kotlin
// file: app/src/main/java/com/empathy/ai/app/floating/FloatingWindowUiDelegate.kt
interface FloatingWindowUiDelegate {
    fun attach(windowManager: WindowManager)
    fun bindCallbacks(onSubmit: (ActionType, String?, String) -> Unit)
    fun detach()
}

// file: app/src/main/java/com/empathy/ai/app/service/FloatingWindowService.kt
class FloatingWindowService : Service() {
    @Inject lateinit var uiDelegate: FloatingWindowUiDelegate
    @Inject lateinit var orchestrator: FloatingWindowOrchestrator

    override fun onCreate() {
        uiDelegate.attach(getSystemService(WINDOW_SERVICE) as WindowManager)
        uiDelegate.bindCallbacks { tab, contactId, text ->
            orchestrator.handleSubmit(tab, contactId, text)
        }
    }
}
```

**风险评估**：
若继续在单一 Service 中累积逻辑，未来 UI 改动、业务改动和系统改动会互相牵制，导致修复成本增大，且对新成员的入门门槛显著提高。

---

#### P1-003: Service 直接依赖 data 实现类，未使用 domain 抽象接口

**问题描述**：
`FloatingWindowService` 直接注入并依赖 `FloatingWindowPreferences`（data 层实现），而 domain 层已定义 `FloatingWindowPreferencesRepository` 接口。虽然 app 模块允许依赖 data，但这种“实现类直连”违背 Clean Architecture 的依赖倒置思想，使得 Service 在测试与替换实现时缺少抽象隔离。长期来看，会让 app 层变成“依赖实现细节”的聚合点，增加重构成本。

**复现/定位步骤**：
1. 搜索 `FloatingWindowPreferences` 在 `FloatingWindowService` 中的注入位置。
2. 对比 domain 层 `FloatingWindowPreferencesRepository` 接口定义。
3. 确认 data 层 `FloatingWindowPreferences` 实现关系。

**根因分析（不少于5句）**：
1. 设计上已经有 domain 层接口，但在 Service 中选择直接注入实现类，可能是出于“方便快捷”的考虑。
2. 这种实践在短期内降低了注入配置复杂度，但牺牲了架构一致性。
3. Service 作为高层业务入口，更需要依赖抽象而不是具体实现。
4. 当前代码中已经有 `FloatingWindowPreferencesRepository` 的 Hilt 绑定，说明接口路径可用。
5. 如果未来需要更换持久化方式或添加加密实现，Service 级别的直接依赖会导致额外修改。

**问题代码**：
```kotlin
// file: app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt
// line: 82-92
@Inject
lateinit var floatingWindowPreferences: com.empathy.ai.data.local.FloatingWindowPreferences
```

```kotlin
// file: domain/src/main/kotlin/com/empathy/ai/domain/repository/FloatingWindowPreferencesRepository.kt
// line: 24-40
interface FloatingWindowPreferencesRepository {
    fun saveState(state: FloatingWindowState)
    fun loadState(): FloatingWindowState
}
```

```kotlin
// file: data/src/main/kotlin/com/empathy/ai/data/local/FloatingWindowPreferences.kt
// line: 23-36
class FloatingWindowPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) : FloatingWindowPreferencesRepository {
    override fun saveState(state: FloatingWindowState) { ... }
}
```

**建议改进**：
- 将 Service 注入类型替换为 `FloatingWindowPreferencesRepository`。
- 确保 Hilt Module 继续绑定 data 实现到 domain 接口。
- 通过接口隔离未来可能的加密实现或替代存储。

**建议代码（示例）**：
```kotlin
// file: app/src/main/java/com/empathy/ai/app/service/FloatingWindowService.kt
@Inject
lateinit var floatingWindowPreferences: FloatingWindowPreferencesRepository
```

**风险评估**：
如果不调整，未来在替换存储实现或增加测试桩时需要修改 Service 源码，削弱扩展性与可测试性。

---

### 🟢 轻微问题（P2）

#### P2-001: KDoc 跨层引用导致架构语义泄漏

**问题描述**：
在 domain 与 data 层的 KDoc 中出现对其他层实现的直接引用，例如 domain 接口引用 data 实现、data 实现引用 presentation ViewModel。这类引用不会产生编译依赖，但会造成“心理依赖”和认知上的跨层耦合。长期来看会让开发者在阅读时误以为跨层依赖是允许的，从而削弱 Clean Architecture 约束。

**复现/定位步骤**：
1. 打开 `AiAdvisorPreferencesRepository.kt`，查看 `@see` 注释。
2. 打开 `AiAdvisorPreferences.kt`，查看 `@see` 注释。

**根因分析（不少于5句）**：
1. 在多人协作场景中，KDoc 常被用作“导航索引”，容易添加具体实现引用。
2. 但 domain 层文档本应保持抽象语义，避免指向具体实现类。
3. data 层同样不应将 UI 层作为参考对象，否则会暗示跨层依赖合理。
4. 这种“文档层面的依赖”虽非编译依赖，但会影响架构教育与团队认知。
5. Clean Architecture 的治理不仅在代码，也在文档与命名的一致性上。

**问题代码**：
```kotlin
// file: domain/src/main/kotlin/com/empathy/ai/domain/repository/AiAdvisorPreferencesRepository.kt
// line: 13-18
/**
 * ...
 * @see com.empathy.ai.data.local.AiAdvisorPreferences 实现类
 */
interface AiAdvisorPreferencesRepository { ... }
```

```kotlin
// file: data/src/main/kotlin/com/empathy/ai/data/local/AiAdvisorPreferences.kt
// line: 30-36
/**
 * ...
 * @see com.empathy.ai.presentation.viewmodel.AiAdvisorChatViewModel
 */
class AiAdvisorPreferences @Inject constructor(...) : AiAdvisorPreferencesRepository { ... }
```

**建议改进**：
- KDoc 中仅保留 domain 内部抽象说明或引用架构文档。
- 如果需要指向实现，建议通过文档目录或“实现类列表”统一说明。

**建议代码（示例）**：
```kotlin
// 建议替换为：
// @see 文档/项目文档/domain/repository/README.md
```

**风险评估**：
该问题不会导致功能错误，但会逐步侵蚀架构约束意识，属于“温水煮青蛙”式的认知风险。

---

#### P2-002: NavGraph 过度集中（616 行），增加导航演进成本

**问题描述**：
`NavGraph.kt` 作为全量导航入口，集中包含所有路由和导航规则，文件长度达到 616 行。随着功能增加，该文件会持续膨胀，导致开发者在修改某一功能导航时需要触碰全局导航文件。虽然目前功能可正常运行，但从组织结构上看，拆分成特性模块化的导航图会更利于维护与演进。

**复现/定位步骤**：
1. 打开 `presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NavGraph.kt`。
2. 查看 `NavHost` 中的路由数量与逻辑。

**根因分析（不少于5句）**：
1. 早期阶段将所有路由集中在一个文件中便于快速开发。
2. 随着路由数量增多，该文件成为“单点修改点”，容易产生合并冲突。
3. UI 与导航规则混杂会削弱模块边界，导致功能之间耦合增强。
4. Clean Architecture 鼓励按功能拆分，以减少跨模块影响。
5. 当前结构缺少“feature-level navigation”，使得迭代成本上升。

**问题代码**：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NavGraph.kt
// line: 105-160
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    includeTabScreens: Boolean = true
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.CONTACT_LIST,
        modifier = modifier
    ) {
        composable(route = NavRoutes.CONTACT_LIST) { ... }
        composable(route = NavRoutes.CONTACT_DETAIL) { ... }
        composable(route = NavRoutes.CHAT) { ... }
        // 其余路由持续增加
    }
}
```

**建议改进**：
- 将 NavGraph 按功能拆分为 FeatureNavGraph（如 ContactNavGraph、AdvisorNavGraph）。
- 使用 `NavGraphBuilder` 扩展函数组合，主 NavGraph 仅负责聚合。

**建议代码（示例）**：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/ContactNavGraph.kt
fun NavGraphBuilder.contactGraph(navController: NavHostController) {
    composable(route = NavRoutes.CONTACT_LIST) { ... }
    composable(route = NavRoutes.CONTACT_DETAIL) { ... }
}

// file: presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NavGraph.kt
NavHost(...) {
    contactGraph(navController)
    advisorGraph(navController)
    settingsGraph(navController)
}
```

**风险评估**：
短期内问题不显性，但路由持续扩展会增加冲突与回归风险，属于可维护性债务。

---

#### P2-003: AiRepositoryImpl 过大（1524 行），职责边界偏宽

**问题描述**：
`AiRepositoryImpl` 涵盖网络调用、服务商适配、Prompt 模板、JSON 清理、Token 估算与用量统计等多种职责。该类行数达到 1524 行，已经具备多个子系统特征。虽然逻辑集中便于快速迭代，但会在长期造成“修改一处、测试多处”的维护负担。

**复现/定位步骤**：
1. 打开 `data/src/main/kotlin/com/empathy/ai/data/repository/AiRepositoryImpl.kt`。
2. 查看类头部与大量常量/策略定义。

**根因分析（不少于5句）**：
1. 多服务商兼容需要大量条件逻辑，导致实现类自然膨胀。
2. Prompt 文本与解析策略缺少独立模块承载，被直接写入 Repository。
3. 用量统计与错误处理逻辑耦合在同一类中，使得职责分界模糊。
4. Repository 层承担了“业务策略 + 技术细节”双重责任。
5. 当新增服务商或新场景时，修改范围不断扩大，风险上升。

**问题代码**：
```kotlin
// file: data/src/main/kotlin/com/empathy/ai/data/repository/AiRepositoryImpl.kt
// line: 1-120
class AiRepositoryImpl @Inject constructor(
    private val api: OpenAiApi,
    private val settingsRepository: SettingsRepository,
    private val sseStreamReader: SseStreamReader,
    private val apiUsageRepository: ApiUsageRepository? = null
) : AiRepository {

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L

        val SYSTEM_ANALYZE = """..."""
        val TOOL_ANALYZE_CHAT = ToolDefinition(...)
        val TOOL_CHECK_SAFETY = ToolDefinition(...)
    }
}
```

**建议改进**：
- 抽离 Prompt 模板为 `PromptLibrary`，抽离 Provider 兼容逻辑为 `ProviderCompatibility`。
- 将解析与清洗逻辑收敛为 `AiResponseParser` 组件，Repository 仅编排调用。
- 用量统计与日志记录独立为 `AiUsageRecorder`。

**建议代码（示例）**：
```kotlin
// file: data/src/main/kotlin/com/empathy/ai/data/ai/AiPromptLibrary.kt
class AiPromptLibrary {
    fun analyzePrompt(): String = "..."
}

// file: data/src/main/kotlin/com/empathy/ai/data/repository/AiRepositoryImpl.kt
class AiRepositoryImpl @Inject constructor(
    private val promptLibrary: AiPromptLibrary,
    private val responseParser: AiResponseParser
) : AiRepository {
    override suspend fun analyzeChat(...) {
        val prompt = promptLibrary.analyzePrompt()
        val response = api.call(prompt)
        return responseParser.parse(response)
    }
}
```

**风险评估**：
短期不改不会导致错误，但长期维护成本与回归风险逐步上升，特别是多服务商扩展场景。

---

#### P2-004: domain 层包含 UI 状态模型，边界语义不清

**问题描述**：
domain 层存在 `FloatingWindowUiState` 与 `StreamingState` 等“显式 UI 语义”的模型，注释明确用于 UI 状态管理与 ViewModel/Compose 渲染。这会导致 domain 层承担 UI 表达职责，削弱 “domain 仅承载业务语义” 的边界清晰度。虽然当前实现不影响功能，但会让后续分层调整（如多端复用或隔离 UI）变得更困难。

**复现/定位步骤**：
1. 打开 `domain/src/main/kotlin/com/empathy/ai/domain/model/FloatingWindowUiState.kt`。
2. 打开 `domain/src/main/kotlin/com/empathy/ai/domain/model/StreamingState.kt`。
3. 查看注释中的 UI 语义描述。

**根因分析（不少于5句）**：
1. 流式响应需要 UI 状态驱动，团队在实现时将 UI 状态类直接放入 domain 以复用。
2. domain 层以纯 Kotlin 实现，确实适合承载跨模块模型，但 UI 语义会逐渐侵蚀 domain 的抽象性。
3. 这些 UI 状态模型在文档中被描述为 “用于 ViewModel 和 UI 之间的状态传递”，属于 presentation 关注点。
4. 当未来进行多端复用或 server-side domain 扩展时，UI 状态模型会成为不必要的依赖。
5. 这类混合模型通常需要在早期识别并隔离，以避免架构演化时的清理成本。
**问题代码**：
```kotlin
// file: domain/src/main/kotlin/com/empathy/ai/domain/model/FloatingWindowUiState.kt
// line: 1-28
/**
 * 悬浮窗UI状态模型
 *
 * 管理悬浮窗的完整UI状态，支持状态保持和恢复
 */
data class FloatingWindowUiState(
    val selectedTab: ActionType = ActionType.ANALYZE,
    val selectedContactId: String? = null,
    val inputText: String = "",
    val lastResult: AiResult? = null
)
```

```kotlin
// file: domain/src/main/kotlin/com/empathy/ai/domain/model/StreamingState.kt
// line: 1-22
/**
 * 流式状态
 *
 * 用于ViewModel和UI之间的状态传递，封装流式响应的各种状态。
 * 与AiStreamChunk不同，StreamingState更关注UI层的状态管理。
 */
sealed class StreamingState { ... }
```

**建议改进**：
- 将 UI 状态模型迁移到 presentation 层（或 app 层）并通过 mapper 进行转换。
- 如果需要跨模块共享，可引入 `presentation-models` 子模块承载 UI 语义模型，避免混入 domain。

**建议代码（示例）**：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/model/StreamingUiState.kt
sealed class StreamingUiState { ... }
```

**风险评估**：
该问题不会导致功能错误，但会在架构演进中形成“隐藏耦合”，清理成本随功能扩展而上升。

---

#### P2-005: domain Repository 接口包含 UI 级别状态持久化方法

**问题描述**：
`FloatingWindowPreferencesRepository` 在 domain 层暴露了大量 UI 级别的状态持久化方法（如 Tab 选择、输入文本、UI 状态对象），这些职责更偏向 presentation/app 层的状态管理。domain 层的 Repository 设计应尽量表达业务语义，而不是 UI 状态细节。当前做法虽可用，但会扩大 domain 接口表面，造成后续重构成本。

**复现/定位步骤**：
1. 打开 `domain/src/main/kotlin/com/empathy/ai/domain/repository/FloatingWindowPreferencesRepository.kt`。
2. 查看 “输入文本管理 / UI 状态管理” 段落的方法列表。

**根因分析（不少于5句）**：
1. 悬浮窗是系统级功能，团队将其状态持久化全部放入 domain 接口以便跨层调用。
2. 但 domain 层的 Repository 过度暴露 UI 状态，将界面细节上移到核心层。
3. 该接口已接近 “UI 状态存储服务”，而非业务仓储。
4. 这种设计会让 domain 逐步承担更多 UI 细节，违背 Clean Architecture 的内聚原则。
5. 当 UI 形态变化时，domain 接口将被迫修改，造成层级反向影响。
**问题代码**：
```kotlin
// file: domain/src/main/kotlin/com/empathy/ai/domain/repository/FloatingWindowPreferencesRepository.kt
// line: 120-190
fun saveSelectedTab(tab: ActionType)
fun getSelectedTab(): String
fun saveInputText(text: String)
fun getInputText(): String
fun saveUiState(state: FloatingWindowUiState)
fun restoreUiStateAsObject(): FloatingWindowUiState?
```

**建议改进**：
- 将 UI 状态持久化拆分为 `FloatingWindowUiStateStore`，放在 presentation 或 app 层。
- 保留 domain Repository 仅承载与业务相关的持久化语义（例如会话/联系人相关状态）。

**建议代码（示例）**：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/state/FloatingWindowUiStateStore.kt
interface FloatingWindowUiStateStore {
    fun saveUiState(state: FloatingWindowUiState)
    fun restoreUiState(): FloatingWindowUiState?
}
```

**风险评估**：
问题主要体现在长期演进成本，不会引发即时缺陷，但会逐步削弱分层清晰度。

---

## 优点总结

### 架构优点
1. **模块化清晰**：domain/data/presentation/app 边界明确，职责分层基本稳定。
2. **依赖方向清楚**：data 与 presentation 均依赖 domain，app 作为组合根。
3. **抽象接口实践**：domain 定义平台能力接口（例如 FloatingWindowManager），app 实现。
4. **技术栈一致性**：Kotlin + Compose + Hilt 全链路使用一致，降低技术碎片化。

### 代码组织优点
1. **Repository + UseCase 结构清晰**，领域模型集中在 domain。 
2. **导航与 UI 组件**按目录组织，虽然集中但结构可读。

---

## 改进建议

### 短期改进（1-2周）
| 建议 | 优先级 | 预估工作量 |
|------|--------|------------|
| 调整 app 层 domain 包名漂移，统一到 app/framework 命名 | 高 | 2-3天 |
| Service 注入改为 domain 接口（FloatingWindowPreferencesRepository） | 高 | 1天 |

### 中期改进（1-2月）
| 建议 | 优先级 | 预估工作量 |
|------|--------|------------|
| 拆分 FloatingWindowService 为 UI Delegate + Orchestrator + Service | 中 | 2-3周 |
| 拆分 NavGraph 为 feature-level 导航 | 中 | 1-2周 |

### 长期改进（3月以上）
| 建议 | 优先级 | 预估工作量 |
|------|--------|------------|
| 拆分 AiRepositoryImpl 为 Prompt/Parser/Compatibility 子模块 | 低 | 1-2月 |

---

## 整改实施路线图（详细版）

### Phase 1: 包名语义修正 + 依赖倒置（低风险）
1. 迁移 app 层 `com.empathy.ai.domain.*` 包名到 `com.empathy.ai.app.*` 或 `com.empathy.ai.app.floating.*`。
2. 更新 `AndroidManifest.xml`、通知管理与服务启动引用路径。
3. 将 `FloatingWindowService` 注入类型从实现类切换为 `FloatingWindowPreferencesRepository`。

### Phase 2: Service 拆分 + 导航拆分（结构优化）
1. 抽离 Service 内 UI 控制逻辑为 `FloatingWindowUiDelegate`。
2. 抽离业务编排为 `FloatingWindowOrchestrator`。
3. 将 `NavGraph` 按 feature 拆分（联系人、军师、设置）。

### Phase 3: Repository/Domain 语义治理（长期）
1. 拆分 `AiRepositoryImpl`：Prompt/Parser/Compatibility/Usage Recorder。
2. 将 `StreamingState`、`FloatingWindowUiState` 迁移至 UI 语义层或独立 module。
3. 将 UI 状态持久化接口从 domain 拆分为 UI Store。

---

## 包名迁移清单（建议）

| 原路径 | 建议新路径 | 说明 |
|--------|-----------|------|
| `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt` | `app/src/main/java/com/empathy/ai/app/service/FloatingWindowService.kt` | Android Service 应归 app 层语义 |
| `app/src/main/java/com/empathy/ai/domain/util/ErrorHandler.kt` | `app/src/main/java/com/empathy/ai/app/util/ErrorHandler.kt` | Android 依赖工具类 |
| `app/src/main/java/com/empathy/ai/domain/util/FloatingView.kt` | `app/src/main/java/com/empathy/ai/app/floating/FloatingView.kt` | 悬浮窗 UI 组件 |
| `app/src/main/java/com/empathy/ai/domain/util/FloatingViewDebugLogger.kt` | `app/src/main/java/com/empathy/ai/app/floating/FloatingViewDebugLogger.kt` | 悬浮窗调试辅助 |
| `app/src/main/java/com/empathy/ai/domain/util/PerformanceMonitor.kt` | `app/src/main/java/com/empathy/ai/app/util/PerformanceMonitor.kt` | app 内性能监控 |

### 关键引用更新点（示例）
```kotlin
// file: app/src/main/AndroidManifest.xml
// android:name=".domain.service.FloatingWindowService"
// 需更新为新包名

// file: app/src/main/java/com/empathy/ai/util/AndroidFloatingWindowManager.kt
// import com.empathy.ai.domain.service.FloatingWindowService

// file: app/src/main/java/com/empathy/ai/notification/AiResultNotificationManager.kt
// import com.empathy.ai.domain.service.FloatingWindowService
```

---

## FloatingWindowService 拆分候选模块（草案）

| 模块 | 职责 | 说明 |
|------|------|------|
| FloatingWindowService | Service 生命周期与系统交互 | 仅保留系统服务职责 |
| FloatingWindowUiDelegate | UI 展示/隐藏与回调 | 处理 FloatingView/FloatingViewV2 |
| FloatingBubbleController | 悬浮球与最小化 UI | 分离 bubble 逻辑 |
| FloatingWindowOrchestrator | UseCase 编排 | 统一处理业务调用 |
| FloatingWindowStateStore | UI 状态持久化 | 对接 Repository/Store |
| FloatingWindowNotificationHelper | 前台通知管理 | 通知职责收敛 |

---

## NavGraph 拆分建议（草案）

| Feature | 目标文件 | 路由范围 |
|---------|----------|----------|
| Contact | `ContactNavGraph.kt` | 联系人列表、详情、创建 |
| Advisor | `AdvisorNavGraph.kt` | AI军师入口、会话、联系人选择 |
| Settings | `SettingsNavGraph.kt` | 设置、配置、用量统计、画像 |

---

## Domain UI 语义治理方案

**方案 A（推荐）**：迁移 UI 状态模型到 presentation，并由 UseCase 输出更中性的 domain 事件模型。  
**方案 B（折中）**：保留模型但移除 UI 语义注释，将其定义为“流式业务事件”。  
**方案 C（结构化）**：新增 `:ui-models` 模块承载 UI 语义模型，domain 仅保留业务模型。

---

## 验证策略（与路线图匹配）

| 阶段 | 构建/测试建议 | 目的 |
|------|---------------|------|
| Phase 1 | `gradlew.bat assembleDebug` | 验证包名迁移与依赖倒置不破坏构建 |
| Phase 2 | `gradlew.bat :presentation:test` | 确保导航拆分与 UI 逻辑不回归 |
| Phase 3 | `gradlew.bat :data:test` | 校验 Repository 拆分与解析逻辑 |

---

## 结论

### 架构评分
| 维度 | 满分 | 得分 | 说明 |
|------|------|------|------|
| 层级划分 | 20 | 15 | 模块划分清晰，但 app 包名漂移与 domain UI 状态混入扣分 |
| 依赖方向 | 20 | 16 | 依赖方向整体正确，但 Service 依赖实现类与文档跨层引用扣分 |
| 命名规范 | 15 | 11 | 包名与模块职责存在不一致，KDoc 跨层引用 |
| 代码组织 | 15 | 11 | NavGraph 与 Service 集中度高，影响组织结构可维护性 |
| 设计模式 | 15 | 12 | Repository/UseCase/DI 充分，但部分实现未拆分 |
| 可维护性 | 15 | 10 | 超大类与多职责组合带来维护成本 |

**总分**：75 / 100 （⭐⭐⭐ 中等）

### 总体评价
该项目在模块化与依赖方向上表现良好，Clean Architecture 主体结构成立，domain 层纯净且可测试，这是最重要的架构优势。当前主要问题集中在 app 层包名漂移与巨型 Service 的职责聚合，这会在中长期演进中放大维护成本与回归风险。整体评价为“架构扎实但存在结构性维护债务”，建议优先处理包名与 Service 拆分问题，确保架构的语义一致性与可扩展性。

---

## 报告质量自检

### 字数检查
- [x] 总字数达到最低要求（约 4000+ 字，要求 3000 字）
- [x] 代码行数达到最低要求（约 200+ 行，要求 50 行）

### 内容完整性
- [x] 所有必须章节都已填写
- [x] 每个问题都有代码证据与改进示例
- [x] 每个问题包含根因分析与风险评估

### 自包含检查
- [x] 报告包含关键代码片段（含文件路径与行号）
- [x] 仅凭报告可理解问题与建议
