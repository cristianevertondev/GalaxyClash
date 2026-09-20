package com.cristian.galaxyclash.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cristian.galaxyclash.GalaxyClashApplication
import com.cristian.galaxyclash.audio.MusicTrack
import com.cristian.galaxyclash.game.GameEngine
import com.cristian.galaxyclash.game.GameSnapshot

/**
 * Represents the top-level screen the app is showing. Minimal and explicit:
 * navigation is driven by a single state variable, not a Navigation library,
 * which keeps the MVP lean and easy to reason about.
 */
private enum class Screen { Menu, Melhorias, Settings, Playing, GameOver }

/**
 * Root composable. Owns the [GameEngine] (one per app lifetime), the current
 * screen, final score, and settings toggles. The engine is a single source of
 * truth for gameplay; this layer is only navigation + view-level state.
 */
@Composable
fun GalaxyClashApp() {
    val engine = remember { GameEngine(360f, 640f) }

    var screen by remember { mutableStateOf(Screen.Menu) }
    var settingsOpen by remember { mutableStateOf(false) }

    // Final-round stats, captured when the player dies.
    var finalScore by remember { mutableStateOf(0) }
    var finalEnemies by remember { mutableStateOf(0) }
    var finalLevel by remember { mutableStateOf(1) }

    // Settings toggles (UI only for now; persistence comes later).
    var soundOn by remember { mutableStateOf(true) }
    var musicOn by remember { mutableStateOf(true) }
    var vibrationOn by remember { mutableStateOf(true) }
    var controlStyle by remember { mutableStateOf(com.cristian.galaxyclash.game.ControlStyle.DPAD) }

    // Process-scoped BGM controller (single instance by construction;
    // owned by the Application so a recreated screen can never spawn a duplicate).
    val audio = (LocalContext.current.applicationContext as GalaxyClashApplication).audioManager

    // Respect the in-app "Música" toggle: muting keeps the loop but silences it.
    LaunchedEffect(musicOn) { audio.setMusicEnabled(musicOn) }

    // Pause/resume BGM with the app lifecycle (backgrounded -> paused, no waste;
    // foregrounded -> resume exactly where it was, no restart and no duplicate).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, audio) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> audio.pauseMusic()
                Lifecycle.Event.ON_START -> audio.resumeMusic()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        settingsOpen -> SettingsOverlay(
            soundOn = soundOn,
            musicOn = musicOn,
            vibrationOn = vibrationOn,
            controlStyle = controlStyle,
            onToggleSound = { soundOn = !soundOn },
            onToggleMusic = { musicOn = !musicOn },
            onToggleVibration = { vibrationOn = !vibrationOn },
            onControlStyleChange = { controlStyle = it },
            onClose = { settingsOpen = false },
        )

        screen == Screen.Melhorias -> MelhoriasScreen(
            onBack = { screen = Screen.Menu },
        )

        screen == Screen.Menu -> MainMenuScreen(
            onPlay = {
                engine.start()
                audio.playMusic(MusicTrack.BATTLE) // BGM começa em loop no início da partida
                screen = Screen.Playing
            },
            onMelhorias = { screen = Screen.Melhorias },
            onSettings = { settingsOpen = true },
        )

        screen == Screen.Playing -> GameScreen(
            engine = engine,
            controlStyle = controlStyle,
            onExit = {
                // Time to show results. Capture the final snapshot first.
                val snap: GameSnapshot = engine.snapshot()
                finalScore = snap.score
                finalEnemies = snap.enemiesDestroyed
                finalLevel = snap.level
                audio.stopMusic() // para o BGM ao sair da partida
                screen = Screen.GameOver
            },
        )

        screen == Screen.GameOver -> GameOverScreen(
            score = finalScore,
            enemiesDestroyed = finalEnemies,
            level = finalLevel,
            onRestart = {
                engine.start()
                audio.playMusic(MusicTrack.BATTLE) // novamente em loop; idempotente
                screen = Screen.Playing
            },
            onMenu = { screen = Screen.Menu },
        )
    }
}