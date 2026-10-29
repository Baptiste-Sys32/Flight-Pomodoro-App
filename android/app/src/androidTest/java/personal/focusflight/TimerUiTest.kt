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
        // API 36 is our disposable emulator; clean a session left by an interrupted test.
        val fixtureApp=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FlightApplication
        runBlocking {fixtureApp.repository.cancel()}
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
        val ruler=compose.onNodeWithContentDescription("Focus duration")
        val before=ruler.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
        ruler.performTouchInput {swipe(center,androidx.compose.ui.geometry.Offset(center.x-width*.17f,center.y),700)}
        compose.waitUntil(3000) {ruler.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]!=before}
        capture("ruler-moved")
        compose.onNodeWithText("Book My Flight").performClick()
        compose.onNodeWithContentDescription("Seat 08F, Work").performScrollTo().performClick()
        compose.onNodeWithText("Confirm").assertIsDisplayed()
        capture("seat-scrolled")
        compose.onNodeWithContentDescription("Seat 03F, Work").performScrollTo().performClick()
        capture("seat")
        compose.onNodeWithText("Confirm").performClick()
        compose.waitUntil(5000) {compose.onAllNodes(hasText("Boarding") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()}
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5000) {compose.onAllNodesWithText("03F").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("03F").assertIsDisplayed()
        capture("pass")
        val barcode=compose.onNodeWithContentDescription("Boarding pass barcode").captureToImage().asAndroidBitmap()
        val pixels=IntArray(barcode.width*barcode.height)
        barcode.getPixels(pixels,0,barcode.width,0,0,barcode.width,barcode.height)
        val decoded=com.google.zxing.pdf417.PDF417Reader().decode(com.google.zxing.BinaryBitmap(
            com.google.zxing.common.HybridBinarizer(com.google.zxing.RGBLuminanceSource(barcode.width,barcode.height,pixels))))
        org.junit.Assert.assertTrue(decoded.text.startsWith("FOCUS FLIGHT: "))
        compose.onNode(hasText("Boarding") and hasClickAction()).performClick()
        compose.waitUntil(5000) {compose.onAllNodesWithText("GO").fetchSemanticsNodes().isNotEmpty()}
        capture("takeoff")
        compose.onNodeWithText("GO").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty() }
        capture("flight",true)
        // A real map drag must release follow and stay released across timer ticks.
        compose.onRoot().performTouchInput {
            swipe(androidx.compose.ui.geometry.Offset(width*.5f,height*.46f),androidx.compose.ui.geometry.Offset(width*.78f,height*.46f),500)
        }
        compose.waitUntil(5000) {compose.onAllNodesWithContentDescription("Follow aircraft").fetchSemanticsNodes().isNotEmpty()}
        fun center(): com.mapbox.geojson.Point {
            var point: com.mapbox.geojson.Point?=null
            compose.activityRule.scenario.onActivity {activity->
                fun find(v: View): MapView? {
                    if(v is MapView) return v
                    if(v is ViewGroup) for(i in 0 until v.childCount) find(v.getChildAt(i))?.let {return it}
                    return null
                }
                point=find(activity.window.decorView)!!.mapboxMap.cameraState.center
            }
            return point!!
        }
        Thread.sleep(1000) // Let gesture inertia settle before checking timer ticks.
        val detached=center()
        Thread.sleep(1200)
        org.junit.Assert.assertEquals(detached.latitude(),center().latitude(),.000001)
        org.junit.Assert.assertEquals(detached.longitude(),center().longitude(),.000001)
        compose.onNodeWithContentDescription("Follow aircraft").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Route overview").assertIsDisplayed()
        compose.onNodeWithContentDescription("Choose map style").performClick()
        compose.onNodeWithText("Terra").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        capture("flight-terra",true)
        compose.onNodeWithContentDescription("Choose map style").performClick()
        compose.onNodeWithText("Monochrome").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        capture("flight-monochrome",true)
        compose.onNodeWithContentDescription("Choose map style").performClick()
        compose.onNodeWithText("Standard").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithContentDescription("Window view").performClick()
        capture("window",true)
        compose.onNodeWithContentDescription("Window blind").performTouchInput {
            swipe(center,androidx.compose.ui.geometry.Offset(center.x,height*7f),650)
        }
        compose.waitUntil(3000) {compose.onAllNodesWithText("Open blind").fetchSemanticsNodes().isNotEmpty()}
        capture("window-closed")
        compose.onNodeWithText("Open blind").performClick()
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Resume").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Flight paused").assertIsDisplayed()
        capture("paused")
        compose.onNodeWithText("Resume flight").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Flight options").performClick()
        compose.onNodeWithText("Give up").performClick()
        compose.onNodeWithText("Give up").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Start Journey").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Start Journey").assertIsDisplayed()
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FlightApplication
        val id=runBlocking {app.repository.flights.first().first {it.state=="CANCELED" && !it.diagnostic}.id}
        compose.onNodeWithContentDescription("Profile and settings").performClick()
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
        if(view is android.view.TextureView && view.contentDescription=="Window scenery ready") return true
        if(view is MapView) return view.contentDescription in listOf("Flight map ready","Flight map limited")
        if(view is ViewGroup) for(i in 0 until view.childCount) if(readyMap(view.getChildAt(i))) return true
        return false
    }
}
