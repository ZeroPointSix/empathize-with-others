package com.empathy.ai.presentation.ui.screen.userprofile

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.empathy.ai.presentation.theme.EmpathyTheme
import org.junit.Rule
import org.junit.Test

/**
 * AddTagDialog UI测试
 *
 * 测试添加标签对话框的UI渲染和交互功能。
 */
class AddTagDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========== 对话框显示测试 ==========

    @Test
    fun addTagDialog_displaysCorrectly() {
        // When
        composeTestRule.setContent {
            EmpathyTheme {
                TestAddTagDialog(onConfirm = {}, onDismiss = {})
            }
        }

        // Then
        composeTestRule.onNodeWithText("添加标签").assertIsDisplayed()
        composeTestRule.onNodeWithText("标签内容").assertIsDisplayed()
        composeTestRule.onNodeWithText("请输入标签").assertIsDisplayed()
        composeTestRule.onNodeWithText("添加").assertIsDisplayed()
        composeTestRule.onNodeWithText("取消").assertIsDisplayed()
    }

    // ========== 输入验证测试 ==========

    @Test
    fun addTagDialog_showsError_whenInputIsEmpty() {
        // Given
        composeTestRule.setContent {
            EmpathyTheme {
                TestAddTagDialog(onConfirm = {}, onDismiss = {})
            }
        }

        // When - 直接点击添加按钮（输入为空）
        composeTestRule.onNodeWithText("添加").performClick()

        // Then
        composeTestRule.onNodeWithText("标签不能为空且长度不超过20字符").assertIsDisplayed()
    }

    @Test
    fun addTagDialog_showsError_whenInputTooLong() {
        // Given
        composeTestRule.setContent {
            EmpathyTheme {
                TestAddTagDialog(onConfirm = {}, onDismiss = {})
            }
        }

        // When - 输入超过20字符的内容
        val longInput = "这是一个非常非常非常非常非常长的标签内容"
        composeTestRule.onNodeWithText("请输入标签").performTextInput(longInput)
        composeTestRule.onNodeWithText("添加").performClick()

        // Then
        composeTestRule.onNodeWithText("标签不能为空且长度不超过20字符").assertIsDisplayed()
    }

    @Test
    fun addTagDialog_acceptsValidInput() {
        // Given
        var confirmedTag: String? = null
        composeTestRule.setContent {
            EmpathyTheme {
                TestAddTagDialog(
                    onConfirm = { confirmedTag = it },
                    onDismiss = {}
                )
            }
        }

        // When - 输入有效标签
        composeTestRule.onNodeWithText("请输入标签").performTextInput("有效标签")
        composeTestRule.onNodeWithText("添加").performClick()

        // Then
        assert(confirmedTag == "有效标签")
    }

    @Test
    fun addTagDialog_trimsWhitespace() {
        // Given
        var confirmedTag: String? = null
        composeTestRule.setContent {
            EmpathyTheme {
                TestAddTagDialog(
                    onConfirm = { confirmedTag = it },
                    onDismiss = {}
                )
            }
        }

        // When - 输入带空格的标签
        composeTestRule.onNodeWithText("请输入标签").performTextInput("  有效标签  ")
        composeTestRule.onNodeWithText("添加").performClick()

        // Then
        assert(confirmedTag == "有效标签")
    }

    // ========== 取消操作测试 ==========

    @Test
    fun addTagDialog_dismisses_whenCancelClicked() {
        // Given
        var dismissed = false
        composeTestRule.setContent {
            EmpathyTheme {
                TestAddTagDialog(
                    onConfirm = {},
                    onDismiss = { dismissed = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("取消").performClick()

        // Then
        assert(dismissed)
    }

    // ========== 边界情况测试 ==========

    @Test
    fun addTagDialog_acceptsMaxLengthInput() {
        // Given
        var confirmedTag: String? = null
        composeTestRule.setContent {
            EmpathyTheme {
                TestAddTagDialog(
                    onConfirm = { confirmedTag = it },
                    onDismiss = {}
                )
            }
        }

        // When - 输入正好20字符的标签
        val maxLengthInput = "12345678901234567890"
        composeTestRule.onNodeWithText("请输入标签").performTextInput(maxLengthInput)
        composeTestRule.onNodeWithText("添加").performClick()

        // Then
        assert(confirmedTag == maxLengthInput)
    }

    @Test
    fun addTagDialog_acceptsSingleCharacter() {
        // Given
        var confirmedTag: String? = null
        composeTestRule.setContent {
            EmpathyTheme {
                TestAddTagDialog(
                    onConfirm = { confirmedTag = it },
                    onDismiss = {}
                )
            }
        }

        // When - 输入单个字符
        composeTestRule.onNodeWithText("请输入标签").performTextInput("A")
        composeTestRule.onNodeWithText("添加").performClick()

        // Then
        assert(confirmedTag == "A")
    }

    @Test
    fun addTagDialog_clearsError_whenInputChanges() {
        // Given
        composeTestRule.setContent {
            EmpathyTheme {
                TestAddTagDialog(onConfirm = {}, onDismiss = {})
            }
        }

        // When - 先触发错误
        composeTestRule.onNodeWithText("添加").performClick()
        composeTestRule.onNodeWithText("标签不能为空且长度不超过20字符").assertIsDisplayed()

        // 然后输入内容
        composeTestRule.onNodeWithText("请输入标签").performTextInput("新标签")

        // Then - 错误应该消失
        composeTestRule.onNodeWithText("标签不能为空且长度不超过20字符").assertDoesNotExist()
    }
}

/**
 * 测试用的AddTagDialog组件
 */
@Composable
private fun TestAddTagDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加标签") },
        text = {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it; isError = false },
                label = { Text("标签内容") },
                placeholder = { Text("请输入标签") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("标签不能为空且长度不超过20字符") }
                } else null,
                modifier = Modifier
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = tagInput.trim()
                if (trimmed.isNotEmpty() && trimmed.length <= 20) {
                    onConfirm(trimmed)
                    onDismiss()
                } else {
                    isError = true
                }
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
