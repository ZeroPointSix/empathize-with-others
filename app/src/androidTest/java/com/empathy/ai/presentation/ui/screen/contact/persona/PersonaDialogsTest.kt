package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.empathy.ai.presentation.theme.EmpathyAiTheme
import org.junit.Rule
import org.junit.Test

/**
 * PersonaDialogs UI测试
 *
 * 测试标签画像V2相关对话框的UI交互
 */
class PersonaDialogsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== MoveCategoryDialog 测试 ====================

    @Test
    fun MoveCategoryDialog_显示标题和选项() {
        composeTestRule.setContent {
            EmpathyAiTheme {
                MoveCategoryDialog(
                    selectedCount = 3,
                    existingCategories = listOf("性格特点", "兴趣爱好"),
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // 验证标题
        composeTestRule.onNodeWithText("移动 3 个标签到").assertIsDisplayed()
        // 验证创建新分类选项
        composeTestRule.onNodeWithText("创建新分类").assertIsDisplayed()
        // 验证现有分类
        composeTestRule.onNodeWithText("性格特点").assertIsDisplayed()
        composeTestRule.onNodeWithText("兴趣爱好").assertIsDisplayed()
    }

    @Test
    fun MoveCategoryDialog_选择现有分类() {
        var confirmedCategory: String? = null

        composeTestRule.setContent {
            EmpathyAiTheme {
                MoveCategoryDialog(
                    selectedCount = 1,
                    existingCategories = listOf("性格特点", "兴趣爱好"),
                    onConfirm = { confirmedCategory = it },
                    onDismiss = {}
                )
            }
        }

        // 选择现有分类
        composeTestRule.onNodeWithText("性格特点").performClick()
        // 点击确认
        composeTestRule.onNodeWithText("确认移动").performClick()

        // 验证回调
        assert(confirmedCategory == "性格特点")
    }

    @Test
    fun MoveCategoryDialog_创建新分类() {
        var confirmedCategory: String? = null

        composeTestRule.setContent {
            EmpathyAiTheme {
                MoveCategoryDialog(
                    selectedCount = 1,
                    existingCategories = listOf("性格特点"),
                    onConfirm = { confirmedCategory = it },
                    onDismiss = {}
                )
            }
        }

        // 选择创建新分类
        composeTestRule.onNodeWithText("创建新分类").performClick()
        // 输入新分类名称
        composeTestRule.onNodeWithText("分类名称").performTextInput("新分类")
        // 点击确认
        composeTestRule.onNodeWithText("确认移动").performClick()

        // 验证回调
        assert(confirmedCategory == "新分类")
    }

    @Test
    fun MoveCategoryDialog_新分类名称为空时确认按钮禁用() {
        composeTestRule.setContent {
            EmpathyAiTheme {
                MoveCategoryDialog(
                    selectedCount = 1,
                    existingCategories = emptyList(),
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // 选择创建新分类但不输入名称
        composeTestRule.onNodeWithText("创建新分类").performClick()

        // 验证确认按钮禁用
        composeTestRule.onNodeWithText("确认移动").assertIsNotEnabled()
    }

    @Test
    fun MoveCategoryDialog_新分类名称过长显示错误() {
        composeTestRule.setContent {
            EmpathyAiTheme {
                MoveCategoryDialog(
                    selectedCount = 1,
                    existingCategories = emptyList(),
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // 选择创建新分类
        composeTestRule.onNodeWithText("创建新分类").performClick()
        // 输入超长名称（超过20字符）
        composeTestRule.onNodeWithText("分类名称").performTextInput("这是一个非常非常非常非常长的分类名称")

        // 验证错误提示
        composeTestRule.onNodeWithText("分类名称不能超过20个字符").assertIsDisplayed()
    }

    @Test
    fun MoveCategoryDialog_取消关闭对话框() {
        var dismissed = false

        composeTestRule.setContent {
            EmpathyAiTheme {
                MoveCategoryDialog(
                    selectedCount = 1,
                    existingCategories = emptyList(),
                    onConfirm = {},
                    onDismiss = { dismissed = true }
                )
            }
        }

        // 点击取消
        composeTestRule.onNodeWithText("取消").performClick()

        // 验证回调
        assert(dismissed)
    }

    // ==================== BatchDeleteConfirmDialog 测试 ====================

    @Test
    fun BatchDeleteConfirmDialog_显示确认信息() {
        composeTestRule.setContent {
            EmpathyAiTheme {
                BatchDeleteConfirmDialog(
                    selectedCount = 5,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // 验证标题
        composeTestRule.onNodeWithText("确认删除").assertIsDisplayed()
        // 验证消息
        composeTestRule.onNodeWithText("确定要删除选中的 5 个标签吗？此操作不可撤销。").assertIsDisplayed()
        // 验证按钮
        composeTestRule.onNodeWithText("取消").assertIsDisplayed()
        composeTestRule.onNodeWithText("删除").assertIsDisplayed()
    }

    @Test
    fun BatchDeleteConfirmDialog_确认删除() {
        var confirmed = false

        composeTestRule.setContent {
            EmpathyAiTheme {
                BatchDeleteConfirmDialog(
                    selectedCount = 3,
                    onConfirm = { confirmed = true },
                    onDismiss = {}
                )
            }
        }

        // 点击删除
        composeTestRule.onNodeWithText("删除").performClick()

        // 验证回调
        assert(confirmed)
    }

    @Test
    fun BatchDeleteConfirmDialog_取消删除() {
        var dismissed = false

        composeTestRule.setContent {
            EmpathyAiTheme {
                BatchDeleteConfirmDialog(
                    selectedCount = 3,
                    onConfirm = {},
                    onDismiss = { dismissed = true }
                )
            }
        }

        // 点击取消
        composeTestRule.onNodeWithText("取消").performClick()

        // 验证回调
        assert(dismissed)
    }

    @Test
    fun BatchDeleteConfirmDialog_单个标签显示正确消息() {
        composeTestRule.setContent {
            EmpathyAiTheme {
                BatchDeleteConfirmDialog(
                    selectedCount = 1,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // 验证消息
        composeTestRule.onNodeWithText("确定要删除选中的 1 个标签吗？此操作不可撤销。").assertIsDisplayed()
    }
}
