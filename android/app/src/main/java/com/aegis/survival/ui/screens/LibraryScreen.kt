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
    val isAllSelected = !state.showSavedOnly &&
        state.selectedPriorityFilter == null &&
        state.selectedDomainFilter == null &&
        state.selectedTierFilter == null

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
                    tint = palette.textMuted,
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
                    text = "Search ${state.allArticlesCount} guides...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textMuted,
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surfaceElevated,
                unfocusedContainerColor = palette.surface,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.borderSubtle,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                cursorColor = palette.accentSoft,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )

        // Single clean horizontal filter row (replaces two stacked chip rows)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                FilterToggleButton(
                    label = "All",
                    selected = isAllSelected,
                    onClick = {
                        if (state.showSavedOnly) onToggleSavedOnly()
                        onSelectTier(null)
                        onSelectPriority(null)
                        onSelectDomain(null)
                    },
                )
            }
            item {
                FilterToggleButton(
                    label = if (state.settings.bookmarkedArticleIds.isNotEmpty()) {
                        "Saved (${state.settings.bookmarkedArticleIds.size})"
                    } else {
                        "Saved"
                    },
                    selected = state.showSavedOnly,
                    onClick = onToggleSavedOnly,
                )
            }
            item {
                FilterToggleButton(
                    label = "Critical",
                    selected = state.selectedPriorityFilter == "critical",
                    onClick = { onSelectPriority("critical") },
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
        HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)

        if (state.filteredArticles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (state.showSavedOnly) "No saved guides yet" else "No matching guides",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (state.showSavedOnly) {
                        "Tap the bookmark icon on any guide to pin it here."
                    } else {
                        "Try another keyword or ask AEGIS directly."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textMuted,
                )
                if (state.searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onAskFallback(state.searchQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(text = "Ask AEGIS", color = Color.White)
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
        color = if (selected) palette.surfaceTinted else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) palette.accent else palette.borderSubtle),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
        else -> palette.textMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.canvas)
            .clickable { onClick() }
            .heightIn(min = 68.dp)
            .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
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
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.article.priority.equals("critical", ignoreCase = true) ||
                    item.article.priority.equals("high", ignoreCase = true)
                ) {
                    Text(
                        text = "${item.article.priority.uppercase()} · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                    )
                }
                Text(
                    text = item.domainLabel,
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
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Save guide",
                    tint = if (isBookmarked) palette.accentSoft else palette.textMuted,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "Read guide",
            tint = palette.textMuted,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(18.dp),
        )
    }
}
