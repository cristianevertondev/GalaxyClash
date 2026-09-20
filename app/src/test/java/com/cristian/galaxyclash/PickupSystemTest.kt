package com.cristian.galaxyclash

import com.cristian.galaxyclash.game.EnemyKind
import com.cristian.galaxyclash.game.GameConfig
import com.cristian.galaxyclash.game.Pickup
import com.cristian.galaxyclash.game.PickupSystem
import com.cristian.galaxyclash.game.PickupType
import com.cristian.galaxyclash.game.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** A [Random] whose nextFloat always returns a fixed value (drives drop rolls). */
private class FixedFloatRandom(private val roll: Float) : Random() {
    override fun nextBits(bitCount: Int): Int = Random(0).nextBits(bitCount)
    override fun nextFloat(): Float = roll
}

class PickupSystemTest {

    private val system = PickupSystem()
    private fun playerAt(x: Float = 150f, y: Float = 150f, hp: Int = GameConfig.PLAYER_MAX_HP) =
        Player(x = x, y = y, hp = hp)

    // --- Drop -------------------------------------------------------------

    @Test
    fun destroyedEnemyCanDropSomePickup() {
        val sys = PickupSystem(FixedFloatRandom(0f)) // force the drop roll to succeed
        val pickup = sys.maybeSpawnOnDestroy(
            kind = EnemyKind.BASIC, x = 120f, y = 90f,
            playWidth = 360f, playHeight = 640f,
        )
        assertNotNull(pickup)
        // Appears at the death position (clamped to size/2 margins).
        assertEquals(120f, pickup!!.x + pickup.width / 2f, 0.5f)
        assertEquals(90f, pickup.y + pickup.height / 2f, 0.5f)
    }

    @Test
    fun destroyedAsteroidCanDrop() {
        val sys = PickupSystem(FixedFloatRandom(0f))
        val pickup = sys.maybeSpawnFromAsteroid(x = 180f, y = 100f, playWidth = 360f, playHeight = 640f)
        assertNotNull(pickup)
    }

    @Test
    fun anyEnemyKindCanDrop() {
        val sys = PickupSystem(FixedFloatRandom(0f))
        for (kind in EnemyKind.values()) {
            val pickup = sys.maybeSpawnOnDestroy(
                kind = kind, x = 180f, y = 100f,
                playWidth = 360f, playHeight = 640f,
            )
            assertNotNull("kind $kind should be able to drop a pickup", pickup)
        }
    }

    @Test
    fun noDropWhenChanceMisses() {
        val sys = PickupSystem(FixedFloatRandom(0.99f)) // exceeds every chance
        assertNull(sys.maybeSpawnOnDestroy(
            kind = EnemyKind.BASIC, x = 120f, y = 90f, playWidth = 360f, playHeight = 640f,
        ))
    }

    @Test
    fun dropPositionClampedInsidePlayArea() {
        val sys = PickupSystem(FixedFloatRandom(0f))
        val pickup = sys.maybeSpawnOnDestroy(
            kind = EnemyKind.BASIC, x = 9999f, y = 9999f, playWidth = 360f, playHeight = 640f,
        )
        assertNotNull(pickup)
        assertTrue(pickup!!.x >= 0f)
        assertTrue(pickup.x + pickup.width <= 360f)
        assertTrue(pickup.y >= 0f)
        assertTrue(pickup.y + pickup.height <= 640f)
    }

    @Test
    fun rarityWeightsAlwaysYieldAValidType() {
        // Rolling many times never produces an invalid/unhandled type.
        val sys = PickupSystem(Random(42L))
        val seen = mutableSetOf<PickupType>()
        repeat(200) { seen += sys.rollType() }
        assertTrue(seen.isNotEmpty())
        assertTrue(seen.all { GameConfig.pickupWeight(it) > 0 })
    }

    // --- Expiry -----------------------------------------------------------

    @Test
    fun pickupExpiresAfterLifetime() {
        val pickup = Pickup(id = 1L, type = PickupType.HEALTH, x = 100f, y = 100f)
        val aged = system.tick(listOf(pickup), GameConfig.PICKUP_LIFETIME_SEC + 0.1f)
        assertTrue(aged.isEmpty())
    }

    @Test
    fun pickupAgesButDoesNotDisappearEarly() {
        val pickup = Pickup(id = 1L, type = PickupType.HEALTH, x = 100f, y = 100f)
        val aged = system.tick(listOf(pickup), 1f)
        assertEquals(1, aged.size)
        assertEquals(1f, aged[0].age, 0.001f)
    }

    // --- Effects ----------------------------------------------------------

    @Test
    fun healthRestoresAndCapsAtMax() {
        val player = playerAt(hp = 1)
        system.applyEffect(Pickup(id = 1L, type = PickupType.HEALTH, x = 150f, y = 150f), player)
        assertEquals(1 + GameConfig.HEALTH_PICKUP_HEAL, player.hp)
        // Already max must stay capped.
        val full = playerAt(hp = GameConfig.PLAYER_MAX_HP)
        system.applyEffect(Pickup(id = 2L, type = PickupType.HEALTH, x = 150f, y = 150f), full)
        assertEquals(GameConfig.PLAYER_MAX_HP, full.hp)
    }

    @Test
    fun doubleShotRaisesPermanentWeaponTier() {
        val player = playerAt()
        system.applyEffect(Pickup(id = 1L, type = PickupType.DOUBLE_SHOT, x = 150f, y = 150f), player)
        assertEquals(2, player.weaponCount)
        assertEquals(2, player.weaponTier)
    }

    @Test
    fun tripleShotUpgradesWeapon() {
        val player = playerAt()
        system.applyEffect(Pickup(id = 1L, type = PickupType.DOUBLE_SHOT, x = 150f, y = 150f), player)
        system.applyEffect(Pickup(id = 2L, type = PickupType.TRIPLE_SHOT, x = 150f, y = 150f), player)
        assertEquals(3, player.weaponCount)
    }

    @Test
    fun quadShotSetsWeaponCountFour() {
        val player = playerAt()
        system.applyEffect(Pickup(id = 1L, type = PickupType.QUAD_SHOT, x = 150f, y = 150f), player)
        assertEquals(4, player.weaponCount)
    }

    @Test
    fun weaponNeverDowngradesBelowTriple() {
        val player = playerAt()
        system.applyEffect(Pickup(id = 1L, type = PickupType.TRIPLE_SHOT, x = 150f, y = 150f), player)
        // Collecting a lower tier (double) must not downgrade the weapon.
        system.applyEffect(Pickup(id = 2L, type = PickupType.DOUBLE_SHOT, x = 150f, y = 150f), player)
        assertEquals(3, player.weaponCount)
    }

    @Test
    fun normalToDoubleToTripleUpgradesPermanently() {
        val player = playerAt()
        assertEquals(1, player.weaponTier) // NORMAL = 1
        system.applyEffect(Pickup(id = 1L, type = PickupType.DOUBLE_SHOT, x = 150f, y = 150f), player)
        assertEquals(2, player.weaponTier)
        system.applyEffect(Pickup(id = 2L, type = PickupType.TRIPLE_SHOT, x = 150f, y = 150f), player)
        assertEquals(3, player.weaponTier)
        assertEquals(3, player.weaponCount)
    }

    @Test
    fun weaponTierIsMonotonicAndNeverDowngrades() {
        val player = playerAt()
        // TRIPLE + DOUBLE -> stays TRIPLE.
        system.applyEffect(Pickup(id = 1L, type = PickupType.TRIPLE_SHOT, x = 150f, y = 150f), player)
        system.applyEffect(Pickup(id = 2L, type = PickupType.DOUBLE_SHOT, x = 150f, y = 150f), player)
        assertEquals(3, player.weaponTier)
        // QUAD + TRIPLE (+ DOUBLE) -> stays QUAD.
        system.applyEffect(Pickup(id = 3L, type = PickupType.QUAD_SHOT, x = 150f, y = 150f), player)
        assertEquals(4, player.weaponTier)
        system.applyEffect(Pickup(id = 4L, type = PickupType.TRIPLE_SHOT, x = 150f, y = 150f), player)
        assertEquals(4, player.weaponTier)
        system.applyEffect(Pickup(id = 5L, type = PickupType.DOUBLE_SHOT, x = 150f, y = 150f), player)
        assertEquals(4, player.weaponTier)
        assertEquals(4, player.weaponCount)
    }

    @Test
    fun weaponPickupsDoNotArmATimer() {
        val player = playerAt()
        system.applyEffect(Pickup(id = 1L, type = PickupType.TRIPLE_SHOT, x = 150f, y = 150f), player)
        assertEquals(3, player.weaponTier)
        assertEquals(3, player.weaponCount)
        // A permanent tier is not driven by any countdown — the tier dictates the
        // barrel count directly, with no temporary weapon timer involved.
    }

    @Test
    fun rapidFireDoesNotAlterWeaponTier() {
        val player = playerAt()
        system.applyEffect(Pickup(id = 1L, type = PickupType.TRIPLE_SHOT, x = 150f, y = 150f), player)
        system.applyEffect(Pickup(id = 2L, type = PickupType.RAPID_FIRE, x = 150f, y = 150f), player)
        assertEquals(3, player.weaponTier)
        assertTrue(player.hasRapidFire)
        // When rapid fire expires, the cooldown returns to normal and the tier stays.
        player.rapidFireTimer = 0f
        assertEquals(3, player.weaponTier)
        assertEquals(GameConfig.PLAYER_FIRE_COOLDOWN, player.fireCooldownDuration, 0.001f)
    }

    @Test
    fun rapidFireSetsTimerAndSpeed() {
        val player = playerAt()
        assertEquals(GameConfig.PLAYER_FIRE_COOLDOWN, player.fireCooldownDuration, 0.001f)
        system.applyEffect(Pickup(id = 1L, type = PickupType.RAPID_FIRE, x = 150f, y = 150f), player)
        assertTrue(player.hasRapidFire)
        assertTrue(player.fireCooldownDuration < GameConfig.PLAYER_FIRE_COOLDOWN)
    }

    @Test
    fun shieldSetsTimer() {
        val player = playerAt()
        system.applyEffect(Pickup(id = 1L, type = PickupType.SHIELD, x = 150f, y = 150f), player)
        assertTrue(player.isShielded)
        assertTrue(player.shieldTimer == GameConfig.SHIELD_DURATION_SECONDS)
    }

    @Test
    fun damageBoostSetsTimerAndDamage() {
        val player = playerAt()
        system.applyEffect(Pickup(id = 1L, type = PickupType.DAMAGE_BOOST, x = 150f, y = 150f), player)
        assertTrue(player.hasDamageBoost)
        assertEquals(GameConfig.PROJ_DAMAGE + GameConfig.DAMAGE_BOOST_BONUS, player.shotDamage)
    }

    @Test
    fun magnetSetsTimerAndPullsPickups() {
        val player = playerAt(x = 200f, y = 200f)
        system.applyEffect(Pickup(id = 1L, type = PickupType.MAGNET, x = 150f, y = 150f), player)
        assertTrue(player.magnetTimer > 0f)
        // A pickup within magnet range should move toward the player.
        val far = Pickup(id = 9L, type = PickupType.HEALTH, x = 130f, y = 130f)
        val moved = system.applyMagnet(listOf(far), player, 0.5f)
        assertTrue(moved[0].centerX > far.centerX)
        assertTrue(moved[0].centerY > far.centerY)
    }

    @Test
    fun noMagnetWhenInactive() {
        val player = playerAt(x = 200f, y = 200f) // magnetTimer == 0
        val far = Pickup(id = 9L, type = PickupType.HEALTH, x = 10f, y = 10f)
        val moved = system.applyMagnet(listOf(far), player, 0.5f)
        assertEquals(far.x, moved[0].x, 0.001f)
        assertEquals(far.y, moved[0].y, 0.001f)
    }

    @Test
    fun scoreMultiplierSetsTimerAndValue() {
        val player = playerAt()
        assertEquals(1, player.scoreMultiplier)
        system.applyEffect(Pickup(id = 1L, type = PickupType.SCORE_MULTIPLIER, x = 150f, y = 150f), player)
        assertTrue(player.hasScoreMultiplier)
        assertEquals(GameConfig.SCORE_MULTIPLIER_VALUE, player.scoreMultiplier)
    }

    @Test
    fun extraLifeIsCappedAtMax() {
        val player = playerAt()
        repeat(10) {
            system.applyEffect(Pickup(id = it.toLong(), type = PickupType.EXTRA_LIFE, x = 150f, y = 150f), player)
        }
        assertEquals(GameConfig.MAX_EXTRA_LIVES, player.extraLives)
    }

    // --- Collection -------------------------------------------------------

    @Test
    fun collectDetectsOverlapWithPlayer() {
        val pickup = Pickup(id = 1L, type = PickupType.HEALTH, x = 150f, y = 150f)
        val collected = system.collectIfPossible(listOf(pickup), playerAt(x = 150f, y = 150f))
        assertNotNull(collected)
        assertEquals(1L, collected!!.id)
    }

    @Test
    fun collectIgnoresDistantPickup() {
        val pickup = Pickup(id = 1L, type = PickupType.HEALTH, x = 10f, y = 10f)
        assertNull(system.collectIfPossible(listOf(pickup), playerAt(x = 200f, y = 200f)))
    }
}