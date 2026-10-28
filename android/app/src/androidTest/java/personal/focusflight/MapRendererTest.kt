package personal.focusflight

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.bindgen.Value
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapRendererTest {
    @Test fun sdkAcceptsGlobeTerrainBuildingsAndFlightLayers() {
        withLoadedMap("globe") {style->
            assertEquals("\"globe\"", style.getStyleProjectionProperty("name").value.toJson())
            assertEquals("\"ff-dem\"", style.getStyleTerrainProperty("source").value.toJson())
            assertEquals("true", style.getStyleImportConfigProperty("basemap", "show3dBuildings").value!!.value.toJson())
            assertTrue(style.styleLayers.any { layer -> layer.id == "ff-route-line" })
            assertTrue(style.styleLayers.any { layer -> layer.id == "ff-airport-labels" })
        }
    }

    @Test fun vectorStyleLabelsCanBeSwitchedOff() {
        withLoadedMap("globe") {style->
            assertEquals(Style.STANDARD,style.styleURI)
            val scope=if(style.getStyleImportConfigProperty("basemap","showPlaceLabels").value!=null) "basemap" else ""
            style.setStyleImportConfigProperty(scope,"showPlaceLabels",Value(false))
            assertEquals("false",style.getStyleImportConfigProperty(scope,"showPlaceLabels").value!!.value.toJson())
        }
    }

    private fun withLoadedMap(scene: String,check: (Style)->Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scenario = ActivityScenario.launch<MainActivity>(Intent(instrumentation.targetContext, MainActivity::class.java)
            .putExtra("renderer_preview", true).putExtra("renderer_scene",scene))
        var map: MapView? = null
        val deadline = android.os.SystemClock.elapsedRealtime() + 45_000
        var ready = false
        while (!ready && android.os.SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity { activity ->
                map = findMap(activity.window.decorView)
                ready = map?.contentDescription in listOf("Flight map ready","Flight map limited") &&
                    map?.mapboxMap?.style?.styleLayers?.any { it.id == "ff-aircraft" } == true
            }
            if (!ready) Thread.sleep(100)
        }
        assertTrue("The map style and flight overlay layers did not load", ready)
        Thread.sleep(2000)
        scenario.onActivity {
            check(map!!.mapboxMap.style!!)
        }
        scenario.close()
    }

    private fun findMap(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findMap(view.getChildAt(i))?.let { return it }
        return null
    }
}
