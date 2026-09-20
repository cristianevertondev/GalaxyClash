package com.cristian.galaxyclash.game

/**
 * A short-lived visual effect (impact / explosion). Purely cosmetic; the engine
 * advances its timer and discards it when expired.
 */
data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    var age: Float = 0f,
    val lifetime: Float,
) {
    val progress: Float get() = (age / lifetime).coerceIn(0f, 1f)
    val isExpired: Boolean get() = age >= lifetime
}