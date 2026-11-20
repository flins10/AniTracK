package com.example.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

internal const val MAX_REASONABLE_UNKNOWN_EPISODES = 20000

sealed class DesktopEpisodeValidation {
    data class Valid(val episodes: Int) : DesktopEpisodeValidation()
    data class Invalid(val message: String) : DesktopEpisodeValidation()
}

object DesktopTrackingRules {
    fun isOfficiallyFinished(officialStatus: String?): Boolean {
        val norm = officialStatus?.trim()?.uppercase().orEmpty()
        return norm == "FINISHED" || norm == "FINISHED_AIRING"
    }

    /**
     * Automatic Anime Completion — strictly requires ALL 3 conditions:
     * 1. Official anime status is FINISHED / FINISHED_AIRING (completed)
     * 2. Official total episode count is known (> 0)
     * 3. User's watched episode count >= total episode count
     */
    fun shouldAutoComplete(
        officialStatus: String?,
        totalEpisodes: Int?,
        watchedEpisodes: Int
    ): Boolean {
        if (!isOfficiallyFinished(officialStatus)) return false
        if (totalEpisodes == null || totalEpisodes <= 0) return false
        return watchedEpisodes >= totalEpisodes
    }

    fun shouldRemoveFromHome(
        watchStatus: String?,
        officialStatus: String?,
        totalEpisodes: Int?,
        watchedEpisodes: Int
    ): Boolean {
        val normStatus = watchStatus?.trim()?.lowercase().orEmpty()
        if (normStatus == "dropped" || normStatus == "completed") return true
        return shouldAutoComplete(officialStatus, totalEpisodes, watchedEpisodes)
    }

    fun validateEpisodeInput(rawInput: String, totalEpisodes: Int?): DesktopEpisodeValidation {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return DesktopEpisodeValidation.Invalid("Enter a whole episode number")
        }
        if (trimmed.startsWith("-")) {
            return DesktopEpisodeValidation.Invalid("Episode count cannot be negative")
        }
        if (trimmed.contains('.') || trimmed.contains(',')) {
            return DesktopEpisodeValidation.Invalid("Whole numbers only (no decimals)")
        }
        if (!trimmed.all { it.isDigit() }) {
            return DesktopEpisodeValidation.Invalid("Numbers only (0–9)")
        }
        val parsed = trimmed.toIntOrNull()
            ?: return DesktopEpisodeValidation.Invalid("Episode number is too large")
        if (parsed < 0) {
            return DesktopEpisodeValidation.Invalid("Episode count cannot be negative")
        }
        if (totalEpisodes != null && totalEpisodes > 0 && parsed > totalEpisodes) {
            return DesktopEpisodeValidation.Invalid("Cannot exceed $totalEpisodes episodes")
        }
        if ((totalEpisodes == null || totalEpisodes <= 0) && parsed > MAX_REASONABLE_UNKNOWN_EPISODES) {
            return DesktopEpisodeValidation.Invalid("Please enter a reasonable episode count")
        }
        return DesktopEpisodeValidation.Valid(parsed)
    }
}

internal fun loadDesktopWindowIconPainter(): Painter? {
    val resourcePaths = listOf(
        "/icons/app_icon.png",
        "icons/app_icon.png",
        "/app_icon.png"
    )
    for (path in resourcePaths) {
        val bytes = runCatching {
            DesktopTrackingRules::class.java.getResourceAsStream(path)?.use { it.readBytes() }
                ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(path.removePrefix("/"))?.use { it.readBytes() }
        }.getOrNull()
        if (bytes != null && bytes.isNotEmpty()) {
            val bmp = runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
            if (bmp != null) return BitmapPainter(bmp)
        }
    }
    val fileCandidates = listOf(
        File("desktop/src/main/resources/icons/app_icon.png"),
        File("src/main/resources/icons/app_icon.png"),
        File("public/icon-512.png"),
        File("../public/icon-512.png")
    )
    for (file in fileCandidates) {
        if (file.exists() && file.isFile) {
            val bmp = runCatching { SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap() }.getOrNull()
            if (bmp != null) return BitmapPainter(bmp)
        }
    }
    return null
}

@Composable
internal fun DesktopRemoveFromHomeDialog(
    animeTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12151E),
        titleContentColor = Color(0xFFF4EFE6),
        textContentColor = Color(0xFF9499A6),
        title = {
            Text(
                text = "Remove from Home?",
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp,
                color = Color(0xFFF4EFE6)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (animeTitle.isNotBlank()) {
                    Text(
                        text = animeTitle,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE5C17B)
                    )
                }
                Text(
                    text = "This anime will be removed from your Home tracking list but will remain in your Library. Your notes, ratings, reviews, and Community discussions will not be affected.",
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF9499A6)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF26D7D),
                    contentColor = Color(0xFF110E08)
                )
            ) {
                Text("Remove from Home", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9499A6))
            }
        }
    )
}

@Composable
internal fun DesktopInlineEpisodeStepper(
    watchedEpisodes: Int,
    totalEpisodes: Int?,
    compact: Boolean = true,
    onCommitEpisode: suspend (Int) -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var draft by remember(watchedEpisodes) {
        mutableStateOf(
            TextFieldValue(
                text = watchedEpisodes.toString(),
                selection = TextRange(0, watchedEpisodes.toString().length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    val totalKnown = totalEpisodes?.takeIf { it > 0 }
    val canDecrement = !isSaving && watchedEpisodes > 0
    val canIncrement = !isSaving && (totalKnown == null || watchedEpisodes < totalKnown)

    fun startEditing() {
        if (isSaving) return
        val txt = watchedEpisodes.toString()
        draft = TextFieldValue(text = txt, selection = TextRange(0, txt.length))
        errorText = null
        isEditing = true
    }

    fun cancelEditing() {
        isEditing = false
        errorText = null
        val txt = watchedEpisodes.toString()
        draft = TextFieldValue(text = txt, selection = TextRange(0, txt.length))
    }

    fun commitValue(raw: String) {
        if (isSaving) return
        when (val validation = DesktopTrackingRules.validateEpisodeInput(raw, totalKnown)) {
            is DesktopEpisodeValidation.Invalid -> {
                errorText = validation.message
            }
            is DesktopEpisodeValidation.Valid -> {
                val target = validation.episodes
                errorText = null
                if (target == watchedEpisodes) {
                    isEditing = false
                    return
                }
                isSaving = true
                scope.launch {
                    val result = onCommitEpisode(target)
                    isSaving = false
                    result.onSuccess {
                        isEditing = false
                        errorText = null
                    }.onFailure { err ->
                        val prev = watchedEpisodes.toString()
                        draft = TextFieldValue(text = prev, selection = TextRange(0, prev.length))
                        errorText = err.message ?: "Failed to save episode count"
                    }
                }
            }
        }
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF171A24),
            border = BorderStroke(
                width = 1.dp,
                color = when {
                    errorText != null -> Color(0xFFF26D7D)
                    isEditing -> Color(0xFFE5C17B)
                    else -> Color(0xFF262B3A)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 4.dp else 8.dp, vertical = if (compact) 3.dp else 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isEditing) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF12151E).copy(alpha = if (canDecrement) 1f else 0.45f),
                        modifier = Modifier
                            .size(if (compact) 26.dp else 32.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canDecrement) {
                                commitValue((watchedEpisodes - 1).coerceAtLeast(0).toString())
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "−",
                                fontSize = if (compact) 14.sp else 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (canDecrement) Color(0xFFF4EFE6) else Color(0xFF636875)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isSaving) { startEditing() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color(0xFFE5C17B),
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$watchedEpisodes",
                                    fontSize = if (compact) 13.sp else 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF4EFE6)
                                )
                                Text(
                                    text = " / ${totalKnown?.toString() ?: "∞"}",
                                    fontSize = if (compact) 11.5.sp else 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF9499A6)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (canIncrement) Color(0xFFE5C17B) else Color(0xFF12151E).copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(if (compact) 26.dp else 32.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canIncrement) {
                                commitValue((watchedEpisodes + 1).toString())
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "+",
                                fontSize = if (compact) 14.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canIncrement) Color(0xFF110E08) else Color(0xFF636875)
                            )
                        }
                    }
                } else {
                    // Direct numeric inline editing mode: [ 850 ] / 1150  [✓] [✕]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0E1017),
                            border = BorderStroke(1.dp, Color(0xFFE5C17B).copy(alpha = 0.75f)),
                            modifier = Modifier
                                .widthIn(min = if (compact) 52.dp else 72.dp, max = if (compact) 72.dp else 96.dp)
                                .height(if (compact) 26.dp else 32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                BasicTextField(
                                    value = draft,
                                    onValueChange = { next ->
                                        draft = next
                                        errorText = null
                                    },
                                    singleLine = true,
                                    enabled = !isSaving,
                                    cursorBrush = SolidColor(Color(0xFFE5C17B)),
                                    textStyle = TextStyle(
                                        color = Color(0xFFF4EFE6),
                                        fontSize = if (compact) 12.5.sp else 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.Enter, Key.NumPadEnter -> {
                                                        commitValue(draft.text)
                                                        true
                                                    }
                                                    Key.Escape -> {
                                                        cancelEditing()
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                )
                            }
                        }

                        Text(
                            text = "/ ${totalKnown?.toString() ?: "∞"}",
                            fontSize = if (compact) 11.sp else 13.sp,
                            color = Color(0xFF9499A6),
                            modifier = Modifier.weight(1f)
                        )

                        if (isSaving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color(0xFFE5C17B),
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE5C17B),
                                modifier = Modifier
                                    .size(if (compact) 24.dp else 28.dp)
                                    .clip(CircleShape)
                                    .clickable { commitValue(draft.text) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Save episodes",
                                        tint = Color(0xFF110E08),
                                        modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF262B3A),
                                modifier = Modifier
                                    .size(if (compact) 24.dp else 28.dp)
                                    .clip(CircleShape)
                                    .clickable { cancelEditing() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Cancel editing",
                                        tint = Color(0xFFF4EFE6),
                                        modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText!!,
                fontSize = 10.5.sp,
                color = Color(0xFFF26D7D),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
