package com.empathy.ai.presentation.ui.screen.prompt

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 提示词场景切换Tab组件（V2版本）
 *
 * 圆角胶囊按钮样式，支持4个场景切换，每个Tab带图标
 * 选中态: 蓝色背景+白色文字+白色图标
 * 未选中态: 灰色背景+灰色文字+灰色图标
 *
 * 设计参考: 文档/开发文档/UI-原型/empathy-prompt-editor-v2.html
 *
 * @param selectedScene 当前选中的场景
 * @param onSceneSelected 场景选择回调
 * @param modifier Modifier
 */
@Composable
fun PromptSceneTab(
    selectedScene: PromptScene,
    onSceneSelected: (PromptScene) -> Unit,
    modifier: Modifier = Modifier
) {
    val scenes = PromptScene.SETTINGS_SCENE_ORDER

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        scenes.forEach { scene ->
            SceneTabItem(
                scene = scene,
                isSelected = scene == selectedScene,
                onClick = { onSceneSelected(scene) }
            )
        }
    }
}

/**
 * 获取场景对应的图标
 */
private fun getSceneIcon(scene: PromptScene): ImageVector {
    return when (scene) {
        PromptScene.ANALYZE -> Icons.Default.Search
        PromptScene.POLISH -> Icons.Default.EditNote
        PromptScene.REPLY -> Icons.AutoMirrored.Filled.Chat
        PromptScene.SUMMARY -> Icons.Default.Summarize
        else -> Icons.Default.Search // 默认图标
    }
}

/**
 * 单个场景Tab项（带图标）
 */
@Composable
private fun SceneTabItem(
    scene: PromptScene,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) iOSBlue else Color(0xFFE5E5EA),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "backgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else iOSTextSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "contentColor"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = getSceneIcon(scene),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = scene.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

// ========== 预览 ==========

@Preview(showBackground = true, name = "场景Tab - 分析选中")
@Composable
private fun PromptSceneTabAnalyzePreview() {
    EmpathyTheme {
        PromptSceneTab(
            selectedScene = PromptScene.ANALYZE,
            onSceneSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "场景Tab - 润色选中")
@Composable
private fun PromptSceneTabPolishPreview() {
    EmpathyTheme {
        PromptSceneTab(
            selectedScene = PromptScene.POLISH,
            onSceneSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "场景Tab - 回复选中")
@Composable
private fun PromptSceneTabReplyPreview() {
    EmpathyTheme {
        PromptSceneTab(
            selectedScene = PromptScene.REPLY,
            onSceneSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "场景Tab - 总结选中")
@Composable
private fun PromptSceneTabSummaryPreview() {
    EmpathyTheme {
        PromptSceneTab(
            selectedScene = PromptScene.SUMMARY,
            onSceneSelected = {}
        )
    }
}
