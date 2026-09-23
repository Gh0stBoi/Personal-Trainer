package com.personaltrainer.ai

import android.util.Log
import kotlinx.coroutines.delay

/**
 * ProviderRouter — section 6.10 of the implementation plan.
 *
 * Tries LLM providers in priority order.
 * On 429 / timeout → exponential back-off + circuit breaker.
 * Last stop: [TemplateProvider] (never fails).
 *
 * Call budget: designed for ≤15 LLM calls/day.
 */
class ProviderRouter(
    private val providers: List<LlmProvider>
) {
    companion object {
        private const val TAG = "ProviderRouter"
        private const val MAX_RETRIES = 1
        private const val CIRCUIT_BREAKER_COOLDOWN_MS = 5 * 60 * 1000L  // 5 min
        private const val BACKOFF_INITIAL_MS = 1000L
    }

    // Tracks the last time each provider hit its circuit breaker.
    private val cooldownUntil = mutableMapOf<String, Long>()

    suspend fun generate(request: LlmRequest): LlmResult {
        val sortedProviders = providers.sortedBy { it.priority }

        for (provider in sortedProviders) {
            // Circuit breaker — skip if in cooldown
            val cooldown = cooldownUntil[provider.name] ?: 0L
            if (System.currentTimeMillis() < cooldown) {
                Log.d(TAG, "${provider.name}: in cooldown, skipping")
                continue
            }

            if (!provider.isAvailable()) continue

            try {
                val result = generateWithRetry(provider, request)
                if (result is LlmResult.Success) {
                    Log.d(TAG, "Response from ${provider.name}")
                    return result
                }
            } catch (e: LlmThrottledException) {
                Log.w(TAG, "${provider.name}: throttled, applying circuit breaker")
                cooldownUntil[provider.name] = System.currentTimeMillis() + CIRCUIT_BREAKER_COOLDOWN_MS
            } catch (e: LlmException) {
                Log.w(TAG, "${provider.name}: failed — ${e.message}")
            }
        }

        // Should never happen because TemplateProvider is in the list
        return LlmResult.Failure("All providers failed")
    }

    private suspend fun generateWithRetry(
        provider: LlmProvider,
        request: LlmRequest
    ): LlmResult {
        var lastException: Exception? = null
        var backoff = BACKOFF_INITIAL_MS

        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                return provider.generate(request)
            } catch (e: LlmThrottledException) {
                throw e  // Don't retry on 429 — circuit breaker handles it
            } catch (e: LlmException) {
                lastException = e
                if (attempt < MAX_RETRIES) delay(backoff).also { backoff *= 2 }
            }
        }
        throw lastException ?: LlmException("Unknown error")
    }
}
