package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.util.ContentValidator
import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSRed

/**
 * 事实编辑对话框 - iOS风格
 *
 * BUG-00036 修复：迁移到iOS风格对话框
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
    val dimensions = AdaptiveDimensions.current
    
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(dimensions.spacingMedium),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = dimensions.spacingLarge,
                            start = dimensions.spacingMedium,
                            end = dimensions.spacingSmall
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.edit_fact_title),
                        fontSize = dimensions.fontSizeTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = iOSRed
                        )
                    }
                }
                
                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.spacingMedium)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 来源和时间信息
                    Text(
                        text = stringResource(
                            R.string.fact_source_info,
                            fact.source.displayName,
                            DateUtils.formatRelativeTime(fact.timestamp)
                        ),
                        fontSize = dimensions.fontSizeCaption,
                        color = Color.Black.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))

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
                                        color = iOSRed,
                                        fontSize = dimensions.fontSizeCaption
                                    )
                                    Text(
                                        text = "${key.length}/${ContentValidator.MAX_FACT_KEY_LENGTH}",
                                        fontSize = dimensions.fontSizeCaption
                                    )
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = iOSBlue,
                                cursorColor = iOSBlue
                            )
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

                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))

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
                                    color = iOSRed,
                                    fontSize = dimensions.fontSizeCaption
                                )
                                Text(
                                    text = "${value.length}/${ContentValidator.MAX_FACT_VALUE_LENGTH}",
                                    fontSize = dimensions.fontSizeCaption
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = iOSBlue,
                            cursorColor = iOSBlue
                        )
                    )

                    // 查看原始内容（已编辑时显示）
                    if (fact.isUserModified && fact.originalKey != null) {
                        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOriginal = !showOriginal },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showOriginal) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = iOSBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.view_original_content),
                                fontSize = dimensions.fontSizeCaption,
                                color = iOSBlue
                            )
                        }
                        AnimatedVisibility(visible = showOriginal) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = dimensions.spacingSmall)
                            ) {
                                Text(
                                    text = stringResource(R.string.original_type, fact.originalKey ?: ""),
                                    fontSize = dimensions.fontSizeCaption,
                                    color = Color.Black.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = stringResource(R.string.original_content, fact.originalValue ?: ""),
                                    fontSize = dimensions.fontSizeCaption,
                                    color = Color.Black.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                }
                
                // 分隔线
                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
                
                // 按钮区域 - iOS风格水平排列
                Row(modifier = Modifier.fillMaxWidth()) {
                    // 取消按钮
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = dimensions.fontSizeTitle,
                            color = iOSBlue
                        )
                    }
                    
                    // 垂直分隔线
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(44.dp)
                            .background(Color.Black.copy(alpha = 0.1f))
                    )
                    
                    // 保存按钮
                    TextButton(
                        onClick = { onSave(key.trim(), value.trim()) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape,
                        enabled = isValid && hasChanges
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = dimensions.fontSizeTitle,
                            color = if (isValid && hasChanges) iOSBlue else iOSBlue.copy(alpha = 0.4f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
