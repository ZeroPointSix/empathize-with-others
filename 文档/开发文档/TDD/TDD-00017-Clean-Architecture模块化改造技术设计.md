# TDD-00017-Clean Architecture模块化改造技术设计

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | TDD-00017 |
| 创建日期 | 2025-12-23 |
| 作者 | Kiro |
| 状态 | 草稿 |
| 版本 | v1.0 |

### 关联文档

| 文档类型 | 文档编号 | 文档名称 |
|----------|----------|----------|
| 调研报告 | RESEARCH-00029 | Clean Architecture架构合规性调研报告 |
| 规范文档 | - | .kiro/steering/structure.md |
| 规范文档 | - | .kiro/steering/quick-start.md |

---

## 1. 技术概述

本文档描述将单模块项目改造为多模块项目的技术方案，通过Gradle模块边界从根本上强制Clean Architecture的依赖方向。

### 1.1 改造目标

| 目标 | 说明 |
|------|------|
| 依赖方向强制 | 编译时检查，违规依赖无法通过编译 |
| Domain层纯净 | :domain模块无Android依赖 |
| 构建速度优化 | 增量编译，只重新编译变更模块 |
| 代码隔离 | 各层代码物理隔离，职责清晰 |

### 1.2 目标架构

```
┌─────────────────────────────────────────────────────────────┐
│                        :app 模块                             │
│  (Application入口, DI配置, 组装所有模块)                      │
│  dependencies:                                               │
│    implementation(project(":domain"))                        │
│    implementation(project(":data"))                          │
│    implementation(project(":presentation"))                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    :presentation 模块                        │
│  (UI, ViewModel, Screen, Component, Navigation)              │
│  dependencies:                                               │
│    implementation(project(":domain"))                        │
│    ❌ 不能依赖 :data                                          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       :data 模块                             │
│  (Repository实现, DAO, Entity, API, Preferences)             │
│  dependencies:                                               │
│    implementation(project(":domain"))                        │
│    ❌ 不能依赖 :presentation                                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      :domain 模块                            │
│  (Model, Repository接口, UseCase, 纯Kotlin)                  │
│  dependencies:                                               │
│    ❌ 不依赖任何其他模块                                       │
│    ❌ 不依赖Android SDK                                       │
└─────────────────────────────────────────────────────────────┘
```

## 2. 分阶段实施计划

### 2.1 阶段概览

| 阶段 | 内容 | 预估时间 | 风险 |
|------|------|----------|------|
| Phase 1 | 创建:domain模块（纯Kotlin） | 2-3天 | 低 |
| Phase 2 | 创建:data模块 | 2-3天 | 中 |
| Phase 3 | 创建:presentation模块 | 3-4天 | 中 |
| Phase 4 | 重构:app模块 | 1-2天 | 低 |
| Phase 5 | 清理和优化 | 1天 | 低 |

### 2.2 Phase 1: 创建:domain模块

#### 2.2.1 模块配置

```kotlin
// domain/build.gradle.kts
plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // 只允许纯Kotlin依赖
    implementation(libs.kotlinx.coroutines.core)
    
    // 测试依赖
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

#### 2.2.2 迁移文件清单

**可直接迁移的文件（无Android依赖）**：

| 原路径 | 新路径 | 说明 |
|--------|--------|------|
| `domain/model/*.kt` | `domain/src/main/kotlin/.../model/` | 大部分模型 |
| `domain/repository/*.kt` | `domain/src/main/kotlin/.../repository/` | 仓库接口 |
| `domain/usecase/*.kt` | `domain/src/main/kotlin/.../usecase/` | 大部分用例 |

**需要重构的文件（有Android依赖）**：

| 文件 | 问题 | 解决方案 |
|------|------|----------|
| `FilterType.kt` | 引用Compose图标 | 移除icon属性 |
| `FailedTaskRepository.kt` | 返回Data层Entity | 创建领域模型 |
| 多个UseCase | 依赖Data层实现类 | 通过接口注入 |

**需要移至其他模块的文件**：

| 文件 | 目标模块 | 原因 |
|------|----------|------|
| `FloatingWindowService.kt` | :presentation | Android Service |
| `FloatingView.kt` | :presentation | Android View |
| `FloatingWindowManager.kt` | :presentation | Android Context |
| `DataEncryption.kt` | :data | Android KeyStore |
| `PermissionManager.kt` | :presentation | Android权限 |
| `WeChatDetector.kt` | :data | Android PackageManager |

#### 2.2.3 接口抽象

```kotlin
// domain/src/main/kotlin/.../util/Logger.kt
interface Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String)
    fun i(tag: String, message: String)
    fun v(tag: String, message: String)
}

// domain/src/main/kotlin/.../model/FailedSummaryTask.kt
data class FailedSummaryTask(
    val id: Long,
    val contactId: String,
    val summaryDate: String,
    val failureReason: String,
    val retryCount: Int,
    val failedAt: Long,
    val lastRetryAt: Long?
)
```

### 2.3 Phase 2: 创建:data模块

#### 2.3.1 模块配置

```kotlin
// data/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.empathy.ai.data"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.generateKotlin", "true")
    }
}

dependencies {
    // 依赖Domain模块
    implementation(project(":domain"))
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    
    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)
    
    // Security
    implementation(libs.androidx.security.crypto)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
```

#### 2.3.2 迁移文件清单

| 原路径 | 新路径 |
|--------|--------|
| `data/local/*.kt` | `data/src/main/kotlin/.../local/` |
| `data/remote/*.kt` | `data/src/main/kotlin/.../remote/` |
| `data/repository/*.kt` | `data/src/main/kotlin/.../repository/` |
| `data/parser/*.kt` | `data/src/main/kotlin/.../parser/` |

### 2.4 Phase 3: 创建:presentation模块

#### 2.4.1 模块配置

```kotlin
// presentation/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.empathy.ai.presentation"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 24
    }
    
    buildFeatures {
        compose = true
    }
}

dependencies {
    // 只依赖Domain模块
    implementation(project(":domain"))
    // ❌ 不能依赖 :data
    
    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
}
```

#### 2.4.2 迁移文件清单

| 原路径 | 新路径 |
|--------|--------|
| `presentation/ui/*.kt` | `presentation/src/main/kotlin/.../ui/` |
| `presentation/viewmodel/*.kt` | `presentation/src/main/kotlin/.../viewmodel/` |
| `presentation/navigation/*.kt` | `presentation/src/main/kotlin/.../navigation/` |
| `presentation/theme/*.kt` | `presentation/src/main/kotlin/.../theme/` |

**从Domain迁移的文件**：

| 文件 | 说明 |
|------|------|
| `FloatingWindowService.kt` | Android Service |
| `FloatingView.kt` | Android View |
| `FloatingWindowManager.kt` | 权限管理 |
| `PermissionManager.kt` | 权限管理 |

### 2.5 Phase 4: 重构:app模块

#### 2.5.1 模块配置

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.empathy.ai"
    // ... 其他配置保持不变
}

dependencies {
    // 依赖所有模块
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":presentation"))
    
    // Hilt (应用入口)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

#### 2.5.2 保留文件

| 文件 | 说明 |
|------|------|
| `EmpathyApplication.kt` | 应用入口 |
| `di/*.kt` | DI模块配置 |

## 3. 关键重构点

### 3.1 Logger接口抽象

**Domain层接口**：
```kotlin
// domain/src/main/kotlin/.../util/Logger.kt
interface Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String)
    fun i(tag: String, message: String)
    fun v(tag: String, message: String)
}
```

**Data层实现**：
```kotlin
// data/src/main/kotlin/.../util/AndroidLogger.kt
class AndroidLogger @Inject constructor() : Logger {
    override fun d(tag: String, message: String) = Log.d(tag, message)
    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.e(tag, message, throwable)
        else Log.e(tag, message)
    }
    override fun w(tag: String, message: String) = Log.w(tag, message)
    override fun i(tag: String, message: String) = Log.i(tag, message)
    override fun v(tag: String, message: String) = Log.v(tag, message)
}
```

**DI绑定**：
```kotlin
// app/src/main/kotlin/.../di/LoggerModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggerModule {
    @Binds
    @Singleton
    abstract fun bindLogger(impl: AndroidLogger): Logger
}
```

### 3.2 FailedSummaryTask领域模型

**Domain层模型**：
```kotlin
// domain/src/main/kotlin/.../model/FailedSummaryTask.kt
data class FailedSummaryTask(
    val id: Long,
    val contactId: String,
    val summaryDate: String,
    val failureReason: String,
    val retryCount: Int,
    val failedAt: Long,
    val lastRetryAt: Long?
)
```

**Data层转换**：
```kotlin
// data/src/main/kotlin/.../local/entity/FailedSummaryTaskEntity.kt
fun FailedSummaryTaskEntity.toDomain(): FailedSummaryTask = FailedSummaryTask(
    id = id,
    contactId = contactId,
    summaryDate = summaryDate,
    failureReason = failureReason,
    retryCount = retryCount,
    failedAt = failedAt,
    lastRetryAt = lastRetryAt
)

fun FailedSummaryTask.toEntity(): FailedSummaryTaskEntity = FailedSummaryTaskEntity(
    id = id,
    contactId = contactId,
    summaryDate = summaryDate,
    failureReason = failureReason,
    retryCount = retryCount,
    failedAt = failedAt,
    lastRetryAt = lastRetryAt
)
```

### 3.3 FilterType纯净化

**Domain层（移除icon）**：
```kotlin
// domain/src/main/kotlin/.../model/FilterType.kt
enum class FilterType(val displayName: String) {
    ALL("全部"),
    AI_SUMMARY("只看AI"),
    CONFLICT("只看冲突"),
    DATE("只看约会"),
    SWEET("只看甜蜜");
    
    fun apply(items: List<TimelineItem>): List<TimelineItem> {
        return when (this) {
            ALL -> items
            AI_SUMMARY -> items.filterIsInstance<TimelineItem.AiSummary>()
            CONFLICT -> items.filter { it.emotionType == EmotionType.CONFLICT }
            DATE -> items.filter { it.emotionType == EmotionType.DATE }
            SWEET -> items.filter { it.emotionType == EmotionType.SWEET }
        }
    }
}
```

**Presentation层（图标映射）**：
```kotlin
// presentation/src/main/kotlin/.../util/FilterTypeIcons.kt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun FilterType.getIcon(): ImageVector = when (this) {
    FilterType.ALL -> Icons.Default.FilterList
    FilterType.AI_SUMMARY -> Icons.Default.Psychology
    FilterType.CONFLICT -> Icons.Default.Warning
    FilterType.DATE -> Icons.Default.Restaurant
    FilterType.SWEET -> Icons.Default.Favorite
}
```

## 4. settings.gradle.kts 配置

```kotlin
// settings.gradle.kts
pluginManagement {
    // ... 保持不变
}

dependencyResolutionManagement {
    // ... 保持不变
}

rootProject.name = "Give Love"

// 模块配置
include(":app")
include(":domain")
include(":data")
include(":presentation")
```

## 5. 风险评估与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 编译错误大量出现 | 高 | 分阶段迁移，每阶段验证 |
| Hilt注入失败 | 高 | 仔细配置各模块的Hilt |
| 循环依赖 | 中 | 严格遵循依赖方向 |
| 测试失败 | 中 | 迁移后运行完整测试 |

### 5.1 回滚方案

| 阶段 | 回滚策略 |
|------|----------|
| Phase 1 | 删除:domain模块，恢复app/src/main/java/domain目录 |
| Phase 2 | 删除:data模块，恢复app/src/main/java/data目录 |
| Phase 3 | 删除:presentation模块，恢复app/src/main/java/presentation目录 |
| Phase 4 | 从Git恢复app/build.gradle.kts和settings.gradle.kts |

## 6. 测试迁移策略

### 6.1 测试文件分布

当前项目共有114个单元测试文件，需要按模块迁移：

| 目标模块 | 测试文件数 | 迁移说明 |
|----------|------------|----------|
| :domain | ~60 | domain/usecase、domain/util、domain/service相关测试 |
| :data | ~30 | data/repository、data/parser、data/local相关测试 |
| :presentation | ~20 | viewmodel、integration相关测试 |
| :app | ~4 | 应用级集成测试 |

### 6.2 测试迁移原则

1. **测试跟随源码**：测试文件与被测源码保持在同一模块
2. **依赖隔离**：各模块测试只依赖本模块和:domain模块
3. **Mock策略**：跨模块依赖使用MockK进行模拟

## 7. DI模块迁移策略

### 7.1 DI模块归属

| DI模块 | 目标位置 | 说明 |
|--------|----------|------|
| DatabaseModule | :data | 提供Room数据库实例 |
| NetworkModule | :data | 提供Retrofit和OkHttp实例 |
| RepositoryModule | :data | 绑定Repository接口和实现 |
| MemoryModule | :data | 提供记忆系统相关依赖 |
| PromptModule | :data | 提供提示词系统相关依赖 |
| SummaryModule | :data | 提供总结系统相关依赖 |
| ServiceModule | :presentation | 提供领域服务实例 |
| FloatingWindowModule | :presentation | 提供悬浮窗相关依赖 |
| NotificationModule | :presentation | 提供通知相关依赖 |
| DispatcherModule | :app | 提供协程调度器（全局共享） |
| LoggerModule | :app | 绑定Logger接口和实现（新增） |
| AppModule | :app | 应用级依赖绑定 |

### 7.2 Hilt多模块配置

每个Android Library模块需要添加：

```kotlin
// 在模块的build.gradle.kts中
plugins {
    alias(libs.plugins.hilt)
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

## 8. 验证清单

### 8.1 Phase 1 验证
- [ ] :domain模块编译通过
- [ ] :domain模块无Android依赖
- [ ] 单元测试通过

### 8.2 Phase 2 验证
- [ ] :data模块编译通过
- [ ] :data只依赖:domain
- [ ] Room迁移正常

### 8.3 Phase 3 验证
- [ ] :presentation模块编译通过
- [ ] :presentation只依赖:domain
- [ ] UI正常显示

### 8.4 Phase 4 验证
- [ ] :app模块编译通过
- [ ] 应用正常启动
- [ ] 所有功能正常

### 8.5 最终验证
- [ ] 完整测试套件通过
- [ ] APK正常构建
- [ ] 真机测试通过

---

**文档版本**: 1.0  
**最后更新**: 2025-12-23
