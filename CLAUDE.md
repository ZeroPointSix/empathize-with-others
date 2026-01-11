# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**共情AI助手** - 基于 Android 平台的智能社交沟通辅助应用，采用 Clean Architecture + MVVM 架构模式。

### 核心原则
- **零后端 (Zero-Backend)**: 无服务器，无用户账户体系
- **BYOK**: AI能力通过用户自备API密钥直连第三方服务
- **隐私优先**: 敏感数据本地脱敏，密钥硬件级加密存储
- **无感接入**: 悬浮窗+无障碍服务与宿主App交互

### 技术栈
- Kotlin 2.0.21 + K2编译器
- Gradle 8.13 + AGP 8.7.3
- Jetpack Compose BOM 2024.12.01
- Hilt 2.52 + Room v16
- 最低SDK: 24, 目标SDK: 35

## 常用命令

**注意**: Windows环境下使用 `gradlew.bat` 代替 `./gradlew`

```bash
# 构建与测试
./gradlew assembleDebug          # Debug构建
./gradlew assembleRelease        # Release构建
./gradlew test                   # 所有单元测试
./gradlew testDebugUnitTest      # Debug单元测试
./gradlew connectedAndroidTest   # 连接设备运行Android测试
./gradlew clean                  # 清理构建缓存

# 单个模块测试
./gradlew :presentation:test     # presentation模块测试
./gradlew :domain:test           # domain模块测试
./gradlew :data:test             # data模块测试

# 运行单个测试类
./gradlew :domain:test --tests "com.empathy.ai.domain.usecase.SendAdvisorMessageUseCaseTest"

# 运行特定Bug测试
./gradlew :presentation:test --tests "*BUG00058*"
./gradlew :presentation:test --tests "*SessionManagement*"

# Lint检查
./gradlew lint                   # 全局Lint
./gradlew :presentation:lint     # presentation模块Lint
./gradlew ktlintCheck            # Kotlin代码风格检查

# 调试脚本 (Windows)
scripts\logcat.bat -e            # ERROR日志
scripts\quick-error.bat          # 快速查看最近错误
scripts\ai-debug.bat             # AI请求调试
scripts\quick-test.bat           # 快速测试
scripts\dev-cycle.bat            # 开发循环（构建+测试+安装）

# ADB调试
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.empathy.ai/.ui.MainActivity
adb shell dumpsys activity activities | findstr ActivityRecord  # 查看Activity栈
adb logcat -c && adb logcat *:E  # 清除日志并只显示ERROR级别
```

## 测试覆盖

| 模块 | 单元测试 | Android测试 | 关键测试 |
|------|----------|-------------|----------|
| domain | 43 | - | UseCase、Model、PromptBuilder |
| presentation | 59 | 7 | ViewModel、Compose UI、Bug回归测试 |
| data | 25 | 6 | Repository、Database |
| app | 141 | 26 | Application初始化、服务测试 |

**Bug回归测试**:
- `BUG00058CreateNewSessionTest.kt` - 新建会话功能测试
- `BUG00059RegenerateMessageRoleTest.kt` - 消息重新生成角色测试
- `BUG00060SessionManagementTest.kt` - 会话管理增强测试
- `BUG00061SessionHistoryNavigationTest.kt` - 会话历史导航测试
- `BUG00064ManualSummaryTest.kt` - AI手动总结功能测试
- `BUG00066EditBrainTagTest.kt` - 大脑标签编辑功能测试

## 架构结构

```
共情AI助手 (根)
├── app/              应用入口、DI配置、Android服务
├── build/            自定义Gradle插件
├── domain/           纯Kotlin业务层，无Android依赖
│   ├── model/        业务模型
│   ├── repository    仓库接口
│   ├── usecase/      业务用例
│   ├── service/      领域服务
│   └── util/         工具类
├── data/             数据层实现
│   ├── local/        Room数据库、SharedPreferences
│   ├── remote/       Retrofit网络请求
│   ├── repository/   仓库实现
│   └── parser/       AI响应解析器
├── presentation/     UI层
│   ├── ui/           Compose组件
│   ├── viewmodel/    ViewModel
│   ├── navigation/   导航系统
│   └── theme/        主题配置
└── Rules/            多AI协作规则
```

### 依赖方向
`app` → `data/presentation` → `domain`（严格单向依赖）

### 关键约束
- **domain层禁止引入Android依赖**（Room, Context, etc.）
- 所有API定义在domain层，data层实现
- 敏感数据处理使用 `PrivacyEngine`
- 错误处理统一使用 `Result<T>` 类型
- Hilt 模块配置：`di/` 目录下的 HiltModule

### 注解处理器配置
| 模块 | 处理器 | 用途 |
|------|--------|------|
| data | KSP | Room 数据库注解处理 |
| data | Kapt | Retrofit/Moshi 注解处理 |
| app/presentation | Kapt | Hilt 依赖注入 |

### Room 数据库版本
- **当前版本**: v16
- **迁移路径**: `data/src/main/kotlin/com/empathy/ai/data/local/MIGRATION_*.kt`
- **Schema目录**: `data/schemas/com.empathy.ai.data.local.AppDatabase/`
- **迁移策略**: 每次架构变更添加增量迁移脚本
- **版本查看**: 检查 `AppDatabase.kt` 中的 `@Database(version = X)` 注解

### Hilt 模块索引
| 模块 | 文件 | 职责 |
|------|------|------|
| app | `AppModule.kt` | Application 级注入 |
| app | `ServiceModule.kt` | Android Service 注入 |
| data | `DatabaseModule.kt` | Room 数据库配置 |
| data | `NetworkModule.kt` | Retrofit/OkHttp 配置 |
| data | `RepositoryModule.kt` | Repository 实现绑定 |
| presentation | `ViewModelModule.kt` | ViewModel Factory |

### 导航系统结构（2026-01-10更新）
- **NavGraph.kt** - 主导航图，定义所有导航路由和导航图组合（支持Tab页面和非Tab页面分离）
- **NavRoutes.kt** - 路由常量定义（contactList, contactDetail, chat, settings 等）
- **BottomNavTab.kt** - 底部导航标签枚举（CONTACTS, AI_ADVISOR, SETTINGS）
- **NonTabNavGraph.kt** - 非Tab页面导航容器，用于承载详情页等非Tab路由
- **BottomNavScaffold.kt** - 带页面缓存的底部导航Scaffold（2026-01-10新增，解决Tab切换黑屏问题）
- **PromptEditorNavigation.kt** - 提示词编辑器导航配置
- **屏幕级导航**: `ContactDetailNavigation`, `PromptEditorNavigation` 等

**悬浮窗+无障碍服务交互**
```
用户选择文本 → 无障碍服务捕获 → FloatingWindowService → ChatViewModel → AI分析 → 结果悬浮窗显示
```
- **悬浮窗服务**: `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
- **无障碍服务**: 捕获用户选中的文本并传递给悬浮窗服务

**每日总结流程**
```
应用启动 → 检查跨天 → SummarizeDailyConversationsUseCase → AI生成 → DailySummary保存 → 本地通知
```
- **触发条件**: 跨天或首次运行
- **清理策略**: 每7天清理90天前的对话记录

**AI 请求流程**
```
ViewModel → UseCase → Repository → AI Provider → Retrofit → AI Service
```
- **多服务商支持**: OpenAI、Azure OpenAI、阿里云、百度、智谱、腾讯混元、讯飞星火
- **响应解析**: 增强型 JSON 解析 + 多级降级策略

## 关键系统组件

### 隐私引擎 (PrivacyEngine)
- **职责**: 敏感数据本地脱敏处理
- **位置**: `domain/src/main/kotlin/com/empathy/ai/domain/service/PrivacyEngine.kt`
- **功能**: 数据脱敏、加密存储、密钥硬件级保护

### 数据清理系统
- **管理类**: `DataCleanupManager`
- **清理周期**: 每7天执行一次
- **清理范围**: 90天前的对话记录和每日总结
- **保留数据**: 联系人画像、大脑标签、用户配置等核心数据

### 版本更新插件
- **插件**: `VersionUpdatePlugin`
- **配置位置**: `app/build.gradle.kts`
- **功能**: 版本更新管理、备份保留（最大50个备份，保留30天）
- **实现**: `build/src/main/kotlin/com/empathy/ai/build/VersionUpdatePlugin.kt`

### AI 服务商系统
- **接口定义**: `domain/src/main/kotlin/com/empathy/ai/domain/model/AiProvider.kt`
- **仓库实现**: `data/src/main/kotlin/com/empathy/ai/data/repository/AiProviderRepositoryImpl.kt`
- **支持服务商**: OpenAI、Azure OpenAI、阿里云、百度、智谱、腾讯混元、讯飞星火（7家主流AI服务商）
- **模型配置**: 每个服务商支持自定义模型名称和 API 版本

### 开发者模式
- **功能**: 系统提示词管理、调试工具
- **入口**: 设置页面 → 开发者选项
- **用途**: 调试 AI 响应、查看日志、管理提示词

### 进行中的问题修复（2026-01-10）
- **BUG-00062**: AI军师会话管理功能增强（已识别，待实现）
- **BUG-00067**: 提示词功能优化（已识别，待实现）
- **BUG-00068**: AI对话输入框与配置回退及非Tab功能屏幕展示问题（已识别，待实现）
| BUG编号 | 问题描述 | 状态 |
|---------|----------|------|
| BUG-00064 | AI手动总结功能未生效问题 | 已修复 |
| BUG-00065 | 联系人搜索功能优化 | 进行中 |

详细修复方案见: [文档/开发文档/BUG/](./文档/开发文档/BUG/)

### 多AI协作规则（关键规则）

项目使用多 AI 工具协作开发：
- **Roo**: 代码审查（Review）
- **Kiro**: 代码实现与调试
- **Claude**: 功能设计与文档编写

**任务执行前必须**:
1. 读取 `Rules/workspace-rules.md` 检查协作规则
2. 如有其他 AI 正在执行相关任务，**必须停止并询问用户**
3. 在 Rules/WORKSPACE.md 中记录任务开始信息（如存在）

**强制优先级**: `Rules/workspace-rules.md` 中的规则 > 所有其他规则

**MCP 服务器配置**:
- `sequentialthinking`: 复杂问题分步推理
- `context7`: 项目文档实时查询

### 组件复用系统

项目采用 **原子-分子-有机体-模板** 四级组件架构：

| 层级 | 示例 | 说明 |
|------|------|------|
| 原子组件 | `IosButton`, `IosTextField`, `AvatarView` | 基础UI单元，无业务逻辑 |
| 分子组件 | `IosSearchBar`, `ModernPersonaTab` | 组合原子组件，有简单交互 |
| 有机体组件 | `FactStreamCard`, `EmotionTimelineView` | 复杂业务逻辑，独立功能单元 |
| 模板组件 | `AiAdvisorChatScreen` | 页面级组合，特定场景使用 |

**组件位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/`

## Rules 多AI协作规则目录

项目使用 `Rules/` 目录管理多AI协作状态（主目录）：

| 目录 | 用途 | 规则内容 |
|------|------|----------|
| `Rules/` | 主协作规则目录 | 工作流规则、项目开发规范 |
| `.kiro/steering/` | 项目决策文档 | 产品概览、技术栈、项目结构、快速开始 |
| `.kiro/specs/` | 功能规格文档 | 需求、设计、任务分解文档 |
| `.kiro/test/` | 测试用例文档 | 测试用例和测试指南 |

**核心规则文件**:
- `Rules/workspace-rules.md` - 多AI协作核心规则（强制执行）
- `Rules/WORKSPACE.md` - 当前任务状态追踪
- `Rules/项目开发规范.md` - 代码规范、命名约定
| 文件/目录 | 用途 |
|-----------|------|
| `Rules/workspace-rules.md` | 多AI协作核心规则（强制执行） |
| `Rules/ai-status.md` | AI工具状态监控 |
| `Rules/项目开发规范.md` | 完整开发规范 |

**注意**: `Rules/WORKSPACE.md` 可能不存在，如需使用请先创建。

**协作流程**:
1. 任务开始前 → 读取 `Rules/workspace-rules.md` 检查协作规则
2. 任务进行中 → 实时更新里程碑和发现问题
3. 任务完成后 → 更新任务状态并记录变更日志

**关键规则（必须遵守）**:
- 每次任务执行前读取 `Rules/workspace-rules.md`
- 如发现其他AI正在执行相关任务，**必须停止并询问用户**
- 所有回答和文档使用中文编写

## 功能规格文档

项目使用 `.kiro/specs/` 目录存储功能规格文档，`.kiro/test/` 存储测试用例：

| 目录 | 内容 |
|------|------|
| `.kiro/specs/` | 需求、设计、任务分解文档 |
| `.kiro/specs/_archived/` | 已归档的历史文档 |
| `.kiro/steering/` | 产品决策、quick-start 指南 |
| `.kiro/commands/` | 命令模板（QuickBuild, QuickTest 等） |
| `.kiro/test/` | 测试用例和测试指南 |

**典型规格文档结构**:
- `requirements.md` - 需求定义
- `design.md` - 设计方案
- `tasks.md` - 任务分解

**当前分支**: `PRD34`（基于 Git）

## 模块文档

详细架构信息参考各模块文档：
- [domain/CLAUDE.md](./domain/CLAUDE.md) - 领域层详细文档（纯Kotlin业务逻辑）
- [data/CLAUDE.md](./data/CLAUDE.md) - 数据层详细文档（Room/Retrofit实现）
- [presentation/CLAUDE.md](./presentation/CLAUDE.md) - 表现层详细文档（Compose UI/ViewModel）
- [app/src/main/java/com/empathy/ai/app/CLAUDE.md](./app/src/main/java/com/empathy/ai/app/CLAUDE.md) - 应用层详细文档（Application/Service）

## 编码规范

- 遵循 SOLID、KISS、DRY、YAGNI 原则
- 所有公共API必须有KDoc注释
- 使用 `Result<T>` 统一处理成功/失败
- 测试文件命名: `XxxTest.kt` / `XxxSpec.kt`

### 导入分组顺序（必须严格遵守）
```
1. Kotlin 标准库
2. 第三方库（按字母顺序）
3. AndroidX 库
4. Jetpack Compose 库
5. 项目内部模块（按模块依赖顺序）
6. 当前包的类
```

### 日志级别规范
| 级别 | 使用场景 | 示例 |
|------|----------|------|
| ERROR | 应用崩溃、关键功能失败 | `Logger.e("AI响应解析失败", e)` |
| WARN | 可恢复的错误、异常情况 | `Logger.w("网络请求超时，使用缓存")` |
| INFO | 重要业务状态变化 | `Logger.i("用户登录成功")` |
| DEBUG | 开发调试信息 | `Logger.d("消息发送: $content")` |

### 协程作用域使用
| 作用域 | 使用场景 | 禁止场景 |
|--------|----------|----------|
| `viewModelScope` | ViewModel 中的业务操作 | 长时间后台任务 |
| `rememberCoroutineScope()` | Composable 中的一次性操作 | 需要跟随 ViewModel 生命周期 |
| `GlobalScope` | **禁止使用** | 所有场景 |

### 测试覆盖率目标
| 模块 | 最低覆盖率 | 建议覆盖率 |
|------|-----------|-----------|
| domain | 80% | 90% |
| data | 70% | 80% |
| presentation | 60% | 70% |

## 仓库镜像配置

项目使用国内镜像加速依赖下载，优先级：腾讯云 > 阿里云 > 官方仓库

## 关键命名约定

| 类型 | 前缀/后缀 | 示例 |
|------|-----------|------|
| UseCase | `XxxUseCase` | `AnalyzeChatUseCase` |
| Repository接口 | `XxxRepository` | `AiRepository` |
| Repository实现 | `XxxRepositoryImpl` | `AiRepositoryImpl` |
| ViewModel | `XxxViewModel` | `ChatViewModel` |
| Entity | `XxxEntity` | `ContactProfileEntity` |
| DTO | `XxxDto` | `ChatRequestDto` |
| 测试用例 | `XxxTest` | `SendAdvisorMessageUseCaseTest` |

## 多AI协作规则

详细规则参考: [Rules/workspace-rules.md](./Rules/workspace-rules.md)
