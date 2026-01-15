# 功能开发探索报告

## 基本信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-01-14 |
| 分支 | freedom-feature |
| 状态 | 📖仅参考 |
| 探索者 | feature-explorer |
| 关联 PRD | PRD-00008 |

---

## 🔗 相关文档

- PRD：`文档/开发文档/PRD/PRD-00008-输入内容身份识别与双向对话历史需求.md`
- 决策日志：`文档/开发文档/MA/FEATURE/FEATURE-20260114-identity-prefix-history-JOURNAL.md`

---

## 需求概述

### 功能描述

PRD-00008 的目标是让 AI 能正确区分“对方说的内容”和“我正在回复的内容”，避免分析与检查场景角色混淆。本次探索在既有实现基础上补齐事实流 UI 渲染缺口，确保历史记录展示符合“左右气泡 + 隐藏前缀”的产品预期，并补充身份前缀工具的单元测试，以降低回归风险。

### 用户故事

作为需要分析聊天上下文的用户，我希望系统能自动识别输入内容的身份，并在历史记录中以自然对话流展示，让我无需手动标记也能获得正确分析与清晰的回看体验。

### 验收标准（代码覆盖情况说明）

- [x] 点击【帮我分析】时，发送给 AI 的内容自动添加 `【对方说】：` 前缀（已有代码覆盖，未做端到端验证）
- [x] 点击【帮我检查】时，发送给 AI 的内容自动添加 `【我正在回复】：` 前缀（已有代码覆盖，未做端到端验证）
- [x] 【帮我分析】的输入内容正确保存到历史记录（带前缀）（已有代码覆盖）
- [x] 【帮我检查】的输入内容不保存到历史记录（已有代码覆盖）
- [x] 历史上下文构建时正确保留身份前缀（ConversationContextBuilder 已实现）
- [x] AI 能正确理解身份前缀含义（SystemPrompts 已加入规则，未做在线验证）
- [x] 旧数据（无前缀）正常显示和处理（IdentityPrefixHelper 兼容 + UI 渲染分支）
- [x] 事实流界面以自然对话流形式展示（左右气泡）且 UI 隐藏前缀（本次实现，待手动验收）

---

## 技术设计

### 架构设计

本次不改动数据层与领域层的核心逻辑，沿用已存在的身份前缀体系（IdentityPrefixHelper + Analyze/Check UseCase + SystemPrompts）。实现重点放在表现层：在事实流的 ConversationCard 中复用 ConversationBubble，使 UI 层按身份左右对齐，同时保留原有“时间 + 总结状态”的信息结构。

### 数据模型

- 无新增数据模型。
- 历史记录仍沿用 `ConversationLog.userInput` 存储带前缀内容，兼容旧数据（无前缀）。

### 接口设计

新增可选参数以支持事实流复用：

```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/message/ConversationBubble.kt
fun ConversationBubble(
    log: ConversationLog,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showHeader: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
)
```

---

## 实现详情

### 新增文件

| 文件 | 类型 | 说明 |
|------|------|------|
| domain/src/test/kotlin/com/empathy/ai/domain/util/IdentityPrefixHelperTest.kt | 单元测试 | 身份前缀添加/解析/清理测试 |

### 修改文件

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/message/ConversationBubble.kt | 修改 | 支持隐藏头部、限制行数，便于事实流复用 |
| presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/card/ConversationCard.kt | 修改 | 事实流对话卡片改用身份气泡渲染 |

### 代码统计（估算）

- 新增代码行数：约 120 行（含测试）
- 修改代码行数：约 60 行
- 删除代码行数：约 40 行

---

## 关键变更与代码说明

下面按“修改前 / 修改后”给出完整代码，并逐条解释原因、影响与替代方案。

### 变更 1：ConversationBubble 支持隐藏头部与行数限制

#### 修改前（完整代码）
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/message/ConversationBubble.kt
// 修改前
package com.empathy.ai.presentation.ui.component.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.presentation.theme.EmpathyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话气泡组件
 *
 * 根据身份前缀自动渲染左右布局的对话气泡。
 *
 * 布局规则（PRD-00008）：
 * - CONTACT（对方说）：靠左对齐，浅灰背景
 * - USER（我正在回复）：靠右对齐，主题色背景
 * - LEGACY（旧数据）：居中对齐，中性背景
 *
 * UI 隐藏前缀：用户不会看到【对方说】：等前缀文本，只显示纯内容。
 *
 * @param log 对话记录
 * @param modifier Modifier
 * @param onClick 点击回调（用于编辑）
 */
@Composable
fun ConversationBubble(
    log: ConversationLog,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // 解析身份前缀，使用 remember 缓存结果避免重复计算
    val parseResult = remember(log.userInput) {
        IdentityPrefixHelper.parse(log.userInput)
    }

    // 根据身份确定对齐方式
    val alignment = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT -> Alignment.Start
        IdentityPrefixHelper.IdentityRole.USER -> Alignment.End
        IdentityPrefixHelper.IdentityRole.LEGACY -> Alignment.CenterHorizontally
    }

    // 根据身份确定背景色
    val backgroundColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.surfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.primaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.surface
    }

    // 根据身份确定气泡形状（不同角的圆角大小）
    val bubbleShape = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)  // 左上角小
        IdentityPrefixHelper.IdentityRole.USER ->
            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)  // 右上角小
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            RoundedCornerShape(16.dp)  // 全圆角
    }

    // 根据身份确定文字颜色
    val textColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.onSurfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.onPrimaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // 标签和时间
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = parseResult.role.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = " · ${formatTime(log.timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 气泡
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = parseResult.content,  // 显示纯文本，不含前缀
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * 简化版对话气泡（直接传入内容和角色）
 *
 * @param content 对话内容（可能带前缀）
 * @param timestamp 时间戳
 * @param modifier Modifier
 * @param onClick 点击回调
 */
@Composable
fun ConversationBubble(
    content: String,
    timestamp: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // 解析身份前缀
    val parseResult = remember(content) {
        IdentityPrefixHelper.parse(content)
    }

    // 根据身份确定对齐方式
    val alignment = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT -> Alignment.Start
        IdentityPrefixHelper.IdentityRole.USER -> Alignment.End
        IdentityPrefixHelper.IdentityRole.LEGACY -> Alignment.CenterHorizontally
    }

    // 根据身份确定背景色
    val backgroundColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.surfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.primaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.surface
    }

    // 根据身份确定气泡形状
    val bubbleShape = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
        IdentityPrefixHelper.IdentityRole.USER ->
            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            RoundedCornerShape(16.dp)
    }

    // 根据身份确定文字颜色
    val textColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.onSurfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.onPrimaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // 标签和时间
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = parseResult.role.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = " · ${formatTime(timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 气泡
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = parseResult.content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ==================== Previews ====================

@Preview(name = "对方说的气泡", showBackground = true)
@Composable
private fun PreviewContactBubble() {
    EmpathyTheme {
        ConversationBubble(
            log = ConversationLog(
                id = 1,
                contactId = "contact_1",
                userInput = "${IdentityPrefixHelper.PREFIX_CONTACT}你怎么才回消息？",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "我正在回复的气泡", showBackground = true)
@Composable
private fun PreviewUserBubble() {
    EmpathyTheme {
        ConversationBubble(
            log = ConversationLog(
                id = 2,
                contactId = "contact_1",
                userInput = "${IdentityPrefixHelper.PREFIX_USER}刚才在开会，抱歉让你久等了",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "旧数据气泡（无前缀）", showBackground = true)
@Composable
private fun PreviewLegacyBubble() {
    EmpathyTheme {
        ConversationBubble(
            log = ConversationLog(
                id = 3,
                contactId = "contact_1",
                userInput = "这是一条旧数据，没有身份前缀",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "对话气泡组合", showBackground = true)
@Composable
private fun PreviewConversationFlow() {
    EmpathyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ConversationBubble(
                content = "${IdentityPrefixHelper.PREFIX_CONTACT}早安",
                timestamp = System.currentTimeMillis() - 3600000
            )
            ConversationBubble(
                content = "${IdentityPrefixHelper.PREFIX_USER}早呀，今天天气真好",
                timestamp = System.currentTimeMillis() - 3500000,
                modifier = Modifier.padding(top = 8.dp)
            )
            ConversationBubble(
                content = "${IdentityPrefixHelper.PREFIX_CONTACT}是啊，要不要出去走走？",
                timestamp = System.currentTimeMillis(),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

#### 修改后（完整代码）

```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/message/ConversationBubble.kt
// 修改后
package com.empathy.ai.presentation.ui.component.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.presentation.theme.EmpathyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话气泡组件
 *
 * 根据身份前缀自动渲染左右布局的对话气泡。
 *
 * 布局规则（PRD-00008）：
 * - CONTACT（对方说）：靠左对齐，浅灰背景
 * - USER（我正在回复）：靠右对齐，主题色背景
 * - LEGACY（旧数据）：居中对齐，中性背景
 *
 * UI 隐藏前缀：用户不会看到【对方说】：等前缀文本，只显示纯内容。
 *
 * @param log 对话记录
 * @param modifier Modifier
 * @param onClick 点击回调（用于编辑）
 */
@Composable
fun ConversationBubble(
    log: ConversationLog,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showHeader: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    // 解析身份前缀，使用 remember 缓存结果避免重复计算
    val parseResult = remember(log.userInput) {
        IdentityPrefixHelper.parse(log.userInput)
    }

    // 根据身份确定对齐方式
    val alignment = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT -> Alignment.Start
        IdentityPrefixHelper.IdentityRole.USER -> Alignment.End
        IdentityPrefixHelper.IdentityRole.LEGACY -> Alignment.CenterHorizontally
    }

    // 根据身份确定背景色
    val backgroundColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.surfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.primaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.surface
    }

    // 根据身份确定气泡形状（不同角的圆角大小）
    val bubbleShape = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)  // 左上角小
        IdentityPrefixHelper.IdentityRole.USER ->
            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)  // 右上角小
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            RoundedCornerShape(16.dp)  // 全圆角
    }

    // 根据身份确定文字颜色
    val textColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.onSurfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.onPrimaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (showHeader) {
            // 标签和时间
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = parseResult.role.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " · ${formatTime(log.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 气泡
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = parseResult.content,  // 显示纯文本，不含前缀
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                maxLines = maxLines,
                overflow = overflow
            )
        }
    }
}

/**
 * 简化版对话气泡（直接传入内容和角色）
 *
 * @param content 对话内容（可能带前缀）
 * @param timestamp 时间戳
 * @param modifier Modifier
 * @param onClick 点击回调
 */
@Composable
fun ConversationBubble(
    content: String,
    timestamp: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showHeader: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    // 解析身份前缀
    val parseResult = remember(content) {
        IdentityPrefixHelper.parse(content)
    }

    // 根据身份确定对齐方式
    val alignment = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT -> Alignment.Start
        IdentityPrefixHelper.IdentityRole.USER -> Alignment.End
        IdentityPrefixHelper.IdentityRole.LEGACY -> Alignment.CenterHorizontally
    }

    // 根据身份确定背景色
    val backgroundColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.surfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.primaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.surface
    }

    // 根据身份确定气泡形状
    val bubbleShape = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
        IdentityPrefixHelper.IdentityRole.USER ->
            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            RoundedCornerShape(16.dp)
    }

    // 根据身份确定文字颜色
    val textColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.onSurfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.onPrimaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (showHeader) {
            // 标签和时间
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = parseResult.role.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " · ${formatTime(timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 气泡
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = parseResult.content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                maxLines = maxLines,
                overflow = overflow
            )
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ==================== Previews ====================

@Preview(name = "对方说的气泡", showBackground = true)
@Composable
private fun PreviewContactBubble() {
    EmpathyTheme {
        ConversationBubble(
            log = ConversationLog(
                id = 1,
                contactId = "contact_1",
                userInput = "${IdentityPrefixHelper.PREFIX_CONTACT}你怎么才回消息？",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "我正在回复的气泡", showBackground = true)
@Composable
private fun PreviewUserBubble() {
    EmpathyTheme {
        ConversationBubble(
            log = ConversationLog(
                id = 2,
                contactId = "contact_1",
                userInput = "${IdentityPrefixHelper.PREFIX_USER}刚才在开会，抱歉让你久等了",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "旧数据气泡（无前缀）", showBackground = true)
@Composable
private fun PreviewLegacyBubble() {
    EmpathyTheme {
        ConversationBubble(
            log = ConversationLog(
                id = 3,
                contactId = "contact_1",
                userInput = "这是一条旧数据，没有身份前缀",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "对话气泡组合", showBackground = true)
@Composable
private fun PreviewConversationFlow() {
    EmpathyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ConversationBubble(
                content = "${IdentityPrefixHelper.PREFIX_CONTACT}早安",
                timestamp = System.currentTimeMillis() - 3600000
            )
            ConversationBubble(
                content = "${IdentityPrefixHelper.PREFIX_USER}早呀，今天天气真好",
                timestamp = System.currentTimeMillis() - 3500000,
                modifier = Modifier.padding(top = 8.dp)
            )
            ConversationBubble(
                content = "${IdentityPrefixHelper.PREFIX_CONTACT}是啊，要不要出去走走？",
                timestamp = System.currentTimeMillis(),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

**修改原因**：事实流需要隐藏身份前缀，但原组件强制显示“身份标签 + 时间”，并且文本行数无法统一控制，导致卡片展示与“气泡型对话”体验不一致。本次新增 `showHeader/maxLines/overflow` 作为可选参数，既能保留默认行为，又能在事实流卡片中复用，避免引入新的 UI 组件。

**影响分析**：
- 对现有调用方：默认参数保持原行为（仍显示头部、无限行），不会引入行为回归。
- 对事实流：ConversationCard 可以隐藏头部并保持三行省略，避免长文本把卡片撑高。
- 对样式一致性：复用现有气泡样式，减少新样式差异。

**替代方案**：
1) 新建专用 `FactStreamConversationBubble`：可完全定制，但会增加样式维护成本。
2) 直接在 ConversationCard 中裁剪前缀文本：逻辑分散且不利于复用。
最终选择参数化复用，兼顾一致性与低风险。

---

### 变更 2：ConversationCard 使用气泡渲染身份内容

#### 修改前（完整代码）
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/card/ConversationCard.kt
// 修改前
package com.empathy.ai.presentation.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话记录卡片组件
 *
 * 展示具体的对话内容，包括用户输入和AI响应
 * 支持长按编辑/删除
 *
 * @param item 对话记录数据
 * @param onClick 点击回调
 * @param onLongClick 长按回调（用于编辑/删除）
 * @param modifier Modifier
 */
@Composable
fun ConversationCard(
    item: TimelineItem.Conversation,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val dimensions = AdaptiveDimensions.current
    
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: onLongClick // 点击时触发编辑
    ) {
        Column(modifier = Modifier.padding(dimensions.spacingMedium)) {
            // 时间和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(item.log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 总结状态
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.log.isSummarized) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Schedule
                        },
                        contentDescription = if (item.log.isSummarized) "已总结" else "待总结",
                        modifier = Modifier.size(dimensions.iconSizeSmall - 2.dp),
                        tint = if (item.log.isSummarized) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = if (item.log.isSummarized) "已总结" else "待总结",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.log.isSummarized) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            
            // 用户输入
            Text(
                text = "你：${item.log.userInput}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // AI响应已移除 - BUG-00001修复
            // 原因：AI回复显示为格式化摘要而非完整建议，影响用户体验
            // 对话记录只保留用户输入，AI建议在分析时实时生成
        }
    }
}

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ========== 预览 ==========

@Preview(name = "对话卡片 - 已总结", showBackground = true)
@Composable
private fun PreviewConversationCardSummarized() {
    EmpathyTheme {
        ConversationCard(
            item = TimelineItem.Conversation(
                id = "1",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.NEUTRAL,
                log = ConversationLog(
                    id = 1,
                    contactId = "contact_1",
                    userInput = "今天想约她出去吃饭，但不知道怎么开口比较好",
                    aiResponse = "建议用轻松的方式邀请，比如说发现了一家不错的餐厅想一起去尝尝",
                    timestamp = System.currentTimeMillis(),
                    isSummarized = true
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "对话卡片 - 待总结", showBackground = true)
@Composable
private fun PreviewConversationCardPending() {
    EmpathyTheme {
        ConversationCard(
            item = TimelineItem.Conversation(
                id = "2",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.SWEET,
                log = ConversationLog(
                    id = 2,
                    contactId = "contact_1",
                    userInput = "她说喜欢我送的礼物，感觉很开心",
                    aiResponse = null,
                    timestamp = System.currentTimeMillis(),
                    isSummarized = false
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

#### 修改后（完整代码）

```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/card/ConversationCard.kt
// 修改后
package com.empathy.ai.presentation.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard
import com.empathy.ai.presentation.ui.component.message.ConversationBubble
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话记录卡片组件
 *
 * 展示具体的对话内容，包括用户输入和AI响应
 * 支持长按编辑/删除
 *
 * @param item 对话记录数据
 * @param onClick 点击回调
 * @param onLongClick 长按回调（用于编辑/删除）
 * @param modifier Modifier
 */
@Composable
fun ConversationCard(
    item: TimelineItem.Conversation,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val dimensions = AdaptiveDimensions.current
    val parseResult = remember(item.log.userInput) {
        IdentityPrefixHelper.parse(item.log.userInput)
    }

    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: onLongClick // 点击时触发编辑
    ) {
        Column(modifier = Modifier.padding(dimensions.spacingMedium)) {
            // 时间和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${parseResult.role.displayName} · ${formatTime(item.log.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 总结状态
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.log.isSummarized) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Schedule
                        },
                        contentDescription = if (item.log.isSummarized) "已总结" else "待总结",
                        modifier = Modifier.size(dimensions.iconSizeSmall - 2.dp),
                        tint = if (item.log.isSummarized) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = if (item.log.isSummarized) "已总结" else "待总结",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.log.isSummarized) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            
            // 对话气泡（隐藏前缀，按身份左右对齐）
            ConversationBubble(
                log = item.log,
                showHeader = false,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // AI响应已移除 - BUG-00001修复
            // 原因：AI回复显示为格式化摘要而非完整建议，影响用户体验
            // 对话记录只保留用户输入，AI建议在分析时实时生成
        }
    }
}

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ========== 预览 ==========

@Preview(name = "对话卡片 - 已总结", showBackground = true)
@Composable
private fun PreviewConversationCardSummarized() {
    EmpathyTheme {
        ConversationCard(
            item = TimelineItem.Conversation(
                id = "1",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.NEUTRAL,
                log = ConversationLog(
                    id = 1,
                    contactId = "contact_1",
                    userInput = "今天想约她出去吃饭，但不知道怎么开口比较好",
                    aiResponse = "建议用轻松的方式邀请，比如说发现了一家不错的餐厅想一起去尝尝",
                    timestamp = System.currentTimeMillis(),
                    isSummarized = true
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "对话卡片 - 待总结", showBackground = true)
@Composable
private fun PreviewConversationCardPending() {
    EmpathyTheme {
        ConversationCard(
            item = TimelineItem.Conversation(
                id = "2",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.SWEET,
                log = ConversationLog(
                    id = 2,
                    contactId = "contact_1",
                    userInput = "她说喜欢我送的礼物，感觉很开心",
                    aiResponse = null,
                    timestamp = System.currentTimeMillis(),
                    isSummarized = false
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

**修改原因**：事实流卡片原本直接展示 `你：${userInput}`，会把 `【对方说】：` 等前缀暴露给用户，且视觉上始终是单列文本，不符合 PRD-00008 的左右气泡要求。将对话内容改为 ConversationBubble 渲染，能够自动解析身份前缀并隐藏文本前缀，同时保留“时间 + 总结状态”的卡片结构。

**影响分析**：
- UI 行为变化：正文区域改为气泡布局，身份标签与时间仍保留在卡片头部；用户可更直观区分“对方/我”的对话。
- 旧数据兼容：无前缀内容会走 LEGACY 渲染（居中 + 中性背景），符合兼容性要求。
- 交互影响：原有 onClick/onLongClick 不变，气泡仍可触发编辑。

**替代方案**：
1) 在 ConversationCard 内部手动解析前缀并画气泡：实现耦合到卡片，复用性较差。
2) 直接替换为新的事实流组件：改动范围大，风险高。
因此选择复用 ConversationBubble 并隐藏头部。

---

### 变更 3：新增 IdentityPrefixHelper 单元测试

#### 新增代码（完整代码）
```kotlin
// file: domain/src/test/kotlin/com/empathy/ai/domain/util/IdentityPrefixHelperTest.kt
// 新增
package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ActionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IdentityPrefixHelper 单元测试
 *
 * 覆盖身份前缀的添加、解析与清理逻辑。
 */
class IdentityPrefixHelperTest {

    @Test
    fun `addPrefix应根据动作类型添加正确前缀并去重`() {
        // Given
        val raw = "你好"

        // When
        val analyze = IdentityPrefixHelper.addPrefix(raw, ActionType.ANALYZE)
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}你好", analyze)

        val check = IdentityPrefixHelper.addPrefix(raw, ActionType.CHECK)
        assertEquals("${IdentityPrefixHelper.PREFIX_USER}你好", check)

        val swapped = IdentityPrefixHelper.addPrefix(
            "${IdentityPrefixHelper.PREFIX_USER}你好",
            ActionType.ANALYZE
        )

        // Then
        assertEquals("${IdentityPrefixHelper.PREFIX_CONTACT}你好", swapped)
    }

    @Test
    fun `parse应正确识别身份并返回纯文本`() {
        // Given
        val contact = IdentityPrefixHelper.parse("${IdentityPrefixHelper.PREFIX_CONTACT}早安")

        // When
        assertEquals(IdentityPrefixHelper.IdentityRole.CONTACT, contact.role)
        assertEquals("早安", contact.content)

        val user = IdentityPrefixHelper.parse("${IdentityPrefixHelper.PREFIX_USER}我马上到")
        assertEquals(IdentityPrefixHelper.IdentityRole.USER, user.role)
        assertEquals("我马上到", user.content)

        val legacy = IdentityPrefixHelper.parse("这是一条旧数据")

        // Then
        assertEquals(IdentityPrefixHelper.IdentityRole.LEGACY, legacy.role)
        assertEquals("这是一条旧数据", legacy.content)
    }

    @Test
    fun `stripAllPrefixes应移除多重前缀`() {
        // Given
        val content = "${IdentityPrefixHelper.PREFIX_CONTACT}${IdentityPrefixHelper.PREFIX_USER}你好"

        // When
        val stripped = IdentityPrefixHelper.stripAllPrefixes(content)

        // Then
        assertEquals("你好", stripped)
    }
}
```

**测试意图说明**：
- `addPrefix`：验证不同 ActionType 下前缀正确、重复前缀可被清理，保证分析/检查输入不会出现双前缀或错误前缀。
- `parse`：验证前缀解析与旧数据兼容行为，确保 UI 渲染与历史上下文的角色判断一致。
- `stripAllPrefixes`：验证递归清理逻辑，确保前缀被重复粘贴时能被归一化。

**边界情况说明**：
- 包含“对方说 + 我正在回复”的混合前缀输入，属于 OCR 或复制场景的常见误差，需确保最终内容不带前缀。

**我的判断**：建议保留。
**判断理由**：测试覆盖了核心前缀操作的主要路径与异常输入，且成本低，不会引入额外依赖，有助于后续改动的回归防护。

---

## 测试情况

### 单元测试
- 执行命令：`gradlew.bat :domain:test --tests "com.empathy.ai.domain.util.IdentityPrefixHelperTest"`
- 结果：通过
- 备注：编译阶段存在既有警告（AiResultTest/MinimizeErrorTest/PromptErrorTest/SystemPromptsTest），与本次修改无直接关联。

### 集成测试
- 未执行（本次仅修改 UI 与单测）。

### 全量回归
- 执行命令：`gradlew.bat test`
- 结果：失败
- 失败原因：`app:testDevUnitTest` 解析依赖失败，`:data`/`:presentation` 缺少 `dev` 构建变体（仅有 debug/release）。

---

## 构建验证
- [x] Debug 构建成功（`gradlew.bat installDebug` 构建阶段完成）
- [x] Debug 安装成功（设备已连接，安装到 2 台设备）
- [ ] Release 构建成功（未执行）
- [ ] 无新增 Lint 警告（未执行）

---

## 已知问题与风险
1. 事实流 UI 变更未进行人工验收，实际视觉效果需确认（左右气泡、行数截断、 头部信息）。
2. 本次未运行 UI/仪器测试，气泡布局在不同屏幕密度上的表现未验证。
3. 全量回归 `gradlew.bat test` 因 `dev` 变体缺失失败，需要确认测试变体或模块配置。
4. 部分既有单元测试存在编译警告，后续可集中清理，但与本次修改无直接关系。

---

## 建议
### 合并建议
仅供参考。建议在人工验收事实流展示无异常后再合并。

### 改进建议
- 如需统一事实流与其他对话展示，可考虑将卡片头部（时间 + 总结状态）抽为可配置区域。
- 后续可补充 UI 测试，覆盖对方/我/旧数据三种渲染分支。

### 后续工作
- 执行一次手动验收：事实流卡片内是否隐藏前缀并展示左右气泡。
- 如需更强一致性，可为 ConversationCard 增加可切换布局配置。

---

## 探索日志

| 时间 | 工作内容 | 状态 |
|------|----------|------|
| 21:15 | 读取 PRD-00008 与现有实现，识别事实流 UI 缺口 | ✅ |
| 21:22 | ConversationBubble 支持隐藏头部与行数限制 | ✅ |
| 21:26 | ConversationCard 改用气泡渲染 | ✅ |
| 21:26 | 新增 IdentityPrefixHelperTest | ✅ |
| 21:31 | 执行 `:domain:test`（指定测试） | ✅ |
| 10:21 | 执行 `gradlew.bat installDebug`（无设备，安装失败） | ❌ |
| 10:37 | 重新执行 `gradlew.bat installDebug`（无设备，安装失败） | ❌ |
| 10:46 | 执行 `gradlew.bat installDebug`（设备已连接，安装成功） | ✅ |
