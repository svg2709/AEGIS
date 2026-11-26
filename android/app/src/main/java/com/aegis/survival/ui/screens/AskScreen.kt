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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegis.survival.AegisUiState
import com.aegis.survival.data.ChatMessage
import com.aegis.survival.data.ChatRole
import com.aegis.survival.data.IndexedArticle
import com.aegis.survival.data.OnlineChatMode
import com.aegis.survival.ui.components.ProtocolBody
import com.aegis.survival.ui.theme.LocalAegisPalette

private val QUICK_PROMPTS = listOf(
    "Stop severe bleeding",
    "Purify water without fire",
    "Emergency shelter hypothermia",
    "Bow drill friction fire",
    "Treat wound infection",
    "Emergency childbirth steps",
)

@Composable
fun AskScreen(
    state: AegisUiState,
    onSendQuery: (String) -> Unit,
    onToggleEmergencyMode: () -> Unit,
    onSelectOnlineChatMode: (OnlineChatMode) -> Unit,
    onOpenArticle: (IndexedArticle) -> Unit,
    onCopyMessage: (String) -> Unit,
) {
    val palette = LocalAegisPalette.current
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.chatMessages.size, state.isThinking) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas),
    ) {
        // Subtle system battery saver notice when auto-detected
        if (state.isBatterySaverActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.canvas)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.BatterySaver,
                    contentDescription = "Battery saver active",
                    tint = palette.ok,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.batteryStatus.isSystemPowerSave) {
                        "Phone Battery Saver detected · Ultra AMOLED #000000 active (${state.batteryStatus.batteryPercent}%)"
                    } else {
                        "Battery Saver Mode · Ultra AMOLED #000000 active (${state.batteryStatus.batteryPercent}%)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.ok,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
        }

        // Quick survival prompt row + emergency mode toggle
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                Surface(
                    color = if (state.settings.emergencyMode) palette.surfaceTinted else palette.surface,
                    border = BorderStroke(
                        1.dp,
                        if (state.settings.emergencyMode) palette.accent else palette.border,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable { onToggleEmergencyMode() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Emergency mode",
                            tint = if (state.settings.emergencyMode) palette.accent else palette.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.settings.emergencyMode) "Emergency: ON" else "Emergency Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.settings.emergencyMode) palette.accentSoft else palette.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (state.settings.onlineProxyEnabled && state.settings.proxyBaseUrl.isNotBlank()) {
                items(OnlineChatMode.entries) { mode ->
                    val selected = state.onlineChatMode == mode
                    Surface(
                        color = if (selected) palette.surfaceElevated else palette.surface,
                        border = BorderStroke(1.dp, if (selected) palette.accentSoft else palette.border),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clickable { onSelectOnlineChatMode(mode) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) palette.accentSoft else palette.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            items(QUICK_PROMPTS) { prompt ->
                Surface(
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable(enabled = !state.isThinking) { onSendQuery(prompt) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)

        // Message stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(palette.canvas),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(state.chatMessages, key = { it.id }) { msg ->
                ChatMessageBlock(
                    message = msg,
                    onOpenArticle = onOpenArticle,
                    onCopy = { onCopyMessage(msg.text) },
                )
            }

            if (state.isThinking) {
                item(key = "thinking") {
                    Text(
                        text = "Searching verified offline protocols...",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentSoft,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = palette.border, thickness = 1.dp)

        // Thumb-zone input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.canvas)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = {
                    Text(
                        text = if (state.settings.emergencyMode) {
                            "Emergency query (bleeding, airway, burn)..."
                        } else {
                            "Ask survival, trauma, water, energy..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (draft.isNotBlank() && !state.isThinking) {
                            val q = draft
                            draft = ""
                            onSendQuery(q)
                        }
                    }
                ),
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
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    if (draft.isNotBlank() && !state.isThinking) {
                        val q = draft
                        draft = ""
                        onSendQuery(q)
                    }
                },
                enabled = draft.isNotBlank() && !state.isThinking,
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = if (draft.isNotBlank() && !state.isThinking) palette.accent else palette.surface,
                        shape = RoundedCornerShape(16.dp),
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send query",
                    tint = if (draft.isNotBlank() && !state.isThinking) Color.White else palette.textMuted,
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBlock(
    message: ChatMessage,
    onOpenArticle: (IndexedArticle) -> Unit,
    onCopy: () -> Unit,
) {
    val palette = LocalAegisPalette.current
    val roleLabel = when (message.role) {
        ChatRole.USER -> "YOU"
        ChatRole.ASSISTANT -> "AEGIS"
        ChatRole.EMERGENCY -> "AEGIS EMERGENCY"
        ChatRole.SYSTEM -> "SYSTEM"
    }
    val roleColor = when (message.role) {
        ChatRole.USER -> palette.ok
        ChatRole.ASSISTANT -> palette.accentSoft
        ChatRole.EMERGENCY -> palette.danger
        ChatRole.SYSTEM -> palette.textSecondary
    }
    val containerColor = when (message.role) {
        ChatRole.USER -> palette.surfaceElevated
        ChatRole.EMERGENCY -> palette.surfaceTinted
        else -> palette.surface
    }
    val containerBorder = when (message.role) {
        ChatRole.EMERGENCY -> BorderStroke(1.dp, palette.danger.copy(alpha = 0.6f))
        ChatRole.USER -> BorderStroke(1.dp, palette.border)
        else -> BorderStroke(1.dp, palette.borderSubtle)
    }

    Surface(
        color = containerColor,
        border = containerBorder,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val metaParts = buildList {
                    add(roleLabel)
                    if (message.sourceLabel.isNotBlank()) add(message.sourceLabel)
                    if (message.confidencePercent != null) add("${message.confidencePercent}% match")
                    if (message.timestampLabel.isNotBlank()) add(message.timestampLabel)
                }

                Text(
                    text = metaParts.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = roleColor,
                    modifier = Modifier.weight(1f),
                )

                if (message.role == ChatRole.ASSISTANT || message.role == ChatRole.EMERGENCY) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy response",
                            tint = palette.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (message.role == ChatRole.USER || message.role == ChatRole.SYSTEM) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.role == ChatRole.USER) palette.textPrimary else palette.textSecondary,
                )
            } else {
                ProtocolBody(text = message.text)
            }

            if (message.groundingSources.isNotEmpty()) {
                val uriHandler = LocalUriHandler.current
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VERIFIED SOURCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                )
                Spacer(modifier = Modifier.height(6.dp))
                message.groundingSources.forEach { src ->
                    if (src.uri.startsWith("https://", ignoreCase = true) || src.uri.startsWith("http://", ignoreCase = true)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable {
                                    runCatching { uriHandler.openUri(src.uri) }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = src.title.ifBlank { src.uri },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (src.type.startsWith("maps")) palette.ok else palette.accentSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = "Open verified source",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            if (message.citedArticles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "OPEN COMPLETE FIELD MANUAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                )
                Spacer(modifier = Modifier.height(6.dp))
                message.citedArticles.forEach { cited ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { onOpenArticle(cited) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cited.article.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.accentSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${cited.domainLabel} · ${cited.article.priority.uppercase()} · ${cited.article.time}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Open article",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
