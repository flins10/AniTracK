package com.example.ui.widgets

import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

enum class SandroneMascotVariant {
    FLOATING_PHONE,
    RAIL,
    SIDEBAR
}

object AndroidSandroneAudioPlayer {
    private const val ASSET_DIR = "sandrone"
    private const val IMAGE_ASSET_PATH = "$ASSET_DIR/sandrone.png"
    private val VOICE_CLIPS = listOf(
        "s.mp3",
        "s1.mp3",
        "s2.mp3",
        "s3.mp3",
        "s4.mp3",
        "s5.mp3",
        "s6.mp3",
        "s7.mp3",
        "s8.mp3",
        "s9.mp3"
    )

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val playToken = AtomicLong(0L)
    private var currentPlayer: MediaPlayer? = null
    private var lastClipIndex: Int = -1

    @Volatile
    private var cachedBitmap: ImageBitmap? = null

    fun loadBitmap(context: Context): ImageBitmap? {
        cachedBitmap?.let { return it }
        return runCatching {
            context.assets.open(IMAGE_ASSET_PATH).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()?.also { decoded ->
            cachedBitmap = decoded
        }
    }

    private fun pickNextClip(): String {
        val nextIndex = synchronized(lock) {
            var idx = Random.nextInt(VOICE_CLIPS.size)
            if (VOICE_CLIPS.size > 1 && idx == lastClipIndex) {
                idx = (idx + 1 + Random.nextInt(VOICE_CLIPS.size - 1)) % VOICE_CLIPS.size
            }
            lastClipIndex = idx
            idx
        }
        return VOICE_CLIPS[nextIndex]
    }

    fun playRandom(context: Context, onPlayingChanged: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        val clipName = pickNextClip()
        val token = playToken.incrementAndGet()
        stopCurrentPlayer()

        val player = MediaPlayer()
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            val assetPath = "$ASSET_DIR/$clipName"
            val attached = runCatching {
                appContext.assets.openFd(assetPath).use { afd ->
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            }.isSuccess

            if (!attached) {
                val cacheFile = File(appContext.cacheDir, "sandrone_$clipName")
                if (!cacheFile.exists() || cacheFile.length() == 0L) {
                    appContext.assets.open(assetPath).use { input ->
                        cacheFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                player.setDataSource(cacheFile.absolutePath)
            }

            player.setOnCompletionListener { mp ->
                if (playToken.get() == token) {
                    mainHandler.post { onPlayingChanged(false) }
                }
                releasePlayerIfCurrent(mp)
            }
            player.setOnErrorListener { mp, _, _ ->
                if (playToken.get() == token) {
                    mainHandler.post { onPlayingChanged(false) }
                }
                releasePlayerIfCurrent(mp)
                true
            }

            synchronized(lock) {
                currentPlayer = player
            }
            player.prepare()
            player.start()
            mainHandler.post { onPlayingChanged(true) }
        } catch (_: Exception) {
            runCatching { player.release() }
            synchronized(lock) {
                if (currentPlayer === player) currentPlayer = null
            }
            mainHandler.post { onPlayingChanged(false) }
        }
    }

    private fun releasePlayerIfCurrent(target: MediaPlayer) {
        synchronized(lock) {
            if (currentPlayer === target) {
                currentPlayer = null
            }
        }
        runCatching { target.release() }
    }

    private fun stopCurrentPlayer() {
        val prev = synchronized(lock) {
            val existing = currentPlayer
            currentPlayer = null
            existing
        }
        if (prev != null) {
            runCatching {
                if (prev.isPlaying) prev.stop()
            }
            runCatching { prev.release() }
        }
    }

    fun stop() {
        playToken.incrementAndGet()
        stopCurrentPlayer()
    }
}

@Composable
fun SandroneMascotButton(
    variant: SandroneMascotVariant,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val bitmap = remember(context) { AndroidSandroneAudioPlayer.loadBitmap(context) } ?: return
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var isPressedBounce by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressedBounce -> 0.91f
            isPlaying -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sandrone_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sandrone_aura")
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sandrone_aura_scale"
    )

    DisposableEffect(Unit) {
        onDispose {
            AndroidSandroneAudioPlayer.stop()
        }
    }

    val handleTap: () -> Unit = {
        isPressedBounce = true
        scope.launch {
            delay(130)
            isPressedBounce = false
        }
        AndroidSandroneAudioPlayer.playRandom(context) { playing ->
            isPlaying = playing
        }
    }

    when (variant) {
        SandroneMascotVariant.FLOATING_PHONE -> {
            Box(
                modifier = modifier
                    .size(68.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("sandrone_mascot_phone"),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .graphicsLayer {
                                scaleX = auraPulse
                                scaleY = auraPulse
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        colors.accentPrimary.copy(alpha = 0.42f),
                                        colors.accentPrimary.copy(alpha = 0f)
                                    )
                                )
                            )
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (colors.isDark) {
                        colors.backgroundSecondary.copy(alpha = 0.90f)
                    } else {
                        colors.cardBackground.copy(alpha = 0.94f)
                    },
                    border = BorderStroke(
                        width = if (isPlaying) 1.5.dp else 1.dp,
                        color = if (isPlaying) {
                            colors.accentPrimary.copy(alpha = 0.85f)
                        } else {
                            colors.borderStrong.copy(alpha = 0.72f)
                        }
                    ),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Play a random Sandrone voice line",
                            onClick = handleTap
                        )
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Sandrone mascot, tap to play a random voice line",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(5.dp)
                    )
                }
            }
        }

        SandroneMascotVariant.RAIL -> {
            Box(
                modifier = modifier
                    .size(66.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "Play a random Sandrone voice line",
                        onClick = handleTap
                    )
                    .testTag("sandrone_mascot_rail"),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .graphicsLayer {
                                scaleX = auraPulse
                                scaleY = auraPulse
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        colors.accentPrimary.copy(alpha = 0.36f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                Image(
                    bitmap = bitmap,
                    contentDescription = "Sandrone mascot, tap to play a random voice line",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(58.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }

        SandroneMascotVariant.SIDEBAR -> {
            Box(
                modifier = modifier
                    .size(184.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "Play a random Sandrone voice line",
                        onClick = handleTap
                    )
                    .testTag("sandrone_mascot_sidebar"),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .graphicsLayer {
                                scaleX = auraPulse
                                scaleY = auraPulse
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        colors.accentPrimary.copy(alpha = 0.34f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                Image(
                    bitmap = bitmap,
                    contentDescription = "Sandrone mascot, tap to play a random voice line",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(172.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }
    }
}
