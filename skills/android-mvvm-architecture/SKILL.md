---
name: android-mvvm-architecture
description: Android MVVM 架构 - ViewModel、LiveData/StateFlow、Repository、UseCase、依赖注入。在实现 Android 架构时使用。
---

# Android MVVM 架构

## 激活时机

当满足以下条件时自动激活此技能：
- 设计 Android 应用架构
- 实现 MVVM 模式
- 组织代码层次
- 管理应用状态

## MVVM 架构概览

### 分层架构

```
┌─────────────────────────────────────┐
│         UI Layer (View)              │
│  Activity / Fragment / Compose       │
│  观察 ViewModel 数据                │
│  处理用户交互                        │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│      Presentation Layer             │
│  ViewModel / StateFlow              │
│  管理 UI 状态                       │
│  处理 UI 逻辑                       │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│         Domain Layer                │
│  UseCase / Interactor               │
│  业务逻辑                           │
│  用例编排                           │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│          Data Layer                 │
│  Repository / DataSource           │
│  数据获取和缓存                     │
└─────────────────────────────────────┘
```

### 各层职责

| 层级 | 职责 | 不应包含 |
|------|------|----------|
| **View** | UI 渲染、用户交互 | 业务逻辑 |
| **ViewModel** | UI 状态管理、UI 逻辑 | 直接数据库操作 |
| **UseCase** | 单个业务用例 | UI 逻辑、数据源细节 |
| **Repository** | 数据协调、缓存策略 | UI 相关代码 |

## ViewModel

### 基础 ViewModel

```kotlin
class HomeViewModel(
    private val getUsersUseCase: GetUsersUseCase,
    private val refreshUsersUseCase: RefreshUsersUseCase
) : ViewModel() {

    // 使用 StateFlow 管理 UI 状态
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 使用 Channel 处理一次性事件
    private val _events = Channel<UiEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            getUsersUseCase()
                .collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.value = UiState.Success(result.data)
                        }
                        is Resource.Error -> {
                            _uiState.value = UiState.Error(result.message)
                        }
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val result = refreshUsersUseCase()
            result.onSuccess {
                _events.send(UiEvent.ShowMessage("刷新成功"))
            }.onFailure { error ->
                _events.send(UiEvent.ShowError(error.message ?: "刷新失败"))
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val users: List<User>) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        data class ShowError(val error: String) : UiEvent()
        data class NavigateToDetail(val userId: String) : UiEvent()
    }
}
```

### ViewModel Factory

```kotlin
// 使用 Hilt 或 Koin 注入
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    // ...
}

// 手动 Factory（不推荐）
class UserProfileViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

## UseCase

### 单个用例

```kotlin
class GetUsersUseCase(
    private val repository: UserRepository
) {
    // 操作符调用
    operator fun invoke(): Flow<Resource<List<User>>> {
        return repository.getUsers()
            .map { users ->
                Resource.Success(users)
            }
            .catch { error ->
                emit(Resource.Error(error.message ?: "Unknown error"))
            }
    }
}

// 使用
viewModelScope.launch {
    val result = getUsersUseCase().first()
    when (result) {
        is Resource.Success -> showUsers(result.data)
        is Resource.Error -> showError(result.message)
    }
}
```

### 组合用例

```kotlin
class RefreshUserProfileUseCase(
    private val getUserUseCase: GetUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val syncPreferencesUseCase: SyncPreferencesUseCase
) {
    suspend operator fun invoke(userId: String): Result<User> {
        // 组合多个用例
        val user = getUserUseCase(userId).first()
            .getOrNull() ?: return Result.failure(Exception("User not found"))

        val updatedUser = updateUserUseCase(user)
            .getOrNull() ?: return Result.failure(Exception("Update failed"))

        syncPreferencesUseCase(updatedUser)

        return Result.success(updatedUser)
    }
}
```

## Repository

### Repository 接口

```kotlin
interface UserRepository {
    fun getUsers(): Flow<List<User>>
    fun getUserById(id: String): Flow<User?>
    suspend fun insertUser(user: User)
    suspend fun updateUser(user: User)
    suspend fun deleteUser(user: User)
}
```

### Repository 实现

```kotlin
class UserRepositoryImpl(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {

    override fun getUsers(): Flow<List<User>> {
        return Flow.concat(
            // 先展示缓存
            localDataSource.getUsers(),
            // 然后从远程获取
            flow {
                emitAll(remoteDataSource.getUsers())
                localDataSource.saveAll(remoteDataSource.getUsers().first())
            }
        ).distinctUntilChanged()
    }

    override fun getUserById(id: String): Flow<User?> {
        return localDataSource.getUserById(id)
    }

    override suspend fun insertUser(user: User) {
        localDataSource.insertUser(user)
        remoteDataSource.createUser(user)
    }

    override suspend fun updateUser(user: User) {
        localDataSource.updateUser(user)
        remoteDataSource.updateUser(user)
    }

    override suspend fun deleteUser(user: User) {
        localDataSource.deleteUser(user)
        remoteDataSource.deleteUser(user)
    }
}
```

## 数据源

### Local DataSource

```kotlin
class UserLocalDataSource(
    private val userDao: UserDao,
    private val sharedPreferences: SharedPreferences
) {
    fun getUsers(): Flow<List<User>> {
        return userDao.getAll()
    }

    fun getUserById(id: String): Flow<User?> {
        return userDao.getById(id)
    }

    suspend fun saveAll(users: List<User>) {
        userDao.insertAll(users)
    }
}
```

### Remote DataSource

```kotlin
class UserRemoteDataSource(
    private val apiService: ApiService
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getUsers(): Flow<List<User>> = flow {
        emit(apiService.getUsers())
    }.catch { error ->
        throw NetworkException(error)
    }
}
```

## 依赖注入

### Hilt 注入

```kotlin
// Application
@HiltAndroidApp
class MyApplication : Application()

// Module
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class UserRepositoryModule {
    @Binds
    @ViewModelScoped
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
}

// 使用
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase
) : ViewModel()
```

### Koin 注入

```kotlin
// Module 定义
val appModule = module {
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().userDao() }

    single<UserRemoteDataSource> {
        UserRemoteDataSource(get())
    }

    single<UserLocalDataSource> {
        UserLocalDataSource(get())
    }

    single<UserRepository> {
        UserRepositoryImpl(get(), get())
    }

    factory { GetUsersUseCase(get()) }
    factory { RefreshUsersUseCase(get()) }

    viewModel { HomeViewModel(get(), get()) }
}

// Application
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)
            modules(appModule)
        }
    }
}
```

## UI 状态管理

### UiState 模式

```kotlin
// 定义 UI 状态
sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val users: List<User>,
        val hasMore: Boolean
    ) : HomeUiState()
    data class Error(
        val message: String,
        val isRefreshError: Boolean = false
    ) : HomeUiState()
}

// ViewModel
class HomeViewModel(
    private val getHomeDataUseCase: GetHomeDataUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            getHomeDataUseCase().collect { result ->
                _uiState.value = when (result) {
                    is Resource.Success -> HomeUiState.Success(
                        users = result.data,
                        hasMore = result.data.size >= PAGE_SIZE
                    )
                    is Resource.Error -> HomeUiState.Error(result.message)
                }
            }
        }
    }
}
```

### Compose UI 状态

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is HomeUiState.Loading -> LoadingView()
        is HomeUiState.Success -> UserList(
            users = uiState.users,
            hasMore = uiState.hasMore,
            onLoadMore = { viewModel.loadMore() }
        )
        is HomeUiState.Error -> ErrorView(
            message = uiState.message,
            onRetry = { viewModel.retry() }
        )
    }
}
```

## 最佳实践

### ✅ 应该做的

```
1. 明确分层职责
2. 单向数据流
3. 使用依赖注入
4. 遵循 SOLID 原则
5. StateFlow/LiveData 用于状态
6. Channel/SharedFlow 用于事件
```

### ❌ 不应该做的

```
1. ViewModel 中持有 View 引用
2. 在 View 中直接访问数据源
3. UseCase 之间直接调用
4. Repository 层包含业务逻辑
5. 跨层直接访问
```

## 相关资源

- `resources/viewmodel-patterns.md` - ViewModel 模式
- `resources/state-management.md` - 状态管理
- `resources/di-guide.md` - 依赖注入指南

---

**技能状态**: 完成 ✅
**推荐架构**: MVVM + Clean Architecture
**状态管理**: StateFlow, LiveData, Kotlin Flow
**DI 框架**: Hilt, Koin
