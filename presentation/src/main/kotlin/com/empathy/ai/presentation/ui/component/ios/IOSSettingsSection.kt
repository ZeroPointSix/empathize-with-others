package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格设置分组组件
 *
 * 标题: 13sp, 大写, 灰色
 * 卡片: 12dp圆角, 白色背景
 *
 * @param title 分组标题（可选）
 * @param footer 分组底部说明（可选）
 * @param modifier Modifier
 * @param content 分组内容
 */
@Composable
fun IOSSettingsSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 分组标题
        if (title != null) {
            Text(
                text = title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = iOSTextSecondary,
                modifier = Modifier.padding(
                    start = 16.dp,
                    bottom = 8.dp,
                    top = 24.dp
                )
            )
        }

        // 卡片容器
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(iOSCardBackground)
        ) {
            content()
        }

        // 底部说明
        if (footer != null) {
            Text(
                text = footer,
                fontSize = 13.sp,
                color = iOSTextSecondary,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 8.dp
                )
            )
        }
    }
}
