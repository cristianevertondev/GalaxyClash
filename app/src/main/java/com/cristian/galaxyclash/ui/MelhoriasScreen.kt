package com.cristian.galaxyclash.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import com.cristian.galaxyclash.ui.PixelFont.drawPixelText

/**
 * Placeholder for the upcoming shop/improvements screen. Kept in the nav graph
 * so "MELHORIAS" is reachable from the menu; the real shop (coins + permanent
 * skills) is planned as a next step.
 */
@Composable
fun MelhoriasScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GameColors.SpaceDeep, GameColors.SpaceMid))),
    ) {
        MenuBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PixelText("MELHORIAS", GameColors.EnemyYellow.copy(alpha = 0.95f), cell = 7.dp)
            Spacer(Modifier.height(16.dp))
            PixelText("LOJA DO PILOTO", GameColors.TextMuted, cell = 2.2.dp)
            Spacer(Modifier.height(56.dp))

            // "em breve" panel
            PixelText("EM BREVE", GameColors.PlayerCyan, cell = 3.4.dp)
            Spacer(Modifier.height(14.dp))
            PixelText("A LOJA DE MELHORIAS CHEGA JÁ", GameColors.TextMuted, cell = 1.8.dp)
            PixelText("GAST AQUI MOEDAS PARA DESBLOQUEAR", GameColors.TextMuted, cell = 1.8.dp)
            PixelText("DISPAROS E PODERES PERMANENTES", GameColors.TextMuted, cell = 1.8.dp)

            Spacer(Modifier.height(80.dp))
            PixelMenuButton(
                label = "VOLTAR",
                onClick = onBack,
                accent = GameColors.AccentViolet,
                modifier = Modifier.width(220.dp),
            )
        }
    }
}

/** Simple bitmap-pixel text line whose box sizes itself from the font. */
@Composable
private fun PixelText(text: String, color: Color, cell: Dp) {
    val density = LocalDensity.current
    val cellPx = with(density) { cell.toPx() }
    val measured = PixelFont.textSize(text, cellPx)
    val w = with(density) { measured.width.toDp() }
    val h = with(density) { measured.height.toDp() }
    Canvas(modifier = Modifier.width(w).height(h)) {
        drawPixelText(text, Offset.Zero, cellPx, color)
    }
}

/** Faint static pixel starfield + planet so the placeholder still feels alive. */
@Composable
private fun MenuBackdrop() {
    val stars = staticStars()
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color(0xFF6BB9A8).copy(alpha = 0.5f),
            radius = size.width * 0.08f,
            center = Offset(size.width * 0.85f, size.height * 0.20f),
        )
        stars.forEachIndexed { i, (fx, fy, r) ->
            val alpha = 0.35f + 0.65f * ((i * 7919) % 10) / 10f
            drawCircle(
                color = GameColors.StarWhite.copy(alpha = alpha),
                radius = r,
                center = Offset(fx * size.width, fy * size.height),
            )
        }
    }
}

private fun staticStars(): List<Triple<Float, Float, Float>> {
    val rnd = kotlin.random.Random(1234)
    return List(60) {
        Triple(rnd.nextFloat(), rnd.nextFloat(), 0.8f + rnd.nextFloat() * 1.8f)
    }
}