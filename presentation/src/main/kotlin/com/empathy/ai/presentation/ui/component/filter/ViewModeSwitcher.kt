package com.empathy.ai.presentation.ui.component.filter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.ios.IOSSegmentedControl

/**
 * 视图模式枚举
 * 用于事实流页面的时光轴/清单视图切换
 */
enum class ViewMode(val displayName: String) {
    TIMELINE("时光轴"),
    LIST("清单")
}

/**
 * 视图模式切换器
 * 
 * 使用IOSSegmentedControl实现时光轴/清单视图切换
 * 
 * @param currentMode 当前视图模式
 * @param onModeChange 模式变化回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 8.2 FactStreamTab视图切换
 */
@Composable
fun ViewModeSwitcher(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = ViewMode.entries
    
    IOSSegmentedControl(
        tabs = modes.map { it.displayName },
        selectedIndex = modes.indexOf(currentMode),
        onTabSelected = { index ->
            onModeChange(modes[index])
        },
        modifier = modifier
    )
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "时光轴模式", showBackground = true)
@Composable
private fun ViewModeSwitcherTimelinePreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ViewModeSwitcher(
                currentMode = ViewMode.TIMELINE,
                onModeChange = {}
            )
        }
    }
}

@Preview(name = "清单模式", showBackground = true)
@Composable
private fun ViewModeSwitcherListPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ViewModeSwitcher(
                currentMode = ViewMode.LIST,
                onModeChange = {}
            )
        }
    }
}
