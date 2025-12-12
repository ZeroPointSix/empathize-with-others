# UI层开发规范

## 概述

本文档定义了UI层(Presentation Layer)的代码规范,包括 Jetpack Compose、ViewModel、状态管理、导航和主题等方面的开发规范。遵循这些规范可以保证代码的一致性、可维护性和可测试性。

---

## 一、通用原则 (General Principles)

### 1.1 文件命名规范

1. **ViewModel 文件**: `[Feature]ViewModel.kt` (例: `ChatViewModel.kt`)
2. **Screen 文件**: `[Feature]Screen.kt` (例: `ChatScreen.kt`)
3. **UiState 文件**: `[Feature]UiState.kt` (例: `ChatUiState.kt`)
4. **组件文件**: `[Component]Component.kt` 或独立命名 (例: `MessageBubble.kt`)

### 1.2 类命名规范

1. **ViewModel 后缀**: 必须以 `ViewModel` 结尾
   - ✅ `ChatViewModel`, `ContactListViewModel`
   - ❌ `ChatVM`, `ContactVM`, `Chat`

2. **UiState 后缀**: 必须以 `UiState` 结尾
   - ✅ `ChatUiState`, `ContactListUiState`
   - ❌ `ChatState`, `ContactState`

3. **UiEvent 后缀**: 必须以 `UiEvent` 结尾
   - ✅ `ChatUiEvent`, `ContactListUiEvent`
   - ❌ `ChatEvent`, `ContactEvent`

### 1.3 Composable 函数命名规范

1. **Screen 级别**: `[Feature]Screen` (例: `ChatScreen`)
2. **组件级别**: `PascalCase` 名词 (例: `MessageBubble`, `ContactCard`)
3. **私有 Composable**: `PascalCase` + `Content` 后缀 (例: `MessageListContent`)

### 1.4 目录组织规范

```
presentation/
├── theme/                  # 主题和样式
│   ├── Theme.kt
│   ├── Color.kt
│   ├── Type.kt
│   └── Shape.kt
├── ui/                     # UI组件
│   ├── MainActivity.kt
│   ├── navigation/         # 导航
│   │   ├── NavGraph.kt
│   │   └── NavRoutes.kt
│   ├── screen/             # 屏幕
│   │   ├── chat/
│   │   │   ├── ChatScreen.kt
│   │   │   ├── ChatViewModel.kt
│   │   │   ├── ChatUiState.kt
│   │   │   └── ChatUiEvent.kt
│   │   └── contact/
│   │       ├── ContactListScreen.kt
│   │       └── ContactListViewModel.kt
│   └── component/          # 可复用组件
│       ├── MessageBubble.kt
│       └── ContactCard.kt
└── viewmodel/              # 或将ViewModel放在这里(可选)
```

---

## Step 1: 环境配置规范 (Dependencies)

### 规范 A: 版本管理

**规范**: 所有 Compose 相关依赖必须使用统一的 BOM (Bill of Materials) 版本管理。

**原因**: 
- 确保所有 Compose 库版本兼容
- 简化版本升级
- 避免版本冲突

```kotlin
// ✅ 正确示例: 使用 Compose BOM
dependencies {
    // === Compose BOM (版本管理) ===
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    
    // === Compose UI (不需要指定版本,由BOM管理) ===
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    // === ViewModel with Compose ===
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // === Hilt for Compose ===
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
}

// ❌ 错误示例: 手动指定每个版本
dependencies {
    implementation("androidx.compose.ui:ui:1.6.0")           // 容易版本不一致
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui-tooling:1.5.4")   // 版本混乱
}
```

### 规范 B: 依赖分组

**规范**: 将依赖按功能分组,并添加清晰的注释。

```kotlin
dependencies {
    // === Compose UI ===
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    
    // === Navigation ===
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // === Lifecycle & ViewModel ===
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // === Dependency Injection (Hilt) ===
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // === Preview & Debug Tools ===
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## Step 2: ViewModel 开发规范 (ViewModel)

### 规范 A: 命名和结构

**规范**: ViewModel 必须遵循 MVVM 模式,使用 `@HiltViewModel` 注解,通过构造函数注入 UseCase。

```kotlin
// ✅ 正确示例
/**
 * 聊天界面的 ViewModel
 *
 * 职责:
 * - 管理聊天界面的 UI 状态
 * - 处理用户交互事件
 * - 调用 UseCase 执行业务逻辑
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase,        // 注入 UseCase
    private val checkDraftUseCase: CheckDraftUseCase,
    private val feedTextUseCase: FeedTextUseCase
) : ViewModel() {
    
    // 1. UI 状态 (使用 StateFlow)
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 2. 事件处理方法
    fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.SendMessage -> sendMessage(event.text)
            is ChatUiEvent.LoadChat -> loadChat(event.contactId)
            is ChatUiEvent.CheckDraft -> checkDraft(event.text)
        }
    }
    
    // 3. 私有业务方法
    private fun sendMessage(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = analyzeChatUseCase(contactId = "...", messages = listOf(text))
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    analysisResult = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}

// ❌ 错误示例: 直接依赖 Repository
class ChatViewModel @Inject constructor(
    private val contactRepository: ContactRepository,  // ❌ ViewModel 不应直接依赖 Repository
    private val aiRepository: AiRepository
) : ViewModel()

// ❌ 错误示例: 缺少 @HiltViewModel 注解
class ChatViewModel(
    private val analyzeChatUseCase: AnalyzeChatUseCase
) : ViewModel()  // ❌ 无法进行依赖注入
```

### 规范 B: 状态管理

**规范**: 使用 `StateFlow` 管理 UI 状态,遵循单一数据源原则。

**原因**:
- `StateFlow` 有初始值,适合 UI 状态
- `asStateFlow()` 防止外部修改状态
- 与 Compose 的 `collectAsState()` 无缝集成

```kotlin
// ✅ 正确示例: 使用 StateFlow 和 不可变状态
@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val saveProfileUseCase: SaveProfileUseCase
) : ViewModel() {
    
    // 私有可变状态
    private val _uiState = MutableStateFlow(ContactListUiState())
    
    // 公开不可变状态
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()
    
    // 更新状态使用 update 方法
    fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }  // ✅ 不可变更新
            
            // 加载数据...
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    contacts = loadedContacts
                )
            }
        }
    }
}

// ❌ 错误示例: 直接暴露 MutableStateFlow
@HiltViewModel
class ContactListViewModel @Inject constructor() : ViewModel() {
    val uiState = MutableStateFlow(ContactListUiState())  // ❌ 外部可以修改
}

// ❌ 错误示例: 使用 LiveData (不推荐用于新代码)
@HiltViewModel
class ContactListViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableLiveData<ContactListUiState>()
    val uiState: LiveData<ContactListUiState> = _uiState  // ❌ 应该使用 StateFlow
}
```

### 规范 C: 事件处理

**规范**: 使用密封类定义 UiEvent,通过统一的 `onEvent()` 方法处理。

**原因**:
- 类型安全,编译时检查所有事件是否处理
- 事件集中管理,易于追踪
- 符合单一职责原则

```kotlin
// ✅ 正确示例: 使用密封类定义事件
sealed class ChatUiEvent {
    data class SendMessage(val text: String) : ChatUiEvent()
    data class LoadChat(val contactId: String) : ChatUiEvent()
    data class CheckDraft(val text: String) : ChatUiEvent()
    data class DeleteMessage(val messageId: String) : ChatUiEvent()
    object ClearError : ChatUiEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 统一事件处理入口
    fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.SendMessage -> sendMessage(event.text)
            is ChatUiEvent.LoadChat -> loadChat(event.contactId)
            is ChatUiEvent.CheckDraft -> checkDraft(event.text)
            is ChatUiEvent.DeleteMessage -> deleteMessage(event.messageId)
            is ChatUiEvent.ClearError -> clearError()
        }
    }
    
    private fun sendMessage(text: String) {
        viewModelScope.launch {
            // 业务逻辑...
        }
    }
}

// ❌ 错误示例: 为每个事件创建单独的方法并直接暴露
@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    // ❌ 过多的公开方法,难以管理
    fun sendMessage(text: String) { }
    fun loadChat(contactId: String) { }
    fun checkDraft(text: String) { }
    fun deleteMessage(messageId: String) { }
    fun clearError() { }
}
```

---

## Step 3: Composable 函数规范 (Composable Functions)

### 规范 A: 命名约定

**规范**: Composable 函数必须使用 PascalCase 命名,像普通类一样。

```kotlin
// ✅ 正确示例: PascalCase 命名
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) { }

@Composable
fun MessageBubble(
    message: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) { }

@Composable
private fun ChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) { }

// ❌ 错误示例: camelCase 命名
@Composable
fun chatScreen() { }  // ❌ 应该用 PascalCase

@Composable
fun messageBubble() { }  // ❌ 应该用 PascalCase
```

### 规范 B: 参数顺序

**规范**: Composable 函数参数必须按照以下顺序排列。

**原因**: 保持一致性,提高代码可读性。

```kotlin
// ✅ 正确示例: 标准参数顺序
@Composable
fun MessageBubble(
    // 1. 必需的数据参数
    message: String,
    timestamp: Long,
    isUser: Boolean,
    
    // 2. 可选的数据参数(带默认值)
    showTimestamp: Boolean = true,
    
    // 3. 事件回调
    onLongPress: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    
    // 4. Modifier (总是最后,且有默认值)
    modifier: Modifier = Modifier
) {
    // Composable 实现...
}

// ❌ 错误示例: 参数顺序混乱
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,          // ❌ modifier 应该在最后
    onClick: (() -> Unit)? = null,          // ❌ 回调不应该在数据之前
    message: String,
    isUser: Boolean
) { }
```

### 规范 C: 状态提升 (State Hoisting)

**规范**: 可复用组件应该是无状态的,状态由父组件管理。

**原因**:
- 提高组件可复用性
- 便于测试
- 使数据流向清晰

```kotlin
// ✅ 正确示例: 状态提升,组件无状态
@Composable
fun ChatInputField(
    value: String,                          // ✅ 状态由父组件提供
    onValueChange: (String) -> Unit,        // ✅ 事件向上传递
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("输入消息...") }
        )
        IconButton(onClick = 
onSend) {
            Icon(Icons.Default.Send, contentDescription = "发送")
        }
    }
}

// 父组件管理状态
@Composable
fun ChatScreen() {
    var inputText by remember { mutableStateOf("") }  // ✅ 状态在父组件
    
    ChatInputField(
        value = inputText,
        onValueChange = { inputText = it },
        onSend = { 
            // 发送逻辑
            inputText = ""
        }
    )
}

// ❌ 错误示例: 组件内部管理状态
@Composable
fun ChatInputField(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf("") }  // ❌ 状态在组件内部,难以复用
    
    Row(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = { value = it }
        )
        IconButton(onClick = { onSend(value) }) {
            Icon(Icons.Default.Send, contentDescription = "发送")
        }
    }
}
```

### 规范 D: 预览函数

**规范**: 每个 Composable 组件必须提供预览函数,使用 `@Preview` 注解。

**原因**:
- 在不运行应用的情况下预览 UI
- 加快开发迭代速度
- 作为组件的使用示例

```kotlin
// ✅ 正确示例: 提供多个预览场景
@Composable
fun MessageBubble(
    message: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(8.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Preview(name = "用户消息")
@Composable
private fun MessageBubbleUserPreview() {
    MaterialTheme {
        MessageBubble(
            message = "你好,这是一条用户消息",
            isUser = true
        )
    }
}

@Preview(name = "对方消息")
@Composable
private fun MessageBubbleOtherPreview() {
    MaterialTheme {
        MessageBubble(
            message = "你好,这是一条对方消息",
            isUser = false
        )
    }
}

@Preview(name = "长文本消息", widthDp = 320)
@Composable
private fun MessageBubbleLongTextPreview() {
    MaterialTheme {
        MessageBubble(
            message = "这是一条很长很长的消息,用来测试文本换行的效果是否正常显示...",
            isUser = true
        )
    }
}

@Preview(name = "深色模式", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessageBubbleDarkPreview() {
    MaterialTheme {
        MessageBubble(
            message = "深色模式下的消息",
            isUser = true
        )
    }
}

// ❌ 错误示例: 没有预览函数
@Composable
fun MessageBubble(
    message: String,
    isUser: Boolean
) {
    // 实现...
}
// ❌ 缺少 @Preview 函数,无法快速预览
```

---

## Step 4: UI State 和 Event 规范 (State & Events)

### 规范 A: UiState 命名

**规范**: UiState 必须是 `data class`,命名以 `UiState` 结尾。

```kotlin
// ✅ 正确示例: 标准 UiState 命名
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val analysisResult: AnalysisResult? = null
)

data class ContactListUiState(
    val contacts: List<ContactProfile> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedContact: ContactProfile? = null
)

// ❌ 错误示例: 命名不规范
data class ChatState(...)           // ❌ 应该是 ChatUiState
data class ChatScreenState(...)     // ❌ 应该是 ChatUiState
class ChatUiState(...)              // ❌ 应该是 data class
```

### 规范 B: UiState 结构

**规范**: UiState 必须包含所有必要的 UI 信息,并提供合理的默认值。

**原因**:
- 单一数据源,避免状态不一致
- 默认值简化初始化
- 不可变数据类保证线程安全

```kotlin
// ✅ 正确示例: 完整的 UiState 结构
data class ChatUiState(
    // 1. 数据状态
    val contactId: String? = null,
    val contactName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val draftMessage: String = "",
    
    // 2. UI 状态
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val showDatePicker: Boolean = false,
    
    // 3. 分析结果
    val analysisResult: AnalysisResult? = null,
    val safetyCheck: SafetyCheckResult? = null,
    
    // 4. 错误状态
    val error: String? = null,
    val errorType: ErrorType? = null
) {
    // 可以添加计算属性
    val hasMessages: Boolean
        get() = messages.isNotEmpty()
    
    val canSend: Boolean
        get() = draftMessage.isNotBlank() && !isLoading
}

// 错误类型枚举
enum class ErrorType {
    NETWORK_ERROR,
    API_KEY_MISSING,
    VALIDATION_ERROR
}

// ❌ 错误示例: 状态分散,缺少默认值
data class ChatUiState(
    val messages: List<ChatMessage>,        // ❌ 没有默认值,初始化困难
    val isLoading: Boolean,                 // ❌ 没有默认值
    val error: String?                      // ✅ 可空类型可以没有默认值,但建议提供 null
)

// ❌ 错误示例: 使用 var 而不是 val
data class ChatUiState(
    var messages: List<ChatMessage> = emptyList(),  // ❌ 应该用 val
    var isLoading: Boolean = false                  // ❌ 应该用 val
)
```

### 规范 C: UiEvent 设计

**规范**: 使用密封类定义 UiEvent,每个事件携带必要的数据。

**原因**:
- 类型安全
- 穷举检查
- 清晰的事件流

```kotlin
// ✅ 正确示例: 使用密封类定义事件
sealed class ChatUiEvent {
    // 数据类事件(携带参数)
    data class SendMessage(val text: String) : ChatUiEvent()
    data class LoadChat(val contactId: String) : ChatUiEvent()
    data class CheckDraft(val text: String) : ChatUiEvent()
    data class DeleteMessage(val messageId: String) : ChatUiEvent()
    data class UpdateDraft(val text: String) : ChatUiEvent()
    
    // 对象事件(不携带参数)
    object ClearError : ChatUiEvent()
    object RefreshChat : ChatUiEvent()
    object RetryLoad : ChatUiEvent()
}

// 在 ViewModel 中处理
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase
) : ViewModel() {
    
    fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.SendMessage -> sendMessage(event.text)
            is ChatUiEvent.LoadChat -> loadChat(event.contactId)
            is ChatUiEvent.CheckDraft -> checkDraft(event.text)
            is ChatUiEvent.DeleteMessage -> deleteMessage(event.messageId)
            is ChatUiEvent.UpdateDraft -> updateDraft(event.text)
            is ChatUiEvent.ClearError -> clearError()
            is ChatUiEvent.RefreshChat -> refreshChat()
            is ChatUiEvent.RetryLoad -> retryLoad()
        }
    }
}

// ❌ 错误示例: 使用普通类或接口
interface ChatUiEvent {              // ❌ 应该用 sealed class
    class SendMessage(val text: String) : ChatUiEvent
}

// ❌ 错误示例: 所有参数都放在一个事件中
data class ChatUiEvent(
    val action: String,               // ❌ 使用字符串表示动作,不类型安全
    val text: String? = null,
    val contactId: String? = null
)
```

---

## Step 5: 导航规范 (Navigation)

### 规范 A: 导航图定义

**规范**: 使用 Navigation Compose,在单独的文件中定义导航图。

```kotlin
// ✅ 正确示例: NavGraph.kt
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.ContactList.route,
        modifier = modifier
    ) {
        // 联系人列表屏幕
        composable(route = NavRoutes.ContactList.route) {
            ContactListScreen(
                onNavigateToChat = { contactId ->
                    navController.navigate(NavRoutes.Chat.createRoute(contactId))
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                }
            )
        }
        
        // 聊天屏幕(带参数)
        composable(
            route = NavRoutes.Chat.route,
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ChatScreen(
                contactId = contactId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 设置屏幕
        composable(route = NavRoutes.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// ❌ 错误示例: 在 MainActivity 中直接定义导航
@Composable
fun MainActivity() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "contacts") {  // ❌ 硬编码路由
        composable("contacts") { /* ... */ }
        composable("chat/{id}") { /* ... */ }  // ❌ 路由字符串分散
    }
}
```

### 规范 B: 路由管理

**规范**: 使用密封类集中管理所有路由,避免硬编码字符串。

**原因**:
- 类型安全
- 统一管理
- 易于重构

```kotlin
// ✅ 正确示例: NavRoutes.kt - 使用密封类管理路由
sealed class NavRoutes(val route: String) {
    // 无参数路由
    object ContactList : NavRoutes("contact_list")
    object Settings : NavRoutes("settings")
    object About : NavRoutes("about")
    
    // 带参数路由
    object Chat : NavRoutes("chat/{contactId}") {
        fun createRoute(contactId: String) = "chat/$contactId"
        const val ARG_CONTACT_ID = "contactId"
    }
    
    object ContactDetail : NavRoutes("contact_detail/{contactId}") {
        fun createRoute(contactId: String) = "contact_detail/$contactId"
        const val ARG_CONTACT_ID = "contactId"
    }
    
    object EditProfile : NavRoutes("edit_profile/{contactId}?mode={mode}") {
        fun createRoute(contactId: String, mode: String = "view") = 
            "edit_profile/$contactId?mode=$mode"
        const val ARG_CONTACT_ID = "contactId"
        const val ARG_MODE = "mode"
    }
}

// 使用示例
@Composable
fun ContactListScreen(
    onNavigateToChat: (String) -> Unit
) {
    Button(onClick = { 
        onNavigateToChat("contact_123")  // ✅ 类型安全的参数传递
    }) {
        Text("打开聊天")
    }
}

// 在 NavGraph 中使用
composable(
    route = NavRoutes.Chat.route,
    arguments = listOf(
        navArgument(NavRoutes.Chat.ARG_CONTACT_ID) { 
            type = NavType.StringType 
        }
    )
) { backStackEntry ->
    val contactId = backStackEntry.arguments?.getString(NavRoutes.Chat.ARG_CONTACT_ID) ?: ""
    ChatScreen(contactId = contactId)
}

// ❌ 错误示例: 硬编码路由字符串
@Composable
fun ContactListScreen(navController: NavController) {
    Button(onClick = { 
        navController.navigate("chat/contact_123")  // ❌ 硬编码,容易出错
    }) {
        Text("打开聊天")
    }
}

// ❌ 错误示例: 使用常量但不类型安全
object Routes {
    const val CHAT = "chat/{id}"  // ❌ 无法保证参数正确性
}
```

### 规范 C: 参数传递

**规范**: 优先使用路由参数传递简单数据,复杂对象通过 ViewModel 或 Repository 共享。

**原因**:
- 简单数据用路由参数(如 ID、标志位)
- 复杂对象避免序列化开销
- 保持导航的可恢复性

```kotlin
// ✅ 正确示例: 简单参数通过路由传递
composable(
    route = "chat/{contactId}",
    arguments = listOf(
        navArgument("contactId") { type = NavType.StringType }
    )
) { backStackEntry ->
    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
    
    // ViewModel 根据 ID 加载完整数据
    val viewModel: ChatViewModel = hiltViewModel()
    
    LaunchedEffect(contactId) {
        viewModel.onEvent(ChatUiEvent.LoadChat(contactId))
    }
    
    ChatScreen(viewModel = viewModel)
}

// ✅ 正确示例: 可选参数
composable(
    route = "edit_profile/{contactId}?mode={mode}",
    arguments = listOf(
        navArgument("contactId") { type = NavType.StringType },
        navArgument("mode") { 
            type = NavType.StringType
            defaultValue = "view"  // ✅ 提供默认值
        }
    )
) { backStackEntry ->
    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
    val mode = backStackEntry.arguments?.getString("mode") ?: "view"
    
    EditProfileScreen(
        contactId = contactId,
        mode = mode
    )
}

// ❌ 错误示例: 尝试传递复杂对象
data class ContactProfile(/* 很多字段 */)

// ❌ 不要这样做
navController.navigate("chat") {
    launchSingleTop = true
    // ❌ 无法直接传递复杂对象
}

// ✅ 
正确做法: 只传递 ID,在目标页面加载完整数据
navController.navigate("chat/$contactId")
```

---

## Step 6: 主题和样式规范 (Theme & Styling)

### 规范 A: 主题定义

**规范**: 在 `theme/Theme.kt` 中定义应用主题,使用 Material 3 设计系统。

```kotlin
// ✅ 正确示例: Theme.kt - 标准主题结构
@Composable
fun EmpathyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Android 12+ 支持动态颜色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 1. 动态颜色 (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        
        // 2. 深色模式
        darkTheme -> DarkColorScheme
        
        // 3. 浅色模式
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// 在 MainActivity 中使用
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmpathyTheme {  // ✅ 包裹整个应用
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}

// ❌ 错误示例: 在每个 Screen 中重复定义主题
@Composable
fun ChatScreen() {
    MaterialTheme {  // ❌ 不应该在 Screen 中定义主题
        // UI 内容...
    }
}
```

### 规范 B: 颜色系统

**规范**: 在 `theme/Color.kt` 中定义颜色,使用 Material 3 颜色角色。

**原因**:
- 支持深色模式自动切换
- 保证可访问性
- 统一品牌色调

```kotlin
// ✅ 正确示例: Color.kt - 定义浅色和深色配色方案
// 品牌色定义
val PrimaryLight = Color(0xFF6750A4)
val SecondaryLight = Color(0xFF625B71)
val TertiaryLight = Color(0xFF7D5260)
val ErrorLight = Color(0xFFB3261E)

val PrimaryDark = Color(0xFFD0BCFF)
val SecondaryDark = Color(0xFFCCC2DC)
val TertiaryDark = Color(0xFFEFB8C8)
val ErrorDark = Color(0xFFF2B8B5)

// 浅色配色方案
val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    
    secondary = SecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    
    tertiary = TertiaryLight,
    error = ErrorLight,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

// 深色配色方案
val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    
    secondary = SecondaryDark,
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    
    tertiary = TertiaryDark,
    error = ErrorDark,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
)

// 在 Composable 中使用
@Composable
fun MessageBubble(message: String, isUser: Boolean) {
    Surface(
        color = if (isUser) 
            MaterialTheme.colorScheme.primary       // ✅ 使用主题颜色
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            color = if (isUser)
                MaterialTheme.colorScheme.onPrimary  // ✅ 使用对应的文本颜色
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

// ❌ 错误示例: 硬编码颜色
@Composable
fun MessageBubble(message: String, isUser: Boolean) {
    Surface(
        color = if (isUser) Color.Blue else Color.Gray  // ❌ 硬编码,不支持深色模式
    ) {
        Text(
            text = message,
            color = Color.White  // ❌ 在深色模式下可能不可见
        )
    }
}
```

### 规范 C: 排版系统

**规范**: 在 `theme/Type.kt` 中定义文字样式,使用 Material 3 排版比例。

```kotlin
// ✅ 正确示例: Type.kt - 定义排版系统
val Typography = Typography(
    // Display 级别 - 用于大标题
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    
    // Headline 级别 - 用于页面标题
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    
    // Title 级别 - 用于卡片标题
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    
    // Body 级别 - 用于正文
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    
    // Label 级别 - 用于按钮文字
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

// 在 Composable 中使用
@Composable
fun ChatScreen() {
    Column {
        // ✅ 使用主题中定义的文字样式
        Text(
            text = "聊天",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Text(
            text = "与好友对话",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ❌ 错误示例: 硬编码文字样式
@Composable
fun ChatScreen() {
    Column {
        Text(
            text = "聊天",
            fontSize = 32.sp,           // ❌ 硬编码字体大小
            fontWeight = FontWeight.Bold // ❌ 不一致的样式
        )
        
        Text(
            text = "与好友对话",
            fontSize = 14.sp,           // ❌ 应该使用主题样式
            color = Color.Gray          // ❌ 应该使用主题颜色
        )
    }
}
```

### 规范 D: 深色模式支持

**规范**: 所有 UI 组件必须正确支持深色模式。

**原因**:
- 提升用户体验
- 省电(OLED 屏幕)
- 符合系统级设置

```kotlin
// ✅ 正确示例: 使用主题颜色自动适配深色模式
@Composable
fun ContactCard(
    contact: ContactProfile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,      // ✅ 自动适配
            contentColor = MaterialTheme.colorScheme.onSurface      // ✅ 自动适配
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // 头像
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,  // ✅ 自动适配
                        CircleShape
                    )
            )
            
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface    // ✅ 自动适配
                )
                Text(
                    text = contact.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant  // ✅ 自动适配
                )
            }
        }
    }
}

// 预览深色模式
@Preview(name = "浅色模式")
@Preview(name = "深色模式", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactCardPreview() {
    EmpathyTheme {
        Surface {
            ContactCard(
                contact = ContactProfile(
                    name = "张三",
                    lastMessage = "晚上一起吃饭吧"
                )
            )
        }
    }
}

// ❌ 错误示例: 硬编码颜色,不支持深色模式
@Composable
fun ContactCard(contact: ContactProfile) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White,    // ❌ 在深色模式下刺眼
            contentColor = Color.Black       // ❌ 在深色模式下不可见
        )
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFEEEEEE))  // ❌ 硬编码,不适配深色模式
            )
            
            Column {
                Text(
                    text = contact.name,
                    color = Color.Black  // ❌ 在深色模式下不可见
                )
                Text(
                    text = contact.lastMessage,
                    color = Color.Gray   // ❌ 对比度可能不足
                )
            }
        }
    }
}
```

---

## 三、总结

### 关键要点回顾

1. **ViewModel 规范**
   - 使用 `@HiltViewModel` 注解进行依赖注入
   - 通过 `StateFlow` 管理 UI 状态
   - 使用密封类定义 UiEvent
   - 只依赖 UseCase,不直接依赖 Repository

2. **Composable 规范**
   - PascalCase 命名,像类一样
   - 参数顺序: 数据 → 回调 → Modifier
   - 状态提升,组件无状态
   - 提供 `@Preview` 函数

3. **State & Event 规范**
   - UiState 使用 `data class`,提供默认值
   - UiEvent 使用 `sealed class`,类型安全
   - 单一数据源原则

4. **导航规范**
   - 使用密封类管理路由
   - 简单数据通过路由参数传递
   - 复杂对象通过 ViewModel 加载

5. **主题规范**
   - 使用 Material 3 设计系统
   - 统一的颜色和排版系统
   - 完整支持深色模式

### 建议执行路径

**阶段 1: 环境配置** (Step 1)
1. 添加 Compose 和 Hilt 依赖
2. 配置 BOM 版本管理
3. 添加预览和调试工具

**阶段 2: 基础架构** (Step 2 + Step 4)
1. 创建 ViewModel 基础结构
2. 定义 UiState 和 UiEvent
3. 实现状态管理逻辑

**阶段 3: UI 开发** (Step 3)
1. 创建 Composable 组件
2. 实现状态提升
3. 添加预览函数

**阶段 4: 导航集成** (Step 5)
1. 定义路由管理
2. 创建导航图
3. 实现页面跳转

**阶段 5: 主题美化** (Step 6)
1. 定义颜色系统
2. 配置排版系统
3. 测试深色模式

### 与其他层的关系

**UI层 → 服务层 (Domain)**:
```kotlin
// UI 层通过 ViewModel 调用 UseCase
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase  // 服务层 UseCase
) : ViewModel() {
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            // 调用服务层执行业务逻辑
            val result = analyzeChatUseCase(
                contactId = contactId,
                messages = listOf(text)
            )
            
            // 处理 Result 类型返回值
            _uiState.update {
                it.copy(
                    analysisResult = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}
```

**依赖方向**: UI层 → 服务层 → 数据层

**禁止**: 
- ❌ UI层直接访问数据层 Repository
- ❌ UI层包含业务逻辑
- ❌ UI层直接处理数据转换

**遵循**:
- ✅ UI层只负责显示和用户交互
- ✅ 所有业务逻辑在服务层 UseCase 中
- ✅ 通过 Result 类型统一错误处理

---

## 