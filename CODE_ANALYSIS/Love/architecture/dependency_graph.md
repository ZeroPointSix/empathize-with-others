# 模块依赖关系图

> **分析日期**: 2025-12-29
> **分析范围**: 项目所有模块的依赖关系

---

## 一、模块依赖总览

### 1.1 依赖层次图

```
┌─────────────────────────────────────────────────────────────┐
│                         app (应用层)                          │
│  - MainActivity.kt                                           │
│  - EmpathyApplication.kt                                     │
│  - DI配置 (11个模块)                                          │
│  - Android服务                                               │
│                                                              │
│  依赖：                                                        │
│  - implementation(project(":domain"))                       │
│  - implementation(project(":data"))                         │
│  - api(project(":presentation"))                            │
└─────────────────────────────────────────────────────────────┘
                          ↓ 依赖
              ┌───────────────────────┐
              │                       │
              ↓                       ↓
┌─────────────────────────┐  ┌─────────────────────────────────┐
│      data (数据层)       │  │  presentation (表现层)          │
│  - Repository实现       │  │  - ViewModel (19个)             │
│  - Room数据库           │  │  - Compose UI (187个)           │
│  - Retrofit网络         │  │  - Navigation                   │
│                          │  │  - Theme                        │
│  依赖：                    │  │                                │
│  - api(project(":domain"))│  │  依赖：                         │
│                          │  │  - api(project(":domain"))     │
└─────────────────────────┘  └─────────────────────────────────┘
              ↓                                ↓
              └──────────────┬───────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│                   domain (领域层) ✨                        │
│  - 业务实体 (66个model)                                       │
│  - 仓库接口 (13个repository)                                  │
│  - 业务用例 (38个usecase)                                     │
│  - 领域服务 (2个service)                                      │
│  - 工具类 (29个util)                                          │
│                                                              │
│  依赖：                                                        │
│  - 仅依赖纯Kotlin库（Coroutines、javax.inject）               │
│  - 无任何Android依赖                                          │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 依赖方向矩阵

|  | domain | data | presentation | app |
|--|--------|------|--------------|-----|
| **domain** | - | ✅ | ✅ | ✅ |
| **data** | ↓ | - | ❌ | ❌ |
| **presentation** | ↓ | ❌ | - | ❌ |
| **app** | ↓ | ↓ | ↓ | - |

**说明**：
- ✅ = 允许的依赖
- ↓ = 依赖方向
- ❌ = 不允许的依赖（违规）

---

## 二、模块详细依赖配置

### 2.1 Domain模块

#### 文件路径
`domain/build.gradle.kts`

#### 依赖配置
```kotlin
plugins {
    `java-library`  // 关键：使用java-library插件
    `kotlin-jvm`    // 关键：使用JVM插件，非Android
}

dependencies {
    // 仅Kotlin标准库和协程
    implementation(libs.kotlinx.coroutines.core)

    // JSR-330注解（用于@Inject）
    implementation("javax.inject:javax.inject:1")

    // 测试依赖
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
```

#### 依赖分析

✅ **无Android依赖**
- 不依赖任何Android库
- 不依赖data、presentation、app模块
- 仅依赖纯Kotlin库

✅ **纯Kotlin JVM**
- 使用`java-library`插件
- 使用`kotlin-jvm`插件
- 可在JVM环境独立运行

### 2.2 Data模块

#### 文件路径
`data/build.gradle.kts`

#### 依赖配置
```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

dependencies {
    // 关键：使用api暴露domain类型
    api(project(":domain"))

    // Room数据库
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit网络
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.moshi)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // 其他依赖...
}
```

#### 依赖分析

✅ **正确依赖domain**
- 使用`api(project(":domain"))`
- 暴露domain类型给依赖data的模块

✅ **Android依赖**
- Room、Retrofit、OkHttp等
- 所有Android依赖使用`implementation`
- 不暴露给上层

### 2.3 Presentation模块

#### 文件路径
`presentation/build.gradle.kts`

#### 依赖配置
```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    // 关键：使用api暴露domain类型
    api(project(":domain"))

    // Compose UI
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // ViewModel
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.compose)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // 其他依赖...
}
```

#### 依赖分析

✅ **正确依赖domain**
- 使用`api(project(":domain"))`
- 暴露domain类型给依赖presentation的模块

✅ **UI依赖**
- Compose、Navigation、Material3
- 所有UI依赖使用`implementation`

### 2.4 App模块

#### 文件路径
`app/build.gradle.kts`

#### 依赖配置
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
    id("com.google.dagger.hilt.android")
}

dependencies {
    // Domain层
    implementation(project(":domain"))

    // Data层
    implementation(project(":data"))

    // Presentation层 - 使用api暴露
    api(project(":presentation"))

    // AndroidX核心库
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // 其他依赖...
}
```

#### 依赖分析

✅ **聚合所有模块**
- 依赖domain、data、presentation
- 使用`api(project(":presentation"))`暴露presentation

✅ **应用入口**
- MainActivity、EmpathyApplication
- DI模块配置（11个模块）

---

## 三、依赖违规检测

### 3.1 检测方法

1. **依赖方向检查**：分析build.gradle.kts文件
2. **Domain层纯净度检查**：搜索`import android`
3. **循环依赖检查**：分析模块依赖图

### 3.2 检测结果

#### 依赖方向检查 ✅

| 检查项 | 结果 |
|--------|------|
| app → domain | ✅ PASS |
| app → data | ✅ PASS |
| app → presentation | ✅ PASS |
| data → domain | ✅ PASS |
| presentation → domain | ✅ PASS |
| domain → * | ✅ PASS (无依赖) |
| data → presentation | ❌ PASS (无依赖) |
| presentation → data | ❌ PASS (无依赖) |

#### Domain层纯净度检查 ✅

```bash
$ grep -r "import android" domain/src/main/kotlin/
# 结果：0个匹配
```

**结论**：**Domain层100%纯净，无Android依赖**

#### 循环依赖检查 ✅

```
app → data/presentation → domain ✅ 单向依赖
app 无反向依赖 ✅
data 无反向依赖 ✅
presentation 无反向依赖 ✅
domain 无依赖 ✅
```

**结论**：**无循环依赖**

---

## 四、依赖传递分析

### 4.1 api vs implementation

#### api

**定义**：传递依赖给依赖此模块的模块

**使用场景**：暴露domain类型

**示例**：
```kotlin
// data/build.gradle.kts
api(project(":domain"))

// 效果：
// 如果模块C依赖data，模块C也能访问domain的类型
```

**依赖图**：
```
app → data → domain
     ↓
   可访问domain
```

#### implementation

**定义**：不传递依赖，仅当前模块可见

**使用场景**：内部实现细节

**示例**：
```kotlin
// data/build.gradle.kts
implementation(libs.room.runtime)

// 效果：
// 如果模块C依赖data，模块C不能直接访问Room
```

**依赖图**：
```
app → data → Room (不可见)
```

### 4.2 项目中的使用

#### 使用api暴露domain

**data模块**：
```kotlin
api(project(":domain"))
```

**presentation模块**：
```kotlin
api(project(":domain"))
```

**原因**：
- 依赖data/presentation的模块（如app）需要访问domain类型
- 解决Hilt多模块类型解析问题

#### 使用implementation隐藏实现

**data模块**：
```kotlin
implementation(libs.room.runtime)
implementation(libs.retrofit)
```

**presentation模块**：
```kotlin
implementation(libs.compose.ui)
implementation(libs.lifecycle.viewmodel.ktx)
```

**原因**：
- Room、Retrofit等是data模块内部实现
- Compose、ViewModel等是presentation模块内部实现
- 上层模块不需要直接访问这些库

---

## 五、依赖注入架构

### 5.1 DI模块分布

#### Data层DI模块（3个）

```
data/di/
├── DatabaseModule.kt       # Room数据库和DAO
├── NetworkModule.kt        # Retrofit/OkHttp配置
└── RepositoryModule.kt     # Repository接口绑定
```

#### App层DI模块（11个）

```
app/di/
├── AppDispatcherModule.kt       # 协程调度器
├── ServiceModule.kt             # 服务类配置
├── FloatingWindowModule.kt      # 悬浮窗依赖
├── SummaryModule.kt             # 每日总结依赖
├── NotificationModule.kt        # 通知系统
├── PersonaModule.kt             # 用户画像
├── TopicModule.kt               # 对话主题
├── LoggerModule.kt              # 日志服务
├── UserProfileModule.kt         # 用户画像配置
├── EditModule.kt                # 编辑功能
└── FloatingWindowManagerModule.kt # 悬浮窗管理器
```

### 5.2 依赖注入流程

```
1. Application启动
   ↓
2. @HiltAndroidApp初始化Hilt
   ↓
3. Hilt扫描所有@Module
   ├─ DatabaseModule（提供AppDatabase）
   ├─ NetworkModule（提供Retrofit）
   ├─ RepositoryModule（绑定Repository）
   ├─ AppDispatcherModule（提供协程调度器）
   └─ ...（其他11个模块）
   ↓
4. Hilt创建依赖图
   ↓
5. 请求依赖时
   ├─ @Inject constructor(...)自动注入
   └─ @Binds接口绑定
   ↓
6. 返回实例（@Singleton保证单例）
```

### 5.3 关键依赖示例

#### ViewModel依赖注入

```
ChatViewModel
  ↓ 依赖
AnalyzeChatUseCase
  ├─ ContactRepository
  ├─ AiRepository
  ├─ PrivacyRepository
  └─ ...（7个Repository）

ContactRepository
  ↓ 实现
ContactRepositoryImpl
  ↓ 依赖
ContactDao（来自DatabaseModule）
```

#### Repository依赖注入

```
ContactRepository（接口）
  ↓ 绑定（@Binds）
ContactRepositoryImpl（实现）
  ↓ 依赖
ContactDao
  ↓ 提供
DatabaseModule
```

---

## 六、模块间通信

### 6.1 Domain → Data

**方式**：接口调用

**示例**：
```kotlin
// Domain层UseCase调用Repository接口
class AnalyzeChatUseCase @Inject constructor(
    private val aiRepository: AiRepository  // 接口
) {
    suspend operator fun invoke(...): Result<AnalysisResult> {
        // 调用接口方法
        return aiRepository.analyzeChat(...)
    }
}

// Data层提供实现
class AiRepositoryImpl @Inject constructor(
    private val api: OpenAiApi
) : AiRepository {
    override suspend fun analyzeChat(...): Result<AnalysisResult> {
        // 具体实现
    }
}
```

### 6.2 Domain → Presentation

**方式**：UseCase返回Flow，ViewModel收集

**示例**：
```kotlin
// Domain层UseCase返回Flow
class GetAllContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    operator fun invoke(): Flow<List<ContactProfile>> {
        return contactRepository.getAllProfiles()
    }
}

// Presentation层ViewModel收集Flow
@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getAllContactsUseCase: GetAllContactsUseCase
) : ViewModel() {

    init {
        viewModelScope.launch {
            getAllContactsUseCase().collect { contacts ->
                _uiState.update { it.copy(contacts = contacts) }
            }
        }
    }
}
```

### 6.3 Presentation → UI

**方式**：StateFlow + Compose

**示例**：
```kotlin
// ViewModel提供StateFlow
class ChatViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
}

// UI收集StateFlow
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // UI自动响应数据变化
    if (uiState.isAnalyzing) {
        CircularProgressIndicator()
    }
}
```

---

## 七、依赖优化建议

### 7.1 当前状态

✅ **依赖方向完全正确**
- 严格单向依赖
- 无循环依赖
- Domain层纯净

✅ **使用api/expose合理**
- data/presentation使用api暴露domain
- 实现细节使用implementation隐藏

### 7.2 优化建议

#### 无需优化

当前依赖架构已经非常优秀，无需优化。

---

## 八、总结

### 8.1 依赖架构评分

| 评估维度 | 得分 | 满分 |
|---------|------|------|
| **依赖方向正确性** | 100 | 100 |
| **Domain层纯净度** | 100 | 100 |
| **无循环依赖** | 100 | 100 |
| **api/expose使用** | 100 | 100 |
| **依赖注入配置** | 100 | 100 |
| **模块解耦程度** | 95 | 100 |

**总体评分：99/100**

### 8.2 核心优势

1. ✅ **严格单向依赖**：app → data/presentation → domain
2. ✅ **Domain层100%纯净**：0个Android依赖
3. ✅ **无循环依赖**：依赖图清晰
4. ✅ **api/expose使用得当**：正确暴露domain类型
5. ✅ **完善的依赖注入**：14个Hilt模块

### 8.3 结论

这是一个**教科书级别的Clean Architecture依赖架构**，可以作为Android项目的参考范例。

---

**分析完成时间**：2025-12-29
