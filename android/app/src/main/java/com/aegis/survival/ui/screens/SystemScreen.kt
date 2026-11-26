package com.aegis.survival.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aegis.survival.AegisUiState
import com.aegis.survival.BuildConfig
import com.aegis.survival.data.DisplayThemeMode
import com.aegis.survival.ui.theme.LocalAegisPalette

@Composable
fun SystemScreen(
    state: AegisUiState,
    onSetDisplayThemeMode: (DisplayThemeMode) -> Unit,
    onToggleAutoBatterySaverAmoled: (Boolean) -> Unit,
    onToggleManualBatterySaver: (Boolean) -> Unit,
    onSetReadingFontScale: (Float) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenUpdatePrompt: () -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleOnlineProxy: (Boolean) -> Unit,
    onSaveProxyUrl: (String) -> Boolean,
    onClearChatHistory: () -> Unit,
    onDismissNotice: () -> Unit,
) {
    val palette = LocalAegisPalette.current
    var proxyInput by rememberSaveable(state.settings.proxyBaseUrl) {
        mutableStateOf(state.settings.proxyBaseUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. App Auto-Update Card (always asks before downloading/installing)
        Surface(
            color = if (state.availableUpdate != null) palette.surfaceTinted else palette.surface,
            border = BorderStroke(
                1.dp,
                if (state.availableUpdate != null) palette.accent else palette.border,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = "App Updates",
                        tint = palette.accentSoft,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "APP UPDATES · GITHUB RELEASES",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentSoft,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                SystemStatRow("Installed Version", "v${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
                SystemStatRow("Release Source", BuildConfig.GITHUB_REPO)

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Check on Launch",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                        )
                        Text(
                            text = "Always asks your permission before downloading or installing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                        )
                    }
                    Switch(
                        checked = state.settings.autoCheckUpdates,
                        onCheckedChange = onToggleAutoCheckUpdates,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.accent,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.availableUpdate != null) {
                    Button(
                        onClick = onOpenUpdatePrompt,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = "Review & Update to v${state.availableUpdate.latestVersion}",
                            color = Color.White,
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onCheckForUpdates,
                        enabled = !state.isCheckingUpdate,
                        border = BorderStroke(1.dp, palette.border),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = if (state.isCheckingUpdate) "Checking Releases..." else "Check for Updates",
                            color = palette.textPrimary,
                        )
                    }
                }

                if (!state.updateStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.updateStatusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentSoft,
                    )
                }
            }
        }

        // 2. Battery Saver & AMOLED Display Card
        Surface(
            color = palette.surface,
            border = BorderStroke(
                1.dp,
                if (state.isBatterySaverActive) palette.ok.copy(alpha = 0.6f) else palette.border,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.BatterySaver,
                        contentDescription = "Battery saver",
                        tint = if (state.isBatterySaverActive) palette.ok else palette.accentSoft,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BATTERY SAVER & AMOLED DISPLAY",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.isBatterySaverActive) palette.ok else palette.accentSoft,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                SystemStatRow(
                    "Phone Battery",
                    "${state.batteryStatus.batteryPercent}%${if (state.batteryStatus.isCharging) " · Charging" else ""}",
                    valueColor = if (state.batteryStatus.batteryPercent <= 20 && !state.batteryStatus.isCharging) palette.warn else palette.textPrimary,
                )
                SystemStatRow(
                    "Android Battery Saver",
                    if (state.batteryStatus.isSystemPowerSave) "DETECTED (ON)" else "OFF",
                    valueColor = if (state.batteryStatus.isSystemPowerSave) palette.ok else palette.textSecondary,
                )
                SystemStatRow(
                    "Active Display Profile",
                    when {
                        state.isBatterySaverActive -> "Ultra AMOLED #000000 (Power Save)"
                        state.effectiveUseAmoled -> "True AMOLED #000000"
                        else -> "Obsidian Dark"
                    },
                    valueColor = if (state.effectiveUseAmoled) palette.ok else palette.textPrimary,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto AMOLED on Battery Saver",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                        )
                        Text(
                            text = "Detects phone Battery Saver and enforces pure #000000 OLED settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                        )
                    }
                    Switch(
                        checked = state.settings.autoBatterySaverAmoled,
                        onCheckedChange = onToggleAutoBatterySaverAmoled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.ok,
                        ),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manual Battery Saver Mode",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                        )
                        Text(
                            text = "Force Ultra AMOLED #000000 and pause routine haptics & update checks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                        )
                    }
                    Switch(
                        checked = state.settings.manualBatterySaver,
                        onCheckedChange = onToggleManualBatterySaver,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.ok,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DEFAULT SURFACE STYLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DisplayThemeMode.entries.forEach { mode ->
                        val selected = state.settings.displayThemeMode == mode
                        Surface(
                            color = if (selected) palette.surfaceTinted else palette.surfaceElevated,
                            border = BorderStroke(1.dp, if (selected) palette.accent else palette.border),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clickable { onSetDisplayThemeMode(mode) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) palette.accentSoft else palette.textSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "READING LEGIBILITY SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        0.92f to "S",
                        1.0f to "M",
                        1.14f to "L",
                        1.28f to "XL",
                    ).forEach { (scale, label) ->
                        val selected = kotlin.math.abs(state.settings.readingFontScale - scale) < 0.05f
                        Surface(
                            color = if (selected) palette.surfaceTinted else palette.surfaceElevated,
                            border = BorderStroke(1.dp, if (selected) palette.accent else palette.border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .clickable { onSetReadingFontScale(scale) },
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) palette.accentSoft else palette.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Offline Core Integrity Card
        Surface(
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "OFFLINE SURVIVAL CORE",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentSoft,
                )
                Spacer(modifier = Modifier.height(10.dp))
                SystemStatRow("Knowledge Domains", "${state.domains.size} loaded")
                SystemStatRow("Offline Protocols", "${state.allArticlesCount} verified")
                SystemStatRow("Saved Bookmarks", "${state.settings.bookmarkedArticleIds.size} pinned")
                SystemStatRow(
                    "Network Status",
                    if (state.isOnline) "Online" else "Offline (Autonomous)",
                    valueColor = if (state.isOnline) palette.ok else palette.textSecondary,
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Feedback",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                        )
                        Text(
                            text = "Vibrate on SOS triggers and critical actions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                        )
                    }
                    Switch(
                        checked = state.settings.hapticsEnabled,
                        onCheckedChange = onToggleHaptics,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.accent,
                        ),
                    )
                }
            }
        }

        // 4. Optional Server-Side AI Proxy
        Surface(
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "OPTIONAL ONLINE AI PROXY",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentSoft,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When connected, route queries to your self-hosted AEGIS HTTPS proxy. API keys stay strictly on the server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Enable Online AI Proxy",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.settings.onlineProxyEnabled,
                        onCheckedChange = onToggleOnlineProxy,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.accent,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = proxyInput,
                    onValueChange = {
                        proxyInput = it
                        onDismissNotice()
                    },
                    label = { Text("HTTPS Proxy Base URL") },
                    placeholder = { Text("https://your-aegis-server.example.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.surfaceElevated,
                        unfocusedContainerColor = palette.canvas,
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onSaveProxyUrl(proxyInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text("Save Proxy Endpoint", color = Color.White)
                }

                if (!state.statusNotice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.statusNotice,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentSoft,
                    )
                }
            }
        }

        // Session Reset
        OutlinedButton(
            onClick = onClearChatHistory,
            border = BorderStroke(1.dp, palette.border),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text("Clear Assistant History", color = palette.textPrimary)
        }
    }
}

@Composable
private fun SystemStatRow(
    label: String,
    value: String,
    valueColor: Color = LocalAegisPalette.current.textPrimary,
) {
    val palette = LocalAegisPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor,
        )
    }
    HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
}
