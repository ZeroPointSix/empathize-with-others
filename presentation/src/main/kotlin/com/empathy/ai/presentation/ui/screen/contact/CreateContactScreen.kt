package com.empathy.ai.presentation.ui.screen.contact

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSSystemGroupedBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.component.contact.AddFactButton
import com.empathy.ai.presentation.ui.component.contact.AvatarPicker
import com.empathy.ai.presentation.ui.component.contact.ContactFormCard
import com.empathy.ai.presentation.ui.component.contact.ContactFormData
import com.empathy.ai.presentation.ui.component.dialog.IOSAddFactBottomSheet
import com.empathy.ai.presentation.ui.component.ios.IOSNavigationBar


/**
 * 新建联系人页面 (iOS风格)
 * 
 * 技术要点:
 * - 顶部：IOSNavigationBar（取消/新建联系人/完成）
 * - 头像选择器（AvatarPicker）
 * - 表单卡片（ContactFormCard）- 简化为4个核心字段
 * - 添加事实按钮和事实列表
 * - 内置添加事实对话框
 * 
 * 修复内容（RESEARCH-00054）：
 * - 实现添加事实对话框功能
 * - 显示已添加的事实列表
 * - 支持删除已添加的事实
 * 
 * @param onCancel 取消回调
 * @param onDone 完成回调，参数为表单数据、头像URI和事实列表
 * @param onPickAvatar 选择头像回调（可选，用于外部图片选择器集成）
 * @param modifier 修饰符
 * 
 * @see TDD-00020 8.5 CreateContactScreen新建联系人页重写
 * @see RESEARCH-00054 新建联系人页面BUG深度调研报告
 */
@Composable
fun CreateContactScreen(
    onCancel: () -> Unit,
    onDone: (ContactFormData, Uri?, List<Fact>) -> Unit,
    onPickAvatar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 表单数据状态
    var formData by remember { mutableStateOf(ContactFormData()) }
    
    // 头像URI状态
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    
    // 事实列表状态
    val facts = remember { mutableStateListOf<Fact>() }
    
    // 添加事实对话框状态
    var showAddFactDialog by remember { mutableStateOf(false) }
    
    // 完成按钮是否可用
    val isDoneEnabled = formData.name.isNotBlank()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSSystemGroupedBackground)
    ) {
        // iOS导航栏
        IOSNavigationBar(
            title = "新建联系人",
            onCancel = onCancel,
            onDone = { onDone(formData, avatarUri, facts.toList()) },
            isDoneEnabled = isDoneEnabled
        )
        
        // 可滚动内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 头像选择器
            AvatarPicker(
                avatarUri = avatarUri,
                onPickAvatar = {
                    onPickAvatar?.invoke()
                    // 如果没有外部处理，暂时不做任何操作
                    // 后续可以集成图片选择器
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 表单卡片
            ContactFormCard(
                formData = formData,
                onFormDataChange = { formData = it }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 已添加的事实列表
            if (facts.isNotEmpty()) {
                FactListCard(
                    facts = facts,
                    onDeleteFact = { fact ->
                        facts.remove(fact)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 添加事实按钮
            AddFactButton(
                onClick = { showAddFactDialog = true },
                text = if (facts.isEmpty()) "添加第一条事实" else "添加更多事实",
                modifier = Modifier.fillMaxWidth()
            )
            
            // 底部间距
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    
    // 添加事实对话框 - 使用iOS风格底部弹窗
    // 该组件采用iOS模态视图设计，包含彩色胶囊类型选择器
    if (showAddFactDialog) {
        IOSAddFactBottomSheet(
            onDismiss = { showAddFactDialog = false },
            onConfirm = { key, value ->
                facts.add(
                    Fact(
                        key = key,
                        value = value,
                        timestamp = System.currentTimeMillis(),
                        source = FactSource.MANUAL
                    )
                )
                showAddFactDialog = false
            }
        )
    }
}

/**
 * 兼容旧版本的CreateContactScreen
 * 
 * 保持向后兼容，将旧的onAddFact回调转换为新的内置对话框模式
 */
@Composable
fun CreateContactScreen(
    onCancel: () -> Unit,
    onDone: (ContactFormData, Uri?) -> Unit,
    onAddFact: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    CreateContactScreen(
        onCancel = onCancel,
        onDone = { formData, avatarUri, _ -> onDone(formData, avatarUri) },
        onPickAvatar = null,
        modifier = modifier
    )
}

/**
 * 事实列表卡片
 */
@Composable
private fun FactListCard(
    facts: List<Fact>,
    onDeleteFact: (Fact) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "已添加的事实",
                fontSize = 13.sp,
                color = iOSTextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            facts.forEachIndexed { index, fact ->
                FactItem(
                    fact = fact,
                    onDelete = { onDeleteFact(fact) }
                )
                
                if (index < facts.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        color = iOSSeparator,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 单个事实项
 */
@Composable
private fun FactItem(
    fact: Fact,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fact.key,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = iOSTextPrimary
            )
            Text(
                text = fact.value,
                fontSize = 14.sp,
                color = iOSTextSecondary
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                tint = iOSTextSecondary
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "新建联系人页面", showBackground = true)
@Composable
private fun CreateContactScreenPreview() {
    EmpathyTheme {
        CreateContactScreen(
            onCancel = {},
            onDone = { _, _, _ -> },
            onPickAvatar = {}
        )
    }
}

@Preview(name = "新建联系人页面-无头像选择", showBackground = true)
@Composable
private fun CreateContactScreenNoAvatarPreview() {
    EmpathyTheme {
        CreateContactScreen(
            onCancel = {},
            onDone = { _, _, _ -> }
        )
    }
}
