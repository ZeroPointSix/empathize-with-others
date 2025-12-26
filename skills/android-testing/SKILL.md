---
name: android-testing
description: Android 测试 - 单元测试、集成测试、UI 测试、测试最佳实践。在编写 Android 测试时使用。
---

# Android 测试

## 激活时机

当满足以下条件时自动激活此技能：
- 编写单元测试
- 编写集成测试
- 编写 UI 测试
- 测试 ViewModel
- 测试 Repository
- 测试 Compose UI

## 测试层次

```
┌─────────────────────────────────────┐
│      E2E Tests (端到端)             │
│  真实设备/模拟器                     │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│    Integration Tests (集成测试)      │
│  Android 组件、数据库                │
└───────────────┬─────────────────────┘
                ↓
┌─────────────────────────────────────┐
│      Unit Tests (单元测试)           │
│  纯 Kotlin/Java，无需 Android        │
└─────────────────────────────────────┘
```

## 单元测试

### ViewModel 测试

```kotlin
@ExperimentalCoroutinesApi
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private val getUsersUseCase: GetUsersUseCase = mockk()
    private val refreshUsersUseCase: RefreshUsersUseCase = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = HomeViewModel(getUsersUseCase, refreshUsersUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when load users, emit success state`() = runTest {
        // Given
        val expectedUsers = listOf(
            User(id = "1", name = "Alice"),
            User(id = "2", name = "Bob")
        )
        coEvery { getUsersUseCase() } returns flowOf(
            Resource.Success(expectedUsers)
        )

        // When
        viewModel.loadUsers()

        // Then
        assertEquals(
            HomeViewModel.UiState.Success(expectedUsers),
            viewModel.uiState.value
        )
    }

    @Test
    fun `when refresh users with error, emit error state`() = runTest {
        // Given
        coEvery { refreshUsersUseCase() } returns Result.failure(
            Exception("Network error")
        )

        // When
        viewModel.refresh()

        // Then
        val event = viewModel.events.testIn(this).awaitItem()
        assertTrue(event is HomeViewModel.UiEvent.ShowError)
    }
}
```

### UseCase 测试

```kotlin
class GetUsersUseCaseTest {

    private val userRepository: UserRepository = mockk()
    private lateinit var useCase: GetUsersUseCase

    @Before
    fun setup() {
        useCase = GetUsersUseCase(userRepository)
    }

    @Test
    fun `should emit users from repository`() = runTest {
        // Given
        val expectedUsers = listOf(
            User(id = "1", name = "Alice"),
            User(id = "2", name = "Bob")
        )
        every { userRepository.getUsers() } returns flowOf(expectedUsers)

        // When
        val result = useCase().toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(expectedUsers, result[0])
    }
}
```

### Repository 测试

```kotlin
class UserRepositoryImplTest {

    private lateinit var repository: UserRepositoryImpl
    private val localDataSource: UserLocalDataSource = mockk()
    private val remoteDataSource: UserRemoteDataSource = mockk()

    @Before
    fun setup() {
        repository = UserRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun `getUsers should return local data first`() = runTest {
        // Given
        val localUsers = listOf(User(id = "1", name = "Alice"))
        val remoteUsers = listOf(User(id = "1", name = "Alice"), User(id = "2", name = "Bob"))
        every { localDataSource.getUsers() } returns flowOf(localUsers)
        every { remoteDataSource.getUsers() } returns Result.success(remoteUsers)

        // When
        val result = repository.getUsers().toList()

        // Then
        assertEquals(2, result.size)
        assertEquals(localUsers, result[0])
        assertEquals(remoteUsers, result[1])
    }
}
```

## 集成测试

### DAO 测试

```kotlin
@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetUser() = runTest {
        // Given
        val user = User(
            name = "Alice",
            email = "alice@example.com"
        )

        // When
        userDao.insert(user)
        val retrieved = userDao.getById(user.id).first()

        // Then
        assertNotNull(retrieved)
        assertEquals("Alice", retrieved?.name)
    }
}
```

### Repository 集成测试

```kotlin
@RunWith(AndroidJUnit4::class)
class UserRepositoryIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = UserRepositoryImpl(database.userDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveAndGetUser() = runTest {
        // Given
        val user = User(name = "Alice", email = "alice@example.com")

        // When
        repository.insertUser(user)
        val retrieved = repository.getUserById(user.id).first()

        // Then
        assertEquals("Alice", retrieved?.name)
    }
}
```

## UI 测试

### Compose UI 测试

```kotlin
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `when loading, show loading indicator`() {
        // Given
        val viewModel = mockk<HomeViewModel>()
        every { viewModel.uiState } returns MutableStateFlow(
            HomeUiState.Loading
        ).asStateFlow()

        // When
        composeTestRule.setContent {
            HomeScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Loading")
            .assertIsDisplayed()
    }

    @Test
    fun `when success, show user list`() {
        // Given
        val users = listOf(
            User(id = "1", name = "Alice"),
            User(id = "2", name = "Bob")
        )
        val viewModel = mockk<HomeViewModel>()
        every { viewModel.uiState } returns MutableStateFlow(
            HomeUiState.Success(users)
        ).asStateFlow()

        // When
        composeTestRule.setContent {
            HomeScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Alice")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Bob")
            .assertIsDisplayed()
    }

    @Test
    fun `when click user, navigate to detail`() {
        // Given
        val users = listOf(User(id = "1", name = "Alice"))
        val viewModel = mockk<HomeViewModel>()
        every { viewModel.uiState } returns MutableStateFlow(
            HomeUiState.Success(users)
        ).asStateFlow()
        coEvery { viewModel.onUserClick(any()) } just Runs

        // When
        composeTestRule.setContent {
            HomeScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithText("Alice")
            .performClick()

        // Then
        coVerify { viewModel.onUserClick(users[0]) }
    }
}
```

### Activity/Fragment 测试

```kotlin
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun `when login with valid credentials, navigate to home`() {
        // Given
        val username = composeTestRule.onNode(withId(R.id.username_edit_text))
        val password = composeTestRule.onNode(withId(R.id.password_edit_text))
        val loginButton = composeTestRule.onNode(withId(R.id.login_button))

        // When
        username.performTextInput("test@example.com")
        password.performTextInput("password123")
        loginButton.performClick()

        // Then
        Intents.intended(IntentMatchers.hasComponent(MainActivity::class.java.name))
    }
}
```

## 测试工具

### MockK

```kotlin
// 创建 mock
val repository: UserRepository = mockk()

// 设置行为
coEvery { repository.getUser("1") } returns User(id = "1", name = "Alice")
every { repository.getUsersFlow() } returns flowOf(listOf(user))

// 验证调用
coVerify { repository.getUser("1") }
verify { repository.getUsersFlow() }

// 验证未调用
coVerify(exactly = 0) { repository.delete(any()) }

// 参数匹配
coEvery { repository.getUser(match { it.length > 0 }) } returns user
```

### Turbine

```kotlin
// 测试 Flow
@Test
fun `test flow emissions`() = runTest {
    val flow = flowOf(1, 2, 3)

    val result = flow.testIn(this)

    assertEquals(1, result.awaitItem())
    assertEquals(2, result.awaitItem())
    assertEquals(3, result.awaitItem())
    result.awaitComplete()
}
```

## 最佳实践

### ✅ 应该做的

```
1. 单元测试要快速
2. 使用 mock 隔离依赖
3. 测试命名清晰
4. 遵循 AAA 模式
5. 测试边界情况
6. 保持测试简单
```

### ❌ 不应该做的

```
1. 测试实现细节
2. 测试第三方库
3. 过度使用 mock
4. 测试 trivial 代码
5. 测试过多私有方法
```

## 相关资源

- `resources/unit-testing.md` - 单元测试指南
- `resources/ui-testing.md` - UI 测试指南
- `resources/test-doubles.md` - 测试替身

---

**技能状态**: 完成 ✅
**测试框架**: JUnit 4/5, MockK, Turbine
**UI 测试**: Compose Testing, Espresso
**Runner**: AndroidJUnit4
