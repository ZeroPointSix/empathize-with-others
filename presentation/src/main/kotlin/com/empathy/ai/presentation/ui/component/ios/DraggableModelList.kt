package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import kotlin.math.roundToInt

/**
 * 可拖拽模型列表数据
 */
data class DraggableModelItem(
    val id: String,
    val displayName: String,
    val isDefault: Boolean = false
)

/**
 * iOS风格可拖拽模型列表组件
 *
 * 支持长按拖拽排序、设置默认模型、删除模型
 *
 * 设计规格（TD-00025）:
 * - 长按触发拖拽
 * - 拖拽项放大1.05倍，显示阴影
 * - 其他项自动腾出空间，平滑动画
 * - 三个蓝色横杠拖拽手柄
 * - 拖拽范围限制在列表内
 *
 * BUG-00039修复：将LazyColumn改为Column，避免嵌套滚动容器导致的崩溃
 * 原因：当DraggableModelList被嵌套在AddProviderScreen的LazyColumn中时，
 *       内层LazyColumn会收到无限高度约束，导致IllegalStateException
 *
 * @param models 模型列表
 * @param onReorder 重新排序回调
 * @param onSetDefault 设置默认模型回调
 * @param onDelete 删除模型回调
 * @param modifier Modifier
 *
 * @see TD-00025 Phase 4: 模型拖拽排序实现
 */
@Composable
fun DraggableModelList(
    models: List<DraggableModelItem>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onSetDefault: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    // 拖拽状态
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    
    val itemHeight = dimensions.iosListItemHeight + dimensions.spacingSmall
    
    // BUG-00039修复：使用Column替代LazyColumn，避免嵌套滚动容器
    // 模型列表通常只有3-10个项目，不需要懒加载优化
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        models.forEachIndexed { index, model ->
            // 使用key确保重组时状态正确
            key(model.id) {
                val isDragging = draggingIndex == index
                
                // 动画效果
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.05f else 1f,
                    animationSpec = spring(),
                    label = "scale"
                )
                
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 0.dp,
                    animationSpec = spring(),
                    label = "elevation"
                )
                
                // 计算偏移量（为拖拽项腾出空间）
                val offsetY by animateDpAsState(
                    targetValue = when {
                        draggingIndex < 0 -> 0.dp
                        isDragging -> 0.dp
                        targetIndex >= 0 && index >= minOf(draggingIndex, targetIndex) && 
                            index <= maxOf(draggingIndex, targetIndex) -> {
                            if (targetIndex > draggingIndex && index > draggingIndex && index <= targetIndex) {
                                -itemHeight
                            } else if (targetIndex < draggingIndex && index < draggingIndex && index >= targetIndex) {
                                itemHeight
                            } else {
                                0.dp
                            }
                        }
                        else -> 0.dp
                    },
                    animationSpec = spring(),
                    label = "offsetY"
                )
                
                DraggableModelItemRow(
                    model = model,
                    isDragging = isDragging,
                    scale = scale,
                    elevation = elevation,
                    offsetY = if (isDragging) dragOffsetY.roundToInt() else offsetY.value.toInt(),
                    onSetDefault = { onSetDefault(model.id) },
                    onDelete = { onDelete(model.id) },
                    onDragStart = {
                        draggingIndex = index
                        targetIndex = index
                    },
                    onDrag = { change ->
                        dragOffsetY += change
                        // 计算目标位置
                        val newTargetIndex = (index + (dragOffsetY / itemHeight.value).roundToInt())
                            .coerceIn(0, models.size - 1)
                        if (newTargetIndex != targetIndex) {
                            targetIndex = newTargetIndex
                        }
                    },
                    onDragEnd = {
                        if (draggingIndex >= 0 && targetIndex >= 0 && draggingIndex != targetIndex) {
                            onReorder(draggingIndex, targetIndex)
                        }
                        draggingIndex = -1
                        targetIndex = -1
                        dragOffsetY = 0f
                    },
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                )
            }
        }
    }
}

/**
 * 可拖拽模型项行
 */
@Composable
private fun DraggableModelItemRow(
    model: DraggableModelItem,
    isDragging: Boolean,
    scale: Float,
    elevation: androidx.compose.ui.unit.Dp,
    offsetY: Int,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY) }
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(dimensions.cornerRadiusSmall))
            .clip(RoundedCornerShape(dimensions.cornerRadiusSmall))
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
        color = if (isDragging) Color.White else iOSCardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.iosListItemHeight)
                .padding(horizontal = dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 拖拽手柄
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "拖拽排序",
                tint = iOSBlue,
                modifier = Modifier.size(dimensions.iconSizeMedium)
            )
            
            Spacer(modifier = Modifier.width(dimensions.spacingMedium))
            
            // 模型信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = model.displayName.ifBlank { model.id },
                    fontSize = dimensions.fontSizeSubtitle,
                    fontWeight = FontWeight.Medium,
                    color = iOSTextPrimary
                )
                if (model.isDefault) {
                    Text(
                        text = "默认模型",
                        fontSize = dimensions.fontSizeCaption,
                        color = iOSBlue
                    )
                }
            }
            
            // 设置默认按钮
            if (!model.isDefault) {
                IconButton(
                    onClick = onSetDefault,
                    modifier = Modifier.size(dimensions.iconSizeLarge)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "设为默认",
                        tint = iOSTextSecondary,
                        modifier = Modifier.size(dimensions.iconSizeMedium)
                    )
                }
            }
            
            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(dimensions.iconSizeLarge)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除",
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "可拖拽模型列表", showBackground = true)
@Composable
private fun DraggableModelListPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            val models = listOf(
                DraggableModelItem("gpt-4", "GPT-4", isDefault = true),
                DraggableModelItem("gpt-3.5-turbo", "GPT-3.5 Turbo"),
                DraggableModelItem("gpt-4-turbo", "GPT-4 Turbo")
            )
            DraggableModelList(
                models = models,
                onReorder = { _, _ -> },
                onSetDefault = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "可拖拽模型列表 - 单个模型", showBackground = true)
@Composable
private fun DraggableModelListSinglePreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            val models = listOf(
                DraggableModelItem("deepseek-chat", "DeepSeek Chat", isDefault = true)
            )
            DraggableModelList(
                models = models,
                onReorder = { _, _ -> },
                onSetDefault = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
