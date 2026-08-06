package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.*
import com.example.ui.components.PalapyeMapView
import com.example.viewmodel.RideViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulaRideApp(viewModel: RideViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDriverMode by viewModel.isDriverMode.collectAsState()
    val language by viewModel.language.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Slate-900 background
    ) {
        Crossfade(targetState = currentScreen, label = "screen_navigation") { screen ->
            when (screen) {
                "SPLASH" -> SplashScreen()
                "LOGIN" -> LoginScreen(viewModel)
                "HOME" -> {
                    if (isDriverMode) {
                        DriverDashboardScreen(viewModel)
                    } else {
                        HomeScreen(viewModel)
                    }
                }
                "RIDE_BIDDING" -> BiddingScreen(viewModel)
                "IN_RIDE" -> InRideScreen(viewModel)
                "PROFILE" -> ProfileHistoryScreen(viewModel)
                "PAYMENTS" -> PaymentsScreen(viewModel)
                else -> HomeScreen(viewModel)
            }
        }
    }
}

// 1. SPLASH SCREEN (Botswana Pride Intro)
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo Icon styled with Botswana Flag colors (Light Blue, White, Black)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .background(Color.White, CircleShape)
                .padding(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF003087), CircleShape) // Botswana Sky Blue / Dark Flag Blue
            ) {
                // Outer circle black/white ribbon
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "PulaRide Logo",
                    tint = Color.White,
                    modifier = Modifier.size(70.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PulaRide",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Setswana Welcoming greeting
        Text(
            text = "Re a go amogela • Welcome",
            color = Color(0xFF2B9BEF), // Botswana Blue
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(48.dp))

        CircularProgressIndicator(
            color = Color(0xFF2B9BEF),
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp)
        )
    }
}

// 2. LOGIN SCREEN (Phone OTP setup)
@Composable
fun LoginScreen(viewModel: RideViewModel) {
    val phone by viewModel.loginPhone.collectAsState()
    val otp by viewModel.otpCode.collectAsState()
    val isOtpSent by viewModel.isOtpSent.collectAsState()
    val lang by viewModel.language.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Language selector at Login Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                tint = Color(0xFF2B9BEF),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Puo / Lang: ",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Text(
                text = if (lang == AppLanguage.ENGLISH) "Setswana" else "English",
                color = Color(0xFF2B9BEF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        viewModel.setLanguage(
                            if (lang == AppLanguage.ENGLISH) AppLanguage.SETSWANA else AppLanguage.ENGLISH
                        )
                    }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // App Logo
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = "Logo",
            tint = Color(0xFF2B9BEF),
            modifier = Modifier.size(64.dp)
        )

        Text(
            text = "PulaRide",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = SetswanaTranslation.translate("app_tagline", lang),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Login Card Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = SetswanaTranslation.translate("welcome", lang),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = SetswanaTranslation.translate("login_sub", lang),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isOtpSent) {
                    // Phone entry + instant anonymous sign-in (no SMS/OTP code needed)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { viewModel.setLoginPhone(it) },
                        label = { Text("Phone Number / Mogala (+267)") },
                        leadingIcon = {
                            Row(
                                modifier = Modifier.padding(start = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🇧🇼", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+267", color = Color.White, fontSize = 14.sp)
                                VerticalDivider(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .padding(horizontal = 8.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2B9BEF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFF2B9BEF),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_phone_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.sendOtp()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B9BEF)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = phone.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_otp_button")
                    ) {
                        Text(
                            text = SetswanaTranslation.translate("send_otp", lang),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // Signed in (anonymous) — waiting for home transition
                    Text(
                        text = "Re a go amogela... (Signing you in)",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// 3. RIDER HOME SCREEN (Search route + bidding configurations)
@Composable
fun HomeScreen(viewModel: RideViewModel) {
    val lang by viewModel.language.collectAsState()
    val pickup by viewModel.pickup.collectAsState()
    val dropoff by viewModel.dropoff.collectAsState()
    val pickupQuery by viewModel.pickupQuery.collectAsState()
    val dropoffQuery by viewModel.dropoffQuery.collectAsState()
    val userOfferedFare by viewModel.userOfferedFare.collectAsState()
    val suggestedFare by viewModel.suggestedFare.collectAsState()
    val paymentMode by viewModel.paymentMode.collectAsState()
    val lowData by viewModel.lowDataMode.collectAsState()
    val womenOnly by viewModel.womenOnlyDriverFilter.collectAsState()
    val preferredAc by viewModel.preferredAcState.collectAsState()

    var activeSearchFocus by remember { mutableStateOf("NONE") } // "PICKUP", "DROPOFF", "NONE"

    val nearbyDrivers by viewModel.mapDrivers.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        // Embed the beautiful Palapye Canvas map!
        PalapyeMapView(
            pickup = pickup,
            dropoff = dropoff,
            nearbyDrivers = if (lowData) emptyList() else nearbyDrivers
        )

        // Top Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Floating mode switch
                Button(
                    onClick = { viewModel.toggleDriverMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("driver_mode_switch")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch Mode",
                        tint = Color(0xFFFFC72C), // Gold
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = SetswanaTranslation.translate("driver_mode", lang),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick language selector / settings
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == AppLanguage.ENGLISH) "EN" else "SETS",
                        color = Color(0xFF2B9BEF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                viewModel.setLanguage(
                                    if (lang == AppLanguage.ENGLISH) AppLanguage.SETSWANA else AppLanguage.ENGLISH
                                )
                            }
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (lowData) Icons.Default.SignalCellularAlt2Bar else Icons.Default.SignalCellular4Bar,
                        contentDescription = "Data status",
                        tint = if (lowData) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { viewModel.toggleLowDataMode() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Low Data Mode Banner if active
            if (lowData) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Low-Data Mode active: map details simplified for offline support.",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom Operations card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Profile/History and Payments shortcut buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.setScreen("PROFILE") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.History, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("History / Diloeto", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.setScreen("PAYMENTS") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Payment, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(paymentMode.displayName, fontSize = 11.sp, color = Color.White)
                        }
                    }

                    // Route Inputs (Pickup and Dropoff)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        // Pickup row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Pickup",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedTextField(
                                value = pickupQuery,
                                onValueChange = {
                                    viewModel.setPickupQuery(it)
                                    activeSearchFocus = "PICKUP"
                                },
                                placeholder = { Text(SetswanaTranslation.translate("search_pickup", lang)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pickup_address_input")
                            )
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Dropoff row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Dropoff",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedTextField(
                                value = dropoffQuery,
                                onValueChange = {
                                    viewModel.setDropoffQuery(it)
                                    activeSearchFocus = "DROPOFF"
                                },
                                placeholder = { Text(SetswanaTranslation.translate("search_dropoff", lang)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dropoff_address_input")
                            )
                        }
                    }

                    // Autocomplete drop list if search field active
                    if (activeSearchFocus != "NONE") {
                        val activeQuery = if (activeSearchFocus == "PICKUP") pickupQuery else dropoffQuery
                        val filteredList = viewModel.locationsInGabs.filter {
                            it.name.contains(activeQuery, ignoreCase = true) ||
                                    it.setsName.contains(activeQuery, ignoreCase = true)
                        }

                        if (filteredList.isNotEmpty()) {
                            Text(
                                text = SetswanaTranslation.translate("popular_places", lang) + ":",
                                color = Color(0xFF2B9BEF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            ) {
                                items(filteredList) { location ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (activeSearchFocus == "PICKUP") {
                                                    viewModel.selectPickup(location)
                                                } else {
                                                    viewModel.selectDropoff(location)
                                                }
                                                activeSearchFocus = "NONE"
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (lang == AppLanguage.SETSWANA) location.setsName else location.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = location.landmarkName,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // BIDDING OPTIONS (Revealed only when pickup and dropoff are locked!)
                    if (pickup != null && dropoff != null && activeSearchFocus == "NONE") {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = SetswanaTranslation.translate("women_safety", lang),
                            color = if (womenOnly) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { viewModel.toggleWomenOnlyDriver() }
                                .background(
                                    if (womenOnly) Color(0xFFFBBF24).copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(6.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Heat / Cooling air conditioner preference
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AC preference / Tikologo: ",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF334155), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                listOf("AC Gentle", "Heater On", "No AC").forEach { pref ->
                                    val isSel = preferredAc == pref
                                    Text(
                                        text = pref,
                                        color = if (isSel) Color.White else Color.White.copy(alpha = 0.4f),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .background(
                                                if (isSel) Color(0xFF2B9BEF) else Color.Transparent,
                                                RoundedCornerShape(50.dp)
                                            )
                                            .clickable { viewModel.setPreferredAcState(pref) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fare input row (bidding system)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = SetswanaTranslation.translate("bid_fare", lang),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = {
                                            val current = userOfferedFare.toDoubleOrNull() ?: suggestedFare
                                            if (current > 30) {
                                                viewModel.setUserOfferedFare((current - 5).toInt().toString())
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Remove, "Less", tint = Color(0xFF2B9BEF))
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = userOfferedFare,
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "P",
                                            color = Color(0xFFFFC72C), // Pula Gold
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val current = userOfferedFare.toDoubleOrNull() ?: suggestedFare
                                            viewModel.setUserOfferedFare((current + 5).toInt().toString())
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, "More", tint = Color(0xFF2B9BEF))
                                    }
                                }

                                Text(
                                    text = "Suggested Standard: ${suggestedFare.toInt()} Pula",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Primary action: Batla Koloi
                        Button(
                            onClick = { viewModel.requestRide() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B9BEF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("request_ride_button")
                        ) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = SetswanaTranslation.translate("request_ride", lang),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// 4. BIDDING SCREEN (Waiting on driver counters)
@Composable
fun BiddingScreen(viewModel: RideViewModel) {
    val lang by viewModel.language.collectAsState()
    val pickup by viewModel.pickup.collectAsState()
    val dropoff by viewModel.dropoff.collectAsState()
    val userOfferedFare by viewModel.userOfferedFare.collectAsState()
    val biddingPhase by viewModel.biddingPhase.collectAsState()
    val driverOffers by viewModel.driverOffers.collectAsState()

    var showExplanation by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        PalapyeMapView(
            pickup = pickup,
            dropoff = dropoff
        )

        // Overlay Header with close/cancel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.cancelRide() },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    .testTag("cancel_bidding_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            IconButton(
                onClick = { showExplanation = true },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(Icons.Default.Info, "How it works", tint = Color(0xFFFFC72C))
            }
        }

        // Bidding explanations dialog
        if (showExplanation) {
            AlertDialog(
                onDismissRequest = { showExplanation = false },
                title = { Text(SetswanaTranslation.translate("how_does_it_work", lang)) },
                text = { Text(SetswanaTranslation.translate("how_work_desc", lang)) },
                confirmButton = {
                    TextButton(onClick = { showExplanation = false }) {
                        Text("Dumela / OK")
                    }
                }
            )
        }

        // Bottom sheet displaying drivers responding
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Offer: $userOfferedFare Pula",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gaborone Local Bidding",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }

                        CircularProgressIndicator(
                            color = Color(0xFF2B9BEF),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (biddingPhase == "SUBMITTED" || driverOffers.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2B9BEF))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = SetswanaTranslation.translate("searching_drivers", lang),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = SetswanaTranslation.translate("driver_offers", lang),
                            color = Color(0xFF2B9BEF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 10.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(driverOffers) { offer ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Driver Avatar & specs
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Mock avatar visual
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .background(Color(0xFF334155), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = offer.name,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        null,
                                                        tint = Color(0xFFFFC72C),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = " ${offer.rating} • ${offer.etaMinutes}m away",
                                                        color = Color.White.copy(alpha = 0.5f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Text(
                                                    text = "${offer.vehicleModel} (${offer.vehiclePlate})",
                                                    color = Color(0xFF2B9BEF),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = offer.heatPreference,
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        // Bid decision
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = offer.offeredBid.toInt().toString(),
                                                    color = Color.White,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = " P",
                                                    color = Color(0xFFFFC72C),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Button(
                                                onClick = { viewModel.acceptDriverOffer(offer) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF10B981)
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("accept_driver_bid_button_${offer.driverId}")
                                            ) {
                                                Text("Accept", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { viewModel.cancelRide() },
                        colors = ButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White.copy(alpha = 0.6f),
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = Color.Transparent
                        )
                    ) {
                        Text("Cancel Request / Sola Kopo")
                    }
                }
            }
        }
    }
}

// 5. IN-RIDE TRACKING SCREEN (Safety SOS + Chat masked dispatch)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InRideScreen(viewModel: RideViewModel) {
    val lang by viewModel.language.collectAsState()
    val activeRide by viewModel.activeRide.collectAsState()
    val progress by viewModel.rideProgress.collectAsState()
    val emergencyTriggered by viewModel.emergencyTriggered.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var typingMessage by remember { mutableStateOf("") }
    var showChatSheet by remember { mutableStateOf(false) }

    if (activeRide == null) return

    Box(modifier = Modifier.fillMaxSize()) {
        PalapyeMapView(
            pickup = activeRide?.pickup,
            dropoff = activeRide?.dropoff
        )

        // Safety Quick floating emergency triggers
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopEnd)
        ) {
            Button(
                onClick = { viewModel.triggerEmergency() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("safety_sos_trigger_button")
            ) {
                Icon(Icons.Default.Warning, null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(SetswanaTranslation.translate("emergency", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { /* Simulated sharing link copying */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Trip / Arogana", fontSize = 10.sp)
            }
        }

        // Emergency Dialog popup
        if (emergencyTriggered) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissEmergency() },
                icon = { Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(48.dp)) },
                title = { Text("EMERGENCY HELP REQUESTED") },
                text = {
                    Text(
                        "PulaRide Emergency Services are dispatching to your live location in Gaborone immediately. " +
                                "\n\nLocal Botswana Police Hotline: 999\nLocal Medical Service: 997\nYour vehicle details have been shared with local authorities."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissEmergency() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("I AM SAFE NOW / KE O KAE JE")
                    }
                }
            )
        }

        // In-App Chat overlay Drawer panel
        if (showChatSheet) {
            AlertDialog(
                onDismissRequest = { showChatSheet = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, null, tint = Color(0xFF2B9BEF))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Chat: ${activeRide?.selectedDriver?.name}", fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(chatMessages) { chat ->
                                val isRider = chat.first == "Rider" || chat.first == "System"
                                val align = if (isRider) Alignment.End else Alignment.Start
                                val bubbleColor = if (isRider) Color(0xFF2B9BEF) else Color(0xFF334155)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = align
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(bubbleColor, RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(chat.second, color = Color.White, fontSize = 13.sp)
                                    }
                                    Text(
                                        chat.first,
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = typingMessage,
                                onValueChange = { typingMessage = it },
                                placeholder = { Text("Eba molaetsa koo...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    viewModel.sendChatMessage(typingMessage)
                                    typingMessage = ""
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color(0xFF2B9BEF))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChatSheet = false }) {
                        Text("Close / Tswala")
                    }
                }
            )
        }

        // Bottom Ride Progress details card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Status tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                null,
                                tint = Color(0xFF2B9BEF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeRide?.status == "EN_ROUTE_PICKUP") "Driver Arriving / O a tla" else "Trip Active / Re tseleng",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${(progress * 100).toInt()}% Done",
                            color = Color(0xFF2B9BEF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        color = Color(0xFF2B9BEF),
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50.dp))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Driver metadata row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF334155), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = activeRide?.selectedDriver?.name ?: "",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${activeRide?.selectedDriver?.vehicleModel} • ${activeRide?.selectedDriver?.vehiclePlate}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeRide?.finalFare?.toInt().toString(),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = " P",
                                color = Color(0xFFFFC72C),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action panels: Chat and Masked Call
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showChatSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Chat, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(SetswanaTranslation.translate("chat_driver", lang), fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = { /* Masked telephony dial simulator */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(SetswanaTranslation.translate("call_driver", lang), fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// 6. PROFILE & PERSISTENT HISTORY SCREEN
@Composable
fun ProfileHistoryScreen(viewModel: RideViewModel) {
    val lang by viewModel.language.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val rideHistory by viewModel.rideHistory.collectAsState()
    val scope = rememberCoroutineScope()

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.setScreen("HOME") },
                modifier = Modifier
                    .background(Color(0xFF1E293B), CircleShape)
                    .testTag("profile_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            Text(
                text = "PulaRide Profile",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Blank spacer for alignment
            Box(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile details Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFF2B9BEF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.name.firstOrNull()?.toString() ?: "U",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile.phone,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = profile.email,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Promo/Referrals panel
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFC72C).copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Referral Discount / Dikopo tse di Gaisang",
                    color = Color(0xFFFFC72C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share code ${profile.referralCode} with friends to earn 15 Pula free credit!",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Room History section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SetswanaTranslation.translate("history", lang),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            if (rideHistory.isNotEmpty()) {
                Text(
                    text = "Clear All / Sola Tsotlhe",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { scope.launch { viewModel.clearHistory() } }
                        .padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (rideHistory.isEmpty()) {
            // Empty State
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.LocalActivity,
                    contentDescription = "No rides",
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No rides completed yet.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
                Text(
                    text = "Your ride logs in Gaborone will show up here offline.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rideHistory) { entity ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dateFormatter.format(Date(entity.timestamp)),
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 10.sp
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${entity.fare.toInt()}",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = " P",
                                        color = Color(0xFFFFC72C),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Pickup
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(entity.pickup, color = Color.White, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Dropoff
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(entity.dropoff, color = Color.White, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Driver: ${entity.driverName} (${entity.driverVehicle})",
                                    color = Color(0xFF2B9BEF),
                                    fontSize = 11.sp
                                )

                                Row {
                                    repeat(5) {
                                        Icon(
                                            Icons.Default.Star,
                                            null,
                                            tint = Color(0xFFFFC72C),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7. PAYMENTS SCREEN (Orange Money / MyZaka and local wallets)
@Composable
fun PaymentsScreen(viewModel: RideViewModel) {
    val lang by viewModel.language.collectAsState()
    val activePayment by viewModel.paymentMode.collectAsState()
    val profile by viewModel.profile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setScreen("HOME") },
                modifier = Modifier
                    .background(Color(0xFF1E293B), CircleShape)
                    .testTag("payments_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = SetswanaTranslation.translate("payments", lang),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Wallet Balance Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B9BEF)), // Botswana Blue theme
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "PulaRide Digital Wallet",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = profile.balance.toInt().toString(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BWP (Pula)",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { /* Top up wallet dialog */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Top Up / Tsenya Madi", color = Color(0xFF003087), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Select Payment Mode / Duela ka:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Display Botswana Local Payment methods
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(PaymentMode.values()) { mode ->
                val isSelected = activePayment == mode
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setPaymentMode(mode) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // High contrast representation of local wallets
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        when (mode) {
                                            PaymentMode.ORANGE_MONEY -> Color(0xFFFF6600) // Orange Money
                                            PaymentMode.MY_ZAKA -> Color(0xFFFFCC00) // Mascom Yellow
                                            PaymentMode.CASH -> Color(0xFF10B981) // Green Cash
                                            else -> Color(0xFF2B9BEF)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        PaymentMode.CARD -> Icons.Default.CreditCard
                                        PaymentMode.CASH -> Icons.Default.AttachMoney
                                        else -> Icons.Default.AccountBalanceWallet
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = mode.displayName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mode.providerName,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 8. DRIVER MODE DASHBOARD SCREEN (online/offline driver app simulation)
@Composable
fun DriverDashboardScreen(viewModel: RideViewModel) {
    val lang by viewModel.language.collectAsState()
    val isOnline by viewModel.driverOnline.collectAsState()
    val earnings by viewModel.driverEarnings.collectAsState()
    val pdpUploaded by viewModel.pdpUploaded.collectAsState()
    val licenseUploaded by viewModel.licenseUploaded.collectAsState()
    val activeRequest by viewModel.activeDriverRequest.collectAsState()

    val complianceStatus by viewModel.complianceStatus.collectAsState()
    val complianceWarningTriggered by viewModel.complianceWarningTriggered.collectAsState()
    val showComplianceDashboard by viewModel.showComplianceDashboard.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Driver header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Floating Switch to Rider mode
            Button(
                onClick = { viewModel.toggleDriverMode() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.testTag("rider_mode_switch")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Rider mode",
                    tint = Color(0xFF2B9BEF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(SetswanaTranslation.translate("rider_mode", lang), fontSize = 11.sp, color = Color.White)
            }

            // Online / Offline slide switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(50.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isOnline) SetswanaTranslation.translate("online", lang) else SetswanaTranslation.translate("offline", lang),
                    color = if (isOnline) Color(0xFF10B981) else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isOnline,
                    onCheckedChange = { viewModel.setDriverOnline(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF10B981),
                        checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("driver_online_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Earnings Display Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = SetswanaTranslation.translate("earnings", lang),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = earnings.toInt().toString(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "P (BWP)",
                        color = Color(0xFFFFC72C),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Low Commission", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        Text("Only 12%", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { /* payout trigger */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B9BEF)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Request Payout", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Compliance & Verification Status checklist section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SetswanaTranslation.translate("verification", lang),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Current overall compliance status badge
            Box(
                modifier = Modifier
                    .background(
                        color = when (complianceStatus) {
                            "COMPLIANT" -> Color(0xFF10B981).copy(alpha = 0.15f)
                            "PENDING_REVIEW" -> Color(0xFFFFC72C).copy(alpha = 0.15f)
                            else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = complianceStatus.replace("_", " "),
                    color = when (complianceStatus) {
                        "COMPLIANT" -> Color(0xFF10B981)
                        "PENDING_REVIEW" -> Color(0xFFFFC72C)
                        else -> Color(0xFFEF4444)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Compliance Dashboard / Ditaelo",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Drivers must submit a Public Driving Permit (PrDP), Driver's License, and Vehicle Inspection Certificate for verification before going online.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Button(
                    onClick = { viewModel.setShowComplianceDashboard(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B9BEF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("open_compliance_button")
                ) {
                    Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Access Compliance Dashboard", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // Compliance Warning Dialog
        if (complianceWarningTriggered) {
            AlertDialog(
                onDismissRequest = { viewModel.setComplianceWarningTriggered(false) },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compliance Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Text(
                        text = "You must upload and verify all compliance permit documents (PrDP, Driver's License, and Vehicle Inspection) before you can start accepting rides on PulaRide.\n\nPlease access the Compliance Dashboard to complete this process.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setComplianceWarningTriggered(false)
                            viewModel.setShowComplianceDashboard(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B9BEF))
                    ) {
                        Text("Open Dashboard", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.setComplianceWarningTriggered(false) }
                    ) {
                        Text("Cancel / Tswala", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Show Compliance Dashboard
        if (showComplianceDashboard) {
            ComplianceDashboardDialog(viewModel)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // INCOMING ACTIVE RIDE REQUEST (REVEALED WHEN ONLINE)
        if (isOnline) {
            Text(
                text = "Live Dispatch Requests",
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (activeRequest == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Waiting for Gaborone dispatch requests...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val req = activeRequest ?: return
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Rider: ${req["riderName"]} (${req["riderRating"]}★)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = req["offeredFare"] ?: "85",
                                        color = Color(0xFF10B981),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = " P",
                                        color = Color(0xFFFFC72C),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Route details
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(req["pickup"] ?: "", color = Color.White, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(req["dropoff"] ?: "", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Bid Action panel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.driverDeclineRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("driver_decline_ride_button")
                            ) {
                                Text(SetswanaTranslation.translate("decline", lang), color = Color.White)
                            }

                            Button(
                                onClick = { viewModel.driverAcceptRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("driver_accept_ride_button")
                            ) {
                                Text(SetswanaTranslation.translate("accept", lang), color = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            // Offline display state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Offline",
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You are currently offline.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Text(
                    text = "Go online to start receiving ride offers in Gaborone.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceDashboardDialog(viewModel: RideViewModel) {
    val pdpNumber by viewModel.pdpNumber.collectAsState()
    val pdpExpiry by viewModel.pdpExpiry.collectAsState()
    val pdpStatus by viewModel.pdpStatus.collectAsState()
    val pdpImageName by viewModel.pdpImageName.collectAsState()

    val licenseNumber by viewModel.licenseNumber.collectAsState()
    val licenseExpiry by viewModel.licenseExpiry.collectAsState()
    val licenseStatus by viewModel.licenseStatus.collectAsState()
    val licenseImageName by viewModel.licenseImageName.collectAsState()

    val roadworthinessNumber by viewModel.roadworthinessNumber.collectAsState()
    val roadworthinessExpiry by viewModel.roadworthinessExpiry.collectAsState()
    val roadworthinessStatus by viewModel.roadworthinessStatus.collectAsState()
    val roadworthinessImageName by viewModel.roadworthinessImageName.collectAsState()

    val complianceStatus by viewModel.complianceStatus.collectAsState()
    val complianceRejectionReason by viewModel.complianceRejectionReason.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.setShowComplianceDashboard(false) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF2B9BEF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Compliance Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Header details
                item {
                    Text(
                        text = "Lekala la Ditaelo tsa Bakhweetsi",
                        color = Color(0xFFFFC72C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Please enter your document numbers, expiries, and upload copies. Your documents must be verified before going online.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Overall status alert banner
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (complianceStatus) {
                                "COMPLIANT" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                "PENDING_REVIEW" -> Color(0xFFFFC72C).copy(alpha = 0.15f)
                                else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (complianceStatus) {
                                    "COMPLIANT" -> Icons.Default.CheckCircle
                                    "PENDING_REVIEW" -> Icons.Default.HourglassEmpty
                                    else -> Icons.Default.Error
                                },
                                contentDescription = null,
                                tint = when (complianceStatus) {
                                    "COMPLIANT" -> Color(0xFF10B981)
                                    "PENDING_REVIEW" -> Color(0xFFFFC72C)
                                    else -> Color(0xFFEF4444)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Status: ${complianceStatus.replace("_", " ")}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (complianceRejectionReason.isNotBlank() && complianceStatus == "NOT_COMPLIANT") {
                                    Text(
                                        text = "Reason: $complianceRejectionReason",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. PrDP Section
                item {
                    DocumentFormSection(
                        title = "1. Public Driving Permit (PrDP)",
                        subLabel = "Required for transporting passengers in Botswana",
                        number = pdpNumber,
                        onNumberChange = { num -> viewModel.updatePdpDetails(num, pdpExpiry, pdpImageName.ifBlank { "prdp_copy.png" }) },
                        expiry = pdpExpiry,
                        onExpiryChange = { exp -> viewModel.updatePdpDetails(pdpNumber, exp, pdpImageName.ifBlank { "prdp_copy.png" }) },
                        status = pdpStatus,
                        imageName = pdpImageName,
                        onUploadMock = {
                            viewModel.updatePdpDetails(
                                pdpNumber.ifBlank { "PRDP-BW-9921" },
                                pdpExpiry.ifBlank { "12-11-2028" },
                                "prdp_permit_scanned.png"
                            )
                        }
                    )
                }

                // 2. Driver's License Section
                item {
                    DocumentFormSection(
                        title = "2. Botswana Driver's License",
                        subLabel = "National class B or above licence card",
                        number = licenseNumber,
                        onNumberChange = { num -> viewModel.updateLicenseDetails(num, licenseExpiry, licenseImageName.ifBlank { "license_copy.png" }) },
                        expiry = licenseExpiry,
                        onExpiryChange = { exp -> viewModel.updateLicenseDetails(licenseNumber, exp, licenseImageName.ifBlank { "license_copy.png" }) },
                        status = licenseStatus,
                        imageName = licenseImageName,
                        onUploadMock = {
                            viewModel.updateLicenseDetails(
                                licenseNumber.ifBlank { "DL-GABS-77221" },
                                licenseExpiry.ifBlank { "30-06-2030" },
                                "license_scanned.png"
                            )
                        }
                    )
                }

                // 3. Roadworthiness Certificate Section
                item {
                    DocumentFormSection(
                        title = "3. Vehicle Roadworthiness / Inspection",
                        subLabel = "Certified vehicle safety inspection certificate",
                        number = roadworthinessNumber,
                        onNumberChange = { num -> viewModel.updateRoadworthinessDetails(num, roadworthinessExpiry, roadworthinessImageName.ifBlank { "roadworthy_copy.png" }) },
                        expiry = roadworthinessExpiry,
                        onExpiryChange = { exp -> viewModel.updateRoadworthinessDetails(roadworthinessNumber, exp, roadworthinessImageName.ifBlank { "roadworthy_copy.png" }) },
                        status = roadworthinessStatus,
                        imageName = roadworthinessImageName,
                        onUploadMock = {
                            viewModel.updateRoadworthinessDetails(
                                roadworthinessNumber.ifBlank { "RW-CERT-4412" },
                                roadworthinessExpiry.ifBlank { "15-02-2027" },
                                "roadworthy_scanned.png"
                            )
                        }
                    )
                }

                // SIMULATOR CONTROLS
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF334155).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFFFC72C), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VERIFICATION SIMULATION SANDBOX",
                                    color = Color(0xFFFFC72C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Simulate how back-end compliance reviewers handle driver uploads. Test both approved and rejected user paths:",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.simulateVerificationApproval() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sandbox_approve_button")
                                ) {
                                    Text("Approve All", fontSize = 10.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.simulateVerificationRejection("The uploaded PrDP image is blurry. Please upload a clear photo.") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sandbox_reject_button")
                                ) {
                                    Text("Reject Permit", fontSize = 10.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.resetCompliance() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sandbox_reset_button")
                                ) {
                                    Text("Reset All", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.setShowComplianceDashboard(false) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B9BEF))
            ) {
                Text("Close / Tswala", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun DocumentFormSection(
    title: String,
    subLabel: String,
    number: String,
    onNumberChange: (String) -> Unit,
    expiry: String,
    onExpiryChange: (String) -> Unit,
    status: String,
    imageName: String,
    onUploadMock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subLabel,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }

                // Document specific status badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (status) {
                                "APPROVED" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                "PENDING" -> Color(0xFFFFC72C).copy(alpha = 0.2f)
                                "REJECTED" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                else -> Color(0xFF64748B).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status,
                        color = when (status) {
                            "APPROVED" -> Color(0xFF10B981)
                            "PENDING" -> Color(0xFFFFC72C)
                            "REJECTED" -> Color(0xFFEF4444)
                            else -> Color(0xFF94A3B8)
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text Inputs for Number & Expiry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = onNumberChange,
                    label = { Text("Doc Number", fontSize = 10.sp) },
                    placeholder = { Text("e.g. PRDP-BW-991") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2B9BEF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    modifier = Modifier.weight(1.2f)
                )

                OutlinedTextField(
                    value = expiry,
                    onValueChange = onExpiryChange,
                    label = { Text("Expiry (DD-MM-YYYY)", fontSize = 10.sp) },
                    placeholder = { Text("e.g. 25-12-2028") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2B9BEF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File Attachment Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = "Attachment",
                        tint = if (imageName.isNotBlank()) Color(0xFF2B9BEF) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (imageName.isNotBlank()) imageName else "No file selected",
                        color = if (imageName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = if (imageName.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Button(
                    onClick = onUploadMock,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Upload, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (imageName.isNotBlank()) "Change" else "Upload copy", fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }
}
