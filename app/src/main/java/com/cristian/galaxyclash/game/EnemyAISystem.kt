package com.cristian.galaxyclash.game

import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

/**
 * Enemy behavior brain. Pure and deterministic given the injected [Random], so
 * it is unit-testable without Android. Mutates each [Enemy]'s velocity/steer
 * state in place based on its [EnemyKind], then the engine integrates position.
 *
 * Invariants enforced here (and tested):
 *  - No enemy is ever permanently stuck at a wall: lateral motion always flips
 *    direction when it hits the horizontal bounds.
 *  - Vertical motion is always downward-ish (enemies advance toward the player).
 *  - Everything is clamped to the play area; an enemy can never leave it except
 *    by exiting through the bottom (where the engine prunes it).
 */
object EnemyAISystem {

    private const val MARGIN = 6f           // px buffer inside the walls
    private const val STEER_MIN = 0.4f
    private const val STEER_MAX = 1.6f

    /**
     * Updates [enemy] for one tick. [playerX]/[playerY] are the player's center
     * (for shooter/aggressive/elite tracking).
     */
    fun update(
        enemy: Enemy,
        dt: Float,
        playWidth: Float,
        playerX: Float,
        playerY: Float,
        random: Random,
    ) {
        val spec = EnemyCatalog.spec(enemy.kind)
        val minX = MARGIN
        val maxX = playWidth - enemy.width - MARGIN

        when (enemy.kind) {
            EnemyKind.BASIC -> {
                // Slight lateral drift; flip gently at walls.
                drift(enemy, spec, dt, minX, maxX, random)
            }

            EnemyKind.ZIGZAG -> {
                // Sweep with full lateral speed; hard-flip at walls.
                enemy.vx = spec.lateralSpeed * direction(enemy)
                flipAtEdges(enemy, minX, maxX)
                enemy.vy = spec.baseSpeed
            }

            EnemyKind.RANDOM -> {
                // Re-aim at random intervals; flip at walls regardless.
                enemy.steerTimer -= dt
                if (enemy.steerTimer <= 0f) {
                    enemy.steerTimer = STEER_MIN + random.nextFloat() * (STEER_MAX - STEER_MIN)
                    // Random horizontal target, weighted to keep it drifting around.
                    enemy.vx = spec.lateralSpeed * (random.nextFloat() * 2f - 1f)
                    enemy.vy = spec.baseSpeed * (0.6f + random.nextFloat() * 0.6f)
                }
                flipAtEdges(enemy, minX, maxX)
            }

            EnemyKind.SHOOTER -> {
                // Gentle approach, partial tracking toward the player's X.
                val toPlayerX = playerX - enemy.centerX
                val steer = toPlayerX.coerceIn(-spec.lateralSpeed, spec.lateralSpeed)
                enemy.vx = (enemy.vx * 0.9f + steer * 0.1f).coerceIn(-spec.lateralSpeed, spec.lateralSpeed)
                enemy.vy = spec.baseSpeed
                flipAtEdges(enemy, minX, maxX)
            }

            EnemyKind.AGGRESSIVE -> {
                // Partial homing: a fraction of the vector toward the player.
                val dx = playerX - enemy.centerX
                val dy = playerY - enemy.centerY
                val pullX = (dx * spec.aggression).coerceIn(-spec.lateralSpeed, spec.lateralSpeed)
                enemy.vx = (enemy.vx * 0.94f + pullX * 0.06f).coerceIn(-spec.lateralSpeed, spec.lateralSpeed)
                // Vertical: keep advancing, with a mild pull toward the player.
                enemy.vy = (spec.baseSpeed + dy * spec.aggression * 0.2f)
                    .coerceIn(spec.baseSpeed * 0.3f, spec.baseSpeed * 1.8f)
                flipAtEdges(enemy, minX, maxX)
            }

            EnemyKind.ELITE -> {
                // Shares shooter tracking + a bit of aggression.
                val toPlayerX = playerX - enemy.centerX
                val steer = toPlayerX.coerceIn(-spec.lateralSpeed, spec.lateralSpeed)
                enemy.vx = (enemy.vx * 0.92f + steer * 0.08f).coerceIn(-spec.lateralSpeed, spec.lateralSpeed)
                enemy.vy = spec.baseSpeed
                flipAtEdges(enemy, minX, maxX)
            }
        }
    }

    /** Whether this enemy should fire a projectile this tick (decrements timer). */
    fun wantsToShoot(enemy: Enemy, dt: Float): Boolean {
        val spec = EnemyCatalog.spec(enemy.kind)
        if (spec.shootCooldown <= 0f) return false
        enemy.shootTimer -= dt
        if (enemy.shootTimer <= 0f) {
            enemy.shootTimer = spec.shootCooldown
            return true
        }
        return false
    }

    // --- helpers -------------------------------------------------------------

    private fun direction(enemy: Enemy): Float =
        if (enemy.vx >= 0f) 1f else -1f

    private fun drift(enemy: Enemy, spec: EnemySpec, dt: Float, minX: Float, maxX: Float, random: Random) {
        // Give the basic enemy a minimum lateral speed so it can't sit still at a
        // wall; random nudges re-pick direction but it always keeps drifting.
        val minLateral = spec.lateralSpeed * 0.4f
        if (abs(enemy.vx) < minLateral) {
            // If we're nearly stopped, push away from the nearest wall.
            val away = if (enemy.x < (minX + maxX) / 2f) 1f else -1f
            enemy.vx = if (random.nextBoolean()) minLateral * away else minLateral * -away
        }
        // Occasionally re-pick the drift to break symmetry (~2% per frame).
        if (random.nextFloat() < 0.02f) {
            enemy.vx = (random.nextFloat() * 2f - 1f) * spec.lateralSpeed * 0.6f
        }
        // Keep the total drift within the allowed range.
        enemy.vx = enemy.vx.coerceIn(-spec.lateralSpeed, spec.lateralSpeed)
        enemy.vy = spec.baseSpeed
        flipAtEdges(enemy, minX, maxX)
    }

    /**
     * If the enemy is at (or past) a horizontal wall, reverse its horizontal
     * velocity so it bounces back inward. This is the core fix for "enemy stuck
     * at wall" — the flip is guaranteed, not dependent on drift randomness.
     */
    private fun flipAtEdges(enemy: Enemy, minX: Float, maxX: Float) {
        val lx = enemy.x
        if (lx <= minX && enemy.vx < 0f) {
            enemy.vx = abs(enemy.vx)
            enemy.x = minX
        } else if (lx >= maxX && enemy.vx > 0f) {
            enemy.vx = -abs(enemy.vx)
            enemy.x = maxX
        }
    }
}