package com.personaltrainer.ai

/**
 * Template-based LLM provider — the last-resort fallback that never fails.
 *
 * It does not call any API.  It uses hard-coded templates to produce
 * sensible, safe responses for all supported tasks.
 *
 * [priority] = Int.MAX_VALUE so ProviderRouter tries it last.
 */
class TemplateProvider : LlmProvider {

    override val name = "Template"
    override val priority = Int.MAX_VALUE

    override suspend fun isAvailable() = true   // Always available

    override suspend fun generate(request: LlmRequest): LlmResult {
        val task = request.taskInstruction.lowercase()

        val text = when {
            task.contains("meal") || task.contains("food") || task.contains("eat") ->
                parseMealTemplate(request.taskInstruction)
            task.contains("summary") || task.contains("review") || task.contains("week") ->
                weeklySummaryTemplate()
            task.contains("motivat") || task.contains("encourage") ->
                motivationTemplate()
            task.contains("miss") || task.contains("skip") ->
                skipTemplate()
            task.contains("swap") || task.contains("exercise") ->
                exerciseSwapTemplate()
            else ->
                defaultTemplate()
        }

        return LlmResult.Success(text)
    }

    // ─ Templates ─────────────────────────────────────────────────────────

    private fun parseMealTemplate(instruction: String): String {
        // Return a minimal valid JSON that the brain can use as a fallback.
        // The brain will show a confidence label and let the user edit.
        return """
            {
              "intent": "log_meal",
              "slot": "unknown",
              "items": [],
              "needs_confirmation": true,
              "note": "Could not parse automatically. Please enter nutritional details manually."
            }
        """.trimIndent()
    }

    private fun weeklySummaryTemplate(): String =
        "Great work this week! Keep focusing on consistency — showing up regularly is what drives " +
        "long-term results. Review your numbers above and adjust as needed."

    private fun motivationTemplate(): String =
        "Every rep counts. Progress happens session by session, not all at once. " +
        "You're building a habit that will last."

    private fun skipTemplate(): String =
        "No worries — life happens. I've adjusted your plan. The most important thing " +
        "is getting back on track at the next opportunity."

    private fun exerciseSwapTemplate(): String =
        "I'd suggest choosing an exercise that targets the same muscle group with similar " +
        "mechanics. Check the exercise library for alternatives."

    private fun defaultTemplate(): String =
        "I'm here to help you stay on track. Check your Today screen for your current plan " +
        "and targets."
}
