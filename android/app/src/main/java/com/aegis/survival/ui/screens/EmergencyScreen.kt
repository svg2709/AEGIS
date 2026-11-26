package com.aegis.survival.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aegis.survival.AegisUiState
import com.aegis.survival.data.IndexedArticle
import com.aegis.survival.ui.theme.AmoledBlack
import com.aegis.survival.ui.theme.CoralPrimary
import com.aegis.survival.ui.theme.CrimsonAccent
import com.aegis.survival.ui.theme.HairlineBorder
import com.aegis.survival.ui.theme.StatusDanger
import com.aegis.survival.ui.theme.SurfaceElevated
import com.aegis.survival.ui.theme.TextMuted
import com.aegis.survival.ui.theme.TextPrimary
import com.aegis.survival.ui.theme.TextSecondary

@Composable
fun EmergencyScreen(
    state: AegisUiState,
    onToggleSosTorch: () -> Unit,
    onToggleScreenBeacon: () -> Unit,
    onOpenArticle: (IndexedArticle) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = SurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (state.sosActive) StatusDanger else HairlineBorder,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (state.sosActive) {
                                "MORSE SOS ACTIVE · ... --- ..."
                            } else {
                                "EMERGENCY HARDWARE BEACON"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.sosActive) StatusDanger else CoralPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Transmit international Morse SOS using the rear camera flash, or use the screen beacon if no flash is available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onToggleSosTorch,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.sosActive) StatusDanger else CrimsonAccent,
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
                                border = BorderStroke(1.dp, HairlineBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LightMode,
                                    contentDescription = "Screen SOS",
                                    tint = CoralPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Screen SOS",
                                    color = TextPrimary,
                                    maxLines = 1,
                                )
                            }
                        }

                        if (!state.hasFlashHardware) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Rear camera flash not detected — SOS will automatically use the screen beacon.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                    }
                }

                Text(
                    text = "CRITICAL IMMEDIATE PROTOCOLS · ${state.emergencyArticles.size} LOADED",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoralPrimary,
                )
            }

            HorizontalDivider(color = HairlineBorder, thickness = 1.dp)
        }

        items(state.emergencyArticles, key = { "em_${it.domainId}_${it.article.id}" }) { article ->
            ArticleListRow(
                item = article,
                onClick = { onOpenArticle(article) },
            )
            HorizontalDivider(color = Color(0xFF151520), thickness = 1.dp)
        }
    }
}
