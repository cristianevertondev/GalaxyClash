package com.cristian.galaxyclash.game

/**
 * Types of collectible power-ups. New types are added here, and their rarity /
 * effect are configured in [GameConfig] and applied in [PickupSystem].
 */
enum class PickupType {
    /** Restores player HP (does not exceed [GameConfig.PLAYER_MAX_HP]). */
    HEALTH,

    /** Permanently raises the weapon tier to at least 2 (double-shot). */
    DOUBLE_SHOT,

    /** Permanently raises the weapon tier to at least 3 (triple-shot). */
    TRIPLE_SHOT,

    /** Permanently raises the weapon tier to 4 (quad-shot). */
    QUAD_SHOT,

    /** Fires much faster for [GameConfig.RAPID_FIRE_DURATION_SECONDS]. */
    RAPID_FIRE,

    /** Grants damage immunity for [GameConfig.SHIELD_DURATION_SECONDS]. */
    SHIELD,

    /** Player shots deal more damage for [GameConfig.DAMAGE_BOOST_DURATION_SECONDS]. */
    DAMAGE_BOOST,

    /** Attracts nearby pickups toward the player for [GameConfig.MAGNET_DURATION_SECONDS]. */
    MAGNET,

    /** Multiplies score gained for [GameConfig.SCORE_MULTIPLIER_DURATION_SECONDS]. */
    SCORE_MULTIPLIER,

    /** Adds an extra life (capped at [GameConfig.MAX_EXTRA_LIVES]). */
    EXTRA_LIFE,
}