package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.viewmodel.AiAdvisorChatUiState
import com.empathy.ai.presentation.viewmodel.AiAdvisorChatViewModel

/**
 * AI军师对话界面（iOS风格）
 */
@Composable
fun AiAdvisorChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContact: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: AiAdvisorChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Handle navigation
    LaunchedEffect(uiState.shouldNavigateToContact) {
        uiState.shouldNavigateToContact?.let { contactId ->
            onNavigateToContact(contactId)
            viewModel.clearNavigationState()
        }
    }

    // Scroll to bottom when new message arrives
    LaunchedEffect(uiState.conversations.size) {
        if (uiState.conversations.isNotEmpty()) {
            listState.animateScrollToItem(uiState.conversations.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
            .imePadding()
    ) {
        // iOS导航栏
        IOSChatNavigationBar(
            contactName = uiState.contactName,
            onNavigateBack = onNavigateBack,
            onContactSelect = { viewModel.showContactSelector() }
        )

        // 会话选择器
        if (uiState.sessions.isNotEmpty()) {
            SessionChips(
                sessions = uiState.sessions,
                currentSessionId = uiState.currentSessionId,
                onSessionSelect = viewModel::switchSession,
                onNewSession = { viewModel.createNewSession() }
            )
        }

        // 对话内容
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = iOSBlue
                    )
                }
                uiState.conversations.isEmpty() -> {
                    EmptyChatState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = uiState.conversations,
                            key = { it.id }
                        ) { conversation ->
                            ChatBubble(
                                conversation = conversation,
                                onRetry = { viewModel.retryMessage(conversation) },
                                onDelete = { viewModel.deleteMessage(conversation.id) }
                            )
                        }
                    }
                }
            }
        }

        // 错误提示
        uiState.error?.let { errorMessage ->
            ErrorBanner(
                message = errorMessage,
                onDismiss = { viewModel.clearError() }
            )
        }

        // 输入栏
        ChatInputBar(
            inputText = uiState.inputText,
            isSending = uiState.isSending,
            onInputChange = viewModel::updateInput,
            onSend = viewModel::sendMessage
        )
    }

    // 联系人选择对话框
    if (uiState.showContactSelector) {
        ContactSelectorDialog(
            contacts = uiState.allContacts,
            onSelect = viewModel::switchContact,
            onDismiss = viewModel::hideContactSelector
        )
    }

    // 切换确认对话框
    if (uiState.showSwitchConfirmDialog) {
        SwitchConfirmDialog(
            onConfirm = viewModel::confirmSwitch,
            onDismiss = viewModel::cancelSwitch
        )
    }
}

@Composable
private fun IOSChatNavigationBar(
    contactName: String,
    onNavigateBack: () -> Unit,
    onContactSelect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = iOSCardBackground,
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = iOSBlue
                )
            }

            // 标题区域
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = iOSPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI军师",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                }
                if (contactName.isNotEmpty()) {
                    Text(
                        text = "与 $contactName 的对话",
                        fontSize = 12.sp,
                        color = iOSTextSecondary
                    )
                }
            }

            // 切换联系人按钮
            IconButton(onClick = onContactSelect) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "切换联系人",
                    tint = iOSBlue
                )
            }
        }
    }
}

@Composable
private fun SessionChips(
    sessions: List<AiAdvisorSession>,
    currentSessionId: String?,
    onSessionSelect: (String) -> Unit,
    onNewSession: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.background(iOSCardBackground)
    ) {
        // 新建会话按钮
        item {
            SessionChip(
                text = "新对话",
                isSelected = false,
                isNew = true,
                onClick = onNewSession
            )
        }

        // 现有会话
        items(sessions) { session ->
            SessionChip(
                text = session.title,
                isSelected = session.id == currentSessionId,
                onClick = { onSessionSelect(session.id) }
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(iOSSeparator)
    )
}

@Composable
private fun SessionChip(
    text: String,
    isSelected: Boolean,
    isNew: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) iOSBlue else Color(0xFFE5E5EA),
        label = "chipBackground"
    )
    val textColor = if (isSelected) Color.White else iOSTextPrimary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isNew) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChatBubble(
    conversation: AiAdvisorConversation,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val isUser = conversation.messageType == MessageType.USER
    val isFailed = conversation.sendStatus == SendStatus.FAILED
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI头像
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                color = iOSPurple.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = iOSPurple
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = screenWidth * 0.75f)
        ) {
            // 消息气泡
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = when {
                    isFailed -> iOSRed.copy(alpha = 0.1f)
                    isUser -> iOSBlue
                    else -> iOSCardBackground
                },
                shadowElevation = if (!isUser && !isFailed) 1.dp else 0.dp
            ) {
                Text(
                    text = conversation.content.ifEmpty { "..." },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = when {
                        isFailed -> iOSRed
                        isUser -> Color.White
                        else -> iOSTextPrimary
                    },
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            }

            // 失败消息操作
            if (isFailed) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.clickable(onClick = onRetry),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = iOSBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重试", fontSize = 12.sp, color = iOSBlue)
                    }
                    Row(
                        modifier = Modifier.clickable(onClick = onDelete),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = iOSRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除", fontSize = 12.sp, color = iOSRed)
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // 用户头像
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                color = iOSBlue.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = iOSBlue
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = iOSCardBackground,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 输入框
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF2F2F7)
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    enabled = !isSending,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = iOSTextPrimary
                    ),
                    cursorBrush = SolidColor(iOSBlue),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "输入你的问题...",
                                    fontSize = 16.sp,
                                    color = iOSTextSecondary
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 发送按钮
            Surface(
                onClick = { if (inputText.isNotBlank() && !isSending) onSend() },
                shape = CircleShape,
                color = if (inputText.isNotBlank() && !isSending) iOSBlue else Color(0xFFE5E5EA)
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (inputText.isNotBlank()) Color.White else iOSTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss),
        color = iOSRed.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = iOSRed
            )
            Text(
                text = "关闭",
                fontSize = 14.sp,
                color = iOSBlue
            )
        }
    }
}

@Composable
private fun ContactSelectorDialog(
    contacts: List<ContactProfile>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择联系人",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(contacts) { index, contact ->
                    Surface(
                        onClick = { onSelect(contact.id) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                color = iOSPurple.copy(alpha = 0.15f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp),
                                    tint = iOSPurple
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = contact.name,
                                fontSize = 17.sp,
                                color = iOSTextPrimary
                            )
                        }
                    }
                    if (index < contacts.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp)
                                .height(0.5.dp)
                                .background(iOSSeparator)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = iOSBlue)
            }
        },
        containerColor = iOSCardBackground
    )
}

@Composable
private fun SwitchConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "切换联系人",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "确定要切换到其他联系人吗？当前对话将被保存。",
                color = iOSTextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定", color = iOSBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = iOSTextSecondary)
            }
        },
        containerColor = iOSCardBackground
    )
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = iOSPurple.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                tint = iOSPurple.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "开始与AI军师对话",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "向AI军师咨询任何社交沟通问题",
            fontSize = 15.sp,
            color = iOSTextSecondary
        )
    }
}
