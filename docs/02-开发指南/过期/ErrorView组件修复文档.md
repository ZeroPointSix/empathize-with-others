# ErrorView 组件修复文档

## 修复时间
2025-12-05

## 问题描述

ErrorView 组件的 `retryAction` 参数设计为可选参数（`(() -> Unit)? = null`），不符合 UI 层设计规范要求。设计文档明确要求错误视图必须提供重试功能。

## 修复内容

### 1. 参数签名修改

**修复前：**
```kotlin
@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,  // ❌ 可选参数
    modifier: Modifier = Modifier,
    errorType: ErrorType = ErrorType.General
)
```

**修复后：**
```kotlin
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,  // ✅ 必需参数
    modifier: Modifier = Modifier,
    errorType: ErrorType = ErrorType.General
)
```

### 2. 移除可空检查

**修复前：**
```kotlin
// 重试按钮
if (onRetry != null) {
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onRetry) {
        Text("重试")
    }
}
```

**修复后：**
```kotlin
Spacer(modifier = Modifier.height(24.dp))

// 重试按钮
Button(onClick = onRetry) {
    Text("重试")
}
```

### 3. 更新 Preview 函数

**修复前：**
```kotlin
@Preview(name = "无重试按钮", showBackground = true)
@Composable
private fun ErrorViewNoRetryPreview() {
    EmpathyTheme {
        ErrorView(
            message = "数据格式不正确，无法加载。",
            onRetry = null,  // ❌ 传入 null
            errorType = ErrorType.General
        )
    }
}
```

**修复后：**
```kotlin
@Preview(name = "未找到数据", showBackground = true)
@Composable
private fun ErrorViewNotFoundPreview() {
    EmpathyTheme {
        ErrorView(
            message = "未找到相关数据，请尝试其他搜索条件。",
            onRetry = {},  // ✅ 提供空实现
            errorType = ErrorType.NotFound
        )
    }
}
```

### 4. 文档注释更新

**修复前：**
```kotlin
/**
 * @param onRetry 重试回调，为null时不显示重试按钮
 */
```

**修复后：**
```kotlin
/**
 * @param onRetry 重试回调
 */
```

## 修复验证

### 编译检查
✅ **无诊断错误** - 代码通过 getDiagnostics 检查

### Preview 函数覆盖
修复后的组件包含 5 个完整的 Preview 函数：

1. **ErrorViewGeneralPreview** - 一般错误
2. **ErrorViewNetworkPreview** - 网络错误
3. **ErrorViewNotFoundPreview** - 未找到数据（替换原"无重试按钮"预览）
4. **ErrorViewLongMessagePreview** - 长文本消息
5. **ErrorViewDarkPreview** - 深色模式

所有 Preview 都正确提供了 `onRetry` 参数。

## 设计规范符合性

### ✅ 状态提升
- 组件完全无状态
- 所有交互通过回调参数处理

### ✅ 必需的重试功能
- `onRetry` 参数为必需参数
- 重试按钮始终显示
- 符合 UI 层设计规范要求

### ✅ Material 3 主题
- 使用 `MaterialTheme.colorScheme` 获取颜色
- 使用 `MaterialTheme.typography` 设置文字样式
- 自动支持深色模式

### ✅ 错误类型支持
保持现有的 ErrorType 密封类设计：
- `ErrorType.General` - 一般错误
- `ErrorType.Network` - 网络错误
- `ErrorType.NotFound` - 未找到数据
- `ErrorType.Permission` - 权限不足

## 使用示例

### 基本用法
```kotlin
ErrorView(
    message = "加载数据失败",
    onRetry = { viewModel.retry() }
)
```

### 指定错误类型
```kotlin
ErrorView(
    message = "无法连接到服务器",
    onRetry = { viewModel.retryNetworkRequest() },
    errorType = ErrorType.Network
)
```

### 在 Screen 中使用
```kotlin
@Composable
fun ContactListScreen(
    uiState: ContactListUiState,
    onEvent: (ContactListUiEvent) -> Unit
) {
    when (uiState) {
        is ContactListUiState.Error -> {
            ErrorView(
                message = uiState.message,
                onRetry = { onEvent(ContactListUiEvent.Retry) },
                errorType = ErrorType.General
            )
        }
        // ... 其他状态
    }
}
```

## 影响范围

### 破坏性变更
⚠️ 此修复是**破坏性变更**，所有使用 ErrorView 的地方必须提供 `onRetry` 参数。

### 需要更新的代码
如果项目中已有使用 ErrorView 的代码，需要检查并更新：

```kotlin
// ❌ 修复前（不再支持）
ErrorView(
    message = "错误",
    onRetry = null
)

// ✅ 修复后（必须提供）
ErrorView(
    message = "错误",
    onRetry = { /* 重试逻辑 */ }
)
```

## 设计理由

### 为什么必须提供重试功能？

1. **用户体验一致性**：所有错误场景都应该给用户提供恢复的机会
2. **符合 Material Design 指南**：错误状态应该提供明确的操作建议
3. **简化组件 API**：移除可选参数，减少使用时的心智负担
4. **强制最佳实践**：确保开发者为每个错误场景提供恢复路径

### 如果确实不需要重试怎么办？

如果某些场景确实不需要重试（如权限被永久拒绝），可以：

1. **提供空实现**：`onRetry = {}`（按钮仍显示但无操作）
2. **使用其他组件**：创建专门的 InfoView 组件用于纯信息展示
3. **隐藏按钮**：在 onRetry 中导航到设置页面或显示帮助信息

## 后续优化建议

1. **按钮文本自定义**：允许自定义重试按钮文本（如"前往设置"、"刷新"等）
2. **加载状态**：重试时显示加载指示器
3. **错误图标扩展**：为不同错误类型使用更具体的图标
4. **可访问性**：添加语义化标签和屏幕阅读器支持

## 修复总结

ErrorView 组件已成功修复，现在完全符合 UI 层设计规范：
- ✅ 移除可选的 `onRetry` 参数
- ✅ 始终显示重试按钮
- ✅ 更新所有 Preview 函数
- ✅ 通过编译检查
- ✅ 保持现有错误类型设计

组件已就绪，可用于 Phase 3 的屏幕开发。
