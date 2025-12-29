package com.empathy.ai.presentation.ui.component.persona

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.TagCategory
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary


/**
 * 标签详情数据类
 */
data class TagDetail(
    val id: String,
    val name: String,
    val category: TagCategory,
    val source: String,
    val createdAt: String
)

/**
 * iOS 风格标签详情对话框
 * 
 * BUG-00036 修复：使用响应式字体尺寸
 * 
 * 技术要点:
 * - 显示标签详情（名称、来源、创建时间）
 * - 编辑/删除按钮
 * - 响应式字体尺寸
 * 
 * @param tag 标签详情
 * @param onDismiss 关闭对话框回调
 * @param onEdit 编辑回调
 * @param onDelete 删除回调
 * 
 * @see FD-00020 画像库页标签详情
 */
@Composable
fun TagDetailDialog(
    tag: TagDetail,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(dimensions.spacingLarge)
            ) {
                // 标题 - 使用响应式字体
                Text(
                    text = "标签详情",
                    fontSize = dimensions.fontSizeTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(dimensions.spacingLarge))
                
                // 标签名称
                DetailRow(label = "名称", value = tag.name)
                HorizontalDivider(color = iOSSeparator)
                
                // 分类
                DetailRow(label = "分类", value = tag.category.displayName)
                HorizontalDivider(color = iOSSeparator)
                
                // 来源
                DetailRow(label = "来源", value = tag.source)
                HorizontalDivider(color = iOSSeparator)
                
                // 创建时间
                DetailRow(label = "创建时间", value = tag.createdAt)
                
                Spacer(modifier = Modifier.height(dimensions.spacingXLarge))
                
                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "删除",
                            fontSize = dimensions.fontSizeTitle,
                            color = iOSRed
                        )
                    }
                    
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "编辑",
                            fontSize = dimensions.fontSizeTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = iOSBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val dimensions = AdaptiveDimensions.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = dimensions.fontSizeBody,
            color = iOSTextSecondary
        )
        Text(
            text = value,
            fontSize = dimensions.fontSizeBody,
            color = iOSTextPrimary
        )
    }
}

@Preview(name = "标签详情对话框", showBackground = true)
@Composable
private fun TagDetailDialogPreview() {
    EmpathyTheme {
        TagDetailDialog(
            tag = TagDetail(
                id = "1",
                name = "喜欢旅行",
                category = TagCategory.INTERESTS,
                source = "手动添加",
                createdAt = "2025-12-20"
            ),
            onDismiss = {},
            onEdit = {},
            onDelete = {}
        )
    }
}
