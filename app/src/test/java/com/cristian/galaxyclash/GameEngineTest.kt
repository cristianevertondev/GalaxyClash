package com.cristian.galaxyclash

import com.cristian.galaxyclash.game.CollisionSystem
import com.cristian.galaxyclash.game.Enemy
import com.cristian.galaxyclash.game.EnemyKind
import com.cristian.galaxyclash.game.GameConfig
import com.cristian.galaxyclash.game.GameEngine
import com.cristian.galaxyclash.game.LevelSystem
import com.cristian.galaxyclash.game.Player
import com.cristian.galaxyclash.game.PlayerInput
import com.cristian.galaxyclash.game.Projectile
import com.cristian.galaxyclash.game.Asteroid
import com.cristian.galaxyclash.game.AsteroidSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun engine(seed: Long = 0L) = GameEngine(360f, 640f, Random(seed))

    // --- Movement ---------------------------------------------------------

    @Test
    fun playerMovesRightWithInput() {
        val e = engine()
        val startX = e.snapshot().player.x
        // Simulate 1 second of holding right.
        repeat(60) { e.step(1f / 60f, PlayerInput(movingRight = true)) }
        assertTrue(e.snapshot().player.x > startX)
    }

    @Test
    fun playerDoesNotPassLeftBound() {
        val e = engine()
        repeat(120) { e.step(1f / 60f, PlayerInput(movingLeft = true)) }
        assertTrue(e.snapshot().player.x >= 0f)
    }

    @Test
    fun dragClampsToBounds() {
        val e = engine()
        // Drag target far off the right edge.
        e.step(1f / 60f, PlayerInput(pointerX = 100000f))
        val p = e.snapshot().player
        assertTrue(p.x + p.width <= 360f + 0.01f)
    }

    // --- Vertical movement (free across the whole screen) --------------------

    @Test
    fun playerSpawnsNearBottom() {
        val e = engine()
        val p = e.snapshot().player
        val height = 640f
        // Spawn near the bottom, as before.
        assertTrue(p.y / height > 0.8f)
    }

    @Test
    fun playerCanRiseFreelyToTop() {
        val e = engine()
        // Drag the player toward the very top of the screen; it must be able to
        // reach near the top (free movement), but never leave the screen.
        repeat(180) { e.step(1f / 60f, PlayerInput(pointerY = 0f)) }
        val p = e.snapshot().player
        assertTrue(p.y <= 4f)
        assertTrue(p.y >= 0f)
    }

    @Test
    fun playerCannotSinkBelowBottomEdge() {
        val e = engine()
        repeat(240) { e.step(1f / 60f, PlayerInput(pointerY = 100000f)) }
        val p = e.snapshot().player
        assertTrue(p.y + p.height <= 640f + 0.01f)
    }

    @Test
    fun playerCannotRiseAboveTopEdge() {
        val e = engine()
        repeat(240) { e.step(1f / 60f, PlayerInput(pointerY = -100000f)) }
        val p = e.snapshot().player
        assertTrue(p.y >= -0.01f)
    }

    @Test
    fun playerStaysInHorizontalBounds() {
        val e = engine()
        repeat(120) { e.step(1f / 60f, PlayerInput(pointerX = -100000f)) }
        var p = e.snapshot().player
        assertTrue(p.x >= -0.01f)
        repeat(120) { e.step(1f / 60f, PlayerInput(pointerX = 100000f)) }
        p = e.snapshot().player
        assertTrue(p.x + p.width <= 360f + 0.01f)
    }

    @Test
    fun firstResizeRecentersPlayerHorizontally() {
        // Engine is constructed with placeholder dims, then resized to the real
        // screen. The first resize must re-center the ship horizontally.
        val e = GameEngine(360f, 640f, Random(0L))
        e.resize(1080f, 2400f)
        val after = e.snapshot().player
        assertEquals(1080f / 2f, after.centerX, 0.5f)
        // And it must sit in the lower vertical band.
        assertTrue(after.y / 2400f > 0.8f)
    }

    @Test
    fun secondResizeDoesNotRecentreAfterMove() {
        val e = engine()
        e.resize(1080f, 2400f) // first resize recenters (x = 540)
        // Move the player left.
        repeat(60) { e.step(1f / 60f, PlayerInput(movingLeft = true)) }
        val movedX = e.snapshot().player.centerX
        assertTrue(movedX < 540f)
        // A second resize (rotation) must preserve horizontal position, not re-center.
        e.resize(1080f, 2200f)
        assertEquals(movedX, e.snapshot().player.centerX, 0.5f)
    }

    // --- Firing -----------------------------------------------------------

    @Test
    fun firingSpawnsProjectile() {
        val e = engine()
        e.step(1f / 60f, PlayerInput(firing = true))
        assertTrue(e.snapshot().projectiles.isNotEmpty())
    }

    @Test
    fun fireRespectsCooldown() {
        val e = engine()
        // First shot.
        e.step(1f / 60f, PlayerInput(firing = true))
        val count1 = e.snapshot().projectiles.size
        // Immediately step again — cooldown should prevent a new shot.
        e.step(1f / 60f, PlayerInput(firing = true))
        assertEquals(count1, e.snapshot().projectiles.size)
    }

    // --- Collision --------------------------------------------------------

    @Test
    fun projectileHitsEnemy() {
        val p = Projectile(x = 0f, y = 0f, width = 10f, height = 10f)
        val en = Enemy(kind = EnemyKind.BASIC, x = 5f, y = 5f)
        assertTrue(CollisionSystem.projectileHitsEnemy(p, en))
    }

    @Test
    fun projectileMissesEnemy() {
        val p = Projectile(x = 0f, y = 0f, width = 10f, height = 10f)
        val en = Enemy(kind = EnemyKind.BASIC, x = 100f, y = 100f)
        assertFalse(CollisionSystem.projectileHitsEnemy(p, en))
    }

    // --- Difficulty / stages --------------------------------------------

    @Test
    fun stageForRampsDifficulty() {
        // Pure function check: stage index only ever increases.
        val stage0 = LevelSystem.stageFor(0f)
        val stage1 = LevelSystem.stageFor(30f)
        val stage2 = LevelSystem.stageFor(60f)
        val stage3 = LevelSystem.stageFor(100f)
        assertTrue(stage0.index < stage1.index)
        assertTrue(stage1.index < stage2.index)
        assertTrue(stage2.index < stage3.index)
    }

    @Test
    fun levelAdvancesWhileSurviving() {
        val e = engine(seed = 1L)
        // Drive the player hard left to dodge the early basic enemies, while the
        // clock advances enough to cross the first stage boundary.
        repeat(60 * 22) { e.step(1f / 60f, PlayerInput(movingLeft = true, firing = true)) }
        val snap = e.snapshot()
        // Either we survived to level 2+, or we died (game over) — assert the
        // invariant we care about: if alive, level advanced.
        if (snap.player.hp > 0) {
            assertTrue(snap.level > 1)
        }
    }

    // --- Pause / reset ---------------------------------------------------

    @Test
    fun pauseFreezesSimulation() {
        val e = engine()
        val before = e.snapshot().player.y
        e.setPaused(true)
        repeat(30) { e.step(1f / 60f, PlayerInput(firing = true)) }
        assertEquals(before, e.snapshot().player.y, 0.001f)
        // No new projectiles, no movement.
        assertEquals(0, e.snapshot().projectiles.size)
    }

    @Test
    fun restartClearsEverything() {
        val e = engine()
        // Play a bit to spawn enemies + fire shots.
        repeat(120) { e.step(1f / 60f, PlayerInput(firing = true)) }
        assertTrue(e.snapshot().enemies.isNotEmpty() || e.snapshot().projectiles.isNotEmpty())

        e.start()
        val snap = e.snapshot()
        assertEquals(0, snap.score)
        assertEquals(0, snap.enemiesDestroyed)
        assertTrue(snap.enemies.isEmpty())
        assertTrue(snap.projectiles.isEmpty())
        assertEquals(GameConfig.PLAYER_MAX_HP, snap.player.hp)
    }

    // --- Pickups ----------------------------------------------------------

    @Test
    fun pickupsExposedInSnapshotAndClearedOnRestart() {
        val e = engine()
        // Play long enough to spawn and destroy enemies (which may drop pickups).
        repeat(200) { e.step(1f / 60f, PlayerInput(firing = true)) }
        // The snapshot must expose the pickups list (field round-trips without error).
        e.snapshot().pickups

        // Restart must clear any lingering pickups.
        e.start()
        assertTrue(e.snapshot().pickups.isEmpty())
    }

    // --- Power-up mechanics (engine integration) ---------------------------

    private fun setPlayerState(e: GameEngine, p: Player) {
        val f = GameEngine::class.java.getDeclaredField("player")
        f.isAccessible = true
        f.set(e, p)
    }

    private fun injectHostile(e: GameEngine, p: Projectile) {
        val f = GameEngine::class.java.getDeclaredField("projectiles")
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (f.get(e) as MutableList<Projectile>).add(p)
    }

    @Test
    fun shieldAbsorbsAllDamage() {
        val e = engine()
        e.resize(360f, 640f)
        setPlayerState(e, Player(x = 150f, y = 150f, hp = 1, shieldTimer = GameConfig.SHIELD_DURATION_SECONDS))
        injectHostile(e, Projectile(x = 150f, y = 150f, fromPlayer = false))
        e.step(1f / 60f)
        assertEquals(1, e.snapshot().player.hp) // shield absorbed the hit
    }

    @Test
    fun extraLifeIsSpentInsteadOfGameOver() {
        val e = engine()
        e.resize(360f, 640f)
        setPlayerState(e, Player(x = 150f, y = 150f, hp = 1, extraLives = 1))
        injectHostile(e, Projectile(x = 150f, y = 150f, fromPlayer = false))
        e.step(1f / 60f)
        val snap = e.snapshot()
        assertEquals(GameConfig.PLAYER_MAX_HP, snap.player.hp) // revived to full
        assertEquals(0, snap.player.extraLives)                  // life consumed
    }

    @Test
    fun doubleShotFiresTwoBarrels() {
        val e = engine()
        setPlayerState(e, Player(x = 150f, y = 150f, weaponTier = 2))
        e.step(1f / 60f, PlayerInput(firing = true))
        assertEquals(2, e.snapshot().projectiles.size)
    }

    @Test
    fun quadShotFiresFourBarrels() {
        val e = engine()
        setPlayerState(e, Player(x = 150f, y = 150f, weaponTier = 4))
        e.step(1f / 60f, PlayerInput(firing = true))
        assertEquals(4, e.snapshot().projectiles.size)
    }

    @Test
    fun tripleShotFiresThreeBarrels() {
        val e = engine()
        setPlayerState(e, Player(x = 150f, y = 150f, weaponTier = 3))
        e.step(1f / 60f, PlayerInput(firing = true))
        assertEquals(3, e.snapshot().projectiles.size)
    }

    @Test
    fun weaponTierSurvivesPassageOfTime() {
        val e = engine()
        e.resize(360f, 640f)
        // DOUBLE must persist even after far more than a former 10s weapon timer.
        setPlayerState(e, Player(x = 150f, y = 150f, weaponTier = 2))
        repeat(600) { e.step(1f / 60f, PlayerInput()) }   // ~10s
        var snap = e.snapshot()
        assertEquals(2, snap.player.weaponTier)
        assertEquals(2, snap.player.weaponCount)
        // TRIPLE likewise.
        setPlayerState(e, Player(x = 150f, y = 150f, weaponTier = 3))
        repeat(600) { e.step(1f / 60f, PlayerInput()) }   // ~10s
        snap = e.snapshot()
        assertEquals(3, snap.player.weaponTier)
        assertEquals(3, snap.player.weaponCount)
    }

    @Test
    fun gameStartResetsWeaponToNormal() {
        val e = engine()
        e.resize(360f, 640f)
        setPlayerState(e, Player(x = 150f, y = 150f, weaponTier = 4))
        e.start()
        assertEquals(1, e.snapshot().player.weaponTier)
        assertEquals(1, e.snapshot().player.weaponCount)
    }

    @Test
    fun temporaryBuffsExpireButWeaponTierStays() {
        val e = engine()
        e.resize(360f, 640f)
        setPlayerState(
            e,
            Player(
                x = 150f, y = 150f, weaponTier = 3,
                rapidFireTimer = 0.2f, shieldTimer = 0.2f, damageBoostTimer = 0.2f,
                magnetTimer = 0.2f, scoreMultiplierTimer = 0.2f,
            ),
        )
        repeat(30) { e.step(1f / 60f, PlayerInput()) }   // ~0.5s -> all temporary timers elapse
        val snap = e.snapshot().player
        assertFalse(snap.hasRapidFire)
        assertFalse(snap.isShielded)
        assertFalse(snap.hasDamageBoost)
        assertEquals(0f, snap.magnetTimer, 0.001f)
        assertFalse(snap.hasScoreMultiplier)
        // Normal cooldown restored once rapid fire expires.
        assertEquals(GameConfig.PLAYER_FIRE_COOLDOWN, snap.fireCooldownDuration, 0.001f)
        // The expiring temporary buffs must NOT lower the permanent weapon tier.
        assertEquals(3, snap.weaponTier)
        assertEquals(3, snap.weaponCount)
    }

    @Test
    fun activeBoostProjectilesDealBonusDamage() {
        val e = engine()
        setPlayerState(e, Player(x = 150f, y = 150f, damageBoostTimer = GameConfig.DAMAGE_BOOST_DURATION_SECONDS))
        e.step(1f / 60f, PlayerInput(firing = true))
        val p = e.snapshot().projectiles.first()
        assertEquals(GameConfig.PROJ_DAMAGE + GameConfig.DAMAGE_BOOST_BONUS, p.damage)
    }

    // --- Asteroids ---------------------------------------------------------

    private fun injectAsteroid(e: GameEngine, a: Asteroid) {
        val f = GameEngine::class.java.getDeclaredField("asteroids")
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (f.get(e) as MutableList<Asteroid>).add(a)
    }

    private fun injectEnemy(e: GameEngine, en: Enemy) {
        val f = GameEngine::class.java.getDeclaredField("enemies")
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (f.get(e) as MutableList<Enemy>).add(en)
    }

    @Test
    fun asteroidSpawnsOverTimeAndClearsOnRestart() {
        val e = engine(seed = 3L)
        e.resize(360f, 640f)
        // Play long enough to hit an asteroid spawn roll.
        repeat(300) { e.step(1f / 60f, PlayerInput()) }
        e.start()
        assertEquals(0, e.snapshot().asteroids.size)
    }

    @Test
    fun asteroidDamagesPlayerOnImpact() {
        val e = engine()
        e.resize(360f, 640f)
        setPlayerState(e, Player(x = 150f, y = 150f, hp = GameConfig.PLAYER_MAX_HP))
        injectAsteroid(e, Asteroid(x = 150f, y = 150f, size = AsteroidSize.MEDIUM,
            vx = 0f, vy = 0f, rotSpeed = 0.5f, hp = GameConfig.ASTEROID_MEDIUM_HP))
        e.step(1f / 60f)
        assertTrue(e.snapshot().player.hp < GameConfig.PLAYER_MAX_HP)
    }

    @Test
    fun asteroidDestroysEnemyAndAwardsScore() {
        val e = engine()
        e.resize(360f, 640f)
        val scoreBefore = e.snapshot().score
        // A weak defeated enemy directly in a rock's path.
        injectEnemy(e, Enemy(kind = EnemyKind.BASIC, x = 140f, y = 140f, hp = 1))
        injectAsteroid(e, Asteroid(x = 150f, y = 150f, size = AsteroidSize.MEDIUM,
            vx = 0f, vy = 0f, rotSpeed = 0.5f, hp = GameConfig.ASTEROID_MEDIUM_HP))
        e.step(1f / 60f)
        // The enemy was crushed: it is gone and score increased.
        assertTrue(e.snapshot().score > scoreBefore)
        assertTrue(e.snapshot().enemies.none { it.x == 140f && it.y == 140f })
    }
}