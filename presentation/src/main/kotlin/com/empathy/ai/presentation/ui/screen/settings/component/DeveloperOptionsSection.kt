package com.empathy.ai.presentation.ui.screen.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.iOSOrange
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsItem
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection

/**
 * 开发者选项区域组件
 *
 * 在设置页面底部显示，仅在开发者模式解锁后可见
 *
 * @param onSystemPromptEditClick 系统提示词编辑点击回调
 * @param modifier Modifier
 *
 * @see PRD-00033 开发者模式与系统提示词编辑
 */
@Composable
fun DeveloperOptionsSection(
    onSystemPromptEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IOSSettingsSection(
        title = "开发者选项",
        modifier = modifier
    ) {
        IOSSettingsItem(
            icon = Icons.Default.Code,
            iconBackgroundColor = iOSOrange,
            title = "系统提示词编辑",
            subtitle = "编辑AI场景的系统提示词（Header/Footer）",
            showDivider = false,
            onClick = onSystemPromptEditClick
        )
    }
}
