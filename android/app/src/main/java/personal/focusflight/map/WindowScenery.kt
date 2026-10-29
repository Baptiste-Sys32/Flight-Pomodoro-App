package personal.focusflight.map

import android.content.Context
import android.view.Choreographer
import android.view.TextureView
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

/** Offline fictional scenery. It deliberately contains no geographic map or network requests. */
@Composable
fun WindowScenery(progress: Double,durationMs: Long,paused: Boolean,covered: Boolean,modifier: Modifier=Modifier) {
    val owner=LocalLifecycleOwner.current
    var renderer by remember {mutableStateOf<SceneryRenderer?>(null)}
    DisposableEffect(owner,renderer) {
        val observer=LifecycleEventObserver {_,event->
            if(event==Lifecycle.Event.ON_START) renderer?.visible=true
            if(event==Lifecycle.Event.ON_STOP) renderer?.visible=false
        }
        owner.lifecycle.addObserver(observer)
        renderer?.visible=owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        onDispose {owner.lifecycle.removeObserver(observer)}
    }
    var mounted by remember {mutableStateOf(false)}
    LaunchedEffect(Unit) {
        // Let the preceding MapView release its EGL resources before creating another engine.
        withFrameNanos { }
        kotlinx.coroutines.delay(160)
        mounted=true
    }
    if(mounted) AndroidView(factory={context->SceneryRenderer(context).also {renderer=it}.container},
        modifier=modifier,onRelease={(it.tag as SceneryRenderer).close()},
        update={(it.tag as SceneryRenderer).update(progress,durationMs,paused,covered)})
}

internal interface SceneryFrameSource {fun requestFrame(callback: (android.graphics.Bitmap)->Unit);fun diagnostics(): String}

private class SceneryRenderer(context: Context): Choreographer.FrameCallback,SceneryFrameSource {
    val texture=TextureView(context)
    val container=android.widget.FrameLayout(context).apply {
        tag=this@SceneryRenderer
        addView(texture,android.widget.FrameLayout.LayoutParams(-1,-1))
    }
    private val viewer: ModelViewer
    private val choreographer=Choreographer.getInstance()
    private var closed=false
    private var scheduled=false
    private var elapsed=0.0
    private var duration=1500.0
    private var updatedAt=0L
    private var paused=false
    private var covered=false
    private var lastFrame=0L
    private var calls=0
    private var frames=0
    var visible=true
        set(value) {field=value;if(value) schedule() else {choreographer.removeFrameCallback(this);scheduled=false}}
    init {
        Utils.init()
        texture.tag=this
        if(personal.focusflight.BuildConfig.DEBUG) android.util.Log.d("WindowScenery","Renderer created")
        texture.contentDescription="Animated airport and cloud scenery"
        // A null manipulator gives our flight camera sole control; no orbit gestures leak into the blind.
        viewer=ModelViewer(textureView=texture,manipulator=null)
        val bytes=context.assets.open("window_scene.glb").use {it.readBytes()}
        viewer.loadModelGlb(ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).apply {flip()})
        viewer.scene.skybox=Skybox.Builder().color(.43f,.67f,.83f,1f).build(viewer.engine)
        viewer.scene.indirectLight=IndirectLight.Builder().irradiance(3,FloatArray(27).also {it[0]=.7f;it[1]=.75f;it[2]=.8f})
            .intensity(15_000f).build(viewer.engine)
        viewer.engine.lightManager.setDirection(viewer.engine.lightManager.getInstance(viewer.light),.5f,-1f,-.3f)
        viewer.camera.setExposure(16f,1f/125f,100f)
        viewer.view.antiAliasing=com.google.android.filament.View.AntiAliasing.FXAA
        viewer.view.fogOptions=com.google.android.filament.View.FogOptions().apply {
            enabled=true;distance=600f;density=.0008f;height=0f;heightFalloff=0f
            color=floatArrayOf(.43f,.67f,.83f);maximumOpacity=.95f;fogColorFromIbl=false
        }
        texture.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {schedule()}
            override fun onViewDetachedFromWindow(v: View) {close()}
        })
        schedule()
    }
    fun update(progress: Double,durationMs: Long,stopped: Boolean,hidden: Boolean) {
        elapsed=progress.coerceIn(0.0,1.0)*durationMs/1000.0
        duration=durationMs/1000.0
        updatedAt=android.os.SystemClock.elapsedRealtimeNanos()
        paused=stopped
        covered=hidden
        if(covered) {choreographer.removeFrameCallback(this);scheduled=false} else schedule()
    }
    private fun schedule() {
        if(!closed && visible && !covered && !scheduled) {scheduled=true;choreographer.postFrameCallback(this)}
    }
    override fun doFrame(frameTimeNanos: Long) {
        scheduled=false
        calls++
        if(closed || !visible) return
        // Cap the small window at 30 fps. A covered/paused window needs only loading/refresh frames.
        if(frameTimeNanos-lastFrame>=32_000_000) {
            lastFrame=frameTimeNanos
            val extra=if(paused) 0.0 else ((android.os.SystemClock.elapsedRealtimeNanos()-updatedAt)/1e9).coerceAtLeast(0.0)
            val t=elapsed+extra
            val remaining=(duration-t).coerceAtLeast(0.0)
            val climb=(minOf(t,remaining)/60.0).coerceIn(0.0,1.0)
            val smooth=climb*climb*(3-2*climb)
            val y=5+smooth*330
            val z=when {t<60->450-t*10;remaining<60->450-(60-remaining)*10;else->-150-((t-60)*9%1600)}
            val roll=sin(t*.17)*.004
            if(texture.height>0) viewer.camera.setProjection(52.0,texture.width.toDouble()/texture.height,.3,6000.0,com.google.android.filament.Camera.Fov.VERTICAL)
            viewer.camera.lookAt(0.0,y,z,500.0,y-(15+smooth*180),z-100,roll,1.0,0.0)
            texture.invalidate();container.invalidate()
            if(viewer.render(frameTimeNanos)) {
                frames++
                if(viewer.progress>=1f) texture.contentDescription="Window scenery ready"
            }
        }
        if(!paused || viewer.progress<1f) schedule()
    }
    override fun diagnostics()="calls=$calls frames=$frames closed=$closed visible=$visible covered=$covered paused=$paused shown=${texture.isShown} available=${texture.isAvailable} alpha=${texture.alpha} hardware=${texture.isHardwareAccelerated}"
    override fun requestFrame(callback: (android.graphics.Bitmap)->Unit) {
        viewer.debugGetNextFrameCallback(callback)
        schedule()
    }
    fun close() {
        if(closed) return
        closed=true
        if(personal.focusflight.BuildConfig.DEBUG) android.util.Log.d("WindowScenery","Renderer closed")
        choreographer.removeFrameCallback(this)
        scheduled=false
        // ModelViewer owns destruction when its TextureView detaches. Do not destroy it twice.
    }
}
