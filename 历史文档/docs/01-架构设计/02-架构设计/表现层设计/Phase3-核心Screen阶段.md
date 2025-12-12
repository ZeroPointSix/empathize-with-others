# Phase3: 核心Screen阶段

## 📋 阶段概览

**目标**: 实现3个核心业务Screen,完成UI层与ViewModel的完整对接

**预计工期**: 3-5天

**优先级**: P0 (必须完成)

**前置条件**:
- ✅ Phase1: 基础设施阶段已完成
- ✅ Phase2: 可复用组件阶段已完成
- ✅ ViewModel层100%完成
- ✅ UiState/UiEvent定义完成

**交付物**:
1. ContactListScreen.kt - 联系人列表界面
2. ContactDetailScreen.kt - 联系人详情界面
3. ChatScreen.kt - 聊天分析界面

---

## 一、Screen设计原则

### 1.1 核心原则

**单一职责**
- 每个Screen只负责一个主要业务功能
- UI逻辑与业务逻辑分离
- Screen只做展示和事件分发

**状态驱动**
- UI完全由UiState驱动
- 用户操作转化为UiEvent
- 单向数据流: State → UI → Event → ViewModel → State

**可测试性**
- Screen拆分为有状态和无状态两部分
- 无状态部分易于预览和测试
- ViewModel通过Hilt注入,可Mock测试

### 1.2 Screen架构模板

```kotlin
// 有状态入口 - 连接ViewModel
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // LaunchedEffect - 初始化操作
    LaunchedEffect(Unit) {
        viewModel.onEvent(FeatureUiEvent.Load)
    }
    
    // 无状态内容
    FeatureScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

// 无状态内容 - 纯UI展示
@Composable
private fun FeatureScreenContent(
    uiState: FeatureUiState,
    onEvent: (FeatureUiEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = { /* TopBar */ },
        floatingActionButton = { /* FAB */ }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.error != null -> ErrorDialog(...)
            else -> MainContent(uiState, onEvent)
        }
    }
}

@Preview
@Composable
private fun FeatureScreenPreview() {
    EmpathyTheme {
        FeatureScreenContent(
            uiState = FeatureUiState(),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
```

---

## 二、ContactListScreen - 联系人列表

### 2.1 功能需求

**核心功能**:
- 展示所有联系人列表
- 支持搜索联系人
- 点击跳转到联系人详情
- 长按可以删除联系人
- 浮动按钮添加新联系人

**与ViewModel对接**:
- ViewModel: [`ContactListViewModel.kt`](../../../app/src/main/java/com/empathy/ai/presentation/viewmodel/ContactListViewModel.kt)
- UiState: [`ContactListUiState.kt`](../../../app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/ContactListUiState.kt)
- UiEvent: [`ContactListUiEvent.kt`](../../../app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/ContactListUiEvent.kt)

### 2.2 完整实现

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/ContactListScreen.kt`

```kotlin
package com.empathy.ai.presentation.ui.screen.contact

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.presentation.ui.component.ContactCard
import com.empathy.ai.presentation.ui.component.ErrorDialog
import com.empathy.ai.presentation.ui.component.LoadingIndicator
import com.empathy.ai.presentation.viewmodel.ContactListViewModel

/**
 * 联系人列表Screen
 *
 * 功能:
 * - 展示所有联系人
 * - 搜索联系人
 * - 导航到详情页
 */
@Composable
fun ContactListScreen(
    viewModel: ContactListViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 初始化加载
    LaunchedEffect(Unit) {
        viewModel.onEvent(ContactListUiEvent.LoadContacts)
    }
    
    ContactListScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToChat = onNavigateToChat
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactListScreenContent(
    uiState: ContactListUiState,
    onEvent: (ContactListUiEvent) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("联系人") },
                actions = {
                    IconButton(onClick = { /* TODO: 搜索功能 */ }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToDetail("new") }
            ) {
                Icon(Icons.Default.Add, "添加联系人")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        message = "加载联系人...",
                        isFullScreen = true
                    )
                }
                uiState.error != null -> {
                    ErrorDialog(
                        message = uiState.error,
                        onDismiss = { onEvent(ContactListUiEvent.ClearError) },
                        onRetry = { onEvent(ContactListUiEvent.LoadContacts) }
                    )
                }
                uiState.contacts.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    ContactList(
                        contacts = uiState.contacts,
                        onContactClick = onNavigateToDetail,
                        onChatClick = onNavigateToChat
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactList(
    contacts: List<com.empathy.ai.domain.model.ContactProfile>,
    onContactClick: (String) -> Unit,
    onChatClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(contacts, key = { it.id }) { contact ->
            ContactCard(
                contact = contact,
                onClick = { onContactClick(contact.id.toString()) }
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(
                text = "还没有联系人",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击+按钮添加第一个联系人",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 2.3 关键实现点

**状态管理**:
```kotlin
// ViewModel中已实现
data class ContactListUiState(
    val contacts: List<ContactProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)
```

**事件处理**:
```kotlin
// ViewModel中已实现
sealed class ContactListUiEvent {
    object LoadContacts : ContactListUiEvent()
    data class SearchContacts(val query: String) : ContactListUiEvent()
    data class DeleteContact(val contactId: String) : ContactListUiEvent()
    object ClearError : ContactListUiEvent()
}
```

### 2.4 验证清单

- [ ] 正确展示联系人列表
- [ ] LoadingIndicator正常显示
- [ ] 错误对话框正常工作
- [ ] 空状态提示正确
- [ ] 导航功能正常
- [ ] 浮动按钮响应正确

---

## 三、ContactDetailScreen - 联系人详情

### 3.1 功能需求

**核心功能**:
- 查看联系人详细信息
- 编辑联系人信息
- 管理脑标签(添加/删除)
- 保存修改
- 返回列表

**与ViewModel对接**:
- ViewModel: [`ContactDetailViewModel.kt`](../../../app/src/main/java/com/empathy/ai/presentation/viewmodel/ContactDetailViewModel.kt)
- UiState: [`ContactDetailUiState.kt`](../../../app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/ContactDetailUiState.kt)
- UiEvent: [`ContactDetailUiEvent.kt`](../../../app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/ContactDetailUiEvent.kt)

### 3.2 关键代码框架

```kotlin
@Composable
fun ContactDetailScreen(
    contactId: String,
    viewModel: ContactDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(contactId) {
        if (contactId != "new") {
            viewModel.onEvent(ContactDetailUiEvent.LoadContact(contactId))
        }
    }
    
    // 监听保存成功,自动返回
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }
    
    ContactDetailScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun ContactDetailScreenContent(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("联系人详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onEvent(ContactDetailUiEvent.SaveContact) },
                        enabled = uiState.canSave
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 姓名输入
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onEvent(ContactDetailUiEvent.UpdateName(it)) },
                label = { Text("姓名") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 目标输入
            OutlinedTextField(
                value = uiState.targetGoal,
                onValueChange = { onEvent(ContactDetailUiEvent.UpdateTargetGoal(it)) },
                label = { Text("沟通目标") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 脑标签管理
            BrainTagsSection(
                tags = uiState.brainTags,
                onAddTag = { /* TODO */ },
                onDeleteTag = { tag ->
                    onEvent(ContactDetailUiEvent.DeleteBrainTag(tag.id.toString()))
                }
            )
        }
    }
}
```

### 3.3 验证清单

- [ ] 正确加载联系人信息
- [ ] 表单输入正常
- [ ] 保存功能正常
- [ ] 标签管理正常
- [ ] 返回导航正常
- [ ] Loading和Error状态处理正确

---

## 四、ChatScreen - 聊天分析

### 4.1 功能需求

**核心功能**:
- 显示聊天消息列表
- 输入新消息
- 分析聊天内容(调用AI)
- 检查草稿安全性
- 显示分析结果

**与ViewModel对接**:
- ViewModel: [`ChatViewModel.kt`](../../../app/src/main/java/com/empathy/ai/presentation/viewmodel/ChatViewModel.kt)
- UiState: [`ChatUiState.kt`](../../../app/src/main/java/com/empathy/ai/presentation/ui/screen/chat/ChatUiState.kt)
- UiEvent: [`ChatUiEvent.kt`](../../../app/src/main/java/com/empathy/ai/presentation/ui/screen/chat/ChatUiEvent.kt)

### 4.2 关键代码框架

```kotlin
@Composable
fun ChatScreen(
    contactId: String,
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(contactId) {
        viewModel.onEvent(ChatUiEvent.LoadChat(contactId))
    }
    
    ChatScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    onEvent: (ChatUiEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.contactProfile?.name ?: "聊天") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEvent(ChatUiEvent.AnalyzeChat) },
                        enabled = uiState.hasMessages && !uiState.isAnalyzing
                    ) {
                        Icon(Icons.Default.Psychology, "分析")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 消息列表
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message = message)
                }
            }
            
            // 输入区域
            MessageInputSection(
                inputText = uiState.inputText,
                onInputChange = { onEvent(ChatUiEvent.UpdateInputText(it)) },
                onSend = { onEvent(ChatUiEvent.SendMessage(uiState.inputText)) },
                canSend = uiState.canSendMessage,
                showSafetyWarning = uiState.shouldShowSafetyWarning
            )
        }
        
        // 分析结果对话框
        if (uiState.showAnalysisDialog && uiState.analysisResult != null) {
            AnalysisResultDialog(
                result = uiState.analysisResult,
                onDismiss = { onEvent(ChatUiEvent.DismissAnalysisDialog) },
                onApplySuggestion = { suggestion ->
                    onEvent(ChatUiEvent.ApplySuggestion(suggestion))
                }
            )
        }
        
        // Loading覆盖层
        if (uiState.isAnalyzing) {
            LoadingIndicator(
                message = "正在分析聊天内容...",
                isFullScreen = true
            )
        }
    }
}

@Composable
private fun MessageInputSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean,
    showSafetyWarning: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        if (showSafetyWarning) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ 此消息可能不太合适",
                    modifier = Modifier.padding(8.dp),
                    style = 