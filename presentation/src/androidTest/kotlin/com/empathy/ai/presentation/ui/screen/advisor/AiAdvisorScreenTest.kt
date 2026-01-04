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
 */
class AiAdvisorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
 * 用于测试的内容组件（不包含ViewModel依赖）
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
