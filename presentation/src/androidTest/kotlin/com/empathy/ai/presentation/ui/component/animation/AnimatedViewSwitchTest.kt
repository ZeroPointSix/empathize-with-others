package com.empathy.ai.presentation.ui.component.animation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class AnimatedViewSwitchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun animatedViewSwitch_switchesContent() {
        var state by mutableStateOf("A")

        composeTestRule.setContent {
            AnimatedViewSwitch(targetState = state) { currentState ->
                Text("State $currentState")
            }
        }

        composeTestRule.onNodeWithText("State A").assertIsDisplayed()

        state = "B"
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("State B").assertIsDisplayed()
    }
}
