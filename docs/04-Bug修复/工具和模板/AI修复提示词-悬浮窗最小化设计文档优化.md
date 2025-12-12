# AI 修复提示词 - 悬浮窗最小化设计文档优化

## 角色定义

你是一位资深的 Android 架构师，专精于 Jetpack Compose 和 Clean Architecture。你的任务是根据评估结果优化悬浮窗最小化功能的设计文档，使其与项目技术栈和架构规范保持一致。

## 项目背景

**项目名称**：共情 AI 助手
**技术栈**：
- UI：Jetpack Compose + Material 3
- 架构：Clean Architecture + MVVM + Hilt
- 异步：Kotlin Coroutines + StateFlow
- 数据：Room + Retrofit + Moshi
- 测试：JUnit + MockK + Compose UI Test

**当前问题**：设计文档使用传统 View 系统，与项目的 Compose 架构不一致

## 优化目标

1. **统一 UI 架构**：将传统 View 改为 Compose 组件
2. **现代化状态管理**：使用 StateFlow 替代回调函数
3. **简化过度设计**：精简状态持久化机制
4. **增强可扩展性**：消除硬编码，支持自定义配置
5. **完善文档**：补充版本兼容性和性能指标

## 具体修改要求

### 1. UI 架构统一化（高优先级）

**当前问题**：
- MinimizedIndicatorView 使用 FrameLayout 和传统 View
- UI 结构使用 XML 布局文件
- 动画使用传统 View 动画系统

**修改要求**：
```kotlin
// 替换传统 View 为 Compose 组件
@Composable
fun MinimizedIndicator(
    state: MinimizedIndicatorState,
    onPositionChanged: (Int, Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用 Compose 实现
    Box(
        modifier = modifier
            .size(56.dp)
            .background(
                color = when (state) {
                    MinimizedIndicatorState.LOADING -> MaterialTheme.colorScheme.primary
                    MinimizedIndicatorState.SUCCESS -> MaterialTheme.colorScheme.primary
                    MinimizedIndicatorState.ERROR -> MaterialTheme.colorScheme.error
                },
                shape = CircleShape
            )
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectDragGestures { onDragGestures(it, onPositionChanged) }
            }
    ) {
        when (state) {
            MinimizedIndicatorState.LOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
            }
            MinimizedIndicatorState.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "完成",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            MinimizedIndicatorState.ERROR -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "错误",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
```

### 2. 状态管理现代化（中优先级）

**当前问题**：
- 使用传统回调函数（onClickListener、onPositionChanged）
- 没有使用 StateFlow 进行状态管理
- 状态变化通知不够响应式

**修改要求**：
```kotlin
// 创建 UiState 数据类
data class MinimizedIndicatorUiState(
    val isVisible: Boolean = false,
    val state: MinimizedIndicatorState = MinimizedIndicatorState.LOADING,
    val position: Pair<Int, Int> = Pair(0, 0),
    val isAnimating: Boolean = false
)

// 在 ViewModel 中管理状态
class FloatingWindowViewModel @Inject constructor(
    // 依赖注入
) : ViewModel() {
    
    private val _minimizedIndicatorUiState = MutableStateFlow(MinimizedIndicatorUiState())
    val minimizedIndicatorUiState: StateFlow<MinimizedIndicatorUiState> = _minimizedIndicatorUiState.asStateFlow()
    
    fun updateIndicatorState(newState: MinimizedIndicatorState) {
        _minimizedIndicatorUiState.update { it.copy(state = newState) }
    }
    
    fun updateIndicatorPosition(x: Int, y: Int) {
        _minimizedIndicatorUiState.update { it.copy(position = Pair(x, y)) }
    }
}
```

### 3. 简化过度设计（低优先级）

**当前问题**：
- RequestInfo 数据模型过于复杂
- 状态持久化机制过于详细
- 错误类型分类过于细致

**修改要求**：
```kotlin
// 简化请求信息数据模型
data class MinimizedRequestInfo(
    val id: String,                    // 请求唯一标识
    val type: ActionType,               // 操作类型
    val timestamp: Long = System.currentTimeMillis()  // 时间戳
)

// 简化错误类型
sealed class MinimizeError(message: String) : Exception(message) {
    class MinimizeFailed(reason: String) : MinimizeError("最小化失败: $reason")
    class RestoreFailed(reason: String) : MinimizeError("恢复失败: $reason")
    class NotificationFailed(reason: String) : MinimizeError("通知失败: $reason")
}
```

### 4. 增强可扩展性

**当前问题**：
- State 枚举只有三种状态
- UI 样式和动画固定
- 难以支持主题切换

**修改要求**：
```kotlin
// 使用接口定义状态行为
interface MinimizedIndicatorState {
    val icon: ImageVector
    val backgroundColor: Color
    val contentDescription: String
    val showProgress: Boolean
}

data class LoadingState(
    override val icon: ImageVector = Icons.Default.Refresh,
    override val backgroundColor: Color = MaterialTheme.colorScheme.primary,
    override val contentDescription: String = "加载中",
    override val showProgress: Boolean = true
) : MinimizedIndicatorState

data class SuccessState(
    override val icon: ImageVector = Icons.Default.Check,
    override val backgroundColor: Color = Color(0xFF4CAF50),
    override val contentDescription: String = "完成",
    override val showProgress: Boolean = false
) : MinimizedIndicatorState

// 支持自定义配置
data class MinimizedIndicatorConfig(
    val size: Dp = 56.dp,
    val animationDuration: Int = 300,
    val enableHapticFeedback: Boolean = true,
    val snapToEdge: Boolean = false
)
```

### 5. 补充版本兼容性和性能文档

**当前问题**：
- 缺少 Android 版本兼容性说明
- 没有性能监控指标
- 缺少特殊设备适配方案

**修改要求**：
```markdown
## 版本兼容性

| Android 版本 | 兼容性 | 特殊处理 |
|-------------|---------|----------|
| Android 7.0 (API 24) | ✅ 完全支持 | 无 |
| Android 8.0 (API 26) | ✅ 完全支持 | 通知渠道 |
| Android 10 (API 29) | ✅ 完全支持 | 后台限制 |
| Android 12 (API 31) | ✅ 完全支持 | 动态颜色 |
| Android 13 (API 33) | ✅ 完全支持 | 通知权限 |

## 性能指标

| 指标 | 目标值 | 测量方法 |
|------|--------|----------|
| 最小化动画时长 | < 300ms | PerformanceMonitor |
| 状态切换延迟 | < 50ms | Compose Tracing |
| 内存占用 | < 5MB | MemoryProfiler |
| 拖动响应时间 | < 16ms | Choreographer |
| CPU 使用率 | < 5% | Profiler |

## 特殊设备适配

### 折叠屏设备
- 支持折叠状态检测
- 自动调整指示器位置
- 保持状态一致性

### 低内存设备
- 禁用复杂动画
- 使用简化渲染
- 及时释放资源
```

## 修改原则

1. **保持向后兼容**：确保现有功能不受影响
2. **渐进式改进**：分阶段实施，优先解决架构不一致问题
3. **测试驱动**：每个修改都要有对应的测试用例
4. **文档同步**：代码修改后立即更新文档

## 输出要求

1. **保持原有结构**：在现有设计文档基础上修改
2. **使用中文注释**：所有新增注释使用中文
3. **提供代码示例**：每个修改点都要有具体的代码示例
4. **说明迁移步骤**：提供从当前实现到新实现的迁移指南
5. **更新测试策略**：根据新架构调整测试方案

## 注意事项

- 确保所有修改符合项目的 Clean Architecture 原则
- 保持与现有 FloatingView 组件的接口一致性
- 考虑性能影响，避免引入不必要的复杂性
- 确保修改后的代码易于测试和维护

---

**使用说明**：将此提示词与需要修改的设计文档一起提供给 AI，AI 将根据上述要求对设计文档进行系统性优化。