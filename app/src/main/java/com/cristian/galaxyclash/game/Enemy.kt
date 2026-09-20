package com.cristian.galaxyclash.game

/**
 * Data-driven specification for each enemy type. The single [Enemy] class is
 * parameterized by these values, so adding a new enemy is adding one entry —
 * no new classes, no new branches scattered around.
 *
 * All speeds are in px/second; sizes in px. scoreValue is awarded on kill.
 */
data class EnemySpec(
    val kind: EnemyKind,
    val hp: Int,
    val width: Float,
    val height: Float,
    val baseSpeed: Float,        // downward baseline speed (px/s)
    val lateralSpeed: Float,     // max horizontal speed (px/s)
    val shootCooldown: Float,    // seconds between shots (0 = never shoots)
    val projectileSpeed: Float,  // speed of this enemy's shots (0 = none)
    val scoreValue: Int,
    val contactDamage: Int,      // damage dealt to the player on collision
    val aggression: Float,       // 0..1: homing pull toward the player (for AGGRESSIVE)
)

/**
 * Enemy behaviors. The engine delegates movement to [EnemyAISystem], which
 * interprets [EnemyKind] against [EnemySpec] values.
 */
enum class EnemyKind {
    /** Falls straight down with a slight lateral drift. */
    BASIC,

    /** Sweeps side to side, reversing direction at the screen edges. */
    ZIGZAG,

    /** Picks a new heading at random intervals; never sticks to a wall. */
    RANDOM,

    /** Approaches the player while periodically firing aimed shots. */
    SHOOTER,

    /** Drifts toward the player with partial homing (not perfect). */
    AGGRESSIVE,

    /** Tougher, faster, may fire. Score-worthy. */
    ELITE,
}

/**
 * A single enemy ship. Pure mutable data advanced by [EnemyAISystem] and the
 * engine; held in the snapshot for rendering.
 */
data class Enemy(
    val kind: EnemyKind,
    var x: Float,
    var y: Float,
    val width: Float = GameConfig.ENEMY_WIDTH,
    val height: Float = GameConfig.ENEMY_HEIGHT,
    var hp: Int = 1,

    // --- movement state (owned by EnemyAISystem) ---------------------------
    /** Current horizontal velocity (px/s). */
    var vx: Float = 0f,
    /** Current vertical velocity (px/s). */
    var vy: Float = 0f,
    /** Time until the next random re-steer (RANDOM). */
    var steerTimer: Float = 0f,
    /** Time until the next shot (SHOOTER / ELITE). */
    var shootTimer: Float = 0f,
) {
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    val isAlive: Boolean get() = hp > 0

    companion object {
        /** Creates a fully-configured enemy of the given kind at (x, y). */
        fun of(kind: EnemyKind, x: Float, y: Float, seed: Float): Enemy {
            val spec = EnemyCatalog.spec(kind)
            return Enemy(
                kind = kind,
                x = x,
                y = y,
                width = spec.width,
                height = spec.height,
                hp = spec.hp,
            ).apply {
                // Give random-ish initial motion so enemies don't move in lock-step.
                val dir = if (((seed * 7919).toInt()) % 2 == 0) 1f else -1f
                vx = spec.lateralSpeed * dir * (0.3f + (seed % 1f) * 0.7f)
                vy = spec.baseSpeed * (0.7f + (seed % 1f) * 0.6f)
                steerTimer = 0.5f + (seed % 1f) * 1.5f
                shootTimer = spec.shootCooldown * 0.5f + (seed % 1f) * spec.shootCooldown
            }
        }
    }
}