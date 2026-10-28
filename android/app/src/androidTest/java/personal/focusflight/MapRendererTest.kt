package personal.focusflight

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.maps.MapView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapRendererTest {
    @Test fun sdkAcceptsGlobeTerrainBuildingsAndFlightLayers() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scenario = ActivityScenario.launch<MainActivity>(Intent(instrumentation.targetContext, MainActivity::class.java)
            .putExtra("renderer_preview", true))
        var map: MapView? = null
        val deadline = android.os.SystemClock.elapsedRealtime() + 45_000
        var ready = false
        while (!ready && android.os.SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity { activity ->
                map = findMap(activity.window.decorView)
                ready = map?.contentDescription == "Flight map ready" &&
                    map?.mapboxMap?.style?.styleLayers?.any { it.id == "ff-aircraft" } == true
            }
            if (!ready) Thread.sleep(100)
        }
        assertTrue("The complete map, including custom resources, did not load", ready)
        Thread.sleep(2000)
        scenario.onActivity {
            val style = map!!.mapboxMap.style!!
            assertEquals("\"globe\"", style.getStyleProjectionProperty("name").value.toJson())
            assertEquals("\"ff-dem\"", style.getStyleTerrainProperty("source").value.toJson())
            assertEquals("true", style.getStyleImportConfigProperty("basemap", "show3dBuildings").value!!.value.toJson())
            assertTrue(style.styleLayers.any { layer -> layer.id == "ff-route-line" })
            assertTrue(style.styleLayers.any { layer -> layer.id == "ff-airport-labels" })
        }
        scenario.close()
    }

    private fun findMap(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findMap(view.getChildAt(i))?.let { return it }
        return null
    }
}
