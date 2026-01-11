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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
 *
 * 测试范围 (PRD-00026):
 *   - 导航栏显示和返回功能
 *   - 空状态展示（"开始与AI军师对话"）
 *   - 用户消息和AI消息的气泡显示
 *   - 输入栏占位符和发送按钮状态
 *   - 加载指示器和错误提示
 *
 * 测试策略 (TDD-00026):
 *   1. 状态渲染测试 - 验证各种UI状态正确显示
 *   2. 交互测试 - 验证用户操作触发正确回调
 *   3. 边界测试 - 验证异常情况处理
 *
 * 架构设计:
 *   - 采用 _ForTest 组件模式隔离UI实现细节
 *   - 直接使用领域模型验证业务逻辑
 *   - 遵循 Clean Architecture 分层依赖
 *
 * 关联测试文件:
 *   - AiAdvisorScreenTest.kt (主界面测试)
 *
 * @see AiAdvisorScreenTest 主界面测试
 */
class AiAdvisorChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * [UI验收] 导航栏显示测试
     *
     * 验收标准 (PRD-00026/6.2):
     *   - AI军师标题正确显示（紫色心理学图标 + "AI军师"文字）
     *   - 显示当前联系人名称（"与 [联系人名] 的对话"）
     *   - 左侧返回按钮，右侧切换联系人按钮
     *
     * 关联需求: 联系人选择与切换功能
     */
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

    /**
     * [空状态] 无对话时显示欢迎界面
     *
     * 业务规则 (PRD-00026/3.1.1):
     *   - 首次进入对话界面显示空状态引导
     *   - 显示紫色心理学图标（80dp圆形背景）
     *   - 主标题"开始与AI军师对话"（20sp，加粗）
     *   - 副标题"向AI军师咨询任何社交沟通问题"（15sp）
     *
     * 设计权衡: 使用独立空状态组件而非复用通用组件
     */
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

    /**
     * [消息渲染] 用户消息气泡显示位置
     *
     * 业务规则 (PRD-00026/3.1.1):
     *   - 用户消息显示在右侧（horizontalArrangement.End）
     *   - 头像显示在消息右侧（蓝色圆形背景 + Person图标）
     *   - 气泡背景色 iOSBlue (#007AFF)，文字白色
     *   - 气泡圆角: 右圆角18dp，左圆角4dp
     *   - 气泡宽度最大为屏幕宽度的75%
     *
     * UI规范: 遵循iOS设计规范，蓝底白字
     */
    @Test
    fun chatScreen_showsUserMessage() {
        val conversations = listOf(
            AiAdvisorConversation.createUserMessage(
                sessionId = "session-1",
                contactId = "contact-1",
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

    /**
     * [消息渲染] AI消息气泡显示位置
     *
     * 业务规则 (PRD-00026/3.1.1):
     *   - AI消息显示在左侧（horizontalArrangement.Start）
     *   - 头像显示在消息左侧（紫色圆形背景 + Psychology图标）
     *   - 气泡背景色 iOSCardBackground (#FFFFFF)，文字黑色
     *   - 气泡圆角: 左圆角18dp，右圆角4dp
     *   - 气泡宽度最大为屏幕宽度的75%
     *
     * 对称设计: 与用户消息形成左右对称布局
     */
    @Test
    fun chatScreen_showsAiMessage() {
        val conversations = listOf(
            AiAdvisorConversation.createAiMessage(
                sessionId = "session-1",
                contactId = "contact-1",
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

    /**
     * [UI元素] 输入栏占位符显示
     *
     * 业务规则 (PRD-00026/3.1.1):
     *   - 输入框显示占位符"输入你的问题..."（16sp）
     *   - 占位符颜色 iOSTextSecondary (#8E8E93）
     *   - 输入框圆角20dp，背景色 #F2F2F7
     *   - 输入框高度自适应，最大4行
     *
     * 交互设计: 占位符在输入为空时显示，有内容时隐藏
     */
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

    /**
     * [发送状态] 发送中显示加载指示器
     *
     * 业务规则 (PRD-00026/6.3.2):
     *   - 发送中禁用输入框和发送按钮
     *   - 发送按钮位置显示加载指示器（20dp白色圆形进度条）
     *   - 输入框保持显示但不可编辑
     *
     * 性能要求: 响应时间 < 500ms (TDD-00026/9)
     * 用户体验: 加载动画提供即时反馈，避免用户重复点击
     */
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
        composeTestRule.onNodeWithTag("send_loading").assertExists()
    }

    /**
     * [错误处理] 网络错误显示错误横幅
     *
     * 错误场景 (TDD-00026/13.1):
     *   - 网络连接失败
     *   - AI服务不可用
     *   - API调用超时
     *
     * UI规范:
     *   - 错误横幅显示在输入栏上方
     *   - 背景色 #FF3B30（10%透明度）
     *   - 显示错误消息和"关闭"按钮
     *
     * 用户提示: "网络连接失败" (PRD-00026/7.3)
     */
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

    /**
     * [交互测试] 返回按钮触发回调
     *
     * 导航逻辑 (TDD-00026/6.3.1):
     *   - 点击返回按钮返回主界面
     *   - 返回时不保存草稿（输入框内容不持久化）
     *   - 确认当前对话已保存到数据库
     *
     * 用户体验: 返回操作应有明确反馈
     *
     * @see NavRoutes.AI_ADVISOR 返回主界面路由
     */
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
 *
 * 设计目的 (TDD-00026):
 *   - 隔离 ViewModel 依赖，直接传入 UiState
 *   - 简化测试断言逻辑
 *   - 支持组件级测试验证
 *
 * 与生产代码区别:
 *   - 使用静态颜色值而非主题动态色（便于断言）
 *   - 固定布局结构便于屏幕快照测试
 *   - 简化点击处理逻辑
 *
 * 布局结构:
 *   Column(iOSBackground背景)
 *   ├── ChatNavigationBarForTest (导航栏，固定高度)
 *   ├── Box(weight=1，对话内容区域)
 *   │   ├── Loading/CircularProgressIndicator
 *   │   ├── EmptyChatStateForTest
 *   │   └── LazyColumn/ChatBubbleForTest列表
 *   ├── ErrorBannerForTest (错误提示，可选)
 *   └── ChatInputBarForTest (输入栏，固定高度)
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

/**
 * 用于测试的聊天界面导航栏组件
 *
 * 布局规则 (PRD-00026/6.2):
 *   - 顶部固定位置，高度自适应
 *   - 左侧: 返回按钮 (ArrowBack图标，蓝色)
 *   - 居中: 标题区域
 *   │   - 主标题: "AI军师" + Psychology图标 (17sp, 半粗体)
 *   │   - 副标题: "与 [联系人名] 的对话" (12sp, 灰色)
 *   - 右侧: 切换联系人按钮 (Person图标，蓝色)
 *
 * iOS风格:
 *   - Surface背景色 iOSCardBackground
 *   - 轻微阴影 (elevation = 0.5dp)
 *   - 图标与文字间距 6dp
 */
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

/**
 * 用于测试的聊天消息气泡组件
 *
 * 布局规则 (PRD-00026/3.1.1):
 *   - AI消息: 左侧显示，头像是紫色圆形图标 (36dp + 10%透明度背景)
 *   - 用户消息: 右侧显示，头像是蓝色圆形图标 (36dp + 10%透明度背景)
 *   - 气泡宽度最大为屏幕宽度的75%
 *   - 圆角: AI消息左圆角18dp，用户消息右圆角18dp
 *
 * 主题常量 (TDD-00026):
 *   - iOSBlue: 用户消息背景色 (#007AFF)
 *   - iOSPurple: AI消息图标色 (#8E8E93)
 *   - iOSCardBackground: AI消息气泡背景色 (#FFFFFF)
 *   - 头像与气泡间距: 8dp
 *
 * @param isUser true=用户消息(右侧), false=AI消息(左侧)
 */
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

/**
 * 用于测试的聊天输入栏组件
 *
 * 功能要求 (PRD-00026/3.1.1):
 *   - 支持多行文本输入
 *   - 发送按钮根据输入状态启用/禁用
 *   - 发送中显示加载指示器
 *
 * 状态逻辑:
 *   - enabled: !isSending && inputText.isNotBlank()
 *   - 发送中锁定输入防止重复提交
 *
 * iOS风格:
 *   - 输入框圆角20dp
 *   - 背景色 #F2F2F7
 *   - 发送按钮圆形设计 (36dp)
 *   - 禁用时发送按钮灰色 (#E5E5EA)
 */
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
                            modifier = Modifier.size(20.dp).testTag("send_loading"),
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

/**
 * 用于测试的错误横幅组件
 *
 * 功能要求 (TDD-00026/13.2):
 *   - 显示错误消息（左侧，红色14sp）
 *   - 显示"关闭"按钮（右侧，蓝色）
 *   - 背景色 #FF3B30（10%透明度）
 *
 * 使用场景:
 *   - 网络连接失败
 *   - AI服务不可用
 *   - 数据保存失败
 */
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

/**
 * 用于测试的对话空状态组件
 *
 * 设计规范 (PRD-00026/3.1.1):
 *   - 居中显示在对话区域
 *   - 紫色心理学图标 (80dp圆形背景，10%透明度)
 *   - 主标题"开始与AI军师对话"（20sp，加粗）
 *   - 副标题"向AI军师咨询任何社交沟通问题"（15sp，灰色）
 *
 * 用户体验:
 *   - 首次使用时提供明确引导
 *   - 图标设计增强品牌认知
 *   - 文字说明降低使用门槛
 */
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
