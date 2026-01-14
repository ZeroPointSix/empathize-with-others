# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

**共情AI助手 (Empathy AI)** - Android 智能社交沟通辅助应用。
- **架构**: Clean Architecture + MVVM + Jetpack Compose
- **语言**: Kotlin 2.0.21 (K2 Compiler)
- **核心依赖**:
    - AGP 8.7.3 / Compose BOM 2024.12.01 / Hilt 2.52
    - Room 2.6.1 / Retrofit 2.11.0 / Coroutines 1.9.0
- **原则**: 零后端 (Zero-Backend)、BYOK (Bring Your Own Key)、隐私优先
- **状态**: v1.1.0 (Dev)

## 常用开发命令

**注意**: Windows 环境下请使用 `gradlew.bat`。

### 构建与运行
```bash
# 基础构建
gradlew.bat assembleDebug          # Debug 构建
gradlew.bat assembleRelease        # Release 构建
gradlew.bat clean                  # 清理

# 快速开发脚本 (Windows)
scripts\quick-build.bat            # 快速构建 (跳过 lint/test)
scripts\dev-cycle.bat              # 开发循环 (构建+测试+安装)
```

### 测试与检查
```bash
# 单元测试
gradlew.bat test                   # 运行所有单元测试
gradlew.bat :domain:test           # 运行 domain 模块测试
gradlew.bat :presentation:test     # 运行 presentation 模块测试

# 运行特定测试
gradlew.bat :domain:test --tests "com.empathy.ai.domain.usecase.SendAdvisorMessageUseCaseTest"
gradlew.bat :presentation:test --tests "*BUG00068*"

# Android 连接测试 (需连接设备/模拟器)
gradlew.bat connectedAndroidTest

# 代码质量
gradlew.bat lint                   # 全局 Lint
gradlew.bat ktlintCheck            # 代码风格检查
```

### 调试工具
```bash
# 日志调试
scripts\logcat.bat                 # 查看 WARN+ 日志
scripts\logcat.bat -e              # 仅查看 ERROR
scripts\quick-error.bat            # 获取最近 ERROR (一次性)

# AI 功能调试
scripts\ai-debug.bat               # 实时监听 AI 请求/响应 (简略)
scripts\ai-debug-full.bat          # 完整 AI 日志 (含 Prompt)

### Kiro 快速命令 (`.kiro/commands/`)
```bash
/kiro QuickBuild    # 快速构建
/kiro QuickTest     # 快速测试
/kiro GenTest       # 生成测试用例
/kiro CodeReview    # 代码审查
/kiro DocReview     # 文档审查
/kiro Research      # 深度研究
/kiro Ask           # 需求澄清
/git                # Git 智能操作
```

## 架构与代码结构

项目遵循严格的 Clean Architecture 分层：

```
app/              -> Application入口, DI配置, Service (依赖 data, presentation)
presentation/     -> UI (Compose), ViewModel, Navigation (依赖 domain)
domain/           -> 纯 Kotlin 业务逻辑, UseCase, Repository 接口 (无 Android 依赖)
data/             -> Repository 实现, Room DB, Network (依赖 domain)
Rules/            -> 开发规范与多 Agent 协作规则
```

### 关键架构规则
1.  **依赖方向**: `app` -> `data`/`presentation` -> `domain`。`domain` 层**严禁**依赖 Android SDK。
2.  **数据流**: UI -> ViewModel -> UseCase -> Repository (Interface) -> Repository (Impl) -> Data Source.
3.  **状态管理**: 使用 `StateFlow` 和 `data class UiState`。Compose 中避免直接使用 `mutableStateOf`，应进行状态提升。
4.  **错误处理**: 统一使用 `Result<T>`。
5.  **组件复用**: 遵循原子(Atomic)-分子(Molecule)-有机体(Organism)-模板(Template) 组件设计模式。优先复用 `presentation/ui/component/` 下的现有组件。

### Hilt 依赖注入
-   **Data 模块**: 提供 Database, Network, Repository 实现 (`di/` 目录)。
-   **App 模块**: 提供全局 Service, ViewModel 工厂等 (`di/` 目录)。
-   **注解处理**: Data 模块使用 KSP (Room)，App/Presentation 使用 KAPT (Hilt)。

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
4.  **数据库**: Room Schema 变更**必须**伴随 Migration 脚本 (`MIGRATION_x_y`) 和测试。
5.  **UI 开发**: 优先使用项目封装的 `Ios*` 组件 (如 `IosButton`, `IosTextField`) 保持设计一致性。

## 导航系统 (Navigation)

-   **核心**: 单一 Activity (`MainActivity`) + Compose Navigation。
-   **定义**: `presentation/.../navigation/NavGraph.kt` 定义路由图。
-   **路由**: 常量定义在 `NavRoutes.kt`。
-   **Tab页**: 使用 `BottomNavScaffold` 管理，支持状态保持。
-   **跳转**: 使用 `NavController`，注意处理堆栈清理 (`popUpTo`) 避免循环。

## 多 Agent 协作

**重要**: 开始任务前**必须**先读取 `WORKSPACE.md` 检查是否有冲突任务。

-   **Agent 角色**:
    -   **Roo**: 代码审查 (Review)
    -   **Kiro**: 代码开发与调试
    -   **Claude**: 功能设计与文档编写
-   **规范**: 参考 `Rules/` 目录下的文档，特别是 `Rules/workspace-rules.md`。
-   **文档**: 功能规格在 `.kiro/specs/`，Bug 修复文档在 `文档/开发文档/BUG/`。
-   **任务**: 复杂任务请先阅读 `.kiro/steering/quick-start.md`。
