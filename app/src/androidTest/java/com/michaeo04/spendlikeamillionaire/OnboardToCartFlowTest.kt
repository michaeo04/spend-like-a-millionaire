package com.michaeo04.spendlikeamillionaire

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end happy path on a fresh install (English UI): onboarding -> buy the cheapest item ->
 * see it on the receipt. Gradle installs the app fresh for connected tests, so no stored state exists.
 */
@RunWith(AndroidJUnit4::class)
class OnboardToCartFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, timeoutMs: Long = 10_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun onboardingThenBuyThenReceipt() {
        // Welcome, then language, then currency (defaults are fine)
        waitForText("Get started")
        compose.onNodeWithText("Get started").performClick()
        waitForText("Choose your language")
        compose.onNodeWithText("Next").performClick()
        waitForText("Pick your currency")
        compose.onNodeWithText("Next").performClick()

        // Step 3 person
        waitForText("Whose fortune will you spend?")
        waitForText("Elon Musk")
        compose.onNodeWithText("Elon Musk").performClick()
        compose.onNodeWithText("Start spending").performClick()

        // Shop: add one of the cheapest item with the + button
        waitForText("Remaining")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Increase quantity of Snickers bar").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Increase quantity of Snickers bar").performClick()
        waitForText("View cart (1)")

        // Receipt shows the cheapest item
        compose.onNodeWithText("View cart (1)").performClick()
        waitForText("Your receipt")
        waitForText("Snickers bar")
        waitForText("Share receipt")
    }
}
