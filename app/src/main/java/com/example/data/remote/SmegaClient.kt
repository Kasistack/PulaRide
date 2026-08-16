package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * PulaRide payment client — talks ONLY to the Supabase Edge Function
 * `smega-payment`, which holds the merchant secret server-side.
 *
 * The app NEVER sees the Smega API key / app id / secret token. It only sends
 * the rider's own payerId + PIN and the amount; the function authorises with
 * Smega and then credits/debits the user's Supabase wallet (single source of
 * truth for balance). See supabase/functions/smega-payment/index.ts.
 */

object SmegaConfig {
    // Functions live on the Supabase project URL.
    val baseUrl: String get() = SupabaseConfig.url
}

@JsonClass(generateAdapter = true)
data class PaymentRequest(
    val amount: Double,
    val payerId: String,
    val pin: String,
    val kind: String = "TOPUP",   // "TOPUP" | "CHARGE"
    val ref: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentResponse(
    val status: String? = null,   // "SUCCESS"
    val balance: Double? = null,
    val ref: String? = null,
    val error: String? = null,
    val message: String? = null
)

interface PaymentApi {
    @Headers("Content-Type: application/json")
    @POST("functions/v1/smega-payment")
    suspend fun charge(
        @Body body: PaymentRequest
    ): Response<PaymentResponse>
}

object SmegaClient {
    private val moshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    val api: PaymentApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("${SmegaConfig.baseUrl}/")
            .addConverterFactory(
                retrofit2.converter.moshi.MoshiConverterFactory.create(moshi)
            )
            .client(
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor(okhttp3.Interceptor { chain ->
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
            .create(PaymentApi::class.java)
    }
}
