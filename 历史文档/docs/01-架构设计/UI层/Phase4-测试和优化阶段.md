# Phase4: 测试和优化阶段

## 📋 阶段概览

**目标**: 完善UI层测试,优化性能,打磨用户体验

**预计工期**: 2-3天

**优先级**: P1 (应该完成)

**前置条件**:
- ✅ Phase1: 基础设施阶段已完成
- ✅ Phase2: 可复用组件阶段已完成
- ✅ Phase3: 核心Screen阶段已完成
- ✅ 所有核心功能可正常运行

**交付物**:
1. ViewModel单元测试
2. UI组件测试
3. 性能优化报告
4. UI/UX细节打磨清单

---

## 一、测试策略

### 1.1 测试金字塔

```
       /\
      /  \  E2E测试 (10%)
     /____\
    /      \  集成测试 (20%)
   /________\
  /          \  单元测试 (70%)
 /__________\
```

**测试优先级**:
1. **P0**: ViewModel单元测试 (业务逻辑核心)
2. **P1**: UI组件测试 (交互验证)
3. **P2**: E2E测试 (关键用户流程)

### 1.2 测试工具栈

```kotlin
// build.gradle.kts
dependencies {
    // 单元测试
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    // UI测试
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## 二、ViewModel单元测试

### 2.1 测试原则

**必须测试**:
- ✅ 状态初始化
- ✅ 事件处理逻辑
- ✅ UseCase调用
- ✅ 错误处理
- ✅ 边界条件

**不需要测试**:
- ❌ Android Framework (已由Google测试)
- ❌ 第三方库内部逻辑
- ❌ 简单的getter/setter

### 2.2 ChatViewModel测试示例

**文件路径**: `app/src/test/java/com/empathy/ai/presentation/viewmodel/ChatViewModelTest.kt`

```kotlin
package com.empathy.ai.presentation.viewmodel

import app.cash.turbine.test
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.usecase.AnalyzeChatUseCase
import com.empathy.ai.domain.usecase.CheckDraftUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.presentation.ui.screen.chat.ChatUiEvent
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    
    private lateinit var viewModel: ChatViewModel
    private lateinit var analyzeChatUseCase: AnalyzeChatUseCase
    private lateinit var checkDraftUseCase: CheckDraftUseCase
    private lateinit var getContactUseCase: GetContactUseCase
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        analyzeChatUseCase = mockk()
        checkDraftUseCase = mockk()
        getContactUseCase = mockk()
        
        viewModel = ChatViewModel(
            analyzeChatUseCase = analyzeChatUseCase,
            checkDraftUseCase = checkDraftUseCase,
            getContactUseCase = getContactUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `初始状态应该正确`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.messages.isEmpty())
            assertEquals("", state.inputText)
        }
    }
    
    @Test
    fun `加载联系人成功应该更新状态`() = runTest {
        // Given
        val mockContact = ContactProfile(
            id = 1L,
            name = "测试联系人",
            targetGoal = "测试目标"
        )
        coEvery { getContactUseCase("1") } returns Result.success(mockContact)
        
        // When
        viewModel.onEvent(ChatUiEvent.LoadChat("1"))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(mockContact, state.contactProfile)
            assertFalse(state.isLoading)
        }
    }
    
    @Test
    fun `分析聊天成功应该显示结果`() = runTest {
        // Given
        val mockAnalysis = AnalysisResult(
            emotionalState = "开心",
            keyInsights = listOf("洞察1", "洞察2"),
            suggestedActions = listOf("建议1", "建议2")
        )
        coEvery { 
            analyzeChatUseCase(any(), any()) 
        } returns Result.success(mockAnalysis)
        
        // When
        viewModel.onEvent(ChatUiEvent.LoadChat("1"))
        viewModel.onEvent(ChatUiEvent.AnalyzeChat)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(mockAnalysis, state.analysisResult)
            assertTrue(state.showAnalysisDialog)
            assertFalse(state.isAnalyzing)
        }
    }
    
    @Test
    fun `分析失败应该显示错误`() = runTest {
        // Given
        val errorMessage = "分析失败"
        coEvery { 
            analyzeChatUseCase(any(), any()) 
        } returns Result.failure(Exception(errorMessage))
        
        // When
        viewModel.onEvent(ChatUiEvent.LoadChat("1"))
        viewModel.onEvent(ChatUiEvent.AnalyzeChat)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(errorMessage, state.error)
            assertFalse(state.isAnalyzing)
        }
    }
    
    @Test
    fun `更新输入文本应该更新状态`() = runTest {
        // Given
        val newText = "新消息"
        
        // When
        viewModel.onEvent(ChatUiEvent.UpdateInputText(newText))
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(newText, state.inputText)
        }
    }
}
```

### 2.3 测试覆盖率目标

**必须达到**:
- ViewModel测试覆盖率 ≥ 80%
- UseCase调用验证 100%
- 错误场景覆盖 100%

**运行测试**:
```bash
# 运行所有单元测试
./gradlew test

# 生成覆盖率报告
./gradlew testDebugUnitTestCoverage

# 查看报告
open app/build/reports/coverage/test/debug/index.html
```

---

## 三、UI组件测试

### 3.1 Compose测试基础

**测试规则**:
```kotlin
@get:Rule
val composeTestRule = createComposeRule()
```

### 3.2 组件测试示例

**文件路径**: `app/src/androidTest/java/com/empathy/ai/presentation/ui/component/ContactCardTest.kt`

```kotlin
package com.empathy.ai.presentation.ui.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.EmpathyTheme
import org.junit.Rule
import org.junit.Test

class ContactCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun contactCard_显示联系人姓名() {
        // Given
        val contact = ContactProfile(
            id = 1L,
            name = "张三",
            targetGoal = "测试目标"
        )
        
        // When
        composeTestRule.setContent {
            EmpathyTheme {
                ContactCard(
                    contact = contact,
                    onClick = {}
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("张三")
            .assertIsDisplayed()
    }
    
    @Test
    fun contactCard_点击触发回调() {
        // Given
        var clicked = false
        val contact = ContactProfile(
            id = 1L,
            name = "张三",
            targetGoal = "测试目标"
        )
        
        // When
        composeTestRule.setContent {
            EmpathyTheme {
                ContactCard(
                    contact = contact,
                    onClick = { clicked = true }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("张三")
            .performClick()
        
        assert(clicked)
    }
    
    @Test
    fun contactCard_显示标签() {
        // Given
        val contact = ContactProfile(
            id = 1L,
            name = "张三",
            targetGoal = "测试目标",
            brainTags = listOf(
                com.empathy.ai.domain.model.BrainTag(
                    id = 1L,
                    label = "同事",
                    category = "关系"
                )
            )
        )
        
        // When
        composeTestRule.setContent {
            EmpathyTheme {
                ContactCard(
                    contact = contact,
                    onClick = {}
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("同事")
            .assertIsDisplayed()
    }
}
```

### 3.3 Screen测试示例

```kotlin
@Test
fun contactListScreen_显示联系人列表() {
    // Given
    val mockContacts = listOf(
        ContactProfile(id = 1L, name = "张三", targetGoal = "目标1"),
        ContactProfile(id = 2L, name = "李四", targetGoal = "目标2")
    )
    
    // Mock ViewModel
    val viewModel = mockk<ContactListViewModel>(relaxed = true)
    every { viewModel.uiState } returns MutableStateFlow(
        ContactListUiState(contacts = mockContacts)
    ).asStateFlow()
    
    // When
    composeTestRule.setContent {
        EmpathyTheme {
            ContactListScreen(
                viewModel = viewModel,
                onNavigateToDetail = {},
                onNavigateToChat = {}
            )
        }
    }
    
    // Then
    composeTestRule.onNodeWithText("张三").assertIsDisplayed()
    composeTestRule.onNodeWithText("李四").assertIsDisplayed()
}
```

---

## 四、性能优化

### 4.1 Compose性能优化

#### 避免不必要的重组

**问题代码**:
```kotlin
@Composable
fun ContactList(contacts: List<ContactProfile>) {
    LazyColumn {
        // ❌ 每次重组都会创建新lambda
        items(contacts) { contact ->
            ContactCard(
                contact = contact,
                onClick = { /* 处理点击 */ }
            )
        }
    }
}
```

**优化后**:
```kotlin
@Composable
fun ContactList(
    contacts: List<ContactProfile>,
    onContactClick: (String) -> Unit  // ✅ 提升到参数
) {
    LazyColumn {
        items(
            items = contacts,
            key = { it.id }  // ✅ 提供稳定的key
        ) { contact ->
            ContactCard(
                contact = contact,
                onClick = { onContactClick(contact.id.toString()) }
            )
        }
    }
}
```

#### 使用remember优化

```kotlin
@Composable
fun ChatScreen() {
    val listState = rememberLazyListState()  // ✅ 记住滚动状态
    val scope = rememberCoroutineScope()     // ✅ 记住协程作用域
    
    LazyColumn(state = listState) {
        // ...
    }
}
```

#### 使用derivedStateOf

```kotlin
@Composable
fun ContactList(contacts: List<ContactProfile>, searchQuery: String) {
    // ✅ 只在searchQuery或contacts变化时重新计算
    val filteredContacts by remember(searchQuery, contacts) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                contacts
            } else {
                contacts.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }
    }
    
    LazyColumn {
        items(filteredContacts) { contact ->
            ContactCard(contact = contact, onClick = {})
        }
    }
}
```

### 4.2 性能检测工具

#### Layout Inspector
```bash
# Android Studio → Tools → Layout Inspector
# 查看Compose布局层次和重组次数
```

#### Profiler
```bash
# Android Studio → View → Tool Windows → Profiler
# 监控CPU、内存、网络使用
```

#### Compose Metrics
```kotlin
// build.gradle.kts
android {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

// 生成Compose metrics报告
tasks.register("generateComposeMetrics") {
    doLast {
        exec {
            commandLine("./gradlew", "assembleRelease", 
                "-P", "androidx.compose.compiler.metricsDestination=build/compose_metrics")
        }
    }
}
```

### 4.3 性能优化清单

- [ ] LazyColumn使用key参数
- [ ] 避免在Composable中创建新对象
- [ ] 使用remember缓存计算结果
- [ ] 图片使用Coil异步加载
- [ ] 长列表使用分页加载
- [ ] 避免过深的布局嵌套
- [ ] 使用Baseline Profile优化启动

---

## 五、UI/UX细节打磨

### 5.1 交互细节

#### 加载状态
- [ ] 所有网络请求显示Loading
- [ ] Loading时禁用操作按钮
- [ ] 超时提示(>10秒)

#### 错误处理
- [ ] 网络错误提供重试
- [ ] 用户友好的错误提示
- [ ] 关键操作二次确认

#### 反馈提示
- [ ] 操作成功显示Snackbar
- [ ] 删除操作可撤销
- [ ] 表单验证实时提示

### 5.2 动画效果

```kotlin
// 页面切换动画
AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
        fadeIn() + slideInHorizontally() with
        fadeOut() + slideOutHorizontally()
    }
) { screen ->
    // Screen内容
}

// 列表项动画
LazyColumn {
    items(
        items = contacts,
        key = { it.id }
    ) { contact ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + expandVertically()
        ) {
            ContactCard(contact = contact, onClick = {})
        }
    }
}
