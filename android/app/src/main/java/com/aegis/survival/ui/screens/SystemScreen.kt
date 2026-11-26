package com.aegis.survival.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // 1. Display & Battery Saver
        SectionHeading("DISPLAY & BATTERY")
        SettingsInfoRow(
            label = "Battery Status",
            value = "${state.batteryStatus.batteryPercent}%${if (state.batteryStatus.isSystemPowerSave) " · System Saver ON" else ""}",
            valueColor = if (state.batteryStatus.isSystemPowerSave) palette.ok else palette.textPrimary,
        )
        SettingsSwitchRow(
            title = "Auto AMOLED on Battery Saver",
            subtitle = "Use pitch-black #000000 when phone Battery Saver activates",
            checked = state.settings.autoBatterySaverAmoled,
            onCheckedChange = onToggleAutoBatterySaverAmoled,
            accentColor = palette.ok,
        )
        SettingsSwitchRow(
            title = "Manual Battery Saver",
            subtitle = "Force Ultra AMOLED and pause routine haptics",
            checked = state.settings.manualBatterySaver,
            onCheckedChange = onToggleManualBatterySaver,
            accentColor = palette.ok,
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DisplayThemeMode.entries.forEach { mode ->
                val selected = state.settings.displayThemeMode == mode
                Surface(
                    color = if (selected) palette.surfaceTinted else palette.surface,
                    border = BorderStroke(1.dp, if (selected) palette.accent else palette.borderSubtle),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clickable { onSetDisplayThemeMode(mode) },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (mode == DisplayThemeMode.AMOLED) "AMOLED #000" else "Obsidian",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.accentSoft else palette.textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0.92f to "S", 1.0f to "M", 1.14f to "L", 1.28f to "XL").forEach { (scale, label) ->
                val selected = kotlin.math.abs(state.settings.readingFontScale - scale) < 0.05f
                Surface(
                    color = if (selected) palette.surfaceTinted else palette.surface,
                    border = BorderStroke(1.dp, if (selected) palette.accent else palette.borderSubtle),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .clickable { onSetReadingFontScale(scale) },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.accentSoft else palette.textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        // 2. App Updates
        SectionHeading("APP UPDATES")
        SettingsInfoRow("Version", "v${BuildConfig.VERSION_NAME}")
        SettingsSwitchRow(
            title = "Auto-Check GitHub Releases",
            subtitle = "Asks your confirmation before downloading or installing",
            checked = state.settings.autoCheckUpdates,
            onCheckedChange = onToggleAutoCheckUpdates,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (state.availableUpdate != null) {
            Button(
                onClick = onOpenUpdatePrompt,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp),
            ) {
                Text("Review Update v${state.availableUpdate.latestVersion}", color = Color.White)
            }
        } else {
            OutlinedButton(
                onClick = onCheckForUpdates,
                enabled = !state.isCheckingUpdate,
                border = BorderStroke(1.dp, palette.border),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp),
            ) {
                Text(
                    text = if (state.isCheckingUpdate) "Checking..." else "Check for Updates",
                    color = palette.textPrimary,
                )
            }
        }
        if (!state.updateStatusMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.updateStatusMessage,
                style = MaterialTheme.typography.labelSmall,
                color = palette.accentSoft,
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        // 3. Optional AI Proxy (URL input only expands when toggled ON)
        SectionHeading("ONLINE AI PROXY")
        SettingsSwitchRow(
            title = "Use Server AI Proxy",
            subtitle = "Optional Gemini chat & grounding with automatic offline fallback",
            checked = state.settings.onlineProxyEnabled,
            onCheckedChange = onToggleOnlineProxy,
        )
        if (state.settings.onlineProxyEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = proxyInput,
                onValueChange = {
                    proxyInput = it
                    onDismissNotice()
                },
                label = { Text("HTTPS Server URL") },
                placeholder = { Text("https://your-aegis-server.example.com") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.surfaceElevated,
                    unfocusedContainerColor = palette.surface,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.borderSubtle,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSaveProxyUrl(proxyInput) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
            ) {
                Text("Save Server URL", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        // 4. Offline Core & Haptics
        SectionHeading("OFFLINE DATA & DEVICE")
        SettingsInfoRow("Bundled Guides", "${state.allArticlesCount} guides · ${state.domains.size} domains")
        SettingsSwitchRow(
            title = "Haptic Feedback",
            subtitle = "Vibrate on SOS and key actions",
            checked = state.settings.hapticsEnabled,
            onCheckedChange = onToggleHaptics,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onClearChatHistory,
            border = BorderStroke(1.dp, palette.borderSubtle),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
        ) {
            Text("Clear Chat History", color = palette.textSecondary)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeading(text: String) {
    val palette = LocalAegisPalette.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = palette.accentSoft,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    valueColor: Color = LocalAegisPalette.current.textPrimary,
) {
    val palette = LocalAegisPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor,
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = LocalAegisPalette.current.accent,
) {
    val palette = LocalAegisPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
            ),
        )
    }
}
