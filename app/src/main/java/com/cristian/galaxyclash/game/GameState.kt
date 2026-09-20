package com.cristian.galaxyclash.game

/**
 * High-level lifecycle of a match. The [GameEngine] drives transitions between
 * these states; the UI only observes them.
 */
enum class GameState {
    /** Player is navigating screens (menu / game over). No active round. */
    MENU,

    /** A round is live and the simulation is advancing. */
    PLAYING,

    /** Simulation is frozen (pause overlay visible). */
    PAUSED,

    /** Player ran out of lives. Score screen shown. */
    GAME_OVER,
}