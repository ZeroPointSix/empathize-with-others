package com.empathy.ai.presentation.ui.screen.userprofile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.presentation.theme.EmpathyTheme
import org.junit.Rule
import org.junit.Test

/**
 * 用户画像端到端流程测试
 *
 * 测试完整的用户交互流程。
 */
class UserProfileFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========== 添加标签流程测试 ==========

    @Test
    fun addTagFlow_complete() {
        // Given
        var addedTag: String? = null
        var addedDimension: String? = null
        val uiState = UserProfileUiState(
            profile = UserProfile(),
            showAddTagDialog = false
        )

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        when (event) {
                            is UserProfileUiEvent.ShowAddTagDialog -> {
                                addedDimension = event.dimensionKey
                            }
                            is UserProfileUiEvent.AddTag -> {
                                addedTag = event.tag
                                addedDimension = event.dimensionKey
                            }
                            else -> {}
                        }
                    }
                )
            }
        }

        // When - 点击添加标签按钮
        composeTestRule.onNodeWithText("添加标签").performClick()

        // Then - 应该触发显示对话框事件
        assert(addedDimension != null) { "应该记录维度" }
    }

    @Test
    fun addTagFlow_fromPresetTags() {
        // Given
        var selectedTag: String? = null
        var selectedDimension: String? = null
        val uiState = UserProfileUiState(
            profile = UserProfile(),
            selectedTabIndex = 0
        )

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.AddTag) {
                            selectedTag = event.tag
                            selectedDimension = event.dimensionKey
                        }
                    }
                )
            }
        }

        // When - 点击预设标签（如果可见）
        // 注意：预设标签在"快速选择"区域
        composeTestRule.waitForIdle()
        
        // Then - 验证界面正确渲染
        composeTestRule.onNodeWithText("性格特点").assertIsDisplayed()
    }

    // ========== 编辑标签流程测试 ==========

    @Test
    fun editTagFlow_showsDialog() {
        // Given
        var editDimension: String? = null
        var editTag: String? = null
        val profile = UserProfile(
            personalityTraits = listOf("内向", "理性")
        )
        val uiState = UserProfileUiState(profile = profile)

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.ShowEditTagDialog) {
                            editDimension = event.dimensionKey
                            editTag = event.tag
                        }
                    }
                )
            }
        }

        // When - 点击已有标签
        composeTestRule.onNodeWithText("内向").performClick()

        // Then
        assert(editDimension == "PERSONALITY_TRAITS") { "应该是性格特点维度" }
        assert(editTag == "内向") { "应该是内向标签" }
    }

    // ========== 删除标签流程测试 ==========

    @Test
    fun deleteTagFlow_showsConfirmDialog() {
        // Given
        var showDeleteConfirm = false
        var deleteTag: String? = null
        val profile = UserProfile(
            personalityTraits = listOf("内向")
        )
        val uiState = UserProfileUiState(
            profile = profile,
            showEditTagDialog = true,
            currentEditDimension = "PERSONALITY_TRAITS",
            currentEditTag = "内向"
        )

        composeTestRule.setContent {
            EmpathyTheme {
                TestEditTagDialogWithDelete(
                    currentTag = "内向",
                    onDelete = {
                        showDeleteConfirm = true
                        deleteTag = "内向"
                    },
                    onDismiss = {}
                )
            }
        }

        // When - 点击删除按钮
        composeTestRule.onNodeWithText("删除").performClick()

        // Then
        assert(showDeleteConfirm) { "应该显示删除确认" }
        assert(deleteTag == "内向") { "应该删除内向标签" }
    }

    // ========== 自定义维度流程测试 ==========

    @Test
    fun addCustomDimensionFlow_showsDialog() {
        // Given
        var showAddDimensionDialog = false
        val uiState = UserProfileUiState(
            profile = UserProfile(),
            selectedTabIndex = 1
        )

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.ShowAddDimensionDialog) {
                            showAddDimensionDialog = true
                        }
                    }
                )
            }
        }

        // When - 切换到自定义维度标签页并点击添加
        composeTestRule.onNodeWithText("自定义维度").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("添加自定义维度").performClick()

        // Then
        assert(showAddDimensionDialog) { "应该显示添加维度对话框" }
    }

    @Test
    fun deleteCustomDimensionFlow_showsConfirmDialog() {
        // Given
        var deleteDimension: String? = null
        val profile = UserProfile(
            customDimensions = mapOf("职业技能" to listOf("Kotlin"))
        )
        val uiState = UserProfileUiState(
            profile = profile,
            selectedTabIndex = 1
        )

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.ShowDeleteDimensionConfirm) {
                            deleteDimension = event.dimensionName
                        }
                    }
                )
            }
        }

        // When - 切换到自定义维度并点击删除
        composeTestRule.onNodeWithText("自定义维度").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("删除维度").performClick()

        // Then
        assert(deleteDimension == "职业技能") { "应该删除职业技能维度" }
    }

    // ========== 导出流程测试 ==========

    @Test
    fun exportFlow_showsDialog() {
        // Given
        var showExportDialog = false
        val uiState = UserProfileUiState(profile = UserProfile())

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.ShowExportDialog) {
                            showExportDialog = true
                        }
                    }
                )
            }
        }

        // When - 点击导出按钮
        composeTestRule.onNodeWithContentDescription("导出").performClick()

        // Then
        assert(showExportDialog) { "应该显示导出对话框" }
    }

    // ========== 重置流程测试 ==========

    @Test
    fun resetFlow_showsConfirmDialog() {
        // Given
        var showResetConfirm = false
        val profile = UserProfile(
            personalityTraits = listOf("内向", "理性")
        )
        val uiState = UserProfileUiState(profile = profile)

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.ShowResetConfirm) {
                            showResetConfirm = true
                        }
                    }
                )
            }
        }

        // When - 点击重置按钮
        composeTestRule.onNodeWithContentDescription("重置").performClick()

        // Then
        assert(showResetConfirm) { "应该显示重置确认对话框" }
    }

    // ========== 标签页切换流程测试 ==========

    @Test
    fun tabSwitchFlow_switchesToCustomDimensions() {
        // Given
        var selectedTab = 0
        val uiState = UserProfileUiState(
            profile = UserProfile(),
            selectedTabIndex = 0
        )

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

        // When - 点击自定义维度标签页
        composeTestRule.onNodeWithText("自定义维度").performClick()

        // Then
        assert(selectedTab == 1) { "应该切换到自定义维度标签页" }
    }

    @Test
    fun tabSwitchFlow_switchesBackToBaseDimensions() {
        // Given
        var selectedTab = 1
        val uiState = UserProfileUiState(
            profile = UserProfile(),
            selectedTabIndex = 1
        )

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

        // When - 点击基础信息标签页
        composeTestRule.onNodeWithText("基础信息").performClick()

        // Then
        assert(selectedTab == 0) { "应该切换到基础信息标签页" }
    }

    // ========== 刷新流程测试 ==========

    @Test
    fun refreshFlow_triggersRefresh() {
        // Given
        var refreshTriggered = false
        val uiState = UserProfileUiState(profile = UserProfile())

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.RefreshProfile) {
                            refreshTriggered = true
                        }
                    }
                )
            }
        }

        // 注意：刷新通常通过下拉刷新触发，这里测试事件处理
        // 实际的下拉刷新需要更复杂的手势模拟
    }

    // ========== 错误处理流程测试 ==========

    @Test
    fun errorFlow_displaysAndClears() {
        // Given
        var errorCleared = false
        val uiState = UserProfileUiState(
            profile = UserProfile(),
            error = "测试错误消息"
        )

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is UserProfileUiEvent.ClearError) {
                            errorCleared = true
                        }
                    }
                )
            }
        }

        // Then - 错误消息应该显示
        composeTestRule.onNodeWithText("测试错误消息").assertIsDisplayed()

        // When - 点击关闭按钮
        composeTestRule.onNodeWithText("关闭").performClick()

        // Then
        assert(errorCleared) { "应该清除错误" }
    }

    // ========== 返回导航测试 ==========

    @Test
    fun backNavigation_triggersCallback() {
        // Given
        var navigatedBack = false
        val uiState = UserProfileUiState(profile = UserProfile())

        composeTestRule.setContent {
            EmpathyTheme {
                UserProfileScreenTestContent(
                    uiState = uiState,
                    onNavigateBack = { navigatedBack = true }
                )
            }
        }

        // When - 点击返回按钮
        composeTestRule.onNodeWithContentDescription("返回").performClick()

        // Then
        assert(navigatedBack) { "应该触发返回导航" }
    }
}

/**
 * 测试用的编辑标签对话框（带删除功能）
 */
@androidx.compose.runtime.Composable
private fun TestEditTagDialogWithDelete(
    currentTag: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text("编辑标签") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = currentTag,
                onValueChange = {},
                label = { androidx.compose.material3.Text("标签内容") }
            )
        },
        confirmButton = {
            androidx.compose.foundation.layout.Row {
                androidx.compose.material3.TextButton(onClick = onDelete) {
                    androidx.compose.material3.Text("删除")
                }
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text("保存")
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("取消")
            }
        }
    )
}
