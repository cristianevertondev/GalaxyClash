package com.cristian.galaxyclash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.cristian.galaxyclash.game.ControlStyle

/**
 * A scrollable, dimmed, simple settings panel. Only the presence of toggles is
 * wired for now (per the MVP scope); the actual persistence lands later.
 */
@Composable
fun SettingsOverlay(
    soundOn: Boolean,
    musicOn: Boolean,
    vibrationOn: Boolean,
    controlStyle: ControlStyle,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleVibration: () -> Unit,
    onControlStyleChange: (ControlStyle) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GameColors.SpaceDeep, GameColors.SpaceMid)))
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "CONFIGURAÇÕES",
            color = GameColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ToggleRow("SOM", soundOn, onToggleSound)
            ToggleRow("MÚSICA", musicOn, onToggleMusic)
            ToggleRow("VIBRAÇÃO", vibrationOn, onToggleVibration)
            ControlStyleSelector(controlStyle, onControlStyleChange)
        }
        Column(
            modifier = Modifier.padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MenuActionButton("VOLTAR", onClose)
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onToggle: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = GameColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        )
        val text = if (value) "LIGADO" else "DESLIGADO"
        Text(
            text = text,
            color = if (value) GameColors.Laser else GameColors.TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ControlStyleSelector(
    currentStyle: ControlStyle,
    onStyleChange: (ControlStyle) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable {
                onStyleChange(
                    if (currentStyle == ControlStyle.DRAG) ControlStyle.DPAD else ControlStyle.DRAG
                )
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "CONTROLES",
            color = GameColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        )
        val text = if (currentStyle == ControlStyle.DRAG) "ARRASTAR" else "D-PAD"
        Text(
            text = text,
            color = GameColors.Laser,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}