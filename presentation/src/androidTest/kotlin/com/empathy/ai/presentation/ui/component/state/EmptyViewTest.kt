package com.empathy.ai.presentation.ui.component.state

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class EmptyViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyView_displaysMessage() {
        composeTestRule.setContent {
            EmptyView(message = "Nothing here")
        }

        composeTestRule.onNodeWithText("Nothing here").assertIsDisplayed()
    }

    @Test
    fun emptyView_displaysActionText() {
        composeTestRule.setContent {
            EmptyView(
                message = "Empty",
                actionText = "Add Item",
                onAction = {}
            )
        }

        composeTestRule.onNodeWithText("Add Item").assertIsDisplayed()
    }
}
