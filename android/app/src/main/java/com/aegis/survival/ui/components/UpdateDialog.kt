package com.aegis.survival.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aegis.survival.BuildConfig
import com.aegis.survival.ui.theme.LocalAegisPalette
import com.aegis.survival.update.AppUpdateInfo
import kotlin.math.roundToInt

@Composable
fun UpdatePromptDialog(
    updateInfo: AppUpdateInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    needsInstallPermission: Boolean,
    statusMessage: String?,
    onConfirmUpdate: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalAegisPalette.current
    val sizeMb = if (updateInfo.apkSizeBytes > 0L) {
        String.format("%.1f MB", updateInfo.apkSizeBytes / (1024.0 * 1024.0))
    } else {
        "APK"
    }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        containerColor = palette.surfaceElevated,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        shape = RoundedCornerShape(18.dp),
        title = {
            Column {
                Text(
                    text = "Update AEGIS to v${updateInfo.latestVersion}?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Installed v${BuildConfig.VERSION_NAME} → New v${updateInfo.latestVersion} · $sizeMb",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentSoft,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "A newer verified GitHub Release is available. Do you want to download and install this update now?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "WHAT'S NEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = palette.canvas,
                    border = BorderStroke(1.dp, palette.borderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                ) {
                    Text(
                        text = updateInfo.changelog.take(1500),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                    )
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        color = palette.accent,
                        trackColor = palette.border,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Downloading & verifying SHA-256 · ${(downloadProgress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentSoft,
                    )
                } else if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (needsInstallPermission) palette.warn else palette.accentSoft,
                    )
                }
            }
        },
        confirmButton = {
            if (needsInstallPermission) {
                Button(
                    onClick = onOpenPermissionSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text("Allow Install", color = Color.White)
                }
            } else {
                Button(
                    onClick = onConfirmUpdate,
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text(
                        text = if (isDownloading) "Downloading..." else "Update Now",
                        color = Color.White,
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isDownloading,
                border = BorderStroke(1.dp, palette.border),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 44.dp),
            ) {
                Text("Later", color = palette.textPrimary)
            }
        },
    )
}
