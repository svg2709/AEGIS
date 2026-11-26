package com.aegis.survival.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.min

class KnowledgeRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var dataset: KnowledgeDataset = KnowledgeDataset()
    private var indexed: List<IndexedArticle> = emptyList()

    suspend fun load(): KnowledgeDataset = withContext(Dispatchers.IO) {
        if (indexed.isNotEmpty()) return@withContext dataset

        val raw = context.assets.open("aegis_kb.json").bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<KnowledgeDataset>(raw)
        val flat = mutableListOf<IndexedArticle>()

        for (domain in parsed.domains) {
            for (topic in domain.topics) {
                for (article in topic.articles) {
                    val combined = "${article.title} ${topic.label} ${domain.label} ${article.body}"
                    flat.add(
                        IndexedArticle(
                            article = article,
                            domainId = domain.id,
                            domainLabel = domain.label,
                            domainColorHex = domain.color,
                            tier = domain.tier,
                            topicId = topic.id,
                            topicLabel = topic.label,
                            searchTokens = tokenize(combined),
                            titleLower = article.title.lowercase(),
                            bodyLower = article.body.lowercase(),
                        )
                    )
                }
            }
        }

        dataset = parsed
        indexed = flat
        parsed
    }

    fun getDomains(): List<KnowledgeDomain> = dataset.domains

    fun getAllArticles(): List<IndexedArticle> = indexed

    fun getArticleById(articleId: String): IndexedArticle? =
        indexed.firstOrNull { it.article.id == articleId }

    fun getEmergencyArticles(): List<IndexedArticle> {
        return indexed
            .filter {
                it.article.priority.equals("critical", ignoreCase = true) ||
                    it.article.time.equals("immediate", ignoreCase = true)
            }
            .sortedWith(
                compareBy<IndexedArticle>(
                    { if (it.article.priority.equals("critical", ignoreCase = true)) 0 else 1 },
                    { if (it.article.time.equals("immediate", ignoreCase = true)) 0 else 1 },
                    { it.tier },
                    { it.article.title }
                )
            )
    }

    fun filterArticles(
        query: String,
        tierFilter: Int? = null,
        domainIdFilter: String? = null,
        priorityFilter: String? = null,
    ): List<IndexedArticle> {
        val base = indexed.filter { item ->
            (tierFilter == null || item.tier == tierFilter) &&
                (domainIdFilter.isNullOrBlank() || item.domainId == domainIdFilter) &&
                (priorityFilter.isNullOrBlank() || item.article.priority.equals(priorityFilter, ignoreCase = true))
        }

        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return base.sortedWith(
                compareBy<IndexedArticle>(
                    { it.tier },
                    { priorityRank(it.article.priority) },
                    { it.article.title }
                )
            )
        }

        val queryLower = trimmed.lowercase()
        val queryTokens = tokenize(trimmed)

        return base
            .mapNotNull { item ->
                val score = scoreArticle(item, queryLower, queryTokens, emergencyMode = false)
                if (score > 0.0) item to score else null
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun answerOffline(
        query: String,
        emergencyMode: Boolean,
        domainIdFilter: String? = null,
        recentMessages: List<ChatMessage> = emptyList(),
        activeArticleId: String? = null,
    ): OfflineAnswer {
        val trimmed = query.trim()
        val queryLower = trimmed.lowercase()
        val queryTokens = tokenize(trimmed)

        val priorUserMessages = recentMessages
            .filter { it.role == ChatRole.USER }
            .takeLast(3)
        val lastCitedArticle = activeArticleId?.let { getArticleById(it) }
            ?: recentMessages
                .lastOrNull { it.citedArticles.isNotEmpty() }
                ?.citedArticles
                ?.firstOrNull()

        val isFollowUp = lastCitedArticle != null && isLikelyFollowUp(queryLower, queryTokens)
        val contextTokens = if (recentMessages.isNotEmpty()) {
            tokenize(
                priorUserMessages.joinToString(" ") { it.text } + " " +
                    (lastCitedArticle?.article?.title.orEmpty())
            )
        } else {
            emptySet()
        }

        val candidates = if (domainIdFilter.isNullOrBlank() || domainIdFilter == "all") {
            indexed
        } else {
            indexed.filter { it.domainId == domainIdFilter }.ifEmpty { indexed }
        }

        val ranked = candidates
            .map { item ->
                var score = scoreArticle(item, queryLower, queryTokens, emergencyMode)

                // Carry over conversation context when user asks follow-ups or stays in the same scenario
                if (contextTokens.isNotEmpty()) {
                    var ctxBoost = 0.0
                    for (tok in contextTokens) {
                        if (item.titleLower.contains(tok)) ctxBoost += 1.1
                        else if (item.searchTokens.contains(tok)) ctxBoost += 0.3
                    }
                    score += ctxBoost.coerceAtMost(if (isFollowUp) 6.0 else 2.0)
                }

                if (lastCitedArticle != null && item.article.id == lastCitedArticle.article.id) {
                    if (isFollowUp) {
                        score += 6.5
                    } else if (activeArticleId == item.article.id) {
                        score += 5.0
                    }
                }

                item to score
            }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }

        if (ranked.isEmpty()) {
            return OfflineAnswer(
                text = buildString {
                    appendLine("**No Exact Offline Protocol Match**")
                    appendLine("Try describing your situation with terms like bleeding, airway, water purification, bow drill fire, shelter, hypothermia, wound infection, or solar power.")
                    appendLine()
                    appendLine("You can also browse all ${indexed.size} protocols across ${dataset.domains.size} domains in the Library tab.")
                }.trim(),
                confidencePercent = 0,
                matchedArticles = emptyList(),
                followUps = listOf(
                    "Stop severe bleeding",
                    "Purify water without fire",
                    "Build emergency shelter",
                ),
                contextTopic = lastCitedArticle?.article?.title,
            )
        }

        val top = ranked.first()
        val topArticle = top.first
        val related = ranked.take(3).map { it.first }
        val confidence = min(98, (top.second * 11.0).toInt().coerceIn(42, 98))
        val continuingSameTopic = lastCitedArticle != null &&
            topArticle.article.id == lastCitedArticle.article.id &&
            isFollowUp

        val formatted = when {
            emergencyMode -> formatEmergencySummary(topArticle)
            continuingSameTopic -> formatContextualFollowUp(topArticle, queryLower, queryTokens)
            else -> buildString {
                appendLine("**${topArticle.article.title}**")
                appendLine("${topArticle.domainLabel} · ${topArticle.topicLabel} · ${topArticle.article.priority.uppercase()} · ${topArticle.article.time}")
                appendLine()
                appendLine(topArticle.article.body)
                if (related.size > 1) {
                    appendLine()
                    appendLine("**Related Offline Protocols**")
                    related.drop(1).forEach { rel ->
                        appendLine("- ${rel.article.title} (${rel.domainLabel})")
                    }
                }
            }.trim()
        }

        return OfflineAnswer(
            text = formatted,
            confidencePercent = confidence,
            matchedArticles = related,
            followUps = buildFollowUpSuggestions(topArticle, related, queryLower),
            contextTopic = topArticle.article.title,
        )
    }

    private fun isLikelyFollowUp(queryLower: String, queryTokens: Set<String>): Boolean {
        if (FOLLOW_UP_PHRASES.any { queryLower.contains(it) }) return true
        if (queryTokens.size <= 3 && FOLLOW_UP_PRONOUNS.any { Regex("\\b$it\\b").containsMatchIn(queryLower) }) {
            return true
        }
        return false
    }

    private fun formatContextualFollowUp(
        item: IndexedArticle,
        queryLower: String,
        queryTokens: Set<String>,
    ): String {
        val lines = item.article.body.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val intentKeywords = buildSet {
            addAll(queryTokens)
            if (queryLower.contains("without") || queryLower.contains("no ") || queryLower.contains("alternative") || queryLower.contains("instead")) {
                addAll(listOf("alternative", "improvised", "bleach", "solar", "sodis", "filter", "cloth", "chemical", "backup", "substitute"))
            }
            if (queryLower.contains("long") || queryLower.contains("time") || queryLower.contains("wait") || queryLower.contains("minute") || queryLower.contains("hour")) {
                addAll(listOf("min", "minute", "hour", "second", "wait", "boil", "duration", "check", "every"))
            }
            if (queryLower.contains("much") || queryLower.contains("many") || queryLower.contains("dose") || queryLower.contains("ratio") || queryLower.contains("liter")) {
                addAll(listOf("drop", "drops", "liter", "gallon", "mg", "ml", "ratio", "dose", "tsp", "tbsp", "cup"))
            }
            if (queryLower.contains("warning") || queryLower.contains("danger") || queryLower.contains("avoid") || queryLower.contains("safe") || queryLower.contains("risk")) {
                addAll(listOf("warning", "danger", "never", "do not", "avoid", "critical", "fatal", "toxic", "infection"))
            }
            if (queryLower.contains("sign") || queryLower.contains("symptom") || queryLower.contains("check") || queryLower.contains("know")) {
                addAll(listOf("sign", "symptom", "check", "assess", "pulse", "color", "shock", "fever", "confusion"))
            }
        }

        val scoredLines = lines.mapIndexed { idx, line ->
            val lower = line.lowercase()
            var lineScore = 0
            for (kw in intentKeywords) {
                if (lower.contains(kw)) lineScore += if (kw in queryTokens) 4 else 2
            }
            if (lower.contains("warning") || lower.contains("do not")) lineScore += 1
            Triple(idx, line, lineScore)
        }

        val topIndices = scoredLines
            .filter { it.third > 0 }
            .sortedByDescending { it.third }
            .take(8)
            .map { it.first }
            .sorted()

        val focusedExcerpt = if (topIndices.isNotEmpty()) {
            val expanded = linkedSetOf<Int>()
            for (idx in topIndices) {
                // Include immediate section heading above if present
                if (idx > 0 && (lines[idx - 1].startsWith("**") || lines[idx - 1].endsWith(":"))) {
                    expanded.add(idx - 1)
                }
                expanded.add(idx)
                if (idx + 1 < lines.size && lines[idx].endsWith(":")) {
                    expanded.add(idx + 1)
                }
            }
            expanded.sorted().take(12).joinToString("\n") { lines[it] }
        } else {
            lines.take(10).joinToString("\n")
        }

        return buildString {
            appendLine("**Continuing: ${item.article.title}**")
            appendLine(focusedExcerpt)
            appendLine()
            append("Need another angle? Ask a specific constraint (gear missing, timing, warnings) or tap the guide below for the full manual.")
        }.trim()
    }

    private fun buildFollowUpSuggestions(
        top: IndexedArticle,
        related: List<IndexedArticle>,
        queryLower: String,
    ): List<String> {
        val suggestions = mutableListOf<String>()
        if (!queryLower.contains("warning") && !queryLower.contains("avoid")) {
            suggestions.add("What mistakes or warnings to avoid?")
        }
        if (!queryLower.contains("without") && !queryLower.contains("alternative")) {
            suggestions.add("What if I don't have standard gear?")
        }
        if (!queryLower.contains("long") && !queryLower.contains("dose")) {
            suggestions.add("Exact timing, dosage, or ratios?")
        }
        if (related.size > 1) {
            suggestions.add("Next step: ${related[1].article.title.take(36)}")
        }
        return suggestions.distinct().take(3)
    }

    private fun formatEmergencySummary(item: IndexedArticle): String {
        val lines = item.article.body.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val prioritized = lines.filter { line ->
            line.startsWith("1.") ||
                line.startsWith("2.") ||
                line.startsWith("3.") ||
                line.startsWith("4.") ||
                line.startsWith("5.") ||
                line.startsWith("6.") ||
                line.startsWith("**") ||
                line.contains("WARNING", ignoreCase = true) ||
                line.contains("CRITICAL", ignoreCase = true) ||
                line.contains("DO NOT", ignoreCase = true)
        }

        val bodyExcerpt = if (prioritized.size >= 3) {
            prioritized.take(14).joinToString("\n")
        } else {
            lines.take(12).joinToString("\n")
        }

        return buildString {
            appendLine("**EMERGENCY PROTOCOL: ${item.article.title}**")
            appendLine(bodyExcerpt)
            appendLine()
            append("Tap the cited protocol below to open the complete step-by-step field manual.")
        }.trim()
    }

    private fun scoreArticle(
        item: IndexedArticle,
        queryLower: String,
        queryTokens: Set<String>,
        emergencyMode: Boolean,
    ): Double {
        var score = 0.0

        if (queryLower.length >= 3 && item.titleLower.contains(queryLower)) {
            score += 9.0
        }
        if (queryLower.length >= 4 && item.bodyLower.contains(queryLower)) {
            score += 4.5
        }

        var matchedTokenCount = 0
        for (token in queryTokens) {
            var tokenMatched = false
            if (item.titleLower.contains(token)) {
                score += 3.5
                tokenMatched = true
            }
            if (item.topicLabel.lowercase().contains(token) || item.domainLabel.lowercase().contains(token)) {
                score += 2.0
                tokenMatched = true
            }
            if (item.searchTokens.contains(token)) {
                score += 1.4
                tokenMatched = true
            } else if (token.length >= 4 && item.bodyLower.contains(token)) {
                score += 0.9
                tokenMatched = true
            }
            if (tokenMatched) matchedTokenCount++
        }

        if (queryTokens.isNotEmpty() && matchedTokenCount > 0) {
            val coverage = matchedTokenCount.toDouble() / queryTokens.size.toDouble()
            score += coverage * 2.5
        }

        if (score > 0.0) {
            if (emergencyMode && item.article.priority.equals("critical", ignoreCase = true)) {
                score += 2.0
            }
            if (emergencyMode && item.article.time.equals("immediate", ignoreCase = true)) {
                score += 1.5
            }
        }

        return score
    }

    private fun priorityRank(priority: String): Int = when (priority.lowercase()) {
        "critical" -> 0
        "high" -> 1
        "medium" -> 2
        else -> 3
    }

    private fun tokenize(text: String): Set<String> {
        return text
            .lowercase()
            .split(NON_WORD_REGEX)
            .filter { it.length >= 2 && it !in STOP_WORDS }
            .toSet()
    }

    companion object {
        private val NON_WORD_REGEX = Regex("[^a-z0-9]+")
        private val STOP_WORDS = setOf(
            "the", "and", "for", "with", "that", "this", "from", "your", "have",
            "are", "how", "what", "when", "where", "into", "about", "can", "you",
            "will", "not", "use", "using", "make", "get", "need", "should", "must",
        )
        private val FOLLOW_UP_PHRASES = listOf(
            "what if", "how long", "how much", "how many", "what next", "next step",
            "without", "don't have", "dont have", "no gear", "alternative", "instead",
            "warning", "mistakes", "avoid", "dosage", "ratio", "symptoms", "signs of",
            "explain", "more detail", "step by step", "for a child", "if it", "if they",
        )
        private val FOLLOW_UP_PRONOUNS = listOf("it", "that", "this", "them", "they", "one")
    }
}
