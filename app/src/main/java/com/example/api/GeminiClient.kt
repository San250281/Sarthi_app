package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String // base64 encoded audio
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        history: List<GeminiContent>,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder!")
            return@withContext "I'm having trouble connecting right now. Please configure your GEMINI_API_KEY in the Secrets panel in AI Studio."
        }

        val requestBodyData = GeminiRequest(
            contents = history,
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(text = systemPrompt))
            )
        )

        try {
            val jsonString = requestAdapter.toJson(requestBodyData)
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonString.toRequestBody(jsonMediaType)

            val apiRequest = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(apiRequest).execute()
            val bodyString = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "Unsuccessful response: ${response.code} $bodyString")
                return@withContext "I'm here to listen, but I'm encountering a small technical issue. Let's try again in a moment."
            }

            if (bodyString != null) {
                val geminiResponse = responseAdapter.fromJson(bodyString)
                val textResponse = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (textResponse != null) {
                    return@withContext textResponse
                }
            }
            return@withContext "I heard you, but I couldn't formulate a response. Can you repeat that, please?"
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            return@withContext "I had a small glitch in my systems. I'm still here for you. What were you saying?"
        }
    }
}
