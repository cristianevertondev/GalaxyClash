package com.cristian.galaxyclash

import com.cristian.galaxyclash.game.CollisionSystem
import com.cristian.galaxyclash.game.Enemy
import com.cristian.galaxyclash.game.EnemyAISystem
import com.cristian.galaxyclash.game.EnemyCatalog
import com.cristian.galaxyclash.game.EnemyKind
import com.cristian.galaxyclash.game.GameEngine
import com.cristian.galaxyclash.game.PlayerInput
import com.cristian.galaxyclash.game.Projectile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EnemyAISystemTest {

    private val width = 360f

    /** Simulates many ticks at a wall to guarantee no enemy sticks forever. */
    @Test
    fun zigzagNeverSticksAtWall() {
        val enemy = Enemy.of(EnemyKind.ZIGZAG, x = 0f, y = 0f, seed = 0.5f)
        val random = Random(1L)
        val dt = 1f / 60f
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE

        // Run 500 frames — an enemy that "sticks" would stay at ~0 the whole time.
        for (i in 0 until 500) {
            EnemyAISystem.update(enemy, dt, width, playerX = width / 2f, playerY = 400f, random)
            enemy.x = (enemy.x + enemy.vx * dt).coerceIn(0f, width - enemy.width)
            enemy.y += enemy.vy * dt
            minX = minOf(minX, enemy.x)
            maxX = maxOf(maxX, enemy.x)
        }

        // It should have traveled across a meaningful span (not pinned at 0).
        assertTrue("enemy stayed pinned near x=$minX", maxX - minX > width * 0.4f)
    }

    @Test
    fun randomEnemyNeverLeavesHorizontalBounds() {
        val enemy = Enemy.of(EnemyKind.RANDOM, x = width / 2f, y = 0f, seed = 0.25f)
        val random = Random(2L)
        val dt = 1f / 60f
        for (i in 0 until 1000) {
            EnemyAISystem.update(enemy, dt, width, playerX = width / 2f, playerY = 400f, random)
            enemy.x = (enemy.x + enemy.vx * dt).coerceIn(0f, width - enemy.width)
            assertTrue(enemy.x >= -0.01f && enemy.x <= width - enemy.width + 0.01f)
        }
    }

    @Test
    fun shooterFiresProjectilesPeriodically() {
        val enemy = Enemy.of(EnemyKind.SHOOTER, x = width / 2f, y = 0f, seed = 0.5f)
        val dt = 1f / 60f
        var shots = 0
        for (i in 0 until (60 * 10)) {
            if (EnemyAISystem.wantsToShoot(enemy, dt)) shots++
        }
        // Over 10s with a ~2.2s cooldown, expect a handful of shots (not zero, not spammed).
        assertTrue("expected some shots, got $shots", shots >= 2)
        assertTrue("expected bounded fire rate, got $shots", shots <= 12)
    }

    @Test
    fun eliteHasMoreHealthThanBasic() {
        val basic = EnemyCatalog.spec(EnemyKind.BASIC)
        val elite = EnemyCatalog.spec(EnemyKind.ELITE)
        assertTrue(elite.hp > basic.hp)
    }

    @Test
    fun hostileProjectileHitsPlayerCollision() {
        // A downward hostile shot overlapping the player's center.
        val p = Projectile(x = 0f, y = 0f, width = 8f, height = 8f, vy = 200f, fromPlayer = false)
        val player = com.cristian.galaxyclash.game.Player(x = -2f, y = -2f)
        assertTrue(CollisionSystem.projectileHitsPlayer(p, player))
    }

    @Test
    fun noEnemyKindEverSticksAtWalls() {
        // Seed-sweep regression: run every lateral-moving enemy kind against the
        // walls across many seeds and frames; none may sit pinned at a wall.
        for (kind in listOf(
            EnemyKind.BASIC, EnemyKind.ZIGZAG, EnemyKind.RANDOM,
            EnemyKind.SHOOTER, EnemyKind.AGGRESSIVE, EnemyKind.ELITE,
        )) {
            for (seed in 0..5L) {
                val random = Random(seed)
                // Spawn right on a wall to stress the flip logic.
                val startOnLeft = Random(seed).nextBoolean()
                val enemy = Enemy.of(
                    kind,
                    x = if (startOnLeft) 0f else width - EnemyCatalog.spec(kind).width,
                    y = 0f,
                    seed = Random(seed).nextFloat(),
                )
                val dt = 1f / 60f
                var minX = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                for (i in 0 until 600) {
                    EnemyAISystem.update(enemy, dt, width, playerX = width / 2f, playerY = 400f, random)
                    enemy.x = (enemy.x + enemy.vx * dt).coerceIn(0f, width - enemy.width)
                    minX = minOf(minX, enemy.x); maxX = maxOf(maxX, enemy.x)
                }
                val span = maxX - minX
                assertTrue(
                    "$kind at seed=$seed stuck at wall (x in [$minX..$maxX])",
                    span > width * 0.15f,
                )
            }
        }
    }

    @Test
    fun engineSpawningProducesEnemiesOverTime() {
        val e = GameEngine(360f, 640f, Random(0L))
        repeat(60 * 15) { e.step(1f / 60f, PlayerInput(firing = true)) }
        // After 15s, at least some enemies should have spawned (unless all got
        // instantly killed by auto-fire — but spawn interval is fast enough).
        assertTrue(e.snapshot().enemies.isNotEmpty() || e.snapshot().enemiesDestroyed > 0)
    }
}