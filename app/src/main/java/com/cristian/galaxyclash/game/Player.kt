package com.cristian.galaxyclash.game

/**
 * The player's ship. Pure data — the engine mutates it via [GameEngine.step].
 *
 * Timed power-up effects are tracked as countdown timers (seconds remaining);
 * they are decremented every step and their computed effective state is exposed
 * via the derived properties below.
 */
data class Player(
    val x: Float,
    val y: Float,
    val width: Float = GameConfig.PLAYER_WIDTH,
    val height: Float = GameConfig.PLAYER_HEIGHT,
    var hp: Int = GameConfig.PLAYER_MAX_HP,
    /** Seconds remaining of post-hit invulnerability; 0 = vulnerable. */
    var invulnTimer: Float = 0f,

    // fire control (owned by the engine, stored here for snapshot simplicity)
    var fireCooldown: Float = 0f,

    /** True while the player is moving left this frame. */
    var movingLeft: Boolean = false,
    /** True while the player is moving right this frame. */
    var movingRight: Boolean = false,

    // Timed power-ups (temporary buffs) -----------------------------------
    /** Permanent weapon tier 1..4 (single / double / triple / quad). Independent
     *  of timers: once raised it never drops for the rest of the match. */
    var weaponTier: Int = 1,
    var rapidFireTimer: Float = 0f,
    var shieldTimer: Float = 0f,
    var damageBoostTimer: Float = 0f,
    var magnetTimer: Float = 0f,
    var scoreMultiplierTimer: Float = 0f,

    /** Stored extra lives (spent when HP would reach zero). */
    var extraLives: Int = 0,
) {
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    val isInvulnerable: Boolean get() = invulnTimer > 0f

    /** True while the shield power-up is active (ignores incoming damage). */
    val isShielded: Boolean get() = shieldTimer > 0f

    /** Effective weapon barrels: reflects the permanent weapon tier 1..4. */
    val weaponCount: Int
        get() = weaponTier.coerceIn(1, 4)

    /** True while damage boost is active. */
    val hasDamageBoost: Boolean get() = damageBoostTimer > 0f

    /** Effective per-shot damage. */
    val shotDamage: Int
        get() = GameConfig.PROJ_DAMAGE + if (hasDamageBoost) GameConfig.DAMAGE_BOOST_BONUS else 0

    /** True while rapid fire is active. */
    val hasRapidFire: Boolean get() = rapidFireTimer > 0f

    /** Current firing cooldown, honoring rapid fire. */
    val fireCooldownDuration: Float
        get() = if (hasRapidFire) {
            GameConfig.PLAYER_FIRE_COOLDOWN_RAPID
        } else {
            GameConfig.PLAYER_FIRE_COOLDOWN
        }

    /** True while score multiplier is active. */
    val hasScoreMultiplier: Boolean get() = scoreMultiplierTimer > 0f

    /** Effective score multiplier. */
    val scoreMultiplier: Int
        get() = if (hasScoreMultiplier) GameConfig.SCORE_MULTIPLIER_VALUE else 1
}