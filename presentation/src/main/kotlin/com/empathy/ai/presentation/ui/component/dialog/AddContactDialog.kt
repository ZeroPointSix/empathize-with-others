package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 添加联系人对话框 - iOS风格
 *
 * 功能：
 * - 输入联系人姓名（必填）
 * - 输入联系人电话（可选）
 * - 输入目标关系（可选）
 * - 表单验证
 *
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认添加回调，参数为 (姓名, 电话, 目标)
 */
@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, targetGoal: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var targetGoal by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current

    IOSInputDialog(
        title = "添加联系人",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 姓名输入框（必填）
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null // 清除错误提示
                    },
                    label = { Text("姓名 *") },
                    placeholder = { Text("请输入联系人姓名") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 电话输入框（可选）
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("电话") },
                    placeholder = { Text("请输入电话号码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 目标关系输入框（可选）
                OutlinedTextField(
                    value = targetGoal,
                    onValueChange = { targetGoal = it },
                    label = { Text("目标关系") },
                    placeholder = { Text("例如：建立良好的合作关系") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            // 验证并提交
                            if (name.isBlank()) {
                                nameError = "姓名不能为空"
                            } else {
                                onConfirm(name.trim(), phone.trim(), targetGoal.trim())
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmText = "添加",
        dismissText = "取消",
        onConfirm = {
            // 验证姓名
            if (name.isBlank()) {
                nameError = "姓名不能为空"
                return@IOSInputDialog
            }
            
            // 验证通过，执行回调
            onConfirm(name.trim(), phone.trim(), targetGoal.trim())
        },
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank()
    )
}

// ==================== Previews ====================

@Preview(name = "添加联系人对话框", showBackground = true)
@Composable
private fun AddContactDialogPreview() {
    EmpathyTheme {
        AddContactDialog(
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}
