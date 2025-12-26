package com.empathy.ai.presentation.ui.component.animation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test
import androidx.compose.material3.Text

class AnimatedListItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun animatedListItem_visible_contentIsDisplayed() {
        composeTestRule.setContent {
            AnimatedListItem(visible = true) {
                Text("Test Item")
            }
        }

        composeTestRule.onNodeWithText("Test Item").assertIsDisplayed()
    }
    
    @Test
    fun animatedListItem_invisible_contentDoesNotExist() {
         composeTestRule.setContent {
            AnimatedListItem(visible = false) {
                Text("Hidden Item")
            }
        }

        // Note: In a real environment with full animations, asserting non-existence immediately might be tricky 
        // depending on exit animation duration, but verify "does not exist" or "is not displayed" is standard.
        composeTestRule.onNodeWithText("Hidden Item").assertDoesNotExist()
    }
}
