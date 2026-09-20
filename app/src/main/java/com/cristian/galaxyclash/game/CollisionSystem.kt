package com.cristian.galaxyclash.game

/**
 * Axis-aligned bounding-box overlap, isolated from everything else.
 *
 * Collision is split into small pure functions so it can be tested directly and
 * reused by any future subsystem (power-ups, bosses) without touching the
 * engine.
 *
 * A `hitbox` is modeled as a center + half-extents; callers build it from each
 * entity's center and size. This is deliberate: it keeps the overlap check
 * independent of how any entity stores its position, and avoids the classic
 * off-by-half-a-size false-positive/negative bugs that come from mixing
 * top-left vs center origins.
 */
object CollisionSystem {

    data class Box(val cx: Float, val cy: Float, val hw: Float, val hh: Float)

    fun box(cx: Float, cy: Float, width: Float, height: Float): Box =
        Box(cx, cy, width / 2f, height / 2f)

    /** True if two boxes overlap on both axes. */
    fun overlaps(a: Box, b: Box): Boolean {
        val dx = kotlin.math.abs(a.cx - b.cx)
        val dy = kotlin.math.abs(a.cy - b.cy)
        return dx < (a.hw + b.hw) && dy < (a.hh + b.hh)
    }

    fun projectileHitsEnemy(p: Projectile, e: Enemy): Boolean = overlaps(
        box(p.centerX, p.centerY, p.width, p.height),
        box(e.centerX, e.centerY, e.width, e.height),
    )

    fun enemyHitsPlayer(e: Enemy, p: Player): Boolean = overlaps(
        box(e.centerX, e.centerY, e.width, e.height),
        box(p.centerX, p.centerY, p.width, p.height),
    )

    fun projectileHitsPlayer(p: Projectile, player: Player): Boolean = overlaps(
        box(p.centerX, p.centerY, p.width, p.height),
        box(player.centerX, player.centerY, player.width, player.height),
    )

    fun projectileHitsAsteroid(p: Projectile, a: Asteroid): Boolean = overlaps(
        box(p.centerX, p.centerY, p.width, p.height),
        box(a.centerX, a.centerY, a.width, a.height),
    )

    fun asteroidHitsPlayer(a: Asteroid, player: Player): Boolean = overlaps(
        box(a.centerX, a.centerY, a.width, a.height),
        box(player.centerX, player.centerY, player.width, player.height),
    )

    fun asteroidHitsEnemy(a: Asteroid, e: Enemy): Boolean = overlaps(
        box(a.centerX, a.centerY, a.width, a.height),
        box(e.centerX, e.centerY, e.width, e.height),
    )

    fun projectileHitsBoss(p: Projectile, b: Boss): Boolean = overlaps(
        box(p.centerX, p.centerY, p.width, p.height),
        box(b.centerX, b.centerY, b.width, b.height),
    )

    fun bossHitsPlayer(b: Boss, player: Player): Boolean = overlaps(
        box(b.centerX, b.centerY, b.width, b.height),
        box(player.centerX, player.centerY, player.width, player.height),
    )
}