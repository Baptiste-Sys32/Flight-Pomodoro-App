package personal.focusflight.domain.session

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class FlightRouteTest {
    @Test fun zeroCoordinatesAreValidAndCoincidentRouteIsFinite() {
        val p = GeoPoint(0.0, 0.0)
        assertEquals(0.0, FlightRoute.distance(p, p), .001)
        assertEquals(p, FlightRoute.position(p, p, .5))
    }
    @Test fun dateLineCrossingTakesShortArc() {
        val from = GeoPoint(0.0, 179.0)
        val to = GeoPoint(0.0, -179.0)
        assertEquals(222_390.0, FlightRoute.distance(from, to), 10.0)
        val middle = FlightRoute.position(from, to, .5)
        assertTrue(abs(middle.lon) > 179.9)
        assertEquals(0.0, middle.lat, .001)
    }
    @Test fun antipodalRouteRemainsFiniteAndPreservesEndpoints() {
        val a = GeoPoint(0.0, 0.0)
        val b = GeoPoint(0.0, 180.0)
        val points = FlightRoute.points(a, b)
        assertEquals(a, points.first())
        assertEquals(b, points.last())
        assertTrue(points.all { it.lat.isFinite() && it.lon.isFinite() })
    }
    @Test fun nauticalMilesUseExactInternationalDefinition() {
        assertEquals(1.0, DistanceUnit.NAUTICAL_MILES.value(1852.0), 1e-9)
        assertEquals(1.852, DistanceUnit.KILOMETERS.value(1852.0), 1e-9)
    }
}
