package com.empathy.ai.presentation.ui.component.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.AvatarColors
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.util.buildHighlightedText
import com.empathy.ai.presentation.util.createSearchHighlightStyle

/**
 * 联系人列表项组件（微信风格）
 *
 * 用于在列表中展示联系人基本信息
 * 采用淡色头像背景+深色首字母的配色方案
 *
 * @param contact 联系人信息
 * @param onClick 点击事件回调
 * @param modifier 修饰符
 * @param tagCount 标签数量，用于显示标签统计
 * @param showDivider 是否显示底部分隔线
 * @param relativeTime 相对时间文本（可选）
 */
@Composable
fun ContactListItem(
    contact: ContactProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tagCount: Int = 0,
    showDivider: Boolean = true,
    relativeTime: String? = null,
    highlightQuery: String = ""
) {
    val (backgroundColor, textColor) = AvatarColors.getColorPair(contact.name)
    val dividerColor = iOSSeparator
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isSystemInDarkTheme(),
        baseColor = iOSBlue
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                if (showDivider) {
                    val startX = 76.dp.toPx()
                    drawLine(
                        color = dividerColor,
                        start = Offset(startX, size.height - 0.5.dp.toPx()),
                        end = Offset(size.width, size.height - 0.5.dp.toPx()),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像 (淡色背景+深色首字母)
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 联系人信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // 第一行：姓名
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildHighlightedText(
                        text = contact.name,
                        query = highlightQuery,
                        highlightStyle = highlightStyle
                    ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 第二行：目标描述
            if (contact.targetGoal.isNotBlank()) {
                Text(
                    text = buildHighlightedText(
                        text = contact.targetGoal,
                        query = highlightQuery,
                        highlightStyle = highlightStyle
                    ),
                    fontSize = 14.sp,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 右侧：时间 + 箭头
        Column(
            horizontalAlignment = Alignment.End
        ) {
            if (relativeTime != null) {
                Text(
                    text = relativeTime,
                    fontSize = 13.sp,
                    color = iOSTextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "查看详情",
                tint = iOSTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
// ============================================================
// 预览函数
// ============================================================

@Preview(name = "基本联系人", showBackground = true)
@Composable
private fun ContactListItemBasicPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "1",
                name = "张三",
                targetGoal = "建立良好的合作关系"
            ),
            onClick = {},
            tagCount = 3,
            relativeTime = "刚刚"
        )
    }
}

@Preview(name = "无目标", showBackground = true)
@Composable
private fun ContactListItemNoGoalPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "2",
                name = "李四",
                targetGoal = ""
            ),
            onClick = {},
            tagCount = 0,
            relativeTime = "5分钟前"
        )
    }
}

@Preview(name = "长文本", showBackground = true)
@Composable
private fun ContactListItemLongTextPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "3",
                name = "王总经理",
                targetGoal = "通过建立深厚的信任关系，最终达成长期战略合作伙伴关系"
            ),
            onClick = {},
            tagCount = 15,
            relativeTime = "昨天"
        )
    }
}

@Preview(name = "单字名", showBackground = true)
@Composable
private fun ContactListItemSingleCharPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "4",
                name = "王",
                targetGoal = "修复关系"
            ),
            onClick = {},
            tagCount = 2,
            relativeTime = "3天前"
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactListItemDarkPreview() {
    EmpathyTheme {
        ContactListItem(
            contact = ContactProfile(
                id = "5",
                name = "张三",
                targetGoal = "建立良好的合作关系"
            ),
            onClick = {},
            tagCount = 3,
            relativeTime = "刚刚"
        )
    }
}
