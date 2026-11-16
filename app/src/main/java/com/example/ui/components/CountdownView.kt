package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AnimeEntity
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.util.IstTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun CountdownView(
    anime: AnimeEntity? = null,
    airingTimeUnix: Long? = anime?.airingTimeUnix,
    nextEpNumber: Int? = anime?.nextEpisodeNumber,
    status: String? = anime?.status,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var currentTimeSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    val effectiveTime = anime?.getEffectiveAiringTime(currentTimeSeconds) ?: airingTimeUnix
    val isFinished = (status ?: anime?.status)?.uppercase() == "FINISHED"

    LaunchedEffect(airingTimeUnix, effectiveTime, isFinished) {
        if (effectiveTime == null || effectiveTime <= 0 || isFinished) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis() / 1000
            currentTimeSeconds = now
            val diff = effectiveTime - now
            delay(if (diff > 86400) 30_000L else 1000L)
        }
    }

    val chipShape = RoundedCornerShape(AppRadius.xs)

    if (effectiveTime == null || effectiveTime <= 0) {
        Row(
            modifier = modifier
                .clip(chipShape)
                .background(colors.elevatedSurface.copy(alpha = 0.7f))
                .border(1.dp, colors.borderSubtle, chipShape)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isFinished) Icons.Default.Check else Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isFinished) colors.success else colors.textMuted
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isFinished) "COMPLETED" else "SCHEDULE TBA",
                style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.sp),
                color = if (isFinished) colors.success else colors.textSecondary
            )
        }
        return
    }

    var targetTime = effectiveTime
    var targetEp = anime?.getEffectiveNextEpisodeNumber(currentTimeSeconds) ?: (nextEpNumber ?: 1)
    if (targetTime <= currentTimeSeconds && !isFinished) {
        val week = 604800L
        while (targetTime <= currentTimeSeconds) {
            targetTime += week
            targetEp += 1
        }
    }

    val secondsRemaining = targetTime - currentTimeSeconds

    if (secondsRemaining <= 0 || isFinished) {
        val formattedTime = remember(targetTime) { IstTimeFormatter.formatIstDateShort(targetTime) }
        Row(
            modifier = modifier
                .clip(chipShape)
                .background(colors.success.copy(alpha = 0.10f))
                .border(1.dp, colors.success.copy(alpha = 0.28f), chipShape)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = colors.success
            )
            Text(
                text = if (isFinished) "FINISHED • $formattedTime" else "AIRED • $formattedTime",
                style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 0.8.sp),
                color = colors.success
            )
        }
    } else {
        val countdownStr = formatCompactCountdown(secondsRemaining)
        Row(
            modifier = modifier
                .clip(chipShape)
                .background(colors.accentPrimary.copy(alpha = 0.08f))
                .border(1.dp, colors.accentPrimary.copy(alpha = 0.22f), chipShape)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "NEXT",
                style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 1.sp),
                color = colors.accentPrimary
            )
            Text(
                text = "EP $targetEp",
                style = TabularNumSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )
            Text(
                text = countdownStr,
                style = TabularNumSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.highlight
            )
        }
    }
}

fun formatCompactCountdown(secondsRemaining: Long): String {
    if (secondsRemaining <= 0L) return "Airing now"
    val days = secondsRemaining / 86400
    val hours = (secondsRemaining % 86400) / 3600
    val minutes = (secondsRemaining % 3600) / 60
    val seconds = secondsRemaining % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m ${seconds}s"
    }
}
