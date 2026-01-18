package com.empathy.ai.presentation.ui.component.contact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSSeparator

/**
 * 联系人表单数据
 * 
 * 简化版表单，只包含4个核心字段：
 * - 姓名（必填）
 * - 联系方式（可选）
 * - 关系类型（必选，影响好感度初始化）
 * - 目标（可选）
 * 
 * @see RESEARCH-00054 新建联系人页面BUG深度调研报告
 */
data class ContactFormData(
    val name: String = "",
    val contact: String = "",
    val relationshipType: RelationshipType = RelationshipType.STRANGER,
    val targetGoal: String = ""
) {
    /**
     * 表单是否有效（姓名不为空）
     */
    val isValid: Boolean
        get() = name.isNotBlank()
    
    /**
     * 完成按钮是否可用
     */
    val isDoneEnabled: Boolean
        get() = name.isNotBlank()
    
    /**
     * 根据关系类型获取初始好感度
     * 
     * 关系类型与好感度映射：
     * - 陌生人 → 0
     * - 同事/同学 → 30
     * - 朋友 → 50
     * - 家人 → 70
     * - 伴侣/恋人/配偶 → 80
     */
    val initialRelationshipScore: Int
        get() = when (relationshipType) {
            RelationshipType.STRANGER -> 0
            RelationshipType.COLLEAGUE, RelationshipType.CLASSMATE -> 30
            RelationshipType.FRIEND, RelationshipType.OTHER -> 50
            RelationshipType.FAMILY -> 70
            RelationshipType.PARTNER, RelationshipType.LOVER, RelationshipType.SPOUSE -> 80
        }
}

/**
 * 联系人表单卡片组件
 * 
 * 技术要点:
 * - iOS风格表单卡片
 * - 简化为4个核心字段：姓名、联系方式、关系类型、目标
 * - 每个表单项：标签+输入框/选择器
 * - 分隔线使用iOSSeparator
 * 
 * @param formData 表单数据
 * @param onFormDataChange 表单数据变化回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 7.3 ContactFormCard表单卡片
 * @see RESEARCH-00054 新建联系人页面BUG深度调研报告
 */
@Composable
fun ContactFormCard(
    formData: ContactFormData,
    onFormDataChange: (ContactFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensions.cornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = dimensions.spacingMedium)
        ) {
            // 姓名（必填）
            FormInputItem(
                label = "姓名",
                value = formData.name,
                onValueChange = { onFormDataChange(formData.copy(name = it)) },
                placeholder = "请输入姓名"
            )
            
            HorizontalDivider(color = iOSSeparator)
            
            // 联系方式（可选）
            FormInputItem(
                label = "联系方式",
                value = formData.contact,
                onValueChange = { onFormDataChange(formData.copy(contact = it)) },
                placeholder = "手机号/微信号（可选）"
            )
            
            HorizontalDivider(color = iOSSeparator)
            
            // 关系类型
            RelationshipPicker(
                label = "关系",
                selectedType = formData.relationshipType,
                onTypeSelected = { onFormDataChange(formData.copy(relationshipType = it)) }
            )
            
            HorizontalDivider(color = iOSSeparator)
            
            // 目标（可选）
            FormInputItem(
                label = "目标",
                value = formData.targetGoal,
                onValueChange = { onFormDataChange(formData.copy(targetGoal = it)) },
                placeholder = "攻略目标（可选）"
            )
        }
    }
}

@Preview(name = "联系人表单卡片-空", showBackground = true)
@Composable
private fun ContactFormCardEmptyPreview() {
    EmpathyTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            ContactFormCard(
                formData = ContactFormData(),
                onFormDataChange = {}
            )
        }
    }
}

@Preview(name = "联系人表单卡片-有数据", showBackground = true)
@Composable
private fun ContactFormCardFilledPreview() {
    EmpathyTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            ContactFormCard(
                formData = ContactFormData(
                    name = "张三",
                    contact = "13000000000",
                    relationshipType = RelationshipType.FRIEND,
                    targetGoal = "建立长期合作关系"
                ),
                onFormDataChange = {}
            )
        }
    }
}
