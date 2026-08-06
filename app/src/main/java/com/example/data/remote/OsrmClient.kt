package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.osmdroid.util.GeoPoint
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Real road routing via the free public OSRM demo server (router.project-osrm.org).
 * No API key, no cost. Returns the actual road geometry between two points so the
 * map draws a real route instead of a straight line.
 *
 * For production you can self-host OSRM or use a free Valhalla/GraphHopper instance;
 * only OSRM_BASE_URL needs to change.
 */
object OsrmConfig {
    const val BASE_URL = "https://router.project-osrm.org/"
}

@JsonClass(generateAdapter = true)
data class OsrmResponse(
    val code: String? = null,
    val routes: List<OsrmRoute>? = null
)

@JsonClass(generateAdapter = true)
data class OsrmRoute(
    val distance: Double? = null,
    val duration: Double? = null,
    val geometry: OsrmGeometry? = null
)

@JsonClass(generateAdapter = true)
data class OsrmGeometry(
    val coordinates: List<List<Double>>? = null // [lng, lat]
)

interface OsrmApi {
    // coords are lng,lat per OSRM spec
    @GET("route/v1/driving/{coords}?overview=full&geometries=geojson")
    suspend fun route(@Path("coords", encoded = true) coords: String): Response<OsrmResponse>
}

object OsrmClient {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(OsrmConfig.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(OsrmApi::class.java)

    /**
     * Returns the real road geometry as a list of GeoPoints (lat,lng) from
     * pickup to dropoff, or null if the request fails.
     */
    suspend fun getRoute(pickup: GeoPoint, dropoff: GeoPoint): List<GeoPoint>? {
        val coords = "${pickup.longitude},${pickup.latitude};${dropoff.longitude},${dropoff.latitude}"
        return try {
            val resp = api.route(coords)
            val route = resp.body()?.routes?.firstOrNull()
            val coordsList = route?.geometry?.coordinates ?: return null
            coordsList.map { c -> GeoPoint(c[1], c[0]) }
        } catch (e: Exception) {
            null
        }
    }

    /** Road distance in km and ETA in minutes, or null on failure. */
    suspend fun getRouteInfo(pickup: GeoPoint, dropoff: GeoPoint): Pair<Double, Int>? {
        val coords = "${pickup.longitude},${pickup.latitude};${dropoff.longitude},${dropoff.latitude}"
        return try {
            val resp = api.route(coords)
            val route = resp.body()?.routes?.firstOrNull() ?: return null
            val km = (route.distance ?: 0.0) / 1000.0
            val min = ((route.duration ?: 0.0) / 60.0).toInt()
            Pair(km, min)
        } catch (e: Exception) {
            null
        }
    }
}
