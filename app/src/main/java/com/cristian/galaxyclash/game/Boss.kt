package com.cristian.galaxyclash.game

/**
 * Boss archetypes. There are 20 of them and the endless run cycles through
 * them in order (one per stage) so each boss encounter feels new. A boss is
 * purely data — behavior lives in the engine (arena-clearing + arrival + firing).
 */
enum class BossType {
    GOLIATH, HYDRA, VOID_MAW, TITAN, REAVER,
    SPECTER, JUGGERNAUT, ORBITER, DECIMATOR, PHANTOM,
    TYRANT, COLOSSUS, WRAITH, LEVIATHAN, NEXUS,
    ABYSS, GARGOYLE, FENRIR, HORNET, DREDGE,
}

/**
 * Boss attack patterns; one is picked at random when the boss spawns so each
 * encounter feels different.
 */
enum class BossAttack {
    /** A single laser aimed straight at the player. */
    LASER,
    /** A fan of aimed lasers. */
    SPRAY,
    /** A row of parallel (vertical) lasers across the width. */
    PARALLEL,
    /** Slow balls that bounce off the side walls. */
    PINGPONG,
    /** Double laser shot (two lasers side‑by‑side). */
    DOUBLE_LASER,
    /** Triple laser shot. */
    TRIPLE_LASER,
    /** Lightning strike (a vertical electric bolt that does AoE damage). */
    LIGHTNING
}

/** Config data for a boss (no behavior here — behavior lives in the engine). */
data class BossSpec(
    val type: BossType,
    val name: String,
    val hp: Int,
    val width: Float,
    val height: Float,
    val baseSpeed: Float,
    val shootCooldown: Float,
    val projectileSpeed: Float,
    val scoreReward: Int,
)

/** Central boss config table — 20 bosses, steadily tougher as the run advances. */
object BossCatalog {
    private fun spec(
        type: BossType,
        name: String,
        hp: Int,
        width: Float,
        height: Float,
        speed: Float,
        cooldown: Float,
        projSpeed: Float,
        reward: Int,
    ) = BossSpec(type, name, hp, width, height, speed, cooldown, projSpeed, reward)

    private val specs: Map<BossType, BossSpec> = mapOf(
        BossType.GOLIATH to spec(BossType.GOLIATH, "GOLIATH", hp = 160, width = 150f, height = 140f, speed = 55f, cooldown = 1.4f, projSpeed = 300f, reward = 8000),
        BossType.HYDRA to spec(BossType.HYDRA, "HYDRA", hp = 260, width = 180f, height = 130f, speed = 65f, cooldown = 1.15f, projSpeed = 340f, reward = 12000),
        BossType.VOID_MAW to spec(BossType.VOID_MAW, "VOID MAW", hp = 360, width = 210f, height = 170f, speed = 50f, cooldown = 0.95f, projSpeed = 370f, reward = 16000),
        BossType.TITAN to spec(BossType.TITAN, "TITAN", hp = 460, width = 240f, height = 170f, speed = 60f, cooldown = 1.0f, projSpeed = 360f, reward = 18000),
        BossType.REAVER to spec(BossType.REAVER, "REAVER", hp = 560, width = 200f, height = 150f, speed = 70f, cooldown = 0.9f, projSpeed = 380f, reward = 20000),
        BossType.SPECTER to spec(BossType.SPECTER, "SPECTER", hp = 660, width = 220f, height = 160f, speed = 55f, cooldown = 0.8f, projSpeed = 390f, reward = 22000),
        BossType.JUGGERNAUT to spec(BossType.JUGGERNAUT, "JUGGERNAUT", hp = 760, width = 300f, height = 180f, speed = 45f, cooldown = 1.1f, projSpeed = 340f, reward = 24000),
        BossType.ORBITER to spec(BossType.ORBITER, "ORBITER", hp = 860, width = 250f, height = 160f, speed = 65f, cooldown = 0.85f, projSpeed = 400f, reward = 26000),
        BossType.DECIMATOR to spec(BossType.DECIMATOR, "DECIMATOR", hp = 960, width = 280f, height = 180f, speed = 55f, cooldown = 0.9f, projSpeed = 380f, reward = 28000),
        BossType.PHANTOM to spec(BossType.PHANTOM, "PHANTOM", hp = 1060, width = 230f, height = 170f, speed = 70f, cooldown = 0.75f, projSpeed = 410f, reward = 30000),
        BossType.TYRANT to spec(BossType.TYRANT, "TYRANT", hp = 1160, width = 320f, height = 190f, speed = 50f, cooldown = 1.0f, projSpeed = 360f, reward = 32000),
        BossType.COLOSSUS to spec(BossType.COLOSSUS, "COLOSSUS", hp = 1260, width = 360f, height = 190f, speed = 42f, cooldown = 1.15f, projSpeed = 340f, reward = 34000),
        BossType.WRAITH to spec(BossType.WRAITH, "WRAITH", hp = 1360, width = 260f, height = 170f, speed = 65f, cooldown = 0.8f, projSpeed = 400f, reward = 36000),
        BossType.LEVIATHAN to spec(BossType.LEVIATHAN, "LEVIATHAN", hp = 1460, width = 340f, height = 200f, speed = 48f, cooldown = 0.95f, projSpeed = 380f, reward = 38000),
        BossType.NEXUS to spec(BossType.NEXUS, "NEXUS", hp = 1560, width = 300f, height = 180f, speed = 55f, cooldown = 0.85f, projSpeed = 420f, reward = 40000),
        BossType.ABYSS to spec(BossType.ABYSS, "ABYSS", hp = 1660, width = 320f, height = 190f, speed = 50f, cooldown = 0.9f, projSpeed = 410f, reward = 42000),
        BossType.GARGOYLE to spec(BossType.GARGOYLE, "GARGOYLE", hp = 1760, width = 350f, height = 185f, speed = 45f, cooldown = 1.0f, projSpeed = 360f, reward = 44000),
        BossType.FENRIR to spec(BossType.FENRIR, "FENRIR", hp = 1860, width = 290f, height = 185f, speed = 58f, cooldown = 0.8f, projSpeed = 420f, reward = 46000),
        BossType.HORNET to spec(BossType.HORNET, "HORNET", hp = 1960, width = 240f, height = 165f, speed = 72f, cooldown = 0.7f, projSpeed = 430f, reward = 48000),
        BossType.DREDGE to spec(BossType.DREDGE, "DREDGE", hp = 2060, width = 360f, height = 200f, speed = 44f, cooldown = 0.95f, projSpeed = 400f, reward = 50000),
    )

    fun spec(type: BossType): BossSpec = specs[type] ?: specs.getValue(BossType.GOLIATH)
}

/**
 * A single boss currently on the play field (null when none is active). Moves in
 * a pattern, fires aimed shots, and must be destroyed to continue.
 */
data class Boss(
    val type: BossType,
    var x: Float,
    var y: Float,
    var hp: Int,
    val maxHp: Int,
    var fireCooldown: Float,
    var phase: Float = 0f,
    var direction: Int = 1,
    /** True once a reward/score has been granted for defeating it. */
    var defeated: Boolean = false,
    /** True once the boss has passed 50% HP (triggers the scene advance). */
    var halfTriggered: Boolean = false,
    /** Attack pattern picked when the boss spawns. */
    val attack: BossAttack = BossAttack.LASER,
    /** Visual/hitbox size multiplier so the boss fills the top of the screen. */
    val scale: Float = 1f,
) {
    val spec: BossSpec get() = BossCatalog.spec(type)
    val width: Float get() = spec.width * scale
    val height: Float get() = spec.height * scale
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    val isAlive: Boolean get() = hp > 0
}