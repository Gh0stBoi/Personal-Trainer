package com.personaltrainer.ai

/**
 * LLM provider contract — section 6.10 of the implementation plan.
 *
 * The LLM layer is optional.  [TemplateProvider] always works without
 * any API key or network connection.
 */
interface LlmProvider {
    val name: String
    val priority: Int        // lower = tried first

    /** Returns true if this provider is currently usable. */
    suspend fun isAvailable(): Boolean

    /**
     * Generate a response for [request].
     * May throw [LlmException] on unrecoverable errors.
     * On throttling (429) it should throw [LlmThrottledException].
     */
    suspend fun generate(request: LlmRequest): LlmResult
}

data class LlmRequest(
    val systemPrompt: String,
    val contextJson: String,       // compact JSON context pack
    val taskInstruction: String,
    val outputSchema: String? = null,  // JSON schema for structured output
    val maxTokens: Int = 300,
    val temperature: Float = 0.3f
)

sealed class LlmResult {
    data class Success(val text: String, val isJson: Boolean = false) : LlmResult()
    data class Failure(val reason: String) : LlmResult()
}

open class LlmException(message: String) : Exception(message)
class LlmThrottledException(provider: String) : LlmException("$provider: rate limited (429)")
class LlmUnavailableException(provider: String) : LlmException("$provider: unavailable")

/** Closed set of intents the LLM may return. */
enum class LlmIntent {
    LOG_MEAL,
    LOG_SLEEP,
    REPORT_SKIP,
    REQUEST_RESCHEDULE,
    SWAP_EXERCISE,
    ASK_QUESTION,
    CHAT_ONLY
}
