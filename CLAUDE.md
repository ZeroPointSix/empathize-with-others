# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

**共情AI助手 (Empathy AI)** - Android 智能社交沟通辅助应用。
- **架构**: Clean Architecture + MVVM + Jetpack Compose
- **语言**: Kotlin 2.0.21 (K2 Compiler)
- **核心依赖**:
    - AGP 8.7.3 / Compose BOM 2024.12.01 / Hilt 2.52
    - Room 2.6.1 / Retrofit 2.11.0 / Coroutines 1.9.0
- **仓库配置**: 腾讯云/阿里云镜像加速（优先使用腾讯云）
- **构建系统**: 自定义 BuildSrc 模块，支持版本自动管理和图标更新
- **原则**: 零后端 (Zero-Backend)、BYOK (Bring Your Own Key)、隐私优先
- **状态**: v1.1.0 (versionCode: 10101, Dev)
- **模块结构**:
  - `app/` - Application 层 (服务、通知、UI 工具类)
  - `domain/` - 纯业务逻辑 (UseCase、Repository 接口、模型)
  - `data/` - 数据层 (Repository 实现、数据库、网络)
  - `presentation/` - UI 层 (Compose、ViewModel、导航)

**代码规模统计** (基于 2026-01-15 最新扫描):
- **主源码**: 636个文件 (domain: 213个, data: 108个, presentation: 285个, app: 30个)
- **测试文件**: 99个 (单元测试: 99个, Android测试: 13个)
- **ViewModel**: 27个
- **UseCase**: 60个
- **Repository**: 18个接口 + 27个实现
- **数据库**: Room v16 (11张表, 10个DAO, 16个迁移脚本)
- **导航路由**: 23个
- **UI组件**: 700+个Composable函数

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
/kiro QuickBuild    # 智能构建 - 自动错误分析并提供修复建议
/kiro QuickTest     # 智能测试 - 自动识别并运行相关测试用例
/kiro GenTest       # 生成测试 - 为指定功能生成完整测试用例
/kiro CodeReview    # 代码审查 - 架构/质量/安全审查
/kiro DocReview     # 文档审查 - 规范检查与一致性验证
/kiro Research      # 深度研究 - 多维度代码分析
/kiro Ask           # 需求澄清 - 交互式需求确认
/git                # Git 智能操作 - 提交、分支、冲突解决
```

### 自定义构建工具 (buildSrc/)
项目使用自定义 BuildSrc 模块管理构建流程：
- **VersionUpdatePlugin** - 自动版本管理和图标更新
- **BackupManager** - 自动备份构建产物
- **CommitParser** - 语义化提交解析
- **IconManager** - 图标资源自动管理
- **GradlePropertiesUpdater** - Gradle 配置自动更新

### 版本管理
构建时自动执行：
1. 解析 Git 提交信息生成版本号
2. 自动更新图标资源
3. 备份历史版本
4. 生成版本历史文件

## 构建优化与性能

### Gradle 优化配置 (gradle.properties)
```properties
# 启用构建缓存 (推荐)
org.gradle.caching=true

# 配置缓存 (实验性，可提升配置速度)
org.gradle.configuration-cache=true

# 并行构建
org.gradle.parallel=true

# 增大内存 (建议 4GB+)
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m

# 启用配置缓存 (实验性)
org.gradle.configureondemand=true
```

### 构建缓存管理
```bash
# 重启 Gradle Daemon (解决内存问题)
gradlew --stop
gradlew.bat assembleDebug

# 清理构建缓存
gradlew clean

# 使用构建缓存
gradlew assembleDebug --build-cache
```

### 依赖下载优化
- 项目已配置腾讯云/阿里云镜像，依赖下载速度显著提升
- 优先使用腾讯云镜像 (mirrors.cloud.tencent.com)
- 阿里云镜像作为备用 (maven.aliyun.com)
- 官方仓库作为最后备用

### 编译加速建议
1. **开发阶段**: 使用 `scripts\quick-build.bat` 跳过 lint/test
2. **完整构建**: 使用 `--parallel` 开启并行编译
3. **问题排查**: 使用 `--info` 或 `--stacktrace` 查看详细日志
4. **性能分析**: 使用 `--profile` 生成构建性能报告

### 常见构建问题解决
- **Kapt 编译慢**: 使用 KSP (Room 已迁移) 替代 Kapt (Hilt/其他)
- **内存不足**: 增加 JVM 内存 `-Xmx4g` 或更高
- **依赖冲突**: 检查 `gradle/libs.versions.toml` 版本统一
- **镜像不可用**: 切换到备用镜像或官方仓库

## 架构与代码结构

项目遵循严格的 Clean Architecture 分层：

```
app/              -> Application入口, DI配置, Service (依赖 data, presentation)
presentation/     -> UI (Compose), ViewModel, Navigation (依赖 domain)
domain/           -> 纯 Kotlin 业务逻辑, UseCase, Repository 接口 (无 Android 依赖)
data/             -> Repository 实现, Room DB, Network (依赖 domain)
Rules/            -> 开发规范与多 Agent 协作规则
buildSrc/         -> 自定义构建工具 (版本管理、图标更新、备份)
```

**详细代码统计** (2026-01-15):
- **domain模块** (213个文件): 173模型 + 18仓库接口 + 60用例 + 2服务 + 29工具
- **data模块** (108个文件): 10DAO + 11Entity + 27仓库实现 + 8DI + 6parser
- **presentation模块** (285个文件): 27ViewModel + 5导航 + 12主题 + 93屏幕 + 180+组件
- **app模块** (30个文件): 14DI + Application + Service

### 仓库配置 (settings.gradle.kts)
- **优先**: 腾讯云 Maven 镜像 (https://mirrors.cloud.tencent.com/)
- **备用**: 阿里云镜像 (https://maven.aliyun.com/)
- **官方**: Google、Maven Central (最后备用)
- **第三方**: JitPack (https://jitpack.io)

### 模块职责
- **app 模块**: Android 服务、通知、权限管理、Application 类
- **data 模块**: 数据库 (Room)、网络 (Retrofit)、Repository 实现
- **domain 模块**: 纯 Kotlin 业务逻辑、UseCase、Repository 接口
- **presentation 模块**: Compose UI、ViewModel、Navigation
- **buildSrc 模块**: 自定义构建工具和自动化脚本

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
-   **参数传递**: 支持通过导航参数传递 `sessionId`、`createNew` 等标识。
-   **栈治理**: 遵循 PRD-00035 导航栈治理规范，防止栈堆积和重复入栈。

## 调试与诊断

### AI 功能调试
```bash
# 基础 AI 日志 (显示关键参数如 Temperature、MaxTokens)
scripts\ai-debug.bat              # 实时监听 AI 日志
scripts\ai-debug.bat -h           # 获取最近100条
scripts\ai-debug.bat -d 127.0.0.1:7555  # 指定模拟器

# 完整 AI 日志 (包含完整 Prompt)
scripts\ai-debug-full.bat         # 完整 AI 请求/响应日志
```

### 通用日志调试
```bash
scripts\logcat.bat                # 查看 WARN+ 日志
scripts\logcat.bat -e             # 仅查看 ERROR
scripts\quick-error.bat           # 获取最近的 ERROR 日志 (一次性)
```

### 设备测试
```bash
# 连接测试 (需连接设备/模拟器)
gradlew.bat connectedAndroidTest

# 设备兼容性测试
scripts\run-wechat-compatibility-tests.bat

# 调试脚本集合
scripts\device-test.bat           # 设备测试
scripts\verify-regex-fix.bat      # 验证修复
```

### 错误处理策略
1. **构建错误**: 使用 `/QuickBuild` 获取智能错误分析和修复建议
2. **测试失败**: 使用 `/QuickTest` 自动运行相关测试并分析失败原因
3. **AI 功能问题**: 使用 `ai-debug-full.bat` 获取完整请求/响应日志
4. **导航问题**: 检查 NavController 栈状态，使用 `popUpTo` 清理策略

## 多 Agent 协作

**强制规则**: 开始任何任务前**必须**先读取 `WORKSPACE.md` 检查冲突并记录任务开始。

### Agent 角色与职责
- **Codex (Free Explorer)**: 自由探索、BUG 修复、创新功能开发
- **Roo (Review)**: 代码审查、质量检查、架构评估
- **Kiro**: 代码开发、测试生成、调试支持
- **Claude**: 功能设计、文档编写、架构规划

### 协作规范 (Rules/workspace-rules.md)
1. **任务前检查**: 读取 `WORKSPACE.md` → 检查冲突 → 记录任务开始
2. **实时同步**: 重要里程碑更新到 WORKSPACE，问题立即记录
3. **任务后更新**: 移动到"已完成" → 更新AI状态 → 更新文档版本 → 添加变更日志
4. **违规处理**: 立即停止、补充更新、道歉并承诺遵守

### 工作流机制
- **决策日志 (Decision Journal)**: `.kiro/templates/DECISION_JOURNAL.template.md`
- **任务跟踪**: `WORKSPACE.md` - 当前状态、已完成任务、待办队列
- **规范文档**: `Rules/` 目录下的开发规范和协作规则
- **文档系统**:
  - 功能规格: `.kiro/specs/`
  - BUG 文档: `文档/开发文档/BUG/`
  - 测试用例: `文档/开发文档/TE/`
  - 需求文档: `文档/开发文档/PRD/`

### 智能体命令
- 所有命令文档位于 `.kiro/commands/*.md`
- 支持 `/QuickBuild`、`/QuickTest`、`/CodeReview` 等快速操作
- 复杂任务参考 `.kiro/steering/quick-start.md`
