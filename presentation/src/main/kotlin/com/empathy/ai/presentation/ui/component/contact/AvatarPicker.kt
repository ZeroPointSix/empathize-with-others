package com.empathy.ai.presentation.ui.component.contact

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSTextTertiary
import androidx.compose.foundation.Canvas


/**
 * 头像选择器组件
 * 
 * 技术要点:
 * - 圆形头像区域（100dp）
 * - 无头像时显示相机图标+虚线边框
 * - 有头像时显示图片+编辑图标覆盖层
 * 
 * @param avatarUri 头像URI（null表示无头像）
 * @param onPickAvatar 选择头像回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 7.2 AvatarPicker头像选择器
 */
@Composable
fun AvatarPicker(
    avatarUri: Uri?,
    onPickAvatar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .clickable(onClick = onPickAvatar),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri != null) {
            // 有头像：显示图片+编辑图标
            AsyncImage(
                model = avatarUri,
                contentDescription = "头像",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            // 编辑图标覆盖层
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑头像",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // 无头像：显示虚线边框+相机图标
            DashedCircle(
                modifier = Modifier.size(100.dp)
            )
            
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "添加头像",
                tint = iOSBlue,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * 虚线圆形边框
 */
@Composable
private fun DashedCircle(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        
        drawCircle(
            color = iOSTextTertiary,
            radius = radius,
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(10f, 10f),
                    0f
                )
            )
        )
    }
}

@Preview(name = "头像选择器-无头像", showBackground = true)
@Composable
private fun AvatarPickerEmptyPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AvatarPicker(
                avatarUri = null,
                onPickAvatar = {}
            )
        }
    }
}
