package com.aegis.survival.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.aegis.survival.ui.theme.AmoledBlack
import com.aegis.survival.ui.theme.CoralPrimary
import com.aegis.survival.ui.theme.CrimsonAccent
import com.aegis.survival.ui.theme.HairlineBorder
import com.aegis.survival.ui.theme.StatusOk
import com.aegis.survival.ui.theme.SurfaceElevated
import com.aegis.survival.ui.theme.SurfaceInteractive
import com.aegis.survival.ui.theme.TextMuted
import com.aegis.survival.ui.theme.TextPrimary
import com.aegis.survival.ui.theme.TextSecondary

@Composable
fun SystemScreen(
    state: AegisUiState,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleOnlineProxy: (Boolean) -> Unit,
    onSaveProxyUrl: (String) -> Boolean,
    onClearChatHistory: () -> Unit,
    onDismissNotice: () -> Unit,
) {
    var proxyInput by rememberSaveable(state.settings.proxyBaseUrl) {
        mutableStateOf(state.settings.proxyBaseUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Offline Core Integrity Card
        Surface(
            color = SurfaceElevated,
            border = BorderStroke(1.dp, HairlineBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "OFFLINE SURVIVAL CORE",
                    style = MaterialTheme.typography.labelMedium,
                    color = CoralPrimary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                SystemStatRow("Knowledge Domains", "${state.domains.size} loaded")
                SystemStatRow("Offline Protocols", "${state.allArticlesCount} verified")
                SystemStatRow("Display Power Mode", "True AMOLED #000000")
                SystemStatRow(
                    "Network Status",
                    if (state.isOnline) "Online" else "Offline (Autonomous)",
                    valueColor = if (state.isOnline) StatusOk else TextSecondary,
                )
                SystemStatRow("App Version", "v${BuildConfig.VERSION_NAME}")
            }
        }

        // Hardware Preferences
        Surface(
            color = SurfaceElevated,
            border = BorderStroke(1.dp, HairlineBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DEVICE & TACTILE CONTROLS",
                    style = MaterialTheme.typography.labelMedium,
                    color = CoralPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                            color = TextPrimary,
                        )
                        Text(
                            text = "Vibrate on SOS toggle and emergency actions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = state.settings.hapticsEnabled,
                        onCheckedChange = onToggleHaptics,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CrimsonAccent,
                        ),
                    )
                }
            }
        }

        // Optional Server-Side AI Proxy
        Surface(
            color = SurfaceElevated,
            border = BorderStroke(1.dp, HairlineBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "OPTIONAL ONLINE AI PROXY",
                    style = MaterialTheme.typography.labelMedium,
                    color = CoralPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When connected, route queries to your self-hosted AEGIS HTTPS proxy. API keys stay strictly on the server and never live on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
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
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.settings.onlineProxyEnabled,
                        onCheckedChange = onToggleOnlineProxy,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CrimsonAccent,
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
                        focusedContainerColor = SurfaceInteractive,
                        unfocusedContainerColor = AmoledBlack,
                        focusedBorderColor = CrimsonAccent,
                        unfocusedBorderColor = HairlineBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onSaveProxyUrl(proxyInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
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
                        color = CoralPrimary,
                    )
                }
            }
        }

        // Session Reset
        OutlinedButton(
            onClick = onClearChatHistory,
            border = BorderStroke(1.dp, HairlineBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text("Clear Assistant History", color = TextPrimary)
        }
    }
}

@Composable
private fun SystemStatRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
) {
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
            color = TextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor,
        )
    }
    HorizontalDivider(color = Color(0xFF171724), thickness = 1.dp)
}
