package com.aegis.survival

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.survival.ui.components.UpdatePromptDialog
import com.aegis.survival.ui.screens.ArticleReaderScreen
import com.aegis.survival.ui.screens.AskScreen
import com.aegis.survival.ui.screens.EmergencyScreen
import com.aegis.survival.ui.screens.LibraryScreen
import com.aegis.survival.ui.screens.SystemScreen
import com.aegis.survival.ui.theme.AegisTheme
import com.aegis.survival.ui.theme.LocalAegisPalette

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.BLACK),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.BLACK),
        )

        setContent {
            AegisRootApp(
                onSyncSystemBars = { useAmoled ->
                    val barColor = if (useAmoled) AndroidColor.BLACK else 0xFF090B12.toInt()
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(barColor),
                        navigationBarStyle = SystemBarStyle.dark(barColor),
                    )
                }
            )
        }
    }
}

@Composable
private fun AegisRootApp(
    onSyncSystemBars: (Boolean) -> Unit,
    viewModel: AegisViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.effectiveUseAmoled) {
        onSyncSystemBars(state.effectiveUseAmoled)
    }

    LaunchedEffect(state.statusNotice) {
        val notice = state.statusNotice
        if (!notice.isNullOrBlank()) {
            snackbarHostState.showSnackbar(notice)
            viewModel.clearStatusNotice()
        }
    }

    AegisTheme(
        useAmoled = state.effectiveUseAmoled,
        batterySaverActive = state.isBatterySaverActive,
        fontScale = state.settings.readingFontScale,
    ) {
        val palette = LocalAegisPalette.current
        // Guarantee top bars always clear both the notification bar and camera cutout
        val topSafeInsets = WindowInsets.statusBars
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

        BackHandler(enabled = state.activeArticle != null || state.sosScreenBeaconVisible) {
            if (state.sosScreenBeaconVisible) {
                viewModel.stopSosBeacon()
            } else if (state.activeArticle != null) {
                viewModel.closeArticle()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.canvas),
        ) {
            val activeArticle = state.activeArticle
            if (activeArticle != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.canvas)
                        .windowInsetsPadding(topSafeInsets)
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    ArticleReaderScreen(
                        article = activeArticle,
                        isBookmarked = activeArticle.article.id in state.settings.bookmarkedArticleIds,
                        fontScale = state.settings.readingFontScale,
                        onAdjustFontScale = viewModel::setReadingFontScale,
                        onToggleBookmark = { viewModel.toggleBookmark(activeArticle.article.id) },
                        onBack = viewModel::closeArticle,
                        onShare = { viewModel.shareArticle(activeArticle) },
                        onAskFollowUp = { prompt -> viewModel.askAboutArticle(activeArticle, prompt) },
                    )
                }
            } else {
                Scaffold(
                    containerColor = palette.canvas,
                    // Insets are handled explicitly on topBar, bottomBar, and content IME padding
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.canvas)
                                .windowInsetsPadding(topSafeInsets),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "AEGIS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentSoft,
                                )

                                if (state.availableUpdate != null && !state.showUpdateDialog) {
                                    Row(
                                        modifier = Modifier
                                            .clickable { viewModel.openUpdatePrompt() }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SystemUpdate,
                                            contentDescription = "Update available",
                                            tint = palette.accentSoft,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "v${state.availableUpdate!!.latestVersion}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.accentSoft,
                                        )
                                    }
                                } else {
                                    Text(
                                        text = when {
                                            state.sosActive -> "SOS ACTIVE"
                                            state.isBatterySaverActive -> "AMOLED SAVER · ${state.batteryStatus.batteryPercent}%"
                                            state.settings.onlineProxyEnabled && state.isOnline && state.settings.proxyBaseUrl.isNotBlank() -> "HYBRID"
                                            else -> "OFFLINE"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            state.sosActive -> palette.danger
                                            state.isBatterySaverActive -> palette.ok
                                            else -> palette.textMuted
                                        },
                                    )
                                }
                            }
                            HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
                        }
                    },
                    bottomBar = {
                        Column(modifier = Modifier.background(palette.canvas)) {
                            HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
                            NavigationBar(
                                containerColor = palette.canvas,
                                contentColor = palette.textPrimary,
                                tonalElevation = 0.dp,
                            ) {
                                AegisTab.entries.forEach { tab ->
                                    val icon = when (tab) {
                                        AegisTab.ASK -> Icons.Outlined.Forum
                                        AegisTab.LIBRARY -> Icons.Outlined.AutoStories
                                        AegisTab.EMERGENCY -> Icons.Outlined.HealthAndSafety
                                        AegisTab.SYSTEM -> Icons.Outlined.Tune
                                    }
                                    NavigationBarItem(
                                        selected = state.selectedTab == tab,
                                        onClick = { viewModel.selectTab(tab) },
                                        icon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = tab.label,
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = palette.accentSoft,
                                            selectedTextColor = palette.accentSoft,
                                            unselectedIconColor = palette.textMuted,
                                            unselectedTextColor = palette.textMuted,
                                            indicatorColor = palette.surfaceElevated,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    // consumeWindowInsets(innerPadding) + imePadding() lifts the bottom composer
                    // cleanly right above the software keyboard with zero overlap or gap.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.canvas)
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                            .imePadding(),
                    ) {
                        when (state.selectedTab) {
                            AegisTab.ASK -> AskScreen(
                                state = state,
                                onSendQuery = viewModel::sendQuery,
                                onToggleEmergencyMode = viewModel::toggleEmergencyMode,
                                onSelectOnlineChatMode = viewModel::setOnlineChatMode,
                                onOpenArticle = viewModel::openArticle,
                                onCopyMessage = viewModel::copyMessageText,
                                onResetChat = viewModel::clearChatHistory,
                            )

                            AegisTab.LIBRARY -> LibraryScreen(
                                state = state,
                                onSearchChange = viewModel::updateSearchQuery,
                                onToggleSavedOnly = viewModel::toggleSavedOnlyFilter,
                                onSelectTier = viewModel::selectTierFilter,
                                onSelectPriority = viewModel::selectPriorityFilter,
                                onSelectDomain = viewModel::selectDomainFilter,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onOpenArticle = viewModel::openArticle,
                                onAskFallback = viewModel::sendQuery,
                            )

                            AegisTab.EMERGENCY -> EmergencyScreen(
                                state = state,
                                onToggleSosTorch = { viewModel.toggleSosTorch(screenOnly = false) },
                                onToggleScreenBeacon = { viewModel.toggleSosTorch(screenOnly = true) },
                                onSetBoilAltitude = viewModel::setBoilAltitudeMode,
                                onToggleBoilTimer = viewModel::toggleBoilTimer,
                                onResetBoilTimer = viewModel::resetBoilTimer,
                                onQuickTriageQuery = viewModel::sendQuery,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onOpenArticle = viewModel::openArticle,
                            )

                            AegisTab.SYSTEM -> SystemScreen(
                                state = state,
                                onSetDisplayThemeMode = viewModel::setDisplayThemeMode,
                                onToggleAutoBatterySaverAmoled = viewModel::toggleAutoBatterySaverAmoled,
                                onToggleManualBatterySaver = viewModel::toggleManualBatterySaver,
                                onSetReadingFontScale = viewModel::setReadingFontScale,
                                onToggleAutoCheckUpdates = viewModel::toggleAutoCheckUpdates,
                                onCheckForUpdates = { viewModel.checkForUpdates(userInitiated = true) },
                                onOpenUpdatePrompt = viewModel::openUpdatePrompt,
                                onToggleHaptics = viewModel::toggleHaptics,
                                onToggleOnlineProxy = viewModel::toggleOnlineProxy,
                                onSaveProxyUrl = viewModel::saveProxyUrl,
                                onClearChatHistory = viewModel::clearChatHistory,
                                onDismissNotice = viewModel::clearStatusNotice,
                            )
                        }
                    }
                }
            }

            if (state.sosScreenBeaconVisible) {
                SosBeaconOverlay(
                    beaconLit = state.beaconLit,
                    onStop = viewModel::stopSosBeacon,
                )
            }

            val pendingUpdate = state.availableUpdate
            if (state.showUpdateDialog && pendingUpdate != null) {
                UpdatePromptDialog(
                    updateInfo = pendingUpdate,
                    isDownloading = state.isDownloadingUpdate,
                    downloadProgress = state.updateDownloadProgress,
                    needsInstallPermission = state.needsInstallPermission,
                    statusMessage = state.updateStatusMessage,
                    onConfirmUpdate = viewModel::confirmAndInstallUpdate,
                    onOpenPermissionSettings = viewModel::openInstallUnknownAppsSettings,
                    onDismiss = viewModel::dismissUpdateDialog,
                )
            }
        }
    }
}

@Composable
private fun SosBeaconOverlay(
    beaconLit: Boolean,
    onStop: () -> Unit,
) {
    val palette = LocalAegisPalette.current
    val bg = if (beaconLit) Color(0xFFFF1E3C) else Color.Black
    val fg = if (beaconLit) Color.White else palette.accentSoft
    val topSafeInsets = WindowInsets.statusBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .windowInsetsPadding(topSafeInsets)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "S · O · S",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = fg,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "••• ——— •••",
                style = MaterialTheme.typography.titleMedium,
                color = fg,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated),
                modifier = Modifier.heightIn(min = 52.dp),
            ) {
                Text(
                    text = "STOP SOS BEACON",
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}
