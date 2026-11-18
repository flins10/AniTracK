package com.example.ui.widgets

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.data.local.AnimeEntity
import com.example.model.BuiltInAvatar
import com.example.model.ProgressRules
import com.example.model.SeasonRules
import com.example.model.SentimentCategory
import com.example.model.StarRatingUtils
import com.example.model.UserProfile
import com.example.model.WatchStatus
import com.example.ui.theme.AppMotion
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldBronze
import com.example.ui.theme.ChampagneGoldSoft
import com.example.ui.theme.ChampagneOnGold
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumLarge
import com.example.ui.theme.TabularNumMedium
import com.example.ui.theme.TabularNumSmall
import com.example.viewmodel.DynamicIslandFeedback
import kotlinx.coroutines.delay

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Obsidian & Champagne Gold subtle top/bottom radial atmosphere matching the HTML .phone backdrop.
 */
@Composable
fun ObsidianAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundMain)
    ) {
        if (colors.isDark) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.accentPrimary.copy(alpha = 0.045f), Color.Transparent),
                        center = Offset(size.width * 0.5f, -size.height * 0.06f),
                        radius = size.maxDimension * 0.58f
                    ),
                    center = Offset(size.width * 0.5f, -size.height * 0.06f),
                    radius = size.maxDimension * 0.58f
                )
            }
        }
        content()
    }
}

/**
 * Luxury editorial top bar matching the HTML .topbar:
 * Left: optional Monogram gold tile ('A') + Cormorant Serif title + uppercase Ash subtitle
 * Right: optional icon actions + glowing circular gold Profile avatar button
 */
@Composable
fun ObsidianTopBar(
    title: String,
    subtitle: String,
    userProfile: UserProfile?,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    showBrandMonogram: Boolean = false,
    accentTitleTail: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (accentTitleTail != null) {
                    Text(
                        text = buildAnnotatedString {
                            append(title)
                            withStyle(
                                SpanStyle(
                                    color = colors.accentPrimary,
                                    fontStyle = FontStyle.Italic
                                )
                            ) {
                                append(accentTitleTail)
                            }
                        },
                        fontFamily = CormorantSerifFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = title,
                        fontFamily = CormorantSerifFamily,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        lineHeight = 30.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = subtitle.uppercase(),
                    style = MicroUpperLabel.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 2.4.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            actions()

            // Crisp Profile Avatar Button
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = colors.accentPrimary.copy(alpha = 0.35f),
                        spotColor = colors.accentPrimary.copy(alpha = 0.35f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.accentPrimary, colors.accentSecondary)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = onOpenProfile)
                    .testTag("profile_nav_button"),
                contentAlignment = Alignment.Center
            ) {
                val preset = BuiltInAvatar.fromKey(
                    userProfile?.avatarKey?.takeIf { it.isNotBlank() } ?: userProfile?.avatarUrl
                )
                Image(
                    painter = painterResource(id = preset.drawableRes()),
                    contentDescription = "Open Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    containerColor: Color = AppTheme.colors.cardBackground,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val reducedMotion = rememberReducedMotion()
    val colors = AppTheme.colors

    val scale by animateFloatAsState(
        targetValue = when {
            reducedMotion || onClick == null -> 1f
            pressed -> 0.985f
            hovered -> 1.03f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "card_hover_scale"
    )
    val elevation by animateDpAsState(
        targetValue = when {
            hovered && onClick != null -> 14.dp
            colors.isDark -> 2.dp
            else -> 4.dp
        },
        animationSpec = tween(durationMillis = AppMotion.FAST_MS),
        label = "card_hover_elevation"
    )
    val resolvedBorder = borderColor ?: colors.borderSubtle
    val animatedBorder by animateColorAsState(
        targetValue = if (hovered && onClick != null) colors.accentPrimary.copy(alpha = 0.72f) else resolvedBorder,
        animationSpec = tween(durationMillis = AppMotion.FAST_MS),
        label = "card_hover_border"
    )

    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(if (hovered && onClick != null) 1.2.dp else 0.75.dp, animatedBorder),
        tonalElevation = 0.dp,
        shadowElevation = elevation,
        modifier = modifier
            .zIndex(if (hovered && onClick != null) 3f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hoverable(interactionSource = interactionSource)
            .then(
                if (onClick != null) {
                    Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clip(shape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                } else Modifier.clip(shape)
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled || reducedMotion -> 1f
            pressed -> 0.97f
            hovered -> 1.025f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "primary_btn_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accentPrimary,
            contentColor = colors.onAccentPrimary,
            disabledContainerColor = colors.elevatedSurface,
            disabledContentColor = colors.textSecondary
        ),
        contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled || reducedMotion -> 1f
            pressed -> 0.97f
            hovered -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "secondary_btn_scale"
    )
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = CircleShape,
        border = BorderStroke(0.9.dp, if (hovered) colors.accentPrimary else colors.borderStrong.copy(alpha = 0.75f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.elevatedSurface.copy(alpha = if (colors.isDark) 0.72f else 0.90f),
            contentColor = colors.textPrimary
        ),
        contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.accentPrimary)
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.textPrimary,
    containerColor: Color = AppTheme.colors.elevatedSurface
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = when {
            reducedMotion -> 1f
            pressed -> 0.94f
            hovered -> 1.06f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "icon_btn_scale"
    )
    Surface(
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(0.8.dp, if (hovered) colors.accentPrimary.copy(alpha = 0.65f) else colors.borderSubtle),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.elevatedSurface.copy(alpha = 0.78f),
            unfocusedContainerColor = colors.cardBackground.copy(alpha = 0.65f),
            focusedBorderColor = colors.accentPrimary.copy(alpha = 0.55f),
            unfocusedBorderColor = if (colors.isDark) Color.White.copy(alpha = 0.06f) else colors.borderSubtle,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accentPrimary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun RatingBadge(
    scoreText: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        shape = CircleShape,
        color = if (colors.isDark) Color(0xEB0E1018) else Color(0xF5FFFFFF),
        border = BorderStroke(0.8.dp, colors.accentPrimary.copy(alpha = 0.55f)),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = colors.accentPrimary,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = scoreText,
                style = TabularNumSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                color = if (colors.isDark) colors.highlight else colors.textPrimary
            )
        }
    }
}

@Composable
fun StarRatingRow(
    stars: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 15.dp,
    showNumeric: Boolean = true,
    tint: Color = AppTheme.colors.accentPrimary,
    emptyTint: Color = AppTheme.colors.textMuted.copy(alpha = 0.32f)
) {
    val colors = AppTheme.colors
    val clamped = stars.coerceIn(0f, 5f)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..5) {
            val fill = (clamped - (i - 1)).coerceIn(0f, 1f)
            Box(modifier = Modifier.size(starSize), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = emptyTint,
                    modifier = Modifier.fillMaxSize()
                )
                if (fill > 0f) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(right = size.width * fill) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            }
        }
        if (showNumeric && clamped > 0f) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = StarRatingUtils.formatStars(clamped),
                style = TabularNumMedium.copy(
                    fontSize = (starSize.value - 1f).coerceAtLeast(12f).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.highlight
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveStarRatingSelector(
    stars: Float,
    onStarsChanged: (Float) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val clamped = stars.coerceIn(0f, 5f)
    val activeSentiment = SentimentCategory.fromStars(clamped)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Big editorial star summary + clear action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "YOUR STAR RATING",
                    style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.8.sp),
                    color = colors.accentPrimary
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (clamped > 0f) "${StarRatingUtils.formatStars(clamped)} ★" else "Unrated",
                        fontFamily = CormorantSerifFamily,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (clamped > 0f) colors.highlight else colors.textMuted
                    )
                    if (activeSentiment != null) {
                        Text(
                            text = activeSentiment.fullLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
            if (clamped > 0f) {
                Surface(
                    shape = CircleShape,
                    color = colors.elevatedSurface.copy(alpha = 0.65f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClear)
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Tactile 5-Star Bar (tap same full star to switch to half-star; tap again for full)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.elevatedSurface.copy(alpha = 0.48f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (starIdx in 1..5) {
                        val fullVal = starIdx.toFloat()
                        val halfVal = starIdx - 0.5f
                        val fill = (clamped - (starIdx - 1)).coerceIn(0f, 1f)
                        val starInteraction = remember { MutableInteractionSource() }
                        val starHovered by starInteraction.collectIsHoveredAsState()
                        val starScale by animateFloatAsState(
                            targetValue = when {
                                starHovered -> 1.16f
                                fill > 0f -> 1.07f
                                else -> 1f
                            },
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                            label = "star_scale_$starIdx"
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = starScale
                                    scaleY = starScale
                                }
                                .hoverable(starInteraction)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable(interactionSource = starInteraction, indication = null) {
                                    val next = when {
                                        clamped == fullVal -> halfVal
                                        else -> fullVal
                                    }
                                    onStarsChanged(next)
                                }
                                .testTag("rating_star_$starIdx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$starIdx stars",
                                tint = colors.textMuted.copy(alpha = 0.25f),
                                modifier = Modifier.size(34.dp)
                            )
                            if (fill > 0f) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = colors.accentPrimary,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .drawWithContent {
                                            clipRect(right = size.width * fill) {
                                                this@drawWithContent.drawContent()
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                // Precision half-star quick pills & hint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap a star again for ½ ★",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = colors.cardBackground.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val next = (clamped - 0.5f).coerceAtLeast(0.5f)
                                    onStarsChanged(next)
                                }
                        ) {
                            Text(
                                text = "−0.5 ★",
                                style = TabularNumSmall.copy(fontSize = 10.sp),
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = colors.cardBackground.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val next = if (clamped <= 0f) 0.5f else (clamped + 0.5f).coerceAtMost(5f)
                                    onStarsChanged(next)
                                }
                        ) {
                            Text(
                                text = "+0.5 ★",
                                style = TabularNumSmall.copy(fontSize = 10.sp),
                                color = colors.accentPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Sentiment Quick-Pick Pills synced with Star rating
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SentimentCategory.entries.forEach { cat ->
                val selected = activeSentiment == cat
                val pillColor = cat.badgeColor()
                Surface(
                    shape = CircleShape,
                    color = if (selected) pillColor.copy(alpha = 0.22f) else colors.elevatedSurface.copy(alpha = 0.45f),
                    border = if (selected) BorderStroke(1.dp, pillColor.copy(alpha = 0.7f)) else null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            onStarsChanged(StarRatingUtils.score100ToStars(cat.suggestedScoreTenths))
                        }
                ) {
                    Text(
                        text = "${cat.shortLabel.uppercase()} • ${StarRatingUtils.formatStars(StarRatingUtils.score100ToStars(cat.suggestedScoreTenths))}★",
                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 0.6.sp),
                        color = if (selected) pillColor else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppProgressBar(
    progress: Float,
    isCompleted: Boolean = false,
    heightDp: Int = 3,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val reducedMotion = rememberReducedMotion()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else AppMotion.STANDARD_MS),
        label = "app_progress_bar"
    )
    val fillBrush = if (isCompleted) {
        Brush.horizontalGradient(listOf(colors.success, colors.success))
    } else {
        Brush.horizontalGradient(listOf(colors.accentSecondary, colors.accentPrimary))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(CircleShape)
            .background(colors.borderSubtle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(fillBrush)
        )
    }
}

@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    AppCard(
        shape = RoundedCornerShape(AppRadius.xl),
        borderColor = colors.borderStrong,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.accentPrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(24.dp))
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                AppPrimaryButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun ShimmerSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadius.lg)
) {
    val colors = AppTheme.colors
    val reducedMotion = rememberReducedMotion()
    val base = colors.cardBackground
    val highlight = colors.elevatedSurface

    if (reducedMotion) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(base)
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translate by transition.animateFloat(
        initialValue = -250f,
        targetValue = 850f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(translate, 0f),
                    end = Offset(translate + 220f, 180f)
                )
            )
    )
}

@Composable
fun ResilientPosterImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadius.md),
    contentScale: ContentScale = ContentScale.Crop
) {
    val colors = AppTheme.colors
    if (imageUrl.isNullOrBlank()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(colors.elevatedSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = contentDescription,
                tint = colors.textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(26.dp)
            )
        }
        return
    }

    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.clip(shape)
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                ShimmerSkeletonBox(modifier = Modifier.fillMaxSize(), shape = shape)
            }
            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.elevatedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = contentDescription,
                        tint = colors.textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            else -> SubcomposeAsyncImageContent()
        }
    }
}

data class NavDestinationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Floating Obsidian & Champagne Gold capsule dock matching the HTML .bottomnav specification.
 */
@Composable
fun MidnightBottomNavigationBar(
    destinations: List<NavDestinationItem>,
    currentRoute: String?,
    onSelect: (NavDestinationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to colors.backgroundMain.copy(alpha = 0.88f),
                    1f to colors.backgroundMain
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("bottom_navigation_bar"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (colors.isDark) colors.backgroundSecondary.copy(alpha = 0.96f) else colors.cardBackground,
            border = BorderStroke(0.9.dp, colors.borderStrong.copy(alpha = 0.7f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val interactionSource = remember { MutableInteractionSource() }
                    val hovered by interactionSource.collectIsHoveredAsState()
                    val iconTint by animateColorAsState(
                        targetValue = if (selected) colors.accentPrimary else if (hovered) colors.textPrimary else colors.textMuted,
                        animationSpec = tween(AppMotion.FAST_MS),
                        label = "nav_icon"
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (selected) colors.highlight else if (hovered) colors.textPrimary else colors.textMuted,
                        animationSpec = tween(AppMotion.FAST_MS),
                        label = "nav_label"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    selected -> colors.accentPrimary.copy(alpha = 0.14f)
                                    hovered -> colors.elevatedSurface.copy(alpha = 0.65f)
                                    else -> Color.Transparent
                                }
                            )
                            .hoverable(interactionSource)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelect(tab) }
                            )
                            .testTag("nav_item_${tab.route}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title.uppercase(),
                            style = MicroUpperLabel.copy(
                                fontSize = 8.sp,
                                letterSpacing = 0.9.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
                            ),
                            color = labelColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MidnightCompactRail(
    destinations: List<NavDestinationItem>,
    currentRoute: String?,
    onSelect: (NavDestinationItem) -> Unit,
    onOpenQuickSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        color = colors.backgroundSecondary,
        modifier = modifier
            .width(84.dp)
            .fillMaxHeight()
            .testTag("navigation_rail")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = AppSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppIconButton(
                    icon = Icons.Filled.Search,
                    contentDescription = "Quick Jump",
                    onClick = onOpenQuickSearch
                )

                Spacer(modifier = Modifier.height(4.dp))

                destinations.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val interactionSource = remember { MutableInteractionSource() }
                    val hovered by interactionSource.collectIsHoveredAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (hovered) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                        label = "compact_rail_scale"
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            selected -> colors.accentPrimary.copy(alpha = 0.16f)
                            hovered -> colors.elevatedSurface
                            else -> Color.Transparent
                        },
                        border = if (selected) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.5f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .hoverable(interactionSource)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(interactionSource = interactionSource, indication = null) { onSelect(tab) }
                            .testTag("rail_item_${tab.route}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (selected) colors.accentPrimary else colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (selected) colors.highlight else colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.borderSubtle)
            )
        }
    }
}

@Composable
fun MidnightDesktopSidebar(
    destinations: List<NavDestinationItem>,
    currentRoute: String?,
    onSelect: (NavDestinationItem) -> Unit,
    onOpenQuickSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        color = colors.backgroundSecondary,
        modifier = modifier
            .width(236.dp)
            .fillMaxHeight()
            .testTag("navigation_rail")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                val searchInteraction = remember { MutableInteractionSource() }
                val searchHovered by searchInteraction.collectIsHoveredAsState()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardBackground,
                    border = BorderStroke(1.dp, if (searchHovered) colors.accentPrimary else colors.borderStrong),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .hoverable(searchInteraction)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(interactionSource = searchInteraction, indication = null, onClick = onOpenQuickSearch)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        Text("Quick Jump...", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                destinations.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val interactionSource = remember { MutableInteractionSource() }
                    val hovered by interactionSource.collectIsHoveredAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (hovered) 1.025f else 1f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                        label = "sidebar_item_scale"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = when {
                            selected -> colors.accentPrimary.copy(alpha = 0.16f)
                            hovered -> colors.cardBackground
                            else -> Color.Transparent
                        },
                        animationSpec = tween(AppMotion.FAST_MS),
                        label = "sidebar_item_bg"
                    )

                    Surface(
                        shape = RoundedCornerShape(AppRadius.md),
                        color = bgColor,
                        border = if (selected) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.50f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .hoverable(interactionSource)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clip(RoundedCornerShape(AppRadius.md))
                            .clickable(interactionSource = interactionSource, indication = null) { onSelect(tab) }
                            .testTag("rail_item_${tab.route}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (selected) colors.accentPrimary else colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) colors.highlight else colors.textSecondary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.borderSubtle)
            )
        }
    }
}

@Composable
fun ActionFeedbackBanner(
    feedback: DynamicIslandFeedback?,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    LaunchedEffect(feedback?.id) {
        if (feedback != null) {
            delay(4000L)
            onDismiss()
        }
    }

    AnimatedContent(
        targetState = feedback,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
        label = "feedback_banner",
        modifier = modifier
    ) { active ->
        if (active != null) {
            Surface(
                shape = RoundedCornerShape(AppRadius.md),
                color = colors.cardBackground.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.35f)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                    .testTag("action_feedback_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = colors.success,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = active.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = active.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (active.canUndo) {
                        TextButton(
                            onClick = onUndo,
                            modifier = Modifier.testTag("action_feedback_undo")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accentPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo", color = colors.accentPrimary)
                        }
                    } else {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSearchBottomSheet(
    watchlist: List<AnimeEntity>,
    trending: List<AnimeEntity>,
    titlePreference: String,
    onDismiss: () -> Unit,
    onSelectAnime: (Int) -> Unit,
    onSearchInCatalog: (String) -> Unit
) {
    val colors = AppTheme.colors
    var query by remember { mutableStateOf("") }
    val combinedPool = remember(watchlist, trending) {
        (watchlist + trending).distinctBy { it.id }
    }
    val matches = remember(combinedPool, query, titlePreference) {
        if (query.isBlank()) combinedPool.take(10)
        else combinedPool.filter {
            it.getResolvedTitle(titlePreference).contains(query, ignoreCase = true) ||
                it.genres.contains(query, ignoreCase = true)
        }.take(16)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundSecondary,
        shape = RoundedCornerShape(topStart = AppRadius.hero, topEnd = AppRadius.hero)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Text(
                text = "Quick Search",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            AppSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search watchlist or jump to catalog...",
                modifier = Modifier.testTag("quick_search_input")
            )
            if (query.isNotBlank()) {
                AppPrimaryButton(
                    text = "Search \"$query\" in Discover",
                    onClick = {
                        onDismiss()
                        onSearchInCatalog(query.trim())
                    },
                    leadingIcon = Icons.Filled.Search,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                contentPadding = PaddingValues(vertical = AppSpacing.xxs)
            ) {
                items(matches, key = { it.id }) { item ->
                    val title = item.getResolvedTitle(titlePreference)
                    AppCard(
                        shape = RoundedCornerShape(AppRadius.md),
                        onClick = {
                            onDismiss()
                            onSelectAnime(item.id)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            ResilientPosterImage(
                                imageUrl = item.coverImage,
                                contentDescription = title,
                                shape = RoundedCornerShape(AppRadius.sm),
                                modifier = Modifier.size(width = 40.dp, height = 56.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (item.inWatchlist) item.effectiveProgressDisplay
                                    else listOfNotNull(item.format, item.seasonYear?.toString()).joinToString(" • "),
                                    style = TabularNumSmall,
                                    color = colors.textSecondary
                                )
                            }
                            if (item.inWatchlist) {
                                WatchStatusPill(status = item.watchStatusEnum)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickContextActionSheet(
    anime: AnimeEntity,
    titlePreference: String = "ENGLISH",
    isInUserList: Boolean = anime.inWatchlist,
    onDismiss: () -> Unit,
    onSave: (status: WatchStatus, episodeProgress: Int, seasonNumber: Int, scoreTenths: Int?, isFavorite: Boolean) -> Unit,
    onOpenFullEditor: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val title = anime.getResolvedTitle(titlePreference)
    var selectedStatus by remember(anime) {
        mutableStateOf(if (isInUserList) anime.watchStatusEnum else WatchStatus.WATCHING)
    }
    var progress by remember(anime) { mutableIntStateOf(anime.watchedEpisodeCount) }
    var progressText by remember(anime) { mutableStateOf(anime.watchedEpisodeCount.toString()) }
    var stars by remember(anime) { mutableFloatStateOf(StarRatingUtils.score100ToStars(anime.userScore)) }
    var favorite by remember(anime) { mutableStateOf(anime.isFavorite) }

    val applyEpisode: (Int) -> Unit = { target ->
        val clamped = ProgressRules.clampEpisodeProgress(target, anime.totalEpisodes)
        progress = clamped
        progressText = clamped.toString()
        if (anime.totalEpisodes != null && anime.totalEpisodes > 0 && clamped >= anime.totalEpisodes) {
            selectedStatus = WatchStatus.COMPLETED
        } else if (clamped > 0 && selectedStatus == WatchStatus.PLAN_TO_WATCH) {
            selectedStatus = WatchStatus.WATCHING
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundSecondary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
                .testTag("quick_context_sheet"),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                ResilientPosterImage(
                    imageUrl = anime.coverImage,
                    contentDescription = title,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(width = 58.dp, height = 84.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        fontFamily = CormorantSerifFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(
                            anime.formatLabel(),
                            anime.totalEpisodes?.let { "$it Eps" },
                            anime.seasonYear?.toString()
                        ).joinToString(" • "),
                        style = TabularNumSmall,
                        color = colors.textSecondary
                    )
                }
                IconButton(onClick = { favorite = !favorite }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (favorite) colors.error else colors.textSecondary
                    )
                }
            }

            Text("STATUS", style = MicroUpperLabel, color = colors.accentPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WatchStatus.entries.forEach { st ->
                    val isSelected = selectedStatus == st
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) colors.accentPrimary.copy(alpha = 0.2f) else colors.elevatedSurface.copy(alpha = 0.45f),
                        border = if (isSelected) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.6f)) else null,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                selectedStatus = st
                                if (st == WatchStatus.COMPLETED && (anime.totalEpisodes ?: 0) > 0) {
                                    applyEpisode(anime.totalEpisodes!!)
                                }
                            }
                    ) {
                        Text(
                            text = st.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) colors.highlight else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if ((isInUserList || anime.hasActiveUserState) && onRemove != null) {
                OutlinedButton(
                    onClick = onRemove,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.error.copy(alpha = 0.08f),
                        contentColor = colors.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("context_sheet_remove")
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove from List", fontWeight = FontWeight.SemiBold)
                }
            }

            // Custom Episode Typing + Stepper (- / +)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.elevatedSurface.copy(alpha = 0.52f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EPISODE", style = MicroUpperLabel, color = colors.textSecondary)
                        Text(
                            text = "of ${anime.totalEpisodes?.takeIf { it > 0 }?.toString() ?: "∞"}",
                            style = TabularNumSmall,
                            color = colors.textMuted
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconButton(
                            icon = Icons.Filled.Remove,
                            contentDescription = "Decrease episode",
                            onClick = { applyEpisode(progress - 1) },
                            containerColor = colors.cardBackground,
                            modifier = Modifier.testTag("context_decrement_ep")
                        )
                        OutlinedTextField(
                            value = progressText,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(5)
                                if (digits.isEmpty()) {
                                    progressText = ""
                                    progress = 0
                                } else {
                                    val parsed = digits.toIntOrNull() ?: 0
                                    val clamped = ProgressRules.clampEpisodeProgress(parsed, anime.totalEpisodes)
                                    progress = clamped
                                    progressText = clamped.toString()
                                    if (anime.totalEpisodes != null && anime.totalEpisodes > 0 && clamped >= anime.totalEpisodes) {
                                        selectedStatus = WatchStatus.COMPLETED
                                    } else if (clamped > 0 && selectedStatus == WatchStatus.PLAN_TO_WATCH) {
                                        selectedStatus = WatchStatus.WATCHING
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = TabularNumMedium.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.cardBackground,
                                unfocusedContainerColor = colors.cardBackground,
                                focusedBorderColor = colors.accentPrimary,
                                unfocusedBorderColor = colors.borderSubtle
                            ),
                            modifier = Modifier
                                .width(84.dp)
                                .testTag("context_episode_input")
                        )
                        AppIconButton(
                            icon = Icons.Filled.Add,
                            contentDescription = "Increase episode",
                            onClick = { applyEpisode(progress + 1) },
                            tint = colors.onAccentPrimary,
                            containerColor = colors.accentPrimary,
                            modifier = Modifier.testTag("context_increment_ep")
                        )
                    }
                }
            }

            InteractiveStarRatingSelector(
                stars = stars,
                onStarsChanged = { stars = it },
                onClear = { stars = 0f }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (onOpenFullEditor != null) {
                    AppSecondaryButton(
                        text = "Write Note",
                        onClick = onOpenFullEditor
                    )
                }
                AppPrimaryButton(
                    text = "Save",
                    onClick = {
                        val scoreInt = if (stars > 0f) StarRatingUtils.starsToScore100(stars) else null
                        val finalEp = progressText.toIntOrNull() ?: progress
                        onSave(
                            selectedStatus,
                            ProgressRules.clampEpisodeProgress(finalEp, anime.totalEpisodes),
                            anime.effectiveSeasonNumber,
                            scoreInt,
                            favorite
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("context_sheet_save")
                )
            }
        }
    }
}

fun AnimeEntity.formatLabel(): String {
    return when {
        countryOfOrigin.equals("CN", ignoreCase = true) -> "DONGHUA"
        !format.isNullOrBlank() -> format.uppercase()
        else -> "TV"
    }
}
