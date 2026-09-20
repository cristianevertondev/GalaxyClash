package com.cristian.galaxyclash

import com.cristian.galaxyclash.game.GameConfig
import com.cristian.galaxyclash.game.LevelSystem
import com.cristian.galaxyclash.game.SpaceBackgroundType
import com.cristian.galaxyclash.game.getBackgroundForScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    // --- Biomes / backgrounds ----------------------------------------------

    @Test
    fun scoreBelow15000IsInitialSpace() {
        assertEquals(SpaceBackgroundType.NORMAL_SPACE, getBackgroundForScore(0))
        assertEquals(SpaceBackgroundType.NORMAL_SPACE, getBackgroundForScore(14999))
    }

    @Test
    fun scoreAt15000ChangesToMilkyWay() {
        assertEquals(SpaceBackgroundType.MILKY_WAY, getBackgroundForScore(15000))
        assertEquals(SpaceBackgroundType.MILKY_WAY, getBackgroundForScore(20000))
    }

    @Test
    fun backgroundProgressesThroughLaterRegions() {
        // One space region per 15000 score, through the extended 24-region map.
        assertEquals(SpaceBackgroundType.NEBULA, getBackgroundForScore(30000))           // level 2
        assertEquals(SpaceBackgroundType.PLANET_SECTOR, getBackgroundForScore(45000))    // level 3
        assertEquals(SpaceBackgroundType.DEEP_SPACE, getBackgroundForScore(60000))       // level 4
        assertEquals(SpaceBackgroundType.BLACK_HOLE, getBackgroundForScore(75000))       // level 5
        assertEquals(SpaceBackgroundType.DENSE_STARFIELD, getBackgroundForScore(90000))  // level 6
        assertEquals(SpaceBackgroundType.ALIEN_SPACE, getBackgroundForScore(105000))     // level 7
        assertEquals(SpaceBackgroundType.SPACE, getBackgroundForScore(120000))           // level 8
        assertEquals(SpaceBackgroundType.NEBULA2, getBackgroundForScore(150000))         // level 10
        assertEquals(SpaceBackgroundType.VOID, getBackgroundForScore(240000))            // level 16
        assertEquals(SpaceBackgroundType.COSMOS, getBackgroundForScore(345000))          // level 23
        // Past the last region the scenery clamps to the final region (no reset).
        assertEquals(SpaceBackgroundType.COSMOS, getBackgroundForScore(500000))          // level 24+
    }

    @Test
    fun backgroundIsMonotonicAsScoreGrows() {
        val scores = listOf(0, 14999, 15000, 30000, 50000, 75000, 110000, 150000, 200000, 500000)
        scores.zipWithNext { a, b ->
            val ia = getBackgroundForScore(a).ordinal
            val ib = getBackgroundForScore(b).ordinal
            assertTrue("ordinal must not decrease at $a -> $b", ia <= ib)
        }
    }

    // --- Infinite stages / difficulty --------------------------------------

    @Test
    fun stagesAreInfiniteAndProcedural() {
        val stage1 = LevelSystem.stageFor(0f)
        val stage6 = LevelSystem.stageFor(100f)
        val stageFar = LevelSystem.stageFor(1_000_000f)
        assertEquals(1, stage1.index)
        assertTrue(stage6.index > stage1.index)
        assertTrue(stageFar.index > stage6.index) // no fixed cap
    }

    @Test
    fun difficultyIsClampedToSaneBounds() {
        val far = LevelSystem.stageFor(1_000_000f)
        assertTrue(far.spawnInterval >= GameConfig.MIN_SPAWN_INTERVAL)
        assertTrue(far.spawnInterval <= GameConfig.MAX_SPAWN_INTERVAL)
        assertTrue(far.enemySpeedMult >= 1f)
        assertTrue(far.enemySpeedMult <= GameConfig.MAX_ENEMY_SPEED_MULT)
        assertTrue(far.enemyHpBonus in 0..8)
    }

    @Test
    fun spawnIntervalShortensWithDifficulty() {
        val stage1 = LevelSystem.stageFor(0f).spawnInterval
        val stage6 = LevelSystem.stageFor(100f).spawnInterval
        assertTrue(stage6 <= stage1)
    }
}