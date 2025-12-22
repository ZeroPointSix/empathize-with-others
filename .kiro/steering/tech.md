# 技术栈

## 🔴 必读文档（开始工作前必须阅读）

**在开始任何工作之前，请务必先阅读以下文档：**

1. **[Rules/RulesReadMe.md](../../Rules/RulesReadMe.md)** - 项目通用规则和文档规范
2. **[WORKSPACE.md](../../WORKSPACE.md)** - 当前工作状态和任务协调

---

## 语言规范

**所有文档和回答必须使用中文。** 代码注释、变量名、类名等保持英文，但所有说明文档、开发指南和与开发者的沟通必须使用中文。

## 构建系统

- **Build Tool**: Gradle 8.13 with Kotlin DSL
- **AGP**: 8.7.3
- **Kotlin**: 2.0.21
- **JDK**: 17
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35
- **KSP**: 2.0.21-1.0.28

## 核心技术

### ✅ UI 层

- **Jetpack Compose**：2024.12.01（BOM 管理）
- **Material 3**：1.3.1（使用 Material Design 3 的声明式 UI）
- **Navigation Compose**：2.8.5
- **Activity Compose**：1.9.3
- **Material Icons Extended**：完整图标库
- **Coil**：2.5.0（图片加载和缓存）

### ✅ 架构

- **模式**：清洁架构 + MVVM
- **DI**：Hilt 2.52（基于 Dagger 的依赖注入）
- **Hilt Navigation Compose**：1.2.0
- **生命周期**：AndroidX Lifecycle 2.8.7 与 Compose 集成

### ✅ 数据层

- **本地数据库**：Room 2.6.1 与 KTX 扩展
- **网络**：Retrofit 2.11.0 + OkHttp 4.12.0 + OkHttp Logging Interceptor
- **JSON**：Moshi 1.15.1 与 Kotlin 代码生成
- **安全**：EncryptedSharedPreferences（androidx.security.crypto 1.1.0-alpha06）
- **分页**：Paging 3.3.5（分页加载支持）

### ✅ 异步处理

- **协程**：Kotlin Coroutines 1.9.0
- **Flow**：用于响应式数据流

### ✅ 媒体处理（已配置但未实现）

- **FFmpeg Kit**：6.0.LTS（音视频处理）

### ✅ 测试

- **单元测试**：JUnit 4.13.2
- **Android 测试**：AndroidX JUnit 1.2.1
- **模拟**：MockK 1.13.13
- **协程测试**：kotlinx-coroutines-test 1.9.0
- **UI 测试**：Compose UI Test + Espresso 3.6.1
- **Room 测试**：androidx-room-testing 2.6.1

## 常用命令

### 构建和运行

```bash
# 构建调试 APK
./gradlew assembleDebug

# 构建发布 APK
./gradlew assembleRelease

# 安装调试 APK 到设备
./gradlew installDebug

# 在连接的设备上运行应用
./gradlew run
```

### 测试

```bash
# 运行所有单元测试
./gradlew test

# 运行调试变体的单元测试
./gradlew testDebugUnitTest

# 运行仪器测试
./gradlew connectedAndroidTest

# 生成测试覆盖率报告
./gradlew jacocoTestReport
```

### 代码质量

```bash
# 清理构建工件
./gradlew clean

# 检查依赖更新
./gradlew dependencyUpdates

# Lint 检查
./gradlew lint
```

### Gradle 同步

```bash
# 同步 Gradle 依赖
./gradlew --refresh-dependencies
```

## 版本目录

依赖项使用 Gradle 版本目录（`gradle/libs.versions.toml`）管理。始终在构建文件中使用目录引用：

```kotlin
// ✅ 正确
implementation(libs.androidx.core.ktx)

// ❌ 错误
implementation("androidx.core:core-ktx:1.15.0")
```

## 关键库

- **Compose BOM**：所有 Compose 库的统一版本管理
- **Hilt**：ViewModel 和 Repository 的构造函数注入
- **Room**：支持 Flow 的类型安全数据库
- **Retrofit**：使用 Moshi 转换器的 REST API 客户端
- **Security Crypto**：敏感数据的硬件支持加密
- **Paging**：分页加载支持
- **Coil**：图片加载和缓存

## 当前实现状态

### ✅ 完全实现的技术栈

- **构建系统**: Gradle 8.13 + Kotlin DSL + KSP 完整配置
  - Gradle版本目录管理，统一依赖版本
  - KSP配置用于Room和Hilt编译时处理
  - Desugaring配置支持Java 8+ API (minSdk=24)
- **UI框架**: Jetpack Compose + Material Design 3 完整实现
  - Compose BOM 2024.12.01统一版本管理
  - Navigation Compose 2.8.5完整导航系统
  - Material Icons Extended完整图标库
  - 提示词编辑器UI：完整的Compose界面实现
  - 悬浮窗功能重构UI：Tab系统和状态管理完整实现
  - 悬浮球状态指示与拖动UI：流畅交互体验完整实现
  - MaxHeightScrollView：自适应高度滚动视图组件
  - PromptSettingsSection：提示词设置界面组件（TD-00015新增）
- **架构模式**: Clean Architecture + MVVM + Hilt 完整实现
  - 严格的层级分离和依赖规则
  - Hilt 2.52依赖注入完整配置
  - PromptEditorViewModel：完整的状态管理
  - ContactDetailTabViewModel：四标签页状态管理
  - SettingsViewModel：已更新，添加promptScenesOrdered属性（TD-00015）
  - 新增DispatcherModule：统一协程调度器管理
- **数据持久化**: Room 数据库 + Flow 响应式编程完整实现
  - Room 2.6.1 + KTX扩展
  - 数据库版本v10，完整Migration链（1→8）
  - Flow响应式数据流
  - 提示词模板和备份表完整实现
  - 记忆系统表：conversation_logs, daily_summaries, failed_summary_tasks
  - Paging 3.3.5分页加载支持
  - PromptFileStorage迁移逻辑优化（TD-00015已完成）
- **网络通信**: Retrofit + OkHttp + Moshi 完整实现
  - Retrofit 2.11.0动态URL支持
  - OkHttp 4.12.0 + Logging拦截器
  - Moshi 1.15.1 Kotlin代码生成
  - 支持多种AI服务商：OpenAI、DeepSeek等
- **异步编程**: Kotlin Coroutines + Flow 完整实现
  - Coroutines 1.9.0
  - 完整的suspend函数和Flow支持
  - DispatcherModule统一协程调度器管理
- **安全存储**: EncryptedSharedPreferences 完整实现
  - androidx.security.crypto 1.1.0-alpha06
  - 硬件级加密支持
  - ApiKeyStorage：API密钥安全存储
- **依赖注入**: Hilt 模块完整配置
  - DatabaseModule、NetworkModule、RepositoryModule
  - 新增MemoryModule支持记忆系统
  - 新增PromptModule支持提示词系统
  - 新增DispatcherModule支持协程调度器
  - 新增FloatingWindowModule支持悬浮窗系统
  - 新增NotificationModule支持通知系统
  - 新增SummaryModule支持总结系统
- **图片加载**: Coil 图片加载和缓存完整实现
  - Coil 2.5.0 + Compose集成
- **测试框架**: Room Testing、单元测试、UI测试完整实现
  - JUnit 4.13.2 + AndroidX JUnit 1.2.1
  - MockK 1.13.13模拟框架
  - Compose UI Test + Espresso 3.6.1
  - Room Testing 2.6.1数据库迁移测试
  - 提示词系统完整测试套件
  - 悬浮窗功能重构完整测试套件
  - 悬浮球状态指示与拖动相关测试
  - MaxHeightScrollView相关测试
  - 提示词设置优化相关测试（TD-00015已完成）
- **通知系统**: Android通知管理完整实现
  - AiResultNotificationManager：AI完成后系统通知
  - 支持多种通知类型和优先级
- **代码统计**: 603个Kotlin文件，94,907行代码
  - 主代码：487个文件，68,675行
  - 单元测试：114个文件，26,232行
  - Android测试：20个文件，6,474行

### ⚠️ 部分实现/待完善功能

- **媒体处理**: FeedTextUseCase已实现，但AiRepositoryImpl中transcribeMedia方法未实现
  - 代码架构已设计：ExtractedData模型、AiRepository接口定义
  - ❌ 实际实现：AiRepositoryImpl.transcribeMedia直接返回未实现异常
  - 需要集成：FFmpeg音视频处理、ASR语音识别、OCR文字识别
  - FFmpeg Kit 6.0.LTS已配置但未启用（app/build.gradle.kts中已注释）

- **AI响应解析**: AiResponseParser接口已定义，但实现可能不完整
  - 代码架构存在：AiResponseParser接口、FallbackHandler等
  - ⚠️ 集成状态不明：需要验证解析器在实际AI调用中的使用情况

- **无障碍服务**: WeChatDetector等工具类存在，但实际集成状态不明
  - 代码架构存在：WeChatDetector、FloatingWindowManager等
  - ❌ 实际集成未验证：需要确认与悬浮窗服务的协作

## 技术债务

- **输入内容身份识别与双向对话历史**: 需要实现TD-00008任务
  - 任务状态：技术设计完成
  - 需要实现：IdentityPrefixHelper、UseCase层集成、系统提示词增强、UI渲染优化
  - 相关文档：TDD-00008-输入内容身份识别与双向对话历史技术设计.md

- **手动触发AI总结功能**: 需要实现TD-00011任务
  - 任务状态：技术设计完成
  - 需要实现：ManualSummaryUseCase、SummaryTask、SummaryProgressDialog等
  - 相关文档：TDD-00011-手动触发AI总结功能技术设计.md

- **媒体处理模块**: transcribeMedia方法需要实现FFmpeg集成
  - 代码架构已设计：ExtractedData模型、AiRepository接口定义
  - ❌ 实际实现：AiRepositoryImpl.transcribeMedia直接返回未实现异常
  - 需要集成：FFmpeg音视频处理、ASR语音识别、OCR文字识别

- **AI响应解析器**: 需要验证AiResponseParser的完整性和错误处理
  - 代码架构存在：AiResponseParser接口、FallbackHandler等
  - ⚠️ 集成状态不明：需要验证解析器在实际AI调用中的使用情况

- **无障碍集成**: 需要验证WeChatDetector与FloatingWindowService的实际协作
  - 代码架构存在：WeChatDetector、FloatingWindowManager等
  - ❌ 实际集成未验证：需要确认与悬浮窗服务的协作

- **规则引擎集成**: 需要验证RuleEngine与CheckDraftUseCase的集成情况
  - 代码架构完整：RuleEngine、多种匹配策略
  - ⚠️ 集成状态不明：可能未在实际业务流程中被调用

### ✅ 已解决的技术债务

- **Room数据库迁移问题**: 已完成完整的Migration脚本和测试
  - ✅ 移除fallbackToDestructiveMigration()，确保数据安全
  - ✅ 启用Schema导出（exportSchema = true）
  - ✅ 配置schema导出目录（$projectDir/schemas）
  - ✅ 添加Room Testing依赖
  - ✅ 完善DatabaseMigrationTest（8个测试用例）

- **悬浮球状态指示与启动模式问题**: 已完成BUG-00014修复
  - ✅ 添加显示模式持久化（FloatingWindowPreferences）
  - ✅ 修复启动时直接显示对话框问题
  - ✅ 在AI调用流程中集成状态回调
  - ✅ 实现悬浮球加载状态和完成通知

- **三种模式上下文不共通问题**: 已完成BUG-00015修复
  - ✅ 新增SessionContextService统一管理历史上下文
  - ✅ 修改PolishDraftUseCase和GenerateReplyUseCase添加历史上下文支持
  - ✅ 更新FloatingWindowModule添加依赖注入
  - ✅ 新增相关测试用例验证修复效果

- **悬浮窗结果页内容过长导致按钮不可见问题**: 已完成BUG-00021修复
  - ✅ 采用动态高度计算策略，将结果区域最大高度限制为屏幕高度的40%
  - ✅ 确保底部操作按钮（复制、重新生成）始终在屏幕可见范围内
  - ✅ 在ResultCard中暴露setMaxHeight接口，支持动态调整
  - ✅ 新增MaxHeightScrollView组件，支持内容超出时的滚动

- **AI响应JSON解析失败问题**: 已完成BUG-00025修复
  - ✅ 增强EnhancedJsonCleaner的清理能力
  - ✅ 改进AiResponseCleaner的错误处理机制
  - ✅ 优化FallbackHandler的错误恢复策略
  - ✅ 提升AI响应解析的稳定性和容错性

- **提示词设置优化**: 已完成TD-00015任务
  - ✅ 简化提示词场景从6个到4个核心场景（ANALYZE、POLISH、REPLY、SUMMARY）
  - ✅ 废弃CHECK和EXTRACT场景（保留代码兼容性，隐藏UI）
  - ✅ 实现CHECK到POLISH的数据迁移逻辑
  - ✅ GlobalPromptConfig版本升级到v3
  - ✅ 新增PromptSettingsSection组件，集成到设置界面
  - ✅ 完整测试覆盖：7个测试文件，61+个测试用例
  - ✅ 状态：22/25任务完成（88%，核心功能100%）