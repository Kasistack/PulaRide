package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocationItem
import kotlin.math.cos
import kotlin.math.sin

data class SimulatedDriver(
    val id: String,
    val name: String,
    val baseLat: Float,
    val baseLng: Float,
    val angleSeed: Float,
    val color: Color = Color(0xFF2B9BEF) // Botswana Blue
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun GaboroneMapView(
    modifier: Modifier = Modifier,
    pickup: LocationItem? = null,
    dropoff: LocationItem? = null,
    activeCarProgress: Float? = null, // null if no active ride, 0.0 to 1.0 for animation
    nearbyDrivers: List<SimulatedDriver> = emptyList()
) {
    val textMeasurer = rememberTextMeasurer()
    
    // Infinite transition for driver/marker animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    
    val timeSeed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // High-contrast premium dark slate background (fits digital Map style)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Map grid transformation (center is middle of screen)
            val centerX = width / 2f
            val centerY = height / 2f
            
            // Draw grid lines (cyber style)
            val gridSpacing = 80.dp.toPx()
            val gridColor = Color(0xFF1E293B)
            
            var x = 0f
            while (x < width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                x += gridSpacing
            }
            var y = 0f
            while (y < height) {
                drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                y += gridSpacing
            }

            // Define map coordinate mapping helper
            fun mapToPixel(latOffset: Float, lngOffset: Float): Offset {
                // latOffset (-1 to 1) maps to Y, lngOffset (-1 to 1) maps to X
                // Gaborone has Airport in North (Y negative), Dam in South (Y positive), Riverwalk East (X positive)
                val scaleX = width * 0.42f
                val scaleY = height * 0.42f
                return Offset(centerX + lngOffset * scaleX, centerY + latOffset * scaleY)
            }

            // Draw Botswana-centric environmental elements (subtle)
            // 1. Gaborone Dam (South-East, around lat = 0.7, lng = 0.5)
            val damCenter = mapToPixel(0.7f, 0.4f)
            drawCircle(
                color = Color(0xFF003087).copy(alpha = 0.25f), // Botswana Blue shade
                radius = 70.dp.toPx(),
                center = damCenter
            )
            
            // 2. Kgale Hill (South-West, around lat = 0.8, lng = -0.6)
            val kgaleCenter = mapToPixel(0.8f, -0.6f)
            drawCircle(
                color = Color(0xFF334155).copy(alpha = 0.3f), // Slate grey hill
                radius = 45.dp.toPx(),
                center = kgaleCenter
            )

            // Draw Road network lines (Gaborone grid network)
            val roadColor = Color(0xFF334155).copy(alpha = 0.6f)
            val majorRoadColor = Color(0xFF475569)
            val roadWidth = 3.dp.toPx()
            val majorRoadWidth = 5.dp.toPx()

            // Samora Machel Drive (West to East)
            val samoraStart = mapToPixel(0.1f, -1.0f)
            val samoraEnd = mapToPixel(0.1f, 1.0f)
            drawLine(majorRoadColor, samoraStart, samoraEnd, strokeWidth = majorRoadWidth)

            // Independence Ave (North to South through center)
            val indStart = mapToPixel(-1.0f, -0.1f)
            val indEnd = mapToPixel(1.0f, -0.1f)
            drawLine(majorRoadColor, indStart, indEnd, strokeWidth = majorRoadWidth)

            // Khama Crescent (CBD Loop)
            val cbdPath = Path().apply {
                val p1 = mapToPixel(-0.1f, -0.4f)
                val p2 = mapToPixel(-0.1f, -0.2f)
                val p3 = mapToPixel(0.1f, -0.2f)
                val p4 = mapToPixel(0.1f, -0.4f)
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
                close()
            }
            drawPath(cbdPath, roadColor, style = Stroke(width = roadWidth))

            // Nelson Mandela Highway (Western Bypass)
            val mandelaStart = mapToPixel(-1.0f, -0.7f)
            val mandelaEnd = mapToPixel(1.0f, -0.7f)
            drawLine(roadColor, mandelaStart, mandelaEnd, strokeWidth = roadWidth)

            // Tlokweng Road (East to Border)
            val tlokwengStart = mapToPixel(0.2f, 0.0f)
            val tlokwengEnd = mapToPixel(0.2f, 1.0f)
            drawLine(roadColor, tlokwengStart, tlokwengEnd, strokeWidth = roadWidth)

            // Draw Landmark labels (subtle text in Dark theme)
            val landmarkStyle = TextStyle(
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
            val landmarks = listOf(
                Pair("Sir Seretse Khama Airport", mapToPixel(-0.8f, -0.3f)),
                Pair("CBD (Three Dikgosi)", mapToPixel(-0.1f, -0.35f)),
                Pair("Main Mall", mapToPixel(0.05f, -0.05f)),
                Pair("University of Botswana", mapToPixel(0.15f, 0.25f)),
                Pair("Riverwalk Mall", mapToPixel(0.35f, 0.55f)),
                Pair("Airport Junction", mapToPixel(-0.45f, -0.1f)),
                Pair("Kgale Hill", mapToPixel(0.85f, -0.6f)),
                Pair("Gaborone Dam", mapToPixel(0.7f, 0.4f))
            )
            
            landmarks.forEach { (name, pos) ->
                // Draw a small dot
                drawCircle(color = Color(0xFF64748B), radius = 3.dp.toPx(), center = pos)
                // Draw text
                drawText(
                    textMeasurer = textMeasurer,
                    text = name,
                    topLeft = Offset(pos.x + 6.dp.toPx(), pos.y - 6.dp.toPx()),
                    style = landmarkStyle
                )
            }

            // Draw simulated nearby taxis driving
            nearbyDrivers.forEach { driver ->
                // Simulate slow micro-movement
                val animatedLat = driver.baseLat + 0.04f * sin(timeSeed + driver.angleSeed)
                val animatedLng = driver.baseLng + 0.04f * cos(timeSeed + driver.angleSeed)
                val pixelPos = mapToPixel(animatedLat, animatedLng)
                
                // Draw trailing path outline
                drawCircle(
                    color = driver.color.copy(alpha = 0.15f),
                    radius = 12.dp.toPx(),
                    center = pixelPos
                )
                
                // Draw actual car marker (triangle / pointer)
                val carPath = Path().apply {
                    val sizePx = 6.dp.toPx()
                    val dirAngle = timeSeed + driver.angleSeed
                    val p1 = Offset(
                        pixelPos.x + sizePx * cos(dirAngle),
                        pixelPos.y + sizePx * sin(dirAngle)
                    )
                    val p2 = Offset(
                        pixelPos.x + sizePx * cos(dirAngle + 2.5f),
                        pixelPos.y + sizePx * sin(dirAngle + 2.5f)
                    )
                    val p3 = Offset(
                        pixelPos.x + sizePx * cos(dirAngle - 2.5f),
                        pixelPos.y + sizePx * sin(dirAngle - 2.5f)
                    )
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    close()
                }
                drawPath(carPath, color = driver.color)
            }

            // Render routing path and active ride markers if available
            if (pickup != null && dropoff != null) {
                val pickupPos = mapToPixel(pickup.latOffset, pickup.lngOffset)
                val dropoffPos = mapToPixel(dropoff.latOffset, dropoff.lngOffset)

                // Draw Route: Beautiful bezier/curved path in light blue
                val routePath = Path().apply {
                    moveTo(pickupPos.x, pickupPos.y)
                    // Control point to curve the path elegantly
                    val ctrlX = (pickupPos.x + dropoffPos.x) / 2f - 60f
                    val ctrlY = (pickupPos.y + dropoffPos.y) / 2f - 120f
                    quadraticTo(ctrlX, ctrlY, dropoffPos.x, dropoffPos.y)
                }

                // Draw background glow for route
                drawPath(
                    path = routePath,
                    color = Color(0xFF003087).copy(alpha = 0.3f),
                    style = Stroke(width = 6.dp.toPx())
                )

                // Draw dashed path for active route
                drawPath(
                    path = routePath,
                    color = Color(0xFF2B9BEF), // Active Botswana Blue
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(20f, 15f),
                            phase = -timeSeed * 30f
                        )
                    )
                )

                // Render active car riding along route if activeCarProgress is provided
                if (activeCarProgress != null) {
                    // Approximate linear interpolation on the quadratic bezier curve
                    val progress = activeCarProgress.coerceIn(0.0f, 1.0f)
                    val ctrlX = (pickupPos.x + dropoffPos.x) / 2f - 60f
                    val ctrlY = (pickupPos.y + dropoffPos.y) / 2f - 120f
                    
                    // Bezier formula: B(t) = (1-t)^2 * P0 + 2(1-t)t * P1 + t^2 * P2
                    val t = progress
                    val mt = 1f - t
                    val carX = mt * mt * pickupPos.x + 2f * mt * t * ctrlX + t * t * dropoffPos.x
                    val carY = mt * mt * pickupPos.y + 2f * mt * t * ctrlY + t * t * dropoffPos.y
                    val carPos = Offset(carX, carY)

                    // Draw car pulse
                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = 0.2f), // Gold highlight pulse
                        radius = pulseScale * 2f,
                        center = carPos
                    )

                    // Draw ride car symbol (Yellow/Gold highlighted circle with inner black vehicle dots)
                    drawCircle(
                        color = Color(0xFFFBBF24), // Gold highlight for active cab
                        radius = 8.dp.toPx(),
                        center = carPos
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 4.dp.toPx(),
                        center = carPos
                    )
                    
                    // Draw mini label "Your Cab"
                    val labelStyle = TextStyle(
                        color = Color(0xFFFBBF24),
                        fontSize = 10.sp
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "Cab (ETA 3m)",
                        topLeft = Offset(carPos.x - 30.dp.toPx(), carPos.y - 22.dp.toPx()),
                        style = labelStyle
                    )
                }

                // Draw Pickup pin (Green pulse & Circle)
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.3f),
                    radius = pulseScale,
                    center = pickupPos
                )
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 6.dp.toPx(),
                    center = pickupPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = pickupPos
                )

                // Draw Drop-off pin (Red / Pink circle)
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 6.dp.toPx(),
                    center = dropoffPos
                )
                // Draw inner white square for drop-off marker
                drawRect(
                    color = Color.White,
                    topLeft = Offset(dropoffPos.x - 2.dp.toPx(), dropoffPos.y - 2.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}
