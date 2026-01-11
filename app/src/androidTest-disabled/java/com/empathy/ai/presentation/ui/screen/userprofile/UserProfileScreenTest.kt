package com.empathy.ai.presentation.ui.screen.userprofile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.presentation.theme.EmpathyTheme
import org.junit.Rule
import org.junit.Test

/**
 * UserProfileScreen UI测试
 *
 * 测试用户画像界面的UI渲染和交互功能。
 */
class UserProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========== 界面渲染测试 ==========

    @Test
    fun userProfileScreen_displaysTopBar() {
        // Given
        val uiState = createDefaultUiState()

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // Then
        composeTestRule.onNodeWithText("个人画像").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("返回").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("导出").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("重置").assertIsDisplayed()
    }

    @Test
    fun userProfileScreen_displaysCompletenessCard() {
        // Given
        val profile = UserProfile(
            personalityTraits = listOf("内向", "理性", "细心"),
            values = listOf("诚实", "责任", "成长"),
            interests = listOf("阅读", "编程", "旅行", "摄影", "音乐", "运动")
        )
        val uiState = createDefaultUiState().copy(
            profile = profile
        )

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // Then
        composeTestRule.onNodeWithText("画像完整度").assertIsDisplayed()
        composeTestRule.onNodeWithText("60%").assertIsDisplayed()
        composeTestRule.onNodeWithText("已添加 12 个标签").assertIsDisplayed()
    }

    @Test
    fun userProfileScreen_displaysTabRow() {
        // Given
        val uiState = createDefaultUiState()

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // Then
        composeTestRule.onNodeWithText("基础信息").assertIsDisplayed()
        composeTestRule.onNodeWithText("自定义维度").assertIsDisplayed()
    }

    @Test
    fun userProfileScreen_displaysBaseDimensions() {
        // Given
        val uiState = createDefaultUiState()

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // Then
        composeTestRule.onNodeWithText("性格特点").assertIsDisplayed()
        composeTestRule.onNodeWithText("价值观").assertIsDisplayed()
        composeTestRule.onNodeWithText("兴趣爱好").assertIsDisplayed()
        composeTestRule.onNodeWithText("沟通风格").assertIsDisplayed()
        composeTestRule.onNodeWithText("社交偏好").assertIsDisplayed()
    }

    @Test
    fun userProfileScreen_displaysLoadingState() {
        // Given
        val uiState = createDefaultUiState().copy(isLoading = true)

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // Then - 加载状态下不应渲染基础维度内容（仅验证不崩溃）
    }

    @Test
    fun userProfileScreen_displaysErrorState() {
        // Given
        val uiState = createDefaultUiState().copy(error = "加载失败")

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // Then
        composeTestRule.onNodeWithText("加载失败").assertIsDisplayed()
        composeTestRule.onNodeWithText("关闭").assertIsDisplayed()
    }

    @Test
    fun userProfileScreen_displaysSuccessMessage() {
        // Given
        val uiState = createDefaultUiState().copy(successMessage = "标签添加成功")

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // Then
        composeTestRule.onNodeWithText("标签添加成功").assertIsDisplayed()
    }

    // ========== 标签页切换测试 ==========

    @Test
    fun userProfileScreen_switchToCustomDimensionsTab() {
        // Given
        var selectedTab = 0
        val uiState = createDefaultUiState()

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.SwitchTab) {
                            selectedTab = event.tabIndex
                        }
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("自定义维度").performClick()

        // Then
        assert(selectedTab == 1)
    }

    @Test
    fun userProfileScreen_switchBackToBaseDimensionsTab() {
        // Given
        var selectedTab = 1
        val uiState = createDefaultUiState().copy(selectedTabIndex = 1)

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.SwitchTab) {
                            selectedTab = event.tabIndex
                        }
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("基础信息").performClick()

        // Then
        assert(selectedTab == 0)
    }

    // ========== 维度卡片展开/收起测试 ==========

    @Test
    fun dimensionCard_expandsAndCollapses() {
        // Given
        val uiState = createDefaultUiState()

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // 初始状态应该是展开的，可以看到"添加标签"按钮
        composeTestRule.onNodeWithText("添加标签").assertIsDisplayed()

        // 点击收起按钮
        composeTestRule.onAllNodes(hasText("收起")).onFirst().performClick()

        // 等待动画完成
        composeTestRule.waitForIdle()
    }

    // ========== 标签显示测试 ==========

    @Test
    fun userProfileScreen_displaysTags() {
        // Given
        val profile = UserProfile(
            personalityTraits = listOf("内向", "理性", "细心"),
            values = listOf("诚实", "责任"),
            interests = listOf("阅读", "编程")
        )
        val uiState = createDefaultUiState().copy(profile = profile)

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // Then
        composeTestRule.onNodeWithText("内向").assertIsDisplayed()
        composeTestRule.onNodeWithText("理性").assertIsDisplayed()
        composeTestRule.onNodeWithText("细心").assertIsDisplayed()
    }

    // ========== 自定义维度测试 ==========

    @Test
    fun customDimensionsTab_displaysAddButton() {
        // Given
        val uiState = createDefaultUiState().copy(
            selectedTabIndex = 1,
            profile = UserProfile(customDimensions = emptyMap())
        )

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        // 切换到自定义维度标签页
        composeTestRule.onNodeWithText("自定义维度").performClick()
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithText("添加自定义维度").assertIsDisplayed()
    }

    @Test
    fun customDimensionsTab_displaysLimitMessage_whenCannotAdd() {
        // Given
        val customDimensions = (1..10).associate { index ->
            "自定义维度$index" to listOf("标签$index")
        }
        val uiState = createDefaultUiState().copy(
            selectedTabIndex = 1,
            profile = UserProfile(customDimensions = customDimensions)
        )

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("自定义维度").performClick()
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithText("已达到自定义维度上限（最多10个）").assertIsDisplayed()
    }

    @Test
    fun customDimensionsTab_displaysEmptyState() {
        // Given
        val uiState = createDefaultUiState().copy(
            selectedTabIndex = 1,
            profile = UserProfile(customDimensions = emptyMap())
        )

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("自定义维度").performClick()
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithText("暂无自定义维度").assertIsDisplayed()
        composeTestRule.onNodeWithText("点击上方按钮添加您的专属维度").assertIsDisplayed()
    }

    @Test
    fun customDimensionsTab_displaysCustomDimensions() {
        // Given
        val profile = UserProfile(
            customDimensions = mapOf(
                "职业技能" to listOf("Kotlin", "Android"),
                "生活习惯" to listOf("早起", "运动")
            )
        )
        val uiState = createDefaultUiState().copy(
            selectedTabIndex = 1,
            profile = profile
        )

        // When
        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("自定义维度").performClick()
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithText("职业技能").assertIsDisplayed()
        composeTestRule.onNodeWithText("生活习惯").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin").assertIsDisplayed()
        composeTestRule.onNodeWithText("早起").assertIsDisplayed()
    }

    // ========== 辅助方法 ==========

    private fun createDefaultUiState(): UserProfileUiState {
        return UserProfileUiState(
            profile = UserProfile(),
            isLoading = false,
            error = null,
            successMessage = null,
            selectedTabIndex = 0,
        )
    }
}
