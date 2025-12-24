package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.util.ContentValidator
import com.empathy.ai.domain.util.DateUtils

/**
 * 事实编辑对话框
 *
 * 支持编辑事实的类型（key）和内容（value）
 * 包含预设类型下拉选择和自定义输入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFactDialog(
    fact: Fact,
    onSave: (newKey: String, newValue: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf(fact.key) }
    var value by remember { mutableStateOf(fact.value) }
    var expanded by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val contentValidator = remember { ContentValidator() }
    val keyValidation = contentValidator.validateFactKey(key)
    val valueValidation = contentValidator.validateFactValue(value)
    val isValid = keyValidation.isValid() && valueValidation.isValid()
    val hasChanges = key != fact.key || value != fact.value

    // 预设类型列表
    val presetTypes = listOf(
        stringResource(R.string.fact_type_personality),
        stringResource(R.string.fact_type_hobby),
        stringResource(R.string.fact_type_work),
        stringResource(R.string.fact_type_family),
        stringResource(R.string.fact_type_important_date),
        stringResource(R.string.fact_type_taboo),
        stringResource(R.string.fact_type_strategy),
        stringResource(R.string.fact_type_other)
    )

    if (showDeleteConfirm) {
        DeleteFactConfirmDialog(
            factKey = fact.key,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.edit_fact_title))
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 来源和时间信息
                Text(
                    text = stringResource(
                        R.string.fact_source_info,
                        fact.source.displayName,
                        DateUtils.formatRelativeTime(fact.timestamp)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 事实类型下拉选择
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text(stringResource(R.string.fact_type_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        isError = !keyValidation.isValid(),
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = keyValidation.getErrorMessage() ?: "",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "${key.length}/${ContentValidator.MAX_FACT_KEY_LENGTH}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        presetTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    key = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 事实内容输入
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.fact_value_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    isError = !valueValidation.isValid(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = valueValidation.getErrorMessage() ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${value.length}/${ContentValidator.MAX_FACT_VALUE_LENGTH}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // 查看原始内容（已编辑时显示）
                if (fact.isUserModified && fact.originalKey != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOriginal = !showOriginal },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showOriginal) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.view_original_content),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    AnimatedVisibility(visible = showOriginal) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.original_type, fact.originalKey ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.original_content, fact.originalValue ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(key.trim(), value.trim()) },
                enabled = isValid && hasChanges
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
