package com.empathy.ai.presentation.ui.component.persona

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.TagCategory
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 添加标签对话框
 * 
 * 技术要点:
 * - iOS风格对话框
 * - 输入框+分类选择器
 * - 取消/确认按钮
 * 
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认添加回调，参数为(标签名, 分类)
 * @param initialCategory 初始分类（可选）
 * 
 * @see FD-00020 画像库页添加标签功能
 */
@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, TagCategory) -> Unit,
    initialCategory: TagCategory? = null
) {
    var tagName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory ?: TagCategory.INTERESTS) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 标题
                Text(
                    text = "添加标签",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 标签名输入框
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("标签名称") },
                    placeholder = { Text("请输入标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = iOSBlue,
                        unfocusedBorderColor = iOSSeparator,
                        cursorColor = iOSBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 分类选择器
                Text(
                    text = "选择分类",
                    fontSize = 13.sp,
                    color = iOSTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFF8F8FA),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { showCategoryDropdown = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategory.displayName,
                            fontSize = 15.sp,
                            color = iOSTextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "选择分类",
                            tint = iOSTextSecondary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        TagCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "取消",
                            fontSize = 17.sp,
                            color = iOSBlue
                        )
                    }
                    
                    TextButton(
                        onClick = {
                            if (tagName.isNotBlank()) {
                                onConfirm(tagName.trim(), selectedCategory)
                            }
                        },
                        enabled = tagName.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "确认",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tagName.isNotBlank()) iOSBlue else iOSTextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "添加标签对话框", showBackground = true)
@Composable
private fun AddTagDialogPreview() {
    EmpathyTheme {
        AddTagDialog(
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}
