package com.cristian.galaxyclash.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import com.cristian.galaxyclash.ui.PixelFont.drawPixelText

/**
 * Chunky pixel-art menu button: a beveled panel + bitmap pixel-font label,
 * with a press squish. No anti-aliased system text involved.
 */
@Composable
fun PixelMenuButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = GameColors.PlayerCyan,
    height: androidx.compose.ui.unit.Dp = 58.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "btn")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            drawPixelPanel(size, accent)
            val edge = size.height * 0.12f
            val availW = size.width - edge * 2.4f
            val availH = size.height - edge * 2.4f
            var cell = availH / 8f
            val oneW = PixelFont.textWidth(label, 1f)
            if (oneW > 0f && PixelFont.textWidth(label, cell) > availW) {
                cell = availW / oneW
            }
            val tw = PixelFont.textWidth(label, cell)
            val th = PixelFont.lineHeightPx(cell, label)
            drawPixelText(
                text = label,
                origin = Offset((size.width - tw) / 2f, (size.height - th) / 2f),
                scale = cell,
                color = Color.White,
                spacing = cell * 0.4f,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelPanel(
    size: Size,
    accent: Color,
) {
    val dark = Color(0xFF060814)
    val edgeCol = Color(0xFF1A2340)
    val hi = Color(0x66FFFFFF)

    // outer dark casing
    drawRect(color = edgeCol, size = size)
    drawRect(
        color = dark,
        topLeft = Offset(2f, 2f),
        size = Size(size.width - 4f, size.height - 4f),
    )
    // inner fill gradient
    drawRect(
        brush = Brush.verticalGradient(
            listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.55f), accent.copy(alpha = 0.85f)),
        ),
        topLeft = Offset(6f, 6f),
        size = Size(size.width - 8f, size.height - 8f),
    )
    // chunky top highlight + bottom shade (bevel)
    drawRect(color = hi, topLeft = Offset(6f, 6f), size = Size(size.width - 8f, 3f))
    drawRect(
        color = Color(0x99000000),
        topLeft = Offset(6f, size.height - 9f),
        size = Size(size.width - 8f, 3f),
    )
    // corner notches to suggest rounded pixel corners
    drawRect(color = dark, topLeft = Offset(0f, 0f), size = Size(2f, 2f))
    drawRect(color = dark, topLeft = Offset(size.width - 2f, 0f), size = Size(2f, 2f))
    drawRect(color = dark, topLeft = Offset(0f, size.height - 2f), size = Size(2f, 2f))
    drawRect(color = dark, topLeft = Offset(size.width - 2f, size.height - 2f), size = Size(2f, 2f))
}