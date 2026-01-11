package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSRed

/**
 * iOS 风格警告对话框
 * 
 * 实现 iOS 原生 UIAlertController 的视觉效果：
 * - 圆角卡片（14dp）
 * - 毛玻璃效果背景
 * - 标题居中（17sp SemiBold）
 * - 消息居中（13sp Regular）
 * - 按钮水平排列（两按钮时）或垂直排列（多按钮时）
 * - 分隔线（0.5dp）
 * 
 * BUG-00036 修复：统一使用 iOS 风格对话框，替换 Material3 AlertDialog
 * 
 * @param title 标题
 * @param message 消息内容
 * @param confirmText 确认按钮文字
 * @param dismissText 取消按钮文字
 * @param onConfirm 确认回调
 * @param onDismiss 取消回调
 * @param isDestructive 是否为破坏性操作（确认按钮显示红色）
 * @param showDismissButton 是否显示取消按钮
 */
@Composable
fun IOSAlertDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
    showDismissButton: Boolean = true
) {
    val dimensions = AdaptiveDimensions.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .padding(dimensions.spacingMedium),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 内容区域
                Column(
                    modifier = Modifier.padding(
                        top = dimensions.spacingLarge,
                        start = dimensions.spacingMedium,
                        end = dimensions.spacingMedium,
                        bottom = dimensions.spacingMedium
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题 - 使用响应式字体
                    Text(
                        text = title,
                        fontSize = dimensions.fontSizeTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                    
                    // 消息 - 使用响应式字体
                    Text(
                        text = message,
                        fontSize = dimensions.fontSizeCaption,
                        color = Color.Black.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
                
                // 分隔线
                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
                
                // 按钮区域
                if (showDismissButton) {
                    // 双按钮：水平排列
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
                                text = dismissText,
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
                        
                        // 确认按钮
                        TextButton(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RectangleShape
                        ) {
                            Text(
                                text = confirmText,
                                fontSize = dimensions.fontSizeTitle,
                                color = if (isDestructive) iOSRed else iOSBlue,
                                fontWeight = if (isDestructive) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                } else {
                    // 单按钮：居中显示
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = dimensions.fontSizeTitle,
                            color = iOSBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS 风格权限请求对话框
 * 
 * 用于向用户解释悬浮窗权限的用途并引导用户授权
 * 
 * BUG-00036 修复：替换原有的 Material3 AlertDialog 实现
 * 
 * @param onDismiss 取消回调
 * @param onConfirm 确认回调（跳转到设置页面）
 */
@Composable
fun IOSPermissionRequestDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    IOSAlertDialog(
        title = "需要悬浮窗权限",
        message = "悬浮窗权限用于在聊天应用上显示快捷按钮，方便您快速访问 AI 助手功能。\n\n我们承诺：\n• 不会读取您的聊天内容\n• 不会收集您的个人信息\n• 所有数据仅在本地处理",
        confirmText = "去设置",
        dismissText = "取消",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = false,
        showDismissButton = true
    )
}

/**
 * iOS 风格删除确认对话框
 * 
 * 用于确认删除操作，确认按钮显示红色
 * 
 * @param title 标题
 * @param message 消息内容
 * @param onConfirm 确认删除回调
 * @param onDismiss 取消回调
 */
@Composable
fun IOSDeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    IOSAlertDialog(
        title = title,
        message = message,
        confirmText = "删除",
        dismissText = "取消",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true,
        showDismissButton = true
    )
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "iOS对话框 - 普通", showBackground = true)
@Composable
private fun IOSAlertDialogPreview() {
    EmpathyTheme {
        IOSAlertDialog(
            title = "提示",
            message = "这是一条提示消息，用于向用户展示重要信息。",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "iOS对话框 - 删除确认", showBackground = true)
@Composable
private fun IOSDeleteConfirmDialogPreview() {
    EmpathyTheme {
        IOSDeleteConfirmDialog(
            title = "删除联系人",
            message = "确定要删除这个联系人吗？此操作无法撤销。",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "iOS对话框 - 权限请求", showBackground = true)
@Composable
private fun IOSPermissionRequestDialogPreview() {
    EmpathyTheme {
        IOSPermissionRequestDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "iOS对话框 - 单按钮", showBackground = true)
@Composable
private fun IOSAlertDialogSingleButtonPreview() {
    EmpathyTheme {
        IOSAlertDialog(
            title = "操作成功",
            message = "您的设置已保存。",
            confirmText = "好的",
            onConfirm = {},
            onDismiss = {},
            showDismissButton = false
        )
    }
}

/**
 * iOS 风格输入对话框
 * 
 * 用于需要用户输入内容的场景，如添加标签、编辑名称等
 * 
 * @param title 标题
 * @param content 自定义内容区域
 * @param confirmText 确认按钮文字
 * @param dismissText 取消按钮文字
 * @param onConfirm 确认回调
 * @param onDismiss 取消回调
 * @param confirmEnabled 确认按钮是否可用
 * @param showDismissButton 是否显示取消按钮
 */
@Composable
fun IOSInputDialog(
    title: String,
    content: @Composable () -> Unit,
    confirmText: String = "确定",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    showDismissButton: Boolean = true
) {
    val dimensions = AdaptiveDimensions.current
    
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
                .widthIn(max = 300.dp)
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
                // 标题
                Text(
                    text = title,
                    fontSize = dimensions.fontSizeTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        top = dimensions.spacingLarge,
                        start = dimensions.spacingMedium,
                        end = dimensions.spacingMedium
                    )
                )
                
                // 自定义内容区域
                Box(
                    modifier = Modifier.padding(
                        horizontal = dimensions.spacingMedium,
                        vertical = dimensions.spacingMedium
                    )
                ) {
                    content()
                }
                
                // 分隔线
                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
                
                // 按钮区域
                if (showDismissButton) {
                    // 双按钮：水平排列
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
                                text = dismissText,
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
                        
                        // 确认按钮
                        TextButton(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RectangleShape,
                            enabled = confirmEnabled
                        ) {
                            Text(
                                text = confirmText,
                                fontSize = dimensions.fontSizeTitle,
                                color = if (confirmEnabled) iOSBlue else iOSBlue.copy(alpha = 0.4f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // 单按钮：居中显示
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RectangleShape,
                        enabled = confirmEnabled
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = dimensions.fontSizeTitle,
                            color = if (confirmEnabled) iOSBlue else iOSBlue.copy(alpha = 0.4f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "iOS输入对话框", showBackground = true)
@Composable
private fun IOSInputDialogPreview() {
    EmpathyTheme {
        IOSInputDialog(
            title = "添加标签",
            content = {
                Text("这里是自定义内容区域")
            },
            onConfirm = {},
            onDismiss = {}
        )
    }
}
