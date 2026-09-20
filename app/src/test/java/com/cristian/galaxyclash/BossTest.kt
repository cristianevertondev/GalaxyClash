package com.cristian.galaxyclash

import com.cristian.galaxyclash.game.Boss
import com.cristian.galaxyclash.game.BossAttack
import com.cristian.galaxyclash.game.BossCatalog
import com.cristian.galaxyclash.game.BossType
import com.cristian.galaxyclash.game.CollisionSystem
import com.cristian.galaxyclash.game.Enemy
import com.cristian.galaxyclash.game.EnemyKind
import com.cristian.galaxyclash.game.GameEngine
import com.cristian.galaxyclash.game.Player
import com.cristian.galaxyclash.game.PlayerInput
import com.cristian.galaxyclash.game.Projectile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BossTest {

    private fun engine() = GameEngine(360f, 640f, Random(0L))

    private fun setField(obj: Any, name: String, value: Any?) {
        val f = obj.javaClass.getDeclaredField(name)
        f.isAccessible = true
        f.set(obj, value)
    }

    private fun boss(type: BossType = BossType.GOLIATH, hp: Int = BossCatalog.spec(BossType.GOLIATH).hp) =
        Boss(type = type, x = 100f, y = 100f, hp = hp, maxHp = BossCatalog.spec(type).hp, fireCooldown = 99f)

    // --- Catalog / architecture -------------------------------------------

    @Test
    fun catalogDefinesMultipleBossTypes() {
        assertTrue(BossType.entries.size >= 3)
        for (type in BossType.entries) {
            assertTrue(BossCatalog.spec(type).hp > 0)
            assertTrue(BossCatalog.spec(type).scoreReward > 0)
        }
    }

    @Test
    fun bossCollisionFunctionsDetectOverlap() {
        val b = boss()
        val proj = Projectile(x = 100f, y = 100f, width = 10f, height = 10f, fromPlayer = true)
        assertTrue(CollisionSystem.projectileHitsBoss(proj, b))
        val player = Player(x = 100f, y = 300f)
        assertTrue(CollisionSystem.bossHitsPlayer(b, Player(x = 100f, y = 100f)))
    }

    // --- Engine integration ------------------------------------------------

    @Test
    fun bossSpawnsBeforeSceneSplit() {
        val e = engine()
        e.resize(360f, 640f)
        // Right at the threshold minus the lead => no boss yet.
        setField(e, "score", 8000)
        e.step(1f / 60f, PlayerInput())
        assertNull(e.snapshot().boss)
        // Inside the "lead" window before the 15000 split => boss appears.
        setField(e, "score", 11000)
        e.step(1f / 60f, PlayerInput())
        assertNotNull(e.snapshot().boss)
    }

    @Test
    fun playerProjectileDamagesBoss() {
        val e = engine()
        e.resize(360f, 640f)
        setField(e, "boss", boss(hp = 10))
        // Inject a friendly shot overlapping the boss.
        val list = e.javaClass.getDeclaredField("projectiles").also { it.isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (list.get(e) as MutableList<Projectile>).add(Projectile(x = 160f, y = 160f, width = 10f, height = 10f, vy = 0f, fromPlayer = true))
        e.step(1f / 60f, PlayerInput(firing = true))
        val b = e.snapshot().boss
        assertNotNull(b)
        assertTrue(b!!.hp < 10)
    }

    @Test
    fun defeatingBossAwardsRewardAndClearsIt() {
        val e = engine()
        e.resize(360f, 640f)
        setField(e, "boss", boss(hp = 1))
        val list = e.javaClass.getDeclaredField("projectiles").also { it.isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (list.get(e) as MutableList<Projectile>).add(Projectile(x = 160f, y = 160f, width = 10f, height = 10f, vy = 0f, fromPlayer = true))
        val scoreBefore = e.snapshot().score
        e.step(1f / 60f, PlayerInput(firing = true))
        assertNull(e.snapshot().boss)
        assertTrue(e.snapshot().score > scoreBefore)
    }

    @Test
    fun bossClearedOnRestart() {
        val e = engine()
        e.resize(360f, 640f)
        setField(e, "boss", boss())
        assertNotNull(e.snapshot().boss)
        e.start()
        assertNull(e.snapshot().boss)
    }

    // --- Boss arena-clearing flow -----------------------------------------

    @Test
    fun bossAppearanceStartsActiveState() {
        val e = engine()
        e.resize(360f, 640f)
        setField(e, "score", 11000)   // just before the first scene split
        e.step(1f / 60f, PlayerInput())
        assertNotNull(e.snapshot().boss)
        assertTrue(bossActiveOf(e))
    }

    @Test
    fun bossWidthFillsTopAndPicksAttack() {
        val e = engine()
        e.resize(360f, 640f)
        setField(e, "score", 11000)
        e.step(1f / 60f, PlayerInput())
        val b = e.snapshot().boss
        assertNotNull(b)
        // Boss scaled to roughly 60% of the play width (fills the top).
        assertTrue(b!!.width >= 360f * 0.5f)
        assertTrue(BossAttack.entries.contains(b.attack))
    }

    @Test
    fun bossHalfHealthAdvancesScene() {
        val e = engine()
        e.resize(360f, 640f)
        e.start()
        // Boss already below half health when it appears.
        setField(e, "boss", boss(hp = BossCatalog.spec(BossType.GOLIATH).hp / 2 - 1))
        setField(e, "bossActive", true)
        assertEquals(0, sceneLevelOf(e))
        e.step(1f / 60f, PlayerInput())
        // Crossing 50% pushes the run into the next space region.
        assertEquals(1, sceneLevelOf(e))
        assertEquals(1, e.snapshot().background.ordinal)
    }

    private fun sceneLevelOf(e: GameEngine): Int {
        val f = e.javaClass.getDeclaredField("sceneLevel")
        f.isAccessible = true
        return f.getInt(e)
    }

    @Test
    fun enemiesDiveAndNothingSpawnsWhileBossActive() {
        val e = engine()
        e.resize(360f, 640f)
        setField(e, "bossActive", true)
        @Suppress("UNCHECKED_CAST")
        val enemies = e.javaClass.getDeclaredField("enemies").also { it.isAccessible = true }.get(e) as MutableList<Enemy>
        enemies.add(Enemy(kind = EnemyKind.BASIC, x = 100f, y = 100f))
        val y0 = enemies[0].y
        repeat(30) { e.step(1f / 60f, PlayerInput()) }   // ~0.5s
        // The ship dived straight down, and no NEW enemies appeared.
        assertTrue(enemies[0].y > y0 + 60f)
        assertEquals(1, enemies.size)
    }

    @Test
    fun bossDefeatEndsActiveState() {
        val e = engine()
        e.resize(360f, 640f)
        setField(e, "bossActive", true)
        setField(e, "boss", boss(hp = 1))
        @Suppress("UNCHECKED_CAST")
        (e.javaClass.getDeclaredField("projectiles").also { it.isAccessible = true }.get(e) as MutableList<Projectile>)
            .add(Projectile(x = 160f, y = 160f, width = 10f, height = 10f, vy = 0f, fromPlayer = true))
        e.step(1f / 60f, PlayerInput(firing = true))
        assertNull(e.snapshot().boss)
        assertFalse(bossActiveOf(e))
    }

    private fun bossActiveOf(e: GameEngine): Boolean {
        val f = e.javaClass.getDeclaredField("bossActive")
        f.isAccessible = true
        return f.getBoolean(e)
    }

    private fun bossSequenceOf(e: GameEngine): Int {
        val f = e.javaClass.getDeclaredField("bossSequence")
        f.isAccessible = true
        return f.getInt(e)
    }

    // --- Boss spacing / progression (growing, far-apart gaps) ---------------

    @Test
    fun firstBossSpawnsNearFirstMilestone() {
        val e = engine()
        e.resize(360f, 640f)
        e.start()
        setField(e, "score", 10000)
        e.step(1f / 60f, PlayerInput())
        assertNull(e.snapshot().boss)                       // below 15000 - 4000 lead
        setField(e, "score", 12000)
        e.step(1f / 60f, PlayerInput())
        assertNotNull(e.snapshot().boss)
        assertEquals(BossType.GOLIATH, e.snapshot().boss!!.type)
    }

    @Test
    fun secondAndThirdBossesComeAtGrowingScores() {
        val e = engine()
        e.resize(360f, 640f)
        e.start()
        val f = 1f / 60f
        // Boss 1 (~15000) -> spawns once score reaches 15000 - 4000 = 11000.
        setField(e, "score", 12000)
        e.step(f, PlayerInput())
        assertEquals(BossType.GOLIATH, e.snapshot().boss!!.type)
        setField(e, "boss", null)
        setField(e, "bossActive", false)
        // Boss 2 targets ~40000 (appears at >= 36000); below that, nothing spawns.
        setField(e, "score", 35000)
        e.step(f, PlayerInput())
        assertNull(e.snapshot().boss)
        setField(e, "score", 37000)
        e.step(f, PlayerInput())
        assertEquals(BossType.HYDRA, e.snapshot().boss!!.type)   // sequence: GOLIATH -> HYDRA
        setField(e, "boss", null)
        setField(e, "bossActive", false)
        // Boss 3 targets ~70000 (appears at >= 66000).
        setField(e, "score", 65000)
        e.step(f, PlayerInput())
        assertNull(e.snapshot().boss)
        setField(e, "score", 68000)
        e.step(f, PlayerInput())
        assertEquals(BossType.VOID_MAW, e.snapshot().boss!!.type)
    }

    @Test
    fun interBossIntervalsKeepGrowing() {
        val e = engine()
        e.resize(360f, 640f)
        e.start()
        val f = 1f / 60f
        val thresholds = listOf(15000, 40000, 70000, 105000, 145000)
        val spawnScore = IntArray(5)
        for (i in 0 until 5) {
            if (i > 0) {
                setField(e, "boss", null)
                setField(e, "bossActive", false)
            }
            // Below the lead window: no boss yet.
            setField(e, "score", thresholds[i] - 4000 - 1)
            e.step(f, PlayerInput())
            assertNull("boss ${i + 1} should not spawn early", e.snapshot().boss)
            // Just past the lead window: the boss must spawn.
            setField(e, "score", thresholds[i] - 4000 + 100)
            e.step(f, PlayerInput())
            assertNotNull("boss ${i + 1} should spawn", e.snapshot().boss)
            spawnScore[i] = e.snapshot().score
        }
        // The gap between consecutive spawn points keeps growing (25k, 30k, 35k, ...).
        val gaps = IntArray(4) { spawnScore[it + 1] - spawnScore[it] }
        for (g in 1 until gaps.size) {
            assertTrue(gaps[g] > gaps[g - 1])
        }
    }

    @Test
    fun bossSequenceLoopsAfterTwenty() {
        val e = engine()
        e.resize(360f, 640f)
        e.start()
        val f = 1f / 60f
        // Boss #20 (index 19) is DREDGE.
        setField(e, "bossSequence", 19)
        setField(e, "score", 10_000_000)
        e.step(f, PlayerInput())
        assertEquals(BossType.DREDGE, e.snapshot().boss!!.type)
        setField(e, "boss", null)
        setField(e, "bossActive", false)
        // Boss #21 (index 20) loops back to GOLIATH.
        setField(e, "bossSequence", 20)
        e.step(f, PlayerInput())
        assertEquals(BossType.GOLIATH, e.snapshot().boss!!.type)
    }

    @Test
    fun noDuplicateOrSimultaneousBosses() {
        val e = engine()
        e.resize(360f, 640f)
        e.start()
        setField(e, "score", 12000)
        e.step(1f / 60f, PlayerInput())
        assertNotNull(e.snapshot().boss)
        val seqAfterFirst = bossSequenceOf(e)
        // Crank the score far past every upcoming threshold while the current boss
        // is still alive: no second boss may spawn and the sequence may not advance.
        setField(e, "score", 500000)
        repeat(10) { e.step(1f / 60f, PlayerInput()) }
        assertNotNull(e.snapshot().boss)
        assertEquals(seqAfterFirst, bossSequenceOf(e))
    }

    @Test
    fun bossNotSkippedEvenWhenScoreVaults() {
        val e = engine()
        e.resize(360f, 640f)
        e.start()
        val f = 1f / 60f
        setField(e, "score", 12000)
        e.step(f, PlayerInput())
        assertEquals(BossType.GOLIATH, e.snapshot().boss!!.type)
        // Defeat it while the score is already far past the next threshold: the
        // next boss in the cycle must still appear — there is no gap in the cycle.
        setField(e, "boss", null)
        setField(e, "bossActive", false)
        setField(e, "score", 500000)
        e.step(f, PlayerInput())
        assertNotNull(e.snapshot().boss)
        assertEquals(BossType.HYDRA, e.snapshot().boss!!.type)
    }
}