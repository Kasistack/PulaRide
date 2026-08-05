package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

data class SimulatedDriver(
    val id: String,
    val name: String,
    val baseLat: Float,
    val baseLng: Float,
    val angleSeed: Float,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF2B9BEF)
)

@Composable
fun GaboroneMapView(
    modifier: Modifier = Modifier,
    pickup: com.example.model.LocationItem? = null,
    dropoff: com.example.model.LocationItem? = null,
    activeCarProgress: Float? = null,
    nearbyDrivers: List<SimulatedDriver> = emptyList()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(-24.6565, 25.9119))
                mapView = this
            }
        },
        update = { view ->
            view.overlays.clear()

            nearbyDrivers.forEach { driver ->
                val marker = Marker(view)
                marker.position = GeoPoint(
                    driver.baseLat + 0.01 * kotlin.math.sin(kotlin.math.PI.toFloat() * driver.angleSeed),
                    driver.baseLng + 0.01 * kotlin.math.cos(kotlin.math.PI.toFloat() * driver.angleSeed)
                )
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                view.overlays.add(marker)
            }

            if (pickup != null && dropoff != null) {
                val pickupMarker = Marker(view).apply {
                    position = GeoPoint(pickup.lat, pickup.lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                view.overlays.add(pickupMarker)

                val dropoffMarker = Marker(view).apply {
                    position = GeoPoint(dropoff.lat, dropoff.lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                view.overlays.add(dropoffMarker)

                val route = Polyline(view).apply {
                    setPoints(
                        listOf(
                            GeoPoint(pickup.lat, pickup.lng),
                            GeoPoint((pickup.lat + dropoff.lat) / 2, (pickup.lng + dropoff.lng) / 2),
                            GeoPoint(dropoff.lat, dropoff.lng)
                        )
                    )
                    color = android.graphics.Color.parseColor("#2B9BEF")
                    width = 6f
                }
                view.overlays.add(route)
            }

            // Draw user's real GPS location dot if available
            userLocation?.let { loc ->
                val userMarker = Marker(view).apply {
                    position = loc
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                view.overlays.add(userMarker)
            }
        }
    )

    // Request real GPS location updates
    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val hasFine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            try {
                val lm = locationManager
            lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,   // 1 second min interval
                    5f,      // 5 meters min distance
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            userLocation = GeoPoint(location.latitude, location.longitude)
                            mapView?.controller?.animateTo(GeoPoint(location.latitude, location.longitude))
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                )
                // Also get last known location for quick display
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { last ->
                    userLocation = GeoPoint(last.latitude, last.longitude)
                }
            } catch (e: SecurityException) {
                // Permission not granted at runtime, skip
            }
        }
    }
}
