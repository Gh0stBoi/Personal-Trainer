package com.personaltrainer.engine

/**
 * Injected clock — allows the engine to be fully tested without real time.
 *
 * In production, use [SystemClock].
 * In tests, use [FakeClock] to simulate any date/time instantly.
 */
interface Clock {
    fun nowMillis(): Long
    fun todayString(): String          // "yyyy-MM-dd"
    fun todayEpochDay(): Long          // days since 1970-01-01
}
