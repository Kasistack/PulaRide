package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RideHistoryEntity
import com.example.data.RideRepository
import com.example.data.remote.SupabaseClient
import com.example.data.remote.OsrmClient
import com.example.data.remote.SmegaClient
import com.example.data.remote.RideFunctionClient
import com.example.data.remote.CreateRideRequest
import com.example.data.remote.PaymentRequest
import com.example.model.*
import com.example.ui.components.MapDriver
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RideViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RideRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = RideRepository(database.rideDao())
    }

    // Language state
    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    // Mode state: false = Rider, true = Driver
    private val _isDriverMode = MutableStateFlow(false)
    val isDriverMode: StateFlow<Boolean> = _isDriverMode.asStateFlow()

    // Screen state: "SPLASH", "LOGIN", "HOME", "RIDE_BIDDING", "IN_RIDE", "PROFILE", "PAYMENTS"
    private val _currentScreen = MutableStateFlow("SPLASH")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Room Persistent History
    val rideHistory: StateFlow<List<RideHistoryEntity>> = repository.rideHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Low data mode setting
    private val _lowDataMode = MutableStateFlow(false)
    val lowDataMode: StateFlow<Boolean> = _lowDataMode.asStateFlow()

    // User Profile
    private val _profile = MutableStateFlow(
        UserProfile(
            name = "Lekgotla Mokgweetsi",
            phone = "+267 71 839 202",
            email = "lekgotla@pularide.co.bw",
            referralCode = "PULA_GABS_83"
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    // Simulated Otp / OTP verification state
    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()
    private val _loginPhone = MutableStateFlow("")
    val loginPhone: StateFlow<String> = _loginPhone.asStateFlow()
    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    // Location choices in Palapye
    val locationsInGabs = listOf(
        LocationItem("Palapye CBD", "Palapye Centre", -22.5465, 27.1145, "Bohikeng jwa Palapye"),
        LocationItem("Palapye Railway Station", "Station", -22.5400, 27.1230, "Seteishene sa Terene"),
        LocationItem("Palapye Industrial Area", "Industrial", -22.5600, 27.1000, "Lefelo la Ditiro"),
        LocationItem("Bothapatlou Ward", "Bothapatlou", -22.5530, 27.1080, "Bothapatlou"),
        LocationItem("Moiyabana Junction", "Moiyabana", -22.5300, 27.0900, "Moiyabana"),
        LocationItem("Palapye Hospital", "Hospital", -22.5480, 27.1280, "Sepetlele sa Palapye"),
        LocationItem("Palapye Stadium", "Stadium", -22.5440, 27.1180, "Lebala la Metshameko"),
        LocationItem("Supa Ngwato Mall", "Supa Ngwato", -22.5470, 27.1150, "Mabenkele a Supa Ngwato"),
        LocationItem("Palapye Police Station", "Police", -22.5490, 27.1120, "Mapodisi a Palapye")
    )

    // Current selected route locations
    private val _pickup = MutableStateFlow<LocationItem?>(null)
    val pickup: StateFlow<LocationItem?> = _pickup.asStateFlow()

    private val _dropoff = MutableStateFlow<LocationItem?>(null)
    val dropoff: StateFlow<LocationItem?> = _dropoff.asStateFlow()

    // Autocomplete list
    private val _pickupQuery = MutableStateFlow("")
    val pickupQuery: StateFlow<String> = _pickupQuery.asStateFlow()

    private val _dropoffQuery = MutableStateFlow("")
    val dropoffQuery: StateFlow<String> = _dropoffQuery.asStateFlow()

    // Selected payment option
    private val _paymentMode = MutableStateFlow(PaymentMode.CASH)
    val paymentMode: StateFlow<PaymentMode> = _paymentMode.asStateFlow()

    // PulaRide Digital Wallet balance (BWP) — persisted locally, top-up via Smega
    private val _walletBalance = MutableStateFlow(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    // Smega payment profile — only the rider's own payer id (never a secret).
    private val _smegaCredentials = MutableStateFlow(SmegaCredentials())
    val smegaCredentials: StateFlow<SmegaCredentials> = _smegaCredentials.asStateFlow()

    // Smega payment progress: "IDLE", "PROCESSING", "SUCCESS", "FAILED"
    private val _smegaPayState = MutableStateFlow("IDLE")
    val smegaPayState: StateFlow<String> = _smegaPayState.asStateFlow()
    private val _smegaPayMessage = MutableStateFlow("")
    val smegaPayMessage: StateFlow<String> = _smegaPayMessage.asStateFlow()

    fun setSmegaCredentials(payerId: String) {
        _smegaCredentials.value = SmegaCredentials(payerId.trim())
    }

    /** Refresh the wallet balance from Supabase (source of truth). */
    fun refreshWallet() {
        viewModelScope.launch {
            try {
                val uid = currentUserId() ?: return@launch
                val resp = SupabaseClient.api.getWallet(SupabaseClient.authHeader())
                val bal = resp.body()?.firstOrNull()?.balance
                if (bal != null) _walletBalance.value = bal
            } catch (e: Exception) { /* keep last known */ }
        }
    }

    /** Top up the PulaRide wallet via the backend Smega broker (secret stays server-side). */
    fun topUpWallet(amount: Double, pin: String) {
        val payerId = _smegaCredentials.value.payerId
        viewModelScope.launch {
            _smegaPayState.value = "PROCESSING"
            try {
                val resp = SmegaClient.api.charge(
                    PaymentRequest(amount = amount, payerId = payerId, pin = pin, kind = "TOPUP")
                )
                val body = resp.body()
                if (resp.isSuccessful && body?.status == "SUCCESS") {
                    _walletBalance.value = body.balance ?: _walletBalance.value
                    _smegaPayState.value = "SUCCESS"
                    _smegaPayMessage.value = body.ref ?: "Top-up successful"
                } else {
                    _smegaPayState.value = "FAILED"
                    _smegaPayMessage.value = body?.message ?: body?.error ?: "Payment not authorized"
                }
            } catch (e: Exception) {
                _smegaPayState.value = "FAILED"
                _smegaPayMessage.value = e.message ?: "Network error"
            }
        }
    }

    /** Pay for a completed ride via the backend broker. Returns true on success. */
    fun payWithSmega(amount: Double, pin: String): Boolean {
        val payerId = _smegaCredentials.value.payerId
        var ok = false
        viewModelScope.launch {
            _smegaPayState.value = "PROCESSING"
            try {
                val resp = SmegaClient.api.charge(
                    PaymentRequest(amount = amount, payerId = payerId, pin = pin, kind = "CHARGE")
                )
                val body = resp.body()
                if (resp.isSuccessful && body?.status == "SUCCESS") {
                    _walletBalance.value = body.balance ?: _walletBalance.value
                    _smegaPayState.value = "SUCCESS"
                    _smegaPayMessage.value = body.ref ?: "Payment successful"
                    ok = true
                } else {
                    _smegaPayState.value = "FAILED"
                    _smegaPayMessage.value = body?.message ?: body?.error ?: "Payment not authorized"
                }
            } catch (e: Exception) {
                _smegaPayState.value = "FAILED"
                _smegaPayMessage.value = e.message ?: "Network error"
            }
        }
        return ok
    }

    fun resetSmegaPayState() { _smegaPayState.value = "IDLE"; _smegaPayMessage.value = "" }

    // Low pricing state / fare offer
    private val _userOfferedFare = MutableStateFlow("")
    val userOfferedFare: StateFlow<String> = _userOfferedFare.asStateFlow()
    
    private val _suggestedFare = MutableStateFlow(45.0) // baseline suggested in Pula
    val suggestedFare: StateFlow<Double> = _suggestedFare.asStateFlow()

    // Bidding phase state: "NONE", "SUBMITTED", "HAS_OFFERS"
    private val _biddingPhase = MutableStateFlow("NONE")
    val biddingPhase: StateFlow<String> = _biddingPhase.asStateFlow()

    private val _driverOffers = MutableStateFlow<List<DriverOffer>>(emptyList())
    val driverOffers: StateFlow<List<DriverOffer>> = _driverOffers.asStateFlow()

    // Active Ride details
    private val _activeRide = MutableStateFlow<ActiveRide?>(null)
    val activeRide: StateFlow<ActiveRide?> = _activeRide.asStateFlow()

    private val _rideProgress = MutableStateFlow(0f)
    val rideProgress: StateFlow<Float> = _rideProgress.asStateFlow()

    private val _womenOnlyDriverFilter = MutableStateFlow(false)
    val womenOnlyDriverFilter: StateFlow<Boolean> = _womenOnlyDriverFilter.asStateFlow()

    private val _preferredAcState = MutableStateFlow("AC Gentle")
    val preferredAcState: StateFlow<String> = _preferredAcState.asStateFlow()

    // Emergency popup triggered
    private val _emergencyTriggered = MutableStateFlow(false)
    val emergencyTriggered: StateFlow<Boolean> = _emergencyTriggered.asStateFlow()

    // Chat conversation
    private val _chatMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // Sender to Message
    val chatMessages: StateFlow<List<Pair<String, String>>> = _chatMessages.asStateFlow()

    // Driver Dashboard specific state
    private val _driverOnline = MutableStateFlow(false)
    val driverOnline: StateFlow<Boolean> = _driverOnline.asStateFlow()

    private val _driverEarnings = MutableStateFlow(480.0) // Total earned in Pula
    val driverEarnings: StateFlow<Double> = _driverEarnings.asStateFlow()

    private val _pdpUploaded = MutableStateFlow(false)
    val pdpUploaded: StateFlow<Boolean> = _pdpUploaded.asStateFlow()

    private val _licenseUploaded = MutableStateFlow(false)
    val licenseUploaded: StateFlow<Boolean> = _licenseUploaded.asStateFlow()

    // Compliance Dashboard state flows
    private val _pdpNumber = MutableStateFlow("")
    val pdpNumber: StateFlow<String> = _pdpNumber.asStateFlow()

    private val _pdpExpiry = MutableStateFlow("")
    val pdpExpiry: StateFlow<String> = _pdpExpiry.asStateFlow()

    private val _pdpStatus = MutableStateFlow("NOT_UPLOADED") // "NOT_UPLOADED", "PENDING", "APPROVED", "REJECTED"
    val pdpStatus: StateFlow<String> = _pdpStatus.asStateFlow()

    private val _pdpImageName = MutableStateFlow("")
    val pdpImageName: StateFlow<String> = _pdpImageName.asStateFlow()

    private val _licenseNumber = MutableStateFlow("")
    val licenseNumber: StateFlow<String> = _licenseNumber.asStateFlow()

    private val _licenseExpiry = MutableStateFlow("")
    val licenseExpiry: StateFlow<String> = _licenseExpiry.asStateFlow()

    private val _licenseStatus = MutableStateFlow("NOT_UPLOADED") // "NOT_UPLOADED", "PENDING", "APPROVED", "REJECTED"
    val licenseStatus: StateFlow<String> = _licenseStatus.asStateFlow()

    private val _licenseImageName = MutableStateFlow("")
    val licenseImageName: StateFlow<String> = _licenseImageName.asStateFlow()

    private val _roadworthinessNumber = MutableStateFlow("")
    val roadworthinessNumber: StateFlow<String> = _roadworthinessNumber.asStateFlow()

    private val _roadworthinessExpiry = MutableStateFlow("")
    val roadworthinessExpiry: StateFlow<String> = _roadworthinessExpiry.asStateFlow()

    private val _roadworthinessStatus = MutableStateFlow("NOT_UPLOADED") // "NOT_UPLOADED", "PENDING", "APPROVED", "REJECTED"
    val roadworthinessStatus: StateFlow<String> = _roadworthinessStatus.asStateFlow()

    private val _roadworthinessImageName = MutableStateFlow("")
    val roadworthinessImageName: StateFlow<String> = _roadworthinessImageName.asStateFlow()

    private val _complianceStatus = MutableStateFlow("NOT_COMPLIANT") // "NOT_COMPLIANT", "PENDING_REVIEW", "COMPLIANT"
    val complianceStatus: StateFlow<String> = _complianceStatus.asStateFlow()

    private val _complianceWarningTriggered = MutableStateFlow(false)
    val complianceWarningTriggered: StateFlow<Boolean> = _complianceWarningTriggered.asStateFlow()

    private val _showComplianceDashboard = MutableStateFlow(false)
    val showComplianceDashboard: StateFlow<Boolean> = _showComplianceDashboard.asStateFlow()

    private val _complianceRejectionReason = MutableStateFlow("")
    val complianceRejectionReason: StateFlow<String> = _complianceRejectionReason.asStateFlow()

    private val _activeDriverRequest = MutableStateFlow<Map<String, String>?>(null)
    val activeDriverRequest: StateFlow<Map<String, String>?> = _activeDriverRequest.asStateFlow()

    // Live nearby drivers pulled from the real Supabase backend.
    private val _mapDrivers = MutableStateFlow<List<MapDriver>>(emptyList())
    val mapDrivers: StateFlow<List<MapDriver>> = _mapDrivers.asStateFlow()

    private var driverPollJob: Job? = null

    fun startDriverFeed() {
        driverPollJob?.cancel()
        driverPollJob = viewModelScope.launch {
            while (true) {
                try {
                    val resp = SupabaseClient.api.getAvailableDrivers(SupabaseClient.authHeader())
                    if (resp.isSuccessful) {
                        _mapDrivers.value = (resp.body() ?: emptyList()).mapNotNull { d ->
                            if (d.lat != null && d.lng != null) {
                                MapDriver(
                                    id = d.id ?: d.userId ?: "",
                                    name = d.name ?: "Driver",
                                    lat = d.lat,
                                    lng = d.lng,
                                    isFemale = d.isFemale ?: false
                                )
                            } else null
                        }
                    }
                } catch (e: Exception) {
                    // network error; keep last known drivers
                }
                delay(5000) // poll every 5s for live positions
            }
        }
    }

    fun stopDriverFeed() {
        driverPollJob?.cancel()
    }

    fun reportDriverLocation(lat: Double, lng: Double) {
        val uid = currentUserId() ?: return
        viewModelScope.launch {
            try {
                SupabaseClient.api.upsertDriver(
                    SupabaseClient.authHeader(),
                    body = com.example.data.remote.DriverUpsert(
                        userId = uid,
                        name = _profile.value.name,
                        lat = lat,
                        lng = lng,
                        available = true,
                        vehicleModel = "Toyota Corolla",
                        vehiclePlate = "B 000 AAA",
                        rating = 4.8,
                        isFemale = false
                    )
                )
            } catch (e: Exception) { /* ignore offline */ }
        }
    }

    private fun currentUserId(): String? = _currentUserId.value

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserIdFlow: StateFlow<String?> = _currentUserId.asStateFlow()

    private var rideSimulationJob: Job? = null
    private var driverBidSimulationJob: Job? = null

    init {
        // Automatically route to Splash then Login after 3s delay
        viewModelScope.launch {
            delay(2500)
            _currentScreen.value = "LOGIN"
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun toggleDriverMode() {
        _isDriverMode.value = !_isDriverMode.value
        if (_isDriverMode.value) {
            // Seed a mock driver request after a few seconds if online
            if (_driverOnline.value) {
                triggerMockDriverRequest()
            }
        }
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun toggleLowDataMode() {
        _lowDataMode.value = !_lowDataMode.value
    }

    fun setLoginPhone(phone: String) {
        _loginPhone.value = phone
    }

    fun sendOtp() {
        val phone = _loginPhone.value
        if (phone.isNotBlank()) {
            // Anonymous sign-in (free, no SMS/email delivery needed). Phone is
            // captured into the local profile so the rider identity is real.
            viewModelScope.launch {
                try {
                    val resp = SupabaseClient.api.signInAnonymous(
                        com.example.data.remote.AuthAnonymousRequest()
                    )
                    if (resp.isSuccessful) {
                        val session = resp.body()
                        SupabaseClient.setSession(session?.accessToken)
                        _currentUserId.value = session?.user?.id
                        // Store the entered phone in the profile
                        _profile.value = _profile.value.copy(
                            phone = "+267 $phone",
                            name = _profile.value.name
                        )
                        _isOtpSent.value = true
                        _currentScreen.value = "HOME"
                        startDriverFeed()
                    } else {
                        _isOtpSent.value = false
                    }
                } catch (e: Exception) {
                    _isOtpSent.value = false
                }
            }
        }
    }

    fun setOtpCode(code: String) {
        _otpCode.value = code
    }

    fun verifyOtp() {
        // Anonymous flow needs no code verification; if a session exists, proceed.
        if (SupabaseClient.accessToken != null) {
            _currentScreen.value = "HOME"
        }
    }

    fun setPickupQuery(q: String) {
        _pickupQuery.value = q
    }

    fun setDropoffQuery(q: String) {
        _dropoffQuery.value = q
    }

    fun selectPickup(item: LocationItem) {
        _pickup.value = item
        _pickupQuery.value = item.name
        recalculateSuggestedFare()
    }

    fun selectDropoff(item: LocationItem) {
        _dropoff.value = item
        _dropoffQuery.value = item.name
        recalculateSuggestedFare()
    }

    fun setPaymentMode(mode: PaymentMode) {
        _paymentMode.value = mode
    }

    fun setUserOfferedFare(fare: String) {
        _userOfferedFare.value = fare
    }

    fun toggleWomenOnlyDriver() {
        _womenOnlyDriverFilter.value = !_womenOnlyDriverFilter.value
    }

    fun setPreferredAcState(state: String) {
        _preferredAcState.value = state
    }

    private fun recalculateSuggestedFare() {
        val p = _pickup.value
        val d = _dropoff.value
        if (p != null && d != null) {
            // Distance approximation based on lat/lng offset
            val dx = p.lng - d.lng
            val dy = p.lat - d.lat
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            // Suggested fare: 30 Pula base + 50 Pula per distance unit
            _suggestedFare.value = (30.0 + distance * 55.0).coerceIn(35.0, 150.0)
            _userOfferedFare.value = _suggestedFare.value.toInt().toString()
        }
    }

    // Interactive 'Offer Your Fare' Bidding Loop — real backend
    fun requestRide() {
        val p = _pickup.value ?: return
        val d = _dropoff.value ?: return
        val uid = _currentUserId.value ?: return

        _biddingPhase.value = "SUBMITTED"
        _currentScreen.value = "RIDE_BIDDING"
        _driverOffers.value = emptyList()
        _activeRequestId.value = null

        driverBidSimulationJob?.cancel()
        driverBidSimulationJob = viewModelScope.launch {
            // 1. Create the ride request on the real backend
            try {
                val resp = SupabaseClient.api.createRideRequest(
                    SupabaseClient.authHeader(),
                    body = com.example.data.remote.RideRequestInsert(
                        riderId = uid,
                        pickupLat = p.lat,
                        pickupLng = p.lng,
                        dropoffLat = d.lat,
                        dropoffLng = d.lng,
                        offeredFare = _userOfferedFare.value.toDoubleOrNull() ?: _suggestedFare.value,
                        status = "OPEN"
                    )
                )
                val req = resp.body()?.firstOrNull()
                _activeRequestId.value = req?.id
            } catch (e: Exception) {
                _biddingPhase.value = "NONE"
                return@launch
            }

            // 2. Poll for real driver bids
            val requestId = _activeRequestId.value ?: return@launch
            var waited = 0
            while (waited < 30000) { // up to 30s
                try {
                    val resp = SupabaseClient.api.getBids(SupabaseClient.authHeader(), requestId)
                    if (resp.isSuccessful) {
                        val bids = resp.body() ?: emptyList()
                        if (bids.isNotEmpty()) {
                            _biddingPhase.value = "HAS_OFFERS"
                            _driverOffers.value = bids.map { b ->
                                DriverOffer(
                                    driverId = b.driverId ?: "",
                                    bidId = b.id ?: "",
                                    name = b.driverName ?: "Driver",
                                    photoRes = "",
                                    rating = (b.rating ?: 4.5).toFloat(),
                                    vehicleModel = b.vehicleModel ?: "",
                                    vehiclePlate = b.vehiclePlate ?: "",
                                    originalBid = _userOfferedFare.value.toDoubleOrNull() ?: _suggestedFare.value,
                                    offeredBid = b.bidAmount ?: 0.0,
                                    etaMinutes = b.etaMinutes ?: 5,
                                    heatPreference = "AC Gentle",
                                    hasFemaleOption = b.isFemale ?: false
                                )
                            }.let { list ->
                                if (_womenOnlyDriverFilter.value) list.filter { it.hasFemaleOption }
                                else list
                            }
                        }
                    }
                } catch (e: Exception) { /* keep polling */ }
                delay(2500)
                waited += 2500
            }
        }
    }

    private val _activeRequestId = MutableStateFlow<String?>(null)
    val activeRequestId: StateFlow<String?> = _activeRequestId.asStateFlow()

    // The bid the rider ultimately accepts (server-validated against the request).
    private val _activeBidId = MutableStateFlow<String?>(null)

    // Accept bid and start ride simulation
    fun acceptDriverOffer(offer: DriverOffer) {
        val p = _pickup.value ?: return
        val d = _dropoff.value ?: return
        
        _activeRide.value = ActiveRide(
            pickup = p,
            dropoff = d,
            selectedDriver = offer,
            finalFare = offer.offeredBid,
            paymentMode = _paymentMode.value,
            status = "EN_ROUTE_PICKUP",
            progress = 0f
        )
        _activeBidId.value = offer.bidId   // used by the server-validated create-ride call
        _currentScreen.value = "IN_RIDE"
        _rideProgress.value = 0f
        _chatMessages.value = listOf(
            Pair("Driver", "Dumela! Ke tseleng go tla go go tsaya. I am on my way to pick you up.")
        )

        // Record the accepted ride via the SERVER-VALIDATED broker function.
        // The function pins rider_id from the request and fare from the accepted
        // bid, so a driver cannot forge a ride or an arbitrary fare.
        val reqId = _activeRequestId.value
        val bidId = _activeBidId.value
        if (reqId != null && bidId != null) {
            viewModelScope.launch {
                try {
                    val resp = RideFunctionClient.api.createRide(
                        CreateRideRequest(requestId = reqId, bidId = bidId)
                    )
                    activeRideId = resp.body()?.ride?.id
                    if (resp.body()?.error != null) {
                        // surface but keep local simulation; backend is source of truth
                        _smegaPayMessage.value = "Ride confirm: ${resp.body()?.error}"
                    }
                } catch (e: Exception) { /* offline: local history still records below */ }
            }
        }

        simulateActiveRideProgress()
    }

    private fun simulateActiveRideProgress() {
        rideSimulationJob?.cancel()
        rideSimulationJob = viewModelScope.launch {
            // Phase 1: Driver arriving (0% to 30% progress representing pickup arrival)
            while (_rideProgress.value < 0.3f) {
                delay(1200)
                _rideProgress.value += 0.05f
            }
            
            _activeRide.value = _activeRide.value?.copy(status = "TRIP_ACTIVE")
            _chatMessages.value = _chatMessages.value + Pair("System", "Trip started! O mo tseleng kafa o yang teng.")
            
            // Phase 2: Trip ongoing (30% to 100%)
            while (_rideProgress.value < 1.0f) {
                delay(1000)
                _rideProgress.value = (_rideProgress.value + 0.07f).coerceAtMost(1.0f)
            }
            
            // Phase 3: Trip Completed! Add to Room Local DB
            val ride = _activeRide.value
            if (ride != null) {
                val newEntity = RideHistoryEntity(
                    pickup = ride.pickup.name,
                    dropoff = ride.dropoff.name,
                    fare = ride.finalFare,
                    paymentMethod = ride.paymentMode.displayName,
                    timestamp = System.currentTimeMillis(),
                    driverName = ride.selectedDriver.name,
                    driverVehicle = ride.selectedDriver.vehicleModel,
                    status = "COMPLETED",
                    rating = 5.0f
                )
                repository.addRide(newEntity)
            }
            
            _activeRide.value = null
            _rideProgress.value = 0f
            // Clear current pickup/dropoff
            _pickup.value = null
            _dropoff.value = null
            _pickupQuery.value = ""
            _dropoffQuery.value = ""
            
            _currentScreen.value = "PROFILE" // Navigate to see completed ride in profile history!
        }
    }

    private var activeRideId: String? = null

    fun sendChatMessage(msg: String) {
        if (msg.isBlank()) return
        _chatMessages.value = _chatMessages.value + Pair("Rider", msg)
        val rideId = activeRideId ?: return
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            try {
                SupabaseClient.api.sendMessage(
                    SupabaseClient.authHeader(),
                    body = com.example.data.remote.MessageInsert(
                        rideId = rideId,
                        senderId = uid,
                        senderName = _profile.value.name,
                        body = msg
                    )
                )
            } catch (e: Exception) { /* offline */ }
        }
    }

    fun triggerEmergency() {
        _emergencyTriggered.value = true
    }

    fun dismissEmergency() {
        _emergencyTriggered.value = false
    }

    fun cancelRide() {
        rideSimulationJob?.cancel()
        _activeRide.value = null
        _rideProgress.value = 0f
        _pickup.value = null
        _dropoff.value = null
        _pickupQuery.value = ""
        _dropoffQuery.value = ""
        _currentScreen.value = "HOME"
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Driver Dashboard specific handlers
    fun setDriverOnline(online: Boolean) {
        if (online && _complianceStatus.value != "COMPLIANT") {
            _complianceWarningTriggered.value = true
            return
        }
        _driverOnline.value = online
        if (online) {
            triggerMockDriverRequest()
        } else {
            _activeDriverRequest.value = null
        }
    }

    fun setComplianceWarningTriggered(triggered: Boolean) {
        _complianceWarningTriggered.value = triggered
    }

    fun setShowComplianceDashboard(show: Boolean) {
        _showComplianceDashboard.value = show
    }

    fun updatePdpDetails(number: String, expiry: String, imageName: String) {
        _pdpNumber.value = number
        _pdpExpiry.value = expiry
        _pdpImageName.value = imageName
        if (number.isNotBlank() && expiry.isNotBlank() && imageName.isNotBlank()) {
            _pdpStatus.value = "PENDING"
        } else {
            _pdpStatus.value = "NOT_UPLOADED"
        }
        recalculateCompliance()
    }

    fun updateLicenseDetails(number: String, expiry: String, imageName: String) {
        _licenseNumber.value = number
        _licenseExpiry.value = expiry
        _licenseImageName.value = imageName
        if (number.isNotBlank() && expiry.isNotBlank() && imageName.isNotBlank()) {
            _licenseStatus.value = "PENDING"
        } else {
            _licenseStatus.value = "NOT_UPLOADED"
        }
        recalculateCompliance()
    }

    fun updateRoadworthinessDetails(number: String, expiry: String, imageName: String) {
        _roadworthinessNumber.value = number
        _roadworthinessExpiry.value = expiry
        _roadworthinessImageName.value = imageName
        if (number.isNotBlank() && expiry.isNotBlank() && imageName.isNotBlank()) {
            _roadworthinessStatus.value = "PENDING"
        } else {
            _roadworthinessStatus.value = "NOT_UPLOADED"
        }
        recalculateCompliance()
    }

    fun submitComplianceDocuments() {
        if (_pdpStatus.value == "NOT_UPLOADED" && _pdpNumber.value.isNotBlank() && _pdpExpiry.value.isNotBlank() && _pdpImageName.value.isNotBlank()) {
            _pdpStatus.value = "PENDING"
        }
        if (_licenseStatus.value == "NOT_UPLOADED" && _licenseNumber.value.isNotBlank() && _licenseExpiry.value.isNotBlank() && _licenseImageName.value.isNotBlank()) {
            _licenseStatus.value = "PENDING"
        }
        if (_roadworthinessStatus.value == "NOT_UPLOADED" && _roadworthinessNumber.value.isNotBlank() && _roadworthinessExpiry.value.isNotBlank() && _roadworthinessImageName.value.isNotBlank()) {
            _roadworthinessStatus.value = "PENDING"
        }
        recalculateCompliance()
    }

    fun simulateVerificationApproval() {
        if (_pdpStatus.value == "PENDING" || _pdpStatus.value == "NOT_UPLOADED" || _pdpStatus.value == "REJECTED") {
            if (_pdpNumber.value.isBlank()) _pdpNumber.value = "PRDP-GABS-2026"
            if (_pdpExpiry.value.isBlank()) _pdpExpiry.value = "31-12-2027"
            if (_pdpImageName.value.isBlank()) _pdpImageName.value = "permit_scan.jpg"
            _pdpStatus.value = "APPROVED"
        }
        
        if (_licenseStatus.value == "PENDING" || _licenseStatus.value == "NOT_UPLOADED" || _licenseStatus.value == "REJECTED") {
            if (_licenseNumber.value.isBlank()) _licenseNumber.value = "DL-BW-83921"
            if (_licenseExpiry.value.isBlank()) _licenseExpiry.value = "15-08-2029"
            if (_licenseImageName.value.isBlank()) _licenseImageName.value = "license_scan.jpg"
            _licenseStatus.value = "APPROVED"
        }

        if (_roadworthinessStatus.value == "PENDING" || _roadworthinessStatus.value == "NOT_UPLOADED" || _roadworthinessStatus.value == "REJECTED") {
            if (_roadworthinessNumber.value.isBlank()) _roadworthinessNumber.value = "CO-RW-2026A"
            if (_roadworthinessExpiry.value.isBlank()) _roadworthinessExpiry.value = "10-10-2027"
            if (_roadworthinessImageName.value.isBlank()) _roadworthinessImageName.value = "roadworthiness_scan.jpg"
            _roadworthinessStatus.value = "APPROVED"
        }

        _complianceRejectionReason.value = ""
        recalculateCompliance()
    }

    fun simulateVerificationRejection(reason: String) {
        _complianceRejectionReason.value = reason
        _pdpStatus.value = "REJECTED"
        recalculateCompliance()
    }

    fun resetCompliance() {
        _pdpNumber.value = ""
        _pdpExpiry.value = ""
        _pdpStatus.value = "NOT_UPLOADED"
        _pdpImageName.value = ""
        _licenseNumber.value = ""
        _licenseExpiry.value = ""
        _licenseStatus.value = "NOT_UPLOADED"
        _licenseImageName.value = ""
        _roadworthinessNumber.value = ""
        _roadworthinessExpiry.value = ""
        _roadworthinessStatus.value = "NOT_UPLOADED"
        _roadworthinessImageName.value = ""
        _complianceRejectionReason.value = ""
        recalculateCompliance()
    }

    private fun recalculateCompliance() {
        _pdpUploaded.value = (_pdpStatus.value == "APPROVED")
        _licenseUploaded.value = (_licenseStatus.value == "APPROVED")

        val pStatus = _pdpStatus.value
        val lStatus = _licenseStatus.value
        val rStatus = _roadworthinessStatus.value

        if (pStatus == "APPROVED" && lStatus == "APPROVED" && rStatus == "APPROVED") {
            _complianceStatus.value = "COMPLIANT"
        } else if (pStatus == "REJECTED" || lStatus == "REJECTED" || rStatus == "REJECTED") {
            _complianceStatus.value = "NOT_COMPLIANT"
            _driverOnline.value = false
        } else if (pStatus == "PENDING" || lStatus == "PENDING" || rStatus == "PENDING") {
            _complianceStatus.value = "PENDING_REVIEW"
            _driverOnline.value = false
        } else {
            _complianceStatus.value = "NOT_COMPLIANT"
            _driverOnline.value = false
        }
    }

    fun uploadDoc(docType: String) {
        _showComplianceDashboard.value = true
        if (docType == "PDP") {
            if (_pdpNumber.value.isBlank()) _pdpNumber.value = "PRDP-TEMP-839"
            if (_pdpExpiry.value.isBlank()) _pdpExpiry.value = "24-12-2027"
            if (_pdpImageName.value.isBlank()) _pdpImageName.value = "temp_prdp.jpg"
            _pdpStatus.value = "PENDING"
        } else if (docType == "LICENSE") {
            if (_licenseNumber.value.isBlank()) _licenseNumber.value = "DL-TEMP-839"
            if (_licenseExpiry.value.isBlank()) _licenseExpiry.value = "11-04-2029"
            if (_licenseImageName.value.isBlank()) _licenseImageName.value = "temp_license.jpg"
            _licenseStatus.value = "PENDING"
        }
        recalculateCompliance()
    }

    private fun triggerMockDriverRequest() {
        viewModelScope.launch {
            delay(3500)
            if (_driverOnline.value) {
                _activeDriverRequest.value = mapOf(
                    "pickup" to "Main Mall, Gaborone",
                    "dropoff" to "Sir Seretse Khama Airport",
                    "offeredFare" to "85",
                    "riderName" to "Tshepo Maru",
                    "riderRating" to "4.8"
                )
            }
        }
    }

    fun driverAcceptRequest() {
        val request = _activeDriverRequest.value ?: return
        viewModelScope.launch {
            _driverEarnings.value += request["offeredFare"]?.toDoubleOrNull() ?: 85.0
            _activeDriverRequest.value = null
            // Trigger a nice toast or transient state indicating complete
            delay(1000)
            triggerMockDriverRequest() // queue next one
        }
    }

    fun driverDeclineRequest() {
        _activeDriverRequest.value = null
        triggerMockDriverRequest()
    }

    override fun onCleared() {
        super.onCleared()
        rideSimulationJob?.cancel()
        driverBidSimulationJob?.cancel()
    }
}
