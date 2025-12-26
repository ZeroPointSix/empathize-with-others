package com.empathy.ai.presentation.ui.screen.chat

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ChatMessage
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MessageSender
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.presentation.theme.AppSpacing
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.card.AnalysisCard
import com.empathy.ai.presentation.ui.component.input.CustomTextField
import com.empathy.ai.presentation.ui.component.message.MessageBubble
import com.empathy.ai.presentation.ui.component.state.EmptyView
import com.empathy.ai.presentation.ui.component.state.FriendlyErrorCard
import com.empathy.ai.presentation.ui.component.state.LoadingIndicator
import com.empathy.ai.presentation.util.UserFriendlyError
import androidx.compose.material.icons.filled.Warning
import com.empathy.ai.presentation.ui.component.state.LoadingIndicatorFullScreen
import com.empathy.ai.presentation.viewmodel.ChatViewModel

/**
 * 聊天分析页面
 *
 * 功能：
 * - 显示聊天消息列表
 * - 输入新消息
 * - 分析聊天内容（调用AI）
 * - 检查草稿安全性
 * - 显示分析结果
 *
 * @param contactId 联系人ID
 * @param onNavigateBack 返回回调
 * @param viewModel 聊天ViewModel
 * @param modifier Modifier
 */
@Composable
fun ChatScreen(
    contactId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 加载聊天数据
    LaunchedEffect(contactId) {
        viewModel.onEvent(ChatUiEvent.LoadChat(contactId))
    }

    ChatScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * 聊天页面内容（无状态）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    onEvent: (ChatUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.contactProfile?.name ?: "聊天") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEvent(ChatUiEvent.AnalyzeChat) },
                        enabled = uiState.hasMessages && !uiState.isAnalyzing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "分析聊天"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicatorFullScreen(
                        message = "加载聊天记录..."
                    )
                }
                uiState.error != null -> {
                    FriendlyErrorCard(
                        error = UserFriendlyError(
                            title = "出错了",
                            message = uiState.error ?: "未知错误",
                            icon = Icons.Default.Warning
                        ),
                        onAction = { onEvent(ChatUiEvent.RefreshChat) }
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 消息列表
                        MessageList(
                            messages = uiState.messages,
                            modifier = Modifier.weight(1f)
                        )

                        // 安全警告
                        if (uiState.shouldShowSafetyWarning) {
                            SafetyWarningBanner(
                                message = uiState.safetyCheckResult?.suggestion ?: "此消息可能不太合适",
                                onDismiss = { onEvent(ChatUiEvent.DismissSafetyWarning) }
                            )
                        }

                        // 输入区域
                        MessageInputSection(
                            inputText = uiState.inputText,
                            onInputChange = { onEvent(ChatUiEvent.UpdateInputText(it)) },
                            onSend = { onEvent(ChatUiEvent.SendMessage(uiState.inputText)) },
                            canSend = uiState.canSendMessage
                        )
                    }
                }
            }

            // 分析中的加载指示器
            if (uiState.isAnalyzing) {
                LoadingIndicatorFullScreen(
                    message = "正在分析聊天内容..."
                )
            }
        }
    }

    // 分析结果对话框
    if (uiState.showAnalysisDialog && uiState.analysisResult != null) {
        AnalysisResultDialog(
            result = uiState.analysisResult,
            onDismiss = { onEvent(ChatUiEvent.DismissAnalysisDialog) },
            onApplySuggestion = { suggestion ->
                onEvent(ChatUiEvent.ApplySuggestion(suggestion))
            }
        )
    }
}

/**
 * 消息列表
 */
@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messages.isEmpty()) {
        EmptyView(
            message = "还没有消息",
            actionText = null,
            onAction = null
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                MessageBubble(
                    text = message.content,
                    timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(message.timestamp)),
                    isFromUser = message.sender == com.empathy.ai.domain.model.MessageSender.ME
                )
            }
        }
    }
}

/**
 * 安全警告横幅
 */
@Composable
private fun SafetyWarningBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️ $message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("忽略")
            }
        }
    }
}

/**
 * 消息输入区域
 */
@Composable
private fun MessageInputSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            CustomTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = "输入消息...",
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onSend,
                enabled = canSend
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "发送",
                    tint = if (canSend) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}

/**
 * 分析结果对话框
 */
@Composable
private fun AnalysisResultDialog(
    result: AnalysisResult,
    onDismiss: () -> Unit,
    onApplySuggestion: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 分析结果") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                AnalysisCard(
                    analysisResult = result,
                    onCopyReply = { onApplySuggestion(result.replySuggestion) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApplySuggestion(result.replySuggestion) }) {
                Text("应用建议")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

// ==================== Previews ====================

@Preview(name = "聊天页面 - 默认", showBackground = true)
@Composable
private fun ChatScreenPreview() {
    EmpathyTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                contactId = "1",
                contactProfile = ContactProfile(
                    id = "1",
                    name = "张三",
                    targetGoal = "建立良好的合作关系",
                    contextDepth = 10,
                    facts = emptyList()
                ),
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        content = "你好，很高兴认识你！",
                        sender = MessageSender.THEM,
                        timestamp = System.currentTimeMillis() - 3600000
                    ),
                    ChatMessage(
                        id = "2",
                        content = "你好！我也很高兴认识你。",
                        sender = MessageSender.ME,
                        timestamp = System.currentTimeMillis() - 3000000
                    ),
                    ChatMessage(
                        id = "3",
                        content = "最近工作怎么样？",
                        sender = MessageSender.THEM,
                        timestamp = System.currentTimeMillis() - 1800000
                    ),
                    ChatMessage(
                        id = "4",
                        content = "还不错，最近在做一个新项目。",
                        sender = MessageSender.ME,
                        timestamp = System.currentTimeMillis() - 600000
                    )
                ),
                inputText = ""
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "聊天页面 - 空状态", showBackground = true)
@Composable
private fun ChatScreenEmptyPreview() {
    EmpathyTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                contactId = "1",
                contactProfile = ContactProfile(
                    id = "1",
                    name = "张三",
                    targetGoal = "建立良好的合作关系",
                    contextDepth = 10,
                    facts = emptyList()
                ),
                messages = emptyList()
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "聊天页面 - 安全警告", showBackground = true)
@Composable
private fun ChatScreenSafetyWarningPreview() {
    EmpathyTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                contactId = "1",
                contactProfile = ContactProfile(
                    id = "1",
                    name = "张三",
                    targetGoal = "建立良好的合作关系",
                    contextDepth = 10,
                    facts = emptyList()
                ),
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        content = "你好",
                        sender = MessageSender.ME,
                        timestamp = System.currentTimeMillis()
                    )
                ),
                inputText = "你最近工作压力大吗？",
                showSafetyWarning = true
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "聊天页面 - 分析中", showBackground = true)
@Composable
private fun ChatScreenAnalyzingPreview() {
    EmpathyTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                contactId = "1",
                contactProfile = ContactProfile(
                    id = "1",
                    name = "张三",
                    targetGoal = "建立良好的合作关系",
                    contextDepth = 10,
                    facts = emptyList()
                ),
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        content = "你好",
                        sender = MessageSender.ME,
                        timestamp = System.currentTimeMillis()
                    )
                ),
                isAnalyzing = true
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "聊天页面 - 深色模式",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ChatScreenDarkPreview() {
    EmpathyTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                contactId = "1",
                contactProfile = ContactProfile(
                    id = "1",
                    name = "张三",
                    targetGoal = "建立良好的合作关系",
                    contextDepth = 10,
                    facts = emptyList()
                ),
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        content = "你好",
                        sender = MessageSender.THEM,
                        timestamp = System.currentTimeMillis()
                    ),
                    ChatMessage(
                        id = "2",
                        content = "你好！",
                        sender = MessageSender.ME,
                        timestamp = System.currentTimeMillis()
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

