package com.cristian.galaxyclash.game

/**
 * Central place for all tunable game numbers. Keeping them here (rather than
 * scattered through the engine) makes balancing and future difficulty/upgrade
 * systems trivial to reason about.
 */
object GameConfig {

    // --- Player -------------------------------------------------------------
    const val PLAYER_WIDTH = 48f          // px
    const val PLAYER_HEIGHT = 56f         // px
    const val PLAYER_MAX_HP = 3           // hits before death
    const val PLAYER_SPEED = 460f         // px / second
    const val PLAYER_FIRE_COOLDOWN = 0.28f // seconds between shots
    const val PLAYER_FIRE_COOLDOWN_RAPID = 0.09f // seconds between shots while RAPID FIRE is active

    // --- Projectiles --------------------------------------------------------
    const val PROJ_WIDTH = 5f
    const val PROJ_HEIGHT = 16f
    const val PROJ_SPEED = 760f            // px / second, upward (player)
    const val PROJ_DAMAGE = 1
    const val PROJ_SPREAD_ANGLE = 0.22f    // radians, spread between multi-shot lanes

    // Enemy (hostile) projectile tuning ---------------------------------------
    const val ENEMY_PROJ_WIDTH = 7f
    const val ENEMY_PROJ_HEIGHT = 14f
    const val ENEMY_PROJ_DAMAGE = 1

    // --- Enemies ------------------------------------------------------------
    const val ENEMY_WIDTH = 44f
    const val ENEMY_HEIGHT = 44f
    const val ENEMY_CONTACT_DAMAGE = 1      // base damage to player on touch

    // --- Spawning -----------------------------------------------------------
    const val SPAWN_INTERVAL = 0.9f        // seconds between spawns (level 1)
    const val SPAWN_INTERVAL_MIN = 0.3f    // floor for difficulty ramp

    // --- Invulnerability ----------------------------------------------------
    const val RESPAWN_INVULN = 1.5f        // seconds of grace after a hit

    // --- Boss encounter flow ------------------------------------------------
    /** Downward speed at which enemies flee the screen when a boss appears. */
    const val ENEMY_FLEE_SPEED = 640f      // px / second (dive toward bottom)
    /** Multiplier on asteroid fall speed while enemies flee / boss is active. */
    const val ASTEROID_FLEE_MULT = 6f
    /** Spawn the next boss this many score points BEFORE its target score. */
    const val BOSS_SCENE_LEAD = 4000
    /** Boss width as a fraction of the play field (fills the top of the screen). */
    const val BOSS_WIDTH_FRACTION = 0.60f
    /** Upper cap on the boss scaling factor (defensive; keeps hitboxes sane). */
    const val BOSS_MAX_SCALE = 8f

    // --- Boss spacing / progression (intervals only; boss stats are untouched) ---
    /** Score at which the FIRST boss is due. */
    const val BOSS_1_SCORE = 15000
    /** Gap between the 1st and 2nd boss; each later gap widens by [BOSS_GAP_STEP]. */
    const val BOSS_GAP_START = 25000
    /** How much the interval between bosses grows after each boss. */
    const val BOSS_GAP_STEP = 5000
    /** Ceiling on the interval between bosses, so the endless loop stays paced. */
    const val BOSS_MAX_GAP = 60000

    // --- Pickups ----------------------------------------------------------
    const val PICKUP_SIZE = 30f            // px (square) — big enough to see on a phone
    const val PICKUP_LIFETIME_SEC = 12f    // how long a pickup stays before expiring
    const val PICKUP_VISUAL_DIST = 1.8f    // rendered ring/glow scale relative to size
    const val HEALTH_PICKUP_HEAL = 1       // HP restored by one health pickup

    // Chance that a destroyed enemy-like entity drops a pickup (per-kind).
    fun pickupChanceFor(kind: EnemyKind): Float = when (kind) {
        EnemyKind.BASIC -> 0.06f
        EnemyKind.ZIGZAG -> 0.06f
        EnemyKind.RANDOM -> 0.07f
        EnemyKind.SHOOTER -> 0.09f
        EnemyKind.AGGRESSIVE -> 0.09f
        EnemyKind.ELITE -> 0.16f
    }

    /** Chance a destroyed asteroid drops a pickup (asteroids drop a bit less). */
    const val ASTEROID_PICKUP_CHANCE = 0.10f

    // --- Power-up rarities (weights; higher = more common) ------------------
    // The engine rolls once to decide whether ANY pickup drops (via chanceFor),
    // then rolls again against this table to decide WHICH one.
    fun pickupWeight(type: PickupType): Int = when (type) {
        PickupType.HEALTH -> 34            // common
        PickupType.DOUBLE_SHOT -> 22       // common
        PickupType.RAPID_FIRE -> 16        // common
        PickupType.TRIPLE_SHOT -> 9        // less common
        PickupType.SHIELD -> 8             // less common
        PickupType.DAMAGE_BOOST -> 7       // less common
        PickupType.QUAD_SHOT -> 3          // rare
        PickupType.MAGNET -> 3             // rare
        PickupType.SCORE_MULTIPLIER -> 2   // rare
        PickupType.EXTRA_LIFE -> 1         // extremely rare
    }

    // --- Timed power-up effects ----------------------------------------------
    const val RAPID_FIRE_DURATION_SECONDS = 8f
    const val SHIELD_DURATION_SECONDS = 6f
    const val DAMAGE_BOOST_DURATION_SECONDS = 8f
    const val MAGNET_DURATION_SECONDS = 7f
    const val SCORE_MULTIPLIER_DURATION_SECONDS = 8f

    /** Extra damage per shot while DAMAGE BOOST is active. */
    const val DAMAGE_BOOST_BONUS = 2        // base 1 + 2 = 3 damage

    /** Magnet: max radius (px) around the player that pulls pickups in. */
    const val MAGNET_RANGE = 200f
    /** Magnet: pull speed toward the player (px/s), scaled by proximity. */
    const val MAGNET_SPEED = 380f

    /** Score multiplier while SCORE MULTIPLIER is active. */
    const val SCORE_MULTIPLIER_VALUE = 2

    // --- Extra life ---------------------------------------------------------
    const val MAX_EXTRA_LIVES = 3           // cap on stored extra lives

    // --- Asteroids ----------------------------------------------------------
    const val ASTEROID_MEDIUM_SIZE = 52f    // px diameter
    const val ASTEROID_LARGE_SIZE = 110f    // px diameter — clearly threatening
    const val ASTEROID_MEDIUM_HP = 6
    const val ASTEROID_LARGE_HP = 20
    const val ASTEROID_SPEED_BASE = 70f     // px/s base drift
    const val ASTEROID_ROT_SPEED = 1.2f     // rad/s base rotation
    const val ASTEROID_MEDIUM_SCORE = 300
    const val ASTEROID_LARGE_SCORE = 900
    const val ASTEROID_CONTACT_DAMAGE = 2   // damage to player on collision
    const val ASTEROID_BASE_DROP_CHANCE = 0.10f
    /** Base probability a spawn tick produces a rock (scales up with level). */
    const val ASTEROID_SPAWN_BASE_CHANCE = 0.55f
    const val ASTEROID_SPAWN_INTERVAL = 3.5f // seconds between asteroid spawn rolls
    /** Weight that a spawned rock is LARGE (vs MEDIUM). */
    const val ASTEROID_LARGE_WEIGHT = 25

    // --- Backgrounds --------------------------------------------------------
    /** Score threshold for the first big scene change (Milky Way). */
    const val FIRST_SCENE_CHANGE_SCORE = 15000
    /** Seconds over which a background cross-fades into the next one. */
    const val BACKGROUND_FADE_SECONDS = 3f

    // --- Stage / difficulty ---------------------------------------------------
    const val MAX_SPAWN_INTERVAL = 0.9f
    const val MIN_SPAWN_INTERVAL = 0.28f
    const val MAX_ENEMY_SPEED_MULT = 1.6f
}