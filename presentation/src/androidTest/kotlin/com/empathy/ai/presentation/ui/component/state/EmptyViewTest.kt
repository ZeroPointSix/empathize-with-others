package com.empathy.ai.presentation.ui.component.state

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.empathy.ai.presentation.theme.ProvideAdaptiveDimensions
import org.junit.Rule
import org.junit.Test

class EmptyViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyView_displaysMessage() {
        composeTestRule.setContent {
            ProvideAdaptiveDimensions {
                EmptyView(message = "Nothing here")
            }
        }

        composeTestRule.onNodeWithText("Nothing here").assertIsDisplayed()
    }

    @Test
    fun emptyView_displaysActionText() {
        composeTestRule.setContent {
            ProvideAdaptiveDimensions {
                EmptyView(
                    message = "Empty",
                    actionText = "Add Item",
                    onAction = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Add Item").assertIsDisplayed()
    }
}
