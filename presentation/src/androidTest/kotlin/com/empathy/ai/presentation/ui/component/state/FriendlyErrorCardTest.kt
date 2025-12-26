package com.empathy.ai.presentation.ui.component.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.empathy.ai.presentation.util.UserFriendlyError
import org.junit.Rule
import org.junit.Test

class FriendlyErrorCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun friendlyErrorCard_displaysTitleAndMessage() {
        val error = UserFriendlyError(
            title = "Error Title",
            message = "Error Message",
            icon = Icons.Default.Warning
        )
        
        composeTestRule.setContent {
            FriendlyErrorCard(error = error, onAction = {})
        }

        composeTestRule.onNodeWithText("Error Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error Message").assertIsDisplayed()
    }

    @Test
    fun friendlyErrorCard_actionClick_invokesCallback() {
        var clicked = false
        val error = UserFriendlyError(
            title = "Title",
            message = "Message",
            actionLabel = "Retry",
            icon = Icons.Default.Warning
        )

        composeTestRule.setContent {
            FriendlyErrorCard(
                error = error, 
                onAction = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Retry").performClick()
        assert(clicked)
    }
}
