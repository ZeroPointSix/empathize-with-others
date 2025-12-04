# UI层第一模块：UI组件开发实践指南

## 概述
本文档提供UI层核心组件的分步开发指南，帮助开发者按照标准流程实现UI功能。基于 Jetpack Compose + MVVM 架构，采用单向数据流和状态提升设计模式。

---

## 开发流程概览
1. **Step 1**: 定义UI State和Event
2. **Step 2**: 创建ViewModel
3. **Step 3**: 实现Screen Composable
4. **Step 4**: 构建可复用组件
5. **Step 5**: 集成导航和主题

---

## Step 1: 定义UI State和Event

### 设计目标
为界面定义清晰的状态和事件结构，实现单向数据流。

**相当于：** 在后端定义 DTO 和 Request 对象，明确前后端交互的数据结构。

### 开发步骤

#### 1.1 创建UiState数据类
**文件位置：** `presentation/ui/screen/[feature]/[Feature]UiState.kt`

**示例：ChatUiState.kt**
```kotlin
package com.empathy.ai.presentation.ui.screen.chat

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ChatMessage

/**
 * 聊天界面的UI状态
 * 
 * 设计原则：
 * 1. 所有字段都有默认值，避免空指针
 * 2. 使用 data class，自动获得 copy() 方法
 * 3. 状态是不可变的 (val)，通过 copy() 更新
 */
data class ChatUiState(
    // 通用状态字段
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // 业务数据字段
    val messages: List<ChatMessage> = emptyList(),
    val analysisResult: AnalysisResult? = null,
    val contactName: String = "",
    val contactId: String = "",
    
    // 特定交互状态
    val isAnalyzing: Boolean = false,
    val showAnalysisDialog: Boolean = false
)
```

**规范要点：**
- ✅ 使用 `data class` 定义，获得自动生成的 `copy()` 方法
- ✅ 所有字段都是 `val`（不可变）
- ✅ 所有字段都有默认值，方便初始化
- ✅ 必须包含 `isLoading` 和 `error` 字段（通用模式）
- ✅ 业务数据字段命名清晰，避免缩写

**字段设计模式：**
```kotlin
// 模式1: 加载状态（必需）
val isLoading: Boolean = false
val error: String? = null

// 模式2: 列表数据
val items: List<T> = emptyList()  // 默认空列表而非 null

// 模式3: 可选单个对象
val detail: T? = null

// 模式4: 交互状态
val isDialogShown: Boolean = false
val selectedItemId: String? = null
```

#### 1.2 创建UiEvent密封类
**文件位置：** 与UiState同目录

**示例：ChatUiEvent.kt**
```kotlin
package com.empathy.ai.presentation.ui.screen.chat

/**
 * 聊天界面的用户事件
 * 
 * 设计原则：
 * 1. 使用 sealed interface 定义，编译时类型安全
 * 2. 每个事件都是独立的类型
 * 3. 有参数用 data class，无参数用 data object
 */
sealed interface ChatUiEvent {
    // 带参数的事件
    data class SendMessage(val content: String) : ChatUiEvent
    data class LoadMessages(val contactId: String) : ChatUiEvent
    
    // 无参数的事件
    data object AnalyzeChat : ChatUiEvent
    data object ClearError : ChatUiEvent
    data object ShowAnalysisDialog : ChatUiEvent
    data object DismissAnalysisDialog : ChatUiEvent
}
```

**规范要点：**
- ✅ 使用 `sealed interface` 定义（Kotlin 1.5+）
- ✅ 每个事件都是独立的类型，避免字符串标识
- ✅ 带参数用 `data class`，无参数用 `data object`
- ✅ 事件命名使用动词开头，描述用户意图

**事件分类模式：**
```kotlin
sealed interface UiEvent {
    // 1. 数据加载类
    data class LoadData(val id: String) : UiEvent
    data object RefreshData : UiEvent
    
    // 2. 用户输入类
    data class UpdateField(val value: String) : UiEvent
    data class Submit(val data: FormData) : UiEvent
    
    // 3. 导航类
    data object NavigateBack : UiEvent
    data class NavigateToDetail(val id: String) : UiEvent
    
    // 4. UI交互类
    data object ToggleDialog : UiEvent
    data object ClearError : UiEvent
}
```

---

## Step 2: 创建ViewModel

### 设计目标
实现状态管理和业务逻辑调用，作为UI和Domain层的桥梁。

**相当于：** 后端的 Controller/Service 层，处理请求并调用业务逻辑。

### 开发步骤

#### 2.1 创建ViewModel文件
**文件位置：** `presentation/viewmodel/[Feature]ViewModel.kt`

**示例：ChatViewModel.kt**
```kotlin
package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.usecase.AnalyzeChatUseCase
import com.empathy.ai.domain.usecase.GetMessagesUseCase
import com.empathy.ai.presentation.ui.screen.chat.ChatUiEvent
import com.empathy.ai.presentation.ui.screen.chat.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 聊天界面的ViewModel
 * 
 * 职责：
 * 1. 管理UI状态（ChatUiState）
 * 2. 处理用户事件（ChatUiEvent）
 * 3. 调用UseCase执行业务逻辑
 * 4. 异常处理和状态更新
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase,
    private val getMessagesUseCase: GetMessagesUseCase
) : ViewModel() {
    
    // 私有可变状态（只能内部修改）
    private val _uiState = MutableStateFlow(ChatUiState())
    
    // 公开不可变状态（外部只读）
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    /**
     * 统一的事件处理入口
     * 
     * 设计意图：
     * 1. 单一入口，便于追踪和调试
     * 2. when 表达式确保处理所有事件类型
     */
    fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.SendMessage -> sendMessage(event.content)
            is ChatUiEvent.AnalyzeChat -> analyzeChat()
            is ChatUiEvent.ClearError -> clearError()
            is ChatUiEvent.LoadMessages -> loadMessages(event.contactId)
            is ChatUiEvent.ShowAnalysisDialog -> showAnalysisDialog()
            is ChatUiEvent.DismissAnalysisDialog -> dismissAnalysisDialog()
        }
    }
    
    /**
     * 私有方法：发送消息
     * 
     * 状态更新模式：使用 update {} 函数式更新
     */
    private fun sendMessage(content: String) {
        viewModelScope.launch {
            // 输入验证
            if (content.isBlank()) {
                _uiState.update { it.copy(error = "消息不能为空") }
                return@launch
            }
            
            // 更新UI状态
            val newMessage = ChatMessage(
                content = content,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
            
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + newMessage,
                    error = null
                )
            }
        }
    }
    
    /**
     * 私有方法：分析聊天
     * 
     * 异步操作模式：
     * 1. 开始前设置 isLoading = true
     * 2. 调用UseCase
     * 3. 成功/失败分别处理
     * 4. 最后设置 isLoading = false
     */
    private fun analyzeChat() {
        viewModelScope.launch {
            // 1. 设置加载状态
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            
            // 2. 调用UseCase
            val currentState = _uiState.value
            analyzeChatUseCase(
                contactId = currentState.contactId,
                messages = currentState.messages.map { it.content }
            )
                .onSuccess { result ->
                    // 3a. 成功：更新结果
                    _uiState.update { 
                        it.copy(
                            isAnalyzing = false,
                            analysisResult = result,
                            showAnalysisDialog = true
                        )
                    }
                }
                .onFailure { error ->
                    // 3b. 失败：显示错误
                    _uiState.update { 
                        it.copy(
                            isAnalyzing = false,
                            error = error.message ?: "分析失败，请重试"
                        )
                    }
                }
        }
    }
    
    /**
     * 私有方法：加载消息
     * 
     * Flow收集模式：长期订阅数据源
     */
    private fun loadMessages(contactId: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true,
                    contactId = contactId,
                    error = null
                )
            }
            
            getMessagesUseCase(contactId)
                .collect { result ->
                    result
                        .onSuccess { messages ->
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    messages = messages
                                )
                            }
                        }
                        .onFailure { error ->
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "加载失败"
                                )
                            }
                        }
                }
        }
    }
    
    // 简单状态切换方法
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun showAnalysisDialog() {
        _uiState.update { it.copy(showAnalysisDialog = true) }
    }
    
    private fun dismissAnalysisDialog() {
        _uiState.update { it.copy(showAnalysisDialog = false) }
    }
}
```

**规范要点：**
- ✅ 使用 `@HiltViewModel` 注解实现依赖注入
- ✅ 构造函数只注入UseCase，不注入Repository
- ✅ 使用 `StateFlow` 管理状态（而非 LiveData）
- ✅ 统一的 `onEvent()` 方法处理所有事件
- ✅ 在 `viewModelScope` 中执行异步操作
- ✅ 使用 `update {}` 函数式更新状态
- ✅ 私有方法处理具体业务逻辑

**状态更新模式对比：**
```kotlin
// ❌ 错误：直接赋值（不是线程安全的）
_uiState.value = _uiState.value.copy(isLoading = true)

// ✅ 正确：使用 update 函数
_uiState.update { it.copy(isLoading = true) }

// ✅ 正确：更新多个字段
_uiState.update { currentState ->
    currentState.copy(
        isLoading = false,
        data = newData,
        error = null
    )
}
```

---

## Step 3: 实现Screen Composable

### 设计目标
创建屏幕级别的Composable函数，实现UI渲染和交互。

**相当于：** 前端的 Page 组件，负责页面布局和用户交互。

### 开发步骤

#### 3.1 创建Screen文件
**文件位置：** `presentation/ui/screen/[feature]/[Feature]Screen.kt`

**示例：ChatScreen.kt**
```kotlin
package com.empathy.ai.presentation.ui.screen.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.presentation.viewmodel.ChatViewModel

/**
 * 聊天界面（有状态）
 * 
 * 职责：
 * 1. 获取ViewModel
 * 2. 收集UI状态
 * 3. 处理副作用（LaunchedEffect）
 * 4. 委托给无状态组件渲染
 * 
 * 设计模式：状态提升（Hoisting）
 */
@Composable
fun ChatScreen(
    contactId: String,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    // 1. 收集UI状态
    val uiState by viewModel.uiState.collectAsState()
    
    // 2. 处理副作用：加载数据
    LaunchedEffect(contactId) {
        viewModel.onEvent(ChatUiEvent.LoadMessages(contactId))
    }
    
    // 3. 委托给无状态组件
    ChatScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * 聊天界面内容（无状态）
 * 
 * 设计意图：
 * 1. 纯UI渲染，无业务逻辑
 * 2. 方便预览和测试
 * 3. 通过回调向上传递事件
 */
@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    onEvent: (ChatUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ChatTopBar(
                contactName = uiState.contactName,
                onNavigateBack = onNavigateBack,
                onAnalyzeClick = { onEvent(ChatUiEvent.AnalyzeChat) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 消息列表
                MessageList(
                    messages = uiState.messages,
                    modifier = Modifier.weight(1f)
                )
                
                // 输入框
                MessageInput(
                    onSendMessage = { content ->
                        onEvent(ChatUiEvent.SendMessage(content))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 加载指示器（覆盖层）
            if (uiState.isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // 错误提示（Snackbar）
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { onEvent(ChatUiEvent.ClearError) }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
    
    // 分析结果对话框
    if (uiState.showAnalysisDialog && uiState.analysisResult != null) {
        AnalysisResultDialog(
            result = uiState.analysisResult,
            onDismiss = { onEvent(ChatUiEvent.DismissAnalysisDialog) }
        )
    }
}
```

**规范要点：**
- ✅ Screen级函数注入ViewModel
- ✅ 使用 `hiltViewModel()` 获取ViewModel
- ✅ 状态提升：分离有状态和无状态Composable
- ✅ 使用 `LaunchedEffect` 处理副作用
- ✅ 通过回调传递事件，不直接访问ViewModel
- ✅ 使用 `Scaffold` 构建标准页面结构

**LaunchedEffect使用模式：**
```kotlin
// 模式1: 页面首次加载数据
LaunchedEffect(Unit) {
    viewModel.onEvent(UiEvent.LoadData)
}

// 模式2: 参数变化时重新加载
LaunchedEffect(itemId) {
    viewModel.onEvent(UiEvent.LoadItem(itemId))
}

// 模式3: 监听多个参数
LaunchedEffect(param1, param2) {
    viewModel.onEvent(UiEvent.Refresh(param1, param2))
}
```

---

## Step 4: 构建可复用组件

### 设计目标
创建小而专注的可复用Composable组件，遵循单一职责原则。

**相当于：** 前端的通用UI组件库，如按钮、卡片、表单等。

### 开发步骤

#### 4.1 创建消息卡片组件
**文件位置：** `presentation/ui/component/MessageCard.kt`

```kotlin
package com.empathy.ai.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ChatMessage
import com.empathy.ai.presentation.theme.GiveLoveTheme

/**
 * 消息卡片组件
 * 
 * 设计原则：
 * 1. 无状态，通过参数接收数据
 * 2. Modifier参数有默认值
 * 3. 使用Material Design 3主题
 */
@Composable
fun MessageCard(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (message.isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageCardPreview() {
    GiveLoveTheme {
        Column {
            MessageCard(
                message = ChatMessage(
                    content = "你好！",
                    isUser = true
                )
            )
            MessageCard(
                message = ChatMessage(
                    content = "你好！有什么可以帮助你的？",
                    isUser = false
                )
            )
        }
    }
}
```

#### 4.2 创建输入组件
**文件位置：** `presentation/ui/component/MessageInput.kt`

```kotlin
package com.empathy.ai.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.GiveLoveTheme

/**
 * 消息输入组件
 * 
 * 设计模式：受控组件
 * 内部管理输入状态，通过回调向上传递结果
 */
@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "输入消息..."
) {
    var text by remember { mutableStateOf("") }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(placeholder) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors()
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSendMessage(text)
                    text = ""  // 清空输入框
                }
            },
            enabled = text.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "发送",
                tint = if (text.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageInputPreview() {
    GiveLoveTheme {
        MessageInput(onSendMessage = {})
    }
}
```

#### 4.3 创建列表组件
**文件位置：** `presentation/ui/component/MessageList.kt`

```kotlin
package com.empathy.ai.presentation.ui.component

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.empathy.ai.domain.model.ChatMessage

/**
 * 消息列表组件
 * 
 * 功能：
 * 1. 显示消息列表
 * 2. 自动滚动到最新消息
 */
@Composable
fun MessageList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // 新消息到来时自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        items(
            items = messages,
            key = { message -> message.id }  // 使用唯一ID作为key
        ) { message ->
            MessageCard(message = message)
        }
    }
}
```

**规范要点：**
- ✅ 组件无状态，通过参数接收数据
- ✅ Modifier参数有默认值且放在最后
- ✅ 使用Material Design 3组件和主题
- ✅ 提供预览函数（`@Preview`）
- ✅ 组件职责单一，易于复用和测试

**Composable组件设计原则：**
```kotlin
// 原则1: 参数顺序
@Composable
fun MyComponent(
    // 1. 必需的数据参数
    data: String,
    // 2. 可选的配置参数
    enabled: Boolean = true,
    // 3. 回调函数
    onClick: () -> Unit = {},
    // 4. Modifier（永远最后）
    modifier: Modifier = Modifier
) { }

// 原则2: 状态管理
// ❌ 错误：在可复用组件中使用ViewModel
@Composable
fun MyComponent(viewModel: MyViewModel) { }

// ✅ 正确：接收数据和回调
@Composable
fun MyComponent(
    data: MyData,
    onEvent: (Event) -> Unit
) { }
```

---

## Step 5: 集成导航和主题

### 设计目标
将界面集成到应用的导航系统和主题系统中。

**相当于：** 后端配置路由和中间件。

### 开发步骤

#### 5.1 定义路由
**文件位置：** `presentation/ui/navigation/NavRoutes.kt`

```kotlin
package com.empathy.ai.presentation.ui.navigation

/**
 * 导航路由定义
 * 
 * 设计模式：
 * 1. 使用 sealed class 确保类型安全
 * 2. 提供 createRoute() 方法构建带参数的路由
 */
sealed class NavRoutes(val route: String) {
    data object Home : NavRoutes("home")
    
    data object ContactList : NavRoutes("contact_list")
    
    data object ChatScreen : NavRoutes("chat/{contactId}") {
        fun createRoute(contactId: String) = "chat/$contactId"
    }
    
    data object ContactDetail : NavRoutes("contact/{contactId}") {
        fun createRoute(contactId: String) = "contact/$contactId"
    }
    
    data object Settings : NavRoutes("settings")
}
```

#### 5.2 配置导航图
**文件位置：** `presentation/ui/navigation/NavGraph.kt`

```kotlin
package com.empathy.ai.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.empathy.ai.presentation.ui.screen.chat.ChatScreen
import com.empathy.ai.presentation.ui.screen.contact.ContactListScreen
import com.empathy.ai.presentation.ui.screen.home.HomeScreen

/**
 * 应用导航图
 * 
 * 职责：
 * 1. 定义所有路由和对应的Screen
 * 2. 配置路由参数
 * 3. 处理导航逻辑
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route,
        modifier = modifier
    ) {
        // 首页
        composable(NavRoutes.Home.route) {
            HomeScreen(
                onNavigateToContactList = {
                    navController.navigate(NavRoutes.ContactList.route)
                }
            )
        }
        
        // 联系人列表
        composable(NavRoutes.ContactList.route) {
            ContactListScreen(
                onNavigateToChat = { contactId ->
                    navController.navigate(NavRoutes.ChatScreen.createRoute(contactId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 聊天界面（带参数）
        composable(
            route = NavRoutes.ChatScreen.route,
            arguments = listOf(
                navArgument("contactId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ChatScreen(
                contactId = contactId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
```

#### 5.3 在MainActivity中集成
**文件位置：** `presentation/ui/MainActivity.kt`

```kotlin
package com.empathy.ai.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.empathy.ai.presentation.theme.GiveLoveTheme
import com.empathy.ai.presentation.ui.navigation.NavGraph
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用主Activity
 * 
 * 职责：
 * 1. 应用主题配置
 * 2. 导航系统初始化
 * 3. 依赖注入入口
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        enableEdgeToEdge()
        
        setContent {
            // 应用主题
            GiveLoveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 创建导航控制器
                    val navController = rememberNavController()
                    
                    // 设置导航图
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
```

**规范要点：**
- ✅ 使用 `sealed class` 定义路由，类型安全
- ✅ 路由参数使用 `navArgument` 配置
- ✅ 使用 `createRoute()` 方法构建带参数的路由
- ✅ MainActivity使用 `@AndroidEntryPoint` 启用依赖注入
- ✅ 使用 `enableEdgeToEdge()` 实现现代UI

---

## 总结：五步开发流程

### 开发顺序
```
Step 1: UiState & UiEvent (定义契约)
   ↓
Step 2: ViewModel (状态管理)
   ↓
Step 3: Screen (页面组装)
   ↓
Step 4: Components (可复用组件)
   ↓
Step 5: Navigation (路由集成)
```

### 文件组织结构
```
presentation/
├── ui/
│   ├── screen/
│   │   └── [feature]/
│   │       ├── [Feature]Screen.kt        // Step 3
│   │       ├── [Feature]UiState.kt       // Step 1
│   │       └── [Feature]UiEvent.kt       // Step 1
│   ├── component/
│   │   └── [Component].kt                // Step 4
│   ├── navigation/
│   │   ├── NavRoutes.kt                  // Step 5
│   │   └── NavGraph.kt                   // Step 5
│   └── MainActivity.kt                    // Step 5
├── viewmodel/
│   └── [Feature]ViewModel.kt             // Step 2
└── theme/
    ├── Theme.kt
    └── Type.kt
```

### 核心设计模式

#### 1. 单向数据流（UDF）
```
用户操作 → UiEvent → ViewModel → UseCase → Repository
                         ↓
                    UiState → UI重新渲染
```

#### 2. 状态提升（State Hoisting）
```kotlin
// 有状态组件（Screen）
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    ChatScreenContent(state, viewModel::onEvent)
}

// 无状态组件（Content）
@Composable
fun ChatScreenContent(
    state: UiState,
    onEvent: (UiEvent) -> Unit
) { /* 纯UI */ }
```

#### 3. 依赖注入
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val useCase: UseCase  // 
只注入UseCase
) : ViewModel()
```

### 关键检查清单

#### ✅ Step 1 检查清单
- [ ] UiState是data class，所有字段有默认值
- [ ] 包含isLoading和error字段
- [ ] UiEvent使用sealed interface定义
- [ ] 事件命名清晰，描述用户意图

#### ✅ Step 2 检查清单
- [ ] ViewModel使用@HiltViewModel注解
- [ ] 只注入UseCase，不注入Repository
- [ ] 使用StateFlow管理状态
- [ ] 统一的onEvent()方法
- [ ] 使用update{}更新状态
- [ ] 在viewModelScope中执行异步操作

#### ✅ Step 3 检查清单
- [ ] Screen函数注入ViewModel
- [ ] 使用collectAsState()收集状态
- [ ] 分离有状态和无状态Composable
- [ ] 使用LaunchedEffect处理副作用
- [ ] 通过回调传递事件

#### ✅ Step 4 检查清单
- [ ] 组件无状态，通过参数接收数据
- [ ] Modifier参数有默认值且在最后
- [ ] 使用Material Design 3组件
- [ ] 提供@Preview函数
- [ ] 组件职责单一

#### ✅ Step 5 检查清单
- [ ] 使用sealed class定义路由
- [ ] 配置navArgument
- [ ] MainActivity使用@AndroidEntryPoint
- [ ] 正确处理导航返回

---

## 常见问题与解决方案

### Q1: 何时使用LaunchedEffect？
**答：** 用于处理副作用，主要场景：
- 页面加载时获取数据
- 参数变化时重新加载
- 监听外部事件（如位置变化）

```kotlin
// 场景1: 页面加载
LaunchedEffect(Unit) {
    viewModel.onEvent(LoadData)
}

// 场景2: 参数变化
LaunchedEffect(userId) {
    viewModel.onEvent(LoadUser(userId))
}
```

### Q2: 如何避免内存泄漏？
**答：** 遵循以下原则：
1. 在viewModelScope中执行异步操作
2. 不在ViewModel中持有Context引用
3. 使用StateFlow而非LiveData
4. Composable正确使用remember和rememberSaveable

### Q3: 何时创建新的Composable组件？
**答：** 满足以下条件之一即可：
- 代码超过50行
- 相同UI模式出现2次以上
- 需要独立预览和测试
- 职责可以清晰分离

### Q4: 如何处理复杂的表单输入？
**答：** 使用FormState模式：
```kotlin
data class FormUiState(
    val name: String = "",
    val nameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val isValid: Boolean = false
)

sealed interface FormUiEvent {
    data class UpdateName(val value: String) : FormUiEvent
    data class UpdateEmail(val value: String) : FormUiEvent
    data object Submit : FormUiEvent
}
```

### Q5: 如何优化列表性能？
**答：** 关键优化点：
1. 使用key参数：`items(items, key = { it.id })`
2. 避免在item中使用重量级操作
3. 使用remember缓存计算结果
4. 考虑使用LazyColumn的contentType参数

---

## 最佳实践总结

### 1. 命名规范
```kotlin
// UiState: [Feature]UiState
data class ChatUiState(...)

// UiEvent: [Feature]UiEvent
sealed interface ChatUiEvent

// ViewModel: [Feature]ViewModel
class ChatViewModel : ViewModel()

// Screen: [Feature]Screen
@Composable fun ChatScreen(...)

// Component: 描述性名称
@Composable fun MessageCard(...)
```

### 2. 文件组织
```
同一功能的文件放在同一目录下：
screen/chat/
  ├── ChatScreen.kt
  ├── ChatUiState.kt
  └── ChatUiEvent.kt
```

### 3. 状态管理
```kotlin
// ✅ 推荐：使用StateFlow
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// ❌ 避免：直接暴露MutableStateFlow
val uiState: MutableStateFlow<UiState>

// ✅ 推荐：使用update更新
_uiState.update { it.copy(loading = true) }

// ❌ 避免：直接赋值
_uiState.value = _uiState.value.copy(loading = true)
```

### 4. 错误处理
```kotlin
// 统一的错误处理模式
private fun loadData() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        useCase()
            .onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, data = data) }
            }
            .onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "未知错误"
                    )
                }
            }
    }
}
```

### 5. 预览最佳实践
```kotlin
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MessageCardPreview() {
    GiveLoveTheme {
        MessageCard(
            message = ChatMessage(
                content = "Hello World",
                isUser = true
            )
        )
    }
}
```

---

## 参考资源

### 官方文档
- [Jetpack Compose官方文档](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Hilt依赖注入](https://developer.android.com/training/dependency-injection/hilt-android)

### 项目内部文档
- [UI层开发规范](./UI层开发规范.md)
- [UI层全局设计](./UI层全局设计.md)
- [数据层开发规范](../数据层/数据层开发规范.md)
- [服务层开发规范](../服务层/服务层开发规范.md)

---

## 下一步

完成本模块开发后，可以继续学习：
1. **第二模块**：复杂UI模式（底部导航、标签页、对话框）
2. **第三模块**：动画和转场效果
3. **第四模块**：性能优化和测试

---

*文档版本：v1.0*  
*最后更新：2024-12*  
*维护者：开发团队*