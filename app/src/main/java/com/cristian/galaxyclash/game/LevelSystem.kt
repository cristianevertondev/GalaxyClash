package com.cristian.galaxyclash.game

/**
 * Infinite, procedurally-computed stage/difficulty progression. There is no
 * fixed cap: difficulty is derived from the elapsed time and clamped to sane
 * bounds so later stages reuse existing enemy types in harder combinations
 * without ever becoming impossible or overflowing.
 */
data class StageConfig(
    val index: Int,
    val name: String,
    /** Seconds between enemy spawns. */
    val spawnInterval: Float,
    /** Relative probability weights of each enemy kind in the spawn pool. */
    val spawnWeights: Map<EnemyKind, Int>,
    /** Multiplier applied to enemy speeds at this stage (1.0 at stage 1). */
    val enemySpeedMult: Float,
    /** Extra HP granted to each spawned enemy at this stage. */
    val enemyHpBonus: Int,
)

object LevelSystem {

    /** First stage lasts [FIRST_STAGE_SECONDS]; later stages switch every [STAGE_SECONDS]. */
    private const val FIRST_STAGE_SECONDS = 15f
    private const val STAGE_SECONDS = 20f

    /**
     * Returns the stage config for a given elapsed play time. The stage number
     * grows without bound, but its *behavior* (interval, weights, speed, HP) is
     * clamped so later stages plateau into a hard-but-fair ceiling.
     */
    fun stageFor(elapsedSeconds: Float): StageConfig {
        val t = elapsedSeconds.coerceAtLeast(0f)
        val index = stageIndex(t)
        // Spawn interval eases down toward MIN_SPAWN_INTERVAL.
        val interval = when {
            index <= 1 -> GameConfig.MAX_SPAWN_INTERVAL
            else -> (GameConfig.MAX_SPAWN_INTERVAL - (index - 1) * 0.045f)
                .coerceAtLeast(GameConfig.MIN_SPAWN_INTERVAL)
        }
        // Enemy speed multiplier ramps up to MAX_ENEMY_SPEED_MULT.
        val speedMult = (1f + (index - 1) * 0.05f).coerceAtMost(GameConfig.MAX_ENEMY_SPEED_MULT)
        // Extra enemy HP grows slowly, then plateaus.
        val hpBonus = ((index - 1) / 2).coerceIn(0, 8)

        return StageConfig(
            index = index,
            name = stageName(index),
            spawnInterval = interval,
            spawnWeights = stageWeights(index),
            enemySpeedMult = speedMult,
            enemyHpBonus = hpBonus,
        )
    }

    /** Stage displayed on the HUD — current stage in human terms (1..n). */
    fun levelFor(elapsedSeconds: Float): Int = stageIndex(elapsedSeconds)

    private fun stageIndex(t: Float): Int {
        if (t < FIRST_STAGE_SECONDS) return 1
        // Stage 1 is [0,15); after that a new stage every STAGE_SECONDS.
        return 2 + ((t - FIRST_STAGE_SECONDS) / STAGE_SECONDS).toInt()
    }

    private fun stageName(index: Int): String = when (index) {
        1 -> "FIRST CONTACT"
        2 -> "MOVING TARGETS"
        3 -> "RANDOM ASSAULT"
        4 -> "FIRST BLOOD"
        5 -> "HUNTERS"
        else -> "STAGE $index"
    }

    /** Enemy mix per stage — later stages combine more types and heavier weights. */
    private fun stageWeights(index: Int): Map<EnemyKind, Int> {
        if (index <= 1) return mapOf(EnemyKind.BASIC to 100)

        val basic = (34 - index * 1).coerceIn(8, 34)
        val zigzag = (18 + index).coerceIn(18, 40)
        val random = (10 + index).coerceIn(10, 34)
        val shooter = (8 + index * 2).coerceIn(8, 42)
        val aggressive = (6 + index * 2).coerceIn(6, 40)
        val elite = (4 + index).coerceIn(4, 34)

        // Only introduce each type once its stage range is reached, so the early
        // game stays readable and later stages get crowded with variety.
        return buildMap {
            put(EnemyKind.BASIC, basic)
            put(EnemyKind.ZIGZAG, zigzag)
            put(EnemyKind.RANDOM, random)
            if (index >= 4) put(EnemyKind.SHOOTER, shooter)
            if (index >= 5) put(EnemyKind.AGGRESSIVE, aggressive)
            if (index >= 6) put(EnemyKind.ELITE, elite)
        }
    }
}