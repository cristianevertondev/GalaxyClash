package com.cristian.galaxyclash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristian.galaxyclash.game.GameConfig
import com.cristian.galaxyclash.game.GameSnapshot
import com.cristian.galaxyclash.game.Player
import com.cristian.galaxyclash.game.Boss

/**
 * Minimal, discreet HUD overlaid on the play field: score, HP pips, extra lives,
 * level, active power-up timers, and the pause affordance. Deliberately sparse
 * so it never hides the action.
 */
@Composable
fun Hud(
    snapshot: GameSnapshot,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = snapshot.player
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "SCORE",
                    color = GameColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot.score.toString().padStart(6, '0'),
                        color = GameColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (p.hasScoreMultiplier) {
                        Text(
                            text = " x${p.scoreMultiplier}",
                            color = GameColors.PickupGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = snapshot.stageName,
                    color = GameColors.AccentViolet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(end = 12.dp),
                )
                ExtraLives(p.extraLives)
                HpPips(hp = p.hp)
                PauseButton(onClick = onPause)
            }
        }

        // Active power-up timers (only what's currently running).
        ActivePowerUps(p)

        // Boss health bar when a boss is on the field.
        snapshot.boss?.let { b -> BossBar(b) }
    }
}

/** Renders a compact list of active power-up chips with remaining-time bars. */
@Composable
private fun ActivePowerUps(p: Player) {
    val items = buildList<Pair<String, Float>> {
        if (p.hasRapidFire) add("RAPID FIRE" to p.rapidFireTimer / GameConfig.RAPID_FIRE_DURATION_SECONDS)
        if (p.isShielded) add("SHIELD" to p.shieldTimer / GameConfig.SHIELD_DURATION_SECONDS)
        if (p.hasDamageBoost) add("DAMAGE x${p.shotDamage}" to p.damageBoostTimer / GameConfig.DAMAGE_BOOST_DURATION_SECONDS)
        if (p.magnetTimer > 0f) add("MAGNET" to p.magnetTimer / GameConfig.MAGNET_DURATION_SECONDS)
    }
    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { (label, frac) -> PowerUpChip(label, frac) }
            }
        }
    }
}

@Composable
private fun PowerUpChip(label: String, fraction: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = ChipShape,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = GameColors.PickupGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        // Remaining-time bar.
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 4.dp)
                .background(GameColors.HpLost, ChipShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .size(width = 34.dp, height = 4.dp)
                    .background(GameColors.PickupGreen, ChipShape),
            )
        }
    }
}

private val ChipShape: Shape = RoundedCornerShape(4.dp)

@Composable
private fun BossBar(b: Boss) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "${b.spec.name} · ${b.hp}",
            color = GameColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        val frac = (b.hp.toFloat() / b.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFF2A0E14), RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .height(10.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(GameColors.EnemyRed, GameColors.EnemyOrange),
                        ),
                        RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}

@Composable
private fun ExtraLives(lives: Int) {
    if (lives > 0) {
        Text(
            text = "♥x$lives",
            color = GameColors.EnemyRed,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 10.dp),
        )
    }
}

@Composable
private fun HpPips(hp: Int) {
    val max = GameConfig.PLAYER_MAX_HP
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max) { i ->
            val filled = i < hp
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 6.dp)
                    .background(
                        color = if (filled) GameColors.HpBar else GameColors.HpLost,
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

@Composable
private fun PauseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Two bars = pause glyph
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(
                Modifier
                    .size(4.dp, 14.dp)
                    .background(GameColors.TextPrimary, RoundedCornerShape(1.dp)),
            )
            Box(
                Modifier
                    .size(4.dp, 14.dp)
                    .background(GameColors.TextPrimary, RoundedCornerShape(1.dp)),
            )
        }
    }
}