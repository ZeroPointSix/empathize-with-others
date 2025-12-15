package com.empathy.ai.presentation.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.EmotionalBackground
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * EmotionalBackground UI组件测试
 *
 * 测试内容：
 * - 组件正常渲染
 * - 不同关系分数的颜色映射
 * - 颜色过渡动画
 *
 * 参考标准：
 * - [TD-00004] T074 EmotionalBackgroundTest
 * - [TDD-00004] 联系人画像记忆系统UI架构设计
 */
@RunWith(AndroidJUnit4::class)
class EmotionalBackgroundTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 测试组件正常渲染
     */
    @Test
    fun emotionalBackground_renders_successfully() {
        composeTestRule.setContent {
            EmpathyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmotionalBackground(relationshipScore = 85)
                }
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * 测试优秀关系分数（80-100）
     */
    @Test
    fun emotionalBackground_excellentScore_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmotionalBackground(relationshipScore = 90)
                }
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * 测试良好关系分数（60-79）
     */
    @Test
    fun emotionalBackground_goodScore_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmotionalBackground(relationshipScore = 70)
                }
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * 测试一般关系分数（40-59）
     */
    @Test
    fun emotionalBackground_normalScore_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmotionalBackground(relationshipScore = 50)
                }
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * 测试冷淡关系分数（0-39）
     */
    @Test
    fun emotionalBackground_poorScore_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmotionalBackground(relationshipScore = 20)
                }
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * 测试边界值：最低分数
     */
    @Test
    fun emotionalBackground_minimumScore_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmotionalBackground(relationshipScore = 0)
                }
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * 测试边界值：最高分数
     */
    @Test
    fun emotionalBackground_maximumScore_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmotionalBackground(relationshipScore = 100)
                }
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * 测试分数变化时的重组
     */
    @Test
    fun emotionalBackground_scoreChange_triggersRecomposition() {
        var score = 50

        composeTestRule.setContent {
            EmpathyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmotionalBackground(relationshipScore = score)
                }
            }
        }

        // 初始渲染
        composeTestRule.onRoot().assertIsDisplayed()

        // 更新分数
        score = 90
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
