---
name: kotlin-java-standards
description: Kotlin/Java 编码规范 - 命名规范、代码风格、最佳实践、Kotlin 惯用法。在编写 Android 代码时使用。
---

# Kotlin/Java 编码规范

## 激活时机

当满足以下条件时自动激活此技能：
- 编写 Kotlin/Java 代码
- 代码审查
- 制定团队编码规范
- 重构代码

## Kotlin 命名规范

### 包名

```
✅ 全小写，不使用下划线
com.example.myapplication
com.example.feature.home

❌ 避免
Com.Example.MyApplication
com.example.my_application
```

### 类名

```
✅ 大驼峰（PascalCase）
class UserProfile
class HomeActivity
class LoginViewModel
class UserAdapter

❌ 避免
class userProfile
class user_profile
```

### 函数名

```
✅ 小驼峰（camelCase），动词开头
fun getUserProfile()
fun validateInput()
fun calculateTotal()

❌ 避免
fun GetUserProfile()
fun get_user_profile()
```

### 变量名

```
✅ 小驼峰（camelCase），名词开头
val userName: String
var isLoggedIn: Boolean
private val MAX_RETRY_COUNT = 3

❌ 避免
val UserName: String
var is_logged_in: Boolean
```

### 常量

```
✅ 全大写，下划线分隔
const val MAX_COUNT = 100
const val DEFAULT_TIMEOUT = 5000L

私有常量也可以用小驼峰
private const val defaultTimeout = 5000L
```

## Kotlin 代码风格

### 类定义

```kotlin
// ✅ 推荐顺序
class UserProfile(
    private val repository: UserRepository,
    private val analytics: Analytics
) {
    // 1. 伴生对象
    companion object {
        const val TAG = "UserProfile"
    }

    // 2. 属性
    private var _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    // 3. 构造函数
    init {
        loadData()
    }

    // 4. 公共方法
    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            // ...
        }
    }

    // 5. 私有方法
    private fun loadData() { ... }

    // 6. 内部类
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { ... }
}
```

### 函数定义

```kotlin
// ✅ 单表达式函数
fun getDisplayName(): String = "$firstName $lastName"

// ✅ 默认参数
fun createUser(
    name: String,
    email: String,
    age: Int = 18,
    isActive: Boolean = true
): User { ... }

// ✅ 命名参数
val user = createUser(
    name = "Alice",
    email = "alice@example.com",
    age = 25
)

// ✅ 可空参数
fun sendEmail(email: String?, subject: String) {
    email ?: return
    // ...
}
```

### 属性定义

```kotlin
// ✅ 只读属性（使用 val）
val userName: String = ""
val userCount: Int
    get() = users.size

// ✅ 可变属性（使用 var）
var isRefreshing: Boolean = false

// ✅ 懒初始化
private val adapter: UserAdapter by lazy {
    UserAdapter(onUserClick = { showUserDetail(it) })
}

// ✅ 后期初始化
private lateinit var binding: ActivityMainBinding
```

## Kotlin 惯用法

### 数据类

```kotlin
// ✅ 使用数据类存储数据
data class User(
    val id: String,
    val name: String,
    val email: String
)

// ✅ 复制并修改
val updatedUser = user.copy(email = "new@example.com")

// ✅ 解构声明
val (id, name, email) = user
```

### 密封类

```kotlin
// ✅ 使用密封类表示受限的类层次结构
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val data: List<User>) : UiState()
    data class Error(val message: String) : UiState()
}

// 使用
when (state) {
    is UiState.Idle -> showIdleView()
    is UiState.Loading -> showLoadingView()
    is UiState.Success -> showData(state.data)
    is UiState.Error -> showError(state.message)
}
```

### 扩展函数

```kotlin
// ✅ 扩展函数
fun String.isValidEmail(): Boolean {
    return this.contains("@") && this.contains(".")
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

// 使用
if (email.isValidEmail()) { ... }
progressBar.visible()
```

### 高阶函数

```kotlin
// ✅ 函数作为参数
fun processUsers(
    users: List<User>,
    predicate: (User) -> Boolean
): List<User> {
    return users.filter(predicate)
}

// 使用
val activeUsers = processUsers(users) { user ->
    user.isActive
}

// ✅ Lambda 表达式
users.filter { it.isActive }
      .map { it.name }
      .forEach { println(it) }
```

### 协程作用域

```kotlin
// ✅ viewModelScope
viewModelScope.launch {
    val result = repository.getData()
    _state.value = UiState.Success(result)
}

// ✅ lifecycleScope
lifecycleScope.launch {
    // 与生命周期绑定的协程
}

// ✅ withContext 切换调度器
suspend fun getUser(): User = withContext(Dispatchers.IO) {
    repository.getUser()
}
```

## 代码组织

### 文件结构

```kotlin
// 1. 文件头注释（可选）
/*
 * Copyright 2024 Example
 */

// 2. 包声明
package com.example.app

// 3. 导入
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// 4. 类/接口/对象定义
class MainActivity : AppCompatActivity() {
    // ...
}
```

### 导入顺序

```kotlin
// 1. Kotlin 标准库
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 2. 第三方库（字母顺序）
import com.google.gson.Gson
import retrofit2.Retrofit

// 3. AndroidX（字母顺序）
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel

// 4. 项目内部
import com.example.app.model.User
import com.example.app.util.Logger

// ✅ 使用通配符导入要谨慎
// ✅ 尽量避免使用 *
```

## 最佳实践

### 空安全

```kotlin
// ✅ 明确可空性
fun getUser(id: String): User? { ... }
fun getUserName(): String { ... } // 非 null

// ✅ 安全调用
user?.email?.lowercase()

// ✅ Elvis 操作符
val name = user?.name ?: "Unknown"

// ✅ 早返回
fun processUser(user: User?) {
    val user = user ?: return
    // 使用非 null 的 user
}

// ❌ 避免强制非 null
val email = user!!.email // 危险
```

### 集合操作

```kotlin
// ✅ 不可变集合（默认）
val users = listOf<User>()
val names = setOf<String>()
val map = mapOf<String, User>()

// ✅ 可变集合（需要时）
val mutableUsers = mutableListOf<User>()

// ✅ 集合操作
users.filter { it.isActive }
     .map { it.name }
     .sorted()
     .take(10)

// ✅ 序列（处理大数据集）
users.asSequence()
     .filter { it.isActive }
     .map { it.name }
     .toList()
```

### 字符串模板

```kotlin
// ✅ 使用字符串模板
val message = "Hello, $name!"
val result = "Count: ${users.size}"

// ✅ 多行字符串
val json = """
    {
        "name": "$name",
        "age": $age
    }
""".trimIndent()
```

### 资源清理

```kotlin
// ✅ use 自动关闭资源
File("data.txt").bufferedReader().use { reader ->
    val text = reader.readText()
    // 自动关闭 reader
}

// ✅ try-finally
var cursor: Cursor? = null
try {
    cursor = query()
    // ...
} finally {
    cursor?.close()
}
```

## 避免的反模式

### ❌ 避免过度使用 !!

```kotlin
// ❌ 危险
val email = user!!.email!!

// ✅ 安全
val email = user?.email ?: return
```

### ❌ 避免深层嵌套

```kotlin
// ❌ 深层嵌套
if (user != null) {
    if (user.isActive) {
        if (user.hasPermission) {
            // ...
        }
    }
}

// ✅ 早返回
if (user == null) return
if (!user.isActive) return
if (!user.hasPermission) return
// ...
```

### ❌ 避免全局状态

```kotlin
// ❌ 全局可变状态
var globalState: String = ""

// ✅ 使用依赖注入
class MyApp(
    private val stateRepository: StateRepository
)
```

## 相关资源

- `resources/kotlin-idioms.md` - Kotlin 惯用法
- `resources/coding-style.md` - 代码风格指南
- `resources/android-patterns.md` - Android 模式

---

**技能状态**: 完成 ✅
**适用语言**: Kotlin, Java
**官方指南**: [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
