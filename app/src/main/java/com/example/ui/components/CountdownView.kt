package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AnimeEntity
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CountdownView(
    anime: AnimeEntity? = null,
    airingTimeUnix: Long? = anime?.airingTimeUnix,
    nextEpNumber: Int? = anime?.nextEpisodeNumber,
    status: String? = anime?.status,
    modifier: Modifier = Modifier
) {
    var currentTimeSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    val effectiveTime = anime?.getEffectiveAiringTime(currentTimeSeconds) ?: airingTimeUnix
    val isFinished = (status ?: anime?.status)?.uppercase() == "FINISHED"

    LaunchedEffect(airingTimeUnix, effectiveTime, isFinished) {
        if (effectiveTime == null || effectiveTime <= 0 || isFinished) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis() / 1000
            currentTimeSeconds = now
            val target = effectiveTime
            val diff = target - now
            val delayMs = if (diff > 86400) 30_000L else 1000L
            delay(delayMs)
        }
    }

    if (effectiveTime == null || effectiveTime <= 0) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "TBA / Finished",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        return
    }

    // Dynamic target calculation so countdown NEVER stops for releasing anime
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
        val formattedTime = remember<String>(targetTime) {
            com.example.util.IstTimeFormatter.formatIstDateShort(targetTime)
        }
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF10B981).copy(alpha = 0.20f))
                .border(0.8.dp, Color(0xFF34D399).copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = Color(0xFF34D399)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isFinished) "Finished ($formattedTime)" else "Aired ($formattedTime)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = Color(0xFF6EE7B7)
            )
        }
    } else {
        val days = secondsRemaining / 86400
        val hours = (secondsRemaining % 86400) / 3600
        val minutes = (secondsRemaining % 3600) / 60
        val seconds = secondsRemaining % 60

        val countdownText = when {
            days > 0 -> "Ep $targetEp in ${days}d ${hours}h ${minutes}m"
            hours > 0 -> "Ep $targetEp in ${hours}h ${minutes}m ${seconds}s"
            else -> "Ep $targetEp in ${minutes}m ${seconds}s"
        }

        Row(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0284C7).copy(alpha = 0.22f))
                .border(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.60f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = countdownText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = Color(0xFFE0F2FE)
            )
        }
    }
}
