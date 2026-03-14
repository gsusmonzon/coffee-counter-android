package com.gsusmonzon.coffeecounter

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gsusmonzon.coffeecounter.ui.UiTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowsInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val repository by lazy {
        val application = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .applicationContext as CoffeeCounterApplication
        application.appContainer.coffeeRepository
    }

    @Before
    fun resetAppState() = runBlocking {
        repository.resetAll()
        composeRule.waitForIdle()
    }

    @Test
    fun addCoffee_updatesTodayAndHistoryFromHomeScreen() {
        composeRule.onNodeWithTag(UiTestTags.HOME_TODAY_COUNT).assertTextEquals("0")
        composeRule.onNodeWithTag(UiTestTags.HOME_7_DAY_TOTAL).assertTextEquals("0")
        composeRule.onNodeWithTag(UiTestTags.HOME_30_DAY_TOTAL).assertTextEquals("0")

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.add_coffee_label)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(UiTestTags.HOME_TODAY_COUNT).assertTextEquals("1")
        composeRule.onNodeWithTag(UiTestTags.HOME_7_DAY_TOTAL).assertTextEquals("1")
        composeRule.onNodeWithTag(UiTestTags.HOME_30_DAY_TOTAL).assertTextEquals("1")
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.history_average_per_active_day_label, "1")
        ).assertIsDisplayed()
    }

    @Test
    fun resetAll_requiresConfirmationBeforeClearingCounts() = runBlocking {
        repository.incrementToday()
        repository.incrementToday()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(UiTestTags.NAV_SETTINGS).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_RESET_BUTTON).performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.delete_all_history_confirmation_title)
        ).assertIsDisplayed()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.delete_all_history_cancel_label))
            .performClick()
        composeRule.onAllNodesWithText(
            composeRule.activity.getString(R.string.delete_all_history_confirmation_title)
        ).assertCountEquals(0)

        composeRule.onNodeWithTag(UiTestTags.NAV_HOME).performClick()
        composeRule.onNodeWithTag(UiTestTags.HOME_TODAY_COUNT).assertTextEquals("2")

        composeRule.onNodeWithTag(UiTestTags.NAV_SETTINGS).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_RESET_BUTTON).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.delete_all_history_confirm_label))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(UiTestTags.NAV_HOME).performClick()
        composeRule.onNodeWithTag(UiTestTags.HOME_TODAY_COUNT).assertTextEquals("0")
        composeRule.onNodeWithTag(UiTestTags.HOME_7_DAY_TOTAL).assertTextEquals("0")
        composeRule.onNodeWithTag(UiTestTags.HOME_30_DAY_TOTAL).assertTextEquals("0")
    }
}
