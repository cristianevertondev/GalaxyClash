package com.cristian.galaxyclash.game

/**
 * A collectible power-up on the play field. Pure data; the engine mutates it
 * via `age` (lifetime countdown) and `collected` (pickup trigger).
 */
data class Pickup(
    val id: Long,               // unique for stable rendering
    val type: PickupType,
    val x: Float,
    val y: Float,
    val width: Float = GameConfig.PICKUP_SIZE,
    val height: Float = GameConfig.PICKUP_SIZE,

    /** Seconds elapsing since spawn. Expires when >= [GameConfig.PICKUP_LIFETIME_SEC]. */
    var age: Float = 0f,

    /** True once the player has collected it (prevents double-pickup). */
    var collected: Boolean = false,
) {
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    val isExpired: Boolean get() = age >= GameConfig.PICKUP_LIFETIME_SEC
}