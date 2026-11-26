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
import com.aegis.survival.ui.theme.AmoledBlack
import com.aegis.survival.ui.theme.CoralPrimary
import com.aegis.survival.ui.theme.CrimsonAccent
import com.aegis.survival.ui.theme.HairlineBorder
import com.aegis.survival.ui.theme.StatusDanger
import com.aegis.survival.ui.theme.StatusOk
import com.aegis.survival.ui.theme.StatusWarn
import com.aegis.survival.ui.theme.SurfaceElevated
import com.aegis.survival.ui.theme.SurfaceInteractive
import com.aegis.survival.ui.theme.TextMuted
import com.aegis.survival.ui.theme.TextPrimary
import com.aegis.survival.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    state: AegisUiState,
    onSearchChange: (String) -> Unit,
    onSelectTier: (Int?) -> Unit,
    onSelectPriority: (String?) -> Unit,
    onSelectDomain: (String?) -> Unit,
    onOpenArticle: (IndexedArticle) -> Unit,
    onAskFallback: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack),
    ) {
        // Search box
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search",
                            tint = TextSecondary,
                        )
                    }
                }
            },
            placeholder = {
                Text(
                    text = "Search ${state.allArticlesCount} offline protocols...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceInteractive,
                unfocusedContainerColor = SurfaceElevated,
                focusedBorderColor = CrimsonAccent,
                unfocusedBorderColor = HairlineBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = CoralPrimary,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )

        // Tier & Priority filter bar (functional segmented buttons)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                FilterToggleButton(
                    label = "All Tiers",
                    selected = state.selectedTierFilter == null,
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

        // Domain filter bar
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

        // Unboxed result summary line
        Text(
            text = "${state.filteredArticles.size} of ${state.allArticlesCount} offline protocols",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        HorizontalDivider(color = HairlineBorder, thickness = 1.dp)

        if (state.filteredArticles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No matching offline protocols",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Clear filters or ask the AEGIS assistant directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                if (state.searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onAskFallback(state.searchQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
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
                    .background(AmoledBlack),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(state.filteredArticles, key = { "${it.domainId}_${it.article.id}" }) { item ->
                    ArticleListRow(
                        item = item,
                        onClick = { onOpenArticle(item) },
                    )
                    HorizontalDivider(color = Color(0xFF151520), thickness = 1.dp)
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
    Surface(
        color = if (selected) CrimsonAccent.copy(alpha = 0.18f) else SurfaceElevated,
        border = BorderStroke(1.dp, if (selected) CrimsonAccent else HairlineBorder),
        shape = RoundedCornerShape(10.dp),
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
                color = if (selected) CoralPrimary else TextSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ArticleListRow(
    item: IndexedArticle,
    onClick: () -> Unit,
) {
    val priorityColor = when (item.article.priority.lowercase()) {
        "critical" -> StatusDanger
        "high" -> StatusWarn
        else -> StatusOk
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmoledBlack)
            .clickable { onClick() }
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.article.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.article.priority.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = priorityColor,
                )
                Text(
                    text = " · ${item.article.time} · ${item.domainLabel} · Tier ${item.tier}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "Read protocol",
            tint = TextMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}
