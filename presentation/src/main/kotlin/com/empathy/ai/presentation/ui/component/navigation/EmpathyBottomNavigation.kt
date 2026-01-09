package com.empathy.ai.presentation.ui.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.navigation.NavRoutes
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.AddButtonRed
import com.empathy.ai.presentation.theme.WeChatGreen
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 共情AI底部导航栏组件
 *
 * 导航栏高度: 50dp（不含安全区）
 * 总高度: 84dp（含34dp安全区）
 * 4Tab布局: 联系人、占位、AI军师、设置
 * 中间红色加号按钮上浮（48dp, #FA5151）
 *
 * @param currentRoute 当前路由
 * @param onNavigate 导航回调
 * @param onAddClick 添加按钮点击回调
 * @param modifier Modifier
 */
@Composable
fun EmpathyBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color.White)
    ) {
        // 顶部分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color(0xFFE5E5E5))
        )

        // 导航栏内容 - BUG-00036: 增加高度到56dp，确保字体不被截断
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)  // 从50dp增加到56dp，给字体更多空间
                .align(Alignment.TopStart)  // 修复BUG-00031: 确保Row在Box顶部对齐
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 联系人Tab
            BottomNavItem(
                icon = Icons.Outlined.Person,
                selectedIcon = Icons.Filled.Person,
                label = "联系人",
                selected = currentRoute == NavRoutes.CONTACT_LIST,
                onClick = { onNavigate(NavRoutes.CONTACT_LIST) },
                modifier = Modifier.weight(1f)
            )

            // 中间占位（为加号按钮留空间）
            Spacer(modifier = Modifier.weight(1f))

            // AI军师Tab
            BottomNavItem(
                icon = Icons.Outlined.Person, // 临时使用，后续替换为自定义图标
                selectedIcon = Icons.Filled.Person,
                label = "AI军师",
                selected = currentRoute == NavRoutes.AI_ADVISOR,
                onClick = { onNavigate(NavRoutes.AI_ADVISOR) },
                modifier = Modifier.weight(1f)
            )

            // 设置Tab
            BottomNavItem(
                icon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings,
                label = "设置",
                selected = currentRoute == NavRoutes.SETTINGS,
                onClick = { onNavigate(NavRoutes.SETTINGS) },
                modifier = Modifier.weight(1f)
            )
        }

        // 中间加号按钮（上浮）
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
                .size(48.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    clip = false
                )
                .clip(CircleShape)
                .background(AddButtonRed)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAddClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "添加",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // iOS Home Indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .width(134.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(Color.Black)
        )
    }
}

/**
 * 底部导航项
 * 
 * BUG-00036 修复：
 * 1. 增加容器高度，确保字体不被截断
 * 2. 使用 fillMaxHeight 确保内容垂直居中
 * 3. 减小图标尺寸，增加字体空间
 * 
 * BUG-00052 修复：
 * 4. 使用 AdaptiveDimensions 响应式字体替代硬编码 10.sp
 */
@Composable
private fun BottomNavItem(
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) WeChatGreen else iOSTextSecondary
    val dimensions = AdaptiveDimensions.current

    Column(
        modifier = modifier
            .fillMaxHeight()  // 填满父容器高度
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),  // 适当的垂直padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)  // 减小图标尺寸
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = dimensions.fontSizeXSmall,  // BUG-00052: 使用响应式字体替代硬编码 10.sp
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = color,
            maxLines = 1,  // 确保单行显示
            softWrap = false  // 禁止换行
        )
    }
}
