package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfileAndSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testProfileScreenDisplaysSettingsButtonAndOpensSettings() {
        var settingsOpened = false

        composeTestRule.setContent {
            ProfileScreen(
                playerName = "Test User",
                playerEmail = "test@brainquiz.ai",
                xp = 100,
                level = 1,
                coins = 50,
                streakDays = 5,
                rank = "Novice",
                onOpenSettings = { settingsOpened = true },
                onSignOut = {}
            )
        }

        // Verify "Settings" button exists on Profile Screen
        composeTestRule.onNodeWithTag("settings_button").performScrollTo().assertIsDisplayed()
        
        // Click "Settings"
        composeTestRule.onNodeWithTag("settings_button").performScrollTo().performClick()

        assertTrue("onOpenSettings callback should be triggered", settingsOpened)
        // Verify Settings Screen is now displayed
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenResetAccountConfirmationDialog() {
        var resetCalled = false

        composeTestRule.setContent {
            SettingsScreen(
                playerName = "Test User",
                playerEmail = "test@brainquiz.ai",
                onResetAccount = { resetCalled = true },
                onBackClick = {}
            )
        }

        // Verify Settings screen and Reset Account option exist
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reset_account_button").performScrollTo().assertIsDisplayed()

        // Click Reset Account
        composeTestRule.onNodeWithTag("reset_account_button").performScrollTo().performClick()

        // Verify Confirmation Dialog with exact text
        composeTestRule.onNodeWithText("Are you sure you want to reset your game progress (XP, streak, score history, and coins)? Your account identity will remain active.").assertIsDisplayed()

        // Click Confirm
        composeTestRule.onNodeWithTag("confirm_reset_account_button").performClick()

        assertTrue("onResetAccount should be executed after confirmation", resetCalled)
    }

    @Test
    fun testSettingsScreenDeleteAccountDialog() {
        composeTestRule.setContent {
            SettingsScreen(
                playerName = "Test User",
                playerEmail = "test@brainquiz.ai",
                onDeleteAccount = {},
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_account_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_account_button").performScrollTo().performClick()

        // Verify Delete Account Dialog elements
        composeTestRule.onNodeWithTag("cancel_delete_account_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_delete_account_button").assertIsDisplayed()
    }
}
