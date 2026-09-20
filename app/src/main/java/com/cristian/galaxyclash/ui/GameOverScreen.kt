package com.cristian.galaxyclash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * End-of-round screen: score, enemies destroyed, level reached, and the two
 * primary actions.
 */
@Composable
fun GameOverScreen(
    score: Int,
    enemiesDestroyed: Int,
    level: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GameColors.SpaceDeep, GameColors.SpaceMid)))
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "GAME OVER",
            color = GameColors.EnemyMagenta,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 5.sp,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatRow("PONTUAÇÃO", score.toString().padStart(6, '0'))
            StatRow("INIMIGOS DESTRUÍDOS", enemiesDestroyed.toString())
            StatRow("FASE ALCANÇADA", level.toString())
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MenuActionButton("JOGAR NOVAMENTE", onRestart)
            MenuActionButton("MENU", onMenu)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = GameColors.TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Start,
        )
        Text(
            text = value,
            color = GameColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
}