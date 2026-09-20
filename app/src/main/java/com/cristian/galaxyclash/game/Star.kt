package com.cristian.galaxyclash.game

/**
 * A background star. Position is in [0,1] range of the play area width/height
 * and converted to pixels at render time; the engine scrolls it downward for a
 * sense of speed. Sizing/brightness vary for depth.
 */
data class Star(
    /** Normalized horizontal position (0..1). */
    val nx: Float,
    /** Normalized vertical position (0..1). */
    val ny: Float,
    /** Radius in px. */
    val radius: Float,
    /** Brightness (alpha) 0..1. */
    val brightness: Float,
    /** Scroll speed in screen-heights per second (parallax depth). */
    val speed: Float,
)