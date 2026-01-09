package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格Tab切换器
 *
 * 设计规格:
 * - 圆角: 响应式
 * - Tab文字: 15sp
 * - 选中态: iOS蓝色, SemiBold
 * - 未选中: 灰色
 * - 下划线: 2dp高度, iOS蓝色, 动画300ms
 * - 下划线宽度: Tab宽度的一半
 *
 * @param tabs Tab标签列表
 * @param selectedIndex 当前选中的索引
 * @param onTabSelected Tab选中回调
 * @param modifier Modifier
 *
 * @see TDD-00021 3.6节 IOSTabSwitcher组件规格
 */
@Composable
fun IOSTabSwitcher(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    val density = LocalDensity.current
    var tabWidth by remember { mutableIntStateOf(0) }

    // 下划线位置动画
    val indicatorOffset by animateDpAsState(
        targetValue = with(density) { (tabWidth * selectedIndex).toDp() },
        animationSpec = tween(durationMillis = 300),
        label = "indicatorOffset"
    )

    // 下划线宽度（Tab宽度的一半）
    val indicatorWidth = with(density) { (tabWidth / 2).toDp() }
    val indicatorPadding = with(density) { (tabWidth / 4).toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensions.cornerRadiusMedium))
            .background(iOSCardBackground)
    ) {
        // Tab行
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(dimensions.iosListItemHeight)
                        .clickable { onTabSelected(index) }
                        .onGloballyPositioned { coordinates ->
                            tabWidth = coordinates.size.width
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = dimensions.fontSizeSubtitle,  // BUG-00055: 使用响应式字体
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (index == selectedIndex) iOSBlue else iOSTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 下划线指示器
        if (tabWidth > 0) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset + indicatorPadding)
                    .width(indicatorWidth)
                    .height(2.dp)
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(1.dp))
                    .background(iOSBlue)
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "Tab切换器 - 两个Tab", showBackground = true)
@Composable
private fun IOSTabSwitcherTwoTabsPreview() {
    EmpathyTheme {
        var selectedIndex by remember { mutableIntStateOf(0) }
        IOSTabSwitcher(
            tabs = listOf("基础信息", "自定义维度"),
            selectedIndex = selectedIndex,
            onTabSelected = { selectedIndex = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Tab切换器 - 三个Tab", showBackground = true)
@Composable
private fun IOSTabSwitcherThreeTabsPreview() {
    EmpathyTheme {
        var selectedIndex by remember { mutableIntStateOf(1) }
        IOSTabSwitcher(
            tabs = listOf("概览", "详情", "设置"),
            selectedIndex = selectedIndex,
            onTabSelected = { selectedIndex = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Tab切换器 - 四个Tab", showBackground = true)
@Composable
private fun IOSTabSwitcherFourTabsPreview() {
    EmpathyTheme {
        var selectedIndex by remember { mutableIntStateOf(2) }
        IOSTabSwitcher(
            tabs = listOf("概览", "事实流", "画像", "资料库"),
            selectedIndex = selectedIndex,
            onTabSelected = { selectedIndex = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}
