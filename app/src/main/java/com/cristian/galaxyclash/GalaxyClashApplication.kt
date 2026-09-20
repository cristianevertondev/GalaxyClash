package com.cristian.galaxyclash

import android.app.Application
import com.cristian.galaxyclash.audio.GameAudioManager

/**
 * Application-scoped holder for components that must exist exactly once for the
 * whole process and must survive activity recreation. Currently that is the BGM
 * system: owning [audioManager] here (instead of inside a composable `remember`)
 * makes it impossible for a recreated screen or a restarted round to ever spin up
 * a second, duplicated copy of a music track.
 */
class GalaxyClashApplication : Application() {
    val audioManager: GameAudioManager by lazy { GameAudioManager(this) }
}