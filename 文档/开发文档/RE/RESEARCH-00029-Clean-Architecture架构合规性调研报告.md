# RESEARCH-00029-Clean Architecture架构合规性调研报告

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | RESEARCH-00029 |
| 创建日期 | 2025-12-23 |
| 调研人 | Kiro |
| 状态 | 调研完成 |
| 调研目的 | 分析项目Clean Architecture架构合规性问题，识别domain层违规依赖 |
| 关联任务 | 架构优化、技术债务清理 |

---

## 1. 调研范围

### 1.1 调研主题
Clean Architecture架构合规性分析，重点关注domain层对Android框架和其他层级的违规依赖。

### 1.2 关注重点
- domain层引用Android SDK的文件
- domain层引用data层的文件
- domain层引用presentation层的文件
- domain层引用R资源文件的文件
- 违规依赖的具体使用场景和修复方案

### 1.3 关联文档

| 文档类型 | 文档编号 | 文档名称 |
|----------|----------|----------|
| 规范 | - | .kiro/steering/structure.md |
| 规范 | - | .kiro/steering/quick-start.md |

---

## 2. 代码现状分析

### 2.1 Domain层文件统计

| 子目录 | 文件数 | 说明 |
|--------|--------|------|
| model | 62 | 领域模型定义 |
| repository | 12 | 仓库接口定义 |
| usecase | 37 | 业务用例实现 |
| service | 4 | 领域服务 |
| util | 44 | 领域工具类 |
| **总计** | **159** | - |

### 2.2 违规文件清单

#### 2.2.1 引用Android SDK的文件（主代码）

| 文件路径 | 违规类型 | 具体依赖 |
|----------|----------|----------|
| `domain/service/FloatingWindowService.kt` | Android Service | android.app.*, android.view.*, androidx.core.app.* |
| `domain/service/SessionContextService.kt` | Android Log | android.util.Log |
| `domain/model/FilterType.kt` | Compose UI | androidx.compose.material.icons.*, androidx.compose.ui.graphics.* |
| `domain/util/FloatingWindowManager.kt` | Android Context | android.app.*, android.content.*, android.provider.* |
| `domain/util/FloatingView.kt` | Android View | android.content.*, android.view.*, android.widget.* |
| `domain/util/DataEncryption.kt` | Android KeyStore | android.security.keystore.*, android.util.Base64 |
| `domain/util/DebugLogger.kt` | Android Log | android.util.Log |
| `domain/util/MemoryLogger.kt` | Android Log | android.util.Log |
| `domain/util/PerformanceMonitor.kt` | Android Debug | android.os.Debug, android.util.Log |
| `domain/util/PerformanceTracker.kt` | Android Trace | android.os.Trace, android.util.Log |
| `domain/util/WeChatDetector.kt` | Android Context | android.app.ActivityManager, android.content.* |
| `domain/util/PromptBuilder.kt` | Android Log | android.util.Log |
| `domain/util/PermissionManager.kt` | Android Context | android.content.*, android.provider.* |
| `domain/util/OperationExecutor.kt` | Android Log | android.util.Log |
| `domain/util/LocalSummaryProcessor.kt` | Android Log | android.util.Log |
| `domain/util/FloatingViewDebugLogger.kt` | Android Log | android.util.Log |

**统计：16个主代码文件存在Android SDK依赖**

#### 2.2.2 引用Data层的文件（主代码）

| 文件路径 | 违规类型 | 具体依赖 |
|----------|----------|----------|
| `domain/usecase/SummarizeDailyConversationsUseCase.kt` | Data Local | com.empathy.ai.data.local.MemoryPreferences |
| `domain/usecase/GetUserProfileUseCase.kt` | Data Local | com.empathy.ai.data.local.UserProfileCache |
| `domain/usecase/UpdateUserProfileUseCase.kt` | Data Local | com.empathy.ai.data.local.UserProfileCache |
| `domain/util/DataCleanupManager.kt` | Data Local | com.empathy.ai.data.local.MemoryPreferences |
| `domain/util/AiSummaryProcessor.kt` | Data Remote | com.empathy.ai.data.remote.model.AiSummaryResponse |
| `domain/repository/FailedTaskRepository.kt` | Data Entity | com.empathy.ai.data.local.entity.FailedSummaryTaskEntity |
| `domain/service/FloatingWindowService.kt` | Data Local | com.empathy.ai.data.local.FloatingWindowPreferences |

**统计：7个主代码文件存在Data层依赖**

#### 2.2.3 引用Presentation层的文件（主代码）

| 文件路径 | 违规类型 | 具体依赖 |
|----------|----------|----------|
| `domain/service/FloatingWindowService.kt` | UI组件 | com.empathy.ai.presentation.ui.floating.FloatingBubbleView, FloatingViewV2 |

**统计：1个主代码文件存在Presentation层依赖**

#### 2.2.4 引用R资源文件的文件（主代码）

| 文件路径 | 违规类型 | 具体依赖 |
|----------|----------|----------|
| `domain/util/FloatingView.kt` | 资源引用 | com.empathy.ai.R |
| `domain/service/FloatingWindowService.kt` | 资源引用 | com.empathy.ai.R |

**统计：2个主代码文件存在R资源依赖**

### 2.3 违规文件总结

| 违规类型 | 文件数 | 占比 |
|----------|--------|------|
| Android SDK依赖 | 16 | 10.1% |
| Data层依赖 | 7 | 4.4% |
| Presentation层依赖 | 1 | 0.6% |
| R资源依赖 | 2 | 1.3% |
| **去重后总计** | **21** | **13.2%** |

> 注：部分文件存在多种违规，去重后实际违规文件数为21个

---

## 3. 架构合规性分析

### 3.1 Clean Architecture层级依赖规则

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  (UI, ViewModel, Screen, Component)                         │
│                         ↓                                    │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│  (Model, Repository接口, UseCase, Service)                  │
│  ⚠️ 应该是纯Kotlin代码，不依赖任何其他层                      │
│                         ↓                                    │
├─────────────────────────────────────────────────────────────┤
│                       Data Layer                             │
│  (Repository实现, DAO, Entity, API, Preferences)            │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 违规依赖方向检查

| 源层级 | 目标层级 | 合规性 | 说明 |
|--------|----------|--------|------|
| Domain | Android SDK | ❌ 违规 | Domain层应为纯Kotlin |
| Domain | Data | ❌ 违规 | Domain不应依赖Data实现 |
| Domain | Presentation | ❌ 违规 | Domain不应依赖UI层 |
| Domain | R资源 | ❌ 违规 | Domain不应依赖Android资源 |
| Data | Domain | ✅ 合规 | Data实现Domain接口 |
| Presentation | Domain | ✅ 合规 | UI通过UseCase调用业务 |

### 3.3 违规严重程度分类

#### 🔴 严重违规（需要重构）

| 文件 | 问题 | 影响 |
|------|------|------|
| `FloatingWindowService.kt` | 继承Android Service，引用多层 | 无法单元测试，耦合严重 |
| `FloatingView.kt` | 继承Android View | 无法单元测试 |
| `FloatingWindowManager.kt` | 深度依赖Android Context | 无法单元测试 |
| `FilterType.kt` | 引用Compose图标 | 领域模型污染 |
| `FailedTaskRepository.kt` | 接口返回Data层Entity | 违反依赖倒置原则 |

#### 🟡 中等违规（建议优化）

| 文件 | 问题 | 影响 |
|------|------|------|
| `DataEncryption.kt` | 使用Android KeyStore | 可通过接口抽象 |
| `PermissionManager.kt` | 依赖Android权限API | 可移至Presentation层 |
| `WeChatDetector.kt` | 依赖Android PackageManager | 可移至Data层 |
| UseCase层3个文件 | 直接依赖Data层实现类 | 应通过接口注入 |

#### 🟢 轻微违规（可接受但建议改进）

| 文件 | 问题 | 影响 |
|------|------|------|
| 多个Util文件 | 使用android.util.Log | 可通过Logger接口抽象 |

---

## 4. 问题与风险

### 4.1 🔴 阻塞问题 (P0)

#### P0-001: FloatingWindowService位于Domain层
- **问题描述**: `FloatingWindowService`是Android Service，却放在domain/service目录
- **影响范围**: 整个悬浮窗功能模块，无法进行纯单元测试
- **建议解决方案**: 
  1. 将`FloatingWindowService`移至`presentation/service`或`app/service`
  2. 抽取业务逻辑到Domain层的纯Kotlin类
  3. 通过接口定义服务能力，在Presentation层实现

#### P0-002: FailedTaskRepository接口返回Data层Entity
- **问题描述**: Domain层接口`FailedTaskRepository`返回`FailedSummaryTaskEntity`
- **影响范围**: 违反依赖倒置原则，Domain层被迫依赖Data层
- **建议解决方案**:
  1. 在Domain层创建`FailedSummaryTask`领域模型
  2. 修改接口返回领域模型
  3. 在Repository实现中进行Entity到Model的转换

### 4.2 🟡 风险问题 (P1)

#### P1-001: FilterType引用Compose图标
- **问题描述**: 领域模型`FilterType`包含`ImageVector`属性
- **潜在影响**: 领域模型被UI框架污染，无法在非Android环境使用
- **建议措施**:
  1. 移除`icon`属性
  2. 在Presentation层创建映射函数获取图标

#### P1-002: UseCase直接依赖Data层实现类
- **问题描述**: 3个UseCase直接注入`MemoryPreferences`、`UserProfileCache`
- **潜在影响**: 违反依赖倒置原则，增加耦合
- **建议措施**:
  1. 为这些类创建Domain层接口
  2. 通过接口注入而非实现类

### 4.3 🟢 优化建议 (P2)

#### P2-001: 统一日志抽象
- **当前状态**: 多个Util文件直接使用`android.util.Log`
- **优化建议**: 创建`Logger`接口，在DI层提供Android实现
- **预期收益**: Domain层可进行纯单元测试

#### P2-002: 加密服务抽象
- **当前状态**: `DataEncryption`直接使用Android KeyStore
- **优化建议**: 创建`EncryptionService`接口
- **预期收益**: 支持多平台，便于测试

### 4.4 ⚪ 待确认问题

| 编号 | 问题 | 需要确认的内容 |
|------|------|----------------|
| Q-001 | FloatingWindowService重构范围 | 是否需要完全重构还是渐进式改进 |
| Q-002 | 测试覆盖优先级 | 哪些违规文件需要优先支持单元测试 |

---

## 5. 机制分析（框架设计者视角）

### 5.1 Clean Architecture运行机制

**正常流程**：
```
用户操作 → Screen → ViewModel → UseCase → Repository接口 → Repository实现 → 数据源
                                    ↓
                              Domain Model（纯Kotlin）
```

**当前问题流程**：
```
用户操作 → Screen → ViewModel → UseCase → Repository接口 → Repository实现
                        ↓              ↓
                   Data实现类      Data Entity（违规）
                        ↓
                   Android SDK（违规）
```

### 5.2 潜在根因树（Root Cause Tree）

```
Clean Architecture违规
├── 框架机制层
│   ├── 缺乏架构守护工具（如ArchUnit）
│   ├── 代码审查未严格检查依赖方向
│   └── 没有模块化隔离（单模块项目）
├── 模块行为层
│   ├── FloatingWindowService需要Android Service生命周期
│   ├── 加密需要Android KeyStore
│   └── 日志需要Android Log
├── 使用方式层
│   ├── 开发者为了方便直接引用实现类
│   ├── 领域模型包含UI属性（FilterType.icon）
│   └── Repository接口返回Entity而非领域模型
└── 环境层
    ├── Android平台特性限制
    └── 快速迭代导致技术债务积累
```

### 5.3 排查路径（从框架到应用层）

1. **检查模块边界**
   - 确认是否有Gradle模块隔离
   - 检查是否配置了依赖约束

2. **检查依赖注入配置**
   - 确认DI模块是否正确分层
   - 检查是否有跨层注入

3. **检查接口定义**
   - 确认Repository接口返回类型
   - 检查是否有Domain层接口依赖Data层类型

4. **检查具体实现**
   - 逐个检查违规文件
   - 分析违规原因和修复成本

---

## 6. 最可能的根因（基于机制推理）

### 根因1: 缺乏模块化隔离
**推理过程**：
- 当前项目是单模块结构（app模块）
- 没有Gradle模块边界强制依赖方向
- 开发者可以自由import任何包
- 导致依赖方向无法在编译时检查

### 根因2: Android Service必须继承系统类
**推理过程**：
- `FloatingWindowService`需要前台服务能力
- Android Service必须继承`android.app.Service`
- 将Service放在Domain层是为了方便注入UseCase
- 但这违反了Clean Architecture原则

### 根因3: 快速迭代导致技术债务
**推理过程**：
- 项目经历多次功能迭代（悬浮窗重构、提示词系统等）
- 为了快速交付，直接引用实现类而非接口
- 领域模型添加了UI属性以简化开发
- 技术债务逐渐积累

---

## 7. 稳定修复方案

### 7.1 短期方案（立即可行）

#### 方案A: 创建Logger接口
```kotlin
// domain/util/Logger.kt
interface Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String)
    fun i(tag: String, message: String)
}

// data/util/AndroidLogger.kt
class AndroidLogger : Logger {
    override fun d(tag: String, message: String) = Log.d(tag, message)
    // ...
}
```

**原理**：通过接口抽象隔离Android依赖，Domain层只依赖接口

#### 方案B: 修复FailedTaskRepository
```kotlin
// domain/model/FailedSummaryTask.kt
data class FailedSummaryTask(
    val id: Long,
    val contactId: String,
    val summaryDate: String,
    val failureReason: String,
    val retryCount: Int,
    val failedAt: Long,
    val lastRetryAt: Long?
)

// domain/repository/FailedTaskRepository.kt
interface FailedTaskRepository {
    suspend fun getPendingTasks(): Result<List<FailedSummaryTask>>
    // ...
}
```

**原理**：Domain层定义领域模型，Data层负责Entity到Model的转换

#### 方案C: 移除FilterType的icon属性
```kotlin
// domain/model/FilterType.kt
enum class FilterType(val displayName: String) {
    ALL("全部"),
    AI_SUMMARY("只看AI"),
    // ...
}

// presentation/util/FilterTypeIcons.kt
fun FilterType.getIcon(): ImageVector = when (this) {
    FilterType.ALL -> Icons.Default.FilterList
    // ...
}
```

**原理**：领域模型保持纯净，UI属性在Presentation层映射

### 7.2 中期方案（需要重构）

#### 方案D: 重构FloatingWindowService
```
presentation/
├── service/
│   └── FloatingWindowService.kt  # Android Service实现
└── floating/
    └── FloatingWindowController.kt  # UI控制器

domain/
├── service/
│   └── FloatingWindowBusinessLogic.kt  # 纯业务逻辑
└── model/
    └── FloatingWindowState.kt  # 状态模型
```

**原理**：
- Android Service只负责生命周期管理
- 业务逻辑抽取到Domain层纯Kotlin类
- 通过接口定义服务能力

### 7.3 长期方案（架构升级）

#### 方案E: 模块化改造
```
:domain  (纯Kotlin模块，无Android依赖)
:data    (依赖:domain)
:presentation (依赖:domain)
:app     (依赖所有模块)
```

**原理**：
- Gradle模块边界强制依赖方向
- 编译时检查违规依赖
- 从根本上防止架构腐化

---

## 8. 后续任务建议

### 8.1 推荐的任务顺序

1. **修复FailedTaskRepository** - 影响范围小，立即可行
2. **创建Logger接口** - 影响多个文件，但改动简单
3. **移除FilterType.icon** - 需要修改UI层调用
4. **重构FloatingWindowService** - 工作量大，需要详细设计

### 8.2 预估工作量

| 任务 | 预估时间 | 复杂度 | 依赖 |
|------|----------|--------|------|
| 修复FailedTaskRepository | 2小时 | 低 | 无 |
| 创建Logger接口 | 4小时 | 低 | 无 |
| 移除FilterType.icon | 2小时 | 低 | 无 |
| 重构FloatingWindowService | 2-3天 | 高 | 详细设计 |
| 模块化改造 | 1-2周 | 高 | 全面评估 |

### 8.3 风险预警

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 重构引入新Bug | 中 | 高 | 充分测试，渐进式改进 |
| 模块化改造工作量超预期 | 高 | 中 | 分阶段实施 |
| 团队不熟悉Clean Architecture | 低 | 中 | 编写架构指南 |

---

## 9. 关键发现总结

### 9.1 核心结论

1. **Domain层存在21个违规文件**，占总文件数的13.2%
2. **最严重的违规是FloatingWindowService**，它是Android Service却放在Domain层
3. **根本原因是缺乏模块化隔离**，无法在编译时检查依赖方向
4. **短期可通过接口抽象解决大部分问题**，长期需要模块化改造

### 9.2 技术要点

| 要点 | 说明 | 重要程度 |
|------|------|----------|
| Domain层应为纯Kotlin | 不依赖Android SDK和其他层 | 高 |
| 依赖倒置原则 | 高层不应依赖低层实现 | 高 |
| 接口抽象 | 通过接口隔离平台依赖 | 中 |
| 模块化隔离 | Gradle模块强制依赖方向 | 高 |

### 9.3 注意事项

- ⚠️ 重构FloatingWindowService需要详细设计，避免影响现有功能
- ⚠️ 修改Repository接口需要同步修改所有调用方
- ⚠️ 模块化改造是大工程，建议分阶段实施

---

## 10. 附录

### 10.1 参考资料
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Android官方架构指南](https://developer.android.com/topic/architecture)

### 10.2 术语表

| 术语 | 解释 |
|------|------|
| Clean Architecture | 一种软件架构模式，强调依赖方向从外向内 |
| Domain Layer | 领域层，包含业务逻辑，应为纯Kotlin代码 |
| Data Layer | 数据层，负责数据访问和持久化 |
| Presentation Layer | 表现层，负责UI展示和用户交互 |
| 依赖倒置原则 | 高层模块不应依赖低层模块，两者都应依赖抽象 |

---

**文档版本**: 1.0  
**最后更新**: 2025-12-23
