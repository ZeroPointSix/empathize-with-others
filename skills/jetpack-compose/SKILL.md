---
name: jetpack-compose
description: Jetpack Compose - 声明式 UI、状态管理、布局组件、动画、Compose 最佳实践。在开发 Compose UI 时使用。
---

# Jetpack Compose

## 激活时机

当满足以下条件时自动激活此技能：
- 使用 Jetpack Compose 构建 UI
- 实现自定义 Compose 组件
- 管理组件状态
- 实现 Compose 动画
- 优化 Compose 性能

## Compose 基础

### 可组合函数

```kotlin
// ✅ Composable 函数命名用大驼峰
@Composable
fun UserCard(user: User) {
    Column {
        Text(text = user.name)
        Text(text = user.email)
    }
}

// ✅ 添加修饰符参数
@Composable
fun UserCard(
    user: User,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .clickable { onClick() }
    ) {
        Text(text = user.name)
    }
}
```

### 布局组件

```kotlin
// Column - 垂直排列
@Composable
fun UserProfile(user: User) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(user.name, style = MaterialTheme.typography.h5)
        Text(user.email, style = MaterialTheme.typography.body1)
        Button(onClick = { /*...*/}) {
            Text("Follow")
        }
    }
}

// Row - 水平排列
@Composable
fun UserListItem(user: User) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(user.name)
        Switch(checked = user.isActive, onCheckedChange = {})
    }
}

// Box - 叠加布局
@Composable
fun ImageWithBadge() {
    Box {
        Image(
            painter = painterResource(R.drawable.avatar),
            contentDescription = null
        )
        Badge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Text("5")
        }
    }
}

// LazyColumn - 列表
@Composable
fun UserList(users: List<User>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users, key = { it.id }) { user ->
            UserListItem(user)
        }
    }
}
```

## 状态管理

### remember 和 rememberSaveable

```kotlin
@Composable
fun Counter() {
    // ✅ remember：组合中保持，重组时保留
    var count by remember { mutableIntStateOf(0) }

    // ✅ rememberSaveable：配置变更后保留
    var text by rememberSaveable { mutableStateOf("") }

    Column {
        Text("Count: $count")
        Button(onClick = { count++ }) {
            Text("Increment")
        }
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter text") }
        )
    }
}
```

### 状态提升

```kotlin
// ❌ 状态在内部，父组件无法控制
@Composable
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}

// ✅ 状态提升，父组件控制
@Composable
fun Counter(
    count: Int,
    onIncrement: () -> Unit
) {
    Button(onClick = onIncrement) {
        Text("Count: $count")
    }
}

// 使用
@Composable
fun CounterContainer() {
    var count by remember { mutableIntStateOf(0) }
    Counter(
        count = count,
        onIncrement = { count++ }
    )
}
```

### ViewModel 状态

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is HomeUiState.Loading -> LoadingView()
        is HomeUiState.Success -> UserList(
            users = uiState.users,
            onUserClick = { viewModel.navigateToDetail(it) }
        )
        is HomeUiState.Error -> ErrorView(
            message = uiState.message,
            onRetry = { viewModel.retry() }
        )
    }
}
```

## 修饰符

### 常用修饰符

```kotlin
@Composable
fun ModifierExample() {
    Box(
        modifier = Modifier
            // 尺寸
            .size(100.dp)
            .fillMaxWidth()
            .fillMaxHeight()
            .weight(1f)

            // 内边距/外边距
            .padding(16.dp)
            .padding(start = 8.dp, end = 8.dp)

            // 背景
            .background(Color.Blue)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Blue, Color Purple)
                )
            )

            // 边框
            .border(2.dp, Color.Black)
            .clip(RoundedCornerShape(8.dp))

            // 可点击
            .clickable { /*...*/ }
            .toggleable(
                value = isChecked,
                onValueChange = { isChecked = it }
            )

            // 滚动
            .verticalScroll(rememberScrollState())

            // 阴影
            .shadow(elevation = 4.dp)
    )
}
```

### 自定义修饰符

```kotlin
// 创建自定义修饰符
fun Modifier.cardStyle(): Modifier = this
    .fillMaxWidth()
    .padding(16.dp)
    .background(Color.White, RoundedCornerShape(8.dp))
    .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))

// 使用
@Composable
fun UserCard(user: User) {
    Column(modifier = Modifier.cardStyle()) {
        Text(user.name)
    }
}
```

## Compose Navigation

### 导航设置

```kotlin
// NavHost 定义
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { userId ->
                    navController.navigate(Screen.Detail.createRoute(userId))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            DetailScreen(userId = userId)
        }
    }
}

// Screen 定义
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{userId}") {
        fun createRoute(userId: String) = "detail/$userId"
    }
}
```

## Compose 动画

### 基础动画

```kotlin
@Composable
fun AnimatedBox() {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .background(Color.Blue)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        if (expanded) {
            Text("This is expanded content", modifier = Modifier.padding(16.dp))
        }
    }

    Button(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Collapse" else "Expand")
    }
}
```

### 淡入淡出动画

```kotlin
@Composable
fun FadeInOutContent(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 300)
        )
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.Red)
        )
    }
}
```

## 列表性能优化

### key 参数

```kotlin
@Composable
fun UserList(users: List<User>) {
    LazyColumn {
        // ✅ 使用 key 参数，Compose 可以正确追踪项
        items(
            items = users,
            key = { user -> user.id }  // 稳定的唯一标识
        ) { user ->
            UserListItem(user)
        }
    }
}
```

### 稳定性

```kotlin
// ✅ 使用 @Immutable 标记不可变数据类
@Immutable
data class User(
    val id: String,
    val name: String,
    val email: String
)

// ✅ 使用 @Stable 标记稳定类
@Stable
class UiState {
    var isLoading: Boolean by mutableStateOf(false)
    var data: List<User> by mutableStateOf(emptyList())
}
```

## Material Design 3

### Material Theme

```kotlin
@Composable
fun MyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> darkColorScheme(
            primary = Purple80,
            secondary = PurpleGrey80,
            tertiary = Pink80
        )
        else -> lightColorScheme(
            primary = Purple40,
            secondary = PurpleGrey40,
            tertiary = Pink40
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### Scaffold 布局

```kotlin
@Composable
fun MainScreen() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Profile")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My App") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /*...*/ }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // 内容
        }
    }
}
```

## 最佳实践

### ✅ 应该做的

```
1. 使用 @Immutable/@Stable 优化重组
2. 状态提升到合适的层级
3. 使用 key 参数帮助列表追踪
4. 遵循单向数据流
5. 使用 Scaffold 实现标准布局
6. 合理使用副作用（LaunchedEffect, SideEffect）
```

### ❌ 不应该做的

```
1. 在 Composable 中执行耗时操作
2. 在 Composable 中直接修改状态
3. 过度使用重组
4. 忽略 key 参数
5. 在 remember 中存储大量数据
```

## 相关资源

- `resources/compose-basics.md` - Compose 基础
- `resources/state-management.md` - 状态管理
- `resources/compose-performance.md` - 性能优化

---

**技能状态**: 完成 ✅
**UI 框架**: Jetpack Compose
**Material 版本**: Material Design 3
**导航**: Navigation Compose
