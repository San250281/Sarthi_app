package com.aistudio.saarthi.api

import android.util.Log
import com.aistudio.saarthi.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.IOException
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
    private const val TAG = "SAARTHI_GEMINI"
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
            return@withContext "The AI service is temporarily unavailable. Please configure your GEMINI_API_KEY securely in the Secrets panel in AI Studio."
        }

        val requestBodyData = GeminiRequest(
            contents = history,
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(text = systemPrompt))
            )
        )

        val jsonString = requestAdapter.toJson(requestBodyData)
        // Log request info safely (excluding key, but showing history size and prompt info)
        Log.d(TAG, "Outgoing Gemini API Request: History size = ${history.size}, Prompt length = ${systemPrompt.length}")

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonString.toRequestBody(jsonMediaType)

        val apiRequest = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(requestBody)
            .build()

        var maxRetries = 3
        var attempt = 0
        var lastErrorMsg = "Failed to communicate with AI model. Please try again."

        while (attempt < maxRetries) {
            attempt++
            try {
                Log.d(TAG, "Sending API request, attempt $attempt of $maxRetries ...")
                val response = client.newCall(apiRequest).execute()
                val code = response.code
                val bodyString = response.body?.string()

                if (response.isSuccessful && bodyString != null) {
                    val geminiResponse = responseAdapter.fromJson(bodyString)
                    val textResponse = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (textResponse != null) {
                        Log.d(TAG, "Gemini API Response Success: Output length = ${textResponse.length}")
                        return@withContext textResponse
                    } else {
                        Log.w(TAG, "Successful response but no candidates/text was returned.")
                        lastErrorMsg = "I heard you, but I couldn't formulate a response. Can you repeat that, please?"
                        // No retry for empty response structure as it's not a network error
                        break
                    }
                } else {
                    Log.e(TAG, "Unsuccessful response code = $code, Body = $bodyString")
                    lastErrorMsg = when (code) {
                        400 -> "Input formatting issue or invalid request structure (400 Bad Request)."
                        401 -> "API key is invalid. Please configure your authentic GEMINI_API_KEY in the Secrets panel."
                        403 -> "Access is denied (403 Forbidden). Please check your API key credentials."
                        404 -> "Model not found (404 Not Found). The requested model 'gemini-3.5-flash' could not be resolved."
                        429 -> "The AI service is experiencing high traffic (429 Rate Limit). Trying again in a moment..."
                        500 -> "The AI service is temporarily unavailable (500 Internal Server Error)."
                        else -> "Encountered technical issue ($code) during processing. Please try again."
                    }

                    // For highly transient errors, perform backoff delay and retry
                    if (code == 429 || code >= 500) {
                        if (attempt < maxRetries) {
                            val backoff = attempt * 1500L
                            Log.w(TAG, "Retrying transient HTTP error $code in ${backoff}ms...")
                            delay(backoff)
                            continue
                        }
                    }
                    // For client setup issues like 401, 403, 404, we break immediately
                    break
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network connection error on attempt $attempt: ${e.message}", e)
                lastErrorMsg = "Please check your internet connection."
                if (attempt < maxRetries) {
                    val backoff = attempt * 1500L
                    delay(backoff)
                    continue
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}", e)
                lastErrorMsg = "The AI service is temporarily unavailable."
                break
            }
        }

        return@withContext lastErrorMsg
    }
}
