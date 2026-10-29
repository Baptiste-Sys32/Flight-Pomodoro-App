package personal.focusflight.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.graphics.drawable.toBitmap
import com.mapbox.bindgen.Value
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.image.addImage
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.modelLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ModelElevationReference
import com.mapbox.maps.extension.style.layers.properties.generated.ModelType
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.gestures.*
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.maps.plugin.scalebar.scalebar
import org.json.JSONArray
import org.json.JSONObject
import personal.focusflight.BuildConfig
import personal.focusflight.R
import personal.focusflight.domain.session.FlightRoute
import personal.focusflight.domain.session.GeoPoint
import kotlin.math.ln
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow

enum class MapMode(val title: String) { MONOCHROME("Monochrome"), TERRA("Terra"), STANDARD("Standard") }
enum class MapStatus { LOADING, READY, LIMITED, UNAVAILABLE }
data class MapAirport(val code: String, val point: GeoPoint, val departure: Boolean = false)
data class MapScene(
    val from: GeoPoint,
    val to: GeoPoint? = null,
    val airports: List<MapAirport> = emptyList(),
    val progress: Double? = null,
    val mode: MapMode = MapMode.STANDARD,
    val labels: Boolean = true,
    val terrain: Boolean = true,
    val buildings: Boolean = true,
    val follow: Boolean = false,
    val globe: Boolean = false,
    val zoomOverride: Double? = null,
    val bottomInsetDp: Float = 90f,
    val showRoute: Boolean = true,
    val animateDirection: Boolean = false,
    val cameraBearing: Double? = null,
    val cameraPitch: Double? = null,
    val movingCenter: Boolean = false,
    val ornamentBottomDp: Float = 28f,

)

@Composable
fun FlightMap(scene: MapScene, modifier: Modifier = Modifier, onUserGesture: () -> Unit = {}, onStatus: (MapStatus) -> Unit = {}) {
    if (BuildConfig.MAPBOX_ACCESS_TOKEN.isBlank()) {
        Box(modifier.background(Color(0xFF132422)), contentAlignment = Alignment.Center) { Text("Map unavailable", color = Color.White) }
        return
    }
    val currentCallback by rememberUpdatedState(onStatus)
    val gestureCallback by rememberUpdatedState(onUserGesture)
    var state by remember {mutableStateOf(MapStatus.LOADING)}
    var attempt by remember {mutableIntStateOf(0)}
    var renderer by remember {mutableStateOf<MapRenderer?>(null)}
    Box(modifier.pointerInput(Unit) {
        // Observe before AndroidView dispatch, without consuming the map's own gestures.
        awaitPointerEventScope {
            while(true) {
                val event=awaitPointerEvent(PointerEventPass.Initial)
                if(event.changes.any {it.pressed && it.position!=it.previousPosition}) renderer?.userInteraction()
            }
        }
    }) {
        key(attempt) {
            AndroidView(
                factory = { context -> MapRenderer(context,{gestureCallback()}) {state=it;currentCallback(it)}.also {renderer=it}.view },
                modifier = Modifier.fillMaxSize(),
                onReset = null,
                onRelease = { (it.tag as? MapRenderer)?.close() },
                update = { (it.tag as MapRenderer).render(scene) },
            )
        }
        if(state==MapStatus.LIMITED) Text("Some map layers are unavailable. Tap to retry.",
            color=Color.White,fontSize=11.sp,
            modifier=Modifier.align(Alignment.TopCenter).padding(top=135.dp,start=20.dp,end=20.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color(0xDC16201B)).clickable {attempt++}.padding(12.dp))
    }
}

/** Rendering has no authority over elapsed time, completed records or session credit. */
private class MapRenderer(context: Context, private val onGesture: () -> Unit, private val status: (MapStatus) -> Unit) {
    val view: MapView
    private var latest: MapScene? = null
    private var loaded: Style? = null
    private var requestedMode: MapMode? = null
    private var styleOptions: List<Any>? = null
    private var routePair: Triple<GeoPoint, GeoPoint?, Boolean>? = null
    private var lastCameraKey: List<Any?>? = null
    private var lastAirports: List<MapAirport>? = null
    private var closed = false
    private var limited = false
    private var frameReady=false
    private var userDetached = false
    private var lastFollow = false
    private var poseKey: List<Any?>? = null
    private var planeKey: List<Any?>? = null
    private var ornamentBottom: Float? = null
    private var animationStep = 0
    private val directionAnimation = object : Runnable {
        override fun run() {
            val scene=latest ?: return
            if(closed || !scene.animateDirection || !scene.showRoute || scene.to==null || loaded==null) return
            val p=FlightRoute.position(scene.from,scene.to,(animationStep++ % 80)/80.0)
            loaded?.setStyleSourceProperty("ff-direction","data",geoPoint(p))
            view.postDelayed(this,75)
        }
    }

    private val density = context.resources.displayMetrics.density

    init {
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        view = MapView(context, MapInitOptions(context, styleUri = null, textureView = true)).also { it.tag = this }
        view.compass.updateSettings { enabled = false }
        view.gestures.updateSettings {
            rotateEnabled=false;pitchEnabled=false;quickZoomEnabled=false
            scrollEnabled=true;pinchToZoomEnabled=true;pinchScrollEnabled=false
            scrollMode=com.mapbox.maps.plugin.ScrollMode.HORIZONTAL_AND_VERTICAL
            simultaneousRotateAndPinchToZoomEnabled=false
        }
        view.scalebar.updateSettings { enabled = false }
        fun detach() = userInteraction()
        view.gestures.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) = detach()
            override fun onMove(detector: MoveGestureDetector) = false
            override fun onMoveEnd(detector: MoveGestureDetector) = Unit
        })
        view.gestures.addOnScaleListener(object : OnScaleListener {
            override fun onScaleBegin(detector: StandardScaleGestureDetector) = detach()
            override fun onScale(detector: StandardScaleGestureDetector) = Unit
            override fun onScaleEnd(detector: StandardScaleGestureDetector) = Unit
        })
        view.gestures.addOnRotateListener(object : OnRotateListener {
            override fun onRotateBegin(detector: RotateGestureDetector) = detach()
            override fun onRotate(detector: RotateGestureDetector) = Unit
            override fun onRotateEnd(detector: RotateGestureDetector) = Unit
        })
        view.contentDescription = "Flight map loading"
        view.mapboxMap.subscribeMapLoaded {
            if (!closed) {
                frameReady=true
                aircraftPose()
                view.removeCallbacks(directionAnimation)
                if(latest?.animateDirection==true) view.post(directionAnimation)
                view.contentDescription = if(limited) "Flight map limited" else "Flight map ready"
                status(if(limited) MapStatus.LIMITED else MapStatus.READY)
            }
        }
        view.mapboxMap.subscribeMapLoadingError { event ->
            if (!closed) {
                // Provider URLs can include credentials. Log only a numeric HTTP status.
                val code=Regex("(?i)(?:http[^0-9]{0,40}|status[^0-9]{0,15})([45][0-9]{2})")
                    .find(event.message)?.groupValues?.get(1) ?: "unknown"
                val resource=when {
                    event.message.contains("terrain",true)->"terrain"
                    else->"map"
                }
                android.util.Log.w("FlightMap","Map resource error: $resource, ${event.type}, HTTP $code")
                if(BuildConfig.DEBUG) {
                    val safe=event.message.replace(BuildConfig.MAPBOX_ACCESS_TOKEN,"[redacted]")
                        .replace(Regex("""[a-zA-Z][a-zA-Z0-9+.-]*://\S+"""),"[resource]").take(240)
                    android.util.Log.d("FlightMap","Resource detail: $safe")
                }
                if(code in setOf("401","403","429") || event.type.toString().equals("Tile",true)) {
                    limited=true;view.contentDescription="Flight map limited";status(MapStatus.LIMITED)
                } else status(MapStatus.UNAVAILABLE)
            }
        }
        view.mapboxMap.subscribeCameraChanged {
            if(!closed) {
                val camera=view.mapboxMap.cameraState
                if(camera.zoom<5 && (kotlin.math.abs(camera.pitch)>.01 || kotlin.math.abs(camera.bearing)>.01)) {
                    view.mapboxMap.setCamera(CameraOptions.Builder().pitch(0.0).bearing(0.0).build())
                }
                aircraftPose()
            }
        }
        view.doOnLayout { if (!closed) {lastCameraKey=null;latest?.let(::render)} }
    }

    fun userInteraction() {
        if(latest?.follow==true && !userDetached) {userDetached=true;onGesture()}
    }

    fun render(scene: MapScene) {
        if (closed) return
        latest = scene
        if(!lastFollow && scene.follow) userDetached=false
        lastFollow=scene.follow
        if(ornamentBottom!=scene.ornamentBottomDp) {
            ornamentBottom=scene.ornamentBottomDp
            view.logo.updateSettings { position = Gravity.BOTTOM or Gravity.RIGHT; marginRight = 42f * density; marginBottom = scene.ornamentBottomDp * density }
            view.attribution.updateSettings { position = Gravity.BOTTOM or Gravity.RIGHT; marginRight = 10f * density; marginBottom = scene.ornamentBottomDp * density }
        }
        if (requestedMode == null) {
            requestedMode = scene.mode
            loaded = null
            routePair = null
            lastAirports = null
            styleOptions = null
            limited = false
            frameReady=false
            status(MapStatus.LOADING)
            view.contentDescription = "Flight map loading"
            val uri = Style.STANDARD
            view.mapboxMap.loadStyle(uri) { style ->
                if (closed) return@loadStyle
                loaded = style
                initialize(style)
                latest?.let(::render)
            }
            return
        }
        val style = loaded ?: return
        val options = listOf(scene.labels, scene.terrain, scene.buildings,scene.mode)
        if (options != styleOptions) {
            styleOptions = options
            val light=scene.mode!=MapMode.TERRA
            style.setStyleLayerProperty("ff-aircraft","model-emissive-strength",Value(if(light) .15 else .8))
            style.setStyleLayerProperty("ff-route-line","line-color",Value(if(light) "#214F62" else "#F3EDC7"))
            style.setStyleLayerProperty("ff-route-shadow","line-color",Value(if(light) "#FFFFFF" else "#101713"))
            style.setStyleProjection(json("""{"name":"globe"}"""))
            configure(style,"show3dObjects",Value(scene.buildings))
            configure(style,"show3dBuildings",Value(scene.buildings))
            for (property in listOf("showPlaceLabels", "showRoadLabels", "showPointOfInterestLabels", "showTransitLabels")) {
                configure(style,property,Value(scene.labels))
            }
            configure(style,"theme",Value(if (scene.mode == MapMode.MONOCHROME || scene.mode == MapMode.TERRA) "monochrome" else "default"))
            configure(style,"lightPreset",Value(if (scene.mode == MapMode.TERRA) "night" else "day"))
            style.setStyleTerrain(if (scene.terrain) json("""{"source":"ff-dem","exaggeration":1.2}""") else json("{}"))
        }
        val routeKey=Triple(scene.from,scene.to,scene.showRoute)
        if (routePair != routeKey) {
            routePair = routeKey
            val points = scene.to?.let { FlightRoute.points(scene.from, it) } ?: listOf(scene.from, scene.from)
            style.setStyleSourceProperty("ff-route", "data", if(scene.showRoute) geoLine(points)
                else json("""{"type":"FeatureCollection","features":[]}"""))
        }
        if(lastAirports!=scene.airports) {
            lastAirports=scene.airports
            val airports = JSONArray()
            scene.airports.forEach { airport ->
                if (!style.hasStyleImage("ff-${airport.code}")) style.addImage(image("ff-${airport.code}", airportChip(airport.code)))
                airports.put(JSONObject().put("type", "Feature").put("geometry", geometry("Point", coordinates(airport.point)))
                    .put("properties", JSONObject().put("chip", "ff-${airport.code}").put("departure",airport.departure)))
            }
            style.setStyleSourceProperty("ff-airports", "data", json(JSONObject().put("type", "FeatureCollection").put("features", airports).toString()))
        }
        val plane = scene.to?.let { FlightRoute.position(scene.from, it, scene.progress ?: 0.0) } ?: scene.from
        val planeState=listOf(plane,scene.progress!=null)
        if(planeKey!=planeState) {
            planeKey=planeState
            style.setStyleSourceProperty("ff-plane", "data", geoPoint(plane))
        }
        val bearing = scene.to?.let { FlightRoute.bearing(plane, it) } ?: 0.0
        style.setStyleLayerProperty("ff-plane-marker", "icon-rotate", Value(bearing))
        style.setStyleLayerProperty("ff-plane-marker", "visibility", Value(if (scene.progress == null) "none" else "visible"))
        val cameraKey = listOf(if(scene.movingCenter) null else scene.from, scene.to, scene.follow, scene.globe, scene.zoomOverride, scene.bottomInsetDp,scene.cameraPitch)
        val cameraChanged=cameraKey!=lastCameraKey
        if ((cameraChanged || scene.follow || scene.movingCenter) && !userDetached) {
            lastCameraKey = cameraKey
            val distance = scene.to?.let { FlightRoute.distance(scene.from, it) } ?: 0.0
            val center = when {
                scene.follow -> plane
                scene.zoomOverride != null -> scene.from
                scene.to != null -> FlightRoute.position(scene.from, scene.to, .5)
                else -> scene.from
            }
            val zoom = scene.zoomOverride ?: when {
                scene.globe -> 1.35
                scene.follow -> 9.0
                scene.to != null -> (ln(40_075_016.0 / distance.coerceAtLeast(1000.0)) / ln(2.0) - .8).coerceIn(1.4, 11.0)
                else -> 4.0
            }
            if(scene.to!=null && !scene.follow && !scene.globe && scene.zoomOverride==null) {
                val points=FlightRoute.points(scene.from,scene.to).map {Point.fromLngLat(it.lon,it.lat)}
                view.mapboxMap.cameraForCoordinates(points,
                    CameraOptions.Builder().bearing(0.0).pitch(0.0)
                        .padding(EdgeInsets(0.0,0.0,(scene.bottomInsetDp*density).toDouble(),0.0)).build(),
                    EdgeInsets(100.0*density,42.0*density,42.0*density,42.0*density),11.0,null) {camera->
                    if(!closed && !userDetached && lastCameraKey==cameraKey) view.mapboxMap.setCamera(camera)
                }
            } else {
            view.mapboxMap.setCamera(CameraOptions.Builder().center(Point.fromLngLat(center.lon, center.lat))
                .padding(EdgeInsets(0.0,0.0,(scene.bottomInsetDp*density).toDouble(),0.0))
                .zoom(if(!cameraChanged && scene.follow) view.mapboxMap.cameraState.zoom else zoom)
                .pitch(scene.cameraPitch ?: if (scene.follow || scene.zoomOverride != null) 55.0 else 0.0)
                .bearing(scene.cameraBearing ?: if (scene.follow) bearing else 0.0).build())
            }
        }
        aircraftPose()
        view.removeCallbacks(directionAnimation)
        if(frameReady && scene.animateDirection && scene.to!=null && scene.showRoute) view.post(directionAnimation)
    }

    private fun configure(style: Style,name: String,value: Value) {
        val scope=when {
            style.getStyleImportConfigProperty("basemap",name).value!=null->"basemap"
            style.getStyleImportConfigProperty("",name).value!=null->""
            else->return
        }
        style.setStyleImportConfigProperty(scope,name,value)
    }

    private fun initialize(style: Style) {
        style.addStyleSource("ff-dem", json("""{"type":"raster-dem","url":"mapbox://mapbox.mapbox-terrain-dem-v1","tileSize":512,"maxzoom":14}"""))
        style.addStyleSource("ff-route", json("""{"type":"geojson","data":{"type":"FeatureCollection","features":[]}}"""))
        style.addStyleSource("ff-direction",json("""{"type":"geojson","data":{"type":"FeatureCollection","features":[]}}"""))
        style.addStyleLayer(json("""{"id":"ff-route-direction","type":"circle","source":"ff-direction","slot":"top","paint":{"circle-radius":4,"circle-color":"#F0D95D","circle-stroke-color":"#102921","circle-stroke-width":1.5}}"""),null)
        style.addStyleSource("ff-plane", json("""{"type":"geojson","data":{"type":"FeatureCollection","features":[]}}"""))
        style.addStyleSource("ff-airports", json("""{"type":"geojson","data":{"type":"FeatureCollection","features":[]}}"""))
        style.addStyleLayer(json("""{"id":"ff-route-shadow","type":"line","source":"ff-route","slot":"top","layout":{"line-cap":"round","line-join":"round"},"paint":{"line-color":"#101713","line-width":6,"line-opacity":0.8,"line-emissive-strength":1}}"""), null)
        style.addStyleLayer(json("""{"id":"ff-route-line","type":"line","source":"ff-route","slot":"top","layout":{"line-cap":"round","line-join":"round"},"paint":{"line-color":"#FFFFFF","line-width":3,"line-opacity":1,"line-emissive-strength":1}}"""), null)
        style.addImage(image("ff-plane-icon", ContextCompat.getDrawable(view.context, R.drawable.ic_notification_plane)!!.toBitmap(80, 80)))
        style.addStyleLayer(json("""{"id":"ff-plane-marker","type":"symbol","source":"ff-plane","slot":"top","layout":{"icon-image":"ff-plane-icon","icon-size":0.35,"icon-allow-overlap":true,"icon-ignore-placement":true,"icon-rotation-alignment":"map"}}"""), null)
        style.addStyleLayer(json("""{"id":"ff-departure-marker","type":"circle","source":"ff-airports","slot":"top","filter":["==",["get","departure"],true],"paint":{"circle-color":"#060C0A","circle-radius":5,"circle-stroke-color":"#D8DFCE","circle-stroke-width":2}}"""), null)
        style.addStyleLayer(json("""{"id":"ff-airport-labels","type":"symbol","source":"ff-airports","slot":"top","layout":{"icon-image":["get","chip"],"icon-size":0.5,"icon-allow-overlap":true,"icon-offset":[0,42]},"paint":{"icon-emissive-strength":1}}"""), null)
        style.addStyleModel("ff-owned-aircraft", "asset://aircraft.glb")
        style.addLayer(modelLayer("ff-aircraft", "ff-plane") {
            modelId("ff-owned-aircraft")
            modelType(ModelType.LOCATION_INDICATOR)
            modelElevationReference(ModelElevationReference.GROUND)
            modelScale(listOf(1.0, 1.0, 1.0))
            modelRotation(listOf(0.0, 0.0, -90.0))
        })
    }

    private fun aircraftPose() {
        val style = loaded ?: return
        val scene = latest ?: return
        val visible = scene.progress != null && !scene.globe && view.mapboxMap.cameraState.zoom >= 6 && view.width > 0 && view.height > 0
        val key=listOf(scene.from,scene.to,scene.progress,view.mapboxMap.cameraState.zoom,view.mapboxMap.cameraState.pitch,view.mapboxMap.cameraState.bearing,view.width,view.height,visible)
        if(poseKey==key) return
        poseKey=key
        style.setStyleLayerProperty("ff-aircraft", "visibility", Value(if (visible) "visible" else "none"))
        style.setStyleLayerProperty("ff-plane-marker", "visibility", Value(if (scene.progress != null && !visible) "visible" else "none"))
        if (!visible) return
        val p = scene.to?.let { FlightRoute.position(scene.from, it, scene.progress ?: 0.0) } ?: scene.from
        val bearing = scene.to?.let { FlightRoute.bearing(p, it) } ?: 0.0
        val offset = Math.toDegrees(34.0 / (6_371_008.8 * cos(Math.toRadians(p.lat)).coerceAtLeast(.01)))
        val a = view.mapboxMap.pixelForCoordinate(Point.fromLngLat(p.lon, p.lat))
        val b = view.mapboxMap.pixelForCoordinate(Point.fromLngLat(p.lon + offset, p.lat))
        val width = hypot(b.x - a.x, b.y - a.y)
        if (!width.isFinite() || width <= .00001) {
            style.setStyleLayerProperty("ff-aircraft", "visibility", Value("none"))
            style.setStyleLayerProperty("ff-plane-marker", "visibility", Value("visible"))
            return
        }
        val markerDp=(38 * 2.0.pow((view.mapboxMap.cameraState.zoom-9.0)*.55)).coerceIn(18.0,280.0)
        val scale = (markerDp * density / width.coerceAtLeast(.001)).coerceIn(.1, 50_000.0)
        style.setStyleLayerProperty("ff-aircraft", "model-scale", Value(listOf(Value(scale), Value(scale), Value(scale))))
        style.setStyleLayerProperty("ff-aircraft", "model-rotation", Value(listOf(Value(0.0), Value(0.0), Value(bearing-90.0))))
        style.setStyleLayerProperty("ff-aircraft", "model-translation", Value(listOf(Value(0.0), Value(0.0), Value(scale * 4.0))))
    }

    private fun airportChip(code: String): Bitmap {
        val bitmap = Bitmap.createBitmap(112, 44, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.rgb(15, 23, 20)
        canvas.drawRoundRect(1f, 1f, 111f, 43f, 6f, 6f, paint)
        paint.color = android.graphics.Color.rgb(244, 222, 83)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f
        canvas.drawRoundRect(2f, 2f, 110f, 42f, 5f, 5f, paint)
        paint.style = Paint.Style.FILL; paint.textSize = 27f
        paint.typeface = Typeface.create("monospace", Typeface.BOLD); paint.textAlign = Paint.Align.CENTER
        canvas.drawText(code, 56f, 31f, paint)
        return bitmap
    }

    fun close() {
        if (closed) return
        closed = true
        view.removeCallbacks(directionAnimation)
        loaded = null
        view.onStop()
        view.onDestroy()
    }
    private fun coordinates(point: GeoPoint) = JSONArray().put(point.lon).put(point.lat)
    private fun geometry(type: String, coords: JSONArray) = JSONObject().put("type", type).put("coordinates", coords)
    private fun geoPoint(point: GeoPoint) = json(JSONObject().put("type", "Feature").put("properties", JSONObject()).put("geometry", geometry("Point", coordinates(point))).toString())
    private fun geoLine(points: List<GeoPoint>): Value {
        val array = JSONArray(); points.forEach { array.put(coordinates(it)) }
        return json(JSONObject().put("type", "Feature").put("properties", JSONObject()).put("geometry", geometry("LineString", array)).toString())
    }
    private fun json(text: String): Value = Value.fromJson(text).value!!
}
