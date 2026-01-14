package com.empathy.ai.presentation.navigation

import androidx.navigation.NavController
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 针对 AI 军师 popUpTo 锚点自动回退逻辑的单元测试。
 *
 * 场景覆盖：
 * 1. AI_ADVISOR 存在于返回栈 -> 应直接使用作为锚点，保持原有子栈行为。
 * 2. AI_ADVISOR 不在返回栈 -> 应回退到 CONTACT_LIST，避免栈被清空导致无法返回。
 */
class NavGraphAiAdvisorAnchorTest {

    @Test
    fun `aiAdvisorPopAnchor prefers AI_ADVISOR when route exists`() {
        val navController = mockk<NavController>()
        every { navController.getBackStackEntry(NavRoutes.AI_ADVISOR) } returns mockk(relaxed = true)

        val anchor = navController.aiAdvisorPopAnchor()

        assertEquals(NavRoutes.AI_ADVISOR, anchor)
    }

    @Test
    fun `aiAdvisorPopAnchor falls back to CONTACT_LIST when AI_ADVISOR missing`() {
        val navController = mockk<NavController>()
        every {
            navController.getBackStackEntry(NavRoutes.AI_ADVISOR)
        } throws IllegalArgumentException("Route not found")

        val anchor = navController.aiAdvisorPopAnchor()

        assertEquals(NavRoutes.CONTACT_LIST, anchor)
    }
}
