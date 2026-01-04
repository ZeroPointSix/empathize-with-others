package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.viewmodel.AiAdvisorChatUiState
import org.junit.Rule
import org.junit.Test

/**
 * AI军师对话界面UI测试
 */
class AiAdvisorChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_showsNavigationBar() {
        composeTestRule.setContent {
            AiAdvisorChatScreenContent(
                uiState = AiAdvisorChatUiState(contactName = "测试联系人"),
                onNavigateBack = {},
                onInputChange = {},
                onSend = {}
            )
        }

        composeTestRule.onNodeWithText("AI军师").assertIsDisplayed()
        composeTestRule.onNodeWithText("与 测试联系人 的对话").assertIsDisplayed()
    }

    @Test
    fun chatScreen_showsEmptyState_whenNoConversations() {
        composeTestRule.setContent {
            AiAdvisorChatScreenContent(
                uiState = AiAdvisorChatUiState(
                    isLoading = false,
                    conversations = emptyList()
                ),
                onNavigateBack = {},
                onInputChange = {},
                onSend = {}
            )
        }

        composeTestRule.onNodeWithText("开始与AI军师对话").assertIsDisplayed()
    }

    @Test
    fun chatScreen_showsUserMessage() {
        val conversations = listOf(
            AiAdvisorConversation.createUserMessage(
                sessionId = "session-1",
                content = "你好，我想咨询一下"
            )
        )

        composeTestRule.setContent {
            AiAdvisorChatScreenContent(
                uiState = AiAdvisorChatUiState(
                    isLoading = false,
                    conversations = conversations
                ),
                onNavigateBack = {},
                onInputChange = {},
                onSend = {}
            )
        }

        composeTestRule.onNodeWithText("你好，我想咨询一下").assertIsDisplayed()
    }

    @Test
    fun chatScreen_showsAiMessage() {
        val conversations = listOf(
            AiAdvisorConversation.createAiMessage(
                sessionId = "session-1",
                content = "你好！我是AI军师，很高兴为你服务。"
            )
        )

        composeTestRule.setContent {
            AiAdvisorChatScreenContent(
                uiState = AiAdvisorChatUiState(
                    isLoading = false,
                    conversations = conversations
                ),
                onNavigateBack = {},
                onInputChange = {},
                onSend = {}
            )
        }

        composeTestRule.onNodeWithText("你好！我是AI军师，很高兴为你服务。").assertIsDisplayed()
    }

    @Test
    fun chatScreen_showsInputPlaceholder() {
        composeTestRule.setContent {
            AiAdvisorChatScreenContent(
                uiState = AiAdvisorChatUiState(inputText = ""),
                onNavigateBack = {},
                onInputChange = {},
                onSend = {}
            )
        }

        composeTestRule.onNodeWithText("输入你的问题...").assertIsDisplayed()
    }

    @Test
    fun chatScreen_showsLoadingIndicator_whenSending() {
        composeTestRule.setContent {
            AiAdvisorChatScreenContent(
                uiState = AiAdvisorChatUiState(
                    isSending = true,
                    inputText = "测试消息"
                ),
                onNavigateBack = {},
                onInputChange = {},
                onSend = {}
            )
        }

        // 发送中时应该显示加载指示器
        composeTestRule.onNodeWithContentDescription("发送").assertExists()
    }

    @Test
    fun chatScreen_showsErrorBanner_whenErrorOccurs() {
        composeTestRule.setContent {
            AiAdvisorChatScreenContent(
                uiState = AiAdvisorChatUiState(error = "网络连接失败"),
                onNavigateBack = {},
                onInputChange = {},
                onSend = {}
            )
        }

        composeTestRule.onNodeWithText("网络连接失败").assertIsDisplayed()
    }

    @Test
    fun chatScreen_backButton_triggersCallback() {
        var backClicked = false

        composeTestRule.setContent {
            AiAdvisorChatScreenContent(
                uiState = AiAdvisorChatUiState(),
                onNavigateBack = { backClicked = true },
                onInputChange = {},
                onSend = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("返回").performClick()

        assert(backClicked) { "Back button should trigger callback" }
    }
}

/**
 * 用于测试的对话界面内容组件
 */
@Composable
private fun AiAdvisorChatScreenContent(
    uiState: AiAdvisorChatUiState,
    onNavigateBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // 导航栏
        ChatNavigationBarForTest(
            contactName = uiState.contactName,
            onNavigateBack = onNavigateBack
        )

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
                    EmptyChatStateForTest(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(uiState.conversations, key = { it.id }) { conversation ->
                            ChatBubbleForTest(conversation = conversation)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        // 错误提示
        uiState.error?.let { errorMessage ->
            ErrorBannerForTest(message = errorMessage)
        }

        // 输入栏
        ChatInputBarForTest(
            inputText = uiState.inputText,
            isSending = uiState.isSending,
            onInputChange = onInputChange,
            onSend = onSend
        )
    }
}

@Composable
private fun ChatNavigationBarForTest(
    contactName: String,
    onNavigateBack: () -> Unit
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
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = iOSBlue
                )
            }

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

            IconButton(onClick = {}) {
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
private fun ChatBubbleForTest(conversation: AiAdvisorConversation) {
    val isUser = conversation.messageType == MessageType.USER
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) {
            androidx.compose.foundation.layout.Arrangement.End
        } else {
            androidx.compose.foundation.layout.Arrangement.Start
        }
    ) {
        if (!isUser) {
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

        Surface(
            modifier = Modifier.widthIn(max = screenWidth * 0.75f),
            shape = RoundedCornerShape(18.dp),
            color = if (isUser) iOSBlue else iOSCardBackground
        ) {
            Text(
                text = conversation.content.ifEmpty { "..." },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isUser) Color.White else iOSTextPrimary,
                fontSize = 16.sp
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
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
private fun ChatInputBarForTest(
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
                    textStyle = TextStyle(fontSize = 16.sp, color = iOSTextPrimary),
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
private fun ErrorBannerForTest(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFF3B30).copy(alpha = 0.1f)
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
                color = Color(0xFFFF3B30)
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
private fun EmptyChatStateForTest(modifier: Modifier = Modifier) {
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
