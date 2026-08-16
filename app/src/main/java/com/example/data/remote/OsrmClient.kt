package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.osmdroid.util.GeoPoint
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Road routing. Routes are computed by the Supabase Edge Function `osrm-route`,
 * which proxies to an OSRM instance YOU control (set OSRM_BASE_URL project
 * secret). User origin/destination pairs never leave your infrastructure to a
 * third-party demo server.
 */
object OsrmConfig {
    val BASE_URL: String get() = SupabaseConfig.url
}

@JsonClass(generateAdapter = true)
data class OsrmRouteResponse(
    val distanceKm: Double? = null,
    val durationMin: Int? = null,
    val geometry: OsrmGeometry? = null,
    val error: String? = null
)

interface OsrmApi {
    @Headers("Content-Type: application/json")
    @POST("functions/v1/osrm-route")
    suspend fun route(@Body body: OsrmRouteRequest): Response<OsrmRouteResponse>
}

@JsonClass(generateAdapter = true)
data class OsrmRouteRequest(
    val pickup: List<Double>,   // [lng, lat]
    val dropoff: List<Double>   // [lng, lat]
)

object OsrmClient {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("${OsrmConfig.BASE_URL}/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(
            OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("apikey", SupabaseConfig.anonKey)
                            .addHeader("Authorization", SupabaseClient.authHeader().let {
                                if (it.startsWith("Bearer ")) it else "Bearer $it"
                            })
                            .build()
                    )
                })
                .build()
        )
        .build()

    private val api = retrofit.create(OsrmApi::class.java)

    /**
     * Returns the real road geometry as a list of GeoPoints (lat,lng) from
     * pickup to dropoff, or null if the request fails.
     */
    suspend fun getRoute(pickup: GeoPoint, dropoff: GeoPoint): List<GeoPoint>? {
        return try {
            val resp = api.route(
                OsrmRouteRequest(
                    pickup = listOf(pickup.longitude, pickup.latitude),
                    dropoff = listOf(dropoff.longitude, dropoff.latitude)
                )
            )
            val geom = resp.body()?.geometry?.coordinates ?: return null
            geom.map { c -> GeoPoint(c[1], c[0]) }
        } catch (e: Exception) {
            null
        }
    }

    /** Road distance in km and ETA in minutes, or null on failure. */
    suspend fun getRouteInfo(pickup: GeoPoint, dropoff: GeoPoint): Pair<Double, Int>? {
        return try {
            val resp = api.route(
                OsrmRouteRequest(
                    pickup = listOf(pickup.longitude, pickup.latitude),
                    dropoff = listOf(dropoff.longitude, dropoff.latitude)
                )
            )
            val body = resp.body() ?: return null
            val km = body.distanceKm ?: return null
            val min = body.durationMin ?: return null
            Pair(km, min)
        } catch (e: Exception) {
            null
        }
    }
}
