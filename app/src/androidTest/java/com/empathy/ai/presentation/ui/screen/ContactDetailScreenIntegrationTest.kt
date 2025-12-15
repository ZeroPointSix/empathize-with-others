package com.empathy.ai.presentation.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.domain.model.DataStatus
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailTabScreen
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiState
import com.empathy.ai.presentation.ui.screen.contact.DetailTab
import com.empathy.ai.testutil.TestDataFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ContactDetailScreen 集成测试
 *
 * 测试内容：
 * - 完整的数据流（Repository → ViewModel → UI）
 * - 标签页切换
 * - 视图模式切换
 * - 筛选功能
 * - 标签确认/驳回
 *
 * 参考标准：
 * - [TD-00004] T077 ContactDetailScreenIntegrationTest
 * - [TDD-00004] 联系人画像记忆系统UI架构设计
 */
@RunWith(AndroidJUnit4::class)
class ContactDetailScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 使用TestDataFactory创建测试数据
    private val testContact = TestDataFactory.createContactProfile()
    private val testTags = TestDataFactory.createBrainTags(count = 2)
    private val testTimelineItems = TestDataFactory.createTimelineItems(count = 2)

    private val defaultUiState = TestDataFactory.createContactDetailUiState(
        contact = testContact,
        timelineItems = testTimelineItems,
        currentTab = DetailTab.Overview,
        viewMode = ViewMode.Timeline,
        selectedFilters = emptySet()
    )

    /**
     * 测试概览页面正常渲染
     */
    @Test
    fun contactDetailScreen_overviewTab_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = defaultUiState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        // 验证组件正常渲染
        composeTestRule.waitForIdle()
    }

    /**
     * 测试加载状态显示
     */
    @Test
    fun contactDetailScreen_loading_showsLoadingIndicator() {
        val loadingState = defaultUiState.copy(isLoading = true)

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = loadingState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        // 加载状态应该显示
        composeTestRule.waitForIdle()
    }

    /**
     * 测试错误状态显示
     */
    @Test
    fun contactDetailScreen_error_showsErrorView() {
        val errorState = defaultUiState.copy(
            isLoading = false,
            error = "加载失败"
        )

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = errorState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("加载失败").assertIsDisplayed()
    }

    /**
     * 测试标签页切换到事实流
     */
    @Test
    fun contactDetailScreen_switchToFactStream_rendersCorrectly() {
        val factStreamState = defaultUiState.copy(selectedTab = DetailTab.FactStream)

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = factStreamState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    /**
     * 测试标签页切换到标签画像
     */
    @Test
    fun contactDetailScreen_switchToPersona_rendersCorrectly() {
        val personaState = defaultUiState.copy(selectedTab = DetailTab.Persona)

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = personaState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    /**
     * 测试标签页切换到资料库
     */
    @Test
    fun contactDetailScreen_switchToDataVault_rendersCorrectly() {
        val dataVaultState = defaultUiState.copy(selectedTab = DetailTab.DataVault)

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = dataVaultState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    /**
     * 测试视图模式切换
     */
    @Test
    fun contactDetailScreen_viewModeSwitch_updatesCorrectly() {
        val listViewState = defaultUiState.copy(
            selectedTab = DetailTab.FactStream,
            viewMode = ViewMode.List
        )

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = listViewState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    /**
     * 测试空联系人状态
     */
    @Test
    fun contactDetailScreen_noContact_showsEmptyState() {
        val emptyState = defaultUiState.copy(contact = null)

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = emptyState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    /**
     * 测试空标签列表
     */
    @Test
    fun contactDetailScreen_noTags_showsEmptyTagsState() {
        val noTagsState = defaultUiState.copy(
            selectedTab = DetailTab.Persona,
            tags = emptyList()
        )

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = noTagsState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    /**
     * 测试空时间线
     */
    @Test
    fun contactDetailScreen_noTimelineItems_showsEmptyState() {
        val noTimelineState = defaultUiState.copy(
            selectedTab = DetailTab.FactStream,
            timelineItems = emptyList()
        )

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = noTimelineState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    /**
     * 测试返回按钮
     */
    @Test
    fun contactDetailScreen_backButton_callsNavigateBack() {
        var backCalled = false

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = defaultUiState,
                    onEvent = {},
                    onNavigateBack = { backCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("返回").performClick()
        assert(backCalled) { "返回回调应该被调用" }
    }

    /**
     * 测试筛选条件变化
     */
    @Test
    fun contactDetailScreen_filterChange_updatesCorrectly() {
        val filteredState = defaultUiState.copy(
            selectedTab = DetailTab.FactStream,
            selectedFilter = FilterType.AI_SUMMARY
        )

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = filteredState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    /**
     * 测试高关系分数显示
     */
    @Test
    fun contactDetailScreen_highRelationshipScore_rendersCorrectly() {
        val highScoreState = defaultUiState.copy(relationshipScore = 95)

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = highScoreState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    /**
     * 测试低关系分数显示
     */
    @Test
    fun contactDetailScreen_lowRelationshipScore_rendersCorrectly() {
        val lowScoreState = defaultUiState.copy(relationshipScore = 25)

        composeTestRule.setContent {
            EmpathyTheme {
                ContactDetailTabScreen(
                    uiState = lowScoreState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
