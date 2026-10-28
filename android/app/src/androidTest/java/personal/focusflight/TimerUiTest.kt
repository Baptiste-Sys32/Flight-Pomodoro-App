package personal.focusflight

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import android.view.View
import android.view.ViewGroup
import com.mapbox.maps.MapView
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
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
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Start Journey").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithContentDescription("Airport CDG").fetchSemanticsNodes().isNotEmpty()
        }
        if(compose.onAllNodesWithContentDescription("Airport CDG").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Airport CDG").performClick()
        }
        compose.waitUntil(5000) {compose.onAllNodesWithText("Start Journey").fetchSemanticsNodes().isNotEmpty()}
        capture("home",true)
        compose.onNodeWithText("Start Journey").performClick()
        compose.waitUntil(5000) {compose.onAllNodes(hasText("Book My Flight") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
        capture("booking",true)
        compose.onNodeWithText("Book My Flight").performClick()
        compose.onNodeWithContentDescription("Seat 03F, Work").performClick()
        capture("seat")
        compose.onNodeWithText("Confirm").performClick()
        compose.waitUntil(5000) {compose.onAllNodes(hasText("Boarding") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()}
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5000) {compose.onAllNodesWithText("03F").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("03F").assertIsDisplayed()
        capture("pass")
        compose.onNode(hasText("Boarding") and hasClickAction()).performClick()
        compose.waitUntil(5000) {compose.onAllNodesWithText("GO").fetchSemanticsNodes().isNotEmpty()}
        capture("takeoff")
        compose.onNodeWithText("GO").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty() }
        capture("flight",true)
        compose.onNodeWithContentDescription("Window view").performClick()
        capture("window")
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Resume").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Resume").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Flight options").performClick()
        compose.onNodeWithText("Give up").performClick()
        compose.onNodeWithText("Give up").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Start Journey").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Start Journey").assertIsDisplayed()
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FlightApplication
        val id=runBlocking {app.repository.flights.first().first {it.state=="CANCELED" && !it.diagnostic}.id}
        compose.onNodeWithText("Mine").performClick()
        compose.onNodeWithText("FlightLog").performClick()
        compose.onNode(hasText("Status") and hasClickAction()).performClick()
        compose.onNode(hasText("Canceled") and hasClickAction()).performClick()
        compose.onNode(hasText("Scenario") and hasClickAction()).performClick()
        compose.onNode(hasText("Work") and hasClickAction()).performClick()
        compose.onAllNodes(hasText("CANCELED") and hasClickAction()).onFirst().performClick()
        compose.onNodeWithText("Edit").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Review chapter 3")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.waitUntil(5000) {runBlocking {app.database.flights().get(id)?.note=="Review chapter 3"}}
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5000) {compose.onAllNodesWithText("Review chapter 3").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Review chapter 3").performScrollTo().assertIsDisplayed()
    }

    private fun capture(name: String,waitForMap: Boolean=false) {
        if(waitForMap) {
            compose.waitUntil(45000) {
                var ready=false
                compose.activityRule.scenario.onActivity {ready=readyMap(it.window.decorView)}
                ready
            }
            Thread.sleep(200)
        }
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val directory=context.getExternalFilesDir("qa")!!;directory.mkdirs()
        File(directory,"$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }

    private fun readyMap(view: View): Boolean {
        if(view is MapView) return view.contentDescription in listOf("Flight map ready","Flight map limited")
        if(view is ViewGroup) for(i in 0 until view.childCount) if(readyMap(view.getChildAt(i))) return true
        return false
    }
}
