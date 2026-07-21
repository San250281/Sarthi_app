package com.aistudio.saarthi.api

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
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
data class SaarthiApiRequest(
    val userId: String,
    val message: String
)

@JsonClass(generateAdapter = true)
data class SaarthiApiResponse(
    val conversationId: String? = null,
    val reply: String? = null,
    val error: String? = null
)

object GeminiClient {

    private const val TAG = "SAARTHI_AWS_API"

    private const val API_URL =
        "https://vw4l7d1ezh.execute-api.us-east-1.amazonaws.com/prod/chat"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(SaarthiApiRequest::class.java)
    private val responseAdapter = moshi.adapter(SaarthiApiResponse::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        history: List<GeminiContent>,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {

        try {
            val latestUserMessage = history.lastOrNull()
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: "Hello"

            val finalMessage = """
                $systemPrompt

                User message:
                $latestUserMessage
            """.trimIndent()

            val requestBodyData = SaarthiApiRequest(
                userId = "test-user-001",
                message = finalMessage
            )

            val jsonString = requestAdapter.toJson(requestBodyData)

            val requestBody = jsonString.toRequestBody(
                "application/json; charset=utf-8".toMediaType()
            )

            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $token")

                Log.d(TAG, "FULL RESPONSE BODY: $bodyString")

                val apiResponse = responseAdapter.fromJson(bodyString)

                return@withContext apiResponse?.reply
                    ?: apiResponse?.error
                    ?: bodyString
            }
            return@withContext "Server error: $code. Please try again."

        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            return@withContext "Please check your internet connection."
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            return@withContext "Unable to connect to Saarthi server."
        }
    }
}
