package com.cristian.galaxyclash.ui

import androidx.compose.ui.graphics.Color

/**
 * Galaxy Clash palette — a dark, deep-space base with energetic neon accents.
 * Centralized so the whole game shares one visual identity and future polish
 * (nebulas, glow, boss theming) taps the same tokens.
 */
object GameColors {
    val SpaceDeep = Color(0xFF05060F)
    val SpaceMid = Color(0xFF0B1026)
    val StarWhite = Color(0xFFE8ECFF)

    val PlayerCyan = Color(0xFF39D5FF)
    val PlayerCore = Color(0xFF9FEFFF)

    val EnemyMagenta = Color(0xFFFF4D8F)
    val EnemyOrange = Color(0xFFFF9E3D)
    val EnemyYellow = Color(0xFFFFE14D)
    val EnemyPurple = Color(0xFFB366FF)
    val EnemyRed = Color(0xFFFF4D4D)
    val EnemyGreen = Color(0xFF4DFF8F)

    val Laser = Color(0xFF6BFF9E)
    val EnemyLaser = Color(0xFFFF5C5C)

    val Explosion = Color(0xFFFFD86B)

    // Health pickup — bright mint green, distinct from the enemy palette.
    val PickupGreen = Color(0xFF00E676)

    // Asteroids — rocky browns/greys.
    val Asteroid = Color(0xFF8F8371)
    val AsteroidShade = Color(0xFF54493C)
    val AsteroidCrater = Color(0xFF3A332B)

    val HpBar = Color(0xFF39D5FF)
    val HpLost = Color(0xFF2A2E45)

    val AccentViolet = Color(0xFF8C6BFF)
    val TextPrimary = Color(0xFFE8ECFF)
    val TextMuted = Color(0xFF8A90B8)
}