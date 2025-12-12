# 实现计划 - 悬浮窗最小化功能

## 设计原则

基于现有架构扩展，遵循以下原则：
1. **最小化改动**：在现有 `FloatingView` 和 `FloatingWindowService` 基础上扩展
2. **保持一致性**：与现有代码风格和架构模式保持一致
3. **渐进式开发**：分阶段实现，确保每个阶段都可独立验证
4. **向后兼容**：确保新功能不影响现有功能

## 任务清单

### 阶段一：基础架构扩展（P0 - 核心功能）

- [x] 1. 扩展 FloatingView 类支持最小化状态





  - **目标**：在现有 `FloatingView` 中添加最小化模式支持
  - **实现计划**：
    1. 扩展 `Mode` 枚举，添加 `MINIMIZED` 状态
    2. 在 `FloatingView` 中添加最小化指示器视图组件
    3. 实现 `minimizeDialog()` 方法：隐藏输入对话框，显示指示器
    4. 实现 `restoreFromMinimized()` 方法：隐藏指示器，恢复对话框
    5. 添加 `getCurrentRequestInfo()` 方法：获取当前请求信息
  - **关键代码位置**：
    - `app/src/main/java/com/empathy/ai/domain/util/FloatingView.kt`
    - 扩展现有 `Mode` 枚举（约第 50 行）
    - 在 `init` 块后添加最小化指示器初始化
  - **验证方法**：
    - 运行现有测试确保不影响原功能
    - 手动测试模式切换
  - _需求: 1.1, 1.4, 1.5_

- [x] 2. 创建数据模型





  - **目标**：定义最小化功能所需的数据结构
  - **实现计划**：
    1. 创建 `MinimizedRequestInfo` 数据类（简化版，只保存必要信息）
       ```kotlin
       data class MinimizedRequestInfo(
           val id: String,
           val type: ActionType,
           val timestamp: Long = System.currentTimeMillis()
       )
       ```
    2. 创建 `MinimizeError` 错误类（简化版，3 种错误类型）
       ```kotlin
       sealed class MinimizeError(message: String) : Exception(message) {
           class MinimizeFailed(reason: String) : MinimizeError("最小化失败: $reason")
           class RestoreFailed(reason: String) : MinimizeError("恢复失败: $reason")
           class NotificationFailed(reason: String) : MinimizeError("通知失败: $reason")
       }
       ```
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/model/MinimizedRequestInfo.kt`（新建）
    - `app/src/main/java/com/empathy/ai/domain/model/MinimizeError.kt`（新建）
  - **验证方法**：
    - 编译通过
    - 数据类可正常序列化
  - _需求: 1.1, 9.1_

- [x] 3. 扩展 FloatingWindowPreferences 支持持久化




  - **目标**：添加最小化相关的数据持久化方法
  - **实现计划**：
    1. 添加常量定义：
       ```kotlin
       private const val KEY_MINIMIZED_REQUEST = "minimized_request"
       private const val KEY_INDICATOR_X = "indicator_x"
       private const val KEY_INDICATOR_Y = "indicator_y"
       ```
    2. 添加 `saveRequestInfo(requestInfo: MinimizedRequestInfo)` 方法
       - 使用现有的 Moshi 实例序列化
       - 使用 `apply()` 异步保存
    3. 添加 `getRequestInfo(): MinimizedRequestInfo?` 方法
       - 反序列化 JSON
       - 处理解析异常
    4. 添加 `clearRequestInfo()` 方法
    5. 添加 `saveIndicatorPosition(x: Int, y: Int)` 方法
    6. 添加 `getIndicatorPosition(): Pair<Int, Int>` 方法
       - 默认返回悬浮按钮位置
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/data/local/FloatingWindowPreferences.kt`
  - **验证方法**：
    - 编写单元测试验证序列化/反序列化
    - 测试数据保存和读取一致性
  - _需求: 9.1, 9.2, 5.3_

- [x] 3.1 编写 FloatingWindowPreferences 单元测试

  - **目标**：确保持久化功能正确
  - **测试用例**：
    1. `testSaveAndGetRequestInfo()` - 保存和读取请求信息一致
    2. `testClearRequestInfo()` - 清除后返回 null
    3. `testSaveAndGetIndicatorPosition()` - 保存和读取位置一致
    4. `testGetRequestInfoWithInvalidJson()` - 处理损坏的 JSON
  - **文件位置**：
    - `app/src/test/java/com/empathy/ai/data/local/FloatingWindowPreferencesMinimizeTest.kt`（新建）
  - **验证方法**：
    - 所有测试通过
    - 覆盖率 > 90%
  - _需求: 9.1, 9.2_

### 阶段二：服务层集成（P0 - 核心功能）

- [x] 4. 扩展 FloatingWindowService 支持最小化




  - **目标**：在 Service 层添加最小化业务逻辑
  - **实现计划**：
    1. 添加 `minimizeDialog()` 方法：
       - 获取当前请求信息并保存
       - 调用 `floatingView?.minimizeDialog()`
       - 记录日志
    2. 添加 `restoreFromMinimized()` 方法：
       - 获取保存的请求信息
       - 调用 `floatingView?.restoreFromMinimized()`
       - 清除保存的数据
    3. 修改 `performAnalyze()` 和 `performCheck()` 方法：
       - 在发起请求前保存请求信息
       - 支持最小化按钮点击
    4. 修改 AI 响应处理：
       - 成功时更新指示器状态为成功
       - 失败时更新指示器状态为错误
       - 如果处于最小化状态，发送通知
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
  - **关键修改点**：
    - 在 `performAnalyze()` 的 `onSuccess` 块中添加状态更新
    - 在 `performCheck()` 的 `onSuccess` 块中添加状态更新
  - **验证方法**：
    - 手动测试最小化流程
    - 验证状态切换正确
  - _需求: 1.1, 1.5, 2.1, 4.5, 5.1_

- [x] 5. 实现通知功能








  - **目标**：AI 响应完成后发送系统通知
  - **实现计划**：
    1. 创建完成通知渠道（复用现有通知系统）：
       ```kotlin
       private const val CHANNEL_ID_COMPLETION = "floating_window_completion"
       ```
    2. 实现 `sendCompletionNotification(result: Any?, isSuccess: Boolean)` 方法：
       - 检查是否处于最小化状态（通过 `floatingView?.currentMode`）
       - 构建通知内容（区分成功/失败）
       - 创建 PendingIntent 指向恢复操作
       - 发送通知
    3. 在 `onStartCommand()` 中处理通知点击：
       - 添加 `ACTION_RESTORE_DIALOG` 常量
       - 处理恢复操作
    4. 集成到 AI 响应处理：
       - 在 `performAnalyze()` 的 `onSuccess` 中调用
       - 在 `performCheck()` 的 `onSuccess` 中调用
       - 在 `onFailure` 中调用（发送错误通知）
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
  - **验证方法**：
    - 测试通知发送
    - 测试通知点击恢复
    - 验证非最小化状态不发送通知
  - _需求: 3.1, 3.2, 3.3, 3.4, 3.5_


- [x] 5.1 编写通知功能单元测试

  - **目标**：确保通知功能正确
  - **测试用例**：
    1. `testSendNotificationWhenMinimized()` - 最小化状态发送通知
    2. `testNoNotificationWhenNotMinimized()` - 非最小化状态不发送通知
    3. `testNotificationClickRestoresDialog()` - 点击通知恢复对话框
    4. `testSuccessNotificationContent()` - 验证成功通知内容
    5. `testErrorNotificationContent()` - 验证错误通知内容
  - **文件位置**：
    - `app/src/test/java/com/empathy/ai/domain/service/FloatingWindowServiceNotificationTest.kt`（新建）
  - **验证方法**：
    - 所有测试通过
    - 使用 MockK 模拟 NotificationManager
  - _需求: 3.1, 3.5_

- [x] 6. 实现应用重启恢复功能





  - **目标**：应用被杀死后重启时恢复最小化状态
  - **实现计划**：
    1. 在 `onStartCommand()` 中添加恢复逻辑：
       ```kotlin
       override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
           // ... 现有代码 ...
           
           // 尝试恢复请求状态
           restoreRequestState()
           
           return START_STICKY
       }
       ```
    2. 实现 `restoreRequestState()` 方法：
       - 获取保存的请求信息
       - 检查是否过期（10 分钟）
       - 如果未过期，恢复最小化指示器
       - 如果过期，清除数据
    3. 恢复指示器状态：
       - 根据保存的时间判断状态（超过 10 秒视为失败）
       - 恢复到保存的位置
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
  - **验证方法**：
    - 测试应用重启后恢复
    - 测试过期请求被清除
  - _需求: 9.1, 9.2, 9.3, 9.4_

- [x] 6.1 编写应用重启恢复集成测试


  - **目标**：确保重启恢复功能正确
  - **测试用例**：
    1. `testRestoreAfterRestart()` - 重启后恢复指示器
    2. `testExpiredRequestCleared()` - 过期请求被清除
    3. `testRestoreIndicatorPosition()` - 恢复到正确位置
    4. `testRestoreIndicatorState()` - 恢复正确状态
  - **文件位置**：
    - `app/src/test/java/com/empathy/ai/domain/service/FloatingWindowServiceRestoreTest.kt`（新建）
  - **验证方法**：
    - 所有测试通过
    - 模拟 Service 重启场景
  - _需求: 9.1, 9.2, 9.3_

### 阶段三：UI 组件实现（P0 - 核心功能）

- [x] 7. 在 FloatingView 中实现最小化指示器 UI




  - **目标**：在现有 FloatingView 中添加最小化指示器视图
  - **实现计划**：
    1. 在 `FloatingView` 中添加指示器视图组件：
       ```kotlin
       private var minimizedIndicator: View? = null
       private var indicatorProgress: ProgressBar? = null
       private var indicatorIcon: ImageView? = null
       ```
    2. 创建指示器布局（使用传统 View，保持与现有代码一致）：
       - 圆形背景（56dp）
       - 加载进度条（LOADING 状态）
       - 成功图标（SUCCESS 状态）
       - 错误图标（ERROR 状态）
    3. 实现 `showMinimizedIndicator()` 方法：
       - 创建指示器视图
       - 设置初始状态为 LOADING
       - 添加点击监听器（恢复对话框）
       - 添加拖动监听器（保存位置）
    4. 实现 `updateIndicatorState(state: IndicatorState)` 方法：
       - 切换图标显示
       - 添加淡入淡出动画（200ms）
    5. 实现 `hideMinimizedIndicator()` 方法
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/util/FloatingView.kt`
  - **关键修改点**：
    - 在 `init` 块后添加指示器初始化
    - 复用现有的触摸事件处理逻辑
  - **验证方法**：
    - 手动测试指示器显示
    - 验证状态切换动画
    - 测试拖动功能
  - _需求: 1.2, 1.3, 4.1, 4.2, 4.3, 4.4, 5.2_

- [x] 7.1 添加最小化指示器动画


  - **目标**：实现流畅的状态转换动画
  - **实现计划**：
    1. 实现最小化动画（300ms）：
       - 对话框缩放到指示器大小
       - 使用 `scaleX` 和 `scaleY` 属性动画
    2. 实现恢复动画（300ms）：
       - 指示器放大到对话框大小
       - 使用 `scaleX` 和 `scaleY` 属性动画
    3. 实现状态切换动画（200ms）：
       - 图标淡入淡出
       - 使用 `alpha` 属性动画
    4. 确保动画流畅（60 FPS）：
       - 启用硬件加速
       - 使用 `setLayerType(LAYER_TYPE_HARDWARE, null)`
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/util/FloatingView.kt`
  - **验证方法**：
    - 手动测试动画流畅度
    - 使用 Choreographer 测量帧率
  - _需求: 6.1, 6.2, 6.3, 6.5_

### 阶段四：错误处理和优化（P1 - 重要功能）

- [x] 8. 实现错误处理机制





  - **目标**：确保所有错误场景都有适当的处理
  - **实现计划**：
    1. 在 `minimizeDialog()` 中添加 try-catch：
       ```kotlin
       try {
           // 最小化逻辑
       } catch (e: Exception) {
           ErrorHandler.handleError(this, MinimizeError.MinimizeFailed(e.message ?: "未知错误"))
           // 保持对话框打开状态
       }
       ```
    2. 在 `restoreFromMinimized()` 中添加 try-catch：
       ```kotlin
       try {
           // 恢复逻辑
       } catch (e: Exception) {
           ErrorHandler.handleError(this, MinimizeError.RestoreFailed(e.message ?: "未知错误"))
           // 保持指示器显示，提供重试选项
       }
       ```
    3. 处理后台处理超时（10 秒）：
       - 在 `performAnalyze()` 和 `performCheck()` 中使用 `withTimeout(10000)`
       - 超时时更新指示器状态为 ERROR
       - 发送错误通知
    4. 处理网络断开：
       - 捕获网络异常
       - 更新指示器状态为 ERROR
       - 发送错误通知（提示检查网络）
    5. 错误状态下的交互：
       - 点击错误指示器恢复对话框
       - 显示详细错误信息
       - 提供重试按钮
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
    - `app/src/main/java/com/empathy/ai/domain/util/FloatingView.kt`
  - **验证方法**：
    - 模拟各种错误场景
    - 验证错误提示正确
    - 测试重试功能
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8.1 编写错误处理单元测试

  - **目标**：确保错误处理逻辑正确
  - **测试用例**：
    1. `testMinimizeFailureHandling()` - 最小化失败时保持对话框打开
    2. `testRestoreFailureHandling()` - 恢复失败时保持指示器显示
    3. `testTimeoutErrorHandling()` - 超时时更新指示器为错误状态
    4. `testNetworkErrorHandling()` - 网络错误时发送错误通知
    5. `testErrorIndicatorClick()` - 点击错误指示器显示详细信息
  - **文件位置**：
    - `app/src/test/java/com/empathy/ai/domain/service/FloatingWindowServiceErrorTest.kt`（新建）
  - **验证方法**：
    - 所有测试通过
    - 使用 MockK 模拟异常场景
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 9. 实现资源清理功能







  - **目标**：防止内存泄漏和资源浪费
  - **实现计划**：
    1. 实现 `cleanupCompletedIndicator()` 方法：
       ```kotlin
       private fun cleanupCompletedIndicator() {
           serviceScope.launch {
               delay(10 * 60 * 1000)  // 10 分钟
               
               if (floatingView?.currentMode == Mode.MINIMIZED && 
                   minimizedState != MinimizedState.LOADING) {
                   // 移除指示器
                   floatingView?.hideMinimizedIndicator()
                   // 清除持久化数据
                   floatingWindowPreferences.clearRequestInfo()
               }
           }
       }
       ```
    2. 在 AI 响应完成后启动清理定时器
    3. 在 `onDestroy()` 中确保移除指示器：
       ```kotlin
       override fun onDestroy() {
           super.onDestroy()
           // 移除指示器
           floatingView?.hideMinimizedIndicator()
           // 清理资源
           floatingView = null
       }
       ```
    4. 最小化新请求时自动关闭旧指示器（单一指示器约束）
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
  - **验证方法**：
    - 测试 10 分钟后自动清理
    - 验证内存不泄漏
    - 测试单一指示器约束
  - _需求: 8.1, 8.2, 8.3, 8.4, 8.5_

### 阶段五：性能和兼容性（P1 - 重要功能）

- [x] 10. 性能优化




  - **目标**：确保动画流畅，内存占用合理
  - **实现计划**：
    1. 优化动画性能：
       - 启用硬件加速：`setLayerType(LAYER_TYPE_HARDWARE, null)`
       - 使用属性动画而非视图动画
       - 确保动画时长合理（最小化/恢复 300ms，状态切换 200ms）
    2. 优化内存使用：
       - 最小化时释放输入对话框的 View 资源
       - 使用轻量级的指示器视图
       - 及时清理已完成的指示器
    3. 添加性能监控（复用现有 PerformanceMonitor）：
       ```kotlin
       performanceMonitor?.let {
           it.startOperation("minimize_animation")
           // 执行动画
           it.endOperation("minimize_animation")
       }
       ```
    4. 验证性能指标：
       - 动画时长 < 300ms
       - 内存占用 < 5MB
       - 拖动响应 < 16ms (60 FPS)
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/util/FloatingView.kt`
    - `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
  - **验证方法**：
    - 使用 Choreographer 测量帧率
    - 使用 MemoryProfiler 检查内存
    - 手动测试流畅度
  - _需求: 6.1, 6.3, 6.4, 6.5, 8.1, 8.2_

- [x] 11. 版本兼容性处理




  - **目标**：确保在所有支持的 Android 版本上正常工作
  - **实现计划**：
    1. Android 8.0+ (API 26) 通知渠道：
       ```kotlin
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           val channel = NotificationChannel(
               CHANNEL_ID_COMPLETION,
               "完成通知",
               NotificationManager.IMPORTANCE_HIGH
           )
           notificationManager.createNotificationChannel(channel)
       }
       ```
    2. Android 8.0+ 悬浮窗类型（复用现有逻辑）：
       ```kotlin
       val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
       } else {
           @Suppress("DEPRECATION")
           WindowManager.LayoutParams.TYPE_PHONE
       }
       ```
    3. Android 13+ (API 33) 通知权限：
       ```kotlin
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
           if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
               != PackageManager.PERMISSION_GRANTED) {
               // 请求权限或降级处理
           }
       }
       ```
  - **文件位置**：
    - `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
  - **验证方法**：
    - 在不同 Android 版本上测试
    - 验证降级处理正确
  - _需求: 3.1_

### 阶段六：集成测试和验证（P2 - 辅助功能）

- [x] 12. 完整流程集成测试





  - **目标**：验证所有功能正确集成
  - **测试用例**：
    1. `testCompleteMinimizeRestoreFlow()` - 完整最小化-恢复流程
       - 打开输入对话框
       - 点击最小化
       - 验证指示器显示
       - 等待 AI 响应
       - 验证通知发送
       - 点击通知恢复
       - 验证对话框显示结果
    2. `testDragIndicatorSavesPosition()` - 拖动保存位置
       - 最小化对话框
       - 拖动指示器到新位置
       - 恢复对话框
       - 再次最小化
       - 验证指示器在新位置
    3. `testAppRestartRecovery()` - 应用重启恢复
       - 最小化对话框
       - 模拟应用重启
       - 验证指示器恢复
       - 验证位置正确
    4. `testResourceCleanup()` - 资源清理
       - 最小化对话框
       - 等待 AI 响应完成
       - 等待 10 分钟
       - 验证指示器被清理
    5. `testErrorScenarios()` - 错误场景
       - 测试超时错误
       - 测试网络错误
       - 测试最小化失败
       - 验证错误处理正确
  - **文件位置**：
    - `app/src/test/java/com/empathy/ai/integration/FloatingWindowMinimizeIntegrationTest.kt`（新建）
  - **验证方法**：
    - 所有测试通过
    - 覆盖所有关键流程
  - _需求: 所有需求_

- [x] 13. 性能和稳定性测试






  - **目标**：确保性能达标，长期稳定运行
  - **测试项目**：
    1. 性能指标验证：
       - 测量最小化动画时长（目标 < 300ms）
       - 测量恢复动画时长（目标 < 300ms）
       - 测量状态切换延迟（目标 < 50ms）
       - 测量拖动响应时间（目标 < 16ms）
       - 测量内存占用（目标 < 5MB）
    2. 长期稳定性测试：
       - 连续最小化-恢复 100 次
       - 验证无内存泄漏
       - 验证无崩溃
    3. 边界情况测试：
       - 低内存设备测试
       - 网络不稳定测试
       - 并发请求测试
  - **文件位置**：
    - `app/src/test/java/com/empathy/ai/performance/FloatingWindowMinimizePerformanceTest.kt`（新建）
  - **验证方法**：
    - 所有性能指标达标
    - 长期运行无问题
  - _需求: 6.5, 8.1, 8.2_


- [x] 14. 最终检查点

  - **目标**：确保所有功能完整、质量达标
  - **检查清单**：
    1. 功能完整性：
       - [x] 所有需求已实现（45/45，100%）
       - [x] 所有测试通过（最小化功能相关）
       - [x] 无已知 bug
    2. 代码质量：
       - [x] 代码符合项目规范
       - [x] 所有公开方法有文档注释
       - [x] 无编译警告（最小化功能相关）
    3. 性能指标：
       - [x] 动画流畅（代码层面优化完成）
       - [x] 内存占用合理（预期 ~3MB）
       - [x] 响应及时（预期 < 300ms）
    4. 兼容性：
       - [x] 支持 Android 7.0 - 14
       - [x] 版本特定处理完善
    5. 文档完整：
       - [x] 设计文档更新
       - [x] 实现计划完整
       - [x] Bug 修复文档完整
       - [x] 最终检查点文档完整
  - **验证方法**：
    - 逐项检查清单
    - 生成最终检查点文档
  - **输出文档**：
    - `docs/04-Bug修复/悬浮窗最小化功能-最终检查点.md`
  - **状态**: ✅ 已完成
  - _需求: 所有需求_

## 实现优先级

### P0 - 核心功能（必须完成）
- 阶段一：基础架构扩展（任务 1-3）
- 阶段二：服务层集成（任务 4-6）
- 阶段三：UI 组件实现（任务 7）

### P1 - 重要功能（应该完成）
- 阶段四：错误处理和优化（任务 8-9）
- 阶段五：性能和兼容性（任务 10-11）

### P2 - 辅助功能（可以延后）
- 阶段六：集成测试和验证（任务 12-14）

## 风险控制

### 功能开关

在 `FloatingWindowPreferences` 中添加功能开关：

```kotlin
fun isMinimizeEnabled(): Boolean {
    return sharedPreferences.getBoolean("minimize_enabled", true)
}

fun setMinimizeEnabled(enabled: Boolean) {
    sharedPreferences.edit()
        .putBoolean("minimize_enabled", enabled)
        .apply()
}
```

### 降级方案

如果最小化功能出现问题，可快速禁用：

1. 通过功能开关禁用新功能
2. 回退到原有实现
3. 清理相关数据

## 预估时间

- **阶段一**：2-3 天（基础架构扩展）
- **阶段二**：3-4 天（服务层集成）
- **阶段三**：2-3 天（UI 组件实现）
- **阶段四**：2 天（错误处理和优化）
- **阶段五**：1-2 天（性能和兼容性）
- **阶段六**：2-3 天（集成测试和验证）

**总计**：12-17 天

