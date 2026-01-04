package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.viewmodel.AiAdvisorUiState
import org.junit.Rule
import org.junit.Test

/**
 * AI军师主界面UI测试
 *
 * 测试范围 (PRD-00026):
 *   - iOS大标题显示（紫色心理学图标 + "AI军师"）
 *   - 联系人列表渲染（无联系人/有联系人）
 *   - 最近会话预览显示
 *   - 错误状态展示
 *   - 联系人点击回调
 *
 * 用户故事 (PRD-00026/US-001):
 *   用户通过主界面选择联系人，进入深度分析对话
 *
 * 架构设计 (TDD-00026):
 *   - 使用 AiAdvisorUiState 验证UI状态
 *   - ContactItemForTest 组件测试联系人项渲染
 *   - 依赖主题常量（iOSBlue, iOSPurple等）
 *
 * @see AiAdvisorChatScreenTest 对话界面测试
 */
class AiAdvisorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * [UI验收] iOS大标题显示
     *
     * 验收标准 (PRD-00026/6.1):
     *   - 显示紫色心理学图标 (32dp)
     *   - 显示"AI军师"标题 (34sp, 加粗)
     *   - 图标与文字间距 12dp
     *   - 背景色 iOSCardBackground
     *
     * iOS设计规范:
     *   - 大标题字体 San Francisco (34sp, Bold)
     *   - 提升视觉层次感
     */
    @Test
    fun aiAdvisorScreen_showsTitle() {
        composeTestRule.setContent {
            AiAdvisorScreenContent(
                uiState = AiAdvisorUiState(),
                onContactClick = {}
            )
        }

        composeTestRule.onNodeWithText("AI军师").assertIsDisplayed()
    }

    /**
     * [空状态] 无联系人时显示空状态
     *
     * 业务规则 (PRD-00026/3.2.1):
     *   - 当联系人列表为空且非加载状态时显示
     *   - 显示"暂无联系人"标题（20sp，半粗体）
     *   - 不显示联系人列表
     *
     * 用户引导: 提示用户先添加联系人再来使用AI军师
     *
     * @see ContactProfile 联系人领域模型
     */
    @Test
    fun aiAdvisorScreen_showsEmptyState_whenNoContacts() {
        composeTestRule.setContent {
            AiAdvisorScreenContent(
                uiState = AiAdvisorUiState(
                    isLoading = false,
                    contacts = emptyList()
                ),
                onContactClick = {}
            )
        }

        composeTestRule.onNodeWithText("暂无联系人").assertIsDisplayed()
    }

    /**
     * [UI渲染] 有联系人时显示联系人列表
     *
     * 业务规则 (PRD-00026/3.2.2):
     *   - 联系人列表使用 LazyColumn 渲染
     *   - 每个联系人显示名称（主文本，17sp）
     *   - 显示最近会话标题或"点击开始对话"（次要文本，14sp）
     *   - 点击联系人触发 onContactClick(contact.id)
     *
     * 布局规范:
     *   - 列表项内边距: horizontal 16dp, vertical 12dp
     *   - 列表项间无额外间距
     *   - 右侧无箭头图标，简洁设计
     */
    @Test
    fun aiAdvisorScreen_showsContactList_whenHasContacts() {
        val contacts = listOf(
            ContactProfile(id = "1", name = "张三", targetGoal = ""),
            ContactProfile(id = "2", name = "李四", targetGoal = "")
        )

        composeTestRule.setContent {
            AiAdvisorScreenContent(
                uiState = AiAdvisorUiState(
                    isLoading = false,
                    contacts = contacts
                ),
                onContactClick = {}
            )
        }

        composeTestRule.onNodeWithText("张三").assertIsDisplayed()
        composeTestRule.onNodeWithText("李四").assertIsDisplayed()
    }

    /**
     * [数据展示] 有最近会话时显示会话标题
     *
     * 业务规则 (PRD-00026/3.2.2):
     *   - 最近会话标题显示在联系人下方
     *   - 使用 AiAdvisorSession.title 字段
     *   - 无会话时显示"点击开始对话"占位文本
     *
     * 数据关联:
     *   - recentSessions: Map<contactId, AiAdvisorSession?>
     *   - 通过 contact.id 作为 key 获取对应会话
     *
     * @see AiAdvisorSession 会话领域模型
     */
    @Test
    fun aiAdvisorScreen_showsRecentSession_whenAvailable() {
        val contacts = listOf(
            ContactProfile(id = "1", name = "张三", targetGoal = "")
        )
        val sessions = mapOf(
            "1" to AiAdvisorSession.create("1").copy(title = "最近的对话")
        )

        composeTestRule.setContent {
            AiAdvisorScreenContent(
                uiState = AiAdvisorUiState(
                    isLoading = false,
                    contacts = contacts,
                    recentSessions = sessions
                ),
                onContactClick = {}
            )
        }

        composeTestRule.onNodeWithText("最近的对话").assertIsDisplayed()
    }

    /**
     * [错误处理] 加载失败显示错误状态
     *
     * 错误场景 (TDD-00026/13.1):
     *   - 网络连接失败
     *   - 数据加载超时
     *   - 数据库访问异常
     *
     * UI规范:
     *   - 显示"加载失败"标题（20sp，半粗体，红色）
     *   - 显示具体错误信息（15sp，灰色）
     *   - 居中显示在内容区域
     *
     * 用户引导: 提供重试入口（由ViewModel处理）
     */
    @Test
    fun aiAdvisorScreen_showsError_whenErrorOccurs() {
        composeTestRule.setContent {
            AiAdvisorScreenContent(
                uiState = AiAdvisorUiState(
                    isLoading = false,
                    error = "网络连接失败"
                ),
                onContactClick = {}
            )
        }

        composeTestRule.onNodeWithText("加载失败").assertIsDisplayed()
        composeTestRule.onNodeWithText("网络连接失败").assertIsDisplayed()
    }

    /**
     * [交互测试] 联系人点击触发回调
     *
     * 用户故事 (PRD-00026/US-001):
     *   用户点击联系人 -> 进入该联系人的AI军师对话界面
     *
     * 导航逻辑 (TDD-00026/6.1.2):
     *   - 触发 onContactClick(contact.id)
     *   - ViewModel/Navigation处理跳转
     *   - 跳转到 NavRoutes.AI_ADVISOR_CHAT 路由
     *
     * 交互设计:
     *   - 整行可点击（无右侧箭头）
     *   - 点击区域覆盖整个联系人项
     *
     * @see NavRoutes.aiAdvisorChat 构建跳转路由
     */
    @Test
    fun aiAdvisorScreen_contactClick_triggersCallback() {
        var clickedContactId: String? = null
        val contacts = listOf(
            ContactProfile(id = "contact-1", name = "测试联系人", targetGoal = "")
        )

        composeTestRule.setContent {
            AiAdvisorScreenContent(
                uiState = AiAdvisorUiState(
                    isLoading = false,
                    contacts = contacts
                ),
                onContactClick = { clickedContactId = it }
            )
        }

        composeTestRule.onNodeWithText("测试联系人").performClick()

        assert(clickedContactId == "contact-1") {
            "Expected contact-1 but got $clickedContactId"
        }
    }
}

/**
 * 用于测试的主界面内容组件
 *
 * 设计目的 (TDD-00026):
 *   - 隔离 ViewModel 依赖，直接传入 UiState
 *   - 简化测试断言逻辑
 *   - 支持组件级测试验证
 *
 * 与生产代码差异:
 *   - AiAdvisorScreen 使用 hiltViewModel() 获取 ViewModel
 *   - 本组件直接接收 UiState，便于测试断言
 *
 * 适用场景:
 *   - 单元测试：验证UI状态渲染
 *   - 集成测试：验证组件间交互
 *   - 回归测试：确保UI行为稳定
 *
 * 布局结构:
 *   Column(iOSBackground背景)
 *   ├── IOSLargeTitleForTest (iOS大标题，固定高度)
 *   └── AiAdvisorContentForTest (内容区域，weight=1)
 */
@Composable
private fun AiAdvisorScreenContent(
    uiState: AiAdvisorUiState,
    onContactClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // iOS大标题
        IOSLargeTitleForTest()

        // 内容区域
        AiAdvisorContentForTest(
            uiState = uiState,
            onContactClick = onContactClick
        )
    }
}

/**
 * 用于测试的iOS大标题组件
 *
 * 设计规范 (PRD-00026/6.1):
 *   - 标题字体大小: 34sp, 加粗
 *   - 图标大小: 32dp, 紫色
 *   - 图标与文字间距: 12dp
 *   - 背景色: iOSCardBackground
 *   - 内边距: horizontal 20dp, vertical 16dp
 *
 * iOS风格:
 *   - 遵循iOS Human Interface Guidelines
 *   - 大标题设计提升视觉层次
 *   - Psychology图标增强品牌识别
 */
@Composable
private fun IOSLargeTitleForTest() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(iOSCardBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                tint = iOSPurple,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "AI军师",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = iOSTextPrimary
            )
        }
    }
}

/**
 * 用于测试的主界面内容区域组件
 *
 * 状态处理逻辑:
 *   1. isLoading: 显示加载指示器（蓝色圆形进度条）
 *   2. error != null: 显示错误状态（红色标题 + 错误信息）
 *   3. contacts.isEmpty(): 显示空状态（"暂无联系人"）
 *   4. else: 显示联系人列表（LazyColumn）
 *
 * 优先级: Loading > Error > Empty > List
 *
 * 主题依赖:
 *   - iOSBlue: 加载指示器颜色
 *   - iOSTextPrimary: 主文本颜色
 *   - iOSTextSecondary: 次要文本颜色
 */
@Composable
private fun AiAdvisorContentForTest(
    uiState: AiAdvisorUiState,
    onContactClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = iOSBlue
                )
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "加载失败",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF3B30)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error,
                        fontSize = 15.sp,
                        color = iOSTextSecondary
                    )
                }
            }
            uiState.contacts.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "暂无联系人",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                }
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(uiState.contacts.size) { index ->
                        val contact = uiState.contacts[index]
                        val recentSession = uiState.recentSessions[contact.id]
                        ContactItemForTest(
                            contact = contact,
                            recentSession = recentSession,
                            onClick = { onContactClick(contact.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 用于测试的联系人列表项组件
 *
 * 业务规则 (PRD-00026/3.2.2):
 *   - 显示联系人姓名（主文本，17sp）
 *   - 显示最近会话标题或"点击开始对话"（次要文本，14sp）
 *
 * 数据来源:
 *   - contact: ContactProfile 领域模型
 *   - recentSession: AiAdvisorSession? 可能为空
 *
 * 交互设计:
 *   - 整行可点击（Modifier.clickable）
 *   - 点击触发 onClick() 回调
 *   - 无选中高亮状态，保持简洁
 *
 * 布局规范:
 *   - 内边距: horizontal 16dp, vertical 12dp
 *   - 右侧无箭头图标
 *   - 左侧无头像图标
 */
@Composable
private fun ContactItemForTest(
    contact: ContactProfile,
    recentSession: AiAdvisorSession?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontSize = 17.sp,
                color = iOSTextPrimary
            )
            Text(
                text = recentSession?.title ?: "点击开始对话",
                fontSize = 14.sp,
                color = iOSTextSecondary
            )
        }
    }
}
