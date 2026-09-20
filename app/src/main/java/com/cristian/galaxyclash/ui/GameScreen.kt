package com.cristian.galaxyclash.ui

import com.cristian.galaxyclash.game.ControlStyle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.cristian.galaxyclash.game.GameEngine
import com.cristian.galaxyclash.game.GameSnapshot
import com.cristian.galaxyclash.game.GameConfig
import com.cristian.galaxyclash.game.SpaceBackgroundType
import com.cristian.galaxyclash.game.PlayerInput
import com.cristian.galaxyclash.game.PickupType
import com.cristian.galaxyclash.game.Asteroid
import com.cristian.galaxyclash.game.Boss
import androidx.compose.ui.graphics.drawscope.rotate
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.cristian.galaxyclash.game.EnemyKind
import com.cristian.galaxyclash.game.BossType
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay

/**
 * Visual upscales for sprites (purely cosmetic, hitboxes unchanged). Ships are
 * drawn bigger so the pilot can always keep track of them on a small screen.
 */
private const val PLAYER_DRAW_SCALE = 1.45f
private const val ENEMY_DRAW_SCALE = 1.30f
// Bosses are already sized to fill the top of the screen via their hitbox width.
private const val BOSS_DRAW_SCALE = 1.0f
// Boss animation frames per second (cycles the 3 poses while a boss is up).
private const val BOSS_ANIM_FPS = 6f

/**
 * Pixel-art sprites (cropped from `pacote_artes.png`) loaded once and reused
 * every frame — no per-frame decode/allocation.
 */
data class SpriteImages(
    val player: ImageBitmap,
    val enemies: Map<EnemyKind, ImageBitmap>,
    val bosses: Map<BossType, List<ImageBitmap>>,
    val asteroid: ImageBitmap,
)

@Composable
private fun rememberSpriteImages(): SpriteImages {
    val res = LocalContext.current.resources
    return remember(res) {
        fun load(@DrawableRes id: Int): ImageBitmap =
            BitmapFactory.decodeResource(res, id).asImageBitmap()
        SpriteImages(
            player = load(SpriteCatalog.player()),
            asteroid = load(SpriteCatalog.asteroid()),
            enemies = EnemyKind.entries.associateWith { load(SpriteCatalog.enemy(it)) },
            bosses = BossType.entries.associateWith { t ->
                (0..2).map { f -> load(SpriteCatalog.getBossSprite(t, f)) }
            },
        )
    }
}

/**
 * The playable screen: renders the [GameSnapshot] with a single Canvas, drives
 * the simulation with a fixed-timestep coroutine loop, and forwards touch input
 * to the engine.
 *
 * Input model (comfortable on small screens):
 *  - Press-and-hold A the right half / right arrow: the ship flies toward your
 *    finger (drag-to-steer). Releasing stops horizontal motion.
 *  - The ship auto-fires while the player is touching; a tap also fires.
 */
@Composable
fun GameScreen(
    engine: GameEngine,
    controlStyle: ControlStyle = ControlStyle.DPAD,
    onExit: () -> Unit
) {
    var snapshot by remember { mutableStateOf(engine.snapshot()) }
    var paused by remember { mutableStateOf(false) }
    // Pixel-art sprites decoded once from resources; reused every frame.
    val sprites = rememberSpriteImages()

    // Continuous touch state for the engine (drag target + auto-fire).
    var dragX by remember { mutableStateOf<Float?>(null) }
    var dragY by remember { mutableStateOf<Float?>(null) }
    var isFiring by remember { mutableStateOf(false) }

    // D-PAD states
    var dpadUp by remember { mutableStateOf(false) }
    var dpadDown by remember { mutableStateOf(false) }
    var dpadLeft by remember { mutableStateOf(false) }
    var dpadRight by remember { mutableStateOf(false) }
    var dpadFire by remember { mutableStateOf(false) }

    // Background cross-fade state — the scenery changes with score, fading
    // smoothly between regions.
    var bg by remember { mutableStateOf(snapshot.background) }
    var prevBg by remember { mutableStateOf(snapshot.background) }
    var fade by remember { mutableStateOf(1f) }
    var bgTime by remember { mutableStateOf(0f) }

    // Game loop: fixed ~60fps step, canceled when the screen leaves composition.
    LaunchedEffect(engine, paused) {
        val frameMs = 16L
        var last = System.nanoTime()
        while (true) {
            delay(frameMs)
            val now = System.nanoTime()
            val dt = (now - last) / 1_000_000_000f
            last = now
            if (!paused) {
                val playerInput = if (controlStyle == ControlStyle.DRAG) {
                    PlayerInput(firing = isFiring, pointerX = dragX, pointerY = dragY)
                } else {
                    PlayerInput(
                        movingUp = dpadUp,
                        movingDown = dpadDown,
                        movingLeft = dpadLeft,
                        movingRight = dpadRight,
                        firing = dpadFire
                    )
                }
                engine.step(dt, playerInput)
                snapshot = engine.snapshot()
                bgTime += dt
                if (snapshot.background != bg) {
                    prevBg = bg
                    bg = snapshot.background
                    fade = 0f
                }
                if (fade < 1f) {
                    fade = kotlin.math.min(fade + dt / GameConfig.BACKGROUND_FADE_SECONDS, 1f)
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GameColors.SpaceDeep, GameColors.SpaceMid))),
    ) {
        val playWidth = constraints.maxWidth.toFloat()
        val playHeight = constraints.maxHeight.toFloat()

        // Keep the engine's world size in sync with the real layout size.
        LaunchedEffect(playWidth, playHeight) {
            engine.resize(playWidth, playHeight)
        }

        var canvasModifier = Modifier.fillMaxSize()
        if (controlStyle == ControlStyle.DRAG) {
            canvasModifier = canvasModifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragX = offset.x
                            dragY = offset.y
                            isFiring = true
                        },
                        onDrag = { change, _ ->
                            dragX = change.position.x
                            dragY = change.position.y
                            isFiring = true
                            change.consume()
                        },
                        onDragEnd = {
                            dragX = null
                            dragY = null
                            isFiring = false
                        },
                        onDragCancel = {
                            dragX = null
                            dragY = null
                            isFiring = false
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isFiring = true
                            tryAwaitRelease()
                            isFiring = false
                        },
                    )
                }
        }

        Canvas(
            modifier = canvasModifier
        ) {
            drawSnapshot(
                snapshot,
                sprites = sprites,
                background = bg,
                prevBackground = prevBg,
                fade = fade,
                time = bgTime,
            )
        }

        if (controlStyle == ControlStyle.DPAD) {
            ControlsOverlay(
                onUp = { dpadUp = it },
                onDown = { dpadDown = it },
                onLeft = { dpadLeft = it },
                onRight = { dpadRight = it },
                onFire = { dpadFire = it }
            )
        }

        Hud(
            snapshot = snapshot,
            onPause = {
                paused = true
                engine.setPaused(true)
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .statusBarsPadding(),
        )

        if (paused) {
            PauseOverlay(
                onResume = {
                    paused = false
                    engine.setPaused(false)
                },
                onRestart = {
                    engine.start()
                    paused = false
                    dragX = null
                    dragY = null
                    isFiring = false
                    snapshot = engine.snapshot()
                },
                onQuit = onExit,
            )
        }

        // When the engine signals game over internally, hand back to the nav layer.
        if (snapshot.player.hp <= 0) {
            LaunchedEffect(Unit) { onExit() }
        }
    }
}

// ---------------------------------------------------------------- rendering

private fun DrawScope.drawSnapshot(
    s: GameSnapshot,
    sprites: SpriteImages,
    background: SpaceBackgroundType,
    prevBackground: SpaceBackgroundType,
    fade: Float,
    time: Float,
) {
    // Background scene — cross-fades from prevBackground into background.
    if (fade >= 1f) {
        drawSpaceScene(background, time)
    } else {
        drawSpaceScene(prevBackground, time, alpha = 1f - fade)
        drawSpaceScene(background, time, alpha = fade)
    }

    // Stars (parallax) — over the scene base, behind everything else.
    val starColor = GameColors.StarWhite
    s.stars.forEach { star ->
        val x = star.nx * size.width
        val y = star.ny * size.height
        val starSize = (star.radius * 1.4f).coerceIn(1.2f, 2.2f)
        drawRect(
            color = starColor.copy(alpha = star.brightness),
            topLeft = Offset(x - starSize / 2f, y - starSize / 2f),
            size = Size(starSize, starSize),
        )
    }

    // Projectiles
    s.projectiles.forEach { p ->
        val color = if (p.fromPlayer) GameColors.Laser else GameColors.EnemyLaser
        // Draw as a vertical-ish streak along its velocity direction.
        drawLine(
            color = color,
            start = Offset(p.centerX, p.centerY - p.height / 2f),
            end = Offset(p.centerX, p.centerY + p.height / 2f),
            strokeWidth = p.width,
        )
    }

    // Enemies — pixel-art sprite per kind.
    s.enemies.forEach { e ->
        drawSprite(sprites.enemies.getValue(e.kind), e.centerX, e.centerY, e.width * ENEMY_DRAW_SCALE)
    }

    // Asteroids — rotating sprite.
    s.asteroids.forEach { a ->
        drawSprite(sprites.asteroid, a.centerX, a.centerY, a.width, rotationDeg = a.rotation * 57.2958f)
    }

    // Boss — animated sprite (3 poses by phase) + bobbing hull.
    s.boss?.let { b ->
        val frames = sprites.bosses.getValue(b.type)
        val idx = ((b.phase * BOSS_ANIM_FPS).toInt() % frames.size).coerceAtMost(frames.size - 1)
        drawBoss(b, frames[idx])
    }

    // Pickups — distinct colored icon per type, gently pulsing.
    s.pickups.forEach { p ->
        drawPickupIcon(p.centerX, p.centerY, p.width, p.age, p.type)
    }

    // Particles (impact/explosion ring that fades out)
    s.particles.forEach { pt ->
        val alpha = 1f - pt.progress
        drawCircle(
            color = GameColors.Explosion.copy(alpha = alpha),
            radius = pt.size * (0.5f + 0.5f * pt.progress),
            center = Offset(pt.x, pt.y),
        )
    }

    // Player ship — hidden (blinking) during invulnerability
    if (!s.player.isInvulnerable || (s.player.invulnTimer * 10).toInt() % 2 == 0) {
        // Damage boost: a warm hotter engine glow behind the ship.
        if (s.player.hasDamageBoost) {
            drawCircle(
                color = GameColors.EnemyRed.copy(alpha = 0.5f),
                radius = s.player.width * PLAYER_DRAW_SCALE,
                center = Offset(s.player.centerX, s.player.centerY),
            )
        }
        drawSprite(sprites.player, s.player.centerX, s.player.centerY, s.player.width * PLAYER_DRAW_SCALE)
        // Shield: a bright cyan bubble that rings the ship while active.
        val shieldRadius = s.player.width * PLAYER_DRAW_SCALE * 0.95f
        if (s.player.isShielded) {
            drawCircle(
                color = GameColors.PlayerCyan.copy(alpha = 0.12f),
                radius = shieldRadius,
                center = Offset(s.player.centerX, s.player.centerY),
            )
            drawCircle(
                color = GameColors.PlayerCyan.copy(alpha = 0.9f),
                radius = shieldRadius,
                center = Offset(s.player.centerX, s.player.centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
            )
        }
    }
}

/**
 * Draws a pixel-art sprite centered on (centerX, centerY), scaled to [width]
 * preserving its aspect ratio. Uses [FilterQuality.None] so the pixels stay
 * hard/sharp (no smoothing) when scaled for gameplay. Optional rotation about
 * the sprite center (used for asteroids).
 */
private fun DrawScope.drawSprite(
    img: ImageBitmap,
    centerX: Float,
    centerY: Float,
    width: Float,
    rotationDeg: Float = 0f,
) {
    val srcW = img.width.toFloat()
    val srcH = img.height.toFloat()
    if (srcW <= 0f || srcH <= 0f || width <= 0f) return
    val scale = width / srcW
    val drawH = srcH * scale
    val dw = width.toInt().coerceAtLeast(1)
    val dh = drawH.toInt().coerceAtLeast(1)
    val dx = (centerX - width / 2f).toInt()
    val dy = (centerY - drawH / 2f).toInt()
    val draw: DrawScope.() -> Unit = {
        drawImage(
            image = img,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset(dx, dy),
            dstSize = IntSize(dw, dh),
            filterQuality = FilterQuality.None,
        )
    }
    if (rotationDeg == 0f) {
        draw()
    } else {
        rotate(degrees = rotationDeg, pivot = Offset(centerX, centerY)) { draw() }
    }
}

// ------------------------------------------------- background scenes

private fun DrawScope.drawSpaceScene(type: SpaceBackgroundType, time: Float, alpha: Float = 1f) {
    val (top, bottom) = backgroundGradient(type)
    drawPixelBase(top, bottom, alpha)
    val drift = (time * 14f) % size.height
    // ambient pixel stars always present so no scene feels empty
    drawPixelStars(if (type == SpaceBackgroundType.DEEP_SPACE) 26 else 46, drift, alpha, seed = 3)
    when (type) {
        SpaceBackgroundType.NORMAL_SPACE -> Unit
        SpaceBackgroundType.MILKY_WAY -> drawMilkyWay(alpha)
        SpaceBackgroundType.NEBULA -> drawNebula(drift, alpha)
        SpaceBackgroundType.PLANET_SECTOR -> drawPlanet(drift, alpha)
        SpaceBackgroundType.DEEP_SPACE -> drawDeepVignette(alpha)
        SpaceBackgroundType.BLACK_HOLE -> drawBlackHole(drift, alpha)
        SpaceBackgroundType.DENSE_STARFIELD -> drawDenseStars(drift, alpha)
        SpaceBackgroundType.ALIEN_SPACE -> drawAlienDecor(drift, alpha)
        // Extended regions: each gets its own palette (see backgroundGradient)
        // and reuses the closest effect helper until dedicated decor is added.
        SpaceBackgroundType.SPACE -> drawDenseStars(drift, alpha)
        SpaceBackgroundType.STARS -> drawDenseStars(drift, alpha)
        SpaceBackgroundType.NEBULA2 -> drawNebula(drift, alpha)
        SpaceBackgroundType.PLANET2 -> drawPlanet(drift, alpha)
        SpaceBackgroundType.DEEP2 -> drawDeepVignette(alpha)
        SpaceBackgroundType.BLACKHOLE2 -> drawBlackHole(drift, alpha)
        SpaceBackgroundType.DENSE2 -> drawDenseStars(drift, alpha)
        SpaceBackgroundType.ALIEN2 -> drawAlienDecor(drift, alpha)
        SpaceBackgroundType.VOID -> drawDeepVignette(alpha)
        SpaceBackgroundType.LAVA -> drawPlanet(drift, alpha)
        SpaceBackgroundType.ICE -> drawMilkyWay(alpha)
        SpaceBackgroundType.MECHANIC -> drawAlienDecor(drift, alpha)
        SpaceBackgroundType.GLITCH -> drawAlienDecor(drift, alpha)
        SpaceBackgroundType.SWAP -> drawBlackHole(drift, alpha)
        SpaceBackgroundType.ORBITS -> drawPlanet(drift, alpha)
        SpaceBackgroundType.COSMOS -> drawDenseStars(drift, alpha)
    }
}

// ------------------------------------------------------------------ helpers

private fun backgroundGradient(type: SpaceBackgroundType): Pair<Color, Color> = when (type) {
    SpaceBackgroundType.NORMAL_SPACE -> GameColors.SpaceDeep to GameColors.SpaceMid
    SpaceBackgroundType.SPACE -> Color(0xFF06060F) to Color(0xFF202A4A)
    SpaceBackgroundType.STARS -> Color(0xFF04040B) to Color(0xFF2C2C50)
    SpaceBackgroundType.NEBULA2 -> Color(0xFF3A0B2E) to Color(0xFF0E1830)
    SpaceBackgroundType.PLANET2 -> Color(0xFF0E1A24) to Color(0xFF2A4E68)
    SpaceBackgroundType.DEEP2 -> Color(0xFF01020C) to Color(0xFF0C1022)
    SpaceBackgroundType.BLACKHOLE2 -> Color(0xFF07020C) to Color(0xFF241020)
    SpaceBackgroundType.DENSE2 -> Color(0xFF03040F) to Color(0xFF18203A)
    SpaceBackgroundType.ALIEN2 -> Color(0xFF0B3A16) to Color(0xFF340B34)
    SpaceBackgroundType.VOID -> Color(0xFF000002) to Color(0xFF050512)
    SpaceBackgroundType.LAVA -> Color(0xFF200803) to Color(0xFF5A1608)
    SpaceBackgroundType.ICE -> Color(0xFF04171F) to Color(0xFF1C4A5E)
    SpaceBackgroundType.MECHANIC -> Color(0xFF0B0E14) to Color(0xFF333B4A)
    SpaceBackgroundType.GLITCH -> Color(0xFF17041C) to Color(0xFF3A1E08)
    SpaceBackgroundType.SWAP -> Color(0xFF041C18) to Color(0xFF0E3A2A)
    SpaceBackgroundType.ORBITS -> Color(0xFF0A0620) to Color(0xFF2E1650)
    SpaceBackgroundType.COSMOS -> Color(0xFF060118) to Color(0xFF2A0A3A)
    SpaceBackgroundType.MILKY_WAY -> Color(0xFF0B1030) to Color(0xFF1E2F5A)
    SpaceBackgroundType.NEBULA -> Color(0xFF2A0B3A) to Color(0xFF0B1830)
    SpaceBackgroundType.PLANET_SECTOR -> Color(0xFF0A1626) to Color(0xFF22405E)
    SpaceBackgroundType.DEEP_SPACE -> Color(0xFF02030A) to Color(0xFF0A0E1E)
    SpaceBackgroundType.BLACK_HOLE -> Color(0xFF050308) to Color(0xFF1A0A14)
    SpaceBackgroundType.DENSE_STARFIELD -> Color(0xFF05060F) to Color(0xFF111A33)
    SpaceBackgroundType.ALIEN_SPACE -> Color(0xFF0B3012) to Color(0xFF2A0B2E)
}

private const val PX_SCENE = 16f   // grid cell for scenery (big chunky pixels)

/** Snaps a coordinate to the pixel-art grid. */
private fun snap(v: Float, cell: Float = PX_SCENE): Float = (v / cell).roundToInt() * cell

/** Draws one chunky square pixel, snapped to the grid. */
private fun DrawScope.px(x: Float, y: Float, s: Float, c: Color, a: Float = 1f, cell: Float = PX_SCENE) {
    val size = kotlin.math.max(cell, (s / cell).roundToInt() * cell)
    drawRect(
        color = c.copy(alpha = (a.coerceIn(0f, 1f))),
        topLeft = Offset(snap(x, cell), snap(y, cell)),
        size = Size(size, size),
    )
}

private fun mix(a: Color, b: Color, t: Float): Color = Color(
    a.red + (b.red - a.red) * t,
    a.green + (b.green - a.green) * t,
    a.blue + (b.blue - a.blue) * t,
)

/** Chunky horizontal bands -> retro vertical gradient, no smoothing. */
private fun DrawScope.drawPixelBase(top: Color, bottom: Color, alpha: Float) {
    val bands = 14
    val bandH = size.height / bands
    for (i in 0 until bands) {
        val t = (i + 0.5f) / bands
        drawRect(
            color = mix(top, bottom, t).copy(alpha = alpha),
            topLeft = Offset(0f, i * bandH),
            size = Size(size.width, bandH + 1f),
        )
    }
}

/** Square pixel stars with twinkle + downward drift (small "magic dust" stars). */
private fun DrawScope.drawPixelStars(count: Int, drift: Float, alpha: Float, seed: Int, cell: Float = 3f) {
    val w = size.width
    val h = size.height
    repeat(count) { i ->
        val fx = ((i * 37 + seed * 13) % 97) / 97f
        val fy = (((i * 53 + seed * 29) % 97) / 97f * h * 2 + drift) % h
        val s = cell * (1 + ((i + seed) % 2))
        val br = 0.35f + 0.65f * ((i * 7 + seed * 3) % 9) / 9f
        val tw = 0.6f + 0.4f * kotlin.math.sin(i * 1.7f + drift * 0.02f)
        px(fx * w - s, (fy - s).coerceAtLeast(0f), s, GameColors.StarWhite.copy(alpha = alpha * br * tw), 1f, cell)
    }
}

/** Fills a blocky disc; color chosen per horizontal band via [shade]. */
private fun DrawScope.fillPixelDisc(
    cx: Float,
    cy: Float,
    radius: Float,
    cell: Float,
    alpha: Float,
    shade: (rowFrac: Float) -> Color,
) {
    val rc = (radius / cell).toInt().coerceAtLeast(1)
    for (by in -rc..rc) {
        val half = kotlin.math.sqrt((rc * rc - by * by).toFloat()).toInt()
        val rowFrac = (by + rc + 0.5f) / (2 * rc + 1f)
        val c = shade(rowFrac)
        if (c.alpha < 0.02f) continue
        for (i in -half..half) {
            px(cx + i * cell, cy + by * cell, cell, c, alpha, cell)
        }
    }
}

// per-pixel variant used by planets for craters/cloud bands
private fun DrawScope.fillPixelDisc(
    cx: Float,
    cy: Float,
    radius: Float,
    cell: Float,
    alpha: Float,
    shade: (rowFrac: Float, xIdx: Int, yIdx: Int) -> Color,
) {
    val rc = (radius / cell).toInt().coerceAtLeast(1)
    for (by in -rc..rc) {
        val half = kotlin.math.sqrt((rc * rc - by * by).toFloat()).toInt()
        val rowFrac = (by + rc + 0.5f) / (2 * rc + 1f)
        for (i in -half..half) {
            val c = shade(rowFrac, i, by)
            if (c.alpha < 0.02f) continue
            px(cx + i * cell, cy + by * cell, cell, c, alpha, cell)
        }
    }
}

/** A ring (ellipse) of pixel blocks around a planet; [front] redraws the lower arc. */
private fun DrawScope.drawPixelRing(cx: Float, cy: Float, majorR: Float, vR: Float, cell: Float, colorBase: Color, alpha: Float, scroll: Float, front: Boolean) {
    val rows = (vR / cell).toInt().coerceAtLeast(1) * 2
    for (j in -rows..rows) {
        val frac = j.toFloat() / rows
        val half = majorR * kotlin.math.sqrt((1 - frac * frac).coerceAtLeast(0f))
        val yy = cy + j * cell * 0.5f
        if (front && yy < cy) continue
        if (!front && yy > cy + cell) continue
        val tw = 0.6f + 0.4f * kotlin.math.sin(j * 2.3f + scroll)
        val color = mix(colorBase, Color.White, 0.2f * tw)
        val steps = (half * 2 / cell).toInt()
        for (k in 0..steps) {
            px(cx - half + k * cell, yy, cell, color, alpha * tw, cell)
        }
    }
}

// ------------------------------------------------------------------- scenes

/** A bright pixel band of condensed stars — the galaxy. */
private fun DrawScope.drawMilkyWay(alpha: Float) {
    val w = size.width
    val h = size.height
    repeat(42) { i ->
        val fx = ((i * 29 + 5) % 97) / 97f
        val fy = ((i * 71 + 13) % 97) / 97f
        val al = 0.25f + 0.5f * ((i % 7) / 7f)
        val s = 2f + i % 3
        px(w * fx, fy * h, s, mix(Color(0xFFB9C6FF), Color(0xFF8C6BFF), i % 2 * 0.6f).copy(alpha = al), alpha, 4f)
    }
}

/** Big translucent pixel clusters drifting — a nebula. */
private fun DrawScope.drawNebula(drift: Float, alpha: Float) {
    val spots = listOf(
        Triple(0.20f, 0.25f, Color(0xFFB366FF)),
        Triple(0.70f, 0.55f, Color(0xFF39D5FF)),
        Triple(0.48f, 0.82f, Color(0xFFFF4D8F)),
        Triple(0.86f, 0.36f, Color(0xFFFF9E3D)),
    )
    for ((fx, fy, c) in spots) {
        val y = ((fy * size.height * 1.6f + drift) % (size.height * 1.6f)) - size.height * 0.3f
        val r = size.width * 0.16f
        fillPixelDisc(size.width * fx, y, r, PX_SCENE * 2, 0.10f * alpha) { c.copy(alpha = 0.9f) }
        fillPixelDisc(size.width * fx - PX_SCENE * 2, y - PX_SCENE * 2, r * 0.6f, PX_SCENE, 0.14f * alpha) { c.copy(alpha = 1f) }
    }
}

/** A BIG ringed planet, front and centre — the player's starfly showcase. */
private fun DrawScope.drawPlanet(drift: Float, alpha: Float) {
    val cx = size.width * 0.72f
    val cy = ((0.30f * size.height + drift * 0.22f) % (size.height * 1.3f)) - size.height * 0.25f
    val r = size.width * 0.13f

    // --- ring behind the planet ---
    drawPixelRing(cx, cy, r * 1.9f, r * 0.5f, PX_SCENE * 0.6f, Color(0xFF9AC0FF).copy(alpha = 0.8f), alpha, drift, front = false)
    // planet body: top atmosphere -> equator cloud band -> dark pole
    fillPixelDisc(cx, cy, r, PX_SCENE, alpha) { row, _, _ ->
        when {
            row < 0.30f -> Color(0xFF7FD8E8)
            row < 0.46f -> Color(0xFF3FA7CC)
            row < 0.62f -> Color(0xFF2C7CA8)
            row < 0.82f -> Color(0xFF1E5068)
            else -> Color(0xFF123A4E)
        }
    }
    // surface cloud streaks + a dark storm spot
    fillPixelDisc(cx, cy, r * 0.9f, PX_SCENE, alpha * (3f / 4f)) { row, x, y ->
        if ((y % 4 == 0) && row in 0.32f..0.78f) Color(0xFFC8F0FF) else Color(0x00000000)
    }
    // ring front: lower arc over the planet
    drawPixelRing(cx, cy, r * 1.9f, r * 0.5f, PX_SCENE * 0.6f, Color(0xFF9FC0FF).copy(alpha = 0.9f), alpha, drift, front = true)
    // distant moon
    fillPixelDisc(cx - r * 2.1f, cy - r * 0.5f, r * 0.22f, PX_SCENE * 0.7f, alpha) { Color(0xFFB9B4A6) }
}

/** Deep space: just a heavy dark vignette and almost no stars. */
private fun DrawScope.drawDeepVignette(alpha: Float) {
    val w = size.width
    val h = size.height
    // pixel vignette: darker blocks toward the edge
    for (i in 0 until 22) {
        val t = i / 22f
        val edgeA = (0.35f * t).coerceAtMost(0.34f) * alpha
        drawRect(
            color = Color(0xFF01030C).copy(alpha = edgeA),
            topLeft = Offset(0f, i * h / 22f),
            size = Size(w, h / 22f + 1f),
        )
    }
}

/** A blocky black hole with a pixel accretion ring and debris. */
private fun DrawScope.drawBlackHole(drift: Float, alpha: Float) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.42f
    val r = size.width * 0.11f
    val spin = drift * 1.6f
    // outer glow
    fillPixelDisc(cx, cy, r * 2.4f, PX_SCENE * 2, 0.10f * alpha) { Color(0xFFFF9E3D).copy(alpha = 1f) }
    // accretion ring (blocky, warmer side)
    fillPixelDisc(cx, cy, r * 1.7f, PX_SCENE * 0.8f, alpha * 0.9f) { row, x, _ ->
        val band = (x + (spin / PX_SCENE).toInt()) % 3
        when (band) {
            0 -> Color(0xFFFFC93D)
            1 -> Color(0xFFFF9E3D)
            else -> Color(0xFFFF5C5C)
        }
    }
    // starless maw
    fillPixelDisc(cx, cy, r, PX_SCENE, alpha) { Color(0xFF000000) }
    // a few trapped pixels circling
    repeat(6) { k ->
        val ang = k.toFloat() / 6f * 6.2831853f + (drift / PX_SCENE) * 0.3f
        px(cx + kotlin.math.cos(ang) * r * 1.35f, cy + kotlin.math.sin(ang) * r * 1.35f, PX_SCENE * 0.7f, Color(0xFFFFE9A8), alpha, 6f)
    }
}

/** Lots of tiny bright squares — a dense packed starfield. */
private fun DrawScope.drawDenseStars(drift: Float, alpha: Float) {
    drawPixelStars(90, drift, alpha, seed = 11, cell = 3f)
    drawPixelStars(50, drift * 1.6f, alpha * 0.8f, seed = 7, cell = 2f)
}

/** Alien/futuristic region: two neon ringed worlds + grid hints. */
private fun DrawScope.drawAlienDecor(drift: Float, alpha: Float) {
    val cx = size.width * 0.25f
    val cy = ((0.6f * size.height + drift * 0.3f) % (size.height * 1.5f)) - size.height * 0.3f
    val r = size.width * 0.075f
    drawPixelRing(cx, cy, r * 1.8f, r * 0.5f, PX_SCENE * 0.6f, Color(0xFFFF4D8F), alpha, drift, front = false)
    fillPixelDisc(cx, cy, r, PX_SCENE, alpha) { row, x, _ ->
        if ((x + row * 7).toInt() % 3 == 0) Color(0xFF4DFF8F) else Color(0xFF1FA060)
    }
    drawPixelRing(cx, cy, r * 1.8f, r * 0.5f, PX_SCENE * 0.6f, Color(0xFFFF8FD0), alpha, drift, front = true)

    val cx2 = size.width * 0.82f
    val cy2 = ((0.18f * size.height + drift * 0.5f) % (size.height * 1.4f)) - size.height * 0.25f
    fillPixelDisc(cx2, cy2, r * 0.55f, PX_SCENE * 0.7f, alpha) { Color(0xFFB366FF) }
}

private fun enemyColor(kind: com.cristian.galaxyclash.game.EnemyKind): Color = when (kind) {
    com.cristian.galaxyclash.game.EnemyKind.BASIC -> GameColors.EnemyMagenta
    com.cristian.galaxyclash.game.EnemyKind.ZIGZAG -> GameColors.EnemyOrange
    com.cristian.galaxyclash.game.EnemyKind.RANDOM -> GameColors.EnemyYellow
    com.cristian.galaxyclash.game.EnemyKind.SHOOTER -> GameColors.EnemyPurple
    com.cristian.galaxyclash.game.EnemyKind.AGGRESSIVE -> GameColors.EnemyRed
    com.cristian.galaxyclash.game.EnemyKind.ELITE -> GameColors.EnemyGreen
}

/**
 * Player ship drawn from primitives: a sleek arrow/triangle with an engine glow.
 */
private fun DrawScope.drawPlayerShip(cx: Float, cy: Float, w: Float) {
    val h = w * 1.16f
    val top = cy - h / 2f
    val bottom = cy + h / 2f
    val half = w / 2f

    // Engine glow (underneath)
    drawCircle(
        color = GameColors.PlayerCyan.copy(alpha = 0.35f),
        radius = w * 0.6f,
        center = Offset(cx, cy),
    )

    // Hull — upward-pointing triangle
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, top)
        lineTo(cx + half, bottom)
        lineTo(cx, bottom - h * 0.18f)
        lineTo(cx - half, bottom)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            listOf(GameColors.PlayerCore, GameColors.PlayerCyan),
        ),
    )

    // Cockpit core
    drawCircle(
        color = GameColors.SpaceDeep.copy(alpha = 0.85f),
        radius = w * 0.14f,
        center = Offset(cx, cy + h * 0.06f),
    )
}

/**
 * Enemy drawn as an inverted (downward) ship with a menacing core.
 */
private fun DrawScope.drawEnemy(cx: Float, cy: Float, w: Float, color: Color) {
    val h = w
    val top = cy - h / 2f
    val bottom = cy + h / 2f
    val half = w / 2f

    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, bottom)
        lineTo(cx + half, top)
        lineTo(cx, top + h * 0.22f)
        lineTo(cx - half, top)
        close()
    }
    drawPath(path = path, brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))))

    // Core
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = w * 0.13f,
        center = Offset(cx, cy - h * 0.08f),
    )
}

/**
 * Asteroid drawn as a rotating, jagged rocky mass with craters. Vertices wobble
 * deterministically from the rock's X so every asteroid looks a little unique.
 */
private fun DrawScope.drawAsteroid(a: Asteroid) {
    val cx = a.centerX
    val cy = a.centerY
    val r = a.radius
    rotate(
        degrees = kotlin.math.round(a.rotation * 57.2958f),
        pivot = Offset(cx, cy),
    ) {
        val seed = (a.x * 7919f) % 1000f
        val points = 11
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0 until points) {
            val ang = i.toFloat() / points * 6.2831853f
            val bob = 0.72f + 0.28f * ((seed + i * 37f) % 1f)
            val px = cx + kotlin.math.cos(ang) * r * bob
            val py = cy + kotlin.math.sin(ang) * r * bob
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, GameColors.Asteroid)

        // Craters (dark spots) that also rotate with the rock.
        drawCircle(
            color = GameColors.AsteroidCrater,
            radius = r * 0.30f,
            center = Offset(cx - r * 0.30f, cy - r * 0.20f),
        )
        drawCircle(
            color = GameColors.AsteroidCrater,
            radius = r * 0.22f,
            center = Offset(cx + r * 0.28f, cy + r * 0.30f),
        )
        drawCircle(
            color = GameColors.AsteroidCrater,
            radius = r * 0.18f,
            center = Offset(cx + r * 0.08f, cy - r * 0.36f),
        )
        // Shadow arc on the lower edge for depth.
        drawCircle(
            color = GameColors.AsteroidShade,
            radius = r * 0.9f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.18f),
        )
    }
}

/**
 * Boss ship — drawn from its pre-decoded pixel-art sprite with a gentle bob.
 * [sprite] is the already-loaded bitmap for this boss (its frame is chosen by
 * the caller).
 */
private fun DrawScope.drawBoss(b: Boss, sprite: ImageBitmap) {
    val cx = b.centerX
    val bob = kotlin.math.sin(b.phase * 1.5f) * 3f
    drawSprite(sprite, cx, b.centerY + bob, b.width * BOSS_DRAW_SCALE)
}

/**
 * Pickup rendered as a glossy colored capsule with a centered glyph, pulsing
 * gently. Each type has a distinct color + symbol; rare power-ups get a larger
 * glow so they stand out. Uses the native canvas to draw the text glyph cheaply.
 */
private fun DrawScope.drawPickupIcon(cx: Float, cy: Float, w: Float, age: Float, type: PickupType) {
    val pulse = 1f + 0.08f * kotlin.math.sin(age * 6f)
    val r = w * 0.5f * pulse
    val iconColor = pickupColor(type)
    val rareGlow = if (type == PickupType.EXTRA_LIFE || type == PickupType.QUAD_SHOT ||
        type == PickupType.MAGNET || type == PickupType.SCORE_MULTIPLIER
    ) 1.9f else 1.4f

    // Soft glow behind the icon.
    drawCircle(
        color = iconColor.copy(alpha = 0.30f),
        radius = r * rareGlow,
        center = Offset(cx, cy),
    )

    // Solid capsule body.
    drawCircle(color = iconColor.copy(alpha = 0.92f), radius = r, center = Offset(cx, cy))
    drawCircle(
        color = Color.White.copy(alpha = 0.25f),
        radius = r * 0.72f,
        center = Offset(cx - r * 0.15f, cy - r * 0.15f),
    )

    // Centered glyph.
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = r
    }
    drawContext.canvas.nativeCanvas.drawText(pickupGlyph(type), cx, cy + r * 0.38f, textPaint)

    // Outline ring.
    drawCircle(
        color = iconColor.copy(alpha = 0.9f),
        radius = r,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f),
    )
}

private fun pickupColor(type: PickupType) = when (type) {
    PickupType.HEALTH -> GameColors.PickupGreen
    PickupType.DOUBLE_SHOT -> Color(0xFF3D9BFF)
    PickupType.TRIPLE_SHOT -> Color(0xFFB366FF)
    PickupType.QUAD_SHOT -> Color(0xFFFF5CC8)
    PickupType.RAPID_FIRE -> Color(0xFFFFC93D)
    PickupType.SHIELD -> Color(0xFF5CE1FF)
    PickupType.DAMAGE_BOOST -> Color(0xFFFF6B4D)
    PickupType.MAGNET -> Color(0xFFFFE14D)
    PickupType.SCORE_MULTIPLIER -> Color(0xFFD9B36B)
    PickupType.EXTRA_LIFE -> GameColors.EnemyRed
}

private fun pickupGlyph(type: PickupType): String = when (type) {
    PickupType.HEALTH -> "+"
    PickupType.DOUBLE_SHOT -> "2"
    PickupType.TRIPLE_SHOT -> "3"
    PickupType.QUAD_SHOT -> "4"
    PickupType.RAPID_FIRE -> "R"
    PickupType.SHIELD -> "S"
    PickupType.DAMAGE_BOOST -> "D"
    PickupType.MAGNET -> "M"
    PickupType.SCORE_MULTIPLIER -> "x2"
    PickupType.EXTRA_LIFE -> "\u2665" // heart
}