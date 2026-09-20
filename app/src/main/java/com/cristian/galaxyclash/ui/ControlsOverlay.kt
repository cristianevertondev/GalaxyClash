package com.cristian.galaxyclash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

fun Modifier.touchButton(
    onPressedChange: (Boolean) -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onPressedChange(true)
        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val isStillDown = event.changes.any { it.id == down.id && it.pressed }
        } while (isStillDown)
        onPressedChange(false)
    }
}

@Composable
fun ControlButton(modifier: Modifier = Modifier, color: Color, onPressedChange: (Boolean) -> Unit) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(color)
            .touchButton(onPressedChange)
    )
}

@Composable
fun ControlsOverlay(
    modifier: Modifier = Modifier,
    onUp: (Boolean) -> Unit,
    onDown: (Boolean) -> Unit,
    onLeft: (Boolean) -> Unit,
    onRight: (Boolean) -> Unit,
    onFire: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        // D-PAD on the left
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ControlButton(
                color = Color.White.copy(alpha = 0.2f),
                onPressedChange = onUp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(64.dp)
            ) {
                ControlButton(
                    color = Color.White.copy(alpha = 0.2f),
                    onPressedChange = onLeft
                )
                ControlButton(
                    color = Color.White.copy(alpha = 0.2f),
                    onPressedChange = onRight
                )
            }
            ControlButton(
                color = Color.White.copy(alpha = 0.2f),
                onPressedChange = onDown
            )
        }

        // Fire button on the right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 48.dp) // align visually with DPAD
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GameColors.EnemyRed.copy(alpha = 0.4f))
                    .touchButton(onFire)
            )
        }
    }
}
