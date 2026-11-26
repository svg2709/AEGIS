package com.aegis.survival.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.survival.AegisUiState
import com.aegis.survival.data.IndexedArticle
import com.aegis.survival.ui.theme.LocalAegisPalette

private val MORSE_SYMBOLS = listOf("•", "•", "•", "—", "—", "—", "•", "•", "•")

@Composable
fun EmergencyScreen(
    state: AegisUiState,
    onToggleSosTorch: () -> Unit,
    onToggleScreenBeacon: () -> Unit,
    onSetBoilAltitude: (Boolean) -> Unit,
    onToggleBoilTimer: () -> Unit,
    onResetBoilTimer: () -> Unit,
    onQuickTriageQuery: (String) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onOpenArticle: (IndexedArticle) -> Unit,
) {
    val palette = LocalAegisPalette.current
    var selectedLiters by rememberSaveable { mutableIntStateOf(1) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Rule of 3 Quick Triage 2x2 Grid
                Text(
                    text = "RULE OF 3 · TAP FOR IMMEDIATE ACTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentSoft,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TriageTile(
                            metric = "3 MIN",
                            label = "Airway & Bleeding",
                            onClick = { onQuickTriageQuery("Airway management and severe bleeding control") },
                            modifier = Modifier.weight(1f),
                        )
                        TriageTile(
                            metric = "3 HRS",
                            label = "Shelter & Exposure",
                            onClick = { onQuickTriageQuery("Emergency shelter hypothermia prevention") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TriageTile(
                            metric = "3 DAYS",
                            label = "Water Purification",
                            onClick = { onQuickTriageQuery("Water purification all methods") },
                            modifier = Modifier.weight(1f),
                        )
                        TriageTile(
                            metric = "3 WKS",
                            label = "Food & Edibility",
                            onClick = { onQuickTriageQuery("Universal edibility test food procurement") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Morse SOS Beacon Card
                Surface(
                    color = if (state.sosActive) palette.surfaceTinted else palette.surface,
                    border = BorderStroke(
                        1.dp,
                        if (state.sosActive) palette.danger else palette.border,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (state.sosActive) {
                                "MORSE SOS TRANSMITTING"
                            } else {
                                "EMERGENCY HARDWARE BEACON"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.sosActive) palette.danger else palette.accentSoft,
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Visual Morse step readout
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MORSE_SYMBOLS.forEachIndexed { idx, sym ->
                                val lit = state.sosActive && state.beaconLit && state.morseStepIndex == idx
                                Text(
                                    text = sym,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lit) palette.danger else palette.textMuted,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onToggleSosTorch,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.sosActive) palette.danger else palette.accent,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Icon(
                                    imageVector = if (state.sosActive) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                                    contentDescription = "SOS Torch",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.sosActive) "Stop SOS" else "Torch SOS",
                                    color = Color.White,
                                    maxLines = 1,
                                )
                            }

                            OutlinedButton(
                                onClick = onToggleScreenBeacon,
                                border = BorderStroke(1.dp, palette.border),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LightMode,
                                    contentDescription = "Screen SOS",
                                    tint = palette.accentSoft,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Screen SOS",
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                )
                            }
                        }

                        if (!state.hasFlashHardware) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Rear camera flash not detected — SOS automatically uses the screen beacon.",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted,
                            )
                        }
                    }
                }

                // Field Utility: Water Boiling Timer & Bleach Dosage Calculator
                Surface(
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.WaterDrop,
                                contentDescription = "Water safety tool",
                                tint = palette.info,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WATER PURIFICATION FIELD TOOL",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.info,
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Rolling Boil Timer
                        val mins = state.boilTimerRemainingSec / 60
                        val secs = state.boilTimerRemainingSec % 60
                        val formattedTime = String.format("%02d:%02d", mins, secs)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (state.boilTimerRunning) palette.ok else palette.textPrimary,
                                )
                                Text(
                                    text = if (state.boilTimerHighAltitude) {
                                        "Rolling boil · >2,000m altitude (3 min)"
                                    } else {
                                        "Rolling boil · Sea level (1 min)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textMuted,
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = onToggleBoilTimer,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(palette.accent, RoundedCornerShape(12.dp)),
                                ) {
                                    Icon(
                                        imageVector = if (state.boilTimerRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                        contentDescription = "Start or pause boil timer",
                                        tint = Color.White,
                                    )
                                }
                                IconButton(
                                    onClick = onResetBoilTimer,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(palette.surfaceElevated, RoundedCornerShape(12.dp)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Reset boil timer",
                                        tint = palette.textPrimary,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AltitudeToggle(
                                label = "Sea Level (1m)",
                                selected = !state.boilTimerHighAltitude,
                                onClick = { onSetBoilAltitude(false) },
                            )
                            AltitudeToggle(
                                label = ">2,000m (3m)",
                                selected = state.boilTimerHighAltitude,
                                onClick = { onSetBoilAltitude(true) },
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Unscented Bleach Dosage (5–6% sodium hypochlorite)
                        Text(
                            text = "Unscented Bleach (5–6% NaOCl) · Wait 30 min",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 4, 10, 20).forEach { liters ->
                                AltitudeToggle(
                                    label = "${liters}L",
                                    selected = selectedLiters == liters,
                                    onClick = { selectedLiters = liters },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val clearDrops = selectedLiters * 2
                        val cloudyDrops = selectedLiters * 4
                        Text(
                            text = "Clear water: $clearDrops drops · Cloudy water: $cloudyDrops drops (filter through cloth first)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                        )
                    }
                }

                // Saved Bookmarks Quick Section
                if (state.bookmarkedArticles.isNotEmpty()) {
                    Text(
                        text = "SAVED PROTOCOLS · ${state.bookmarkedArticles.size} PINNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ok,
                    )
                }
            }
        }

        if (state.bookmarkedArticles.isNotEmpty()) {
            items(state.bookmarkedArticles, key = { "saved_${it.article.id}" }) { article ->
                ArticleListRow(
                    item = article,
                    isBookmarked = true,
                    onToggleBookmark = { onToggleBookmark(article.article.id) },
                    onClick = { onOpenArticle(article) },
                )
                HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
            }
        }

        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                Text(
                    text = "CRITICAL IMMEDIATE PROTOCOLS · ${state.emergencyArticles.size} LOADED",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentSoft,
                )
            }
            HorizontalDivider(color = palette.border, thickness = 1.dp)
        }

        items(state.emergencyArticles, key = { "em_${it.domainId}_${it.article.id}" }) { article ->
            val isSaved = article.article.id in state.settings.bookmarkedArticleIds
            ArticleListRow(
                item = article,
                isBookmarked = isSaved,
                onToggleBookmark = { onToggleBookmark(article.article.id) },
                onClick = { onOpenArticle(article) },
            )
            HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
        }
    }
}

@Composable
private fun TriageTile(
    metric: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAegisPalette.current
    Surface(
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .heightIn(min = 72.dp)
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = metric,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = palette.accentSoft,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AltitudeToggle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalAegisPalette.current
    Surface(
        color = if (selected) palette.surfaceTinted else palette.surfaceElevated,
        border = BorderStroke(1.dp, if (selected) palette.accent else palette.border),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) palette.accentSoft else palette.textSecondary,
                maxLines = 1,
            )
        }
    }
}
