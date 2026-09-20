package com.cristian.galaxyclash.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import com.cristian.galaxyclash.ui.PixelFont.drawPixelText
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private data class MenuSprites(val player: ImageBitmap, val asteroid: ImageBitmap)

private val Stars = (0 until 90).map {
    val r = Random(it * 31L + 7)
    StarFx(
        fx = r.nextFloat(),
        fy = r.nextFloat(),
        size = 1 + r.nextInt(3),
        speed = 0.4f + r.nextFloat() * 1.6f,
        phase = r.nextFloat() * 6.28f,
    )
}
private data class StarFx(val fx: Float, val fy: Float, val size: Int, val speed: Float, val phase: Float)

private data class DriftAsteroid(val fx: Float, val spawnT: Float, val speed: Float, val scale: Float)

/**
 * Old-school pixel-art main menu: chunky bitmap text, a live space background
 * (stars, drifting asteroids, planet, shooting stars, a flying hero ship),
 * and crisp pixel buttons for JOGAR / MELHORIAS / CONFIGURAÇÕES.
 */
@Composable
fun MainMenuScreen(
    onPlay: () -> Unit,
    onMelhorias: () -> Unit,
    onSettings: () -> Unit,
) {
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            delay(16L)
            val now = System.nanoTime()
            val dt = if (last == 0L) 0.016f else (now - last) / 1_000_000_000f
            last = now
            t += dt.coerceAtMost(0.05f)
        }
    }

    val sprites = rememberMenuSprites()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GameColors.SpaceDeep, GameColors.SpaceMid))),
    ) {
        // Live space backdrop + cinematic flyby ship.
        MenuSpaceCanvas(sprites, t)
        Scanlines()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PixelTitleLine("GALAXY", GameColors.PlayerCyan, cell = 11.dp, time = t, glow = true)
            PixelTitleLine("CLASH", Color(0xFFFFFFFF), cell = 11.dp, time = t, glow = true, phase = 1.2f)
            Spacer(Modifier.height(18.dp))
            PixelTitleLine("DEFENDA A GALÁXIA", GameColors.TextMuted, cell = 2.4.dp, time = t, phase = 2.0f)
            Spacer(Modifier.height(64.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PixelMenuButton("JOGAR", onPlay, accent = GameColors.PlayerCyan)
                PixelMenuButton("MELHORIAS", onMelhorias, accent = Color(0xFFFFC93D))
                PixelMenuButton("CONFIGURAÇÕES", onSettings, accent = GameColors.AccentViolet)
            }

            Spacer(Modifier.height(40.dp))
            PixelTitleLine("LONGE DO PLANETA, A LUTA NÃO TERMINA", GameColors.TextMuted, cell = 1.7.dp, time = t, phase = 3.0f)
        }
    }
}

/**
 * A single line of bitmap pixel text, tall enough to host accent rows.
 * Internally sizes itself from the font metrics (cellular to px via density).
 */
@Composable
private fun PixelTitleLine(
    text: String,
    color: Color,
    cell: Dp,
    time: Float,
    glow: Boolean = false,
    phase: Float = 0f,
) {
    val density = LocalDensity.current
    val cellPx = with(density) { cell.toPx() }
    val box = PixelFont.textSize(text, cellPx)
    val wDp = with(density) { box.width.toDp() }
    val hDp = with(density) { box.height.toDp() }
    val bob = sin(time * 1.8f + phase) * cellPx * 0.25f

    Canvas(
        modifier = Modifier
            .width(wDp)
            .height(hDp)
            .graphicsLayer { translationY = bob },
    ) {
        if (glow) {
            val a = 0.45f + 0.35f * sin(time * 3f + phase)
            for (i in 1..4) {
                drawPixelText(text, Offset(cellPx * i * 0.5f, cellPx * i * 0.5f), cellPx, color.copy(alpha = 0.12f * a))
            }
        }
        drawPixelText(text, Offset.Zero, cellPx, color)
    }
}

// ---------------------------------------------------------------- background

private fun DrawScope.drawPx(
    img: ImageBitmap,
    cx: Float,
    cy: Float,
    w: Float,
    rotDeg: Float = 0f,
) {
    val srcW = img.width.toFloat()
    if (srcW <= 0f || w <= 0f) return
    val srcH = img.height.toFloat()
    val h = srcH * w / srcW
    val dw = w.toInt().coerceAtLeast(1)
    val dh = h.toInt().coerceAtLeast(1)
    val drawBlock: () -> Unit = {
        drawImage(
            image = img,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset((cx - w / 2f).toInt(), (cy - h / 2f).toInt()),
            dstSize = IntSize(dw, dh),
            filterQuality = FilterQuality.None,
        )
    }
    if (rotDeg == 0f) drawBlock() else rotate(rotDeg, Offset(cx, cy)) { drawBlock() }
}

private fun DrawScope.drawPlanet(cx: Float, cy: Float, r: Float) {
    drawCircle(color = Color(0xFF6BB9A8), radius = r, center = Offset(cx, cy))
    drawCircle(
        color = Color(0xFF2A5A5A),
        radius = r,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.28f),
    )
    drawCircle(
        color = Color(0xFF8C6BFF),
        radius = r * 1.9f,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.16f),
    )
}

private fun DrawScope.drawScanlines() {
    val step = 5f
    var y = 0f
    while (y < size.height) {
        drawRect(color = Color(0x11101420), topLeft = Offset(0f, y), size = Size(size.width, 1.2f))
        y += step
    }
    // vignette
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x00000000), Color(0x000B0E1E)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.height * 0.72f,
        ),
        size = this.size,
    )
}

@Composable
private fun MenuSpaceCanvas(sprites: MenuSprites, t: Float) {
    val asteroids = remember {
        listOf(
            DriftAsteroid(0.16f, -4f, 26f, 0.5f),
            DriftAsteroid(0.84f, -9f, 20f, 0.35f),
            DriftAsteroid(0.55f, -19f, 34f, 0.26f),
            DriftAsteroid(0.71f, -30f, 24f, 0.4f),
            DriftAsteroid(0.98f, -38f, 30f, 0.3f),
        )
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // twinkling stars drifting downward
        Stars.forEach { s ->
            val ny = ((s.fy * h + t * s.speed * 14f) % (h + 20f)) - 10f
            val a = 0.25f + 0.55f * (0.5f + 0.5f * sin(t * s.speed * 3f + s.phase))
            drawCircle(
                color = GameColors.StarWhite.copy(alpha = a.coerceIn(0f, 1f)),
                radius = s.size.toFloat() * (0.7f + 0.4f * sin(t * 2f + s.phase)),
                center = Offset(s.fx * w, ny.coerceAtLeast(0f)),
            )
        }

        // distant ringed planet, slow parallax
        val py = ((0.16f * h + t * 8f) % (h * 1.6f)) - h * 0.6f
        drawPlanet(w * 0.82f, py, w * 0.10f)

        // drifting asteroids (sprites)
        asteroids.forEach { a ->
            val y = ((a.spawnT * h + t * a.speed) % (h * 1.4f)) - h * 0.4f
            val x = a.fx * w
            drawPx(sprites.asteroid, x, y, a.scale * w * 0.16f, rotDeg = t * 20f + a.fx * 90f)
        }

        // shooting star climbing the screen every cycle
        val cyc = 7f
        val local = t % cyc
        if (local < 1.4f) {
            val p = local / 1.4f
            val sx = (w * 0.20f + p * w * 0.55f)
            val sy = (h * 0.55f - p * h * 0.34f)
            val tail = 70f
            drawRect(
                brush = Brush.linearGradient(listOf(Color(0x00FFFFFF), Color(0xAAFFFFFF))),
                topLeft = Offset(sx - tail / 2f, sy - 1f),
                size = Size(tail, 2.2f),
            )
            drawRect(color = Color.White, topLeft = Offset(sx, sy - 1.2f), size = Size(6f, 2.4f))
        }

        // hero ship flyby across the top, every ~5s, with an engine trail
        val fc = 5f
        val fl = t % fc
        if (fl < 2.3f) {
            val p = fl / 2.3f
            val sx = (w * (-0.15f) + p * w * 1.3f)
            val sy = h * 0.13f + sin(fl * 3f) * 8f
            drawPx(sprites.player, sx, sy, w * 0.085f, rotDeg = -4f)
            // engine flame
            drawRect(
                brush = Brush.linearGradient(listOf(Color(0x00FFC93D), Color(0x00FFFFFF))),
                topLeft = Offset(sx - w * 0.05f, sy - 1.5f),
                size = Size(w * 0.05f, 3f),
            )
        }
    }
}

@Composable
private fun Scanlines() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawScanlines()
    }
}

@Composable
private fun rememberMenuSprites(): MenuSprites {
    val res = LocalContext.current.resources
    return remember(res) {
        fun load(@DrawableRes id: Int): ImageBitmap =
            BitmapFactory.decodeResource(res, id).asImageBitmap()
        MenuSprites(
            player = load(com.cristian.galaxyclash.R.drawable.sprite_player),
            asteroid = load(com.cristian.galaxyclash.R.drawable.sprite_asteroid),
        )
    }
}