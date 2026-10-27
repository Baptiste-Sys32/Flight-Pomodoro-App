package personal.focusflight.domain.session

import kotlin.math.*

data class GeoPoint(val lat: Double, val lon: Double)

object FlightRoute {
    private const val EARTH_METERS = 6_371_008.8
    fun distance(from: GeoPoint, to: GeoPoint): Double {
        val a = Math.toRadians(from.lat)
        val b = Math.toRadians(to.lat)
        val dLat = b - a
        val dLon = Math.toRadians(to.lon - from.lon)
        val h = sin(dLat / 2).pow(2) + cos(a) * cos(b) * sin(dLon / 2).pow(2)
        return 2 * EARTH_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun position(from: GeoPoint, to: GeoPoint, progress: Double): GeoPoint {
        val t = progress.coerceIn(0.0, 1.0)
        if (t == 0.0) return from
        if (t == 1.0) return to
        val a = vector(from)
        val b = vector(to)
        val dot = a.zip(b).sumOf { it.first * it.second }.coerceIn(-1.0, 1.0)
        val omega = acos(dot)
        if (omega < 1e-8) return from
        val v = if (PI - omega < 1e-6) {
            // Antipodal endpoints have infinitely many routes; choose a stable orthogonal arc.
            val axis = if (abs(a[0]) < .8) doubleArrayOf(1.0, 0.0, 0.0) else doubleArrayOf(0.0, 1.0, 0.0)
            val projection = a.zip(axis).sumOf { it.first * it.second }
            val orthogonal = DoubleArray(3) { axis[it] - a[it] * projection }
            val length = sqrt(orthogonal.sumOf { it * it })
            DoubleArray(3) { a[it] * cos(PI * t) + orthogonal[it] / length * sin(PI * t) }
        } else {
            val denominator = sin(omega)
            val start = sin((1 - t) * omega) / denominator
            val end = sin(t * omega) / denominator
            DoubleArray(3) { a[it] * start + b[it] * end }
        }
        return GeoPoint(Math.toDegrees(atan2(v[2], hypot(v[0], v[1]))), Math.toDegrees(atan2(v[1], v[0])))
    }

    fun bearing(from: GeoPoint, to: GeoPoint): Double {
        val a = Math.toRadians(from.lat)
        val b = Math.toRadians(to.lat)
        val delta = Math.toRadians(to.lon - from.lon)
        return (Math.toDegrees(atan2(sin(delta) * cos(b), cos(a) * sin(b) - sin(a) * cos(b) * cos(delta))) + 360) % 360
    }

    fun points(from: GeoPoint, to: GeoPoint, segments: Int = 128): List<GeoPoint> {
        require(segments > 0)
        return (0..segments).map { position(from, to, it.toDouble() / segments) }
    }

    private fun vector(p: GeoPoint): DoubleArray {
        val lat = Math.toRadians(p.lat)
        val lon = Math.toRadians(p.lon)
        return doubleArrayOf(cos(lat) * cos(lon), cos(lat) * sin(lon), sin(lat))
    }
}

enum class DistanceUnit { KILOMETERS, NAUTICAL_MILES;
    fun value(meters: Double) = meters / if (this == KILOMETERS) 1000.0 else 1852.0
    val label get() = if (this == KILOMETERS) "km" else "nmi"
}
