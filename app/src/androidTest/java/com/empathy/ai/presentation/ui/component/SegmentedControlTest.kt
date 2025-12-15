package com.empathy.ai.presentation.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.control.SegmentedControl
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SegmentedControl UI组件测试
 *
 * 测试内容：
 * - 组件正常渲染
 * - 选项切换
 * - 多选项支持
 * - 选中状态显示
 *
 * 参考标准：
 * - [TD-00004] T076 SegmentedControlTest
 * - [TDD-00004] 联系人画像记忆系统UI架构设计
 */
@RunWith(AndroidJUnit4::class)
class SegmentedControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 测试双选项正常渲染
     */
    @Test
    fun segmentedControl_twoItems_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                SegmentedControl(
                    items = listOf("时光轴", "清单"),
                    selectedIndex = 0,
                    onItemSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("时光轴").assertIsDisplayed()
        composeTestRule.onNodeWithText("清单").assertIsDisplayed()
    }

    /**
     * 测试三选项正常渲染
     */
    @Test
    fun segmentedControl_threeItems_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                SegmentedControl(
                    items = listOf("概览", "事实流", "标签"),
                    selectedIndex = 0,
                    onItemSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("概览").assertIsDisplayed()
        composeTestRule.onNodeWithText("事实流").assertIsDisplayed()
        composeTestRule.onNodeWithText("标签").assertIsDisplayed()
    }

    /**
     * 测试四选项正常渲染
     */
    @Test
    fun segmentedControl_fourItems_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                SegmentedControl(
                    items = listOf("概览", "事实流", "标签", "资料库"),
                    selectedIndex = 0,
                    onItemSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("概览").assertIsDisplayed()
        composeTestRule.onNodeWithText("事实流").assertIsDisplayed()
        composeTestRule.onNodeWithText("标签").assertIsDisplayed()
        composeTestRule.onNodeWithText("资料库").assertIsDisplayed()
    }

    /**
     * 测试点击切换选项
     */
    @Test
    fun segmentedControl_clickItem_changesSelection() {
        var selectedIndex = 0

        composeTestRule.setContent {
            EmpathyTheme {
                SegmentedControl(
                    items = listOf("时光轴", "清单"),
                    selectedIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it }
                )
            }
        }

        // 点击第二个选项
        composeTestRule.onNodeWithText("清单").performClick()
        assertEquals("选中索引应该变为1", 1, selectedIndex)
    }

    /**
     * 测试点击已选中项
     */
    @Test
    fun segmentedControl_clickSelectedItem_callbackStillInvoked() {
        var callbackCount = 0

        composeTestRule.setContent {
            EmpathyTheme {
                SegmentedControl(
                    items = listOf("时光轴", "清单"),
                    selectedIndex = 0,
                    onItemSelected = { callbackCount++ }
                )
            }
        }

        // 点击已选中的第一个选项
        composeTestRule.onNodeWithText("时光轴").performClick()
        assertEquals("回调应该被调用", 1, callbackCount)
    }

    /**
     * 测试选项切换回调参数
     */
    @Test
    fun segmentedControl_itemSelection_correctIndexPassed() {
        val selectedIndices = mutableListOf<Int>()

        composeTestRule.setContent {
            EmpathyTheme {
                SegmentedControl(
                    items = listOf("A", "B", "C"),
                    selectedIndex = 0,
                    onItemSelected = { selectedIndices.add(it) }
                )
            }
        }

        // 依次点击各选项
        composeTestRule.onNodeWithText("B").performClick()
        composeTestRule.onNodeWithText("C").performClick()
        composeTestRule.onNodeWithText("A").performClick()

        assertEquals("应该记录3次选择", 3, selectedIndices.size)
        assertEquals("第一次选择索引1", 1, selectedIndices[0])
        assertEquals("第二次选择索引2", 2, selectedIndices[1])
        assertEquals("第三次选择索引0", 0, selectedIndices[2])
    }

    /**
     * 测试状态管理集成
     */
    @Test
    fun segmentedControl_withState_updatesCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                var selectedIndex by remember { mutableIntStateOf(0) }
                SegmentedControl(
                    items = listOf("时光轴", "清单"),
                    selectedIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it }
                )
            }
        }

        // 初始状态
        composeTestRule.onNodeWithText("时光轴").assertIsDisplayed()
        composeTestRule.onNodeWithText("清单").assertIsDisplayed()

        // 切换到清单
        composeTestRule.onNodeWithText("清单").performClick()
        composeTestRule.waitForIdle()

        // 切换回时光轴
        composeTestRule.onNodeWithText("时光轴").performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * 测试空列表
     */
    @Test
    fun segmentedControl_emptyItems_rendersWithoutCrash() {
        composeTestRule.setContent {
            EmpathyTheme {
                SegmentedControl(
                    items = emptyList(),
                    selectedIndex = 0,
                    onItemSelected = {}
                )
            }
        }

        // 不应该崩溃
        composeTestRule.waitForIdle()
    }

    /**
     * 测试单选项
     */
    @Test
    fun segmentedControl_singleItem_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                SegmentedControl(
                    items = listOf("唯一选项"),
                    selectedIndex = 0,
                    onItemSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("唯一选项").assertIsDisplayed()
    }

    /**
     * 测试长文本选项
     */
    @Test
    fun segmentedControl_longText_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                SegmentedControl(
                    items = listOf("这是一个很长的选项文本", "短"),
                    selectedIndex = 0,
                    onItemSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("这是一个很长的选项文本").assertIsDisplayed()
        composeTestRule.onNodeWithText("短").assertIsDisplayed()
    }
}
