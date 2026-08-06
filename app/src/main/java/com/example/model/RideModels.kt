package com.example.model

data class LocationItem(
    val name: String,
    val landmarkName: String,
    val lat: Double,
    val lng: Double,
    val setsName: String = name // Setswana localized name
)

enum class AppLanguage {
    ENGLISH, SETSWANA
}

enum class PaymentMode(val displayName: String, val providerName: String) {
    CASH("Cash / Ke madi", "Cash"),
    ORANGE_MONEY("Orange Money", "Orange Botswana"),
    MY_ZAKA("MyZaka (Mascom)", "Mascom Wireless"),
    SMEGA("Smega", "BTC Mobile Money"),
    WALLET("Digital Wallet", "PulaRide Wallet")
}

/** Smega merchant credentials (free, from https://smegaapi.btc.bw after register). */
data class SmegaCredentials(
    val apiKey: String = "",        // WalletGateway.xxxx
    val appId: String = "",         // your app id
    val secretToken: String = ""    // your secret token
)

val PaymentMode.isSmega: Boolean get() = this == PaymentMode.SMEGA

data class DriverOffer(
    val driverId: String,
    val name: String,
    val photoRes: String, // visual representation
    val rating: Float,
    val vehicleModel: String,
    val vehiclePlate: String,
    val originalBid: Double,
    val offeredBid: Double,
    val etaMinutes: Int,
    val heatPreference: String, // AC preference: "AC High", "AC Gentle", "Heater On", "Natural Breeze"
    val hasFemaleOption: Boolean = false
)

data class ActiveRide(
    val pickup: LocationItem,
    val dropoff: LocationItem,
    val selectedDriver: DriverOffer,
    val finalFare: Double,
    val paymentMode: PaymentMode,
    val status: String, // "EN_ROUTE_PICKUP", "TRIP_ACTIVE", "COMPLETED"
    val progress: Float = 0.0f
)

data class UserProfile(
    val name: String,
    val phone: String,
    val email: String,
    val referralCode: String,
    val verified: Boolean = true,
    val balance: Double = 150.0 // Pula balance
)

// Translation Dictionary for localized Setswana Support!
object SetswanaTranslation {
    private val enMap = mapOf(
        "app_tagline" to "Your Safe, Affordable Ride in Botswana",
        "welcome" to "Dumela! Welcome",
        "login_sub" to "Enter your phone to secure a ride or start driving",
        "send_otp" to "Send OTP Code",
        "enter_otp" to "Enter 6-digit OTP",
        "verify_btn" to "Verify & Continue",
        "otp_hint" to "We sent code 8392 to verify",
        "search_pickup" to "Where from? (Pickup)",
        "search_dropoff" to "Where to? (Destination)",
        "popular_places" to "Popular Palapye Places",
        "low_data" to "Low-Data Mode",
        "language_lbl" to "Language / Puo",
        "bid_fare" to "Offer Your Fare",
        "fixed_fare" to "Fixed Fair Price",
        "fare_hint" to "Enter your bid (e.g., 45 P)",
        "pula_lbl" to "Pula (BWP)",
        "request_ride" to "Find Ride / Batla Koloi",
        "searching_drivers" to "Looking for nearby drivers in Palapye...",
        "driver_offers" to "Driver Counter Offers (Bids)",
        "accept" to "Accept / Dumela",
        "decline" to "Decline / Gana",
        "active_trip" to "Active Trip",
        "eta" to "ETA",
        "emergency" to "EMERGENCY / THUSO",
        "share_trip" to "Share Trip Details",
        "call_driver" to "In-App Masked Call",
        "chat_driver" to "Chat with Driver",
        "women_safety" to "Women/Minors safety: Request Female Driver Only",
        "preferences" to "Ride Preferences (AC/Heat)",
        "profile" to "My Profile / Mafoko a Me",
        "history" to "Ride History / Loeto la Me",
        "payments" to "Payment / Dituelo",
        "driver_mode" to "Switch to Driver App",
        "rider_mode" to "Switch to Rider App",
        "online" to "Online / Ke Bereka",
        "offline" to "Offline / Ke Itshegeditse",
        "earnings" to "Earnings Tracker",
        "verification" to "Vehicle Verification Documents",
        "upload_pdp" to "Upload PDP / Permit",
        "upload_license" to "Upload Driver's License",
        "payout" to "Quick Payout (Orange/MyZaka)",
        "incentives" to "Incentives & Bonuses",
        "total_trips" to "Total Trips",
        "joined" to "Joined PulaRide",
        "how_does_it_work" to "How does 'Offer Your Fare' work?",
        "how_work_desc" to "Enter a reasonable fare. Nearby drivers see your offer and can match it or counter-bid with their price. You pick the best driver based on rating, model, and price!"
    )

    private val tnMap = mapOf(
        "app_tagline" to "Mmapatla wa dipalangwa yo o babalesegileng mo Botswana",
        "welcome" to "Dumela! O amogetswe",
        "login_sub" to "Tsenya mogala wa gago go simolola go palama kapa go kgweetsa",
        "send_otp" to "Rola Lentswe la OTP",
        "enter_otp" to "Tsenya Nomore ya OTP ya dipalo tse 6",
        "verify_btn" to "Netefatsa o bo o Tswelela",
        "otp_hint" to "Re rometse OTP ya 8392 go go netefatsa",
        "search_pickup" to "O tswa kae? (Koo o tswang teng)",
        "search_dropoff" to "O ya kae? (Koo o yang teng)",
        "popular_places" to "Mafelo a a Itsegeng mo Gaborone",
        "low_data" to "Tsela ya go Sego Lentswe (Low-Data)",
        "language_lbl" to "Puo / Language",
        "bid_fare" to "Nka duela bo kae (Bid)",
        "fixed_fare" to "Tlhwatlhwa e e Beilweng",
        "fare_hint" to "Tsenya madi a o a tlhagang (sekai: 45 P)",
        "pula_lbl" to "Pula (BWP)",
        "request_ride" to "Batla Koloi ya PulaRide",
        "searching_drivers" to "Re batla bakhweetsi ba ba gaufi mo Gabs...",
        "driver_offers" to "Ditlhwatlhwa tsa Bakhweetsi (Bids)",
        "accept" to "Dumela / Amogela",
        "decline" to "Gana / Bogelago",
        "active_trip" to "Loeto lo lo Tsweletseng",
        "eta" to "Nako e e salang",
        "emergency" to "TSHIRELETSO / THUSO YA SEPOTLAKO",
        "share_trip" to "Arogana ka loeto le ba bangwe",
        "call_driver" to "Founela Mokgweetsi ka tshireletso",
        "chat_driver" to "Kwala molaetsa go Mokgweetsi",
        "women_safety" to "Tshireletso ya bomme/bana: Batla mokgweetsi wa mosadi fela",
        "preferences" to "Se o se batlang (AC kapa Mogote)",
        "profile" to "Mafoko a me a User",
        "history" to "Ditiragalo tsa Diloeto tsame",
        "payments" to "Ditsela tsa go Duela (Dituelo)",
        "driver_mode" to "Fetolela kwa go Kgweetseng",
        "rider_mode" to "Fetolela kwa go Palameng",
        "online" to "Ke Bereka / Online",
        "offline" to "Ke Itshegeditse / Offline",
        "earnings" to "Lentswe la Dituelo/Madi",
        "verification" to "Dikhonferense tsa Koloi ya Gago",
        "upload_pdp" to "Tsenya PDP kapa Permit ya Gago",
        "upload_license" to "Tsenya Teseletso ya go Kgweetsa",
        "payout" to "Kopo ya Madi ya Potsana (Orange/MyZaka)",
        "incentives" to "Incentives & Dietsele tsa Mafura",
        "total_trips" to "Dipalo tsotlhe tsa Diloeto",
        "joined" to "O kailwe mo PulaRide",
        "how_does_it_work" to "Offer Your Fare e bereka jang?",
        "how_work_desc" to "Tsenya tlhwatlhwa e e lekaneng. Bakhweetsi ba ba gaufi ba bona tlhwatlhwa eo mme ba ka dumela kapa ba go fa tlhwatlhwa e nngwe. O tlhopha mokgweetsi yo o mmatlang ka dipalo, tlhwatlhwa le sekgala sa gagwe!"
    )

    fun translate(key: String, lang: AppLanguage): String {
        return if (lang == AppLanguage.SETSWANA) {
            tnMap[key] ?: enMap[key] ?: key
        } else {
            enMap[key] ?: key
        }
    }
}
