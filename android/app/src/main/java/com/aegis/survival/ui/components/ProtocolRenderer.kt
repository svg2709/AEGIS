package com.aegis.survival.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.survival.ui.theme.LocalAegisPalette

private val NUMBERED_STEP_REGEX = Regex("^(\\d+)\\.\\s+(.*)$")
private val HEADING_REGEX = Regex("^\\*\\*(.+)\\*\\*$")

@Composable
fun ProtocolBody(
    text: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAegisPalette.current
    val lines = text.lines()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        lines.forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                return@forEach
            }

            val headingMatch = HEADING_REGEX.matchEntire(line)
            val stepMatch = NUMBERED_STEP_REGEX.matchEntire(line)

            when {
                line.startsWith("WARNING:", ignoreCase = true) ||
                    line.startsWith("⚠") ||
                    line.startsWith("CRITICAL:", ignoreCase = true) -> {
                    Surface(
                        color = palette.surfaceTinted,
                        border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = "Critical Warning",
                                tint = palette.danger,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cleanInlineMarkdown(line.removePrefix("⚠").trim()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.accentSoft,
                            )
                        }
                    }
                }

                line.startsWith("CAUTION:", ignoreCase = true) ||
                    line.startsWith("NOTE:", ignoreCase = true) -> {
                    Surface(
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.warn.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Caution",
                                tint = palette.warn,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cleanInlineMarkdown(line),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.warn,
                            )
                        }
                    }
                }

                headingMatch != null -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = headingMatch.groupValues[1],
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.accentSoft,
                        fontWeight = FontWeight.SemiBold,
                    )
                    HorizontalDivider(color = palette.borderSubtle, thickness = 1.dp)
                }

                stepMatch != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            color = palette.surfaceElevated,
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = stepMatch.groupValues[1],
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = palette.accentSoft,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = cleanInlineMarkdown(stepMatch.groupValues[2]),
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                line.startsWith("- ") || line.startsWith("• ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.accentSoft,
                            modifier = Modifier.padding(end = 10.dp),
                        )
                        Text(
                            text = cleanInlineMarkdown(line.drop(2).trim()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                else -> {
                    Text(
                        text = cleanInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                    )
                }
            }
        }
    }
}

private fun cleanInlineMarkdown(input: String): String {
    return input
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
}
