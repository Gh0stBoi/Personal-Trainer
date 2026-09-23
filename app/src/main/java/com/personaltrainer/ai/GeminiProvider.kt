package com.personaltrainer.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gemini Flash free-tier provider.
 *
 * Uses the Gemini REST API (AI Studio).
 * The API key is stored encrypted in Android Keystore preferences
 * and passed in at construction time.
 *
 * Free tier limits (as of Sept 2026 — verify in AI Studio):
 *   Flash: ~15 req/min, daily cap per project.
 */
class GeminiProvider(
    private val apiKey: String,
    override val priority: Int = 10
) : LlmProvider {

    override val name = "Gemini"

    companion object {
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL = "gemini-2.0-flash-lite"
        private const val TIMEOUT_SECONDS = 15L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun isAvailable(): Boolean = apiKey.isNotBlank()

    override suspend fun generate(request: LlmRequest): LlmResult {
        val body = buildRequestBody(request)
        val httpRequest = Request.Builder()
            .url("$BASE_URL/$MODEL:generateContent?key=$apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(httpRequest).execute()
            when {
                response.code == 429 -> throw LlmThrottledException(name)
                !response.isSuccessful -> LlmResult.Failure(
                    "HTTP ${response.code}: ${response.message}"
                )
                else -> {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val text = json
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    LlmResult.Success(text.trim())
                }
            }
        } catch (e: LlmThrottledException) {
            throw e
        } catch (e: IOException) {
            throw LlmException("$name: network error — ${e.message}")
        }
    }

    private fun buildRequestBody(request: LlmRequest): String {
        val systemInstruction = JSONObject().apply {
            put("role", "user")
            put("parts", org.json.JSONArray().apply {
                put(JSONObject().apply { put("text", request.systemPrompt) })
            })
        }
        val userTurn = JSONObject().apply {
            put("role", "user")
            put("parts", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("text", buildString {
                        append("Context:\n${request.contextJson}\n\n")
                        append("Task:\n${request.taskInstruction}")
                        if (request.outputSchema != null) {
                            append("\n\nReturn ONLY valid JSON matching this schema:\n${request.outputSchema}")
                        }
                    })
                })
            })
        }

        return JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().apply { put("text", request.systemPrompt) })
                })
            })
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", buildString {
                                append("Context:\n${request.contextJson}\n\n")
                                append("Task:\n${request.taskInstruction}")
                                if (request.outputSchema != null) {
                                    append("\n\nReturn ONLY valid JSON matching this schema:\n${request.outputSchema}")
                                }
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", request.maxTokens)
                put("temperature", request.temperature)
            })
        }.toString()
    }
}
