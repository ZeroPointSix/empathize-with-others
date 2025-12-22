package com.empathy.ai.presentation.ui.screen.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 提示词设置区域组件
 *
 * 显示4个核心场景的提示词设置选项：
 * - 聊天分析（ANALYZE）
 * - 润色优化（POLISH）
 * - 生成回复（REPLY）
 * - 每日总结（SUMMARY）
 *
 * @param scenes 要显示的场景列表（应使用 PromptScene.SETTINGS_SCENE_ORDER）
 * @param onSceneClick 场景点击回调
 * @param modifier Modifier
 *
 * @see TDD-00015 提示词设置优化技术设计
 */
@Composable
fun PromptSettingsSection(
    scenes: List<PromptScene>,
    onSceneClick: (PromptScene) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "提示词设置",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        scenes.forEach { scene ->
            PromptSettingItem(
                scene = scene,
                onClick = { onSceneClick(scene) }
            )
        }

        Text(
            text = "提示：自定义指令可以让AI更好地理解您的需求，提供更精准的分析和建议",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * 单个提示词设置项
 *
 * @param scene 场景类型
 * @param onClick 点击回调
 * @param modifier Modifier
 */
@Composable
private fun PromptSettingItem(
    scene: PromptScene,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(text = "${scene.displayName}指令")
        },
        supportingContent = {
            Text(
                text = scene.description,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

// ==================== Previews ====================

@Preview(name = "提示词设置区域", showBackground = true)
@Composable
private fun PromptSettingsSectionPreview() {
    EmpathyTheme {
        PromptSettingsSection(
            scenes = PromptScene.SETTINGS_SCENE_ORDER,
            onSceneClick = {}
        )
    }
}

@Preview(name = "单个设置项", showBackground = true)
@Composable
private fun PromptSettingItemPreview() {
    EmpathyTheme {
        PromptSettingItem(
            scene = PromptScene.ANALYZE,
            onClick = {}
        )
    }
}
