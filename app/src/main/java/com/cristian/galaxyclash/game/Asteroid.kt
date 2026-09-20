package com.cristian.galaxyclash.game

/** Asteroid sizes. Larger rocks are rarer and hit harder. */
enum class AsteroidSize {
    MEDIUM,
    LARGE,
}

/**
 * A rotating, drifting space rock. Pure data; the engine advances its position,
 * rotation, hp and collision state. Only MEDIUM and LARGE exist.
 *
 * `x`/`y` are the top-left corner (consistent with the rest of the game), and
 * the size/radius derive from [AsteroidSize].
 */
data class Asteroid(
    var x: Float,
    var y: Float,
    val size: AsteroidSize,
    var vx: Float,
    var vy: Float,
    var rotation: Float = 0f,
    val rotSpeed: Float,
    var hp: Int,
) {
    val width: Float
        get() = when (size) {
            AsteroidSize.MEDIUM -> GameConfig.ASTEROID_MEDIUM_SIZE
            AsteroidSize.LARGE -> GameConfig.ASTEROID_LARGE_SIZE
        }

    val height: Float get() = width
    val radius: Float get() = width / 2f
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    val isAlive: Boolean get() = hp > 0

    val scoreValue: Int
    get() = when (size) {
        AsteroidSize.MEDIUM -> GameConfig.ASTEROID_MEDIUM_SCORE
        AsteroidSize.LARGE -> GameConfig.ASTEROID_LARGE_SCORE
    }

    /** Damage dealt to an ENEMY when this rock crushes it (strategic use). */
    val crushDamage: Int
        get() = when (size) {
            AsteroidSize.MEDIUM -> 3
            AsteroidSize.LARGE -> 8
        }

    companion object {
        fun spawn(
            size: AsteroidSize,
            playWidth: Float,
            random: kotlin.random.Random,
        ): Asteroid {
            val w = when (size) {
                AsteroidSize.MEDIUM -> GameConfig.ASTEROID_MEDIUM_SIZE
                AsteroidSize.LARGE -> GameConfig.ASTEROID_LARGE_SIZE
            }
            val x = random.nextFloat() * (playWidth - w).coerceAtLeast(0f)
            val y = -w - random.nextFloat() * 40f
            // Random heading, mostly downward with some sideways drift.
            val ang = (-0.3f + random.nextFloat() * 0.6f)
            val speed = GameConfig.ASTEROID_SPEED_BASE * (0.7f + random.nextFloat() * 0.5f)
            val vx = kotlin.math.sin(ang) * speed
            val vy = speed * (0.8f + random.nextFloat() * 0.6f)
            val rot = (-0.5f + random.nextFloat()) * GameConfig.ASTEROID_ROT_SPEED
            val hp = when (size) {
                AsteroidSize.MEDIUM -> GameConfig.ASTEROID_MEDIUM_HP
                AsteroidSize.LARGE -> GameConfig.ASTEROID_LARGE_HP
            }
            return Asteroid(
                x = x, y = y, size = size, vx = vx, vy = vy,
                rotation = random.nextFloat() * 6.28f, rotSpeed = rot, hp = hp,
            )
        }
    }
}