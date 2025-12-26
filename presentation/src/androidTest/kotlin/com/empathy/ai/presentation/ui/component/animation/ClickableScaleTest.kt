package com.empathy.ai.presentation.ui.component.animation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ClickableScaleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickableScale_clickInvoked() {
        var clicked = false
        composeTestRule.setContent {
            ClickableScale(onClick = { clicked = true }) {
                Text("Click Me")
            }
        }

        composeTestRule.onNodeWithText("Click Me").performClick()
        assert(clicked)
    }
}
