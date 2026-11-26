package com.aegis.survival

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.survival.data.AppSettings
import com.aegis.survival.data.ChatMessage
import com.aegis.survival.data.ChatRole
import com.aegis.survival.data.DisplayThemeMode
import com.aegis.survival.data.IndexedArticle
import com.aegis.survival.data.KnowledgeDomain
import com.aegis.survival.data.KnowledgeRepository
import com.aegis.survival.data.OnlineChatMode
import com.aegis.survival.data.OnlineProxyClient
import com.aegis.survival.data.SettingsStore
import com.aegis.survival.hardware.AegisHardwareController
import com.aegis.survival.hardware.BatteryStateMonitor
import com.aegis.survival.hardware.BatteryStatus
import com.aegis.survival.update.AppUpdateInfo
import com.aegis.survival.update.UpdateCheckResult
import com.aegis.survival.update.UpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AegisTab(val label: String) {
    ASK("Ask"),
    LIBRARY("Library"),
    EMERGENCY("Emergency"),
    SYSTEM("System"),
}

data class AegisUiState(
    val isLoadingKb: Boolean = true,
    val selectedTab: AegisTab = AegisTab.ASK,
    val domains: List<KnowledgeDomain> = emptyList(),
    val allArticlesCount: Int = 0,
    val filteredArticles: List<IndexedArticle> = emptyList(),
    val emergencyArticles: List<IndexedArticle> = emptyList(),
    val bookmarkedArticles: List<IndexedArticle> = emptyList(),
    val activeArticle: IndexedArticle? = null,
    val searchQuery: String = "",
    val selectedTierFilter: Int? = null,
    val selectedDomainFilter: String? = null,
    val selectedPriorityFilter: String? = null,
    val showSavedOnly: Boolean = false,
    val chatDomainFocus: String = "all",
    val onlineChatMode: OnlineChatMode = OnlineChatMode.GENERAL,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isThinking: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val isOnline: Boolean = false,
    val hasFlashHardware: Boolean = false,
    val sosActive: Boolean = false,
    val beaconLit: Boolean = false,
    val morseStepIndex: Int = -1,
    val sosScreenBeaconVisible: Boolean = false,
    // Field utility: Water boiling countdown timer
    val boilTimerRunning: Boolean = false,
    val boilTimerRemainingSec: Int = 60,
    val boilTimerHighAltitude: Boolean = false,
    // Auto-update state (always asks user before downloading/installing)
    val isCheckingUpdate: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateDownloadProgress: Float = 0f,
    val pendingInstallerApk: File? = null,
    val needsInstallPermission: Boolean = false,
    val updateStatusMessage: String? = null,
    val statusNotice: String? = null,
) {
    /**
     * True whenever Battery Saver mode is engaged — either detected automatically
     * from Android system PowerManager or enabled manually by the user.
     */
    val isBatterySaverActive: Boolean
        get() = settings.manualBatterySaver ||
            (settings.autoBatterySaverAmoled && batteryStatus.isSystemPowerSave)

    /**
     * True AMOLED #000000 is the default and is always enforced whenever Battery Saver is active.
     */
    val effectiveUseAmoled: Boolean
        get() = isBatterySaverActive || settings.displayThemeMode == DisplayThemeMode.AMOLED

    /**
     * In Ultra Battery Saver mode, routine non-critical haptics are conserved automatically.
     */
    val effectiveRoutineHaptics: Boolean
        get() = settings.hapticsEnabled && !isBatterySaverActive
}

class AegisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KnowledgeRepository(application)
    private val proxyClient = OnlineProxyClient()
    private val settingsStore = SettingsStore(application)
    private val batteryMonitor = BatteryStateMonitor(application)
    private val updateManager = UpdateManager(application)
    val hardware = AegisHardwareController(application)

    private var boilTimerJob: Job? = null

    private val _uiState = MutableStateFlow(
        AegisUiState(
            settings = settingsStore.load(),
            batteryStatus = batteryMonitor.status.value,
            isOnline = hardware.isOnline(),
            hasFlashHardware = hardware.hasFlashHardware(),
        )
    )
    val uiState: StateFlow<AegisUiState> = _uiState.asStateFlow()

    init {
        batteryMonitor.start()
        loadKnowledgeBase()
        observeHardwareAndBattery()
        runStartupUpdateCheckIfEligible()
    }

    private fun loadKnowledgeBase() {
        viewModelScope.launch {
            val dataset = repository.load()
            val allArticles = repository.getAllArticles()
            val emergency = repository.getEmergencyArticles()

            _uiState.update { state ->
                val savedSet = state.settings.bookmarkedArticleIds
                val savedList = allArticles.filter { it.article.id in savedSet }
                val welcome = ChatMessage(
                    id = "welcome",
                    role = ChatRole.SYSTEM,
                    text = "${dataset.articleCount} offline survival protocols across ${dataset.domainCount} domains are ready on this device. Ask any question below or switch to Emergency for rapid triage tools.",
                    sourceLabel = "OFFLINE KB",
                    timestampLabel = currentTime(),
                )
                state.copy(
                    isLoadingKb = false,
                    domains = dataset.domains,
                    allArticlesCount = allArticles.size,
                    filteredArticles = repository.filterArticles(""),
                    emergencyArticles = emergency,
                    bookmarkedArticles = savedList,
                    chatMessages = if (state.chatMessages.isEmpty()) listOf(welcome) else state.chatMessages,
                    isOnline = hardware.isOnline(),
                )
            }
        }
    }

    private fun observeHardwareAndBattery() {
        viewModelScope.launch {
            batteryMonitor.status.collect { status ->
                _uiState.update { it.copy(batteryStatus = status) }
            }
        }
        viewModelScope.launch {
            hardware.sosActive.collect { active ->
                _uiState.update { it.copy(sosActive = active) }
            }
        }
        viewModelScope.launch {
            hardware.beaconLit.collect { lit ->
                _uiState.update { it.copy(beaconLit = lit) }
            }
        }
        viewModelScope.launch {
            hardware.morseStepIndex.collect { idx ->
                _uiState.update { it.copy(morseStepIndex = idx) }
            }
        }
        viewModelScope.launch {
            hardware.usingScreenFallback.collect { fallback ->
                _uiState.update { state ->
                    state.copy(sosScreenBeaconVisible = state.sosActive && fallback)
                }
            }
        }
    }

    private fun runStartupUpdateCheckIfEligible() {
        viewModelScope.launch {
            delay(1500L)
            val state = _uiState.value
            if (state.settings.autoCheckUpdates && !state.isBatterySaverActive && hardware.isOnline()) {
                checkForUpdates(userInitiated = false)
            }
        }
    }

    // ── Navigation & Reader ───────────────────────────────────────────────

    fun selectTab(tab: AegisTab) {
        hardware.vibrate(16L, _uiState.value.effectiveRoutineHaptics)
        _uiState.update {
            it.copy(
                selectedTab = tab,
                activeArticle = null,
                isOnline = hardware.isOnline(),
            )
        }
    }

    fun openArticle(article: IndexedArticle) {
        hardware.vibrate(18L, _uiState.value.effectiveRoutineHaptics)
        _uiState.update { it.copy(activeArticle = article) }
    }

    fun closeArticle() {
        _uiState.update { it.copy(activeArticle = null) }
    }

    fun shareArticle(article: IndexedArticle) {
        hardware.vibrate(28L, _uiState.value.settings.hapticsEnabled)
        hardware.shareProtocol(article.article.title, article.article.body)
    }

    fun copyMessageText(text: String) {
        hardware.vibrate(18L, _uiState.value.effectiveRoutineHaptics)
        hardware.copyToClipboard("AEGIS Protocol", text)
        _uiState.update { it.copy(statusNotice = "Copied to clipboard") }
    }

    fun toggleBookmark(articleId: String) {
        val current = _uiState.value.settings
        val nextSet = if (articleId in current.bookmarkedArticleIds) {
            current.bookmarkedArticleIds - articleId
        } else {
            current.bookmarkedArticleIds + articleId
        }
        val updated = current.copy(bookmarkedArticleIds = nextSet)
        settingsStore.save(updated)
        hardware.vibrate(22L, _uiState.value.effectiveRoutineHaptics)

        _uiState.update { state ->
            val savedList = repository.getAllArticles().filter { it.article.id in nextSet }
            val nextFiltered = computeLibraryList(
                query = state.searchQuery,
                tier = state.selectedTierFilter,
                domain = state.selectedDomainFilter,
                priority = state.selectedPriorityFilter,
                savedOnly = state.showSavedOnly,
                savedIds = nextSet,
            )
            state.copy(
                settings = updated,
                bookmarkedArticles = savedList,
                filteredArticles = nextFiltered,
            )
        }
    }

    // ── Library Filtering ─────────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredArticles = computeLibraryList(
                    query = query,
                    tier = state.selectedTierFilter,
                    domain = state.selectedDomainFilter,
                    priority = state.selectedPriorityFilter,
                    savedOnly = state.showSavedOnly,
                    savedIds = state.settings.bookmarkedArticleIds,
                )
            )
        }
    }

    fun toggleSavedOnlyFilter() {
        _uiState.update { state ->
            val next = !state.showSavedOnly
            state.copy(
                showSavedOnly = next,
                filteredArticles = computeLibraryList(
                    query = state.searchQuery,
                    tier = state.selectedTierFilter,
                    domain = state.selectedDomainFilter,
                    priority = state.selectedPriorityFilter,
                    savedOnly = next,
                    savedIds = state.settings.bookmarkedArticleIds,
                )
            )
        }
    }

    fun selectTierFilter(tier: Int?) {
        _uiState.update { state ->
            val nextTier = if (state.selectedTierFilter == tier) null else tier
            state.copy(
                selectedTierFilter = nextTier,
                filteredArticles = computeLibraryList(
                    query = state.searchQuery,
                    tier = nextTier,
                    domain = state.selectedDomainFilter,
                    priority = state.selectedPriorityFilter,
                    savedOnly = state.showSavedOnly,
                    savedIds = state.settings.bookmarkedArticleIds,
                )
            )
        }
    }

    fun selectDomainFilter(domainId: String?) {
        _uiState.update { state ->
            val nextDomain = if (state.selectedDomainFilter == domainId) null else domainId
            state.copy(
                selectedDomainFilter = nextDomain,
                filteredArticles = computeLibraryList(
                    query = state.searchQuery,
                    tier = state.selectedTierFilter,
                    domain = nextDomain,
                    priority = state.selectedPriorityFilter,
                    savedOnly = state.showSavedOnly,
                    savedIds = state.settings.bookmarkedArticleIds,
                )
            )
        }
    }

    fun selectPriorityFilter(priority: String?) {
        _uiState.update { state ->
            val nextPriority = if (state.selectedPriorityFilter == priority) null else priority
            state.copy(
                selectedPriorityFilter = nextPriority,
                filteredArticles = computeLibraryList(
                    query = state.searchQuery,
                    tier = state.selectedTierFilter,
                    domain = state.selectedDomainFilter,
                    priority = nextPriority,
                    savedOnly = state.showSavedOnly,
                    savedIds = state.settings.bookmarkedArticleIds,
                )
            )
        }
    }

    private fun computeLibraryList(
        query: String,
        tier: Int?,
        domain: String?,
        priority: String?,
        savedOnly: Boolean,
        savedIds: Set<String>,
    ): List<IndexedArticle> {
        val base = repository.filterArticles(
            query = query,
            tierFilter = tier,
            domainIdFilter = domain,
            priorityFilter = priority,
        )
        return if (savedOnly) base.filter { it.article.id in savedIds } else base
    }

    // ── Display, Battery Saver & Settings ─────────────────────────────────

    fun setDisplayThemeMode(mode: DisplayThemeMode) {
        val updated = _uiState.value.settings.copy(displayThemeMode = mode)
        settingsStore.save(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun toggleAutoBatterySaverAmoled(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(autoBatterySaverAmoled = enabled)
        settingsStore.save(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun toggleManualBatterySaver(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(manualBatterySaver = enabled)
        settingsStore.save(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun setReadingFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.88f, 1.32f)
        val updated = _uiState.value.settings.copy(readingFontScale = clamped)
        settingsStore.save(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun toggleAutoCheckUpdates(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(autoCheckUpdates = enabled)
        settingsStore.save(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun toggleEmergencyMode() {
        val current = _uiState.value.settings
        val updated = current.copy(emergencyMode = !current.emergencyMode)
        settingsStore.save(updated)
        hardware.vibrate(if (updated.emergencyMode) 80L else 28L, updated.hapticsEnabled)
        _uiState.update { it.copy(settings = updated) }
    }

    fun toggleHaptics(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(hapticsEnabled = enabled)
        settingsStore.save(updated)
        if (enabled) hardware.vibrate(25L, true)
        _uiState.update { it.copy(settings = updated) }
    }

    fun toggleOnlineProxy(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(onlineProxyEnabled = enabled)
        settingsStore.save(updated)
        _uiState.update { it.copy(settings = updated, isOnline = hardware.isOnline()) }
    }

    fun saveProxyUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isNotEmpty() && !proxyClient.isValidHttpsBaseUrl(trimmed)) {
            _uiState.update { it.copy(statusNotice = "Proxy URL must start with https://") }
            return false
        }
        val updated = _uiState.value.settings.copy(proxyBaseUrl = trimmed)
        settingsStore.save(updated)
        _uiState.update {
            it.copy(
                settings = updated,
                statusNotice = if (trimmed.isEmpty()) "Proxy URL cleared. Using offline KB." else "HTTPS proxy URL saved."
            )
        }
        return true
    }

    fun clearStatusNotice() {
        _uiState.update { it.copy(statusNotice = null) }
    }

    fun clearChatHistory() {
        hardware.vibrate(25L, _uiState.value.effectiveRoutineHaptics)
        _uiState.update { state ->
            state.copy(
                chatMessages = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.SYSTEM,
                        text = "Session cleared. Offline knowledge base (${state.allArticlesCount} protocols) remains ready.",
                        sourceLabel = "OFFLINE KB",
                        timestampLabel = currentTime(),
                    )
                )
            )
        }
    }

    // ── Auto-Update Flow (Always asks user before downloading/installing) ──

    fun checkForUpdates(userInitiated: Boolean = true) {
        if (_uiState.value.isCheckingUpdate || _uiState.value.isDownloadingUpdate) return
        if (!hardware.isOnline()) {
            if (userInitiated) {
                _uiState.update { it.copy(updateStatusMessage = "Offline — connect to internet to check GitHub Releases.") }
            }
            return
        }

        _uiState.update {
            it.copy(
                isCheckingUpdate = true,
                updateStatusMessage = if (userInitiated) "Checking GitHub Releases..." else it.updateStatusMessage,
            )
        }

        viewModelScope.launch {
            when (val result = updateManager.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = result.info,
                            showUpdateDialog = true,
                            updateStatusMessage = "Update v${result.info.latestVersion} available",
                        )
                    }
                }

                is UpdateCheckResult.UpToDate -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = null,
                            showUpdateDialog = false,
                            updateStatusMessage = if (userInitiated) {
                                "You are up to date (v${result.currentVersion})."
                            } else {
                                null
                            },
                        )
                    }
                }

                is UpdateCheckResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateStatusMessage = if (userInitiated) result.reason else null,
                        )
                    }
                }
            }
        }
    }

    fun openUpdatePrompt() {
        if (_uiState.value.availableUpdate != null) {
            _uiState.update { it.copy(showUpdateDialog = true) }
        }
    }

    fun dismissUpdateDialog() {
        if (_uiState.value.isDownloadingUpdate) return
        _uiState.update {
            it.copy(
                showUpdateDialog = false,
                needsInstallPermission = false,
            )
        }
    }

    fun confirmAndInstallUpdate() {
        val info = _uiState.value.availableUpdate ?: return
        if (_uiState.value.isDownloadingUpdate) return

        // If already downloaded and user just granted install permission, install right away
        val cachedApk = _uiState.value.pendingInstallerApk
        if (cachedApk != null && cachedApk.exists()) {
            triggerSystemInstaller(cachedApk)
            return
        }

        _uiState.update {
            it.copy(
                isDownloadingUpdate = true,
                updateDownloadProgress = 0f,
                needsInstallPermission = false,
                updateStatusMessage = "Downloading v${info.latestVersion}...",
            )
        }

        viewModelScope.launch {
            val downloadResult = updateManager.downloadAndVerifyApk(info) { progress ->
                _uiState.update { it.copy(updateDownloadProgress = progress) }
            }

            downloadResult.onSuccess { apkFile ->
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        updateDownloadProgress = 1f,
                        pendingInstallerApk = apkFile,
                    )
                }
                triggerSystemInstaller(apkFile)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        updateDownloadProgress = 0f,
                        updateStatusMessage = err.message ?: "Update download failed.",
                    )
                }
            }
        }
    }

    private fun triggerSystemInstaller(apkFile: File) {
        if (!updateManager.canRequestPackageInstalls()) {
            _uiState.update {
                it.copy(
                    needsInstallPermission = true,
                    updateStatusMessage = "Allow AEGIS to install updates in Android Settings, then tap Install Now.",
                )
            }
            return
        }

        val launchResult = updateManager.launchApkInstaller(apkFile)
        launchResult.onSuccess {
            _uiState.update {
                it.copy(
                    showUpdateDialog = false,
                    needsInstallPermission = false,
                    updateStatusMessage = "Installer opened for v${it.availableUpdate?.latestVersion ?: ""}.",
                )
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(updateStatusMessage = "Could not launch installer: ${err.message}")
            }
        }
    }

    fun openInstallUnknownAppsSettings() {
        updateManager.openInstallPermissionSettings()
    }

    // ── Emergency Tools: SOS Beacon & Water Boiling Timer ─────────────────

    fun toggleSosTorch(screenOnly: Boolean = false) {
        hardware.toggleSosBeacon(
            scope = viewModelScope,
            screenOnly = screenOnly,
            hapticsEnabled = _uiState.value.settings.hapticsEnabled,
        )
        if (screenOnly && hardware.sosActive.value) {
            _uiState.update { it.copy(sosScreenBeaconVisible = true) }
        }
    }

    fun stopSosBeacon() {
        hardware.stopSosBeacon()
        _uiState.update { it.copy(sosScreenBeaconVisible = false) }
    }

    fun setBoilAltitudeMode(highAltitude: Boolean) {
        if (_uiState.value.boilTimerRunning) return
        _uiState.update {
            it.copy(
                boilTimerHighAltitude = highAltitude,
                boilTimerRemainingSec = if (highAltitude) 180 else 60,
            )
        }
    }

    fun toggleBoilTimer() {
        if (_uiState.value.boilTimerRunning) {
            boilTimerJob?.cancel()
            boilTimerJob = null
            _uiState.update { it.copy(boilTimerRunning = false) }
            return
        }

        val startSec = _uiState.value.boilTimerRemainingSec.takeIf { it > 0 }
            ?: if (_uiState.value.boilTimerHighAltitude) 180 else 60

        _uiState.update { it.copy(boilTimerRunning = true, boilTimerRemainingSec = startSec) }
        hardware.vibrate(30L, _uiState.value.settings.hapticsEnabled)

        boilTimerJob = viewModelScope.launch {
            var remaining = startSec
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining -= 1
                _uiState.update { it.copy(boilTimerRemainingSec = remaining) }
            }
            if (remaining == 0) {
                hardware.vibrate(250L, _uiState.value.settings.hapticsEnabled)
                _uiState.update {
                    it.copy(
                        boilTimerRunning = false,
                        statusNotice = "Rolling boil complete — water is safe once cooled.",
                    )
                }
            }
        }
    }

    fun resetBoilTimer() {
        boilTimerJob?.cancel()
        boilTimerJob = null
        _uiState.update {
            it.copy(
                boilTimerRunning = false,
                boilTimerRemainingSec = if (it.boilTimerHighAltitude) 180 else 60,
            )
        }
    }

    // ── Hybrid Survival Q&A ───────────────────────────────────────────────

    fun setOnlineChatMode(mode: OnlineChatMode) {
        _uiState.update { it.copy(onlineChatMode = mode) }
    }

    fun sendQuery(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty() || _uiState.value.isThinking) return

        val stateSnapshot = _uiState.value
        val onlineNow = hardware.isOnline()
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = query,
            timestampLabel = currentTime(),
        )

        hardware.vibrate(18L, stateSnapshot.effectiveRoutineHaptics)
        _uiState.update {
            it.copy(
                selectedTab = AegisTab.ASK,
                activeArticle = null,
                isOnline = onlineNow,
                isThinking = true,
                chatMessages = it.chatMessages + userMsg,
            )
        }

        viewModelScope.launch {
            val settings = _uiState.value.settings
            val domainFocus = _uiState.value.chatDomainFocus
            val chatMode = _uiState.value.onlineChatMode
            val canTryOnline = settings.onlineProxyEnabled &&
                onlineNow &&
                proxyClient.isValidHttpsBaseUrl(settings.proxyBaseUrl)

            var replyMsg: ChatMessage? = null
            var fallbackNotice: String? = null

            if (canTryOnline) {
                val result = proxyClient.sendChat(
                    baseUrl = settings.proxyBaseUrl,
                    message = query,
                    emergency = settings.emergencyMode,
                    domain = domainFocus,
                    mode = chatMode.apiValue,
                    recentMessages = _uiState.value.chatMessages,
                )
                result.onSuccess { proxyReply ->
                    val related = repository.answerOffline(query, settings.emergencyMode, domainFocus).matchedArticles
                    replyMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = if (settings.emergencyMode) ChatRole.EMERGENCY else ChatRole.ASSISTANT,
                        text = proxyReply.reply,
                        sourceLabel = proxyReply.source.uppercase(),
                        citedArticles = related,
                        groundingSources = proxyReply.groundingSources,
                        timestampLabel = currentTime(),
                    )
                }.onFailure { err ->
                    fallbackNotice = "Online proxy unreachable (${err.message ?: "offline"}) — answered from offline KB."
                }
            }

            if (replyMsg == null) {
                val offline = repository.answerOffline(
                    query = query,
                    emergencyMode = settings.emergencyMode,
                    domainIdFilter = domainFocus,
                )
                replyMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = if (settings.emergencyMode) ChatRole.EMERGENCY else ChatRole.ASSISTANT,
                    text = offline.text,
                    sourceLabel = "OFFLINE KB",
                    confidencePercent = offline.confidencePercent.takeIf { it > 0 },
                    citedArticles = offline.matchedArticles,
                    timestampLabel = currentTime(),
                )
            }

            _uiState.update { state ->
                val nextMessages = buildList {
                    addAll(state.chatMessages)
                    if (fallbackNotice != null) {
                        add(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                role = ChatRole.SYSTEM,
                                text = fallbackNotice,
                                sourceLabel = "FALLBACK",
                                timestampLabel = currentTime(),
                            )
                        )
                    }
                    add(replyMsg!!)
                }
                state.copy(
                    isThinking = false,
                    chatMessages = nextMessages,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        boilTimerJob?.cancel()
        batteryMonitor.stop()
        hardware.stopSosBeacon()
    }

    private fun currentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
