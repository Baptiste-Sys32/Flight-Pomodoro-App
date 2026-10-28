package personal.focusflight.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.graphics.drawable.toBitmap
import com.mapbox.bindgen.Value
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
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

enum class MapMode(val title: String) { MONOCHROME("Monochrome"), TERRA("Terra"), STANDARD("Standard"), SATELLITE("Satellite") }
enum class MapStatus { LOADING, READY, UNAVAILABLE }
data class MapAirport(val code: String, val point: GeoPoint)
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
)

@Composable
fun FlightMap(scene: MapScene, modifier: Modifier = Modifier, onStatus: (MapStatus) -> Unit = {}) {
    if (BuildConfig.MAPBOX_ACCESS_TOKEN.isBlank()) {
        Box(modifier.background(Color(0xFF132422)), contentAlignment = Alignment.Center) { Text("Map unavailable", color = Color.White) }
        return
    }
    val currentCallback by rememberUpdatedState(onStatus)
    AndroidView(
        factory = { context -> MapRenderer(context) { currentCallback(it) }.view },
        modifier = modifier,
        onReset = null,
        onRelease = { (it.tag as? MapRenderer)?.close() },
        update = { (it.tag as MapRenderer).render(scene) },
    )
}

/** Rendering has no authority over elapsed time, completed records or session credit. */
private class MapRenderer(context: Context, private val status: (MapStatus) -> Unit) {
    val view: MapView
    private var latest: MapScene? = null
    private var loaded: Style? = null
    private var requestedMode: MapMode? = null
    private var styleOptions: Triple<Boolean, Boolean, Boolean>? = null
    private var routePair: Pair<GeoPoint, GeoPoint?>? = null
    private var lastCameraKey: List<Any?>? = null
    private var closed = false
    private val density = context.resources.displayMetrics.density

    init {
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        view = MapView(context, MapInitOptions(context, textureView = true)).also { it.tag = this }
        view.compass.updateSettings { enabled = false }
        view.scalebar.updateSettings { enabled = false }
        view.contentDescription = "Flight map loading"
        view.mapboxMap.subscribeMapLoaded {
            if (!closed) {
                aircraftPose()
                view.contentDescription = "Flight map ready"
                status(MapStatus.READY)
            }
        }
        view.mapboxMap.subscribeMapLoadingError { if (!closed) status(MapStatus.UNAVAILABLE) }
        view.mapboxMap.subscribeCameraChanged { if (!closed) aircraftPose() }
        view.doOnLayout { if (!closed) aircraftPose() }
    }

    fun render(scene: MapScene) {
        if (closed) return
        latest = scene
        view.logo.updateSettings { position = Gravity.BOTTOM or Gravity.LEFT; marginLeft = 12f * density; marginBottom = scene.bottomInsetDp * density }
        view.attribution.updateSettings { position = Gravity.BOTTOM or Gravity.LEFT; marginLeft = 92f * density; marginBottom = scene.bottomInsetDp * density }
        if (requestedMode != scene.mode) {
            requestedMode = scene.mode
            loaded = null
            routePair = null
            lastCameraKey = null
            styleOptions = null
            status(MapStatus.LOADING)
            view.contentDescription = "Flight map loading"
            val uri = if (scene.mode == MapMode.SATELLITE) "mapbox://styles/mapbox/standard-satellite" else "mapbox://styles/mapbox/standard"
            view.mapboxMap.loadStyle(uri) { style ->
                if (closed || latest?.mode != scene.mode) return@loadStyle
                loaded = style
                initialize(style)
                latest?.let(::render)
            }
            return
        }
        val style = loaded ?: return
        val options = Triple(scene.labels, scene.terrain, scene.buildings)
        if (options != styleOptions) {
            styleOptions = options
            style.setStyleProjection(json("""{"name":"globe"}"""))
            style.setStyleImportConfigProperty("basemap", "show3dObjects", Value(scene.buildings))
            style.setStyleImportConfigProperty("basemap", "show3dBuildings", Value(scene.buildings))
            for (property in listOf("showPlaceLabels", "showRoadLabels", "showPointOfInterestLabels", "showTransitLabels")) {
                style.setStyleImportConfigProperty("basemap", property, Value(scene.labels))
            }
            if (scene.mode != MapMode.SATELLITE) {
                style.setStyleImportConfigProperty("basemap", "theme", Value(if (scene.mode == MapMode.MONOCHROME || scene.mode == MapMode.TERRA) "monochrome" else "default"))
                style.setStyleImportConfigProperty("basemap", "lightPreset", Value(if (scene.mode == MapMode.TERRA) "night" else "day"))
            }
            style.setStyleTerrain(if (scene.terrain) json("""{"source":"ff-dem","exaggeration":1.2}""") else json("{}"))
        }
        if (routePair != scene.from to scene.to) {
            routePair = scene.from to scene.to
            val points = scene.to?.let { FlightRoute.points(scene.from, it) } ?: listOf(scene.from, scene.from)
            style.setStyleSourceProperty("ff-route", "data", geoLine(points))
        }
        for (airport in scene.airports) {
            if (!style.hasStyleImage("ff-${airport.code}")) style.addImage(image("ff-${airport.code}", airportChip(airport.code)))
        }
        val airports = JSONArray()
        scene.airports.forEach { airport ->
            airports.put(JSONObject().put("type", "Feature").put("geometry", geometry("Point", coordinates(airport.point)))
                .put("properties", JSONObject().put("chip", "ff-${airport.code}")))
        }
        style.setStyleSourceProperty("ff-airports", "data", json(JSONObject().put("type", "FeatureCollection").put("features", airports).toString()))
        val plane = scene.to?.let { FlightRoute.position(scene.from, it, scene.progress ?: 0.0) } ?: scene.from
        style.setStyleSourceProperty("ff-plane", "data", geoPoint(plane))
        val bearing = scene.to?.let { FlightRoute.bearing(plane, it) } ?: 0.0
        style.setStyleLayerProperty("ff-plane-marker", "icon-rotate", Value(bearing))
        style.setStyleLayerProperty("ff-plane-marker", "visibility", Value(if (scene.progress == null) "none" else "visible"))
        val cameraKey = listOf(scene.from, scene.to, scene.follow, scene.globe, scene.zoomOverride)
        if (cameraKey != lastCameraKey || scene.follow) {
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
            view.mapboxMap.setCamera(CameraOptions.Builder().center(Point.fromLngLat(center.lon, center.lat))
                .zoom(zoom).pitch(if (scene.follow || scene.zoomOverride != null) 60.0 else 0.0)
                .bearing(if (scene.follow) bearing else 0.0).build())
        }
        aircraftPose()
    }

    private fun initialize(style: Style) {
        style.addStyleSource("ff-dem", json("""{"type":"raster-dem","url":"mapbox://mapbox.mapbox-terrain-dem-v1","tileSize":512,"maxzoom":14}"""))
        style.addStyleSource("ff-route", json("""{"type":"geojson","data":{"type":"FeatureCollection","features":[]}}"""))
        style.addStyleSource("ff-plane", json("""{"type":"geojson","data":{"type":"FeatureCollection","features":[]}}"""))
        style.addStyleSource("ff-airports", json("""{"type":"geojson","data":{"type":"FeatureCollection","features":[]}}"""))
        style.addStyleLayer(json("""{"id":"ff-route-line","type":"line","source":"ff-route","slot":"top","layout":{"line-cap":"round","line-join":"round"},"paint":{"line-color":"#FFFFFF","line-width":2.4,"line-opacity":0.85}}"""), null)
        style.addImage(image("ff-plane-icon", ContextCompat.getDrawable(view.context, R.drawable.ic_notification_plane)!!.toBitmap(80, 80)))
        style.addStyleLayer(json("""{"id":"ff-plane-marker","type":"symbol","source":"ff-plane","slot":"top","layout":{"icon-image":"ff-plane-icon","icon-size":0.6,"icon-allow-overlap":true,"icon-ignore-placement":true,"icon-rotation-alignment":"map"}}"""), null)
        style.addStyleLayer(json("""{"id":"ff-airport-labels","type":"symbol","source":"ff-airports","slot":"top","layout":{"icon-image":["get","chip"],"icon-size":0.5,"icon-allow-overlap":true,"icon-offset":[0,42]}}"""), null)
        style.addStyleModel("ff-owned-aircraft", "asset://aircraft.glb")
        style.addLayer(modelLayer("ff-aircraft", "ff-plane") {
            modelId("ff-owned-aircraft")
            modelType(ModelType.LOCATION_INDICATOR)
            modelElevationReference(ModelElevationReference.GROUND)
            modelScale(listOf(1.0, 1.0, 1.0))
            modelRotation(listOf(90.0, 0.0, 180.0))
        })
    }

    private fun aircraftPose() {
        val style = loaded ?: return
        val scene = latest ?: return
        val visible = scene.progress != null && !scene.globe && view.mapboxMap.cameraState.zoom >= 6 && view.width > 0 && view.height > 0
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
        val scale = (28 * density / width.coerceAtLeast(.001)).coerceIn(.1, 50_000.0)
        style.setStyleLayerProperty("ff-aircraft", "model-scale", Value(listOf(Value(scale), Value(scale), Value(scale))))
        style.setStyleLayerProperty("ff-aircraft", "model-rotation", Value(listOf(Value(90.0), Value(0.0), Value(bearing + 180.0))))
        style.setStyleLayerProperty("ff-aircraft", "model-translation", Value(listOf(Value(0.0), Value(0.0), Value(scale * 2.2))))
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
