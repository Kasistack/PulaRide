package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Real Smega (BTC Botswana) payment client — free JSON API, no card, no paid gateway.
 * Docs: https://smegaapi.btc.bw/documentation.html
 *
 * Production endpoint:  https://smegaapi.btc.bw/api/transact/jsonTxn
 * Sandbox (testing):    same host is used for tests after registering a test account.
 *
 * The payer authorises the debit from their Smega wallet using their Smega PIN.
 * A successful charge has txnStatus == "AUTHORIZED".
 */
object SmegaConfig {
    const val BASE_URL = "https://smegaapi.btc.bw/"
}

@JsonClass(generateAdapter = true)
data class SmegaMerchantId(
    val apiKey: String,
    val appId: String,
    @Json(name = "secretToken") val secretToken: String
)

@JsonClass(generateAdapter = true)
data class SmegaCustomer(
    @Json(name = "payerId") val payerId: String,
    val pin: String,
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class SmegaTxnRequest(
    @Json(name = "merchantId") val merchantId: SmegaMerchantId,
    val customer: SmegaCustomer,
    @Json(name = "gatewayId") val gatewayId: String = "SMEGA"
)

@JsonClass(generateAdapter = true)
data class SmegaTxnData(
    @Json(name = "payerId") val payerId: String? = null,
    val amount: Double? = null,
    val approved: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class SmegaTxnResponse(
    val id: Long? = null,
    @Json(name = "txnData") val txnData: SmegaTxnData? = null,
    val status: Int? = null,
    @Json(name = "txnStatus") val txnStatus: String? = null,
    val message: String? = null
)

interface SmegaApi {
    @Headers("Content-Type: application/json")
    @POST("api/transact/jsonTxn")
    suspend fun charge(@Body body: SmegaTxnRequest): Response<SmegaTxnResponse>
}

object SmegaClient {
    private val moshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    val api: SmegaApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(SmegaConfig.BASE_URL)
            .addConverterFactory(
                retrofit2.converter.moshi.MoshiConverterFactory.create(moshi)
            )
            .build()
            .create(SmegaApi::class.java)
    }
}
