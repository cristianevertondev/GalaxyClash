package com.cristian.galaxyclash

import com.cristian.galaxyclash.game.Asteroid
import com.cristian.galaxyclash.game.AsteroidSize
import com.cristian.galaxyclash.game.CollisionSystem
import com.cristian.galaxyclash.game.Enemy
import com.cristian.galaxyclash.game.EnemyKind
import com.cristian.galaxyclash.game.GameConfig
import com.cristian.galaxyclash.game.Player
import com.cristian.galaxyclash.game.Projectile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AsteroidTest {

    private fun rock(size: AsteroidSize, x: Float = 100f, y: Float = 100f) =
        Asteroid(x = x, y = y, size = size, vx = 10f, vy = 30f, rotSpeed = 0.5f,
            hp = if (size == AsteroidSize.LARGE) GameConfig.ASTEROID_LARGE_HP else GameConfig.ASTEROID_MEDIUM_HP)

    @Test
    fun largeIsBiggerThanMedium() {
        val medium = rock(AsteroidSize.MEDIUM)
        val large = rock(AsteroidSize.LARGE)
        assertTrue(large.radius > medium.radius)
        assertTrue(large.scoreValue > medium.scoreValue)
        assertTrue(large.crushDamage > medium.crushDamage)
    }

    @Test
    fun spawnPlacesRockAboveScreen() {
        val a = Asteroid.spawn(AsteroidSize.MEDIUM, playWidth = 360f, random = Random(1L))
        assertTrue(a.y < 0f)
        assertTrue(a.x >= 0f)
        assertTrue(a.x + a.width <= 360f)
    }

    @Test
    fun movementAdvancesPosition() {
        val a = rock(AsteroidSize.MEDIUM, x = 100f, y = 100f)
        a.x += a.vx * 1f
        a.y += a.vy * 1f
        a.rotation += a.rotSpeed * 1f
        assertEquals(110f, a.x, 0.001f)
        assertEquals(130f, a.y, 0.001f)
        assertTrue(a.rotation > 0f)
    }

    @Test
    fun projectileHitsAsteroid() {
        val proj = Projectile(x = 100f, y = 100f, width = 8f, height = 8f, fromPlayer = true)
        assertTrue(CollisionSystem.projectileHitsAsteroid(proj, rock(AsteroidSize.MEDIUM, x = 100f, y = 100f)))
    }

    @Test
    fun projectileMissesAsteroid() {
        val proj = Projectile(x = 200f, y = 200f, width = 8f, height = 8f, fromPlayer = true)
        assertFalse(CollisionSystem.projectileHitsAsteroid(proj, rock(AsteroidSize.MEDIUM, x = 100f, y = 100f)))
    }

    @Test
    fun asteroidHitsPlayer() {
        val player = Player(x = 150f, y = 150f)
        assertTrue(CollisionSystem.asteroidHitsPlayer(rock(AsteroidSize.MEDIUM, x = 150f, y = 150f), player))
        assertFalse(CollisionSystem.asteroidHitsPlayer(rock(AsteroidSize.MEDIUM, x = 10f, y = 10f), player))
    }

    @Test
    fun asteroidHitsEnemy() {
        val enemy = Enemy(kind = EnemyKind.BASIC, x = 140f, y = 140f)
        assertTrue(CollisionSystem.asteroidHitsEnemy(rock(AsteroidSize.MEDIUM, x = 150f, y = 150f), enemy))
        assertFalse(CollisionSystem.asteroidHitsEnemy(rock(AsteroidSize.MEDIUM, x = 10f, y = 10f), enemy))
    }

    @Test
    fun asteroidCanDropPickup() {
        val sys = com.cristian.galaxyclash.game.PickupSystem(
            object : kotlin.random.Random() {
                override fun nextBits(bitCount: Int): Int = Random(0).nextBits(bitCount)
                override fun nextFloat(): Float = 0f
            },
        )
        val pickup = sys.maybeSpawnFromAsteroid(x = 150f, y = 150f, playWidth = 360f, playHeight = 640f)
        assertTrue(pickup != null)
    }
}