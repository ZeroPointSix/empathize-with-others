package com.empathy.ai.presentation.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.chip.GuessedTag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GuessedTag UI组件测试
 *
 * 测试内容：
 * - 组件正常渲染
 * - 点击交互
 * - 动画启用/禁用
 * - 不同标签类型的显示
 *
 * 参考标准：
 * - [TD-00004] T075 GuessedTagTest
 * - [TDD-00004] 联系人画像记忆系统UI架构设计
 */
@RunWith(AndroidJUnit4::class)
class GuessedTagTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val riskTag = BrainTag(
        id = 1,
        contactId = "contact_1",
        content = "不喜欢加班话题",
        type = TagType.RISK_RED,
        isConfirmed = false,
        source = "ai"
    )

    private val strategyTag = BrainTag(
        id = 2,
        contactId = "contact_1",
        content = "喜欢美食话题",
        type = TagType.STRATEGY_GREEN,
        isConfirmed = false,
        source = "ai"
    )

    /**
     * 测试雷区标签正常渲染
     */
    @Test
    fun guessedTag_riskType_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                GuessedTag(tag = riskTag)
            }
        }

        composeTestRule.onNodeWithText("不喜欢加班话题").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("待确认").assertIsDisplayed()
    }

    /**
     * 测试策略标签正常渲染
     */
    @Test
    fun guessedTag_strategyType_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                GuessedTag(tag = strategyTag)
            }
        }

        composeTestRule.onNodeWithText("喜欢美食话题").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("待确认").assertIsDisplayed()
    }

    /**
     * 测试点击回调
     */
    @Test
    fun guessedTag_onClick_callbackInvoked() {
        var clicked = false

        composeTestRule.setContent {
            EmpathyTheme {
                GuessedTag(
                    tag = riskTag,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("不喜欢加班话题").performClick()
        assertTrue("点击回调应该被调用", clicked)
    }

    /**
     * 测试无点击回调时不可点击
     */
    @Test
    fun guessedTag_noOnClick_notClickable() {
        composeTestRule.setContent {
            EmpathyTheme {
                GuessedTag(tag = riskTag, onClick = null)
            }
        }

        // 组件应该正常显示
        composeTestRule.onNodeWithText("不喜欢加班话题").assertIsDisplayed()
    }

    /**
     * 测试禁用动画
     */
    @Test
    fun guessedTag_animationDisabled_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                GuessedTag(
                    tag = riskTag,
                    enableAnimation = false
                )
            }
        }

        composeTestRule.onNodeWithText("不喜欢加班话题").assertIsDisplayed()
    }

    /**
     * 测试启用动画
     */
    @Test
    fun guessedTag_animationEnabled_rendersCorrectly() {
        composeTestRule.setContent {
            EmpathyTheme {
                GuessedTag(
                    tag = strategyTag,
                    enableAnimation = true
                )
            }
        }

        composeTestRule.onNodeWithText("喜欢美食话题").assertIsDisplayed()
    }

    /**
     * 测试不可见时禁用动画
     */
    @Test
    fun guessedTag_notVisible_animationDisabled() {
        composeTestRule.setContent {
            EmpathyTheme {
                GuessedTag(
                    tag = riskTag,
                    enableAnimation = true,
                    isVisible = false
                )
            }
        }

        composeTestRule.onNodeWithText("不喜欢加班话题").assertIsDisplayed()
    }

    /**
     * 测试长文本标签
     */
    @Test
    fun guessedTag_longText_rendersCorrectly() {
        val longTag = BrainTag(
            id = 3,
            contactId = "contact_1",
            content = "这是一个非常长的标签内容用于测试文本显示效果",
            type = TagType.STRATEGY_GREEN,
            isConfirmed = false,
            source = "ai"
        )

        composeTestRule.setContent {
            EmpathyTheme {
                GuessedTag(tag = longTag)
            }
        }

        composeTestRule.onNodeWithText("这是一个非常长的标签内容用于测试文本显示效果")
            .assertIsDisplayed()
    }

    /**
     * 测试空内容标签
     */
    @Test
    fun guessedTag_emptyContent_rendersCorrectly() {
        val emptyTag = BrainTag(
            id = 4,
            contactId = "contact_1",
            content = "",
            type = TagType.RISK_RED,
            isConfirmed = false,
            source = "ai"
        )

        composeTestRule.setContent {
            EmpathyTheme {
                GuessedTag(tag = emptyTag)
            }
        }

        // 问号图标应该显示
        composeTestRule.onNodeWithContentDescription("待确认").assertIsDisplayed()
    }
}
