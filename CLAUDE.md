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
- Hilt 2.52 + Room v12
- 最低SDK: 24, 目标SDK: 35

## 常用命令

```bash
# 构建与测试
./gradlew assembleDebug          # Debug构建
./gradlew assembleRelease        # Release构建
./gradlew test                   # 所有单元测试
./gradlew testDebugUnitTest      # Debug单元测试
./gradlew connectedAndroidTest   # 连接设备运行Android测试

# 单个模块测试
./gradlew :presentation:test     # presentation模块测试
./gradlew :domain:test           # domain模块测试
./gradlew :data:test             # data模块测试

# 运行单个测试类
./gradlew :domain:test --tests "com.empathy.ai.domain.usecase.SendAdvisorMessageUseCaseTest"

# Lint检查
./gradlew lint                   # 全局Lint
./gradlew :presentation:lint     # presentation模块Lint
./gradlew ktlintCheck            # Kotlin代码风格检查

# 调试脚本
scripts\logcat.bat -e            # ERROR日志
scripts\quick-error.bat          # 快速查看最近错误
scripts\ai-debug.bat             # AI请求调试

# ADB调试
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.empathy.ai/.ui.MainActivity
adb shell dumpsys activity activities | findstr ActivityRecord  # 查看Activity栈
```

## 测试覆盖

| 模块 | 单元测试 | Android测试 | 关键测试 |
|------|----------|-------------|----------|
| domain | 43 | - | UseCase、Model、PromptBuilder |
| presentation | 50 | 7 | ViewModel、Compose UI |
| data | 25 | 6 | Repository、Database |
| app | 141 | 26 | Application初始化、服务测试 |

### 模块文件统计（2026-01-09最新扫描）

| 模块 | 主源码 | 单元测试 | Android测试 | 总计 |
|------|--------|---------|------------|------|
| **:domain** | 183 | 43 | 0 | 226 |
| **:data** | 84 | 25 | 6 | 115 |
| **:presentation** | 280 | 50 | 7 | 337 |
| **:app** | 27 | 141 | 26 | 194 |
| **总计** | **574** | **259** | **39** | **872** |

**最后更新**: 2026-01-09

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
- **当前版本**: v12
- **迁移路径**: `data/src/main/kotlin/com/empathy/ai/data/local/MIGRATION_*.kt`
- **迁移策略**: 每次架构变更添加增量迁移脚本

### Hilt 模块索引
| 模块 | 文件 | 职责 |
|------|------|------|
| app | `AppModule.kt` | Application 级注入 |
| app | `ServiceModule.kt` | Android Service 注入 |
| data | `DatabaseModule.kt` | Room 数据库配置 |
| data | `NetworkModule.kt` | Retrofit/OkHttp 配置 |
| data | `RepositoryModule.kt` | Repository 实现绑定 |
| presentation | `ViewModelModule.kt` | ViewModel Factory |

### 核心功能流程

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
- **支持服务商**: OpenAI、Azure OpenAI、阿里云、百度、智谱、腾讯混元、讯飞星火
- **模型配置**: 每个服务商支持自定义模型名称和 API 版本

### 开发者模式
- **功能**: 系统提示词管理、调试工具
- **入口**: 设置页面 → 开发者选项
- **用途**: 调试 AI 响应、查看日志、管理提示词

### 最近完成的功能与修复（2026-01-09）
- **BUG-00053/54/56**: AI配置与用户画像多项问题修复
  - 修复AI配置保存逻辑，确保配置变更正确持久化
  - 修复用户画像数据加载问题，优化数据初始化流程
  - 完善错误处理机制，提升系统稳定性
- **BUG-00055**: 全局字体响应式适配问题修复
  - 修复字体大小自适应问题，确保不同设备上字体显示正确
  - 优化布局响应式适配，提升多设备兼容性
- **BUG-00052**: 界面布局与字体自适应问题修复
  - 修复界面布局在不同屏幕尺寸上的显示问题
  - 优化字体自适应逻辑，确保阅读体验一致
- **PRD-00033**: 开发者模式与系统提示词管理功能
  - 新增开发者模式入口，支持调试工具和系统提示词管理
  - 系统提示词管理功能，支持查看和调试AI提示词配置
  - 提供调试日志查看、性能监控等开发者工具

### 组件复用系统

项目采用 **原子-分子-有机体-模板** 四级组件架构：

| 层级 | 示例 | 说明 |
|------|------|------|
| 原子组件 | `IosButton`, `IosTextField`, `AvatarView` | 基础UI单元，无业务逻辑 |
| 分子组件 | `IosSearchBar`, `ModernPersonaTab` | 组合原子组件，有简单交互 |
| 有机体组件 | `FactStreamCard`, `EmotionTimelineView` | 复杂业务逻辑，独立功能单元 |
| 模板组件 | `AiAdvisorChatScreen` | 页面级组合，特定场景使用 |

**组件位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/`

## 模块文档

详细架构信息参考各模块文档：
- [domain/CLAUDE.md](./domain/CLAUDE.md) - 领域层详细文档
- [data/CLAUDE.md](./data/CLAUDE.md) - 数据层详细文档
- [presentation/CLAUDE.md](./presentation/CLAUDE.md) - 表现层详细文档
- [app/src/main/java/com/empathy/ai/app/CLAUDE.md](./app/src/main/java/com/empathy/ai/app/CLAUDE.md) - 应用层详细文档

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

项目使用 `Rules/workspace-rules.md` 和 `Rules/WORKSPACE.md` 管理多AI协作状态：

**任务开始前必须**：
1. 尝试读取 `Rules/WORKSPACE.md`（如不存在则跳过）
2. 检查是否有其他AI正在执行相关任务
3. 检查资源锁定状态和待处理冲突
4. 在 WORKSPACE 中记录任务开始信息

**任务执行中必须**：
- 重要里程碑实时更新到 WORKSPACE
- 发现的问题立即记录

**任务完成后必须**：
- 更新 WORKSPACE 任务状态
- 更新AI工具状态
- 添加变更日志

详细规则参考: [Rules/workspace-rules.md](./Rules/workspace-rules.md)
