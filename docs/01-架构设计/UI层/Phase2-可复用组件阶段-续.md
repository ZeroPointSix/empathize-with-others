# Phase2: 可复用组件阶段 (续)

> 本文件是Phase2-可复用组件阶段.md的续篇

## 四、ContactCard - 联系人卡片 (续)

### 4.2 完整实现 (续)

```kotlin
            // 箭头图标
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "查看详情",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "标准卡片")
@Composable
private fun ContactCardPreview() {
    EmpathyTheme {
        ContactCard(
            contact = ContactProfile(
                id = 1L,
                name = "张三",
                targetGoal = "维护长期友谊",
                brainTags = listOf(
                    com.empathy.ai.domain.model.BrainTag(
                        id = 1L,
                        label = "同事",
                        category = "关系"
                    ),
                    com.empathy.ai.domain.model.BrainTag(
                        id = 2L,
                        label = "喜欢运动",
                        category = "兴趣"
                    )
                )
            ),
            onClick = {}
        )
    }
}

@Preview(name = "多标签")
@Composable
private fun ContactCardMultiTagsPreview() {
    EmpathyTheme {
        ContactCard(
            contact = ContactProfile(
                id = 1L,
                name = "李四",
                targetGoal = "商业合作伙伴",
                brainTags = listOf(
                    com.empathy.ai.domain.model.BrainTag(id = 1L, label = "客户", category = "关系"),
                    com.empathy.ai.domain.model.BrainTag(id = 2L, label = "技术爱好者", category = "兴趣"),
                    com.empathy.ai.domain.model.BrainTag(id = 3L, label = "创业者", category = "职业"),
                    com.empathy.ai.domain.model.BrainTag(id = 4L, label = "北京", category = "地域")
                )
            ),
            onClick = {}
        )
    }
}

@Preview(name = "深色模式", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactCardDarkPreview() {
    EmpathyTheme {
        ContactCard(
            contact = ContactProfile(
                id = 1L,
                name = "王五",
                targetGoal = "学习交流",
                brainTags = emptyList()
            ),
            onClick = {}
        )
    }
}
```

### 4.3 使用示例

```kotlin
// 在ContactListScreen中使用
@Composable
fun ContactListScreen(viewModel: ContactListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(uiState.contacts) { contact ->
            ContactCard(
                contact = contact,
                onClick = {
                    viewModel.onEvent(
                        ContactListUiEvent.NavigateToDetail(contact.id.toString())
                    )
                }
            )
        }
    }
}
```

### 4.4 验证清单

- [ ] 显示首字母头像
- [ ] 支持多个标签(最多3个+计数)
- [ ] 点击响应正常
- [ ] 长文本正确省略
- [ ] 支持深色模式

---

## 五、BrainTagChip - 标签芯片

### 5.1 组件目标

展示和管理脑标签的芯片组件,支持普通和紧凑两种样式。

### 5.2 完整实现

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/ui/component/BrainTagChip.kt`

```kotlin
package com.empathy.ai.presentation.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 脑标签芯片组件
 *
 * 用于显示联系人的标签信息
 *
 * @param text 标签文字
 * @param isCompact 是否紧凑模式(不显示删除按钮)
 * @param onDelete 删除回调,为null时不显示删除按钮
 * @param modifier 修饰符
 */
@Composable
fun BrainTagChip(
    text: String,
    isCompact: Boolean = false,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 8.dp else 12.dp,
                vertical = if (isCompact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = if (isCompact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            if (!isCompact && onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除标签",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "标准模式")
@Composable
private fun BrainTagChipPreview() {
    EmpathyTheme {
        BrainTagChip(
            text = "同事",
            isCompact = false,
            onDelete = {}
        )
    }
}

@Preview(name = "紧凑模式")
@Composable
private fun BrainTagChipCompactPreview() {
    EmpathyTheme {
        BrainTagChip(
            text = "同事",
            isCompact = true
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(name = "多个标签")
@Composable
private fun BrainTagChipMultiplePreview() {
    EmpathyTheme {
        Surface {
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrainTagChip(text = "同事", onDelete = {})
                BrainTagChip(text = "喜欢运动", onDelete = {})
                BrainTagChip(text = "技术爱好者", onDelete = {})
                BrainTagChip(text = "创业者", onDelete = {})
            }
        }
    }
}

@Preview(name = "深色模式", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BrainTagChipDarkPreview() {
    EmpathyTheme {
        Surface {
            BrainTagChip(
                text = "朋友",
                onDelete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
```

### 5.3 使用示例

```kotlin
// 在ContactDetailScreen中使用
@Composable
fun BrainTagsSection(
    tags: List<BrainTag>,
    onTagDelete: (BrainTag) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "脑标签",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                BrainTagChip(
                    text = tag.label,
                    onDelete = { onTagDelete(tag) }
                )
            }
        }
    }
}
```

### 5.4 验证清单

- [ ] 支持标准和紧凑两种模式
- [ ] 可选的删除按钮
- [ ] 使用主题颜色
- [ ] 适配深色模式
- [ ] 文字大小合适

---

## 六、MessageBubble - 消息气泡

### 6.1 组件目标

聊天消息气泡,支持用户和对方两种样式。

### 6.2 完整实现

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/ui/component/MessageBubble.kt`

```kotlin
package com.empathy.ai.presentation.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ChatMessage
import com.empathy.ai.domain.model.MessageSender
import com.empathy.ai.presentation.theme.EmpathyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 消息气泡组件
 *
 * 用于显示聊天消息,区分用户和对方样式
 *
 * @param message 消息对象
 * @param showTimestamp 是否显示时间戳
 * @param modifier 修饰符
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    showTimestamp: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isFromMe = message.sender == MessageSender.ME
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromMe) 16.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 16.dp
            ),
            color = if (isFromMe) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 消息内容
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFromMe) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                // 时间戳
                if (showTimestamp) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFromMe) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "我的消息")
@Composable
private fun MessageBubbleMyMessagePreview() {
    EmpathyTheme {
        Surface {
            MessageBubble(
                message = ChatMessage(
                    id = "1",
                    content = "你好,最近怎么样?",
                    sender = MessageSender.ME,
                    timestamp = System.currentTimeMillis()
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "对方消息")
@Composable
private fun MessageBubbleTheirMessagePreview() {
    EmpathyTheme {
        Surface {
            MessageBubble(
                message = ChatMessage(
                    id = "2",
                    content = "我很好,谢谢!你呢?",
                    sender = MessageSender.THEM,
                    timestamp = System.currentTimeMillis()
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "长文本消息")
@Composable
private fun MessageBubbleLongTextPreview() {
    EmpathyTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageBubble(
                    message = ChatMessage(
                        id = "3",
                        content = "这是一条很长的消息,用来测试消息气泡在显示长文本时的换行效果。消息应该能够正确地自动换行,并且保持良好的可读性。",
                        sender = MessageSender.ME,
                        timestamp = System.currentTimeMillis()
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                MessageBubble(
                    message = ChatMessage(
                        id = "4",
                        content = "收到!我也想测试一下长文本在对方消息气泡中的显示效果。看起来效果还不错,文本能够正确换行。",
                        sender = MessageSender.THEM,
                        timestamp = System.currentTimeMillis()
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Preview(name = "深色模式", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessageBubbleDarkPreview() {
    EmpathyTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MessageBubble(
                    message = ChatMessage(
                        id = "5",
                        content = "深色模式下的消息",
                        sender = MessageSender.ME,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                MessageBubble(
                    message = ChatMessage(
                        id = "6",
                        content = "看起来不错!",
                        sender = MessageSender.THEM,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
```

### 6.3 使用示例

```kotlin
// 在ChatScreen中使用
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(uiState.messages) { message ->
            MessageBubble(
                message = message,
                showTimestamp = true
            )
        }
    }
}
```

### 6.4 验证清单

- [ ] 用户和对方消息样式不同
- [ ] 气泡圆角方向正确
- [ ] 时间戳格式正确
- [ ] 长文本自动换行
- [ ] 支持深色模式

---

## 七、组件测试

### 7.1 单元测试示例

虽然Compose组件主要通过预览测试,但也可以编写UI测试:

```kotlin
// BrainTagChipTest.kt
@Test
fun brainTagChip_displaysText() {
    composeTestRule.setContent {
        BrainTagChip(text = "测试标签")
    }
    
    composeTestRule
        .onNodeWithText("测试标签")
        .assertIsDisplayed()
}

@Test
fun brainTagChip_deleteButton_callsCallback() {
    var deleted = false
    
    composeTestRule.setContent {
        BrainTagChip(
            text = "测试标签",
            onDelete = { deleted = true }
        )
    }
    
    composeTestRule
        .onNodeWithContentDescription("删除标签")
        .performClick()
    
    assertTrue(deleted)
}
```

### 7.2 视觉回归测试

使用Screenshot Testing进行视觉回归测试:

```kotlin
@Test
fun contactCard_screenshot() {
    composeTestRule.setContent {
        ContactCard(
            contact = mockContact,
            onClick = {}
        )
    }
    
    composeTestRule
        .onRoot()
        .captureToImage()
        .assertAgainstGolden("contact_card")
}
```

---

## 八、组件文档

### 8.1 组件使用指南

创建组件使用文档: 