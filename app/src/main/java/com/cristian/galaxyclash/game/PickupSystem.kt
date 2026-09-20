package com.cristian.galaxyclash.game

import kotlin.random.Random

/**
 * Spawn, update, magnet and collect logic for ALL pickups. Pure JVM, no Android
 * imports. This is the single drop/collection system — asteroids and bosses
 * reuse it; there is deliberately no parallel "AsteroidPickupSystem".
 *
 * Responsibilities:
 *  - Roll RNG per destroyed entity and maybe return a weighted-random pickup.
 *  - Choose WHICH pickup type to drop based on rarity weights in [GameConfig].
 *  - Apply a pickup's effect to the player (health, weapon tier, timers, lives).
 *  - Age/expire pickups each tick.
 *  - Optionally pull nearby pickups toward the player when MAGNET is active.
 *  - Check player-overlap and report a single collected pickup per tick.
 */
class PickupSystem(
    private val random: Random = Random.Default,
) {

    /** @return a weighted-random [PickupType] (respects common/rare/extremely-rare). */
    fun rollType(): PickupType {
        val entries = PickupType.entries
        val total = entries.sumOf { GameConfig.pickupWeight(it) }
        if (total <= 0) return PickupType.HEALTH
        var roll = random.nextInt(total)
        for (type in entries) {
            roll -= GameConfig.pickupWeight(type)
            if (roll < 0) return type
        }
        return entries.last()
    }

    /** Roll a drop for a destroyed enemy of [kind]; null if none should spawn. */
    fun maybeSpawnOnDestroy(
        kind: EnemyKind,
        x: Float,
        y: Float,
        playWidth: Float,
        playHeight: Float,
    ): Pickup? {
        val chance = GameConfig.pickupChanceFor(kind)
        return maybeSpawn(chance, x, y, playWidth, playHeight)
    }

    /** Roll a drop for a destroyed asteroid; null if none should spawn. */
    fun maybeSpawnFromAsteroid(
        x: Float,
        y: Float,
        playWidth: Float,
        playHeight: Float,
    ): Pickup? = maybeSpawn(GameConfig.ASTEROID_BASE_DROP_CHANCE, x, y, playWidth, playHeight)

    private fun maybeSpawn(
        chance: Float,
        x: Float,
        y: Float,
        playWidth: Float,
        playHeight: Float,
    ): Pickup? {
        if (chance <= 0f) return null
        if (random.nextFloat() >= chance) return null
        return newPickup(rollType(), x, y, playWidth, playHeight)
    }

    /** Create a fresh pickup of [type] at (x,y), clamped fully inside the play area. */
    fun newPickup(
        type: PickupType,
        x: Float,
        y: Float,
        playWidth: Float,
        playHeight: Float,
    ): Pickup {
        val half = GameConfig.PICKUP_SIZE / 2f
        val w = GameConfig.PICKUP_SIZE
        return Pickup(
            id = random.nextLong(),
            type = type,
            x = (x - half).coerceIn(0f, (playWidth - w).coerceAtLeast(0f)),
            y = (y - half).coerceIn(0f, (playHeight - w).coerceAtLeast(0f)),
        )
    }

    /**
     * Age all pickups one tick and return the updated list (expired removed).
     */
    fun tick(pickups: List<Pickup>, dt: Float): List<Pickup> = pickups
        .map { p -> p.copy(age = p.age + dt) }
        .filter { p -> !p.isExpired && !p.collected }

    /**
     * When MAGNET is active, pull each pickup within range toward the player.
     * Returns the updated list with positions moved toward the player's center.
     * No runaway: pull is bounded by [GameConfig.MAGNET_SPEED] and only acts on
     * reachable (in-range) pickups.
     */
    fun applyMagnet(pickups: List<Pickup>, player: Player, dt: Float): List<Pickup> {
        if (player.magnetTimer <= 0f || dt <= 0f) return pickups
        val range = GameConfig.MAGNET_RANGE
        val maxMove = GameConfig.MAGNET_SPEED * dt
        return pickups.map { p ->
            val dx = player.centerX - p.centerX
            val dy = player.centerY - p.centerY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist <= 0f || dist > range) {
                p
            } else {
                // Speed scales with proximity: closer = stronger pull.
                val pull = maxMove * (1f - (dist / range).coerceIn(0.15f, 1f))
                val step = pull.coerceAtMost(pull)
                p.copy(
                    x = (p.x + dx / dist * (pull * 0.5f)),
                    y = (p.y + dy / dist * (pull * 0.5f)),
                )
            }
        }
    }

    /**
     * Check collisions with the player; return the pickup (if any) collected.
     * Only one pickup is collected per tick.
     */
    fun collectIfPossible(pickups: List<Pickup>, player: Player): Pickup? {
        for (pickup in pickups) {
            if (pickup.collected) continue
            if (CollisionSystem.projectileHitsPlayer(
                    p = pickupAsCollisionBox(pickup),
                    player = player,
                )
            ) {
                return pickup
            }
        }
        return null
    }

    /** Apply [pickup]'s effect to [player] in place (mutates the passed object). */
    fun applyEffect(pickup: Pickup, player: Player) {
        when (pickup.type) {
            PickupType.HEALTH ->
                player.hp = minOf(player.hp + GameConfig.HEALTH_PICKUP_HEAL, GameConfig.PLAYER_MAX_HP)

            PickupType.DOUBLE_SHOT -> setWeaponTier(player, 2)
            PickupType.TRIPLE_SHOT -> setWeaponTier(player, 3)
            PickupType.QUAD_SHOT -> setWeaponTier(player, 4)

            PickupType.RAPID_FIRE ->
                player.rapidFireTimer = GameConfig.RAPID_FIRE_DURATION_SECONDS

            PickupType.SHIELD ->
                player.shieldTimer = GameConfig.SHIELD_DURATION_SECONDS

            PickupType.DAMAGE_BOOST ->
                player.damageBoostTimer = GameConfig.DAMAGE_BOOST_DURATION_SECONDS

            PickupType.MAGNET ->
                player.magnetTimer = GameConfig.MAGNET_DURATION_SECONDS

            PickupType.SCORE_MULTIPLIER ->
                player.scoreMultiplierTimer = GameConfig.SCORE_MULTIPLIER_DURATION_SECONDS

            PickupType.EXTRA_LIFE ->
                player.extraLives = minOf(player.extraLives + 1, GameConfig.MAX_EXTRA_LIVES)
        }
    }

    /** Raise the weapon to [tier] — permanently, never downgrades (monotonic). */
    private fun setWeaponTier(player: Player, tier: Int) {
        player.weaponTier = maxOf(player.weaponTier, tier).coerceIn(1, 4)
    }

    private fun pickupAsCollisionBox(pickup: Pickup): Projectile =
        Projectile(
            x = pickup.x, y = pickup.y,
            width = pickup.width, height = pickup.height,
            vx = 0f, vy = 0f, damage = 0, fromPlayer = false,
        )
}