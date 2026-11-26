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
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegis.survival.AegisUiState
import com.aegis.survival.data.ChatMessage
import com.aegis.survival.data.ChatRole
import com.aegis.survival.data.IndexedArticle
import com.aegis.survival.ui.components.ProtocolBody
import com.aegis.survival.ui.theme.AmoledBlack
import com.aegis.survival.ui.theme.CoralPrimary
import com.aegis.survival.ui.theme.CrimsonAccent
import com.aegis.survival.ui.theme.HairlineBorder
import com.aegis.survival.ui.theme.StatusDanger
import com.aegis.survival.ui.theme.StatusOk
import com.aegis.survival.ui.theme.SurfaceElevated
import com.aegis.survival.ui.theme.SurfaceInteractive
import com.aegis.survival.ui.theme.TextMuted
import com.aegis.survival.ui.theme.TextPrimary
import com.aegis.survival.ui.theme.TextSecondary

private val QUICK_PROMPTS = listOf(
    "Stop severe bleeding",
    "Purify water without fire",
    "Emergency shelter hypothermia",
    "Bow drill friction fire",
    "Treat wound infection",
)

@Composable
fun AskScreen(
    state: AegisUiState,
    onSendQuery: (String) -> Unit,
    onToggleEmergencyMode: () -> Unit,
    onOpenArticle: (IndexedArticle) -> Unit,
) {
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
            .background(AmoledBlack),
    ) {
        // Quick survival prompt row + emergency mode toggle
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                Surface(
                    color = if (state.settings.emergencyMode) CrimsonAccent.copy(alpha = 0.2f) else SurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (state.settings.emergencyMode) CrimsonAccent else HairlineBorder,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable { onToggleEmergencyMode() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Emergency mode",
                            tint = if (state.settings.emergencyMode) CrimsonAccent else TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.settings.emergencyMode) "Emergency: ON" else "Emergency Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.settings.emergencyMode) CoralPrimary else TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }

            items(QUICK_PROMPTS) { prompt ->
                Surface(
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, HairlineBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable(enabled = !state.isThinking) { onSendQuery(prompt) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = HairlineBorder, thickness = 1.dp)

        // Message stream on true AMOLED black
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AmoledBlack),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.chatMessages, key = { it.id }) { msg ->
                ChatMessageBlock(
                    message = msg,
                    onOpenArticle = onOpenArticle,
                )
            }

            if (state.isThinking) {
                item(key = "thinking") {
                    Text(
                        text = "Searching survival protocols...",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoralPrimary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = HairlineBorder, thickness = 1.dp)

        // Thumb-zone input bar on pure black
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmoledBlack)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = {
                    Text(
                        text = if (state.settings.emergencyMode) {
                            "Describe emergency (bleeding, airway, burn)..."
                        } else {
                            "Ask survival, medical, water, energy..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
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
                    focusedContainerColor = SurfaceInteractive,
                    unfocusedContainerColor = SurfaceElevated,
                    focusedBorderColor = CrimsonAccent,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CoralPrimary,
                ),
                shape = RoundedCornerShape(14.dp),
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
                    .size(48.dp)
                    .background(
                        color = if (draft.isNotBlank() && !state.isThinking) CrimsonAccent else SurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send query",
                    tint = if (draft.isNotBlank() && !state.isThinking) Color.White else TextMuted,
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBlock(
    message: ChatMessage,
    onOpenArticle: (IndexedArticle) -> Unit,
) {
    val roleLabel = when (message.role) {
        ChatRole.USER -> "YOU"
        ChatRole.ASSISTANT -> "AEGIS"
        ChatRole.EMERGENCY -> "AEGIS EMERGENCY"
        ChatRole.SYSTEM -> "SYSTEM"
    }
    val roleColor = when (message.role) {
        ChatRole.USER -> StatusOk
        ChatRole.ASSISTANT -> CoralPrimary
        ChatRole.EMERGENCY -> StatusDanger
        ChatRole.SYSTEM -> TextSecondary
    }
    val containerBorder = when (message.role) {
        ChatRole.EMERGENCY -> BorderStroke(1.dp, StatusDanger.copy(alpha = 0.6f))
        ChatRole.USER -> BorderStroke(1.dp, HairlineBorder)
        else -> BorderStroke(1.dp, Color(0xFF191926))
    }

    Surface(
        color = if (message.role == ChatRole.USER) SurfaceInteractive else SurfaceElevated,
        border = containerBorder,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Unboxed inline metadata row separated by typographic dots
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
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (message.role == ChatRole.USER || message.role == ChatRole.SYSTEM) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.role == ChatRole.USER) TextPrimary else TextSecondary,
                )
            } else {
                ProtocolBody(text = message.text)
            }

            if (message.citedArticles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = HairlineBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Open Full Field Manual",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Spacer(modifier = Modifier.height(6.dp))
                message.citedArticles.forEach { cited ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onOpenArticle(cited) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cited.article.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = CoralPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${cited.domainLabel} · ${cited.article.priority.uppercase()} · ${cited.article.time}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Open article",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
