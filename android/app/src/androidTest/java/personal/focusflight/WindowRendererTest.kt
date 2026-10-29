package personal.focusflight

import android.graphics.Bitmap
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import personal.focusflight.domain.session.GeoPoint
import personal.focusflight.map.MapScene
import personal.focusflight.map.WindowFocusView
import java.io.File

/** Uses native Activity/View APIs, including on the phone's API 37. No Espresso input injector. */
@RunWith(AndroidJUnit4::class)
class WindowRendererTest {
    @Test fun offlineSceneryMovesAndPausesWithoutAMapView() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val paused=mutableStateOf(false)
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity {activity->activity.setContent {
                WindowFocusView(Modifier.fillMaxSize(),paused.value,MapScene(GeoPoint(0.0,0.0),progress=0.0))
            }}
            var texture: TextureView?=null
            val deadline=android.os.SystemClock.elapsedRealtime()+45_000
            while(android.os.SystemClock.elapsedRealtime()<deadline) {
                scenario.onActivity {activity->texture=findTexture(activity.window.decorView)}
                if(texture?.contentDescription=="Window scenery ready") break
                Thread.sleep(100)
            }
            assertEquals("Window scenery ready",texture?.contentDescription)
            fun frame(): Bitmap {
                val latch=java.util.concurrent.CountDownLatch(1)
                var bitmap: Bitmap?=null
                var result=-1
                scenario.onActivity {activity->
                    val target=texture!!
                    bitmap=Bitmap.createBitmap(target.width,target.height,Bitmap.Config.ARGB_8888)
                    val location=IntArray(2);target.getLocationInWindow(location)
                    val rect=android.graphics.Rect(location[0],location[1],location[0]+target.width,location[1]+target.height)
                    android.view.PixelCopy.request(activity.window,rect,bitmap!!,{status->result=status;latch.countDown()},android.os.Handler(android.os.Looper.getMainLooper()))
                }
                assertTrue("Window pixels did not arrive",latch.await(5,java.util.concurrent.TimeUnit.SECONDS))
                assertEquals(android.view.PixelCopy.SUCCESS,result)
                return bitmap!!
            }
            var gpuFrame: Bitmap?=null
            val gpuLatch=java.util.concurrent.CountDownLatch(1)
            scenario.onActivity {
                (texture!!.tag as personal.focusflight.map.SceneryFrameSource).requestFrame {bitmap->
                    gpuFrame=bitmap
                    val dir=context.getExternalFilesDir("qa")!!;dir.mkdirs()
                    File(dir,"native-window-gpu.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                    gpuLatch.countDown()
                }
            }
            assertTrue((texture!!.tag as personal.focusflight.map.SceneryFrameSource).diagnostics(),gpuLatch.await(5,java.util.concurrent.TimeUnit.SECONDS))
            val colors=HashSet<Int>()
            val gpu=gpuFrame!!
            for(y in 0 until gpu.height step 4) for(x in 0 until gpu.width step 4) colors.add(gpu.getPixel(x,y) and 0x00F0F0F0)
            assertTrue("Scenery must contain visible detail, not a blank or washed-out frame",colors.size>32)
            val a=frame();Thread.sleep(700);val b=frame()
            val directory=context.getExternalFilesDir("qa")!!;directory.mkdirs()
            File(directory,"native-window-scene.png").outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)}
            assertTrue("The 3D airport should move through the window",differences(a,b)>200)
            scenario.onActivity {paused.value=true}
            Thread.sleep(600)
            val frozen=frame();Thread.sleep(700);val still=frame()
            assertEquals("A paused window must stop its scenery",0,differences(frozen,still))
            scenario.onActivity {activity->assertFalse("Window must not contain a Mapbox renderer",containsMap(activity.window.decorView))}
        } finally {scenario.close()}
    }
    private fun differences(a: Bitmap,b: Bitmap): Int {
        assertEquals(a.width,b.width);assertEquals(a.height,b.height)
        val x=IntArray(a.width*a.height);val y=IntArray(x.size)
        a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height)
        return x.indices.count {x[it]!=y[it]}
    }
    private fun findTexture(view: View): TextureView? {
        if(view is TextureView) return view
        if(view is ViewGroup) for(i in 0 until view.childCount) findTexture(view.getChildAt(i))?.let {return it}
        return null
    }
    private fun containsMap(view: View): Boolean {
        if(view is com.mapbox.maps.MapView) return true
        if(view is ViewGroup) for(i in 0 until view.childCount) if(containsMap(view.getChildAt(i))) return true
        return false
    }
}
