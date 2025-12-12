# 设计文档（MVP 版本）

**文档版本**：v1.0  
**创建日期**：2025-12-07  
**最后更新**：2025-12-07  
**作者**：hushaokang  
**状态**：待审核

## 变更记录

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| v1.0 | 2025-12-07 | hushaokang | 初始版本 |

## 术语表

| 术语 | 定义 |
|------|------|
| FloatingWindowService | Android 前台服务，管理悬浮窗的生命周期 |
| WindowManager | Android 系统服务，用于添加和管理窗口视图 |
| TYPE_APPLICATION_OVERLAY | Android 8.0+ 的悬浮窗类型，需要特殊权限 |
| 前台服务 | 显示持久通知的服务，不易被系统杀死 |
| 边缘吸附 | 悬浮按钮自动移动到屏幕边缘的行为 |

## 概述

本文档描述了共情 AI 助手 Android 系统服务层的技术设计（MVP 版本）。本设计专注于实现核心功能，采用简化的架构以确保快速交付。

### 设计目标

- **快速交付**：2 周内完成开发和测试
- **功能完整**：支持悬浮窗触发 AI 分析和安全检查
- **用户友好**：简单直观的交互流程
- **性能可控**：内存占用 < 150MB，响应时间 < 10s
- **稳定可靠**：基本的错误处理和兼容性保证

### MVP 版本特点

- ✅ 仅实现悬浮窗服务（FloatingWindowService）
- ✅ 用户手动输入聊天内容（不自动抓取）
- ✅ 专注微信兼容性
- ✅ 简化错误处理和恢复机制
- ✅ 基础的单元测试覆盖

## 架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌──────────────────────────────────────────────────┐  │
│  │           SettingsScreen (设置界面)               │  │
│  │  - 启用/禁用悬浮窗开关                            │  │
│  │  - 权限状态显示                                   │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │ 启动/停止服务
                         ↓
┌─────────────────────────────────────────────────────────┐
│                    Service Layer                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │        FloatingWindowService (前台服务)          │  │
│  │  ┌────────────────────────────────────────────┐ │  │
│  │  │  FloatingView (统一悬浮视图)               │ │  │
│  │  │  - 悬浮按钮模式                            │ │  │
│  │  │  - 菜单展开模式                            │ │  │
│  │  │  - 输入对话框模式                          │ │  │
│  │  └────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │ 调用 UseCase
                         ↓
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  AnalyzeChatUseCase (聊天分析用例)               │  │
│  │  CheckDraftUseCase (草稿检查用例)                │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 组件和接口

### 1. FloatingWindowService

**职责**：管理悬浮窗的生命周期和视图

**关键方法**：

```kotlin
@AndroidEntryPoint
class FloatingWindowService : Service() {
    
    @Inject
    lateinit var analyzeChatUseCase: AnalyzeChatUseCase
    
    @Inject
    lateinit var checkDraftUseCase: CheckDraftUseCase
    
    @Inject
    lateinit var contactRepository: ContactRepository
    
    private lateinit var windowManager: WindowManager
    private var floatingView: FloatingView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        showFloatingView()
        return START_STICKY
    }
    
    private fun showFloatingView() {
        if (floatingView == null) {
            floatingView = FloatingView(this).apply {
                onAnalyzeClick = { handleAnalyze() }
                onCheckClick = { handleCheck() }
            }
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            
            windowManager.addView(floatingView, params)
        }
    }
    
    private fun handleAnalyze() {
        serviceScope.launch {
            try {
                val contacts = contactRepository.getAllProfiles().first()
                floatingView?.showInputDialog(
                    actionType = ActionType.ANALYZE,
                    contacts = contacts,
                    onConfirm = { contactId, text ->
                        performAnalyze(contactId, text)
                    }
                )
            } catch (e: Exception) {
                showError("加载联系人失败：${e.message}")
            }
        }
    }
    
    private fun performAnalyze(contactId: String, text: String) {
        serviceScope.launch {
            try {
                floatingView?.showLoading()
                val result = withTimeout(10000) {
                    analyzeChatUseCase(contactId, listOf(text))
                }
                result.onSuccess {
                    showSuccess("分析完成")
                    // TODO: 显示分析结果
                }.onFailure {
                    showError("分析失败：${it.message}")
                }
            } catch (e: TimeoutCancellationException) {
                showError("操作超时，请检查网络连接")
            } finally {
                floatingView?.hideLoading()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        floatingView = null
        serviceScope.cancel()
    }
    
    private fun createNotification(): Notification {
        val channelId = "floating_window_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("共情 AI 助手")
            .setContentText("悬浮窗服务运行中")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}

enum class ActionType {
    ANALYZE,  // 帮我分析
    CHECK     // 帮我检查
}
```

### 2. FloatingView（统一悬浮视图）

**职责**：管理悬浮窗的所有显示状态

**关键方法**：

```kotlin
class FloatingView(context: Context) : FrameLayout(context) {
    
    private var currentMode = Mode.BUTTON
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    
    var onAnalyzeClick: (() -> Unit)? = null
    var onCheckClick: (() -> Unit)? = null
    
    init {
        inflate(context, R.layout.floating_view, this)
        setupButtonMode()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = (layoutParams as WindowManager.LayoutParams).x
                initialY = (layoutParams as WindowManager.LayoutParams).y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val params = layoutParams as WindowManager.LayoutParams
                params.x = initialX + (event.rawX - initialTouchX).toInt()
                params.y = initialY + (event.rawY - initialTouchY).toInt()
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .updateViewLayout(this, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (abs(event.rawX - initialTouchX) < 10 && 
                    abs(event.rawY - initialTouchY) < 10) {
                    performClick()
                } else {
                    snapToEdge()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    override fun performClick(): Boolean {
        super.performClick()
        when (currentMode) {
            Mode.BUTTON -> showMenu()
            Mode.MENU -> hideMenu()
            Mode.INPUT -> {}
        }
        return true
    }
    
    private fun snapToEdge() {
        val params = layoutParams as WindowManager.LayoutParams
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        params.x = if (params.x < screenWidth / 2) 0 else screenWidth
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .updateViewLayout(this, params)
    }
    
    fun showInputDialog(
        actionType: ActionType,
        contacts: List<ContactProfile>,
        onConfirm: (String, String) -> Unit
    ) {
        // 显示输入对话框
        currentMode = Mode.INPUT
        // TODO: 实现输入对话框UI
    }
    
    fun showLoading() {
        // 显示加载状态
    }
    
    fun hideLoading() {
        // 隐藏加载状态
    }
    
    private enum class Mode {
        BUTTON,  // 悬浮按钮模式
        MENU,    // 菜单展开模式
        INPUT    // 输入对话框模式
    }
}
```

### 3. FloatingWindowManager（工具类）

**职责**：管理悬浮窗的权限和服务状态

```kotlin
object FloatingWindowManager {
    
    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }
    
    fun startService(context: Context) {
        val intent = Intent(context, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun stopService(context: Context) {
        val intent = Intent(context, FloatingWindowService::class.java)
        context.stopService(intent)
    }
    
    const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
}
```

### 4. Hilt 依赖注入配置

**ServiceModule**：

```kotlin
@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {
    
    @Provides
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main + SupervisorJob())
    }
}
```

**注意**：FloatingWindowService 使用 `@AndroidEntryPoint` 注解，自动注入 UseCase 和 Repository。

## 数据模型

### FloatingWindowState

**用途**：保存悬浮窗的状态（用于持久化）

```kotlin
data class FloatingWindowState(
    val isEnabled: Boolean = false,
    val buttonX: Int = 0,
    val buttonY: Int = 0
)
```

**存储实现**：

```kotlin
class FloatingWindowPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(
        "floating_window_prefs",
        Context.MODE_PRIVATE
    )
    
    fun saveState(state: FloatingWindowState) {
        prefs.edit {
            putBoolean("is_enabled", state.isEnabled)
            putInt("button_x", state.buttonX)
            putInt("button_y", state.buttonY)
        }
    }
    
    fun loadState(): FloatingWindowState {
        return FloatingWindowState(
            isEnabled = prefs.getBoolean("is_enabled", false),
            buttonX = prefs.getInt("button_x", 0),
            buttonY = prefs.getInt("button_y", 0)
        )
    }
}
```

## 正确性属性

*属性是一个特征或行为，应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的正式陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### 核心属性（6 个）

**属性 1：服务生命周期一致性**
*对于任何*悬浮窗服务实例，启动服务应该显示悬浮视图，停止服务应该移除悬浮视图并清理所有资源
**验证需求：1.1, 1.5, 4.4**

**属性 2：拖动位置更新**
*对于任何*悬浮按钮，当用户拖动按钮时，按钮的位置应该随触摸点移动而更新，松手后应该吸附到最近的屏幕边缘
**验证需求：1.2, 1.3**

**属性 3：点击展开菜单**
*对于任何*悬浮按钮，点击按钮应该展开功能菜单，再次点击或点击外部应该收起菜单
**验证需求：2.1, 2.5**

**属性 4：功能触发对话框**
*对于任何*功能菜单中的操作按钮（分析或检查），点击按钮应该打开输入对话框，并加载联系人列表
**验证需求：2.3, 2.4, 3.1, 10.2**

**属性 5：权限检查流程**
*对于任何*悬浮窗功能启用请求，如果没有悬浮窗权限，系统应该引导用户授权；如果有权限，系统应该直接启动服务
**验证需求：4.1, 4.2, 5.1**

**属性 6：数据传递和隐私保护**
*对于任何*确认的输入，系统应该将联系人 ID 和文本内容传递给对应的 UseCase，并且文本应该通过 PrivacyEngine 脱敏后才发送给 AI
**验证需求：3.5, 5.4, 10.3**

## 错误处理

### 错误类型（简化版）

```kotlin
sealed class FloatingWindowError(val userMessage: String) : Exception(userMessage) {
    
    object PermissionDenied : FloatingWindowError("需要悬浮窗权限才能使用此功能")
    
    data class ServiceError(val reason: String) : FloatingWindowError("服务启动失败：$reason")
    
    data class ValidationError(val field: String) : FloatingWindowError(
        when (field) {
            "contact" -> "请选择联系人"
            "text" -> "请输入内容"
            "textLength" -> "输入内容不能超过 5000 字符"
            else -> "输入验证失败"
        }
    )
    
    data class UseCaseError(val cause: Throwable) : FloatingWindowError(
        "操作失败：${cause.message ?: "未知错误"}"
    )
}
```

### 错误处理策略

```kotlin
fun handleError(context: Context, error: FloatingWindowError) {
    when (error) {
        is FloatingWindowError.PermissionDenied -> {
            // 显示权限请求对话框
            showPermissionDialog(context)
        }
        else -> {
            // 显示 Toast 提示
            Toast.makeText(context, error.userMessage, Toast.LENGTH_SHORT).show()
        }
    }
    Log.e("FloatingWindow", "Error: ${error.message}", error)
}
```

## 测试策略

### 单元测试

**测试框架**：JUnit 4 + MockK + Robolectric

**核心测试用例**：

```kotlin
@RunWith(RobolectricTestRunner::class)
class FloatingWindowServiceTest {
    
    private lateinit var service: FloatingWindowService
    private lateinit var analyzeChatUseCase: AnalyzeChatUseCase
    private lateinit var contactRepository: ContactRepository
    
    @Before
    fun setup() {
        analyzeChatUseCase = mockk()
        contactRepository = mockk()
        service = FloatingWindowService().apply {
            this.analyzeChatUseCase = this@FloatingWindowServiceTest.analyzeChatUseCase
            this.contactRepository = this@FloatingWindowServiceTest.contactRepository
        }
    }
    
    @Test
    fun `属性1 - 服务启动后应该显示悬浮视图`() {
        service.onCreate()
        service.onStartCommand(null, 0, 0)
        
        assertNotNull(service.floatingView)
    }
    
    @Test
    fun `属性1 - 服务停止后应该移除悬浮视图`() {
        service.onCreate()
        service.onStartCommand(null, 0, 0)
        service.onDestroy()
        
        assertNull(service.floatingView)
    }
    
    @Test
    fun `属性5 - 没有权限时应该引导用户授权`() {
        val context = mockk<Context>()
        every { Settings.canDrawOverlays(context) } returns false
        
        val hasPermission = FloatingWindowManager.hasPermission(context)
        
        assertFalse(hasPermission)
    }
    
    @Test
    fun `属性6 - 输入内容应该传递给UseCase`() = runTest {
        val contactId = "test_contact"
        val text = "测试内容"
        
        coEvery { 
            analyzeChatUseCase(contactId, listOf(text)) 
        } returns Result.success(mockk())
        
        service.performAnalyze(contactId, text)
        
        coVerify { analyzeChatUseCase(contactId, listOf(text)) }
    }
}

class FloatingViewTest {
    
    private lateinit var floatingView: FloatingView
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        floatingView = FloatingView(context)
    }
    
    @Test
    fun `属性2 - 拖动应该更新位置`() {
        val initialParams = floatingView.layoutParams as WindowManager.LayoutParams
        val initialX = initialParams.x
        
        // 模拟拖动事件
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        val moveEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 200f, 200f, 0)
        
        floatingView.onTouchEvent(downEvent)
        floatingView.onTouchEvent(moveEvent)
        
        val newParams = floatingView.layoutParams as WindowManager.LayoutParams
        assertNotEquals(initialX, newParams.x)
    }
}
```

### 集成测试

**测试场景**：

1. **完整的分析流程**：
   ```kotlin
   @Test
   fun `完整分析流程测试`() = runTest {
       // 1. 启动服务
       FloatingWindowManager.startService(context)
       
       // 2. 点击悬浮按钮
       floatingView.performClick()
       
       // 3. 点击"帮我分析"
       floatingView.onAnalyzeClick?.invoke()
       
       // 4. 验证联系人列表加载
       coVerify { contactRepository.getAllProfiles() }
       
       // 5. 选择联系人并输入文本
       floatingView.showInputDialog(
           actionType = ActionType.ANALYZE,
           contacts = mockContacts,
           onConfirm = { contactId, text ->
               // 6. 验证 UseCase 调用
               coVerify { analyzeChatUseCase(contactId, listOf(text)) }
           }
       )
   }
   ```

2. **权限请求流程**：
   ```kotlin
   @Test
   fun `权限请求流程测试`() {
       // 1. 检查权限
       val hasPermission = FloatingWindowManager.hasPermission(context)
       assertFalse(hasPermission)
       
       // 2. 请求权限
       FloatingWindowManager.requestPermission(activity)
       
       // 3. 验证跳转到设置页面
       verify { activity.startActivityForResult(any(), any()) }
   }
   ```

### 手动测试清单

**微信兼容性测试**：
- [ ] 在微信聊天界面显示悬浮按钮
- [ ] 悬浮按钮可以正常拖动
- [ ] 点击悬浮按钮展开菜单
- [ ] 从微信复制内容后可以粘贴到输入框
- [ ] 微信切换到后台时悬浮按钮保持显示
- [ ] 测试微信最新 3 个版本的兼容性

**性能测试**：
- [ ] 服务运行时内存占用 < 150MB
- [ ] 点击响应时间 < 300ms
- [ ] AI 分析在 10 秒内返回结果或超时提示
- [ ] 拖动悬浮按钮时 UI 流畅（目测 60 FPS）

**错误处理测试**：
- [ ] 没有权限时显示权限请求对话框
- [ ] 未选择联系人时显示提示
- [ ] 输入内容为空时显示提示
- [ ] 网络请求失败时显示错误提示
- [ ] 超时时显示超时提示

## 性能优化

### 内存优化

1. **视图复用**：
   - FloatingView 在服务生命周期内复用，避免频繁创建
   - 使用 ViewStub 延迟加载输入对话框

2. **资源释放**：
   ```kotlin
   override fun onDestroy() {
       super.onDestroy()
       floatingView?.let { windowManager.removeView(it) }
       floatingView = null
       serviceScope.cancel()  // 取消所有协程
   }
   ```

3. **内存监控**：
   ```kotlin
   fun checkMemoryUsage(): Long {
       val runtime = Runtime.getRuntime()
       val usedMemory = runtime.totalMemory() - runtime.freeMemory()
       return usedMemory / (1024 * 1024) // MB
   }
   ```

### 响应时间优化

1. **异步操作**：
   - 所有 UseCase 调用在后台线程执行
   - 使用协程避免阻塞 UI 线程

2. **超时控制**：
   ```kotlin
   suspend fun executeWithTimeout(
       timeoutMillis: Long = 10000,
       block: suspend () -> Unit
   ) {
       withTimeout(timeoutMillis) {
           block()
       }
   }
   ```

3. **UI 流畅性保证**：
   - 拖动事件在主线程处理，避免卡顿
   - 使用硬件加速：`setLayerType(View.LAYER_TYPE_HARDWARE, null)`

## 兼容性设计

### 微信版本兼容性

**策略**：
1. 测试最新 3 个微信版本（8.0.48, 8.0.47, 8.0.46）
2. 悬浮窗功能不依赖微信内部实现，仅依赖 Android 系统 API
3. 如果微信更新导致问题，通过系统 API 仍然可以正常工作

**检测机制**：
```kotlin
fun isWeChatRunning(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val runningApps = activityManager.runningAppProcesses
    return runningApps?.any { it.processName == "com.tencent.mm" } ?: false
}
```

### 厂商 ROM 适配

**已知问题**：
- 小米 MIUI：需要在安全中心授予悬浮窗权限
- 华为 EMUI：需要在权限管理中授予悬浮窗权限
- OPPO ColorOS：需要在应用权限中授予悬浮窗权限

**适配策略**：
1. 使用标准 Android API（TYPE_APPLICATION_OVERLAY）
2. 提供详细的权限授予指引
3. 检测权限状态，引导用户手动授权

### 后台保持机制

**策略**：
1. 使用前台服务（Foreground Service）避免被系统杀死
2. 显示持久通知告知用户服务运行中
3. 使用 START_STICKY 确保服务被杀死后自动重启

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForeground(NOTIFICATION_ID, createNotification())
    return START_STICKY  // 服务被杀死后自动重启
}
```

## 安全考虑

### 权限管理

1. **最小权限原则**：
   - 只请求悬浮窗权限（SYSTEM_ALERT_WINDOW）
   - 不请求其他敏感权限

2. **权限说明**：
   ```kotlin
   fun showPermissionExplanation(context: Context) {
       AlertDialog.Builder(context)
           .setTitle("需要悬浮窗权限")
           .setMessage("悬浮窗权限用于在聊天应用上显示快捷按钮，方便您快速访问 AI 助手功能。\n\n我们承诺：\n• 不会读取您的聊天内容\n• 不会收集您的个人信息\n• 所有数据仅在本地处理")
           .setPositiveButton("去设置") { _, _ ->
               FloatingWindowManager.requestPermission(context as Activity)
           }
           .setNegativeButton("取消", null)
           .show()
   }
   ```

### 数据安全

1. **隐私保护**：
   - 所有用户输入通过 PrivacyEngine 脱敏
   - 不在本地持久化原始聊天内容

2. **数据传输**：
   - 仅在用户主动触发时发送数据
   - 使用 HTTPS 加密传输（由 Retrofit 配置）

## 部署考虑

### Android 版本兼容性

- **最低版本**：Android 8.0（API 26）
- **目标版本**：Android 14（API 34）
- **兼容性处理**：
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
  } else {
      context.startService(intent)
  }
  ```

### AndroidManifest.xml 配置

```xml
<manifest>
    <!-- 悬浮窗权限 -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    
    <!-- 前台服务权限 -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    
    <application>
        <!-- 悬浮窗服务 -->
        <service
            android:name=".domain.service.FloatingWindowService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync" />
    </application>
</manifest>
```

## 总结

本设计文档描述了共情 AI 助手 Android 系统服务层的 MVP 版本实现方案。通过采用简化的架构和清晰的组件划分，我们能够在 2 周内完成开发并确保核心功能的稳定性。

**关键设计决策**：
1. ✅ 统一悬浮视图（FloatingView）简化组件层级
2. ✅ 使用 Hilt 依赖注入简化代码
3. ✅ 6 个核心正确性属性覆盖关键功能
4. ✅ 基础单元测试 + 集成测试 + 手动测试
5. ✅ 明确的性能优化和兼容性策略

**交付时间线**：
- Week 1：实现核心组件和基础功能
- Week 2：测试、优化和 Bug 修复
