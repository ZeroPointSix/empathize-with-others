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

### ✅ 异步处理

- **协程**：Kotlin Coroutines 1.9.0
- **Flow**：用于响应式数据流

### ✅ 媒体处理（已配置但未实现）

- **FFmpeg Kit**：6.0.LTS（音视频处理）

## ✅ 测试

- **单元测试**：JUnit 4.13.2
- **Android 测试**：AndroidX JUnit 1.2.1
- **模拟**：MockK 1.13.13
- **协程测试**：kotlinx-coroutines-test 1.9.0
- **UI 测试**：Compose UI Test + Espresso 3.6.1

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

## 当前实现状态

### ✅ 完全实现的技术栈

- **构建系统**: Gradle 8.13 + Kotlin DSL 完整配置
- **UI框架**: Jetpack Compose + Material Design 3 完整实现
- **架构模式**: Clean Architecture + MVVM + Hilt 完整实现
- **数据持久化**: Room 数据库 + Flow 响应式编程完整实现
- **网络通信**: Retrofit + OkHttp + Moshi 完整实现
- **异步编程**: Kotlin Coroutines + Flow 完整实现
- **安全存储**: EncryptedSharedPreferences 完整实现
- **依赖注入**: Hilt 模块完整配置

### ⚠️ 部分实现/待完善功能

- **媒体处理**: FeedTextUseCase已实现，但AiRepositoryImpl中transcribeMedia方法未实现
  - 代码架构已设计：ExtractedData模型、AiRepository接口定义
  - ❌ 实际实现：AiRepositoryImpl.transcribeMedia直接返回未实现异常
  - 需要集成：FFmpeg音视频处理、ASR语音识别、OCR文字识别

- **AI响应解析**: AiResponseParser接口已定义，但实现可能不完整
  - 代码架构存在：AiResponseParser接口、FallbackHandler等
  - ⚠️ 集成状态不明：需要验证解析器在实际AI调用中的使用情况

- **无障碍服务**: WeChatDetector等工具类存在，但实际集成状态不明
  - 代码架构存在：WeChatDetector、FloatingWindowManager等
  - ❌ 实际集成未验证：需要确认与悬浮窗服务的协作

## 技术债务

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