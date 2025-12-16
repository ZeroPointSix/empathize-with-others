package com.empathy.ai.presentation.ui.screen.prompt.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.R
import com.empathy.ai.presentation.theme.BrandWarmGold
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 提示词编辑器顶部导航栏
 *
 * @param title 标题文本
 * @param isSaving 是否正在保存
 * @param canSave 是否可以保存
 * @param onCancelClick 取消按钮点击回调
 * @param onSaveClick 保存按钮点击回调
 * @param modifier Modifier
 */
@Composable
fun PromptEditorTopBar(
    title: String,
    isSaving: Boolean,
    canSave: Boolean,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 取消按钮
        TextButton(onClick = onCancelClick) {
            Text(
                text = stringResource(R.string.prompt_editor_cancel),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        // 标题
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 完成按钮或加载圈
        if (isSaving) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = BrandWarmGold
                )
            }
        } else {
            TextButton(
                onClick = onSaveClick,
                enabled = canSave
            ) {
                Text(
                    text = stringResource(R.string.prompt_editor_save),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canSave) BrandWarmGold else BrandWarmGold.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PromptEditorTopBarPreview() {
    EmpathyTheme {
        PromptEditorTopBar(
            title = "编辑指令",
            isSaving = false,
            canSave = true,
            onCancelClick = {},
            onSaveClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PromptEditorTopBarSavingPreview() {
    EmpathyTheme {
        PromptEditorTopBar(
            title = "编辑指令",
            isSaving = true,
            canSave = false,
            onCancelClick = {},
            onSaveClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PromptEditorTopBarDisabledPreview() {
    EmpathyTheme {
        PromptEditorTopBar(
            title = "编辑林婉儿的专属指令",
            isSaving = false,
            canSave = false,
            onCancelClick = {},
            onSaveClick = {}
        )
    }
}
