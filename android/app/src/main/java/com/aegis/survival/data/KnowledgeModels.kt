package com.aegis.survival.data

import kotlinx.serialization.Serializable

@Serializable
data class KnowledgeDataset(
    val version: String = "1.0.0",
    val domainCount: Int = 0,
    val topicCount: Int = 0,
    val articleCount: Int = 0,
    val domains: List<KnowledgeDomain> = emptyList(),
)

@Serializable
data class KnowledgeDomain(
    val id: String,
    val label: String,
    val icon: String = "◈",
    val color: String = "#FF4757",
    val tier: Int = 1,
    val description: String = "",
    val topics: List<KnowledgeTopic> = emptyList(),
)

@Serializable
data class KnowledgeTopic(
    val id: String,
    val label: String,
    val icon: String = "•",
    val articles: List<SurvivalArticle> = emptyList(),
)

@Serializable
data class SurvivalArticle(
    val id: String,
    val title: String,
    val priority: String = "medium",
    val time: String = "ongoing",
    val body: String = "",
)

data class IndexedArticle(
    val article: SurvivalArticle,
    val domainId: String,
    val domainLabel: String,
    val domainColorHex: String,
    val tier: Int,
    val topicId: String,
    val topicLabel: String,
    val searchTokens: Set<String>,
    val titleLower: String,
    val bodyLower: String,
)

enum class ChatRole {
    USER,
    ASSISTANT,
    EMERGENCY,
    SYSTEM,
}

@Serializable
data class GroundingSource(
    val type: String = "web",
    val title: String = "",
    val uri: String = "",
)

enum class OnlineChatMode(val apiValue: String, val label: String) {
    GENERAL("general", "Gemini"),
    FAST("fast", "Fast"),
    SEARCH("search", "Web Search"),
    MAPS("maps", "Maps AI"),
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val sourceLabel: String = "",
    val confidencePercent: Int? = null,
    val citedArticles: List<IndexedArticle> = emptyList(),
    val groundingSources: List<GroundingSource> = emptyList(),
    val timestampLabel: String = "",
)

data class OfflineAnswer(
    val text: String,
    val confidencePercent: Int,
    val matchedArticles: List<IndexedArticle>,
)

enum class DisplayThemeMode(val label: String) {
    AMOLED("AMOLED Pitch Black"),
    OBSIDIAN("Obsidian Dark"),
}

data class AppSettings(
    val emergencyMode: Boolean = false,
    val onlineProxyEnabled: Boolean = false,
    val proxyBaseUrl: String = "",
    val hapticsEnabled: Boolean = true,
    val displayThemeMode: DisplayThemeMode = DisplayThemeMode.AMOLED,
    val autoBatterySaverAmoled: Boolean = true,
    val manualBatterySaver: Boolean = false,
    val autoCheckUpdates: Boolean = true,
    val readingFontScale: Float = 1.0f,
    val bookmarkedArticleIds: Set<String> = emptySet(),
)
