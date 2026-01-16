package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.util.ContentValidator
import com.empathy.ai.presentation.theme.iOSBlue

// ============================================================
// 极简风格颜色定义
// ============================================================

/** 标题颜色 - 深灰色 */
private val TitleColor = Color(0xFF333333)

/** 标签颜色 - 中灰色 */
private val LabelColor = Color(0xFF999999)

/** 输入内容颜色 - 深灰色 */
private val InputTextColor = Color(0xFF333333)

/** 字数统计颜色 - 浅灰色 */
private val CounterColor = Color(0xFFBBBBBB)

/** 分隔线颜色 - 极浅灰色 */
private val DividerColor = Color(0xFFE5E5E5)

/** 取消按钮颜色 - 中灰色 */
private val CancelButtonColor = Color(0xFF999999)

/** 纯白背景 */
private val PureWhite = Color(0xFFFFFFFF)

/**
 * 极简风格输入框组件
 * 
 * 设计原则:
 * - 无边框，仅底部单线条
 * - 激活时底部出现品牌蓝色横线
 * - 标签使用中灰色，输入内容使用深灰色
 */
@Composable
private fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxLength: Int,
    singleLine: Boolean = true,
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // 底部线条颜色动画
    val lineColor by animateColorAsState(
        targetValue = if (isFocused) iOSBlue else DividerColor,
        label = "lineColor"
    )
    
    // 标签颜色动画
    val labelColorAnimated by animateColorAsState(
        targetValue = if (isFocused) iOSBlue else LabelColor,
        label = "labelColor"
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 标签
        Text(
            text = label,
            fontSize = 14.sp,
            color = labelColorAnimated,
            fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 输入框
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = InputTextColor,
                lineHeight = 22.sp
            ),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = if (singleLine) 1 else 5,
            cursorBrush = SolidColor(iOSBlue),
            decorationBox = { innerTextField ->
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "请输入$label",
                                fontSize = 16.sp,
                                color = LabelColor
                            )
                        }
                        innerTextField()
                    }
                    
                    // 底部线条
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isFocused) 2.dp else 1.dp)
                            .background(lineColor)
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // 字数统计 - 右对齐
        Text(
            text = "${value.length}/$maxLength",
            fontSize = 12.sp,
            color = CounterColor,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

/**
 * 联系人姓名编辑对话框 (极简风格)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // 标题
                Text(
                    text = stringResource(R.string.edit_contact_name_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleColor
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 输入框
                MinimalTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.contact_name_label),
                    maxLength = ContentValidator.MAX_CONTACT_NAME_LENGTH
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // 取消按钮 - 灰色描边
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CancelButtonColor
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(CancelButtonColor)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 保存按钮 - 品牌蓝色
                    Button(
                        onClick = { onSave(name.trim()) },
                        enabled = isValid && hasChanges,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iOSBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE5E5EA),
                            disabledContentColor = Color(0xFF999999)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 联系人目标编辑对话框 (极简风格)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // 标题
                Text(
                    text = stringResource(R.string.edit_contact_goal_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleColor
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 输入框
                MinimalTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = stringResource(R.string.contact_goal_label),
                    maxLength = ContentValidator.MAX_CONTACT_GOAL_LENGTH,
                    singleLine = false,
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CancelButtonColor
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(CancelButtonColor)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = { onSave(goal.trim()) },
                        enabled = isValid && hasChanges,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iOSBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE5E5EA),
                            disabledContentColor = Color(0xFF999999)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 联系人信息编辑对话框 (极简风格重构)
 *
 * 设计原则:
 * 1. 容器"空气化" - 纯白背景，24dp大圆角
 * 2. 输入框"轻量化" - 无边框，仅底部单线条
 * 3. 按钮"语义校正" - 保存按钮使用品牌蓝色，取消按钮使用灰色
 * 4. 空间"呼吸感" - 增加各元素间距
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            // 大圆角 - 温润的玉石质感
            shape = RoundedCornerShape(24.dp),
            // 纯白背景
            color = PureWhite,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // 标题 - 深灰色
                Text(
                    text = stringResource(R.string.edit_contact_info_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleColor
                )
                
                // 标题与输入框间距 - 增加呼吸感
                Spacer(modifier = Modifier.height(32.dp))
                
                // 姓名输入框 - 极简线条风格
                MinimalTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.contact_name_label),
                    maxLength = ContentValidator.MAX_CONTACT_NAME_LENGTH
                )
                
                // 两个输入框间距 - 增加呼吸感
                Spacer(modifier = Modifier.height(28.dp))
                
                // 目标输入框 - 极简线条风格，减少minLines
                MinimalTextField(
                    value = targetGoal,
                    onValueChange = { targetGoal = it },
                    label = stringResource(R.string.contact_goal_label),
                    maxLength = ContentValidator.MAX_CONTACT_GOAL_LENGTH,
                    singleLine = false,
                    minLines = 1
                )
                
                // 输入框与按钮间距 - 增加呼吸感
                Spacer(modifier = Modifier.height(40.dp))
                
                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // 取消按钮 - 灰色描边，视觉退后
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CancelButtonColor
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(CancelButtonColor)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 保存按钮 - 品牌蓝色，CTA行动召唤
                    Button(
                        onClick = { onConfirm(name.trim(), targetGoal.trim()) },
                        enabled = isValid && hasChanges,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iOSBlue,
                            contentColor = Color.White,
                            // 禁用状态使用浅灰色，而非深灰色
                            disabledContainerColor = Color(0xFFE5E5EA),
                            disabledContentColor = Color(0xFF999999)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
