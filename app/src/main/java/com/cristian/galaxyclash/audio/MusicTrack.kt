package com.cristian.galaxyclash.audio

/**
 * The music tracks the game can play. Each value is bound to an audio file that
 * must be placed in:
 *
 *     app/src/main/res/raw/<rawFileName>.ogg      (also works with .mp3 / .wav)
 *
 * Files are resolved BY NAME — never by a compile-time `R.raw.*` reference — so
 * a missing track is a silent no-op instead of a build error. You can drop the
 * file in at any time and it will be picked up automatically on the next launch,
 * with no code change.
 *
 * DO NOT rename [rawFileName] after adding a file, or the name lookup will
 * silently stop matching it.
 *
 * Only [BATTLE] is wired to gameplay today. [MENU] and [BOSS] are reserved for
 * future features; they are already resolvable so wiring them in later is a
 * one-line call to GameAudioManager.playMusic(...).
 */
enum class MusicTrack(val rawFileName: String) {
    /** Background music that loops for the whole duration of a match. */
    BATTLE("battle_music"),

    /** Reserved: main-menu theme (future). */
    MENU("menu_music"),

    /** Reserved: boss-fight theme (future). */
    BOSS("boss_music"),
}