# 设计文档 - 悬浮窗最小化功能

## 概述

本设计文档描述了悬浮窗最小化功能的技术实现方案。该功能允许用户在等待 AI 响应时将输入对话框最小化为小型加载指示器，从而可以继续使用其他应用而不被阻塞。当 AI 响应完成后，系统会通过通知提醒用户查看结果。

### 核心目标

1. **改善用户体验**：用户不再需要等待 AI 响应完成才能继续操作
2. **后台处理**：AI 请求在后台继续执行，不影响用户使用其他应用
3. **及时通知**：AI 响应完成后通过系统通知提醒用户
4. **状态可视化**：通过加载指示器的外观清晰展示当前处理状态
5. **位置一致性**：最小化指示器显示在原悬浮按钮的位置

### 设计原则

- **非侵入性**：最小化指示器应尽可能小，不遮挡用户操作
- **状态清晰**：通过颜色和图标清晰表达当前状态（处理中/成功/失败）
- **流畅动画**：所有状态转换都应有流畅的动画效果
- **资源高效**：最小化状态下应释放不必要的 UI 资源
- **容错性强**：即使应用被系统杀死，也能恢复请求状态

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    FloatingWindowService                 │
│  ┌───────────────────────────────────────────────────┐  │
│  │              FloatingView (主视图)                 │  │
│  │  ┌─────────────┐  ┌──────────────────────────┐   │  │
│  │  │ 悬浮按钮模式 │  │   输入对话框模式          │   │  │
│  │  └─────────────┘  └──────────────────────────┘   │  │
│  │                    ┌──────────────────────────┐   │  │
│  │                    │   最小化指示器模式        │   │  │
│  │                    └──────────────────────────┘   │  │
│  └───────────────────────────────────────────────────┘  │
│                           │                              │
│                           ▼                              │
│  ┌───────────────────────────────────────────────────┐  │
│  │          MinimizedIndicatorView (新增)            │  │
│  │  - 显示加载动画                                    │  │
│  │  - 显示完成状态（成功/失败）                       │  │
│  │  - 支持拖动                                        │  │
│  │  - 点击恢复对话框                                  │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│              NotificationManager (系统)                  │
│  - 发送完成通知                                          │
│  - 处理通知点击事件                                      │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│         FloatingWindowPreferences (持久化)               │
│  - 保存请求状态                                          │
│  - 保存指示器位置                                        │
│  - 保存响应结果                                          │
└─────────────────────────────────────────────────────────┘
```

### 层次结构

1. **Service 层**：`FloatingWindowService`
   - 管理服务生命周期
   - 协调视图切换
   - 处理后台任务
   - 发送通知

2. **View 层**：`FloatingView` + `MinimizedIndicatorView`（新增）
   - `FloatingView`：管理悬浮按钮和输入对话框
   - `MinimizedIndicatorView`：管理最小化指示器

3. **Data 层**：`FloatingWindowPreferences`（扩展）
   - 持久化请求状态
   - 持久化响应结果
   - 持久化指示器位置


## 组件和接口

### 1. MinimizedIndicator（新增 Compose 组件）

最小化指示器 Composable，显示 AI 请求的处理状态。

#### 职责

- 显示加载动画（旋转的圆形进度条）
- 显示完成状态（绿色对勾或红色错误图标）
- 支持拖动和位置保存
- 处理点击事件（恢复对话框）

#### 状态定义

```kotlin
// 状态接口（支持扩展）
interface MinimizedIndicatorState {
    val icon: ImageVector
    val backgroundColor: Color
    val contentDescription: String
    val showProgress: Boolean
}

// 加载状态
data class LoadingState(
    override val icon: ImageVector = Icons.Default.Refresh,
    override val backgroundColor: Color = Color(0xFF2196F3),  // 蓝色
    override val contentDescription: String = "加载中",
    override val showProgress: Boolean = true
) : MinimizedIndicatorState

// 成功状态
data class SuccessState(
    override val icon: ImageVector = Icons.Default.Check,
    override val backgroundColor: Color = Color(0xFF4CAF50),  // 绿色
    override val contentDescription: String = "完成",
    override val showProgress: Boolean = false
) : MinimizedIndicatorState

// 错误状态
data class ErrorState(
    override val icon: ImageVector = Icons.Default.Error,
    override val backgroundColor: Color = Color(0xFFF44336),  // 红色
    override val contentDescription: String = "错误",
    override val showProgress: Boolean = false
) : MinimizedIndicatorState
```

#### 配置类

```kotlin
// 指示器配置（支持自定义）
data class MinimizedIndicatorConfig(
    val size: Dp = 56.dp,                      // 指示器大小
    val animationDuration: Int = 300,          // 动画时长（ms）
    val enableHapticFeedback: Boolean = true,  // 触觉反馈
    val snapToEdge: Boolean = false            // 边缘吸附（已废弃）
)
```

#### Composable 实现

```kotlin
@Composable
fun MinimizedIndicator(
    state: MinimizedIndicatorState,
    config: MinimizedIndicatorConfig = MinimizedIndicatorConfig(),
    onPositionChanged: (Int, Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 动画状态
    val scale by animateFloatAsState(
        targetValue = if (state.showProgress) 1f else 1.1f,
        animationSpec = tween(durationMillis = config.animationDuration),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = state.backgroundColor,
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColor"
    )
    
    Box(
        modifier = modifier
            .size(config.size)
            .scale(scale)
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            .clickable { 
                if (config.enableHapticFeedback) {
                    // 触觉反馈
                }
                onClick() 
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // 计算新位置
                    val newX = (change.position.x + dragAmount.x).toInt()
                    val newY = (change.position.y + dragAmount.y).toInt()
                    onPositionChanged(newX, newY)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            state.showProgress -> {
                // 显示加载进度
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
            else -> {
                // 显示图标
                Icon(
                    imageVector = state.icon,
                    contentDescription = state.contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

### 2. FloatingWindowViewModel（新增 ViewModel）

使用 ViewModel 管理悬浮窗和最小化指示器的状态。

#### UiState 定义

```kotlin
// 最小化指示器 UI 状态
data class MinimizedIndicatorUiState(
    val isVisible: Boolean = false,
    val state: MinimizedIndicatorState = LoadingState(),
    val position: Pair<Int, Int> = Pair(0, 0),
    val isAnimating: Boolean = false
)

// 悬浮窗 UI 状态
data class FloatingWindowUiState(
    val isDialogVisible: Boolean = false,
    val isButtonVisible: Boolean = true,
    val minimizedIndicator: MinimizedIndicatorUiState = MinimizedIndicatorUiState()
)
```

#### ViewModel 实现

```kotlin
@HiltViewModel
class FloatingWindowViewModel @Inject constructor(
    private val floatingWindowPreferences: FloatingWindowPreferences,
    private val analyzeChatUseCase: AnalyzeChatUseCase,
    private val checkDraftUseCase: CheckDraftUseCase
) : ViewModel() {
    
    // 私有可变状态
    private val _uiState = MutableStateFlow(FloatingWindowUiState())
    
    // 公开不可变状态
    val uiState: StateFlow<FloatingWindowUiState> = _uiState.asStateFlow()
    
    // 当前请求信息
    private var currentRequest: MinimizedRequestInfo? = null
    
    // 最小化对话框
    fun minimizeDialog() {
        viewModelScope.launch {
            try {
                // 保存当前请求信息
                currentRequest?.let { request ->
                    floatingWindowPreferences.saveRequestInfo(request)
                }
                
                // 获取当前悬浮按钮位置
                val buttonPos = floatingWindowPreferences.getButtonPosition()
                
                // 更新 UI 状态
                _uiState.update { state ->
                    state.copy(
                        isDialogVisible = false,
                        isButtonVisible = false,
                        minimizedIndicator = MinimizedIndicatorUiState(
                            isVisible = true,
                            state = LoadingState(),
                            position = buttonPos,
                            isAnimating = true
                        )
                    )
                }
                
                // 动画完成后停止动画状态
                delay(300)
                _uiState.update { state ->
                    state.copy(
                        minimizedIndicator = state.minimizedIndicator.copy(
                            isAnimating = false
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "最小化失败", e)
            }
        }
    }
    
    // 恢复对话框
    fun restoreDialog() {
        viewModelScope.launch {
            try {
                // 获取指示器位置
                val indicatorPos = _uiState.value.minimizedIndicator.position
                
                // 保存位置到悬浮按钮
                floatingWindowPreferences.saveButtonPosition(indicatorPos.first, indicatorPos.second)
                
                // 更新 UI 状态
                _uiState.update { state ->
                    state.copy(
                        isDialogVisible = true,
                        isButtonVisible = false,
                        minimizedIndicator = state.minimizedIndicator.copy(
                            isVisible = false,
                            isAnimating = true
                        )
                    )
                }
                
                // 清除保存的请求信息
                floatingWindowPreferences.clearRequestInfo()
                
                // 动画完成后停止动画状态
                delay(300)
                _uiState.update { state ->
                    state.copy(
                        minimizedIndicator = state.minimizedIndicator.copy(
                            isAnimating = false
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "恢复失败", e)
            }
        }
    }
    
    // 更新指示器状态
    fun updateIndicatorState(newState: MinimizedIndicatorState) {
        _uiState.update { state ->
            state.copy(
                minimizedIndicator = state.minimizedIndicator.copy(
                    state = newState
                )
            )
        }
    }
    
    // 更新指示器位置
    fun updateIndicatorPosition(x: Int, y: Int) {
        _uiState.update { state ->
            state.copy(
                minimizedIndicator = state.minimizedIndicator.copy(
                    position = Pair(x, y)
                )
            )
        }
        // 保存位置
        floatingWindowPreferences.saveIndicatorPosition(x, y)
    }
    
    companion object {
        private const val TAG = "FloatingWindowViewModel"
    }
}
```

#### 简化的请求信息数据类

```kotlin
// 简化的请求信息（只保存必要数据）
data class MinimizedRequestInfo(
    val id: String,                                    // 请求唯一标识（UUID）
    val type: ActionType,                              // 操作类型（ANALYZE 或 CHECK）
    val timestamp: Long = System.currentTimeMillis()   // 请求时间戳
)
```

### 3. FloatingWindowService（扩展现有服务）

扩展现有的 `FloatingWindowService` 以支持最小化和通知功能。

#### 新增属性和方法

```kotlin
@AndroidEntryPoint
class FloatingWindowService : Service() {
    
    // 注入 ViewModel
    @Inject
    lateinit var viewModel: FloatingWindowViewModel
    
    // Compose View 用于显示最小化指示器
    private var indicatorComposeView: ComposeView? = null
    
    // 新增：显示最小化指示器
    private fun showMinimizedIndicator() {
        // 创建 ComposeView
        val composeView = ComposeView(this).apply {
            setContent {
                // 收集 ViewModel 状态
                val uiState by viewModel.uiState.collectAsState()
                val indicatorState = uiState.minimizedIndicator
                
                // 只在可见时显示
                if (indicatorState.isVisible) {
                    MinimizedIndicator(
                        state = indicatorState.state,
                        onPositionChanged = { x, y ->
                            viewModel.updateIndicatorPosition(x, y)
                        },
                        onClick = {
                            viewModel.restoreDialog()
                        }
                    )
                }
            }
        }
        
        // 配置布局参数
        val params = createIndicatorLayoutParams(
            x = viewModel.uiState.value.minimizedIndicator.position.first,
            y = viewModel.uiState.value.minimizedIndicator.position.second
        )
        
        // 添加到 WindowManager
        windowManager.addView(composeView, params)
        indicatorComposeView = composeView
    }
    
    // 新增：移除最小化指示器
    private fun removeMinimizedIndicator() {
        indicatorComposeView?.let {
            windowManager.removeView(it)
            indicatorComposeView = null
        }
    }
    
    // 新增：发送完成通知
    private fun sendCompletionNotification(result: Any?, isSuccess: Boolean) {
        // 只在最小化状态下发送通知
        if (!viewModel.uiState.value.minimizedIndicator.isVisible) {
            return
        }
        
        // 构建通知...
    }
    
    // 新增：处理通知点击
    private fun handleNotificationClick() {
        viewModel.restoreDialog()
    }
    
    // 新增：恢复请求状态（应用重启后）
    private fun restoreRequestState() {
        val requestInfo = floatingWindowPreferences.getRequestInfo()
        if (requestInfo != null) {
            // 检查是否过期（10 分钟）
            val currentTime = System.currentTimeMillis()
            if (currentTime - requestInfo.timestamp > 10 * 60 * 1000) {
                floatingWindowPreferences.clearRequestInfo()
                return
            }
            
            // 恢复指示器
            showMinimizedIndicator()
        }
    }
    
    // 新增：清理已完成的指示器
    private fun cleanupCompletedIndicator() {
        serviceScope.launch {
            delay(10 * 60 * 1000)  // 10 分钟后清理
            
            val indicatorState = viewModel.uiState.value.minimizedIndicator
            if (indicatorState.isVisible && !indicatorState.state.showProgress) {
                removeMinimizedIndicator()
                floatingWindowPreferences.clearRequestInfo()
            }
        }
    }
    
    // 创建指示器布局参数
    private fun createIndicatorLayoutParams(x: Int, y: Int): WindowManager.LayoutParams {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }
}
```

### 4. FloatingWindowPreferences（扩展现有类）

扩展现有的 `FloatingWindowPreferences` 以支持请求状态持久化。

#### 新增方法

```kotlin
class FloatingWindowPreferences @Inject constructor(
    private val context: Context
) {
    
    // 新增：保存请求信息
    fun saveRequestInfo(requestInfo: RequestInfo)
    
    // 新增：获取请求信息
    fun getRequestInfo(): RequestInfo?
    
    // 新增：清除请求信息
    fun clearRequestInfo()
    
    // 新增：保存响应结果
    fun saveResponse(requestId: String, response: String)
    
    // 新增：获取响应结果
    fun getResponse(requestId: String): String?
    
    // 新增：清除响应结果
    fun clearResponse(requestId: String)
    
    // 新增：保存指示器位置
    fun saveIndicatorPosition(x: Int, y: Int)
    
    // 新增：获取指示器位置
    fun getIndicatorPosition(): Pair<Int, Int>
}
```


## 数据模型

### MinimizedRequestInfo（简化的请求信息）

```kotlin
// 简化的请求信息（只保存必要数据）
data class MinimizedRequestInfo(
    val id: String,                                    // 请求唯一标识（UUID）
    val type: ActionType,                              // 操作类型（ANALYZE 或 CHECK）
    val timestamp: Long = System.currentTimeMillis()   // 请求时间戳
)
```

### MinimizedIndicatorState（指示器状态接口）

```kotlin
// 状态接口（支持扩展）
interface MinimizedIndicatorState {
    val icon: ImageVector
    val backgroundColor: Color
    val contentDescription: String
    val showProgress: Boolean
}

// 加载状态
data class LoadingState(
    override val icon: ImageVector = Icons.Default.Refresh,
    override val backgroundColor: Color = Color(0xFF2196F3),
    override val contentDescription: String = "加载中",
    override val showProgress: Boolean = true
) : MinimizedIndicatorState

// 成功状态
data class SuccessState(
    override val icon: ImageVector = Icons.Default.Check,
    override val backgroundColor: Color = Color(0xFF4CAF50),
    override val contentDescription: String = "完成",
    override val showProgress: Boolean = false
) : MinimizedIndicatorState

// 错误状态
data class ErrorState(
    override val icon: ImageVector = Icons.Default.Error,
    override val backgroundColor: Color = Color(0xFFF44336),
    override val contentDescription: String = "错误",
    override val showProgress: Boolean = false
) : MinimizedIndicatorState
```

### MinimizedIndicatorUiState（指示器 UI 状态）

```kotlin
data class MinimizedIndicatorUiState(
    val isVisible: Boolean = false,
    val state: MinimizedIndicatorState = LoadingState(),
    val position: Pair<Int, Int> = Pair(0, 0),
    val isAnimating: Boolean = false
)
```

### FloatingWindowUiState（悬浮窗 UI 状态）

```kotlin
data class FloatingWindowUiState(
    val isDialogVisible: Boolean = false,
    val isButtonVisible: Boolean = true,
    val minimizedIndicator: MinimizedIndicatorUiState = MinimizedIndicatorUiState()
)
```

## 正确性属性

*属性是一个特征或行为，应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的正式陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### 属性 1：最小化后状态保持

*对于任何*输入对话框，当用户点击最小化按钮时，系统应该将对话框收起为指示器，并且 AI 请求应该继续在后台执行而不中断。

**验证：需求 1.1, 1.5, 2.1**

### 属性 2：指示器位置一致性

*对于任何*最小化操作，指示器应该显示在原悬浮按钮的位置，并且当用户关闭对话框时，悬浮按钮应该恢复在指示器的最后位置。

**验证：需求 5.1, 5.5**

### 属性 3：单一指示器约束

*对于任何*时刻，系统中最多只能存在一个最小化指示器。当用户最小化新请求时，之前的指示器应该被自动关闭。

**验证：需求 8.3**

### 属性 4：通知触发条件

*对于任何*AI 响应（成功或失败），如果对话框处于最小化状态，系统应该发送系统通知；如果对话框未最小化，则不应发送通知。

**验证：需求 3.1, 3.5**

### 属性 5：状态指示正确性

*对于任何*请求状态，指示器的外观应该正确反映当前状态：处理中显示蓝色旋转动画，成功显示绿色对勾，失败显示红色错误图标。

**验证：需求 4.1, 4.2, 4.3**

### 属性 6：持久化恢复一致性

*对于任何*未完成的请求，如果应用被系统杀死后重启，系统应该能够恢复请求状态和指示器，并且恢复后的状态应该与杀死前一致。

**验证：需求 9.1, 9.2, 9.3**

### 属性 7：资源释放及时性

*对于任何*最小化操作，系统应该立即释放输入对话框的 UI 资源，只保留轻量级的指示器视图。

**验证：需求 8.1, 8.2**

### 属性 8：动画流畅性

*对于任何*状态转换（最小化、恢复、状态变化），动画应该在指定时间内完成（最小化/恢复 300ms，状态变化 200ms），并且保持 60 FPS 的流畅度。

**验证：需求 6.1, 6.2, 6.3, 6.5**

### 属性 9：拖动位置保存

*对于任何*指示器拖动操作，当用户松手时，系统应该保存新位置，并且下次最小化时应该在该位置显示。

**验证：需求 5.2, 5.3, 5.4**

### 属性 10：错误处理完整性

*对于任何*后台处理错误（超时、网络断开等），系统应该更新指示器状态为错误，发送错误通知，并且允许用户点击查看详细错误信息和重试。

**验证：需求 7.1, 7.2, 7.3, 7.4, 7.5**


## 错误处理

### 错误类型（简化）

```kotlin
// 简化的错误类型
sealed class MinimizeError(message: String) : Exception(message) {
    
    // 最小化失败
    class MinimizeFailed(reason: String) : MinimizeError("最小化失败: $reason")
    
    // 恢复失败
    class RestoreFailed(reason: String) : MinimizeError("恢复失败: $reason")
    
    // 通知失败
    class NotificationFailed(reason: String) : MinimizeError("通知失败: $reason")
}
```

### 错误处理策略

#### 1. 最小化操作失败

**场景**：用户点击最小化按钮，但创建指示器失败

**处理**：
1. 记录错误日志
2. 显示 Toast 提示用户
3. 保持对话框打开状态
4. 不影响 AI 请求继续执行

#### 2. 恢复操作失败

**场景**：用户点击指示器或通知，但恢复对话框失败

**处理**：
1. 记录错误日志
2. 显示 Toast 提示用户
3. 保持指示器显示
4. 如果有响应结果，尝试通过通知展示

#### 3. 通知发送失败

**场景**：AI 响应完成，但发送通知失败

**处理**：
1. 记录错误日志
2. 更新指示器状态为完成
3. 不影响用户点击指示器查看结果

#### 4. 状态持久化失败

**场景**：保存请求状态到本地存储失败

**处理**：
1. 记录错误日志
2. 继续执行，但标记为"不可恢复"
3. 如果应用被杀死，该请求将丢失

#### 5. 状态恢复失败

**场景**：应用重启后，恢复请求状态失败

**处理**：
1. 记录错误日志
2. 清除损坏的状态数据
3. 不显示指示器
4. 用户需要重新发起请求

#### 6. 后台处理超时

**场景**：AI 请求超过 10 秒未响应

**处理**：
1. 取消请求
2. 更新指示器状态为错误
3. 发送错误通知
4. 允许用户重试

#### 7. 网络连接断开

**场景**：AI 请求过程中网络断开

**处理**：
1. 捕获网络异常
2. 更新指示器状态为错误
3. 发送错误通知（提示检查网络）
4. 允许用户重试

## 测试策略

### 单元测试

#### FloatingWindowViewModel 测试

```kotlin
class FloatingWindowViewModelTest {
    
    private lateinit var viewModel: FloatingWindowViewModel
    private lateinit var mockPreferences: FloatingWindowPreferences
    
    @Before
    fun setup() {
        mockPreferences = mockk(relaxed = true)
        viewModel = FloatingWindowViewModel(
            floatingWindowPreferences = mockPreferences,
            analyzeChatUseCase = mockk(),
            checkDraftUseCase = mockk()
        )
    }
    
    @Test
    fun `最小化应更新 UI 状态`() = runTest {
        // Given
        val initialState = viewModel.uiState.value
        
        // When
        viewModel.minimizeDialog()
        advanceTimeBy(350)  // 等待动画完成
        
        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isDialogVisible)
        assertFalse(finalState.isButtonVisible)
        assertTrue(finalState.minimizedIndicator.isVisible)
        assertEquals(LoadingState(), finalState.minimizedIndicator.state)
    }
    
    @Test
    fun `恢复应更新 UI 状态`() = runTest {
        // Given
        viewModel.minimizeDialog()
        advanceTimeBy(350)
        
        // When
        viewModel.restoreDialog()
        advanceTimeBy(350)
        
        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState.isDialogVisible)
        assertFalse(finalState.isButtonVisible)
        assertFalse(finalState.minimizedIndicator.isVisible)
    }
    
    @Test
    fun `更新指示器状态应反映在 UI 状态中`() = runTest {
        // Given
        viewModel.minimizeDialog()
        advanceTimeBy(350)
        
        // When
        viewModel.updateIndicatorState(SuccessState())
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.minimizedIndicator.state is SuccessState)
    }
    
    @Test
    fun `更新指示器位置应保存到 Preferences`() = runTest {
        // Given
        val newX = 100
        val newY = 200
        
        // When
        viewModel.updateIndicatorPosition(newX, newY)
        
        // Then
        verify { mockPreferences.saveIndicatorPosition(newX, newY) }
        assertEquals(Pair(newX, newY), viewModel.uiState.value.minimizedIndicator.position)
    }
}
```

#### MinimizedIndicator Compose 测试

```kotlin
class MinimizedIndicatorTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `加载状态应显示进度指示器`() {
        // Given
        val state = LoadingState()
        
        // When
        composeTestRule.setContent {
            MinimizedIndicator(
                state = state,
                onPositionChanged = { _, _ -> },
                onClick = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("加载中").assertExists()
    }
    
    @Test
    fun `成功状态应显示对勾图标`() {
        // Given
        val state = SuccessState()
        
        // When
        composeTestRule.setContent {
            MinimizedIndicator(
                state = state,
                onPositionChanged = { _, _ -> },
                onClick = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("完成").assertExists()
    }
    
    @Test
    fun `错误状态应显示错误图标`() {
        // Given
        val state = ErrorState()
        
        // When
        composeTestRule.setContent {
            MinimizedIndicator(
                state = state,
                onPositionChanged = { _, _ -> },
                onClick = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("错误").assertExists()
    }
    
    @Test
    fun `点击应触发回调`() {
        // Given
        var clicked = false
        val state = LoadingState()
        
        // When
        composeTestRule.setContent {
            MinimizedIndicator(
                state = state,
                onPositionChanged = { _, _ -> },
                onClick = { clicked = true }
            )
        }
        
        composeTestRule.onNodeWithContentDescription("加载中").performClick()
        
        // Then
        assertTrue(clicked)
    }
}
```

#### FloatingWindowPreferences 测试

```kotlin
class FloatingWindowPreferencesMinimizeTest {
    
    private lateinit var preferences: FloatingWindowPreferences
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferences = FloatingWindowPreferences(context)
    }
    
    @After
    fun tearDown() {
        preferences.clearRequestInfo()
    }
    
    @Test
    fun `保存和读取请求信息应一致`() {
        // Given
        val requestInfo = MinimizedRequestInfo(
            id = "test-123",
            type = ActionType.ANALYZE,
            timestamp = System.currentTimeMillis()
        )
        
        // When
        preferences.saveRequestInfo(requestInfo)
        val retrieved = preferences.getRequestInfo()
        
        // Then
        assertNotNull(retrieved)
        assertEquals(requestInfo.id, retrieved?.id)
        assertEquals(requestInfo.type, retrieved?.type)
    }
    
    @Test
    fun `清除请求信息应返回 null`() {
        // Given
        val requestInfo = MinimizedRequestInfo(
            id = "test-123",
            type = ActionType.ANALYZE
        )
        preferences.saveRequestInfo(requestInfo)
        
        // When
        preferences.clearRequestInfo()
        val retrieved = preferences.getRequestInfo()
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun `保存和读取指示器位置应一致`() {
        // Given
        val x = 100
        val y = 200
        
        // When
        preferences.saveIndicatorPosition(x, y)
        val (retrievedX, retrievedY) = preferences.getIndicatorPosition()
        
        // Then
        assertEquals(x, retrievedX)
        assertEquals(y, retrievedY)
    }
}
```

### 属性测试

#### 属性 1：最小化后状态保持

```kotlin
@Test
fun `属性测试 - 最小化后 AI 请求继续执行`() = runTest {
    forAll(
        Arb.string(1..100),  // 随机输入文本
        Arb.enum<ActionType>()  // 随机操作类型
    ) { inputText, actionType ->
        // 1. 创建请求
        val requestId = service.createRequest(contactId, inputText, actionType)
        
        // 2. 最小化
        service.minimizeDialog()
        
        // 3. 验证请求仍在执行
        val requestInfo = preferences.getRequestInfo()
        requestInfo != null && 
        requestInfo.state == RequestState.PROCESSING &&
        requestInfo.requestId == requestId
    }
}
```

#### 属性 2：指示器位置一致性

```kotlin
@Test
fun `属性测试 - 指示器位置与悬浮按钮位置一致`() = runTest {
    forAll(
        Arb.int(0..1000),  // 随机 X 坐标
        Arb.int(0..2000)   // 随机 Y 坐标
    ) { x, y ->
        // 1. 设置悬浮按钮位置
        floatingView.setPosition(x, y)
        
        // 2. 最小化
        val indicator = service.minimizeDialog()
        
        // 3. 验证指示器位置
        val indicatorPos = indicator.getPosition()
        indicatorPos.first == x && indicatorPos.second == y
    }
}
```

#### 属性 3：单一指示器约束

```kotlin
@Test
fun `属性测试 - 同时只有一个指示器`() = runTest {
    forAll(
        Arb.list(Arb.string(1..100), 2..5)  // 随机多个请求
    ) { requests ->
        // 1. 依次创建并最小化多个请求
        requests.forEach { text ->
            service.createRequest(contactId, text, ActionType.ANALYZE)
            service.minimizeDialog()
        }
        
        // 2. 验证只有一个指示器
        val indicatorCount = service.getActiveIndicatorCount()
        indicatorCount == 1
    }
}
```

### 集成测试

#### 完整流程测试

```kotlin
@Test
fun `集成测试 - 完整最小化恢复流程`() = runTest {
    // 1. 显示输入对话框
    floatingView.showInputDialog(ActionType.ANALYZE, contacts) { contactId, text ->
        service.performAnalyze(contactId, text)
    }
    
    // 2. 最小化
    val indicator = service.minimizeDialog()
    assertNotNull(indicator)
    assertEquals(MinimizedIndicatorView.State.LOADING, indicator.state)
    
    // 3. 等待 AI 响应
    delay(2000)
    
    // 4. 验证指示器状态更新
    assertEquals(MinimizedIndicatorView.State.SUCCESS, indicator.state)
    
    // 5. 验证通知发送
    val notifications = notificationManager.getActiveNotifications()
    assertTrue(notifications.isNotEmpty())
    
    // 6. 点击指示器恢复
    indicator.performClick()
    
    // 7. 验证对话框恢复并显示结果
    assertTrue(floatingView.isInputDialogVisible())
    assertTrue(floatingView.isResultVisible())
}
```

#### 应用重启恢复测试

```kotlin
@Test
fun `集成测试 - 应用重启后恢复请求`() = runTest {
    // 1. 创建请求并最小化
    service.createRequest(contactId, "test", ActionType.ANALYZE)
    service.minimizeDialog()
    
    // 2. 保存请求状态
    service.saveRequestState()
    
    // 3. 模拟应用重启
    service.onDestroy()
    val newService = FloatingWindowService()
    newService.onCreate()
    
    // 4. 恢复请求状态
    newService.restoreRequestState()
    
    // 5. 验证指示器恢复
    val indicator = newService.getActiveIndicator()
    assertNotNull(indicator)
    assertEquals(MinimizedIndicatorView.State.LOADING, indicator.state)
}
```


## 实现细节

### 1. 最小化流程

```kotlin
// FloatingWindowService.kt
fun minimizeDialog() {
    try {
        // 1. 检查是否已有指示器，如果有则先关闭
        minimizedIndicator?.let {
            windowManager.removeView(it)
            minimizedIndicator = null
        }
        
        // 2. 获取当前请求信息
        val requestInfo = floatingView?.getCurrentRequestInfo()
            ?: throw MinimizeError.MinimizeOperationError("无法获取请求信息")
        
        // 3. 保存请求状态
        floatingWindowPreferences.saveRequestInfo(requestInfo)
        
        // 4. 获取当前悬浮按钮位置
        val buttonPos = floatingView?.getButtonPosition()
            ?: Pair(0, 100)
        
        // 5. 创建最小化指示器
        val indicator = MinimizedIndicatorView(this).apply {
            state = MinimizedIndicatorView.State.LOADING
            onClickListener = { restoreDialog() }
            onPositionChanged = { x, y ->
                floatingWindowPreferences.saveIndicatorPosition(x, y)
            }
        }
        
        // 6. 配置布局参数（使用悬浮按钮的位置）
        val params = indicator.createLayoutParams().apply {
            x = buttonPos.first
            y = buttonPos.second
        }
        
        // 7. 添加指示器到 WindowManager
        windowManager.addView(indicator, params)
        minimizedIndicator = indicator
        
        // 8. 隐藏输入对话框
        floatingView?.hideInputDialog()
        
        // 9. 记录日志
        android.util.Log.d(TAG, "对话框已最小化，指示器位置: $buttonPos")
        
    } catch (e: Exception) {
        android.util.Log.e(TAG, "最小化失败", e)
        val error = MinimizeError.MinimizeOperationError(e.message ?: "未知错误")
        ErrorHandler.handleError(this, error)
    }
}
```

### 2. 恢复流程

```kotlin
// FloatingWindowService.kt
fun restoreDialog() {
    try {
        // 1. 获取保存的请求信息
        val requestInfo = floatingWindowPreferences.getRequestInfo()
            ?: throw MinimizeError.RestoreOperationError("无法获取请求信息")
        
        // 2. 获取保存的响应结果（如果有）
        val response = floatingWindowPreferences.getResponse(requestInfo.requestId)
        
        // 3. 获取指示器位置
        val indicatorPos = minimizedIndicator?.let {
            val params = it.layoutParams as WindowManager.LayoutParams
            Pair(params.x, params.y)
        } ?: floatingWindowPreferences.getIndicatorPosition()
        
        // 4. 移除指示器
        minimizedIndicator?.let {
            windowManager.removeView(it)
            minimizedIndicator = null
        }
        
        // 5. 恢复悬浮按钮到指示器位置
        floatingView?.setButtonPosition(indicatorPos.first, indicatorPos.second)
        
        // 6. 显示输入对话框
        floatingView?.restoreDialog(response)
        
        // 7. 清除保存的状态
        floatingWindowPreferences.clearRequestInfo()
        if (response != null) {
            floatingWindowPreferences.clearResponse(requestInfo.requestId)
        }
        
        // 8. 记录日志
        android.util.Log.d(TAG, "对话框已恢复，悬浮按钮位置: $indicatorPos")
        
    } catch (e: Exception) {
        android.util.Log.e(TAG, "恢复失败", e)
        val error = MinimizeError.RestoreOperationError(e.message ?: "未知错误")
        ErrorHandler.handleError(this, error)
    }
}
```

### 3. 通知发送

```kotlin
// FloatingWindowService.kt
private fun sendCompletionNotification(result: Any?, isSuccess: Boolean) {
    try {
        // 1. 检查是否处于最小化状态
        if (minimizedIndicator == null) {
            android.util.Log.d(TAG, "对话框未最小化，跳过通知")
            return
        }
        
        // 2. 获取请求信息
        val requestInfo = floatingWindowPreferences.getRequestInfo()
            ?: throw MinimizeError.NotificationError("无法获取请求信息")
        
        // 3. 构建通知内容
        val title = if (isSuccess) {
            when (requestInfo.actionType) {
                ActionType.ANALYZE -> "分析完成"
                ActionType.CHECK -> "检查完成"
            }
        } else {
            "处理失败"
        }
        
        val content = if (isSuccess) {
            "点击查看结果"
        } else {
            "点击查看详情"
        }
        
        // 4. 创建通知意图
        val intent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_RESTORE_DIALOG
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 5. 构建通知
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(if (isSuccess) R.drawable.ic_check else R.drawable.ic_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        // 6. 发送通知
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
        
        // 7. 记录日志
        android.util.Log.d(TAG, "完成通知已发送: $title")
        
    } catch (e: Exception) {
        android.util.Log.e(TAG, "发送通知失败", e)
        val error = MinimizeError.NotificationError(e.message ?: "未知错误")
        ErrorHandler.handleError(this, error)
    }
}
```

### 4. 状态持久化

```kotlin
// FloatingWindowPreferences.kt
fun saveRequestInfo(requestInfo: RequestInfo) {
    try {
        val json = moshi.adapter(RequestInfo::class.java).toJson(requestInfo)
        sharedPreferences.edit()
            .putString(KEY_REQUEST_INFO, json)
            .apply()
        android.util.Log.d(TAG, "请求信息已保存: ${requestInfo.requestId}")
    } catch (e: Exception) {
        android.util.Log.e(TAG, "保存请求信息失败", e)
        throw MinimizeError.PersistenceError(e.message ?: "未知错误")
    }
}

fun getRequestInfo(): RequestInfo? {
    return try {
        val json = sharedPreferences.getString(KEY_REQUEST_INFO, null)
            ?: return null
        moshi.adapter(RequestInfo::class.java).fromJson(json)
    } catch (e: Exception) {
        android.util.Log.e(TAG, "读取请求信息失败", e)
        null
    }
}

fun saveResponse(requestId: String, response: String) {
    try {
        sharedPreferences.edit()
            .putString("${KEY_RESPONSE_PREFIX}$requestId", response)
            .apply()
        android.util.Log.d(TAG, "响应结果已保存: $requestId")
    } catch (e: Exception) {
        android.util.Log.e(TAG, "保存响应结果失败", e)
        throw MinimizeError.PersistenceError(e.message ?: "未知错误")
    }
}
```

### 5. 应用重启恢复

```kotlin
// FloatingWindowService.kt
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // ... 现有代码 ...
    
    // 尝试恢复请求状态
    restoreRequestState()
    
    return START_STICKY
}

private fun restoreRequestState() {
    try {
        // 1. 获取保存的请求信息
        val requestInfo = floatingWindowPreferences.getRequestInfo()
            ?: return  // 没有未完成的请求
        
        // 2. 检查请求是否已过期（超过 10 分钟）
        val currentTime = System.currentTimeMillis()
        if (currentTime - requestInfo.timestamp > 10 * 60 * 1000) {
            android.util.Log.w(TAG, "请求已过期，清除状态")
            floatingWindowPreferences.clearRequestInfo()
            return
        }
        
        // 3. 创建最小化指示器
        val indicator = MinimizedIndicatorView(this).apply {
            // 根据保存的状态设置指示器状态
            state = when (requestInfo.state) {
                RequestState.PROCESSING -> MinimizedIndicatorView.State.LOADING
                RequestState.COMPLETED -> MinimizedIndicatorView.State.SUCCESS
                RequestState.FAILED -> MinimizedIndicatorView.State.ERROR
                else -> MinimizedIndicatorView.State.LOADING
            }
            onClickListener = { restoreDialog() }
            onPositionChanged = { x, y ->
                floatingWindowPreferences.saveIndicatorPosition(x, y)
            }
        }
        
        // 4. 配置布局参数
        val params = indicator.createLayoutParams().apply {
            x = requestInfo.indicatorX
            y = requestInfo.indicatorY
        }
        
        // 5. 添加指示器到 WindowManager
        windowManager.addView(indicator, params)
        minimizedIndicator = indicator
        
        // 6. 如果请求仍在处理中，尝试重新发起
        if (requestInfo.state == RequestState.PROCESSING) {
            android.util.Log.d(TAG, "请求仍在处理中，标记为恢复状态")
            // 注意：实际的 AI 请求无法恢复，只能显示"正在恢复"状态
            // 用户需要点击指示器查看详情并选择重试
        }
        
        android.util.Log.d(TAG, "请求状态已恢复: ${requestInfo.requestId}")
        
    } catch (e: Exception) {
        android.util.Log.e(TAG, "恢复请求状态失败", e)
        // 清除损坏的状态数据
        floatingWindowPreferences.clearRequestInfo()
    }
}
```

### 6. 资源清理

```kotlin
// FloatingWindowService.kt
private fun cleanupCompletedIndicator() {
    serviceScope.launch {
        // 等待 10 分钟
        delay(10 * 60 * 1000)
        
        // 检查指示器是否仍然存在且已完成
        minimizedIndicator?.let { indicator ->
            if (indicator.state != MinimizedIndicatorView.State.LOADING) {
                android.util.Log.d(TAG, "清理已完成的指示器")
                windowManager.removeView(indicator)
                minimizedIndicator = null
                floatingWindowPreferences.clearRequestInfo()
            }
        }
    }
}

override fun onDestroy() {
    // ... 现有代码 ...
    
    // 移除最小化指示器
    minimizedIndicator?.let {
        try {
            windowManager.removeView(it)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "移除指示器失败", e)
        }
    }
    minimizedIndicator = null
}
```

## 性能优化

### 1. 内存优化

- **释放对话框资源**：最小化时释放输入对话框的 View 资源
- **轻量级指示器**：指示器只包含必要的 UI 元素
- **及时清理**：10 分钟后自动清理已完成的指示器

### 2. 动画优化

- **硬件加速**：启用硬件加速提升动画性能
- **合理时长**：最小化/恢复 300ms，状态变化 200ms
- **避免过度绘制**：使用 `alpha` 和 `scale` 动画，避免复杂的布局变化

### 3. 持久化优化

- **异步保存**：使用 `apply()` 而非 `commit()` 异步保存数据
- **JSON 序列化**：使用 Moshi 高效序列化请求信息
- **定期清理**：清理过期的请求数据

### 4. 通知优化

- **条件发送**：只在最小化状态下发送通知
- **优先级控制**：使用 HIGH 优先级确保及时送达
- **自动取消**：点击后自动取消通知

## 安全性考虑

### 1. 数据安全

- **敏感信息保护**：请求信息中的输入文本可能包含敏感内容，需要加密存储
- **响应结果保护**：AI 响应结果也需要加密存储
- **及时清理**：恢复对话框后立即清除持久化数据

### 2. 权限安全

- **通知权限**：Android 13+ 需要请求通知权限
- **前台服务权限**：确保前台服务权限已授予
- **悬浮窗权限**：确保悬浮窗权限已授予

### 3. 异常安全

- **空指针检查**：所有可能为 null 的对象都进行检查
- **异常捕获**：所有关键操作都包裹在 try-catch 中
- **降级处理**：关键功能失败时提供降级方案


## 版本兼容性

### Android 版本支持

| Android 版本 | API Level | 兼容性 | 特殊处理 |
|-------------|-----------|---------|----------|
| Android 7.0 (Nougat) | 24 | ✅ 完全支持 | 无 |
| Android 8.0 (Oreo) | 26 | ✅ 完全支持 | 通知渠道、TYPE_APPLICATION_OVERLAY |
| Android 9.0 (Pie) | 28 | ✅ 完全支持 | 前台服务权限 |
| Android 10 (Q) | 29 | ✅ 完全支持 | 后台位置限制 |
| Android 11 (R) | 30 | ✅ 完全支持 | 软件包可见性 |
| Android 12 (S) | 31 | ✅ 完全支持 | 动态颜色、精确闹钟权限 |
| Android 13 (T) | 33 | ✅ 完全支持 | 通知权限 |
| Android 14 (U) | 34 | ✅ 完全支持 | 前台服务类型 |

### 版本特定处理

#### Android 8.0+ (API 26)

```kotlin
// 通知渠道创建
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "悬浮窗服务",
        NotificationManager.IMPORTANCE_LOW
    )
    notificationManager.createNotificationChannel(channel)
}

// 悬浮窗类型
val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
} else {
    @Suppress("DEPRECATION")
    WindowManager.LayoutParams.TYPE_PHONE
}
```

#### Android 9.0+ (API 28)

```kotlin
// 前台服务权限检查
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    if (context.checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) 
        != PackageManager.PERMISSION_GRANTED) {
        // 请求权限
    }
}
```

#### Android 13+ (API 33)

```kotlin
// 通知权限检查
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
        != PackageManager.PERMISSION_GRANTED) {
        // 请求权限
    }
}
```

## 性能指标

### 目标性能指标

| 指标 | 目标值 | 测量方法 | 优先级 |
|------|--------|----------|--------|
| 最小化动画时长 | < 300ms | PerformanceMonitor | P0 |
| 恢复动画时长 | < 300ms | PerformanceMonitor | P0 |
| 状态切换延迟 | < 50ms | Compose Tracing | P0 |
| 拖动响应时间 | < 16ms (60 FPS) | Choreographer | P0 |
| 内存占用（指示器） | < 5MB | MemoryProfiler | P1 |
| CPU 使用率（空闲） | < 1% | Profiler | P1 |
| CPU 使用率（动画） | < 5% | Profiler | P1 |
| 通知发送延迟 | < 100ms | 自定义计时 | P2 |
| 持久化写入时间 | < 50ms | 自定义计时 | P2 |

### 性能监控实现

```kotlin
class MinimizePerformanceMonitor {
    
    private val metrics = mutableMapOf<String, Long>()
    
    // 记录动画开始
    fun startAnimation(type: String) {
        metrics["${type}_start"] = System.currentTimeMillis()
    }
    
    // 记录动画结束
    fun endAnimation(type: String) {
        val startTime = metrics["${type}_start"] ?: return
        val duration = System.currentTimeMillis() - startTime
        android.util.Log.d(TAG, "$type 动画时长: ${duration}ms")
        
        // 检查是否超过目标值
        if (duration > 300) {
            android.util.Log.w(TAG, "$type 动画超时: ${duration}ms > 300ms")
        }
    }
    
    // 记录内存使用
    fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        android.util.Log.d(TAG, "内存使用: ${usedMemory}MB")
    }
    
    companion object {
        private const val TAG = "MinimizePerformance"
    }
}
```

## 特殊设备适配

### 折叠屏设备

```kotlin
// 检测折叠状态
class FoldableDeviceAdapter(private val context: Context) {
    
    fun isFoldableDevice(): Boolean {
        return try {
            val windowManager = context.getSystemService(WindowManager::class.java)
            val displayFeatures = windowManager.currentWindowMetrics.windowInsets
            // 检测是否有折叠特征
            false  // 简化实现
        } catch (e: Exception) {
            false
        }
    }
    
    fun adjustIndicatorPosition(x: Int, y: Int): Pair<Int, Int> {
        // 根据折叠状态调整位置
        return Pair(x, y)
    }
}
```

### 低内存设备

```kotlin
// 低内存设备检测
class LowMemoryDeviceAdapter {
    
    fun isLowMemoryDevice(): Boolean {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // 可用内存小于 512MB 视为低内存设备
        return memoryInfo.totalMem < 512 * 1024 * 1024
    }
    
    fun getOptimizedConfig(): MinimizedIndicatorConfig {
        return if (isLowMemoryDevice()) {
            // 低内存设备使用简化配置
            MinimizedIndicatorConfig(
                size = 48.dp,  // 更小的尺寸
                animationDuration = 200,  // 更短的动画
                enableHapticFeedback = false  // 禁用触觉反馈
            )
        } else {
            // 正常配置
            MinimizedIndicatorConfig()
        }
    }
}
```

### 高刷新率屏幕

```kotlin
// 高刷新率屏幕适配
class HighRefreshRateAdapter(private val context: Context) {
    
    fun getRefreshRate(): Float {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(WindowManager::class.java)).defaultDisplay
        }
        return display?.refreshRate ?: 60f
    }
    
    fun getOptimizedAnimationDuration(): Int {
        val refreshRate = getRefreshRate()
        return when {
            refreshRate >= 120f -> 250  // 高刷新率屏幕使用更短动画
            refreshRate >= 90f -> 275
            else -> 300  // 60Hz 屏幕使用标准动画
        }
    }
}
```

## 从传统 View 到 Compose 的迁移指南

### 迁移步骤

#### 第一阶段：准备工作

1. **添加 Compose 依赖**
   ```kotlin
   // build.gradle.kts
   implementation(libs.androidx.compose.ui)
   implementation(libs.androidx.compose.material3)
   implementation(libs.androidx.activity.compose)
   ```

2. **创建 ViewModel**
   - 创建 `FloatingWindowViewModel`
   - 定义 `MinimizedIndicatorUiState`
   - 实现状态管理逻辑

3. **创建 Composable 组件**
   - 创建 `MinimizedIndicator` Composable
   - 实现状态接口和实现类
   - 添加动画效果

#### 第二阶段：集成到 Service

1. **修改 FloatingWindowService**
   ```kotlin
   // 添加 ComposeView 支持
   private var indicatorComposeView: ComposeView? = null
   
   private fun showMinimizedIndicator() {
       val composeView = ComposeView(this).apply {
           setContent {
               val uiState by viewModel.uiState.collectAsState()
               if (uiState.minimizedIndicator.isVisible) {
                   MinimizedIndicator(
                       state = uiState.minimizedIndicator.state,
                       onPositionChanged = { x, y ->
                           viewModel.updateIndicatorPosition(x, y)
                       },
                       onClick = {
                           viewModel.restoreDialog()
                       }
                   )
               }
           }
       }
       windowManager.addView(composeView, params)
       indicatorComposeView = composeView
   }
   ```

2. **更新最小化/恢复逻辑**
   - 使用 ViewModel 方法替代直接操作 View
   - 通过 StateFlow 观察状态变化
   - 移除传统 View 的回调函数

#### 第三阶段：测试和验证

1. **单元测试**
   - 测试 ViewModel 状态管理
   - 测试状态转换逻辑

2. **UI 测试**
   - 使用 Compose UI Test 测试组件
   - 验证动画效果
   - 测试拖动交互

3. **集成测试**
   - 测试完整的最小化/恢复流程
   - 验证通知功能
   - 测试应用重启恢复

#### 第四阶段：清理和优化

1. **移除旧代码**
   - 删除 `MinimizedIndicatorView` 类（如果存在）
   - 删除相关的 XML 布局文件
   - 清理未使用的回调接口

2. **性能优化**
   - 使用 Compose Compiler Metrics 分析性能
   - 优化重组次数
   - 确保动画流畅

### 兼容性注意事项

1. **保持 API 兼容**
   - 公开接口保持不变
   - 内部实现可以改变

2. **渐进式迁移**
   - 先迁移最小化指示器
   - 保持其他部分不变
   - 逐步迁移其他组件

3. **回退方案**
   - 保留旧代码作为备份
   - 提供配置开关
   - 确保可以快速回退

