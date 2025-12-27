package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * iOS风格分段控制器
 * 
 * 实现iOS原生分段控制器的视觉效果和交互体验
 * - 滑块平滑移动动画（250ms, FastOutSlowInEasing）
 * - 白色滑块背景+投影
 * - 背景使用rgba(118, 118, 128, 0.12)
 * 
 * @param tabs 标签页列表
 * @param selectedIndex 当前选中索引
 * @param onTabSelected 标签选中回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 3.1 IOSSegmentedControl分段控制器
 */
@Composable
fun IOSSegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    val tabCount = tabs.size
    if (tabCount == 0) return
    
    val controlHeight = dimensions.iosSegmentedControlHeight
    val innerPadding = (2 * dimensions.fontScale).dp
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(controlHeight)
            .clip(RoundedCornerShape(dimensions.cornerRadiusSmall))
            .background(Color(0x1F767680)) // rgba(118, 118, 128, 0.12)
            .padding(innerPadding)
    ) {
        val tabWidth = (maxWidth - innerPadding * 2) / tabCount
        
        // 滑块位置动画
        val sliderOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing
            ),
            label = "sliderOffset"
        )
        
        // 白色滑块
        Box(
            modifier = Modifier
                .offset(x = sliderOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = dimensions.cardElevation,
                    shape = RoundedCornerShape(dimensions.cornerRadiusSmall - 1.dp),
                    clip = false
                )
                .background(Color.White, RoundedCornerShape(dimensions.cornerRadiusSmall - 1.dp))
        )
        
        // 标签文字
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 13.sp,
                        fontWeight = if (index == selectedIndex) 
                            FontWeight.SemiBold else FontWeight.Normal,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "四个标签", showBackground = true)
@Composable
private fun IOSSegmentedControlFourTabsPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IOSSegmentedControl(
                tabs = listOf("概览", "事实流", "画像", "资料库"),
                selectedIndex = 0,
                onTabSelected = {}
            )
        }
    }
}

@Preview(name = "两个标签", showBackground = true)
@Composable
private fun IOSSegmentedControlTwoTabsPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IOSSegmentedControl(
                tabs = listOf("时光轴", "清单"),
                selectedIndex = 1,
                onTabSelected = {}
            )
        }
    }
}

@Preview(name = "三个标签-选中中间", showBackground = true)
@Composable
private fun IOSSegmentedControlThreeTabsPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IOSSegmentedControl(
                tabs = listOf("分析", "润色", "回复"),
                selectedIndex = 1,
                onTabSelected = {}
            )
        }
    }
}
