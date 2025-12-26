---
name: android-coroutines
description: Android 协程和异步 - Kotlin Coroutines、Flow、Dispatcher、异常处理、协程最佳实践。在处理异步操作时使用。
---

# Android 协程和异步

## 激活时机

当满足以下条件时自动激活此技能：
- 实现异步操作
- 处理并发任务
- 使用 Flow 处理数据流
- 协程作用域管理
- 协程性能优化

## 协程基础

### CoroutineScope

```kotlin
// viewModelScope - ViewModel 作用域
viewModelScope.launch {
    // ViewModel 销毁时自动取消
    val result = repository.getData()
    _uiState.value = UiState.Success(result)
}

// lifecycleScope - 生命周期作用域
lifecycleScope.launch {
    // 生命周期销毁时自动取消
    delay(5000)
    showNotification()
}

// 自定义 CoroutineScope
class UserRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun loadData() {
        scope.launch {
            // 异步操作
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}
```

### Dispatchers 调度器

```kotlin
// Dispatchers.Main - 主线程
viewModelScope.launch(Dispatchers.Main) {
    // UI 操作
    textView.text = "Updated"
}

// Dispatchers.IO - IO 操作
viewModelScope.launch(Dispatchers.IO) {
    // 数据库、文件操作
    val data = database.userDao().getAll()
}

// Dispatchers.Default - CPU 密集
viewModelScope.launch(Dispatchers.Default) {
    // 计算、数据处理
    val result = heavyComputation()
}

// Dispatchers.Unconfined - 无限制
viewModelScope.launch(Dispatchers.Unconfined) {
    // 不推荐使用，除非明确需要
}
```

## Flow 操作

### Flow 创建

```kotlin
// 使用 flow 构建器
fun getUsers(): Flow<List<User>> = flow {
    emit(repository.getUsersFromLocal())
    val remote = repository.getUsersFromRemote()
    emit(remote)
}

// 使用 channelFlow
fun observeEvents(): Flow<Event> = channelFlow {
    eventListener.collect { event ->
        send(event)
    }
}

// 使用 callbackFlow
fun observeCallback(): Flow<Data> = callbackFlow {
    val callback = object : Callback {
        override fun onData(data: Data) {
            trySend(data)
        }
    }
    api.registerCallback(callback)

    awaitClose { api.unregisterCallback(callback) }
}
```

### Flow 操作符

```kotlin
// map - 转换数据
usersFlow
    .map { users -> users.map { it.toDomainModel() } }

// filter - 过滤数据
usersFlow
    .filter { users -> users.isNotEmpty() }

// debounce - 防抖
searchFlow
    .debounce(300)
    .collect { query -> search(query) }

// combine - 组合多个 Flow
combine(userFlow, settingsFlow) { user, settings ->
    UserProfile(user, settings)
}

// flatMapLatest - 切换流
searchQueryFlow
    .flatMapLatest { query ->
        searchRepository.search(query)
    }

// catch - 捕获异常
usersFlow
    .catch { error ->
        emit(emptyList())
    }
```

### StateFlow 和 SharedFlow

```kotlin
// StateFlow - 状态管理
class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.getData()
            _uiState.value = UiState.Success(result)
        }
    }
}

// SharedFlow - 事件管理
class HomeViewModel : ViewModel() {
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun showSnackBar(message: String) {
        viewModelScope.launch {
            _events.emit(Event.ShowSnackBar(message))
        }
    }
}

// 在 Compose 中收集
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.ShowSnackBar -> showSnackBar(event.message)
            }
        }
    }
}
```

## 异常处理

### try-catch

```kotlin
viewModelScope.launch {
    try {
        val data = repository.getData()
        _uiState.value = UiState.Success(data)
    } catch (e: NetworkException) {
        _uiState.value = UiState.Error("Network error")
    } catch (e: Exception) {
        _uiState.value = UiState.Error("Unknown error")
    }
}
```

### Flow 异常处理

```kotlin
// catch 操作符
repository.getUsers()
    .catch { error ->
        emit(Resource.Error(error.message ?: "Unknown error"))
    }
    .collect { resource ->
        when (resource) {
            is Resource.Success -> showUsers(resource.data)
            is Resource.Error -> showError(resource.message)
        }
    }

// retry - 重试
repository.getUsers()
    .retry(3) { error ->
        error is NetworkException
    }
    .catch { error ->
        emit(Resource.Error(error.message))
    }
```

### SupervisorJob

```kotlin
// 使用 SupervisorJob，子协程失败不影响其他
val scope = CoroutineScope(SupervisorJob())

scope.launch {
    throw Exception("Failed")
}

scope.launch {
    delay(1000)
    println("Still running")
}

// viewModelScope 使用 SupervisorJob
```

## 并发模式

### async/await

```kotlin
// 并行执行
suspend fun loadUserData(): UserData {
    return viewModelScope.async(Dispatchers.IO) {
        repository.getUser()
    }.await()
}

// 并行执行多个
suspend fun loadAllData(): AllData {
    val usersDeferred = async { repository.getUsers() }
    val postsDeferred = async { repository.getPosts() }

    return AllData(
        users = usersDeferred.await(),
        posts = postsDeferred.await()
    )
}
```

### 结构化并发

```kotlin
// 使用 coroutineScope 等待所有子协程
suspend fun refreshData() {
    coroutineScope {
        launch(Dispatchers.IO) { refreshUsers() }
        launch(Dispatchers.IO) { refreshPosts() }
        launch(Dispatchers.IO) { refreshComments() }
    }
}

// 使用 withContext 切换调度器
suspend fun getUser(): User = withContext(Dispatchers.IO) {
    repository.getUser()
}
```

## 协程上下文

### CoroutineContext

```kotlin
// 组合上下文
val context = SupervisorJob() + Dispatchers.Main + CoroutineName("MyCoroutine")

viewModelScope.launch(context) {
    // 在 Main 线程执行，名称为 "MyCoroutine"
}

val handler = CoroutineExceptionHandler { _, exception ->
    println("Caught $exception")
}

viewModelScope.launch(handler) {
    // 异常会被 handler 捕获
}
```

### ThreadLocal

```kotlin
// ThreadLocal 与 asContextElement
val userIdContext = ThreadLocal<String>()

viewModelScope.launch {
    // 设置 ThreadLocal
    userIdContext.set("user123")

    withContext(Dispatchers.IO + userIdContext.asContextElement()) {
        // 在 IO 线程仍能访问
        val userId = userIdContext.get()
    }
}
```

## 背压策略

### Flow 背压

```kotlin
// buffer - 缓冲
usersFlow
    .buffer(capacity = 10)
    .collect { user -> processUser(user) }

// conflate - 只处理最新
eventsFlow
    .conflate()
    .collect { event -> handleEvent(event) }

// collectLatest - 取消旧的
searchFlow
    .collectLatest { query ->
        search(query)
    }
```

## 协程调试

### 协程调试

```kotlin
// 启用协程调试（仅开发环境）
System.setProperty("kotlinx.coroutines.debug", "on")

// 使用 newCoroutineContext 查看上下文
viewModelScope.launch {
    println("Context: $coroutineContext")
}
```

## 最佳实践

### ✅ 应该做的

```
1. 使用 viewModelScope 和 lifecycleScope
2. 在 IO 调度器执行数据库/网络操作
3. 在 Main 调度器更新 UI
4. 正确处理异常
5. 使用结构化并发
6. 避免 GlobalScope（除非必要）
```

### ❌ 不应该做的

```
1. 使用 Dispatchers.Main 执行耗时操作
2. 忘记取消协程
3. 捕获所有异常不处理
4. 在挂起函数中使用阻塞调用
5. 过度使用 async/await
```

## 相关资源

- `resources/coroutines-guide.md` - 协程完整指南
- `resources/flow-patterns.md` - Flow 模式
- `resources/coroutines-testing.md` - 协程测试

---

**技能状态**: 完成 ✅
**协程版本**: Kotlin Coroutines 1.7+
**Flow 版本**: Kotlin Flow
**推荐作用域**: viewModelScope, lifecycleScope
