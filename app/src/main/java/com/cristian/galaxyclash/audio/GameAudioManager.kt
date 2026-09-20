package com.cristian.galaxyclash.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * Plays background music (BGM) with a single [MediaPlayer]. It is designed to be
 * owned by the [android.app.Application] (see `GalaxyClashApplication`), so there
 * is exactly ONE live music player for the whole process lifetime. That is the
 * guarantee that a recreated screen or a restarted round can never stack a second
 * copy of the same track.
 *
 * Tracks are resolved BY NAME from `res/raw` (see [MusicTrack]). If a track's file
 * has not been dropped in yet, the name lookup returns `0` and the manager stays
 * silent — the app builds and runs with no music today, and the same track is
 * picked up automatically the moment a matching file is added, without touching
 * the game logic.
 *
 * Future SFX (tiros, explosões, power-ups, vitória/derrota) will use a short-sound
 * player (SoundPool) — a separate concern from this long-loop music manager.
 */
class GameAudioManager(context: Context) {

    /** The track currently held by the player, or `null` if nothing is active. */
    var activeTrack: MusicTrack? = null
        private set

    private val appContext = context.applicationContext

    private var player: MediaPlayer? = null
    private var musicEnabled = true
    private var volume = 0.8f

    /**
     * Starts loop-play of [track]. Idempotent: if that same track is already the
     * active one and still playing, this does nothing — which is what prevents
     * duplicate reproduction when entering a match more than once. Pass
     * [restart] = `true` only when you want to force a fresh copy explicitly.
     */
    fun playMusic(track: MusicTrack, restart: Boolean = false) {
        val active = player
        // Already looping this exact track -> no-op (the duplicate guard).
        if (!restart && track == activeTrack && active != null && active.isPlaying) return

        stopMusic()

        val resId = resolveRawResource(track)
        if (resId == 0) {
            Log.d(
                TAG,
                "Música '${track.rawFileName}' não encontrada em res/raw — jogando sem BGM. " +
                    "Adicione app/src/main/res/raw/${track.rawFileName}.ogg (ou .mp3) para ativá-la.",
            )
            return
        }

        val mp = MediaPlayer.create(appContext, resId) ?: run {
            Log.w(TAG, "Falha ao carregar '${track.rawFileName}' (resId=$resId).")
            return
        }
        // True infinite loop: MediaPlayer automatically restarts from the top
        // when playback reaches the end, so no manual completion handling needed.
        mp.isLooping = true
        mp.setVolume(if (musicEnabled) volume else 0f, if (musicEnabled) volume else 0f)
        try {
            mp.start()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Não foi possível iniciar '${track.rawFileName}'.", e)
            mp.release()
            return
        }
        player = mp
        activeTrack = track
        Log.d(TAG, "BGM em loop iniciado: ${track.rawFileName}")
    }

    /** Stops and releases the current BGM and leaves the manager idle. Call this
     *  when the player leaves a match. */
    fun stopMusic() {
        player?.let { mp ->
            runCatching { mp.stop() }
            mp.release()
        }
        player = null
        activeTrack = null
    }

    /** Pauses output without releasing the track. Call on lifecycle ON_STOP
     *  (app backgrounded) so the music does not keep consuming audio. */
    fun pauseMusic() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    /** Resumes the paused BGM. Call on lifecycle ON_START (app foregrounded). */
    fun resumeMusic() {
        val mp = player ?: return
        if (activeTrack != null && !mp.isPlaying) {
            runCatching { mp.start() }
        }
    }

    /** Respects the in-app "Música" toggle: keeps the loop state but silences
     *  output. Re-enabling brings the volume back without restarting the track. */
    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        val v = if (enabled) volume else 0f
        player?.setVolume(v, v)
    }

    /** Master BGM volume, 0..1. Applies to the current and future players. */
    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        val applied = if (musicEnabled) volume else 0f
        player?.setVolume(applied, applied)
    }

    /** Resolve a track's audio file by NAME from res/raw. Returns `0` when the
     *  file isn't present yet (silent no-op). Works for any extension the Android
     *  MediaPlayer supports (.ogg/.mp3/.wav) because only the resource NAME is used. */
    private fun resolveRawResource(track: MusicTrack): Int =
        appContext.resources.getIdentifier(track.rawFileName, "raw", appContext.packageName)

    private companion object {
        const val TAG = "GameAudioManager"
    }
}