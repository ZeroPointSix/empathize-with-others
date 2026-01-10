# 技术栈

## 🔴 必读文档（开始工作前必须阅读）

**在开始任何工作之前，请务必先阅读以下文档：**

1. **[Rules/RulesReadMe.md](../../Rules/RulesReadMe.md)** - 项目通用规则和文档规范
2. **[WORKSPACE.md](../../WORKSPACE.md)** - 当前工作状态和任务协调

---

## 语言规范

**所有文档和回答必须使用中文。** 代码注释、变量名、类名等保持英文，但所有说明文档、开发指南和与开发者的沟通必须使用中文。

## 多模块构建系统 (TD-00017)

> 项目已完成Clean Architecture多模块改造

### 模块配置

| 模块 | 类型 | 插件 | 主要依赖 |
|------|------|------|----------|
| `:domain` | Kotlin Library | `java-library`, `kotlin.jvm` | kotlinx.coroutines |
| `:data` | Android Library | `android.library`, `kotlin.android`, `ksp`, `hilt` | Room, Retrofit, Moshi, :domain |
| `:presentation` | Android Library | `android.library`, `kotlin.android`, `kotlin.compose`, `hilt`, `kapt` | Compose, Navigation, :domain |
| `:app` | Application | `android.application`, `kotlin.android`, `kotlin.compose`, `hilt`, `ksp`, `kapt` | 所有模块 |

### 构建工具版本

- **Build Tool**: Gradle 8.13 with Kotlin DSL
- **AGP**: 8.7.3
- **Kotlin**: 2.0.21 (K2编译器)
- **JDK**: 17
- **KSP**: 2.0.21-1.0.28 (Room, Moshi)
- **KAPT**: Hilt注解处理

### 模块构建命令

```bash
# 构建单个模块
./gradlew :domain:build          # 纯Kotlin模块
./gradlew :data:assembleDebug    # 数据层
./gradlew :presentation:assembleDebug  # 表现层
./gradlew :app:assembleDebug     # 完整应用

# 构建所有模块
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
```

## 构建系统

- **Build Tool**: Gradle 8.13 with Kotlin DSL
- **AGP**: 8.7.3
- **Kotlin**: 2.0.21
- **JDK**: 17
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35
- **KSP**: 2.0.21-1.0.28

## 核心技术

### UI 层

- **Jetpack Compose**：2024.12.01（BOM 管理）
- **Material 3**：1.3.1（使用 Material Design 3 的声明式 UI）
- **Navigation Compose**：2.8.5
- **Activity Compose**：1.9.3
- **Material Icons Extended**：完整图标库
- **Coil**：2.5.0（图片加载和缓存）

### 架构

- **模式**：清洁架构 + MVVM
- **DI**：Hilt 2.52（基于 Dagger 的依赖注入）
- **Hilt Navigation Compose**：1.2.0
- **生命周期**：AndroidX Lifecycle 2.8.7 与 Compose 集成

### 数据层

- **本地数据库**：Room 2.6.1 与 KTX 扩展
- **网络**：Retrofit 2.11.0 + OkHttp 4.12.0 + OkHttp Logging Interceptor
- **JSON**：Moshi 1.15.1 与 Kotlin 代码生成
- **安全**：EncryptedSharedPreferences（androidx.security.crypto 1.1.0-alpha06）
- **分页**：Paging 3.3.5（分页加载支持）

### 异步处理

- **协程**：Kotlin Coroutines 1.9.0
- **Flow**：用于响应式数据流

### 媒体处理（已配置但未实现）

- **FFmpeg Kit**：6.0.LTS（音视频处理）

### 测试

- **单元测试**：JUnit 4.13.2
- **Android 测试**：AndroidX JUnit 1.2.1
- **模拟**：MockK 1.13.13
- **协程测试**：kotlinx-coroutines-test 1.9.0
- **UI 测试**：Compose UI Test + Espresso 3.6.1
- **Room 测试**：androidx.room-testing 2.6.1

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

### AI调试脚本

```bash
# AI请求日志过滤（显示Temperature、MaxTokens等关键参数）
scripts\ai-debug.bat              # 实时监听AI日志
scripts\ai-debug.bat -h           # 获取最近100条AI日志
scripts\ai-debug.bat -h -n 200    # 获取最近200条AI日志
scripts\ai-debug.bat -d 127.0.0.1:7555  # 指定MuMu模拟器
scripts\ai-debug.bat -f ai_log.txt     # 输出到文件

# 完整AI日志（包含提示词内容）
scripts\ai-debug-full.bat         # 获取完整AI请求日志
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

### 完全实现的技术栈

- **构建系统**: Gradle 8.13 + Kotlin DSL + KSP + KAPT 完整配置
  - Gradle版本目录管理，统一依赖版本
  - KSP配置用于Room和Moshi编译时处理
  - KAPT配置用于Hilt编译时处理（解决多模块兼容性问题）
  - Desugaring配置支持Java 8+ API (minSdk=24)
- **UI框架**: Jetpack Compose + Material Design 3 完整实现
  - Compose BOM 2024.12.01统一版本管理
  - Navigation Compose 2.8.5完整导航系统
  - Material Icons Extended完整图标库
  - 完整的UI组件系统（原子-分子-有机体-模板四级架构）
- **架构模式**: Clean Architecture + MVVM + Hilt 完整实现
  - 严格的层级分离和依赖规则
  - Hilt 2.52依赖注入完整配置
  - ViewModel完整状态管理
- **数据持久化**: Room 数据库 + Flow 响应式编程完整实现
  - Room 2.6.1 + KTX扩展
  - 数据库版本v16，完整Migration链（1→16）
  - Flow响应式数据流
  - Paging 3.3.5分页加载支持
- **网络通信**: Retrofit + OkHttp + Moshi 完整实现
  - Retrofit 2.11.0动态URL支持
  - OkHttp 4.12.0 + Logging拦截器
  - OkHttpClientFactory：动态代理切换机制
  - Moshi 1.15.1 Kotlin代码生成
  - 支持多种AI服务商：OpenAI、Azure OpenAI、阿里云、百度、智谱、腾讯混元、讯飞星火
  - 网络代理支持：HTTP/HTTPS/SOCKS4/SOCKS5
- **异步编程**: Kotlin Coroutines + Flow 完整实现
  - Coroutines 1.9.0
  - 完整的suspend函数和Flow支持
  - DispatcherModule统一协程调度器管理
- **安全存储**: EncryptedSharedPreferences 完整实现
  - androidx.security.crypto 1.1.0-alpha06
  - 硬件级加密支持
  - ApiKeyStorage：API密钥安全存储
- **依赖注入**: Hilt 模块完整配置
  - 18个DI模块完整配置
- **图片加载**: Coil 图片加载和缓存完整实现
  - Coil 2.5.0 + Compose集成
- **测试框架**: 完整测试套件实现
  - 268个单元测试 + 39个Android测试
  - MockK 1.13.13模拟框架
  - Compose UI Test + Espresso 3.6.1
- **通知系统**: Android通知管理完整实现
  - AiResultNotificationManager：AI完成后系统通知
- **代码统计**: 574个Kotlin主源码文件 + 307个测试文件

### 部分实现/待完善功能

- **媒体处理**: FeedTextUseCase已实现，但AiRepositoryImpl中transcribeMedia方法未实现
  - 需要集成：FFmpeg音视频处理、ASR语音识别、OCR文字识别
  - FFmpeg Kit 6.0.LTS已配置但未启用

## 技术债务

### 待实现功能

- **输入内容身份识别与双向对话历史**: 需要完整实现
  - IdentityPrefixHelper工具类已实现
  - 需要实现：UseCase层集成、系统提示词增强、UI渲染优化

- **手动触发AI总结功能**: 已实现核心功能
  - ManualSummaryUseCase、SummaryTask等已实现
  - UI组件已实现

- **媒体处理模块**: transcribeMedia方法需要实现FFmpeg集成
  - 代码架构已设计
  - 需要集成：FFmpeg音视频处理、ASR语音识别、OCR文字识别

### 已解决的技术债务

- **Clean Architecture多模块改造**: 已完成TD-00017任务
  - 创建:domain模块（纯Kotlin，无Android依赖）
  - 创建:data模块（Android Library，Room、Retrofit、Repository实现）
  - 创建:presentation模块（Android Library，Compose UI、ViewModel）
  - 重构:app模块（应用入口、Android服务、DI聚合）
  - 完成65/65任务，100%完成率

- **Room数据库迁移问题**: 已完成完整的Migration脚本和测试
  - 移除fallbackToDestructiveMigration()，确保数据安全
  - 启用Schema导出（exportSchema = true）

- **悬浮球状态指示与启动模式问题**: 已完成BUG-00014修复
  - 添加显示模式持久化（FloatingWindowPreferences）
  - 修复启动时直接显示对话框问题

- **三种模式上下文不共通问题**: 已完成BUG-00015修复
  - 新增SessionContextService统一管理历史上下文

- **悬浮窗结果页内容过长导致按钮不可见问题**: 已完成BUG-00021修复
  - 采用动态高度计算策略
  - 新增MaxHeightScrollView组件

- **AI响应JSON解析失败问题**: 已完成BUG-00025修复
  - 增强EnhancedJsonCleaner的清理能力
  - 改进AiResponseCleaner的错误处理机制

- **提示词设置优化**: 已完成TD-00015任务
  - 简化提示词场景从6个到4个核心场景
  - GlobalPromptConfig版本升级到v3

- **AI配置功能完善**: 已完成TD-00025任务
  - 高级选项：Temperature滑块和Token限制输入
  - 模型拖拽排序：DraggableModelList组件
  - 网络代理配置：ProxySettingsDialog
  - 用量统计系统：UsageStatsScreen

- **AI军师对话界面可读性问题**: 已完成BUG-00057修复
  - 优化对话界面布局和样式
  - 提升长文本显示效果
  - 完善Markdown渲染支持

- **AI手动总结功能未生效问题**: 已完成BUG-00064修复
  - 修复手动总结功能未正确触发问题
  - 测试用例已验证

### 进行中的问题修复

| Bug ID | 问题描述 | 状态 |
|--------|----------|------|
| BUG-00058 | 新建会话功能失效问题 | 已修复，测试用例已验证 |
| BUG-00059 | 中断生成后重新生成消息角色错乱问题 | 已修复，测试用例已验证 |
| BUG-00060 | 会话管理增强需求 | 已修复，测试用例已验证 |
| BUG-00061 | 会话历史跳转失败问题 | 已修复，测试用例已验证 |
| BUG-00064 | AI手动总结功能未生效问题 | 已修复，测试用例已验证 |
| BUG-00065 | 联系人搜索功能优化 | 进行中 |

**文档版本**: 2.19
**最后更新**: 2026-01-10
**更新内容**:
- 更新模块测试文件统计（基于实际代码架构扫描）
- 更新BUG-00064状态为已修复
- 更新进行中的BUG修复列表
