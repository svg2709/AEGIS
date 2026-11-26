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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.HealthAndSafety
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.survival.ui.screens.ArticleReaderScreen
import com.aegis.survival.ui.screens.AskScreen
import com.aegis.survival.ui.screens.EmergencyScreen
import com.aegis.survival.ui.screens.LibraryScreen
import com.aegis.survival.ui.screens.SystemScreen
import com.aegis.survival.ui.theme.AegisTheme
import com.aegis.survival.ui.theme.AmoledBlack
import com.aegis.survival.ui.theme.CoralPrimary
import com.aegis.survival.ui.theme.CrimsonAccent
import com.aegis.survival.ui.theme.HairlineBorder
import com.aegis.survival.ui.theme.StatusDanger
import com.aegis.survival.ui.theme.SurfaceElevated
import com.aegis.survival.ui.theme.TextMuted
import com.aegis.survival.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pure AMOLED #000000 system bars for maximum OLED battery conservation
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.BLACK),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.BLACK),
        )

        setContent {
            AegisTheme {
                AegisRootApp()
            }
        }
    }
}

@Composable
private fun AegisRootApp(
    viewModel: AegisViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            .background(AmoledBlack),
    ) {
        val activeArticle = state.activeArticle
        if (activeArticle != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmoledBlack)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                ArticleReaderScreen(
                    article = activeArticle,
                    onBack = viewModel::closeArticle,
                    onShare = { viewModel.shareArticle(activeArticle) },
                    onAskFollowUp = { prompt ->
                        viewModel.sendQuery(prompt)
                    },
                )
            }
        } else {
            Scaffold(
                containerColor = AmoledBlack,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Column(modifier = Modifier.background(AmoledBlack)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "AEGIS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CoralPrimary,
                            )
                            Text(
                                text = if (state.sosActive) {
                                    "SOS ACTIVE"
                                } else if (state.settings.onlineProxyEnabled && state.isOnline && state.settings.proxyBaseUrl.isNotBlank()) {
                                    "HYBRID ONLINE"
                                } else {
                                    "OFFLINE · ${state.allArticlesCount} PROTOCOLS"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.sosActive) StatusDanger else TextMuted,
                            )
                        }
                        HorizontalDivider(color = HairlineBorder, thickness = 1.dp)
                    }
                },
                bottomBar = {
                    Column(modifier = Modifier.background(AmoledBlack)) {
                        HorizontalDivider(color = HairlineBorder, thickness = 1.dp)
                        NavigationBar(
                            containerColor = AmoledBlack,
                            contentColor = TextPrimary,
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
                                        selectedIconColor = CoralPrimary,
                                        selectedTextColor = CoralPrimary,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = SurfaceElevated,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AmoledBlack)
                        .padding(innerPadding),
                ) {
                    when (state.selectedTab) {
                        AegisTab.ASK -> AskScreen(
                            state = state,
                            onSendQuery = viewModel::sendQuery,
                            onToggleEmergencyMode = viewModel::toggleEmergencyMode,
                            onOpenArticle = viewModel::openArticle,
                        )

                        AegisTab.LIBRARY -> LibraryScreen(
                            state = state,
                            onSearchChange = viewModel::updateSearchQuery,
                            onSelectTier = viewModel::selectTierFilter,
                            onSelectPriority = viewModel::selectPriorityFilter,
                            onSelectDomain = viewModel::selectDomainFilter,
                            onOpenArticle = viewModel::openArticle,
                            onAskFallback = viewModel::sendQuery,
                        )

                        AegisTab.EMERGENCY -> EmergencyScreen(
                            state = state,
                            onToggleSosTorch = { viewModel.toggleSosTorch(screenOnly = false) },
                            onToggleScreenBeacon = { viewModel.toggleSosTorch(screenOnly = true) },
                            onOpenArticle = viewModel::openArticle,
                        )

                        AegisTab.SYSTEM -> SystemScreen(
                            state = state,
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

        // Full-screen SOS visual beacon overlay
        if (state.sosScreenBeaconVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (state.beaconLit) CrimsonAccent else AmoledBlack)
                    .clickable { viewModel.stopSosBeacon() },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "S · O · S",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::stopSosBeacon,
                        colors = ButtonDefaults.buttonColors(containerColor = AmoledBlack),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Tap to Stop Beacon", color = Color.White)
                    }
                }
            }
        }
    }
}
