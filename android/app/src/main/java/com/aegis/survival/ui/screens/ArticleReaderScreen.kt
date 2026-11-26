package com.aegis.survival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegis.survival.data.IndexedArticle
import com.aegis.survival.ui.components.ProtocolBody
import com.aegis.survival.ui.theme.LocalAegisPalette

@Composable
fun ArticleReaderScreen(
    article: IndexedArticle,
    isBookmarked: Boolean,
    fontScale: Float,
    onAdjustFontScale: (Float) -> Unit,
    onToggleBookmark: () -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onAskFollowUp: (String) -> Unit,
) {
    val palette = LocalAegisPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas),
    ) {
        // Compact 56dp reader top bar with font legibility controls, bookmark & share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(palette.canvas)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.textPrimary,
                )
            }
            Text(
                text = article.domainLabel,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onAdjustFontScale((fontScale - 0.1f).coerceAtLeast(0.88f)) },
                enabled = fontScale > 0.90f,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.TextDecrease,
                    contentDescription = "Decrease text size",
                    tint = if (fontScale > 0.90f) palette.textSecondary else palette.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = { onAdjustFontScale((fontScale + 0.1f).coerceAtMost(1.32f)) },
                enabled = fontScale < 1.30f,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.TextIncrease,
                    contentDescription = "Increase text size",
                    tint = if (fontScale < 1.30f) palette.textSecondary else palette.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark protocol",
                    tint = if (isBookmarked) palette.accentSoft else palette.textSecondary,
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share protocol",
                    tint = palette.accentSoft,
                )
            }
        }

        HorizontalDivider(color = palette.border, thickness = 1.dp)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                text = "${article.article.priority.uppercase()} · ${article.article.time.uppercase()} · TIER ${article.tier} · ${article.topicLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.accentSoft,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.article.title,
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = palette.border, thickness = 1.dp)
            Spacer(modifier = Modifier.height(18.dp))

            ProtocolBody(text = article.article.body)

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Stored 100% offline inside AEGIS.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        HorizontalDivider(color = palette.border, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.canvas)
                .padding(14.dp),
        ) {
            Button(
                onClick = { onAskFollowUp("Explain key steps for ${article.article.title}") },
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = "Ask Follow-Up About This Protocol",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}
