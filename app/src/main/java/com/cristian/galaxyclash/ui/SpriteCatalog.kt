package com.cristian.galaxyclash.ui

import androidx.annotation.DrawableRes
import com.cristian.galaxyclash.R
import com.cristian.galaxyclash.game.BossType
import com.cristian.galaxyclash.game.EnemyKind

/**
 * Central, editable mapping between in-game entities and the pixel-art sprites
 * cropped out of the reference atlas `res/drawable/pacote_artes.png`.
 *
 * Each sprite is a single transparent PNG in `res/drawable-nodpi`. To re-map an
 * entity to a different sprite, change only the `@DrawableRes` returned here —
 * no gameplay/rendering logic needs to change.
 *
 * Atlas mapping (color-grounded, from the sheet layout):
 *  - player            Cyan fighter (band 1, x~1106..1202)
 *  - basic             Magenta fighter (band 1, x~1424..1516)
 *  - zigzag            Orange fighter (band 1, x~648..744)
 *  - random            Red fighter (band 1, x~1214..1310) — no yellow hull in
 *                          the atlas; closest hull stands in pending a better match
 *  - shooter           Purple fighter (band 1, x~532..640)
 *  - aggressive        Bright-red fighter (band 1, x~868..974)
 *  - elite             Green fighter (band 1, x~1322..1412)
 *  - asteroid          Rocky blob (band 2, x~22..114)
 *  - boss GOLIATH      Large red vessel (band 2, x~856..1072)
 *  - boss HYDRA        Large purple vessel (band 2, x~1308..1510)
 *  - boss VOID_MAW     Large green vessel (band 2, x~1082..1298)
 *
 * Projectiles, explosions (particles) and pickups keep their procedural
 * rendering (glowing vectors/icons) — no matching sprite is confidently
 * identified in the atlas yet.
 */
object SpriteCatalog {

    @DrawableRes
    fun player(): Int = R.drawable.sprite_player

    @DrawableRes
    fun enemy(kind: EnemyKind): Int = when (kind) {
        EnemyKind.BASIC -> R.drawable.sprite_enemy_basic
        EnemyKind.ZIGZAG -> R.drawable.sprite_enemy_zigzag
        EnemyKind.RANDOM -> R.drawable.sprite_enemy_random
        EnemyKind.SHOOTER -> R.drawable.sprite_enemy_shooter
        EnemyKind.AGGRESSIVE -> R.drawable.sprite_enemy_aggressive
        EnemyKind.ELITE -> R.drawable.sprite_enemy_elite
    }

    @DrawableRes
    fun asteroid(): Int = R.drawable.sprite_asteroid

    /**
     * Returns the drawable resource for one animation frame (0..2) of a boss's
     * body. GOLIATH / HYDRA / VOID_MAW keep the 3 original bodies; the redesign
     * gives every other boss its own 3-frame body as they are drawn.
     */
    @DrawableRes
    fun getBossSprite(type: BossType, frame: Int): Int {
        val f = frame.coerceIn(0, 2)
        return when (type) {
            BossType.GOLIATH -> when (f) {
                0 -> R.drawable.boss_goliath_0
                1 -> R.drawable.boss_goliath_1
                else -> R.drawable.boss_goliath_2
            }
            BossType.HYDRA -> when (f) {
                0 -> R.drawable.boss_hydra_0
                1 -> R.drawable.boss_hydra_1
                else -> R.drawable.boss_hydra_2
            }
            BossType.VOID_MAW -> when (f) {
                0 -> R.drawable.boss_void_maw_0
                1 -> R.drawable.boss_void_maw_1
                else -> R.drawable.boss_void_maw_2
            }
            BossType.TITAN -> when (f) {
                0 -> R.drawable.boss_titan_0
                1 -> R.drawable.boss_titan_1
                else -> R.drawable.boss_titan_2
            }
            // Bosses not yet re-skinned keep cycling the 3 original bodies.
            else -> when (type.ordinal % 3) {
                0 -> when (f) {
                    0 -> R.drawable.boss_goliath_0
                    1 -> R.drawable.boss_goliath_1
                    else -> R.drawable.boss_goliath_2
                }
                1 -> when (f) {
                    0 -> R.drawable.boss_hydra_0
                    1 -> R.drawable.boss_hydra_1
                    else -> R.drawable.boss_hydra_2
                }
                else -> when (f) {
                    0 -> R.drawable.boss_void_maw_0
                    1 -> R.drawable.boss_void_maw_1
                    else -> R.drawable.boss_void_maw_2
                }
            }
        }
    }
}
