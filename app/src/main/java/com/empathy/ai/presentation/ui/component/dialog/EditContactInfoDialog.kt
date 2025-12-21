package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.empathy.ai.R
import com.empathy.ai.domain.util.ContentValidator

/**
 * 联系人姓名编辑对话框
 *
 * 单行输入，最多50字
 */
@Composable
fun EditContactNameDialog(
    currentName: String,
    onSave: (newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    val contentValidator = remember { ContentValidator() }
    val validation = contentValidator.validateContactName(name)
    val isValid = validation.isValid()
    val hasChanges = name.trim() != currentName

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.edit_contact_name_title))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.contact_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !validation.isValid(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = validation.getErrorMessage() ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${name.length}/${ContentValidator.MAX_CONTACT_NAME_LENGTH}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim()) },
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

/**
 * 联系人目标编辑对话框
 *
 * 多行输入，最多200字
 */
@Composable
fun EditContactGoalDialog(
    currentGoal: String,
    onSave: (newGoal: String) -> Unit,
    onDismiss: () -> Unit
) {
    var goal by remember { mutableStateOf(currentGoal) }

    val contentValidator = remember { ContentValidator() }
    val validation = contentValidator.validateContactGoal(goal)
    val isValid = validation.isValid()
    val hasChanges = goal.trim() != currentGoal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.edit_contact_goal_title))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text(stringResource(R.string.contact_goal_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = !validation.isValid(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = validation.getErrorMessage() ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${goal.length}/${ContentValidator.MAX_CONTACT_GOAL_LENGTH}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(goal.trim()) },
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


/**
 * 联系人信息编辑对话框
 *
 * 同时编辑姓名和目标
 */
@Composable
fun EditContactInfoDialog(
    initialName: String,
    initialTargetGoal: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newTargetGoal: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var targetGoal by remember { mutableStateOf(initialTargetGoal) }

    val contentValidator = remember { ContentValidator() }
    val nameValidation = contentValidator.validateContactName(name)
    val goalValidation = contentValidator.validateContactGoal(targetGoal)
    val isValid = nameValidation.isValid() && goalValidation.isValid()
    val hasChanges = name.trim() != initialName || targetGoal.trim() != initialTargetGoal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.edit_contact_info_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 姓名输入
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.contact_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !nameValidation.isValid(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = nameValidation.getErrorMessage() ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${name.length}/${ContentValidator.MAX_CONTACT_NAME_LENGTH}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // 目标输入
                OutlinedTextField(
                    value = targetGoal,
                    onValueChange = { targetGoal = it },
                    label = { Text(stringResource(R.string.contact_goal_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = !goalValidation.isValid(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = goalValidation.getErrorMessage() ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${targetGoal.length}/${ContentValidator.MAX_CONTACT_GOAL_LENGTH}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), targetGoal.trim()) },
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
