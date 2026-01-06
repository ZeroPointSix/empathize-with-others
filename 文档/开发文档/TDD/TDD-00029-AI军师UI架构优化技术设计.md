# TDD-00029: AI军师UI架构优化技术设计

## 1. 文档信息

| 项目 | 内容 |
|------|------|
| 文档类型 | TDD (Technical Design Document) |
| 文档编号 | TDD-00029 |
| 功能名称 | AI军师UI架构优化技术设计 |
| 版本 | 1.1 |
| 创建日期 | 2026-01-06 |
| 最后更新 | 2026-01-06 |
| 作者 | Kiro |
| 审核人 | - |
| 审核状态 | 🔄 待审核 |
| 关联文档 | PRD-00029, DR-00029, TDD-00026 |

### 1.1 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0 | 2026-01-06 | Kiro | 初始版本 |
| 1.1 | 2026-01-06 | Kiro | 根据DR-00029审查报告补充：Repository接口设计、UseCase设计、DI模块集成、字符串资源配置章节 |

### 1.2 参考标准

| 标准文档 | 版本 | 说明 |
|---------|------|------|
| Clean Architecture | - | 架构模式标准 |
| MVVM Pattern | - | UI架构模式 |
| Kotlin Coding Conventions | 2.0.21 | 代码规范 |
| Material Design 3 | 1.3.1 | UI设计规范 |
| iOS Human Interface Guidelines | - | iOS风格参考 |

### 1.3 技术债务评估

| 债务ID | 描述 | 影响 | 优先级 | 计划解决时间 |
|--------|------|------|--------|-------------|
| TD-029-01 | 会话搜索功能延后实现 | 低 | 🟢 低 | v1.1 |
| TD-029-02 | 联系人搜索功能延后实现 | 低 | 🟢 低 | v1.1 |
| TD-029-03 | 左滑删除会话功能延后实现 | 低 | 🟢 低 | v1.1 |

---

## 2. 架构概述

### 2.1 架构目标

AI军师UI架构优化采用 Clean Architecture 分层架构，实现三页面独立导航体系。

**核心目标**：
- 重构AI军师UI为三个独立全屏页面：对话界面、会话历史页面、联系人选择页面
- 实现自动恢复上次联系人功能，提升用户体验
- 严格遵循UI原型文件实现iOS风格界面
- 保持与现有TDD-00026架构的兼容性

### 2.2 技术栈

| 技术领域 | 技术选择 | 版本 | 用途 |
|---------|----------|------|------|
| UI框架 | Jetpack Compose | BOM 2024.12.01 | 声明式UI |
| 组件库 | Material 3 | 1.3.1 | UI组件 |
| 导航 | Navigation Compose | 2.8.5 | 页面导航 |
| 依赖注入 | Hilt | 2.52 | 依赖管理 |
| 状态管理 | StateFlow | 1.9.0 | UI状态 |
| 安全存储 | EncryptedSharedPreferences | 1.1.0-alpha06 | 偏好设置 |


### 2.3 设计原则

- **单一职责**：每个页面只负责一种功能
- **开闭原则**：通过扩展支持新功能，不修改现有代码
- **依赖倒置**：ViewModel依赖UseCase接口，不依赖具体实现
- **状态不可变**：UiState使用data class，通过copy更新状态
- **响应式数据流**：使用Flow实现数据的响应式更新

---

## 3. 整体架构设计

### 3.1 三页面架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     用户点击"AI军师"Tab                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  AiAdvisorScreen (入口页面)                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 检查 AiAdvisorPreferences.lastContactId             │   │
│  │ ├── 有值 → 导航到 AiAdvisorChatScreen(contactId)    │   │
│  │ └── 无值 → 导航到 ContactSelectScreen               │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ AiAdvisorChat    │ │ SessionHistory   │ │ ContactSelect    │
│ Screen           │ │ Screen           │ │ Screen           │
│ (对话界面)       │ │ (会话历史页面)   │ │ (联系人选择页面) │
├──────────────────┤ ├──────────────────┤ ├──────────────────┤
│ [☰]  AI军师 [👤] │ │ [<] 会话历史     │ │ [<] 选择联系人   │
│                  │ │                  │ │                  │
│ 对话消息列表     │ │ 会话列表         │ │ 联系人列表       │
│                  │ │                  │ │                  │
│ [输入框]         │ │                  │ │                  │
└──────────────────┘ └──────────────────┘ └──────────────────┘
        │                    │                    │
        │ ☰点击              │ 点击会话           │ 点击联系人
        └────────────────────┘                    │
                                                  │
                                                  ▼
                                    ┌──────────────────────┐
                                    │ 保存lastContactId    │
                                    │ 创建新会话           │
                                    │ 返回对话界面         │
                                    └──────────────────────┘
```

### 3.2 分层架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   :presentation 模块                  │  │
│  │  ┌────────────────┐ ┌────────────────────────────┐  │  │
│  │  │AiAdvisorScreen │ │AiAdvisorChatScreen         │  │  │
│  │  │  (入口页面)    │ │  (对话界面-修改)           │  │  │
│  │  └────────────────┘ └────────────────────────────┘  │  │
│  │  ┌────────────────┐ ┌────────────────────────────┐  │  │
│  │  │SessionHistory  │ │ContactSelectScreen         │  │  │
│  │  │  Screen(新增)  │ │  (新增)                    │  │  │
│  │  └────────────────┘ └────────────────────────────┘  │  │
│  │  ┌────────────────┐ ┌────────────────────────────┐  │  │
│  │  │SessionHistory  │ │ContactSelectViewModel      │  │  │
│  │  │  ViewModel(新) │ │  (新增)                    │  │  │
│  │  └────────────────┘ └────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                    :domain 模块                       │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │  │
│  │  │GetSessions   │ │GetAllContacts│ │CreateSession │  │  │
│  │  │ByContactId   │ │  UseCase     │ │  UseCase     │  │  │
│  │  │  UseCase     │ │  (确认存在)  │ │  (确认存在)  │  │  │
│  │  └──────────────┘ └──────────────┘ └──────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                     :data 模块                        │  │
│  │  ┌──────────────────────────────────────────────────┐│  │
│  │  │AiAdvisorPreferences (新增)                       ││  │
│  │  │  - lastContactId: String                         ││  │
│  │  │  - lastSessionId: String?                        ││  │
│  │  └──────────────────────────────────────────────────┘│  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```


### 3.3 数据流图

```
┌─────────────────────────────────────────────────────────────┐
│                     页面导航数据流                           │
└─────────────────────────────────────────────────────────────┘

  用户点击AI军师Tab
         │
         ▼
  ┌──────────────┐
  │ AiAdvisor    │ ← 入口页面
  │   Screen     │
  └──────────────┘
         │
         ▼
  ┌──────────────┐
  │ AiAdvisor    │ ← 读取偏好设置
  │ Preferences  │
  └──────────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐ ┌────────┐
│有联系人│ │无联系人│
│记录    │ │记录    │
└────────┘ └────────┘
    │         │
    ▼         ▼
┌────────┐ ┌────────┐
│导航到  │ │导航到  │
│对话界面│ │联系人  │
│+新会话 │ │选择页面│
└────────┘ └────────┘
```

### 3.4 模块划分

```
:presentation/src/main/kotlin/com/empathy/ai/presentation/
├── ui/screen/advisor/
│   ├── AiAdvisorScreen.kt            # 入口页面（修改）
│   ├── AiAdvisorChatScreen.kt        # 对话界面（修改）
│   ├── SessionHistoryScreen.kt       # 会话历史页面（新增）
│   ├── ContactSelectScreen.kt        # 联系人选择页面（新增）
│   └── component/
│       ├── SessionListItem.kt        # 会话列表项（新增）
│       └── ContactListItem.kt        # 联系人列表项（新增）
├── viewmodel/
│   ├── AiAdvisorChatViewModel.kt     # 对话ViewModel（修改）
│   ├── SessionHistoryViewModel.kt    # 会话历史ViewModel（新增）
│   └── ContactSelectViewModel.kt     # 联系人选择ViewModel（新增）
└── navigation/
    ├── NavGraph.kt                   # 导航图（修改）
    └── NavRoutes.kt                  # 路由常量（修改）

:data/src/main/kotlin/com/empathy/ai/data/
└── local/
    └── AiAdvisorPreferences.kt       # AI军师偏好设置（新增）
```

---

## 4. 详细技术设计

### 4.1 路由定义

#### 4.1.1 NavRoutes.kt 修改

**文件位置**：`:presentation/navigation/NavRoutes.kt`

```kotlin
object NavRoutes {
    // ... 现有路由 ...

    /**
     * AI军师会话历史页面
     * PRD-00029: 新增会话历史路由
     * 参数: contactId (String) - 联系人ID
     */
    const val AI_ADVISOR_SESSIONS = "ai_advisor_sessions/{contactId}"
    const val AI_ADVISOR_SESSIONS_ARG_ID = "contactId"

    /**
     * AI军师联系人选择页面
     * PRD-00029: 新增联系人选择路由
     */
    const val AI_ADVISOR_CONTACTS = "ai_advisor_contacts"

    /**
     * 创建AI军师会话历史路由
     * PRD-00029: 新增辅助函数
     */
    fun aiAdvisorSessions(contactId: String): String {
        return "ai_advisor_sessions/$contactId"
    }
}
```

#### 4.1.2 NavGraph.kt 修改

**文件位置**：`:presentation/navigation/NavGraph.kt`

```kotlin
// 在NavHost中新增以下路由配置

// AI军师会话历史页面
composable(
    route = NavRoutes.AI_ADVISOR_SESSIONS,
    arguments = listOf(
        navArgument(NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID) {
            type = NavType.StringType
        }
    )
) { backStackEntry ->
    val contactId = backStackEntry.arguments
        ?.getString(NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID) ?: ""
    SessionHistoryScreen(
        contactId = contactId,
        onNavigateBack = { navController.navigateUp() },
        onNavigateToChat = { sessionId ->
            // 加载指定会话并返回对话界面
            navController.navigate(NavRoutes.aiAdvisorChat(contactId)) {
                popUpTo(NavRoutes.AI_ADVISOR_SESSIONS) { inclusive = true }
            }
        },
        onCreateNewSession = {
            // 创建新会话并返回对话界面
            navController.navigate(NavRoutes.aiAdvisorChat(contactId)) {
                popUpTo(NavRoutes.AI_ADVISOR_SESSIONS) { inclusive = true }
            }
        }
    )
}

// AI军师联系人选择页面
composable(route = NavRoutes.AI_ADVISOR_CONTACTS) {
    ContactSelectScreen(
        onNavigateBack = { navController.navigateUp() },
        onSelectContact = { contactId ->
            // 保存联系人ID并导航到对话界面
            navController.navigate(NavRoutes.aiAdvisorChat(contactId)) {
                popUpTo(NavRoutes.AI_ADVISOR) { inclusive = true }
            }
        }
    )
}
```


### 4.2 数据存储设计

#### 4.2.1 AiAdvisorPreferences

**文件位置**：`:data/local/AiAdvisorPreferences.kt`

```kotlin
package com.empathy.ai.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI军师偏好设置存储
 *
 * 使用EncryptedSharedPreferences加密存储用户偏好设置
 * PRD-00029: 新增文件
 */
@Singleton
class AiAdvisorPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "ai_advisor_preferences"
        private const val KEY_LAST_CONTACT_ID = "last_contact_id"
        private const val KEY_LAST_SESSION_ID = "last_session_id"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * 获取上次使用的联系人ID
     * @return 联系人ID，如果不存在返回null
     */
    fun getLastContactId(): String? {
        return prefs.getString(KEY_LAST_CONTACT_ID, null)
    }

    /**
     * 保存上次使用的联系人ID
     * @param contactId 联系人ID
     */
    fun setLastContactId(contactId: String) {
        prefs.edit().putString(KEY_LAST_CONTACT_ID, contactId).apply()
    }

    /**
     * 获取上次使用的会话ID
     * @return 会话ID，如果不存在返回null
     */
    fun getLastSessionId(): String? {
        return prefs.getString(KEY_LAST_SESSION_ID, null)
    }

    /**
     * 保存上次使用的会话ID
     * @param sessionId 会话ID
     */
    fun setLastSessionId(sessionId: String?) {
        if (sessionId != null) {
            prefs.edit().putString(KEY_LAST_SESSION_ID, sessionId).apply()
        } else {
            prefs.edit().remove(KEY_LAST_SESSION_ID).apply()
        }
    }

    /**
     * 清除所有偏好设置
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
```

### 4.3 ViewModel设计

#### 4.3.1 SessionHistoryViewModel

**文件位置**：`:presentation/viewmodel/SessionHistoryViewModel.kt`

```kotlin
package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.presentation.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 会话历史页面ViewModel
 *
 * PRD-00029: 新增文件
 */
@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val contactRepository: ContactRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val contactId: String = savedStateHandle[NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID] ?: ""

    private val _uiState = MutableStateFlow(SessionHistoryUiState())
    val uiState: StateFlow<SessionHistoryUiState> = _uiState.asStateFlow()

    init {
        loadContactInfo()
        loadSessions()
    }

    /**
     * 加载联系人信息
     */
    private fun loadContactInfo() {
        viewModelScope.launch {
            contactRepository.getProfileById(contactId)
                .onSuccess { contact ->
                    _uiState.update { it.copy(contactName = contact?.name ?: "未知联系人") }
                }
                .onFailure {
                    _uiState.update { it.copy(contactName = "未知联系人") }
                }
        }
    }

    /**
     * 加载会话列表
     */
    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            aiAdvisorRepository.getSessions(contactId)
                .onSuccess { sessions ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessions = sessions,
                            isEmpty = sessions.isEmpty()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    /**
     * 删除会话
     */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            aiAdvisorRepository.deleteSession(sessionId)
                .onSuccess {
                    loadSessions() // 重新加载列表
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 会话历史UI状态
 */
data class SessionHistoryUiState(
    val isLoading: Boolean = false,
    val contactName: String = "",
    val sessions: List<AiAdvisorSession> = emptyList(),
    val isEmpty: Boolean = false,
    val error: String? = null
)
```


#### 4.3.2 ContactSelectViewModel

**文件位置**：`:presentation/viewmodel/ContactSelectViewModel.kt`

```kotlin
package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.data.local.AiAdvisorPreferences
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 联系人选择页面ViewModel
 *
 * PRD-00029: 新增文件
 */
@HiltViewModel
class ContactSelectViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val aiAdvisorPreferences: AiAdvisorPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactSelectUiState())
    val uiState: StateFlow<ContactSelectUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    /**
     * 加载联系人列表
     */
    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            contactRepository.getAllProfiles()
                .collect { contacts ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            contacts = contacts,
                            isEmpty = contacts.isEmpty()
                        )
                    }
                }
        }
    }

    /**
     * 选择联系人
     * 保存联系人ID到偏好设置
     */
    fun selectContact(contactId: String) {
        aiAdvisorPreferences.setLastContactId(contactId)
        _uiState.update { it.copy(selectedContactId = contactId) }
    }

    /**
     * 搜索联系人
     */
    fun searchContacts(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // TODO: 实现搜索过滤逻辑（TD-029-02）
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 联系人选择UI状态
 */
data class ContactSelectUiState(
    val isLoading: Boolean = false,
    val contacts: List<ContactProfile> = emptyList(),
    val isEmpty: Boolean = false,
    val searchQuery: String = "",
    val selectedContactId: String? = null,
    val error: String? = null
)
```

#### 4.3.3 AiAdvisorChatViewModel 修改

**文件位置**：`:presentation/viewmodel/AiAdvisorChatViewModel.kt`

需要修改的部分：

```kotlin
// 在现有ViewModel中添加以下功能

/**
 * 注入AiAdvisorPreferences
 */
@HiltViewModel
class AiAdvisorChatViewModel @Inject constructor(
    // ... 现有依赖 ...
    private val aiAdvisorPreferences: AiAdvisorPreferences, // 新增
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ... 现有代码 ...

    /**
     * 初始化时保存当前联系人ID
     */
    init {
        // 保存当前联系人ID到偏好设置
        aiAdvisorPreferences.setLastContactId(contactId)
        
        // 创建新会话（如果需要）
        createNewSessionIfNeeded()
        
        // ... 现有初始化代码 ...
    }

    /**
     * 创建新会话（如果需要）
     * PRD-00029: 进入对话界面时自动创建新会话
     */
    private fun createNewSessionIfNeeded() {
        viewModelScope.launch {
            // 检查是否需要创建新会话
            val lastSessionId = aiAdvisorPreferences.getLastSessionId()
            if (lastSessionId == null || shouldCreateNewSession) {
                aiAdvisorRepository.createSession(contactId)
                    .onSuccess { session ->
                        aiAdvisorPreferences.setLastSessionId(session.id)
                        _uiState.update { it.copy(currentSessionId = session.id) }
                    }
            }
        }
    }

    /**
     * 导航到会话历史页面
     */
    fun navigateToSessionHistory() {
        _uiState.update { it.copy(navigateToSessionHistory = true) }
    }

    /**
     * 导航到联系人选择页面
     */
    fun navigateToContactSelect() {
        _uiState.update { it.copy(navigateToContactSelect = true) }
    }

    /**
     * 重置导航状态
     */
    fun resetNavigationState() {
        _uiState.update {
            it.copy(
                navigateToSessionHistory = false,
                navigateToContactSelect = false
            )
        }
    }
}
```

### 4.4 Screen组件设计

#### 4.4.1 SessionHistoryScreen

**文件位置**：`:presentation/ui/screen/advisor/SessionHistoryScreen.kt`

```kotlin
package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.presentation.viewmodel.SessionHistoryViewModel

/**
 * 会话历史页面
 *
 * 🔴 必须参考原型: 文档/开发文档/UI-原型/PRD29/ai-advisor-home-ios.html
 *
 * PRD-00029: 新增文件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    contactId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (sessionId: String) -> Unit,
    onCreateNewSession: () -> Unit,
    viewModel: SessionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // iOS风格背景色
    val iosBackground = Color(0xFFF2F2F7)
    val iosBlue = Color(0xFF007AFF)

    Scaffold(
        containerColor = iosBackground,
        topBar = {
            // iOS风格导航栏
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "会话历史",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = iosBlue
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onCreateNewSession) {
                        Text(
                            text = "新建",
                            color = iosBlue,
                            fontSize = 17.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = iosBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 分组标题
            Text(
                text = "与 ${uiState.contactName} 的对话",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.Normal
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iosBlue)
                    }
                }
                uiState.isEmpty -> {
                    EmptySessionsView(onCreateNewSession = onCreateNewSession)
                }
                else -> {
                    SessionList(
                        sessions = uiState.sessions,
                        onSessionClick = onNavigateToChat
                    )
                }
            }
        }
    }
}

/**
 * 会话列表
 */
@Composable
private fun SessionList(
    sessions: List<AiAdvisorSession>,
    onSessionClick: (sessionId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(sessions) { session ->
            SessionListItem(
                session = session,
                onClick = { onSessionClick(session.id) }
            )
        }
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptySessionsView(onCreateNewSession: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无历史会话",
                fontSize = 17.sp,
                color = Color(0xFF8E8E93)
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCreateNewSession) {
                Text(
                    text = "发起新对话",
                    color = Color(0xFF007AFF),
                    fontSize = 17.sp
                )
            }
        }
    }
}
```


#### 4.4.2 ContactSelectScreen

**文件位置**：`:presentation/ui/screen/advisor/ContactSelectScreen.kt`

```kotlin
package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.viewmodel.ContactSelectViewModel

/**
 * 联系人选择页面
 *
 * 🔴 必须参考原型: 文档/开发文档/UI-原型/PRD29/ai-advisor-home-ios.html
 *
 * PRD-00029: 新增文件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectScreen(
    onNavigateBack: () -> Unit,
    onSelectContact: (contactId: String) -> Unit,
    viewModel: ContactSelectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 监听选择状态，触发导航
    LaunchedEffect(uiState.selectedContactId) {
        uiState.selectedContactId?.let { contactId ->
            onSelectContact(contactId)
        }
    }

    // iOS风格颜色
    val iosBackground = Color(0xFFF2F2F7)
    val iosBlue = Color(0xFF007AFF)
    val iosGray = Color(0xFF8E8E93)

    Scaffold(
        containerColor = iosBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "选择联系人",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = iosBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = iosBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索框（P2优先级，暂时显示但不实现功能）
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.searchContacts(it) },
                placeholder = "搜索联系人"
            )

            // 分组标题
            Text(
                text = "联系人",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = iosGray,
                fontWeight = FontWeight.Normal
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iosBlue)
                    }
                }
                uiState.isEmpty -> {
                    EmptyContactsView()
                }
                else -> {
                    ContactList(
                        contacts = uiState.contacts,
                        onContactClick = { viewModel.selectContact(it.id) }
                    )
                }
            }
        }
    }
}

/**
 * 搜索框组件
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    val iosSearchBackground = Color(0x1F8E8E93) // rgba(142, 142, 147, 0.12)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFF8E8E93),
                    fontSize = 15.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(iosSearchBackground),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = iosSearchBackground,
                unfocusedContainerColor = iosSearchBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}

/**
 * 联系人列表
 */
@Composable
private fun ContactList(
    contacts: List<ContactProfile>,
    onContactClick: (ContactProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(contacts) { contact ->
            ContactListItem(
                contact = contact,
                onClick = { onContactClick(contact) }
            )
        }
    }
}

/**
 * 联系人列表项
 * 
 * 🔴 必须参考原型样式：
 * - 头像: 44dp方形圆角，彩色背景+姓氏首字
 * - 姓名: 15sp, 黑色
 * - 关系标签: 11sp, 灰色
 * - 时间: 11sp, 灰色
 * - 消息预览: 13sp, 灰色, 单行截断
 */
@Composable
private fun ContactListItem(
    contact: ContactProfile,
    onClick: () -> Unit
) {
    val avatarColors = listOf(
        Color(0xFFE8EAF6) to Color(0xFF5C6BC0), // indigo
        Color(0xFFE3F2FD) to Color(0xFF42A5F5), // blue
        Color(0xFFFCE4EC) to Color(0xFFEC407A), // rose
        Color(0xFFE8F5E9) to Color(0xFF66BB6A), // emerald
        Color(0xFFE0F7FA) to Color(0xFF26C6DA)  // cyan
    )
    
    // 根据联系人ID选择颜色
    val colorIndex = contact.id.hashCode().let { Math.abs(it) % avatarColors.size }
    val (bgColor, textColor) = avatarColors[colorIndex]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()?.toString() ?: "?",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 信息区域
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1C1C1E)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = contact.relationship?.displayName ?: "普通",
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
                Text(
                    text = formatRelativeTime(contact.updatedAt),
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 最后消息预览
            Text(
                text = contact.notes ?: "暂无消息",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    
    // 分隔线
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = Color(0xFFE5E5EA),
        thickness = 0.5.dp
    )
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyContactsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无联系人",
                fontSize = 17.sp,
                color = Color(0xFF8E8E93)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请先添加联系人",
                fontSize = 15.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

/**
 * 格式化相对时间
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> {
            val hours = diff / (60 * 60 * 1000)
            if (hours < 12) "${hours}小时前" else "今天"
        }
        diff < 2 * 24 * 60 * 60 * 1000 -> "昨天"
        diff < 7 * 24 * 60 * 60 * 1000 -> {
            val days = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
            days[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        }
        else -> {
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
            "${calendar.get(java.util.Calendar.MONTH) + 1}/${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
        }
    }
}
```


#### 4.4.3 AiAdvisorChatScreen 修改

**文件位置**：`:presentation/ui/screen/advisor/AiAdvisorChatScreen.kt`

需要修改的部分（导航栏）：

```kotlin
/**
 * 对话界面导航栏
 *
 * 🔴 必须参考原型: 文档/开发文档/UI-原型/PRD29/gemini对话界面.html
 *
 * 修改内容：
 * - 左侧：☰ 菜单图标 → 点击进入会话历史页面
 * - 中间：标题 "AI 军师"
 * - 右侧：👤 联系人图标 → 点击进入联系人选择页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvisorTopBar(
    onMenuClick: () -> Unit,
    onContactClick: () -> Unit
) {
    val iosBackground = Color(0xFFF2F2F7)
    val iosBlue = Color(0xFF007AFF)

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "AI 军师",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "会话历史",
                    tint = iosBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        actions = {
            IconButton(onClick = onContactClick) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "选择联系人",
                    tint = iosBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = iosBackground
        )
    )
}
```

#### 4.4.4 AiAdvisorScreen 修改

**文件位置**：`:presentation/ui/screen/advisor/AiAdvisorScreen.kt`

修改为入口页面，负责路由分发：

```kotlin
package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.presentation.viewmodel.AiAdvisorViewModel

/**
 * AI军师入口页面
 *
 * 负责检查偏好设置并路由到正确的页面：
 * - 有上次联系人记录 → 导航到对话界面
 * - 无上次联系人记录 → 导航到联系人选择页面
 *
 * PRD-00029: 修改为入口路由页面
 */
@Composable
fun AiAdvisorScreen(
    onNavigateToChat: (contactId: String) -> Unit,
    onNavigateToContactSelect: () -> Unit,
    viewModel: AiAdvisorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 根据偏好设置决定导航目标
    LaunchedEffect(uiState.navigationTarget) {
        when (val target = uiState.navigationTarget) {
            is NavigationTarget.Chat -> {
                onNavigateToChat(target.contactId)
                viewModel.resetNavigationState()
            }
            is NavigationTarget.ContactSelect -> {
                onNavigateToContactSelect()
                viewModel.resetNavigationState()
            }
            null -> {
                // 等待加载完成
            }
        }
    }

    // 显示加载状态（可选）
    if (uiState.isLoading) {
        // 可以显示一个简单的加载指示器
        // 或者保持空白，因为导航会很快发生
    }
}

/**
 * 导航目标
 */
sealed class NavigationTarget {
    data class Chat(val contactId: String) : NavigationTarget()
    object ContactSelect : NavigationTarget()
}
```

---

## 5. Repository 接口设计

### 5.1 现有Repository复用

本次UI优化主要复用现有的Repository接口，无需新增Repository。

| Repository | 模块 | 复用方法 | 说明 |
|------------|------|----------|------|
| `AiAdvisorRepository` | :domain | `getSessions(contactId)` | 获取联系人的会话列表 |
| `AiAdvisorRepository` | :domain | `createSession(contactId)` | 创建新会话 |
| `AiAdvisorRepository` | :domain | `deleteSession(sessionId)` | 删除会话 |
| `ContactRepository` | :domain | `getAllProfiles()` | 获取所有联系人 |
| `ContactRepository` | :domain | `getProfileById(contactId)` | 获取联系人详情 |

### 5.2 AiAdvisorRepository 接口确认

**文件位置**：`:domain/repository/AiAdvisorRepository.kt`

需要确认以下方法存在：

```kotlin
interface AiAdvisorRepository {
    /**
     * 获取联系人的所有会话
     * @param contactId 联系人ID
     * @return 会话列表，按更新时间倒序
     */
    suspend fun getSessions(contactId: String): Result<List<AiAdvisorSession>>
    
    /**
     * 创建新会话
     * @param contactId 联系人ID
     * @param title 会话标题（可选）
     * @return 新创建的会话
     */
    suspend fun createSession(contactId: String, title: String = "新对话"): Result<AiAdvisorSession>
    
    /**
     * 删除会话
     * @param sessionId 会话ID
     */
    suspend fun deleteSession(sessionId: String): Result<Unit>
    
    // ... 其他现有方法 ...
}
```

### 5.3 ContactRepository 接口确认

**文件位置**：`:domain/repository/ContactRepository.kt`

需要确认以下方法存在：

```kotlin
interface ContactRepository {
    /**
     * 获取所有联系人
     * @return 联系人列表Flow
     */
    fun getAllProfiles(): Flow<List<ContactProfile>>
    
    /**
     * 根据ID获取联系人
     * @param contactId 联系人ID
     * @return 联系人详情
     */
    suspend fun getProfileById(contactId: String): Result<ContactProfile?>
    
    // ... 其他现有方法 ...
}
```

---

## 6. UseCase 设计

### 6.1 现有UseCase复用

本次UI优化主要复用现有的UseCase，无需新增UseCase。

| UseCase | 模块 | 说明 | 调用方 |
|---------|------|------|--------|
| `GetAdvisorSessionsUseCase` | :domain | 获取会话列表 | SessionHistoryViewModel |
| `CreateAdvisorSessionUseCase` | :domain | 创建新会话 | AiAdvisorChatViewModel |
| `DeleteAdvisorSessionUseCase` | :domain | 删除会话 | SessionHistoryViewModel |

### 6.2 UseCase 调用示例

**SessionHistoryViewModel 中的UseCase调用**：

```kotlin
@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val getAdvisorSessionsUseCase: GetAdvisorSessionsUseCase,
    private val deleteAdvisorSessionUseCase: DeleteAdvisorSessionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val contactId: String = savedStateHandle[NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID] ?: ""

    fun loadSessions() {
        viewModelScope.launch {
            getAdvisorSessionsUseCase(contactId)
                .onSuccess { sessions ->
                    _uiState.update { it.copy(sessions = sessions) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            deleteAdvisorSessionUseCase(sessionId)
                .onSuccess { loadSessions() }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }
}
```

### 6.3 备选方案：直接调用Repository

如果现有UseCase不满足需求，ViewModel可以直接注入Repository：

```kotlin
@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val contactRepository: ContactRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    // 直接调用Repository方法
}
```

---

## 7. DI 模块集成

### 7.1 现有DI模块复用

本次UI优化无需新增DI模块，复用现有模块配置。

| DI模块 | 所在模块 | 提供的依赖 |
|--------|----------|-----------|
| `DatabaseModule` | :data | AiAdvisorDao |
| `RepositoryModule` | :data | AiAdvisorRepository, ContactRepository |
| `AiAdvisorModule` | :app | AiAdvisor相关UseCase |

### 7.2 AiAdvisorPreferences 注入配置

新增的 `AiAdvisorPreferences` 类使用 `@Singleton` 和 `@Inject` 注解，Hilt会自动处理注入：

```kotlin
@Singleton
class AiAdvisorPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ... 实现 ...
}
```

**无需额外配置**，Hilt会自动将其注册为单例。

### 7.3 ViewModel 注入配置

所有新增的ViewModel使用 `@HiltViewModel` 注解，Hilt会自动处理注入：

```kotlin
@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val contactRepository: ContactRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel()

@HiltViewModel
class ContactSelectViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val aiAdvisorPreferences: AiAdvisorPreferences
) : ViewModel()
```

### 7.4 依赖关系图

```
┌─────────────────────────────────────────────────────────────┐
│                        :app 模块                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ AiAdvisorModule                                      │   │
│  │   - GetAdvisorSessionsUseCase                       │   │
│  │   - CreateAdvisorSessionUseCase                     │   │
│  │   - DeleteAdvisorSessionUseCase                     │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       :data 模块                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ RepositoryModule                                     │   │
│  │   - AiAdvisorRepository → AiAdvisorRepositoryImpl   │   │
│  │   - ContactRepository → ContactRepositoryImpl       │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ DatabaseModule                                       │   │
│  │   - AiAdvisorDao                                    │   │
│  │   - ContactDao                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ AiAdvisorPreferences (自动注入)                      │   │
│  │   - @Singleton @Inject constructor                  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. 字符串资源配置

### 8.1 新增字符串资源

**文件位置**：`:presentation/src/main/res/values/strings.xml`

```xml
<!-- AI军师UI优化 - PRD-00029 -->
<string name="ai_advisor_title">AI 军师</string>
<string name="session_history_title">会话历史</string>
<string name="select_contact_title">选择联系人</string>
<string name="new_session">新建</string>
<string name="search_sessions">搜索会话</string>
<string name="search_contacts">搜索联系人</string>
<string name="empty_sessions_hint">暂无历史会话</string>
<string name="empty_contacts_hint">暂无联系人</string>
<string name="empty_contacts_add_hint">请先添加联系人</string>
<string name="start_new_conversation">发起新对话</string>
<string name="conversation_with">与 %s 的对话</string>
<string name="welcome_title">共情</string>
<string name="welcome_subtitle">懂你所想，助你表达</string>
<string name="input_hint">输入消息或粘贴聊天记录...</string>
<string name="content_desc_menu">会话历史</string>
<string name="content_desc_contact">选择联系人</string>
<string name="content_desc_back">返回</string>
<string name="relationship_intimate">亲密</string>
<string name="relationship_familiar">熟悉</string>
<string name="relationship_normal">普通</string>
<string name="relationship_new">新认识</string>
<string name="time_just_now">刚刚</string>
<string name="time_minutes_ago">%d分钟前</string>
<string name="time_hours_ago">%d小时前</string>
<string name="time_today">今天</string>
<string name="time_yesterday">昨天</string>
```

### 8.2 字符串资源使用示例

```kotlin
// 在Composable中使用
Text(text = stringResource(R.string.ai_advisor_title))

// 带参数的字符串
Text(text = stringResource(R.string.conversation_with, contactName))

// ContentDescription
Icon(
    imageVector = Icons.Default.Menu,
    contentDescription = stringResource(R.string.content_desc_menu)
)
```

### 8.3 多语言支持（可选）

如需支持英文，创建 `values-en/strings.xml`：

```xml
<!-- AI Advisor UI Optimization - PRD-00029 -->
<string name="ai_advisor_title">AI Advisor</string>
<string name="session_history_title">Session History</string>
<string name="select_contact_title">Select Contact</string>
<!-- ... 其他翻译 ... -->
```

---

## 9. 调用链设计

### 9.1 会话历史页面调用链

```
SessionHistoryScreen
    ↓ (通过hiltViewModel()注入)
SessionHistoryViewModel
    ↓ (调用Repository)
AiAdvisorRepository.getSessions(contactId)
    ↓ (调用DAO)
AiAdvisorDao.getSessionsByContact(contactId)
    ↓
返回 List<AiAdvisorSession>
```

### 9.2 联系人选择页面调用链

```
ContactSelectScreen
    ↓ (通过hiltViewModel()注入)
ContactSelectViewModel
    ↓ (调用Repository)
ContactRepository.getAllProfiles()
    ↓ (调用DAO)
ContactDao.getAllProfiles()
    ↓
返回 Flow<List<ContactProfile>>
```

### 9.3 自动恢复联系人调用链

```
AiAdvisorScreen (入口)
    ↓ (通过hiltViewModel()注入)
AiAdvisorViewModel
    ↓ (读取偏好设置)
AiAdvisorPreferences.getLastContactId()
    ↓
├── 有值 → NavigationTarget.Chat(contactId)
└── 无值 → NavigationTarget.ContactSelect
    ↓
导航到对应页面
```

---

## 10. 文件清单

### 10.1 新增文件

| 文件路径 | 模块 | 说明 |
|---------|------|------|
| `data/local/AiAdvisorPreferences.kt` | :data | AI军师偏好设置存储 |
| `ui/screen/advisor/SessionHistoryScreen.kt` | :presentation | 会话历史页面 |
| `ui/screen/advisor/ContactSelectScreen.kt` | :presentation | 联系人选择页面 |
| `viewmodel/SessionHistoryViewModel.kt` | :presentation | 会话历史ViewModel |
| `viewmodel/ContactSelectViewModel.kt` | :presentation | 联系人选择ViewModel |
| `ui/screen/advisor/component/SessionListItem.kt` | :presentation | 会话列表项组件 |

### 10.2 修改文件

| 文件路径 | 模块 | 修改内容 |
|---------|------|----------|
| `navigation/NavRoutes.kt` | :presentation | 新增AI_ADVISOR_SESSIONS、AI_ADVISOR_CONTACTS路由 |
| `navigation/NavGraph.kt` | :presentation | 新增两个页面的路由配置 |
| `ui/screen/advisor/AiAdvisorScreen.kt` | :presentation | 改为入口路由页面 |
| `ui/screen/advisor/AiAdvisorChatScreen.kt` | :presentation | 修改导航栏为☰和👤图标 |
| `viewmodel/AiAdvisorChatViewModel.kt` | :presentation | 添加自动恢复联系人逻辑 |
| `viewmodel/AiAdvisorViewModel.kt` | :presentation | 添加导航目标判断逻辑 |

---

## 11. 测试计划

### 11.1 单元测试

| 测试类 | 测试内容 | 优先级 |
|--------|----------|--------|
| `AiAdvisorPreferencesTest` | 偏好设置读写测试 | P0 |
| `SessionHistoryViewModelTest` | 会话列表加载、删除测试 | P0 |
| `ContactSelectViewModelTest` | 联系人列表加载、选择测试 | P0 |
| `AiAdvisorViewModelTest` | 导航目标判断测试 | P0 |

### 11.2 UI测试

| 测试类 | 测试内容 | 优先级 |
|--------|----------|--------|
| `SessionHistoryScreenTest` | 会话列表显示、点击交互 | P1 |
| `ContactSelectScreenTest` | 联系人列表显示、选择交互 | P1 |
| `AiAdvisorChatScreenTest` | 导航栏图标点击测试 | P1 |

### 11.3 集成测试

| 测试场景 | 测试内容 | 优先级 |
|----------|----------|--------|
| 首次使用流程 | 无历史记录→联系人选择→对话界面 | P0 |
| 恢复联系人流程 | 有历史记录→自动进入对话界面 | P0 |
| 切换联系人流程 | 对话界面→联系人选择→新对话 | P0 |
| 查看历史会话流程 | 对话界面→会话历史→加载会话 | P0 |

---

## 12. 任务分解

### 12.1 Phase 1: 基础设施（预计2天）

| 任务ID | 任务描述 | 预计工时 | 依赖 |
|--------|----------|----------|------|
| T029-01 | 创建AiAdvisorPreferences | 2h | - |
| T029-02 | 修改NavRoutes添加新路由 | 1h | - |
| T029-03 | 修改NavGraph添加路由配置 | 2h | T029-02 |
| T029-04 | 编写AiAdvisorPreferencesTest | 2h | T029-01 |

### 12.2 Phase 2: 会话历史页面（预计3天）

| 任务ID | 任务描述 | 预计工时 | 依赖 |
|--------|----------|----------|------|
| T029-05 | 创建SessionHistoryViewModel | 3h | T029-01 |
| T029-06 | 创建SessionHistoryScreen | 4h | T029-05 |
| T029-07 | 创建SessionListItem组件 | 2h | - |
| T029-08 | 编写SessionHistoryViewModelTest | 2h | T029-05 |
| T029-09 | 编写SessionHistoryScreenTest | 2h | T029-06 |

### 12.3 Phase 3: 联系人选择页面（预计3天）

| 任务ID | 任务描述 | 预计工时 | 依赖 |
|--------|----------|----------|------|
| T029-10 | 创建ContactSelectViewModel | 3h | T029-01 |
| T029-11 | 创建ContactSelectScreen | 4h | T029-10 |
| T029-12 | 编写ContactSelectViewModelTest | 2h | T029-10 |
| T029-13 | 编写ContactSelectScreenTest | 2h | T029-11 |

### 12.4 Phase 4: 对话界面修改（预计2天）

| 任务ID | 任务描述 | 预计工时 | 依赖 |
|--------|----------|----------|------|
| T029-14 | 修改AiAdvisorChatScreen导航栏 | 2h | - |
| T029-15 | 修改AiAdvisorChatViewModel添加导航逻辑 | 2h | T029-01 |
| T029-16 | 修改AiAdvisorScreen为入口页面 | 2h | T029-01 |
| T029-17 | 修改AiAdvisorViewModel添加导航判断 | 2h | T029-01 |

### 12.5 Phase 5: 集成测试（预计1天）

| 任务ID | 任务描述 | 预计工时 | 依赖 |
|--------|----------|----------|------|
| T029-18 | 首次使用流程集成测试 | 2h | T029-11, T029-16 |
| T029-19 | 恢复联系人流程集成测试 | 2h | T029-16 |
| T029-20 | 切换联系人流程集成测试 | 2h | T029-11, T029-14 |
| T029-21 | 查看历史会话流程集成测试 | 2h | T029-06, T029-14 |

---

## 13. 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| UI原型与实现不一致 | 高 | 中 | 严格按照HTML原型文件实现，开发前仔细对照 |
| 导航状态管理复杂 | 中 | 中 | 使用Navigation Compose的SavedStateHandle管理状态 |
| 偏好设置加密失败 | 高 | 低 | 添加fallback机制，使用普通SharedPreferences |
| 会话列表加载性能 | 中 | 低 | 使用Paging 3分页加载，限制初始加载数量 |

---

## 14. 关联文档

- [PRD-00029-AI军师UI优化需求](../PRD/PRD-00029-AI军师UI优化需求.md)
- [DR-00029-PRD00029文档审查报告](../DR/DR-00029-PRD00029文档审查报告.md)
- [TDD-00026-AI军师对话功能技术设计](./TDD-00026-AI军师对话功能技术设计.md)

---

**文档版本**: 1.1  
**最后更新**: 2026-01-06  
**更新内容**: 根据DR-00029审查报告补充Repository接口设计、UseCase设计、DI模块集成、字符串资源配置章节
