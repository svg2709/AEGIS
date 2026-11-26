package com.aegis.survival

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.survival.data.AppSettings
import com.aegis.survival.data.ChatMessage
import com.aegis.survival.data.ChatRole
import com.aegis.survival.data.IndexedArticle
import com.aegis.survival.data.KnowledgeDomain
import com.aegis.survival.data.KnowledgeRepository
import com.aegis.survival.data.OnlineProxyClient
import com.aegis.survival.data.SettingsStore
import com.aegis.survival.hardware.AegisHardwareController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val activeArticle: IndexedArticle? = null,
    val searchQuery: String = "",
    val selectedTierFilter: Int? = null,
    val selectedDomainFilter: String? = null,
    val selectedPriorityFilter: String? = null,
    val chatDomainFocus: String = "all",
    val chatMessages: List<ChatMessage> = emptyList(),
    val isThinking: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val isOnline: Boolean = false,
    val hasFlashHardware: Boolean = false,
    val sosActive: Boolean = false,
    val beaconLit: Boolean = false,
    val sosScreenBeaconVisible: Boolean = false,
    val statusNotice: String? = null,
)

class AegisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KnowledgeRepository(application)
    private val proxyClient = OnlineProxyClient()
    private val settingsStore = SettingsStore(application)
    val hardware = AegisHardwareController(application)

    private val _uiState = MutableStateFlow(
        AegisUiState(
            settings = settingsStore.load(),
            isOnline = hardware.isOnline(),
            hasFlashHardware = hardware.hasFlashHardware(),
        )
    )
    val uiState: StateFlow<AegisUiState> = _uiState.asStateFlow()

    init {
        loadKnowledgeBase()
        observeHardware()
    }

    private fun loadKnowledgeBase() {
        viewModelScope.launch {
            val dataset = repository.load()
            val allArticles = repository.getAllArticles()
            val emergency = repository.getEmergencyArticles()

            _uiState.update { state ->
                val welcome = ChatMessage(
                    id = "welcome",
                    role = ChatRole.SYSTEM,
                    text = "Offline survival intelligence ready. ${dataset.articleCount} verified protocols loaded across ${dataset.domainCount} domains. Ask any survival, medical, water, energy, or recovery question below.",
                    sourceLabel = "OFFLINE KB",
                    timestampLabel = currentTime(),
                )
                state.copy(
                    isLoadingKb = false,
                    domains = dataset.domains,
                    allArticlesCount = allArticles.size,
                    filteredArticles = repository.filterArticles(""),
                    emergencyArticles = emergency,
                    chatMessages = if (state.chatMessages.isEmpty()) listOf(welcome) else state.chatMessages,
                    isOnline = hardware.isOnline(),
                )
            }
        }
    }

    private fun observeHardware() {
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
            hardware.usingScreenFallback.collect { fallback ->
                _uiState.update { state ->
                    state.copy(sosScreenBeaconVisible = state.sosActive && fallback)
                }
            }
        }
    }

    fun selectTab(tab: AegisTab) {
        hardware.vibrate(18L, _uiState.value.settings.hapticsEnabled)
        _uiState.update {
            it.copy(
                selectedTab = tab,
                activeArticle = null,
                isOnline = hardware.isOnline(),
            )
        }
    }

    fun openArticle(article: IndexedArticle) {
        hardware.vibrate(20L, _uiState.value.settings.hapticsEnabled)
        _uiState.update { it.copy(activeArticle = article) }
    }

    fun closeArticle() {
        _uiState.update { it.copy(activeArticle = null) }
    }

    fun shareArticle(article: IndexedArticle) {
        hardware.vibrate(30L, _uiState.value.settings.hapticsEnabled)
        hardware.shareProtocol(article.article.title, article.article.body)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredArticles = repository.filterArticles(
                    query = query,
                    tierFilter = state.selectedTierFilter,
                    domainIdFilter = state.selectedDomainFilter,
                    priorityFilter = state.selectedPriorityFilter,
                )
            )
        }
    }

    fun selectTierFilter(tier: Int?) {
        _uiState.update { state ->
            val nextTier = if (state.selectedTierFilter == tier) null else tier
            state.copy(
                selectedTierFilter = nextTier,
                filteredArticles = repository.filterArticles(
                    query = state.searchQuery,
                    tierFilter = nextTier,
                    domainIdFilter = state.selectedDomainFilter,
                    priorityFilter = state.selectedPriorityFilter,
                )
            )
        }
    }

    fun selectDomainFilter(domainId: String?) {
        _uiState.update { state ->
            val nextDomain = if (state.selectedDomainFilter == domainId) null else domainId
            state.copy(
                selectedDomainFilter = nextDomain,
                filteredArticles = repository.filterArticles(
                    query = state.searchQuery,
                    tierFilter = state.selectedTierFilter,
                    domainIdFilter = nextDomain,
                    priorityFilter = state.selectedPriorityFilter,
                )
            )
        }
    }

    fun selectPriorityFilter(priority: String?) {
        _uiState.update { state ->
            val nextPriority = if (state.selectedPriorityFilter == priority) null else priority
            state.copy(
                selectedPriorityFilter = nextPriority,
                filteredArticles = repository.filterArticles(
                    query = state.searchQuery,
                    tierFilter = state.selectedTierFilter,
                    domainIdFilter = state.selectedDomainFilter,
                    priorityFilter = nextPriority,
                )
            )
        }
    }

    fun setChatDomainFocus(domainId: String) {
        _uiState.update { it.copy(chatDomainFocus = domainId) }
    }

    fun toggleEmergencyMode() {
        val current = _uiState.value.settings
        val updated = current.copy(emergencyMode = !current.emergencyMode)
        settingsStore.save(updated)
        hardware.vibrate(if (updated.emergencyMode) 90L else 30L, updated.hapticsEnabled)
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
        hardware.vibrate(30L, _uiState.value.settings.hapticsEnabled)
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

        hardware.vibrate(20L, stateSnapshot.settings.hapticsEnabled)
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
        hardware.stopSosBeacon()
    }

    private fun currentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
