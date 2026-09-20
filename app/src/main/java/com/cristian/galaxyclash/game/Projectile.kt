package com.cristian.galaxyclash.game

/**
 * A projectile. Used by both the player (friendly) and enemies (hostile).
 * The [fromPlayer] flag decides collision grouping. For player shots, vy is
 * negative (upward); for enemy shots, vy is positive (downward) — set at spawn.
 */
data class Projectile(
    val x: Float,
    val y: Float,
    val width: Float = GameConfig.PROJ_WIDTH,
    val height: Float = GameConfig.PROJ_HEIGHT,
    val vx: Float = 0f,
    val vy: Float = -GameConfig.PROJ_SPEED,
    val damage: Int = GameConfig.PROJ_DAMAGE,
    /** True when fired by the player; false when fired by an enemy. */
    val fromPlayer: Boolean = true,
    /** Set true when this shot has already impacted something. */
    var hit: Boolean = false,
    /** Hostile "ping-pong" shot: bounces off the left/right walls. */
    val bounce: Boolean = false,
) {
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
}