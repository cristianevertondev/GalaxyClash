package com.cristian.galaxyclash.game

import kotlin.random.Random

/**
 * Immutable read-only view of the whole simulation, rebuilt once per frame and
 * handed to the renderer. The UI never touches the engine's mutable internals.
 */
data class GameSnapshot(
    val player: Player,
    val enemies: List<Enemy>,
    val asteroids: List<Asteroid>,
    val projectiles: List<Projectile>,
    val particles: List<Particle>,
    val stars: List<Star>,
    val score: Int,
    val enemiesDestroyed: Int,
    val level: Int,
    val stageName: String,
    val background: SpaceBackgroundType,
    val pickups: List<Pickup>,
    /** Currently active boss; null when none is on the field. */
    val boss: Boss?,
)

/**
 * Frame inputs from the UI, applied on the next [step]. Kept as a tiny data
 * class so the engine's update signature stays stable as controls evolve.
 */
data class PlayerInput(
    val movingLeft: Boolean = false,
    val movingRight: Boolean = false,
    val movingUp: Boolean = false,
    val movingDown: Boolean = false,
    val firing: Boolean = false,
    /** Horizontal drag target (px). When set, the ship steers toward the finger. */
    val pointerX: Float? = null,
    /** Vertical drag target (px). When set, the ship steers toward the finger. */
    val pointerY: Float? = null,
)

/**
 * Central simulation. Owns all game state and advances it deterministically on
 * each [step] call with a fixed frame duration. No Android/Compose imports, so
 * the whole thing runs under plain JVM unit tests.
 *
 * Design notes:
 *  - One coordinate system: pixels, origin top-left. The renderer consumes the
 *    snapshot and draws in the same space.
 *  - One `step(deltaSeconds)` advances EVERYTHING (player, projectiles, enemies,
 *    spawns, collisions, score, effects) — frame-rate independent, no timer soup.
 *  - Randomness is injected so tests (and future seeded replays) are deterministic.
 *  - Enemy movement is delegated to [EnemyAISystem]; the engine only integrates.
 */
class GameEngine(
    private var playWidth: Float,
    private var playHeight: Float,
    private val random: Random = Random.Default,
) {

    enum class EngineStatus { RUNNING, PAUSED, GAME_OVER }

    private var player = Player(
        x = playWidth / 2f - GameConfig.PLAYER_WIDTH / 2f,
        y = spawnPlayerY(),
    )
    private val enemies = mutableListOf<Enemy>()
    private val asteroids = mutableListOf<Asteroid>()
    private val projectiles = mutableListOf<Projectile>()
    private val particles = mutableListOf<Particle>()
    private var pickups = mutableListOf<Pickup>()
    private val pickupSystem = PickupSystem(random)
    private val stars = generateStars()
    /** Currently active boss (null when no boss is on the field). */
    private var boss: Boss? = null
    /** The current space region (0 = first). A boss fight advances it at half HP. */
    private var sceneLevel = 0
    /** Counter of how many bosses have been fought (cycles the 20 boss types). */
    private var bossSequence = 0
    /**
     * True from the moment a boss appears until it is defeated. While active,
     * normal enemies and asteroids are cleared off the screen and no new ones
     * spawn — a clean arena for the boss fight.
     */
    private var bossActive = false

    private var score = 0
    private var enemiesDestroyed = 0
    private var elapsed = 0f
    private var spawnTimer = 0f
    private var asteroidTimer = 2f
    private var status = EngineStatus.RUNNING

    // Tracks whether the player has been hand-positioned against the *real* play
    // area yet. The engine is constructed with placeholder dimensions (before
    // layout), so the first `resize` must re-seat the ship at its true spawn.
    private var positionedForLayout = false

    // ---------------------------------------------------------------- public

    fun start() {
        player = Player(
            x = playWidth / 2f - GameConfig.PLAYER_WIDTH / 2f,
            y = spawnPlayerY(),
        )
        enemies.clear()
        asteroids.clear()
        projectiles.clear()
        particles.clear()
        pickups.clear()
        score = 0
        enemiesDestroyed = 0
        elapsed = 0f
        spawnTimer = 0f
        asteroidTimer = 2f
        boss = null
        sceneLevel = 0
        bossSequence = 0
        bossActive = false
        status = EngineStatus.RUNNING
    }

    fun status(): EngineStatus = status

    /** Called by the UI once layout is known (handles rotation / size changes). */
    fun resize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        playWidth = width
        playHeight = height

        if (!positionedForLayout) {
            // First real layout: re-seat the ship at its true spawn (centered
            // horizontally, near the bottom). Fixes the race where the engine is
            // built with placeholder dims before layout.
            positionedForLayout = true
            player = player.copy(
                x = playWidth / 2f - player.width / 2f,
                y = spawnPlayerY(),
            )
        } else {
            // Subsequent resizes (rotation): preserve position by re-clamping into
            // the new play area.
            player = player.copy(
                x = player.x.coerceIn(0f, playWidth - player.width),
                y = player.y.coerceIn(0f, playHeight - player.height),
            )
        }
    }

    fun setPaused(paused: Boolean) {
        if (status != EngineStatus.GAME_OVER) {
            status = if (paused) EngineStatus.PAUSED else EngineStatus.RUNNING
        }
    }

    /** Advances the simulation by [deltaSeconds] if running. No-op otherwise. */
    fun step(deltaSeconds: Float, input: PlayerInput = PlayerInput()) {
        if (status != EngineStatus.RUNNING) return
        val dt = deltaSeconds.coerceAtMost(0.05f) // clamp huge frames (debugger pauses)

        elapsed += dt
        stepPlayer(dt, input)
        stepProjectiles(dt)
        stepEnemies(dt)
        stepAsteroids(dt)
        maybeSpawnBoss()
        stepBoss(dt)
        stepStars(dt)
        stepSpawning(dt)
        stepParticles(dt)
        stepPickups(dt)
        resolveCollisions()
        pruneEntities()
    }

    fun snapshot(): GameSnapshot {
        val stage = LevelSystem.stageFor(elapsed)
        return GameSnapshot(
            player = player,
            enemies = enemies.toList(),
            asteroids = asteroids.toList(),
            projectiles = projectiles.toList(),
            particles = particles.toList(),
            stars = stars.toList(),
            score = score,
            enemiesDestroyed = enemiesDestroyed,
            level = stage.index,
            stageName = stage.name,
            background = sceneTypeFor(sceneLevel),
            pickups = pickups.toList(),
            boss = boss,
        )
    }

    // ------------------------------------------------------------- internals

    private fun stepPlayer(dt: Float, input: PlayerInput) {
        // Horizontal: drag target first (steers toward finger), else hold state.
        val tx = input.pointerX
        if (tx != null) {
            val desiredCenter = tx.coerceIn(
                player.width / 2f,
                playWidth - player.width / 2f,
            )
            val dx = desiredCenter - player.centerX
            val stepLimit = GameConfig.PLAYER_SPEED * dt
            val newX = player.x + dx.coerceIn(-stepLimit, stepLimit)
            player = player.copy(x = newX.coerceIn(0f, playWidth - player.width))
        } else {
            var vx = 0f
            if (input.movingLeft) vx -= GameConfig.PLAYER_SPEED
            if (input.movingRight) vx += GameConfig.PLAYER_SPEED
            player = player.copy(
                x = (player.x + vx * dt).coerceIn(0f, playWidth - player.width),
                movingLeft = input.movingLeft,
                movingRight = input.movingRight,
            )
        }

        // Vertical: free movement across the WHOLE play area, clamped to the
        // screen edges only. Drag target first, else hold state.
        val ty = input.pointerY
        if (ty != null) {
            val desiredCenterY = ty.coerceIn(
                player.height / 2f,
                playHeight - player.height / 2f,
            )
            val dy = desiredCenterY - player.centerY
            val stepLimit = GameConfig.PLAYER_SPEED * dt
            val newY = player.y + dy.coerceIn(-stepLimit, stepLimit)
            player = player.copy(y = newY.coerceIn(0f, playHeight - player.height))
        } else {
            var vy = 0f
            if (input.movingUp) vy -= GameConfig.PLAYER_SPEED
            if (input.movingDown) vy += GameConfig.PLAYER_SPEED
            player = player.copy(
                y = (player.y + vy * dt).coerceIn(0f, playHeight - player.height),
            )
        }

        // Fire control (weaponCount barrels, honored at rapid rate when active).
        player = player.copy(fireCooldown = (player.fireCooldown - dt).coerceAtLeast(0f))
        if (input.firing && player.fireCooldown <= 0f) {
            firePlayerShots()
            player = player.copy(fireCooldown = player.fireCooldownDuration)
        }

        // Invulnerability timer.
        player = player.copy(invulnTimer = (player.invulnTimer - dt).coerceAtLeast(0f))

        // Timed power-up countdowns.
        player = player.copy(
            rapidFireTimer = (player.rapidFireTimer - dt).coerceAtLeast(0f),
            shieldTimer = (player.shieldTimer - dt).coerceAtLeast(0f),
            damageBoostTimer = (player.damageBoostTimer - dt).coerceAtLeast(0f),
            magnetTimer = (player.magnetTimer - dt).coerceAtLeast(0f),
            scoreMultiplierTimer = (player.scoreMultiplierTimer - dt).coerceAtLeast(0f),
        )
    }

    /** Spawns [Player.weaponCount] barrels (single/double/triple/quad shot). */
    private fun firePlayerShots() {
        val count = player.weaponCount
        val damage = player.shotDamage
        val gap = GameConfig.PROJ_WIDTH * 1.8f
        val baseY = player.y - GameConfig.PROJ_HEIGHT
        for (i in 0 until count) {
            // Evenly space the barrels around the ship center.
            val offset = when (count) {
                2 -> if (i == 0) -gap else gap
                3 -> when (i) { 0 -> -gap; 1 -> 0f; else -> gap }
                4 -> when (i) { 0 -> -gap * 1.5f; 1 -> -gap * 0.5f; 2 -> gap * 0.5f; else -> gap * 1.5f }
                else -> 0f
            }
            val x = player.centerX + offset
            // Outer barrels fan outward slightly for a classic shmup spread.
            val isCenter = count == 1 || (count == 3 && i == 1)
            val vy = -GameConfig.PROJ_SPEED
            val spread: Float = if (isCenter) 0f else {
                val sideways = if (offset < 0f) -GameConfig.PROJ_SPREAD_ANGLE else GameConfig.PROJ_SPREAD_ANGLE
                sideways
            }
            val vx = kotlin.math.tan(spread) * -vy
            projectiles += Projectile(
                x = x - GameConfig.PROJ_WIDTH / 2f,
                y = baseY,
                vx = vx,
                vy = vy,
                damage = damage,
                fromPlayer = true,
            )
        }
    }

    private fun spawnPlayerY(): Float = playHeight * 0.86f

    private fun stepProjectiles(dt: Float) {
        projectiles.replaceAll { p ->
            var np = p.copy(x = p.x + p.vx * dt, y = p.y + p.vy * dt)
            if (np.bounce) {
                // Ping-pong balls: reverse horizontal direction at the side walls.
                if (np.x <= 0f) np = np.copy(x = 0f, vx = kotlin.math.abs(np.vx))
                if (np.x + np.width >= playWidth) {
                    np = np.copy(x = playWidth - np.width, vx = -kotlin.math.abs(np.vx))
                }
            }
            np
        }
    }

    private fun stepEnemies(dt: Float) {
        for (e in enemies) {
            if (bossActive) {
                // Clear the arena for the boss: every ship dives straight down,
                // stops strafing, and stops shooting until it flies off-screen.
                e.vx = 0f
                e.vy = GameConfig.ENEMY_FLEE_SPEED
            } else {
                EnemyAISystem.update(
                    enemy = e,
                    dt = dt,
                    playWidth = playWidth,
                    playerX = player.centerX,
                    playerY = player.centerY,
                    random = random,
                )
            }
            // Integrate position from the velocity chosen by the AI / flee.
            e.x = (e.x + e.vx * dt).coerceIn(0f, playWidth - e.width)
            e.y = e.y + e.vy * dt

            // Hostile fire (disabled while fleeing for the boss).
            if (!bossActive && EnemyAISystem.wantsToShoot(e, dt)) {
                fireEnemyProjectile(e)
            }
        }
    }

    /** Fires a single aimed shot from [e] toward the player's current position. */
    private fun fireEnemyProjectile(e: Enemy) {
        val spec = EnemyCatalog.spec(e.kind)
        val dx = player.centerX - e.centerX
        val dy = player.centerY - e.centerY
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val speed = spec.projectileSpeed
        val vx = dx / len * speed
        val vy = dy / len * speed
        projectiles += Projectile(
            x = e.centerX - GameConfig.ENEMY_PROJ_WIDTH / 2f,
            y = e.centerY,
            width = GameConfig.ENEMY_PROJ_WIDTH,
            height = GameConfig.ENEMY_PROJ_HEIGHT,
            vx = vx,
            vy = vy,
            damage = GameConfig.ENEMY_PROJ_DAMAGE,
            fromPlayer = false,
        )
    }

    private fun stepAsteroids(dt: Float) {
        for (a in asteroids) {
            // While the boss approaches, rocks tumble away faster to clear the arena.
            val boost = if (bossActive) GameConfig.ASTEROID_FLEE_MULT else 1f
            a.x += a.vx * dt
            a.y += a.vy * boost * dt
            a.rotation += a.rotSpeed * dt
        }
    }

    // --- Boss -----------------------------------------------------------------

    /** The boss arrives just BEFORE its target score, so the arena-clearing fight
     *  doubles as the session transition. No upper bound: the moment the score
     *  reaches target - lead, the boss spawns — it can never be skipped even if a
     *  big reward vaults the score past the exact milestone. */
    private fun maybeSpawnBoss() {
        if (boss != null) return
        val target = nextBossScore()
        if (score < target - GameConfig.BOSS_SCENE_LEAD) return

        val type = BossType.entries[bossSequence % BossType.entries.size]
        bossSequence++
        val spec = BossCatalog.spec(type)
        // Fill the top of the screen: scale the boss so its width is a fraction
        // of the play field, however big the device is.
        val scale = (playWidth * GameConfig.BOSS_WIDTH_FRACTION / spec.width).coerceAtLeast(1f)
        boss = Boss(
            type = type,
            x = playWidth / 2f - spec.width * scale / 2f,
            y = -spec.height * scale,
            hp = spec.hp,
            maxHp = spec.hp,
            fireCooldown = 2f,
            scale = scale,
            attack = randomBossAttack(),
        )
        // Clean arena: existing enemies flee, asteroids tumble away, no new spawns.
        bossActive = true
    }

    /**
     * Score threshold at which the NEXT boss is due, growing faster over the run.
     * Boss 1 ~15k, boss 2 ~40k, boss 3 ~70k, boss 4 ~105k, boss 5 ~145k … each
     * gap widens by [GameConfig.BOSS_GAP_STEP] until it reaches the
     * [GameConfig.BOSS_MAX_GAP] ceiling, so late bosses (past the 20 archetypes,
     * which loop) keep a steady, far-apart, infinite pace.
     */
    private fun nextBossScore(): Int {
        val fought = bossSequence          // how many bosses have spawned already
        if (fought == 0) return GameConfig.BOSS_1_SCORE
        var target = GameConfig.BOSS_1_SCORE
        // Accumulate the growing, capped gaps between consecutive bosses.
        for (j in 1..fought) {
            val gap = (GameConfig.BOSS_GAP_START + (j - 1) * GameConfig.BOSS_GAP_STEP)
                .coerceAtMost(GameConfig.BOSS_MAX_GAP)
            target += gap
        }
        return target
    }

    private fun randomBossAttack(): BossAttack =
        BossAttack.entries[random.nextInt(BossAttack.entries.size)]

    private fun stepBoss(dt: Float) {
        val b = boss ?: return
        if (!b.isAlive) {
            boss = null
            return
        }
        // Half health: push the run into the next space region (the "session
        // change" happens behind the fight). Rewards the player for the mid-fight.
        if (!b.halfTriggered && b.hp <= b.maxHp / 2) {
            b.halfTriggered = true
            sceneLevel++
        }
        b.phase += dt

        // Descend into its holding line, then bob horizontally.
        val targetY = playHeight * 0.16f
        if (b.y < targetY) {
            b.y = kotlin.math.min(b.y + b.spec.baseSpeed * 3f * dt, targetY)
        }
        b.x += b.direction * b.spec.baseSpeed * dt
        if (b.x <= 0f || b.x + b.width >= playWidth) {
            b.direction *= -1
            b.x = b.x.coerceIn(0f, playWidth - b.width)
        }

        // Fires aimed shots at the player.
        b.fireCooldown -= dt
        if (b.fireCooldown <= 0f) {
            fireBossShot(b)
            b.fireCooldown = b.spec.shootCooldown
        }
    }

    private fun fireBossShot(b: Boss) {
            when (b.attack) {
                BossAttack.LASER -> fireBossLaser(b)
                BossAttack.SPRAY -> repeat(5) { i -> fireBossLaser(b, spread = (i - 2) * 0.22f) }
                BossAttack.PARALLEL -> {
                    // Vertical laser wall across the boss's width.
                    val count = 6
                    for (i in 0 until count) {
                        val fx = b.x + (i + 1f) / (count + 1f) * b.width
                        projectiles += Projectile(
                            x = fx - GameConfig.ENEMY_PROJ_WIDTH / 2f,
                            y = b.y + b.height * 0.5f,
                            width = GameConfig.ENEMY_PROJ_WIDTH * 1.4f,
                            height = GameConfig.ENEMY_PROJ_HEIGHT,
                            vx = 0f,
                            vy = b.spec.projectileSpeed * 0.9f,
                            damage = GameConfig.ENEMY_PROJ_DAMAGE,
                            fromPlayer = false,
                        )
                    }
                }
                BossAttack.PINGPONG -> {
                    // Two slow balls that ricochet off the side walls.
                    for (dir in intArrayOf(-1, 1)) {
                        projectiles += Projectile(
                            x = b.centerX - GameConfig.ENEMY_PROJ_WIDTH,
                            y = b.centerY,
                            width = GameConfig.ENEMY_PROJ_WIDTH * 2f,
                            height = GameConfig.ENEMY_PROJ_WIDTH * 2f,
                            vx = dir * b.spec.projectileSpeed * 0.62f,
                            vy = b.spec.projectileSpeed * 0.62f,
                            damage = GameConfig.ENEMY_PROJ_DAMAGE,
                            fromPlayer = false,
                            bounce = true,
                        )
                    }
                }
            BossAttack.DOUBLE_LASER -> {
                // Two aimed lasers, slightly side-by-side.
                repeat(2) { i -> fireBossLaser(b, spread = (i - 0.5f) * 0.12f) }
            }
            BossAttack.TRIPLE_LASER -> {
                // Three-laser fan.
                repeat(3) { i -> fireBossLaser(b, spread = (i - 1) * 0.14f) }
            }
            BossAttack.LIGHTNING -> {
                // Vertical electric bolt dropped straight onto the player's
                // column — high damage, no bounce.
                projectiles += Projectile(
                    x = player.centerX - GameConfig.ENEMY_PROJ_WIDTH / 2f,
                    y = b.centerY + b.height * 0.4f,
                    width = GameConfig.ENEMY_PROJ_WIDTH,
                    height = GameConfig.ENEMY_PROJ_HEIGHT,
                    vx = 0f,
                    vy = b.spec.projectileSpeed * 1.4f,
                    damage = GameConfig.ENEMY_PROJ_DAMAGE * 3,
                    fromPlayer = false,
                )
            }
        }
        }

        private fun fireBossLaser(b: Boss, spread: Float = 0f) {
            val dx = player.centerX - b.centerX
            val dy = player.centerY - b.centerY
            val ang = (kotlin.math.atan2(dy, dx) + spread)
            val speed = b.spec.projectileSpeed
            projectiles += Projectile(
                x = b.centerX - GameConfig.ENEMY_PROJ_WIDTH / 2f,
                y = b.centerY + b.height * 0.3f,
                width = GameConfig.ENEMY_PROJ_WIDTH,
                height = GameConfig.ENEMY_PROJ_HEIGHT,
                vx = kotlin.math.cos(ang).toFloat() * speed,
                vy = kotlin.math.sin(ang).toFloat() * speed,
                damage = GameConfig.ENEMY_PROJ_DAMAGE,
                fromPlayer = false,
            )
        }

    private fun onBossDefeated(b: Boss) {
        score += b.spec.scoreReward * player.scoreMultiplier
        b.defeated = true
        // Back to the normal endless flow once the boss falls.
        bossActive = false
        // Big explosion.
        repeat(8) { i ->
            particles += Particle(
                x = b.centerX + 50f * ((i % 4) - 1.5f),
                y = b.centerY + 50f * ((i / 4) - 0.5f),
                size = 60f,
                lifetime = 0.5f,
            )
        }
        // Guaranteed reward drop (a random power-up).
        pickups += pickupSystem.newPickup(
            pickupSystem.rollType(),
            b.centerX, b.centerY,
            playWidth, playHeight,
        )
        boss = null
    }

    private fun stepStars(dt: Float) {
        stars.replaceAll { s ->
            val ny = (s.ny + s.speed * dt) % 1f
            s.copy(ny = ny)
        }
    }

    private fun stepPickups(dt: Float) {
        // Magnet: pull nearby pickups toward the player (if active).
        pickups = pickupSystem.applyMagnet(pickups, player, dt).toMutableList()

        // Age pickups and drop expired ones.
        val current = pickups.toList()
        pickups.clear()
        pickups.addAll(pickupSystem.tick(current, dt))

        // Collision detection with the player; apply the collected effect.
        val collected = pickupSystem.collectIfPossible(pickups, player)
        if (collected != null) {
            pickupSystem.applyEffect(collected, player)
            pickups.removeAll { it.id == collected.id }
        }
    }

    private fun stepSpawning(dt: Float) {
        // No new enemies or asteroids while a boss fight is clearing / running.
        if (bossActive) return
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer = LevelSystem.stageFor(elapsed).spawnInterval
            spawnEnemy()
        }
        asteroidTimer -= dt
        if (asteroidTimer <= 0f) {
            asteroidTimer = GameConfig.ASTEROID_SPAWN_INTERVAL
            maybeSpawnAsteroid()
        }
    }

    private fun maybeSpawnAsteroid() {
        val level = LevelSystem.levelFor(elapsed)
        val chance = (GameConfig.ASTEROID_SPAWN_BASE_CHANCE + (level - 1) * 0.06f)
            .coerceAtMost(0.92f)
        if (random.nextFloat() >= chance) return
        // Large rocks are rarer than medium.
        val largeWeight = GameConfig.ASTEROID_LARGE_WEIGHT
        val mediumWeight = 100
        val size = if (random.nextInt(largeWeight + mediumWeight) < largeWeight) {
            AsteroidSize.LARGE
        } else {
            AsteroidSize.MEDIUM
        }
        asteroids += Asteroid.spawn(size, playWidth, random)
    }

    private fun spawnEnemy() {
        val stage = LevelSystem.stageFor(elapsed)
        val kind = pickKind(stage.spawnWeights)
        val spec = EnemyCatalog.spec(kind)
        val x = random.nextFloat() * (playWidth - spec.width)
        val y = -spec.height - random.nextFloat() * 40f
        val enemy = Enemy.of(kind, x, y, seed = random.nextFloat())
        // Difficulty scaling: faster and tougher as the stage rises.
        enemy.vx *= stage.enemySpeedMult
        enemy.vy *= stage.enemySpeedMult
        enemy.hp += stage.enemyHpBonus
        enemies += enemy
    }

    private fun pickKind(weights: Map<EnemyKind, Int>): EnemyKind {
        val total = weights.values.sum()
        if (total <= 0) return EnemyKind.BASIC
        var roll = random.nextInt(total)
        for ((kind, weight) in weights) {
            roll -= weight
            if (roll < 0) return kind
        }
        return EnemyKind.BASIC
    }

    private fun stepParticles(dt: Float) {
        particles.replaceAll { it.copy(age = it.age + dt) }
    }

    private fun resolveCollisions() {
        // Friendly projectile -> enemy.
        for (p in projectiles) {
            if (p.hit || !p.fromPlayer) continue
            for (e in enemies) {
                if (e.isAlive && CollisionSystem.projectileHitsEnemy(p, e)) {
                    e.hp -= p.damage
                    if (!e.isAlive) {
                        onEnemyDestroyed(e)
                    }
                    p.hit = true
                    particles += Particle(e.centerX, e.centerY, e.width, lifetime = 0.3f)
                    break
                }
            }
        }

        // Hostile projectile -> player (skip when invulnerable).
        if (!player.isInvulnerable) {
            for (p in projectiles) {
                if (p.hit || p.fromPlayer) continue
                if (CollisionSystem.projectileHitsPlayer(p, player)) {
                    p.hit = true
                    damagePlayer(GameConfig.ENEMY_PROJ_DAMAGE)
                    break
                }
            }
        }

        // Enemy -> player (skip when invulnerable).
        if (!player.isInvulnerable) {
            for (e in enemies) {
                if (e.isAlive && CollisionSystem.enemyHitsPlayer(e, player)) {
                    e.hp = 0
                    onEnemyDestroyed(e)
                    damagePlayer(EnemyCatalog.spec(e.kind).contactDamage)
                    break
                }
            }
        }

        // Friendly projectile -> asteroid.
        for (p in projectiles) {
            if (p.hit || !p.fromPlayer) continue
            for (a in asteroids) {
                if (a.isAlive && CollisionSystem.projectileHitsAsteroid(p, a)) {
                    a.hp -= p.damage
                    if (!a.isAlive) {
                        onAsteroidDestroyed(a)
                    }
                    p.hit = true
                    particles += Particle(a.centerX, a.centerY, a.width, lifetime = 0.35f)
                    break
                }
            }
        }

        // Asteroid -> player: damages (or is blocked by a shield), then is consumed.
        for (a in asteroids) {
            if (!a.isAlive) continue
            if (CollisionSystem.asteroidHitsPlayer(a, player)) {
                a.hp = 0
                particles += Particle(a.centerX, a.centerY, a.width, lifetime = 0.4f)
                damagePlayer(GameConfig.ASTEROID_CONTACT_DAMAGE)
                break
            }
        }

        // Asteroid -> enemy: strategic mechanic — rocks crush/destroy enemies
        // (scored + may drop). Each asteroid hits at most one enemy per frame.
        for (a in asteroids) {
            if (!a.isAlive) continue
            for (e in enemies) {
                if (e.isAlive && CollisionSystem.asteroidHitsEnemy(a, e)) {
                    e.hp -= a.crushDamage
                    // Rock takes a little damage from the impact too.
                    a.hp -= 1
                    if (!a.isAlive) {
                        onAsteroidDestroyed(a)
                    }
                    if (!e.isAlive) {
                        onEnemyDestroyed(e)
                    }
                    particles += Particle(e.centerX, e.centerY, e.width, lifetime = 0.3f)
                    break
                }
            }
        }

        // Boss collisions: player shots deal damage; boss touching the player hurts.
        val b = boss
        if (b != null && b.isAlive) {
            for (p in projectiles) {
                if (p.hit || !p.fromPlayer) continue
                if (CollisionSystem.projectileHitsBoss(p, b)) {
                    b.hp -= p.damage
                    p.hit = true
                    particles += Particle(b.centerX, b.centerY, 40f, lifetime = 0.3f)
                    if (!b.isAlive) {
                        onBossDefeated(b)
                    }
                    break
                }
            }
            if (!player.isInvulnerable && CollisionSystem.bossHitsPlayer(b, player)) {
                damagePlayer(GameConfig.ENEMY_CONTACT_DAMAGE)
            }
        }
    }

    private fun onEnemyDestroyed(e: Enemy) {
        score += EnemyCatalog.spec(e.kind).scoreValue * player.scoreMultiplier
        enemiesDestroyed += 1
        // Attempt to spawn a pickup on destruction.
        pickupSystem.maybeSpawnOnDestroy(
            kind = e.kind,
            x = e.x,
            y = e.y,
            playWidth = playWidth,
            playHeight = playHeight,
        )?.let { pickups += it }
    }

    private fun onAsteroidDestroyed(a: Asteroid) {
        score += a.scoreValue * player.scoreMultiplier
        particles += Particle(a.centerX, a.centerY, a.width, lifetime = 0.4f)
        // Asteroids can also drop pickups — same generic system.
        pickupSystem.maybeSpawnFromAsteroid(
            x = a.x,
            y = a.y,
            playWidth = playWidth,
            playHeight = playHeight,
        )?.let { pickups += it }
    }

    private fun damagePlayer(amount: Int) {
        // A shield absorbs ALL incoming damage while active.
        if (player.isShielded) return

        player = player.copy(
            hp = player.hp - amount,
            invulnTimer = GameConfig.RESPAWN_INVULN,
        )
        particles += Particle(
            x = player.centerX,
            y = player.centerY,
            size = player.width * 2f,
            lifetime = 0.4f,
        )
        if (player.hp <= 0) {
            if (player.extraLives > 0) {
                // Spend an extra life instead of game over.
                player = player.copy(
                    hp = GameConfig.PLAYER_MAX_HP,
                    extraLives = player.extraLives - 1,
                    invulnTimer = GameConfig.RESPAWN_INVULN,
                )
            } else {
                player = player.copy(hp = 0)
                status = EngineStatus.GAME_OVER
            }
        }
    }

    private fun pruneEntities() {
        // Remove dead enemies and any that have fully exited past the bottom
        // (or drifted too far off the sides — should not happen, but be safe).
        enemies.removeAll { e ->
            !e.isAlive ||
                e.y > playHeight + e.height ||
                e.x < -100f ||
                e.x > playWidth + 100f
        }

        // Asteroids: remove dead rocks or any that fully exit the play area.
        asteroids.removeAll { a ->
            !a.isAlive ||
                a.y > playHeight + a.height ||
                a.x < -400f ||
                a.x > playWidth + 400f
        }

        // Projectiles: remove off-screen (all directions) or already-hit.
        projectiles.removeAll { p ->
            p.hit ||
                p.y < -100f ||
                p.y > playHeight + 100f ||
                p.x < -100f ||
                p.x > playWidth + 100f
        }

        particles.removeAll { it.isExpired }
        pickups.removeAll { it.isExpired || it.collected }
    }

    private fun generateStars(): MutableList<Star> {
        val count = 70
        return MutableList(count) { _ ->
            Star(
                nx = random.nextFloat(),
                ny = random.nextFloat(),
                radius = 0.8f + random.nextFloat() * 2.2f,
                brightness = 0.4f + random.nextFloat() * 0.6f,
                speed = 0.03f + random.nextFloat() * 0.12f,
            )
        }
    }
}