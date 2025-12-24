package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.R

/**
 * 移动分类对话框
 *
 * 允许用户选择现有分类或创建新分类
 *
 * @param selectedCount 已选中的标签数量
 * @param existingCategories 现有分类列表
 * @param onConfirm 确认回调，参数为目标分类名称
 * @param onDismiss 取消回调
 */
@Composable
fun MoveCategoryDialog(
    selectedCount: Int,
    existingCategories: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 是否选择创建新分类
    var isCreateNew by remember { mutableStateOf(false) }
    // 新分类名称
    var newCategoryName by remember { mutableStateOf("") }
    // 选中的现有分类
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // 验证是否可以确认
    val canConfirm = if (isCreateNew) {
        newCategoryName.isNotBlank() && newCategoryName.length <= 20
    } else {
        selectedCategory != null
    }

    // 获取目标分类名称
    val targetCategory = if (isCreateNew) newCategoryName.trim() else selectedCategory

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.move_to_category_title, selectedCount))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                // 创建新分类选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isCreateNew,
                            onClick = {
                                isCreateNew = true
                                selectedCategory = null
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isCreateNew,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.create_new_category),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // 新分类名称输入框
                if (isCreateNew) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { if (it.length <= 20) newCategoryName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp),
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        supportingText = {
                            Text("${newCategoryName.length}/20")
                        },
                        isError = newCategoryName.length > 20
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 现有分类标题
                if (existingCategories.isNotEmpty()) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.existing_categories),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 现有分类列表
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(existingCategories) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = !isCreateNew && selectedCategory == category,
                                        onClick = {
                                            isCreateNew = false
                                            selectedCategory = category
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !isCreateNew && selectedCategory == category,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { targetCategory?.let { onConfirm(it) } },
                enabled = canConfirm
            ) {
                Text(stringResource(R.string.confirm_move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
