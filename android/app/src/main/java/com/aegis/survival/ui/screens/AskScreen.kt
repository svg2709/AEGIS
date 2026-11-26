package com.aegis.survival.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
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
import androidx.compose.ui.text.font.FontWeight
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

private val STARTER_PROMPTS = listOf(
    "Bleeding" to "Stop severe bleeding",
    "Water" to "Purify water without fire",
    "Shelter" to "Emergency shelter hypothermia",
    "Fire" to "Bow drill friction fire",
)

@Composable
fun AskScreen(
    state: AegisUiState,
    onSendQuery: (String) -> Unit,
    onToggleEmergencyMode: () -> Unit,
    onSelectOnlineChatMode: (OnlineChatMode) -> Unit,
    onOpenArticle: (IndexedArticle) -> Unit,
    onCopyMessage: (String) -> Unit,
    onResetChat: () -> Unit,
) {
    val palette = LocalAegisPalette.current
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val visibleMessages = state.chatMessages.filter { it.id != "welcome" }
    val hasConversation = visibleMessages.any { it.role == ChatRole.USER }

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
        // Optional subtle online AI mode strip (only visible when user enabled HTTPS proxy)
        if (state.settings.onlineProxyEnabled && state.settings.proxyBaseUrl.isNotBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(OnlineChatMode.entries) { mode ->
                    val selected = state.onlineChatMode == mode
                    Surface(
                        color = if (selected) palette.surfaceElevated else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (selected) palette.accentSoft else palette.borderSubtle,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clickable { onSelectOnlineChatMode(mode) },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) palette.accentSoft else palette.textMuted,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
        }

        // Main conversation or calm minimal empty state
        if (!hasConversation) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Offline Survival Intelligence",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${state.allArticlesCount} verified protocols ready on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textMuted,
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Clean 2x2 starter grid (disappears once user starts chatting)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    STARTER_PROMPTS.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowItems.forEach { (tag, prompt) ->
                                Surface(
                                    color = palette.surface,
                                    border = BorderStroke(1.dp, palette.borderSubtle),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 68.dp)
                                        .clickable(enabled = !state.isThinking) { onSendQuery(prompt) },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = tag.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.accentSoft,
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = prompt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Minimal single-line active context bar (shows tracked scenario + 1-tap New Chat reset)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.canvas)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.activeContextTopic?.let { "Context · $it" } ?: "Context · Active Session",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "New Chat",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accentSoft,
                    modifier = Modifier
                        .clickable(enabled = !state.isThinking) { onResetChat() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)

            val lastAssistantId = visibleMessages.lastOrNull {
                it.role == ChatRole.ASSISTANT || it.role == ChatRole.EMERGENCY
            }?.id

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(palette.canvas),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(
                    visibleMessages,
                    key = { it.id },
                ) { msg ->
                    ChatMessageBlock(
                        message = msg,
                        showFollowUps = !state.isThinking && msg.id == lastAssistantId && msg.followUps.isNotEmpty(),
                        onFollowUpClick = onSendQuery,
                        onOpenArticle = onOpenArticle,
                        onCopy = { onCopyMessage(msg.text) },
                    )
                }

                if (state.isThinking) {
                    item(key = "thinking") {
                        Text(
                            text = "Thinking with context...",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accentSoft,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)

        // Clean bottom composer (glides smoothly above the keyboard via MainActivity imePadding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.canvas)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Compact Emergency Mode toggle right inside the composer bar
            IconButton(
                onClick = onToggleEmergencyMode,
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (state.settings.emergencyMode) palette.surfaceTinted else palette.surface,
                        shape = RoundedCornerShape(14.dp),
                    ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = if (state.settings.emergencyMode) "Emergency mode active" else "Enable emergency mode",
                    tint = if (state.settings.emergencyMode) palette.danger else palette.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = {
                    Text(
                        text = if (state.settings.emergencyMode) {
                            "Emergency mode active — ask..."
                        } else {
                            "Ask AEGIS..."
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
                    focusedBorderColor = if (state.settings.emergencyMode) palette.danger else palette.accent,
                    unfocusedBorderColor = palette.borderSubtle,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    cursorColor = palette.accentSoft,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(8.dp))

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
                    .size(46.dp)
                    .background(
                        color = if (draft.isNotBlank() && !state.isThinking) palette.accent else palette.surface,
                        shape = RoundedCornerShape(14.dp),
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    tint = if (draft.isNotBlank() && !state.isThinking) Color.White else palette.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBlock(
    message: ChatMessage,
    showFollowUps: Boolean,
    onFollowUpClick: (String) -> Unit,
    onOpenArticle: (IndexedArticle) -> Unit,
    onCopy: () -> Unit,
) {
    val palette = LocalAegisPalette.current

    if (message.role == ChatRole.USER) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Surface(
                color = palette.surfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        return
    }

    if (message.role == ChatRole.SYSTEM) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        return
    }

    // Clean, borderless AMOLED response block for maximum legibility
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (message.role == ChatRole.EMERGENCY) "AEGIS EMERGENCY" else "AEGIS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (message.role == ChatRole.EMERGENCY) palette.danger else palette.accentSoft,
            )
            if (message.sourceLabel.isNotBlank()) {
                Text(
                    text = " · ${message.sourceLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
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

        Spacer(modifier = Modifier.height(6.dp))
        ProtocolBody(text = message.text)

        if (message.groundingSources.isNotEmpty()) {
            val uriHandler = LocalUriHandler.current
            Spacer(modifier = Modifier.height(12.dp))
            message.groundingSources.forEach { src ->
                if (src.uri.startsWith("https://", ignoreCase = true) || src.uri.startsWith("http://", ignoreCase = true)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clickable { runCatching { uriHandler.openUri(src.uri) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = src.title.ifBlank { src.uri },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (src.type.startsWith("maps")) palette.ok else palette.info,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = "Open source",
                            tint = palette.textMuted,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }

        if (message.citedArticles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            message.citedArticles.take(2).forEach { cited ->
                Surface(
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.borderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOpenArticle(cited) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cited.article.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = cited.domainLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Open guide",
                            tint = palette.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        if (showFollowUps) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(message.followUps) { suggestion ->
                    Surface(
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, palette.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clickable { onFollowUpClick(suggestion) },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
