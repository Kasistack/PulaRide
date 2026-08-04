package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RideHistoryEntity
import com.example.data.RideRepository
import com.example.model.*
import androidx.compose.ui.graphics.Color
import com.example.ui.components.SimulatedDriver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

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

    // Location choices in Gabs
    val locationsInGabs = listOf(
        LocationItem("Sir Seretse Khama International Airport", "SSKB Airport", -24.6282, 25.9243, "Lepatlelo la Lifofane la SSKB"),
        LocationItem("CBD (Three Dikgosi Monument)", "Central Business District", -24.6565, 25.9119, "Sebeletso sa Dikgosi Tse Tharo"),
        LocationItem("Main Mall, Gaborone", "Main Mall", -24.6551, 25.9138, "Main Mall Gaborone"),
        LocationItem("University of Botswana (UB)", "UB Campus", -24.6694, 25.9229, "Mmadikolo (UB)"),
        LocationItem("Riverwalk Mall", "Riverwalk East", -24.6792, 25.9570, "Riverwalk Mall"),
        LocationItem("Airport Junction Mall", "Airport Junction", -24.6468, 25.9266, "Mabenkele a Airport Junction"),
        LocationItem("Gaborone Game Reserve", "Broadhurst East", -24.6739, 25.9066, "Lefelo la Diphologolo"),
        LocationItem("Gaborone Dam", "Dam Site", -24.6430, 25.8750, "Letamo la Gaborone"),
        LocationItem("Government Enclave", "Parliament & Offices", -24.6566, 25.9132, "Lefelo la Puso")
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

    // Simulated nearby cabs moving on the map
    val nearbyDriversMap = listOf(
        SimulatedDriver("d1", "Moffat", -0.05f, -0.4f, 0.5f),
        SimulatedDriver("d2", "Tiro", 0.1f, -0.05f, 1.2f),
        SimulatedDriver("d3", "Neo (Female Driver)", 0.2f, 0.3f, 2.5f, Color(0xFFE02424)), // Red for highlight
        SimulatedDriver("d4", "Ofilwe", -0.3f, -0.15f, 4.0f)
    )

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
        if (_loginPhone.value.isNotBlank()) {
            _isOtpSent.value = true
        }
    }

    fun setOtpCode(code: String) {
        _otpCode.value = code
    }

    fun verifyOtp() {
        if (_otpCode.value == "8392" || _otpCode.value.length == 4 || _otpCode.value.length == 6) {
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

    // Interactive 'Offer Your Fare' Bidding Loop
    fun requestRide() {
        if (_pickup.value == null || _dropoff.value == null) return
        
        _biddingPhase.value = "SUBMITTED"
        _currentScreen.value = "RIDE_BIDDING"
        _driverOffers.value = emptyList()

        driverBidSimulationJob?.cancel()
        driverBidSimulationJob = viewModelScope.launch {
            // Simulate waiting 2.5 seconds, then load bids
            delay(2500)
            _biddingPhase.value = "HAS_OFFERS"
            
            val baseUserOffer = _userOfferedFare.value.toDoubleOrNull() ?: _suggestedFare.value
            
            // Build 3 distinct Botswana-styled drivers bidding!
            val offers = mutableListOf(
                DriverOffer(
                    driverId = "d_thabo",
                    name = "Thabo Sekgopi",
                    photoRes = "https://images.unsplash.com/photo-1542909168-82c3e7fdca5c",
                    rating = 4.8f,
                    vehicleModel = "Toyota Corolla (White)",
                    vehiclePlate = "B 382 ADG",
                    originalBid = baseUserOffer,
                    offeredBid = baseUserOffer + 10.0, // Thabo counters +10 Pula
                    etaMinutes = 3,
                    heatPreference = "AC High / Ke e buletse haholo"
                ),
                DriverOffer(
                    driverId = "d_neo",
                    name = "Neo Kgetse",
                    photoRes = "https://images.unsplash.com/photo-1544005313-94ddf0286df2",
                    rating = 4.9f,
                    vehicleModel = "Honda Fit (Sky Blue)",
                    vehiclePlate = "B 911 ALK",
                    originalBid = baseUserOffer,
                    offeredBid = baseUserOffer, // Neo matches exactly!
                    etaMinutes = 1,
                    heatPreference = "AC Gentle / E thotse",
                    hasFemaleOption = true
                ),
                DriverOffer(
                    driverId = "d_kagiso",
                    name = "Kagiso Phiri",
                    photoRes = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d",
                    rating = 4.6f,
                    vehicleModel = "VW Polo (Silver)",
                    vehiclePlate = "B 283 ARD",
                    originalBid = baseUserOffer,
                    offeredBid = (baseUserOffer - 5.0).coerceAtLeast(30.0), // Bargain lower bid!
                    etaMinutes = 5,
                    heatPreference = "Windows down / Lifensetere di buletswe"
                )
            )

            // Filter out non-female options if womenOnly is toggled
            _driverOffers.value = if (_womenOnlyDriverFilter.value) {
                offers.filter { it.hasFemaleOption }
            } else {
                offers
            }
        }
    }

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
        _currentScreen.value = "IN_RIDE"
        _rideProgress.value = 0f
        _chatMessages.value = listOf(
            Pair("Driver", "Dumela! Ke tseleng go tla go go tsaya. I am on my way to pick you up.")
        )

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

    fun sendChatMessage(msg: String) {
        if (msg.isBlank()) return
        _chatMessages.value = _chatMessages.value + Pair("Rider", msg)
        
        // Simulated responsive answer
        viewModelScope.launch {
            delay(1500)
            val responses = listOf(
                "Ke gaufi le rre, ke feditse go tswa mo CBD.",
                "Eya rra/mma, ke ya go bula masepala wa AC jaanong.",
                "A o gaufi le letshwao la kgale? Ke tla ema foo.",
                "Sharp! I am arriving in 30 seconds."
            )
            _chatMessages.value = _chatMessages.value + Pair("Driver", responses[Random.nextInt(responses.size)])
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
