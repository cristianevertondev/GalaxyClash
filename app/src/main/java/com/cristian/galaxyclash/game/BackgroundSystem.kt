package com.cristian.galaxyclash.game

/**
 * Space regions visited as the run progresses. Centralized so new backgrounds
 * are just an enum value + a palette entry, decided by a single pure function.
 */

enum class SpaceBackgroundType {
    NORMAL_SPACE,
    MILKY_WAY,
    NEBULA,
    PLANET_SECTOR,
    DEEP_SPACE,
    BLACK_HOLE,
    DENSE_STARFIELD,
    ALIEN_SPACE,
    SPACE,
    STARS,
    NEBULA2,
    PLANET2,
    DEEP2,
    BLACKHOLE2,
    DENSE2,
    ALIEN2,
    VOID,
    LAVA,
    ICE,
    MECHANIC,
    GLITCH,
    SWAP,
    ORBITS,
    COSMOS,
}

/**
 * Decides which space region the player has reached based on score. The first
 * big change is at [GameConfig.FIRST_SCENE_CHANGE_SCORE] (15.000) — the run is
 * continuous (nothing resets), only the scenery cross-fades.
 */
fun getBackgroundForScore(score: Int): SpaceBackgroundType {
    val idx = (score / 15000).coerceIn(0, 24)
    return sceneTypeFor(idx)
}

/**
 * Score milestones at which the run advances to the next space region. The boss
 * appears just before each milestone and, at half HP, "pushes" the run into the
 * next region. Kept here so the engine and the boss scheduling share the table.
 */
val SCENE_MILESTONES: IntArray = IntArray(25) { it * 15000 }

/**
 * Space background for a scene [level] (0 = the very first region). Levels past
 * the last region clamp back to it, so the endless run keeps looping on color.
 */
fun sceneTypeFor(level: Int): SpaceBackgroundType = when (level.coerceAtLeast(0)) {
    0 -> SpaceBackgroundType.NORMAL_SPACE
    1 -> SpaceBackgroundType.MILKY_WAY
    2 -> SpaceBackgroundType.NEBULA
    3 -> SpaceBackgroundType.PLANET_SECTOR
    4 -> SpaceBackgroundType.DEEP_SPACE
    5 -> SpaceBackgroundType.BLACK_HOLE
    6 -> SpaceBackgroundType.DENSE_STARFIELD
    7 -> SpaceBackgroundType.ALIEN_SPACE
    8 -> SpaceBackgroundType.SPACE
    9 -> SpaceBackgroundType.STARS
    10 -> SpaceBackgroundType.NEBULA2
    11 -> SpaceBackgroundType.PLANET2
    12 -> SpaceBackgroundType.DEEP2
    13 -> SpaceBackgroundType.BLACKHOLE2
    14 -> SpaceBackgroundType.DENSE2
    15 -> SpaceBackgroundType.ALIEN2
    16 -> SpaceBackgroundType.VOID
    17 -> SpaceBackgroundType.LAVA
    18 -> SpaceBackgroundType.ICE
    19 -> SpaceBackgroundType.MECHANIC
    20 -> SpaceBackgroundType.GLITCH
    21 -> SpaceBackgroundType.SWAP
    22 -> SpaceBackgroundType.ORBITS
    23 -> SpaceBackgroundType.COSMOS
    else -> SpaceBackgroundType.COSMOS
}
