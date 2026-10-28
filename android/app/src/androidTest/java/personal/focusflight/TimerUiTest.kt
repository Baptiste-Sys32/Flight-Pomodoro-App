package personal.focusflight

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Espresso's input injector uses an API removed on API 37; exercise this on API 36. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(maxSdkVersion = 36)
class TimerUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun visibleControlsStartPauseResumeAndCancel() {
        compose.onNodeWithText("15-second test flight").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Resume").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Resume").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Flight options").performClick()
        compose.onNodeWithText("Give up").performClick()
        compose.onNodeWithText("Give up").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Start focus flight").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Start focus flight").assertIsDisplayed()
    }
}
