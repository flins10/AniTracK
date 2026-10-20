package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

val PLAYBACK_SPEED_PRESETS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)

val ALL_PLAYBACK_SPEEDS = buildList {
    add(0.5f)
    add(0.75f)
    // 1.0 to 4.0 in 0.1 increments
    for (i in 10..40) {
        add(i / 10.0f)
    }
}

/**
 * Scrollable playback speed ruler supporting speeds from 1.0x to 4.0x in 0.1x increments,
 * plus quick presets, a Reset to 1.0x button, and snapping.
 */
@Composable
fun PlaybackSpeedRuler(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Center the current speed in the ruler when first launched or changed externally
    LaunchedEffect(currentSpeed) {
        val targetIndex = ALL_PLAYBACK_SPEEDS.indexOfFirst { abs(it - currentSpeed) < 0.05f }
        if (targetIndex >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1322), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("playback_speed_ruler_panel")
    ) {
        // Header: Speed Icon, Current Speed Indicator & Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PLAYBACK SPEED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF94A3B8)
                        )
                    )
                    Text(
                        text = String.format(Locale.ROOT, "%.1f×", currentSpeed),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF38BDF8)
                        )
                    )
                }
            }

            // Quick "Reset to 1.0x" Button
            if (abs(currentSpeed - 1.0f) > 0.05f) {
                OutlinedButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onSpeedSelected(1.0f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF0284C7).copy(alpha = 0.15f),
                        contentColor = Color(0xFF38BDF8)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("reset_speed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset 1.0×",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Ruler Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFF070B16), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(horizontal = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("speed_ruler_scroll_row")
            ) {
                itemsIndexed(ALL_PLAYBACK_SPEEDS) { _, speed ->
                    val isSelected = abs(speed - currentSpeed) < 0.05f
                    val isMajor = (speed * 10).roundToInt() % 10 == 0 || speed == 0.5f

                    val pillBgColor by animateColorAsState(
                        if (isSelected) Color(0xFF0284C7) else Color.Transparent,
                        label = "pill_bg"
                    )
                    val textColor by animateColorAsState(
                        if (isSelected) Color.White else if (isMajor) Color(0xFFE2E8F0) else Color(0xFF64748B),
                        label = "pill_text"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSpeedSelected(speed)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = pillBgColor,
                            border = if (isSelected) BorderStroke(1.2.dp, Color(0xFF38BDF8)) else null,
                            shadowElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Text(
                                text = String.format(Locale.ROOT, "%.1f×", speed),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else if (isMajor) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = if (isSelected) 12.sp else 11.sp,
                                    color = textColor
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Ruler Tick Mark
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 3.dp else if (isMajor) 2.dp else 1.dp)
                                .height(if (isSelected) 14.dp else if (isMajor) 10.dp else 6.dp)
                                .background(
                                    if (isSelected) Color(0xFF38BDF8)
                                    else if (isMajor) Color.White.copy(alpha = 0.5f)
                                    else Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        )
                    }
                }
            }

            // Center Indicator Line (Cyan Gradient)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(36.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF38BDF8),
                                Color(0xFF06B6D4),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Preset Chips (0.75x, 1.0x, 1.25x, 1.5x, 2.0x, 3.0x, 4.0x)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PLAYBACK_SPEED_PRESETS.forEach { preset ->
                val isSelected = abs(preset - currentSpeed) < 0.05f
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onSpeedSelected(preset)
                        }
                ) {
                    Text(
                        text = "${preset}×",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
