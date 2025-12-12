# Phase2: 可复用组件阶段

## 📋 阶段概览

**目标**: 构建5个核心可复用UI组件,建立组件库基础

**预计工期**: 2-3天

**优先级**: P0 (必须完成)

**前置条件**:
- ✅ Phase1: 基础设施阶段已完成
- ✅ 主题系统 (Color.kt, Theme.kt) 已配置
- ✅ 导航系统框架已搭建

**交付物**:
1. LoadingIndicator.kt - 加载指示器组件
2. ErrorDialog.kt - 错误对话框组件
3. ContactCard.kt - 联系人卡片组件
4. BrainTagChip.kt - 标签芯片组件
5. MessageBubble.kt - 消息气泡组件

---

## 一、组件设计原则

### 1.1 设计理念

**原子设计 (Atomic Design)**
```
原子 (Atoms) → 分子 (Molecules) → 组织 (Organisms) → 模板 (Templates) → 页面 (Pages)
```

本阶段创建的是**分子级组件** - 由基础UI元素组合而成的功能单元。

### 1.2 组件规范

#### 必须遵循
1. **状态提升**: 组件无状态,状态由父组件管理
2. **参数顺序**: 数据 → 回调 → Modifier
3. **默认值**: 提供合理的默认值
4. **Preview**: 每个组件必须有@Preview函数
5. **主题颜色**: 使用MaterialTheme.colorScheme
6. **文档注释**: 完整的KDoc注释

#### 组件模板
```kotlin
/**
 * [组件名称]
 *
 * [功能描述]
 *
 * @param [参数1] [参数1说明]
 * @param [参数2] [参数2说明]
 * @param modifier 修饰符
 */
@Composable
fun ComponentName(
    // 必需参数
    data: DataType,
    // 可选参数
    optional: Boolean = false,
    // 回调函数
    onAction: () -> Unit = {},
    // Modifier总是最后
    modifier: Modifier = Modifier
) {
    // 组件实现
}

@Preview
@Composable
private fun ComponentNamePreview() {
    EmpathyTheme {
        ComponentName(data = mockData)
    }
}
```

---

## 二、LoadingIndicator - 加载指示器

### 2.1 组件目标

提供统一的加载状态指示,支持全屏和内联两种模式。

### 2.2 完整实现

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/ui/component/LoadingIndicator.kt`

```kotlin
package com.empathy.ai.presentation.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 加载指示器组件
 *
 * 用于显示数据加载状态,支持全屏遮罩和内联两种模式
 *
 * @param message 加载提示文字,为空时不显示文字
 * @param isFullScreen 是否全屏显示,true=全屏遮罩,false=内联显示
 * @param modifier 修饰符
 */
@Composable
fun LoadingIndicator(
    message: String? = null,
    isFullScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = if (isFullScreen) {
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
        } else {
            modifier
        },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            
            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "内联模式")
@Composable
private fun LoadingIndicatorInlinePreview() {
    EmpathyTheme {
        LoadingIndicator(
            message = "加载中...",
            isFullScreen = false
        )
    }
}

@Preview(name = "全屏模式")
@Composable
private fun LoadingIndicatorFullScreenPreview() {
    EmpathyTheme {
        LoadingIndicator(
            message = "正在分析聊天记录...",
            isFullScreen = true
        )
    }
}

@Preview(name = "无文字")
@Composable
private fun LoadingIndicatorNoMessagePreview() {
    EmpathyTheme {
        LoadingIndicator(
            message = null,
            isFullScreen = false
        )
    }
}

@Preview(name = "深色模式", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingIndicatorDarkPreview() {
    EmpathyTheme {
        LoadingIndicator(
            message = "加载中...",
            isFullScreen = true
        )
    }
}
```

### 2.3 使用示例

```kotlin
// 在Screen中使用
@Composable
fun ContactListScreen(viewModel: ContactListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 主要内容
        ContactList(contacts = uiState.contacts)
        
        // 加载状态覆盖层
        if (uiState.isLoading) {
            LoadingIndicator(
                message = "加载联系人...",
                isFullScreen = true
            )
        }
    }
}
```

### 2.4 验证清单

- [ ] 组件支持全屏和内联两种模式
- [ ] 可选的提示文字参数
- [ ] 使用主题颜色
- [ ] 提供多个预览函数
- [ ] 支持深色模式

---

## 三、ErrorDialog - 错误对话框

### 3.1 组件目标

统一的错误提示对话框,支持重试和取消操作。

### 3.2 完整实现

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/ui/component/ErrorDialog.kt`

```kotlin
package com.empathy.ai.presentation.ui.component

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 错误对话框组件
 *
 * 用于显示错误信息并提供重试/取消操作
 *
 * @param title 对话框标题
 * @param message 错误消息
 * @param onDismiss 关闭对话框回调
 * @param onRetry 重试回调,为null时不显示重试按钮
 * @param confirmText 确认按钮文字
 * @param dismissText 取消按钮文字
 */
@Composable
fun ErrorDialog(
    title: String = "错误",
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    confirmText: String = "重试",
    dismissText: String = "取消"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "错误图标",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = androidx.compose.ui.unit.dp(6f)
    )
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "带重试按钮")
@Composable
private fun ErrorDialogWithRetryPreview() {
    EmpathyTheme {
        ErrorDialog(
            title = "网络错误",
            message = "无法连接到服务器,请检查网络连接后重试。",
            onDismiss = {},
            onRetry = {}
        )
    }
}

@Preview(name = "无重试按钮")
@Composable
private fun ErrorDialogWithoutRetryPreview() {
    EmpathyTheme {
        ErrorDialog(
            title = "数据错误",
            message = "联系人信息格式不正确,无法加载。",
            onDismiss = {},
            onRetry = null,
            dismissText = "确定"
        )
    }
}

@Preview(name = "长文本消息")
@Composable
private fun ErrorDialogLongMessagePreview() {
    EmpathyTheme {
        ErrorDialog(
            title = "操作失败",
            message = "保存联系人信息时发生错误。可能是因为网络连接不稳定,或者服务器正在维护中。请稍后再试,如果问题持续存在,请联系技术支持。",
            onDismiss = {},
            onRetry = {}
        )
    }
}

@Preview(name = "深色模式", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorDialogDarkPreview() {
    EmpathyTheme {
        ErrorDialog(
            title = "网络错误",
            message = "无法连接到服务器,请检查网络连接后重试。",
            onDismiss = {},
            onRetry = {}
        )
    }
}
```

### 3.3 使用示例

```kotlin
// 在Screen中使用
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 主要内容
    ChatContent(uiState = uiState)
    
    // 错误对话框
    if (uiState.error != null) {
        ErrorDialog(
            title = "分析失败",
            message = uiState.error!!,
            onDismiss = { viewModel.onEvent(ChatUiEvent.ClearError) },
            onRetry = { viewModel.onEvent(ChatUiEvent.AnalyzeChat) }
        )
    }
}
```

### 3.4 验证清单

- [ ] 显示错误图标和标题
- [ ] 支持自定义按钮文字
- [ ] 可选的重试按钮
- [ ] 使用主题颜色和样式
- [ ] 支持长文本自动换行

---

## 四、ContactCard - 联系人卡片

### 4.1 组件目标

展示联系人信息的卡片组件,用于列表展示。

### 4.2 完整实现

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/ui/component/ContactCard.kt`

```kotlin
package com.empathy.ai.presentation.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 联系人卡片组件
 *
 * 用于在列表中展示联系人基本信息
 *
 * @param contact 联系人信息
 * @param onClick 点击事件回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactCard(
    contact: ContactProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像 (首字母)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 联系人信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 姓名
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 目标
                if (contact.targetGoal.isNotBlank()) {
                    Text(
                        text = contact.targetGoal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 标签 (最多显示3个)
                if (contact.brainTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        contact.brainTags.take(3).forEach { tag ->
                            BrainTagChip(
                                text = tag.label,
                                isCompact = true
                            )
                        }
                        
                        if (contact.brainTags.size > 3) {
                            Text(
                                text = "+${contact.brainTags.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 箭头图标
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "查看详情",
                