# 技术栈

## 🔴 必读文档（开始工作前必须阅读）

**在开始任何工作之前，请务必先阅读以下文档：**

1. **[Rules/RulesReadMe.md](../../Rules/RulesReadMe.md)** - 项目通用规则和文档规范
2. **[WORKSPACE.md](../../WORKSPACE.md)** - 当前工作状态和任务协调

---

## 语言规范

**所有文档和回答必须使用中文。** 代码注释、变量名、类名等保持英文，但所有说明文档、开发指南和与开发者的沟通必须使用中文。

## 多模块构建系统（基于当前代码）

> 当前分支: main
> 当前版本: v1.14.17 (versionCode: 11417, dev)
> 数据库版本: Room v17

### 模块配置

| 模块 | 类型 | 插件 | 主要依赖 |
|------|------|------|----------|
| `:domain` | Kotlin Library | `java-library`, `kotlin.jvm` | Coroutines, javax.inject |
| `:data` | Android Library | `android.library`, `kotlin.android`, `hilt`, `ksp` | Room, Retrofit, Moshi, :domain |
| `:presentation` | Android Library | `android.library`, `kotlin.android`, `kotlin.compose`, `hilt`, `ksp` | Compose, Navigation, :domain |
| `:app` | Application | `android.application`, `kotlin.android`, `kotlin.compose`, `hilt`, `ksp` | 全模块聚合 |

### 构建工具版本

- **Gradle**: 8.13（见 `gradle/wrapper/gradle-wrapper.properties`）
- **AGP**: 8.7.3
- **Kotlin**: 2.0.21
- **JDK**: 17
- **KSP**: 2.0.21-1.0.28

## 核心技术栈（基于版本目录）

### UI
- **Jetpack Compose**: BOM 2024.12.01
- **Material 3**: 1.3.1
- **Navigation Compose**: 2.8.5
- **Coil**: 2.5.0
- **Markdown 渲染**: compose-richtext 1.0.0-alpha01

### 架构与 DI
- **架构**: Clean Architecture + MVVM
- **DI**: Hilt 2.52

### 数据与网络
- **Room**: 2.6.1
- **Retrofit**: 2.11.0
- **OkHttp**: 4.12.0（含 SSE）
- **Moshi**: 1.15.1
- **Paging**: 3.3.5
- **安全存储**: EncryptedSharedPreferences (security-crypto 1.1.0-alpha06)

### 异步
- **Coroutines**: 1.9.0 + Flow

### 其他
- **UCrop**: 2.2.8（头像裁剪）
- **Desugaring**: desugar_jdk_libs 2.0.4
- **FFmpeg Kit**: 6.0.LTS（已在版本目录中声明，但当前未启用）

## 常用命令

### 构建与运行
```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

### 测试
```bash
./gradlew test
./gradlew testDebugUnitTest
./gradlew connectedAndroidTest
```

### 清理
```bash
./gradlew clean
```

### Gradle 同步
```bash
./gradlew --refresh-dependencies
```

## 版本目录

依赖项使用 Gradle 版本目录（`gradle/libs.versions.toml`）管理。优先使用目录引用：

```kotlin
// ✅ 正确
implementation(libs.androidx.core.ktx)

// ❌ 错误
implementation("androidx.core:core-ktx:1.15.0")
```

---

**文档版本**: 4.0
**最后更新**: 2026-01-21
**更新内容**:
- 同步 Gradle/Kotlin/Room/Compose 版本
- 更新模块插件与依赖说明
