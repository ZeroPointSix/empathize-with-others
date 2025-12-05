# 技术栈

## 语言规范

**所有文档和回答必须使用中文。** 代码注释、变量名、类名等保持英文，但所有说明文档、开发指南和与开发者的沟通必须使用中文。

## 构建系统

- **Build Tool**: Gradle 8.13 with Kotlin DSL
- **AGP**: 8.7.3
- **Kotlin**: 2.0.21
- **JDK**: 17
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35

## 核心技术

### UI 层
- **Jetpack Compose**：2024.12.01（BOM 管理）
- **Material 3**：使用 Material Design 3 的声明式 UI
- **Navigation Compose**：2.8.5

### 架构
- **模式**：清洁架构 + MVVM
- **DI**：Hilt 2.52（基于 Dagger 的依赖注入）
- **生命周期**：AndroidX Lifecycle 2.8.7 与 Compose 集成

### 数据层
- **本地数据库**：Room 2.6.1 与 KTX 扩展
- **网络**：Retrofit 2.11.0 + OkHttp 4.12.0
- **JSON**：Moshi 1.15.1 与 Kotlin 代码生成
- **安全**：EncryptedSharedPreferences（androidx.security.crypto 1.1.0-alpha06）

### 异步处理
- **协程**：Kotlin Coroutines 1.9.0
- **Flow**：用于响应式数据流

### 测试
- **单元测试**：JUnit 4.13.2
- **模拟**：MockK 1.13.13
- **协程测试**：kotlinx-coroutines-test 1.9.0
- **UI 测试**：Compose UI Test + Espresso

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
