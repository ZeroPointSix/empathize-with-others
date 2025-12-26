package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.WeChatGreen
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.theme.iOSTextTertiary

/**
 * iOS风格添加事实底部弹窗
 *
 * 设计原则：
 * 1. 容器形态：iOS模态底部弹窗（Bottom Sheet）
 *    - 纯白背景（#FFFFFF）
 *    - 顶部20dp超大平滑圆角
 *    - 背后40%透明度深黑遮罩
 *
 * 2. 顶部导航栏：标准"左-中-右"结构
 *    - 左侧：取消按钮（系统蓝）
 *    - 中间：标题"添加事实"（17sp, SemiBold）
 *    - 右侧：保存按钮（内容为空时置灰，有内容时高亮绿色）
 *
 * 3. 表单设计：去框化与分组
 *    - 事实类型：横向滑动的彩色胶囊（Chips）
 *    - 事实内容：无边框多行文本域
 *    - 极细分割线（0.5dp）
 *
 * 4. AI辅助：右下角AI润色图标（✨）
 *
 * @param onDismiss 关闭弹窗回调
 * @param onConfirm 确认添加回调，参数为 (key, value)
 * @param onAiEnhance AI润色回调（可选）
 * @param modifier 修饰符
 *
 * @see TDD-00020 iOS风格UI优化
 */
@Composable
fun IOSAddFactBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (key: String, value: String) -> Unit,
    onAiEnhance: ((String) -> String)? = null,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf<FactType?>(null) }
    var factContent by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    
    // 判断保存按钮是否可用
    val isSaveEnabled = selectedType != null && factContent.isNotBlank()
    
    // 遮罩透明度动画
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.4f else 0f,
        animationSpec = tween(300),
        label = "scrimAlpha"
    )

    Dialog(
        onDismissRequest = {
            isVisible = false
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isVisible = false
                    onDismiss()
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250)
                ) + fadeOut(animationSpec = tween(150))
            ) {
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* 阻止点击穿透 */ },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        // 顶部拖动指示条
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFE5E5EA))
                            )
                        }
                        
                        // iOS风格导航栏
                        IOSSheetNavigationBar(
                            title = "添加事实",
                            onCancel = {
                                isVisible = false
                                onDismiss()
                            },
                            onSave = {
                                selectedType?.let { type ->
                                    onConfirm(type.label, factContent.trim())
                                }
                            },
                            isSaveEnabled = isSaveEnabled
                        )
                        
                        // 事实类型选择区域
                        FactTypeChipsSection(
                            selectedType = selectedType,
                            onTypeSelected = { selectedType = it }
                        )
                        
                        // 分割线
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(0.5.dp)
                                .background(Color(0xFFE5E5EA))
                        )
                        
                        // 事实内容输入区域
                        FactContentInputSection(
                            content = factContent,
                            onContentChange = { factContent = it },
                            onAiEnhance = onAiEnhance?.let { enhance ->
                                { factContent = enhance(factContent) }
                            },
                            focusRequester = focusRequester
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    
    // 自动聚焦到输入框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}


/**
 * 事实类型枚举
 * 
 * 每个类型包含：
 * - emoji: 表情图标
 * - label: 中文标签
 * - color: 胶囊背景色
 */
enum class FactType(
    val emoji: String,
    val label: String,
    val color: Color,
    val selectedColor: Color
) {
    PERSONALITY("😊", "性格特点", Color(0xFFFFF3E0), Color(0xFFFFB74D)),
    HOBBY("🎯", "兴趣爱好", Color(0xFFE3F2FD), Color(0xFF64B5F6)),
    WORK("💼", "工作信息", Color(0xFFF3E5F5), Color(0xFFBA68C8)),
    FAMILY("🏠", "家庭情况", Color(0xFFE8F5E9), Color(0xFF81C784)),
    DATE("📅", "重要日期", Color(0xFFFCE4EC), Color(0xFFF06292)),
    TABOO("⚠️", "禁忌话题", Color(0xFFFFEBEE), Color(0xFFE57373)),
    STRATEGY("💡", "沟通策略", Color(0xFFFFF8E1), Color(0xFFFFD54F)),
    OTHER("📝", "其他", Color(0xFFF5F5F5), Color(0xFF9E9E9E))
}

/**
 * iOS风格底部弹窗导航栏
 */
@Composable
private fun IOSSheetNavigationBar(
    title: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    isSaveEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 取消按钮
        TextButton(onClick = onCancel) {
            Text(
                text = "取消",
                fontSize = 17.sp,
                color = iOSBlue
            )
        }
        
        // 标题
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSTextPrimary
        )
        
        // 保存按钮
        TextButton(
            onClick = onSave,
            enabled = isSaveEnabled
        ) {
            Text(
                text = "保存",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSaveEnabled) WeChatGreen else iOSTextTertiary
            )
        }
    }
}

/**
 * 事实类型胶囊选择区域
 * 
 * 横向滑动的彩色胶囊，点击选中后高亮显示
 */
@Composable
private fun FactTypeChipsSection(
    selectedType: FactType?,
    onTypeSelected: (FactType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // 小标题
        Text(
            text = "选择类型",
            fontSize = 13.sp,
            color = iOSTextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 横向滑动的胶囊
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FactType.entries.forEach { type ->
                FactTypeChip(
                    type = type,
                    isSelected = selectedType == type,
                    onClick = { onTypeSelected(type) }
                )
            }
        }
    }
}

/**
 * 单个事实类型胶囊
 */
@Composable
private fun FactTypeChip(
    type: FactType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) type.selectedColor else type.color
    val textColor = if (isSelected) Color.White else iOSTextPrimary
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = type.emoji,
                fontSize = 14.sp
            )
            Text(
                text = type.label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

/**
 * 事实内容输入区域
 * 
 * 无边框的纯白书写区，带AI润色按钮
 */
@Composable
private fun FactContentInputSection(
    content: String,
    onContentChange: (String) -> Unit,
    onAiEnhance: (() -> Unit)?,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 小标题
        Text(
            text = "事实内容",
            fontSize = 13.sp,
            color = iOSTextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            // 无边框输入框
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = iOSTextPrimary,
                    lineHeight = 24.sp
                ),
                cursorBrush = SolidColor(iOSBlue),
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                text = "请输入事实内容，例如：他周六下午通常会去打网球...",
                                fontSize = 16.sp,
                                color = iOSTextTertiary,
                                lineHeight = 24.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            // AI润色按钮（右下角）
            if (onAiEnhance != null && content.isNotBlank()) {
                IconButton(
                    onClick = onAiEnhance,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI润色",
                        tint = WeChatGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // 字数统计
        Text(
            text = "${content.length}/500",
            fontSize = 12.sp,
            color = iOSTextTertiary,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

// ==================== Previews ====================

@Preview(name = "iOS添加事实底部弹窗", showBackground = true)
@Composable
private fun IOSAddFactBottomSheetPreview() {
    EmpathyTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray)
        ) {
            IOSAddFactBottomSheet(
                onDismiss = {},
                onConfirm = { _, _ -> }
            )
        }
    }
}

@Preview(name = "事实类型胶囊", showBackground = true)
@Composable
private fun FactTypeChipsSectionPreview() {
    EmpathyTheme {
        FactTypeChipsSection(
            selectedType = FactType.HOBBY,
            onTypeSelected = {}
        )
    }
}

@Preview(name = "导航栏-可保存", showBackground = true)
@Composable
private fun IOSSheetNavigationBarEnabledPreview() {
    EmpathyTheme {
        IOSSheetNavigationBar(
            title = "添加事实",
            onCancel = {},
            onSave = {},
            isSaveEnabled = true
        )
    }
}

@Preview(name = "导航栏-不可保存", showBackground = true)
@Composable
private fun IOSSheetNavigationBarDisabledPreview() {
    EmpathyTheme {
        IOSSheetNavigationBar(
            title = "添加事实",
            onCancel = {},
            onSave = {},
            isSaveEnabled = false
        )
    }
}
