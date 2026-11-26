package com.aegis.survival.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegis.survival.AegisUiState
import com.aegis.survival.data.IndexedArticle
import com.aegis.survival.ui.theme.LocalAegisPalette

@Composable
fun LibraryScreen(
    state: AegisUiState,
    onSearchChange: (String) -> Unit,
    onToggleSavedOnly: () -> Unit,
    onSelectTier: (Int?) -> Unit,
    onSelectPriority: (String?) -> Unit,
    onSelectDomain: (String?) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onOpenArticle: (IndexedArticle) -> Unit,
    onAskFallback: (String) -> Unit,
) {
    val palette = LocalAegisPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas),
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = palette.textSecondary,
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search",
                            tint = palette.textSecondary,
                        )
                    }
                }
            },
            placeholder = {
                Text(
                    text = "Search ${state.allArticlesCount} offline protocols...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textMuted,
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surfaceElevated,
                unfocusedContainerColor = palette.surface,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.border,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                cursorColor = palette.accentSoft,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )

        // Saved + Tier + Priority filters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                FilterToggleButton(
                    label = "Saved (${state.settings.bookmarkedArticleIds.size})",
                    selected = state.showSavedOnly,
                    onClick = onToggleSavedOnly,
                )
            }
            item {
                FilterToggleButton(
                    label = "All Tiers",
                    selected = state.selectedTierFilter == null && !state.showSavedOnly,
                    onClick = { onSelectTier(null) },
                )
            }
            listOf(1 to "Tier 1 · Survive", 2 to "Tier 2 · Sustain", 3 to "Tier 3 · Rebuild").forEach { (tier, label) ->
                item {
                    FilterToggleButton(
                        label = label,
                        selected = state.selectedTierFilter == tier,
                        onClick = { onSelectTier(tier) },
                    )
                }
            }
            listOf("critical" to "Critical", "high" to "High").forEach { (prio, label) ->
                item {
                    FilterToggleButton(
                        label = label,
                        selected = state.selectedPriorityFilter == prio,
                        onClick = { onSelectPriority(prio) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Domain filters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                FilterToggleButton(
                    label = "All Domains (${state.domains.size})",
                    selected = state.selectedDomainFilter == null,
                    onClick = { onSelectDomain(null) },
                )
            }
            items(state.domains, key = { it.id }) { domain ->
                FilterToggleButton(
                    label = domain.label,
                    selected = state.selectedDomainFilter == domain.id,
                    onClick = { onSelectDomain(domain.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${state.filteredArticles.size} of ${state.allArticlesCount} offline protocols",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        HorizontalDivider(color = palette.border, thickness = 1.dp)

        if (state.filteredArticles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (state.showSavedOnly) "No saved protocols yet" else "No matching offline protocols",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.showSavedOnly) {
                        "Tap the bookmark icon on any protocol to pin it for instant emergency access."
                    } else {
                        "Clear filters or ask the AEGIS assistant directly."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
                if (state.searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onAskFallback(state.searchQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(text = "Ask AEGIS Assistant", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.canvas),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(state.filteredArticles, key = { "${it.domainId}_${it.article.id}" }) { item ->
                    val isSaved = item.article.id in state.settings.bookmarkedArticleIds
                    ArticleListRow(
                        item = item,
                        isBookmarked = isSaved,
                        onToggleBookmark = { onToggleBookmark(item.article.id) },
                        onClick = { onOpenArticle(item) },
                    )
                    HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun FilterToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalAegisPalette.current
    Surface(
        color = if (selected) palette.surfaceTinted else palette.surface,
        border = BorderStroke(1.dp, if (selected) palette.accent else palette.border),
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.accentSoft else palette.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ArticleListRow(
    item: IndexedArticle,
    isBookmarked: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val palette = LocalAegisPalette.current
    val priorityColor = when (item.article.priority.lowercase()) {
        "critical" -> palette.danger
        "high" -> palette.warn
        else -> palette.ok
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.canvas)
            .clickable { onClick() }
            .heightIn(min = 68.dp)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.article.title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.article.priority.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = priorityColor,
                )
                Text(
                    text = " · ${item.article.time} · ${item.domainLabel} · Tier ${item.tier}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onToggleBookmark != null) {
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Save protocol",
                    tint = if (isBookmarked) palette.accentSoft else palette.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "Read protocol",
            tint = palette.textMuted,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(20.dp),
        )
    }
}
