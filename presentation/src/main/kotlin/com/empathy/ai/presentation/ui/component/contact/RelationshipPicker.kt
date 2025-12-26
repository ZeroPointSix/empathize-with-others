package com.empathy.ai.presentation.ui.component.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 关系类型枚举
 * 
 * 每种关系类型对应不同的初始好感度：
 * - STRANGER(陌生人) → 0
 * - COLLEAGUE(同事) → 30
 * - CLASSMATE(同学) → 30
 * - FRIEND(朋友) → 50
 * - FAMILY(家人) → 70
 * - PARTNER(伴侣) → 80
 * - LOVER(恋人) → 80
 * - SPOUSE(配偶) → 80
 * - OTHER(其他) → 50
 */
enum class RelationshipType(val displayName: String) {
    STRANGER("陌生人"),
    FRIEND("朋友"),
    FAMILY("家人"),
    COLLEAGUE("同事"),
    PARTNER("伴侣"),
    LOVER("恋人"),
    SPOUSE("配偶"),
    CLASSMATE("同学"),
    OTHER("其他")
}

/**
 * 关系类型选择器组件
 * 
 * 技术要点:
 * - 下拉选择器样式
 * - 支持RelationshipType枚举
 * - 显示当前选中值+箭头图标
 * 
 * @param label 标签文字
 * @param selectedType 当前选中的关系类型
 * @param onTypeSelected 类型选择回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 7.3 关系类型选择
 */
@Composable
fun RelationshipPicker(
    label: String,
    selectedType: RelationshipType?,
    onTypeSelected: (RelationshipType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧标签
        Text(
            text = label,
            fontSize = 15.sp,
            color = iOSTextPrimary,
            modifier = Modifier.width(80.dp)
        )
        
        // 右侧选择器
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedType?.displayName ?: "请选择",
                    fontSize = 15.sp,
                    color = if (selectedType != null) iOSTextPrimary else iOSTextSecondary
                )
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "选择",
                    tint = iOSTextSecondary
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                RelationshipType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            onTypeSelected(type)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Preview(name = "关系类型选择器-已选择", showBackground = true)
@Composable
private fun RelationshipPickerSelectedPreview() {
    EmpathyTheme {
        RelationshipPicker(
            label = "关系",
            selectedType = RelationshipType.LOVER,
            onTypeSelected = {}
        )
    }
}

@Preview(name = "关系类型选择器-未选择", showBackground = true)
@Composable
private fun RelationshipPickerEmptyPreview() {
    EmpathyTheme {
        RelationshipPicker(
            label = "关系",
            selectedType = null,
            onTypeSelected = {}
        )
    }
}
