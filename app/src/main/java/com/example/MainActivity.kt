package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AnimeDetailScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StreamingPlayerScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.WeeklyAnimePlannerTheme
import com.example.viewmodel.AnimeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Schedule("schedule", "Planner", Icons.Filled.Tv, Icons.Outlined.Tv),
    Search("search", "Discover", Icons.Filled.Search, Icons.Outlined.Search),
    Wishlist("wishlist", "Watchlist", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    Settings("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: AnimeViewModel by viewModels()

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)

        // Schedule 30-minute background sync WorkManager job
        try {
            val syncWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.ScheduleUpdateWorker>(
                30, java.util.concurrent.TimeUnit.MINUTES
            ).build()
            androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "anitrack_schedule_sync_work",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        } catch (e: Exception) {
            // WorkManager setup fallback
        }

        setContent {
            val currentThemeColor by viewModel.themeColor.collectAsState()
            WeeklyAnimePlannerTheme(themeName = currentThemeColor) {
                // Controlled splash state - preserved across configuration changes
                var showSplash by rememberSaveable { mutableStateOf(true) }
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val userMessage by viewModel.userMessage.collectAsState()

                LaunchedEffect(userMessage, showSplash) {
                    if (!showSplash) {
                        userMessage?.let { msg ->
                            snackbarHostState.showSnackbar(msg)
                            viewModel.clearUserMessage()
                        }
                    } else if (userMessage != null) {
                        viewModel.clearUserMessage()
                    }
                }

                val configuration = LocalConfiguration.current
                val isWideScreen = configuration.screenWidthDp >= 600
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute in NavigationItem.entries.map { it.route }

                // Manage edge-to-edge system appearance
                val context = LocalContext.current
                val window = (context as? Activity)?.window
                DisposableEffect(window) {
                    if (window != null) {
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                    }
                    onDispose {}
                }

                var isBarsVisible by remember { mutableStateOf(true) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF060913),
                                    Color(0xFF0D1224),
                                    Color(0xFF080B18),
                                    Color(0xFF050711)
                                )
                            )
                        )
                ) {
                    // Cosmic Backdrop: Gradient Nebulae, Stars, Cyber Grid Lines & Orbit Curves
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Deep Indigo & Purple Nebulae
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF0284C7).copy(alpha = 0.14f), Color(0xFF0284C7).copy(alpha = 0.03f), Color.Transparent),
                                center = Offset(w * 0.82f, h * 0.12f),
                                radius = w * 0.90f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.12f), Color(0xFF6366F1).copy(alpha = 0.03f), Color.Transparent),
                                center = Offset(w * 0.18f, h * 0.55f),
                                radius = w * 0.80f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.10f), Color.Transparent),
                                center = Offset(w * 0.70f, h * 0.88f),
                                radius = w * 0.70f
                            )
                        )

                        // Subtle Glowing Cyber Orbit Arc Lines
                        drawArc(
                            color = Color(0xFF38BDF8).copy(alpha = 0.08f),
                            startAngle = 160f,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(-w * 0.4f, h * 0.2f),
                            size = androidx.compose.ui.geometry.Size(w * 1.8f, h * 0.5f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                        )
                        drawArc(
                            color = Color(0xFFA855F7).copy(alpha = 0.06f),
                            startAngle = 10f,
                            sweepAngle = 120f,
                            useCenter = false,
                            topLeft = Offset(w * 0.1f, h * 0.4f),
                            size = androidx.compose.ui.geometry.Size(w * 1.4f, h * 0.6f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )

                        // Stardust & Glowing Cosmic Stars
                        val starPositions = listOf(
                            Offset(w * 0.12f, h * 0.08f) to 2.5f,
                            Offset(w * 0.88f, h * 0.18f) to 3.0f,
                            Offset(w * 0.35f, h * 0.25f) to 2.0f,
                            Offset(w * 0.75f, h * 0.42f) to 2.8f,
                            Offset(w * 0.08f, h * 0.68f) to 2.2f,
                            Offset(w * 0.92f, h * 0.78f) to 3.2f,
                            Offset(w * 0.48f, h * 0.85f) to 2.0f
                        )
                        starPositions.forEach { (pos, radius) ->
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = radius,
                                center = pos
                            )
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                                radius = radius * 2.2f,
                                center = pos
                            )
                        }
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            AnimatedVisibility(
                                visible = showBottomBar && isBarsVisible && !isWideScreen,
                                enter = androidx.compose.animation.slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 220)),
                                exit = androidx.compose.animation.slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 180))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 22.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Ambient Frosted Neon Halo Glow behind the dock
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(28.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF38BDF8).copy(alpha = 0.22f),
                                                        Color(0xFF06B6D4).copy(alpha = 0.28f),
                                                        Color(0xFF818CF8).copy(alpha = 0.20f),
                                                        Color(0xFF38BDF8).copy(alpha = 0.22f)
                                                    )
                                                )
                                            )
                                            .blur(16.dp)
                                    )

                                    // Floating Slim Frosted-Glass Navigation Bar
                                    Surface(
                                        shape = RoundedCornerShape(28.dp),
                                        color = Color(0xFF080D1A).copy(alpha = 0.78f),
                                        border = BorderStroke(
                                            1.2.dp,
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF38BDF8).copy(alpha = 0.90f),
                                                    Color(0xFF06B6D4).copy(alpha = 0.95f),
                                                    Color.White.copy(alpha = 0.50f),
                                                    Color(0xFF818CF8).copy(alpha = 0.70f),
                                                    Color(0xFF38BDF8).copy(alpha = 0.90f)
                                                )
                                            )
                                        ),
                                        shadowElevation = 14.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("bottom_navigation_bar")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color.White.copy(alpha = 0.15f),
                                                            Color.White.copy(alpha = 0.03f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                NavigationItem.entries.forEach { item ->
                                                    val isSelected = currentRoute == item.route
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clickable {
                                                                if (currentRoute != item.route) {
                                                                    navController.navigate(item.route) {
                                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                                            saveState = true
                                                                        }
                                                                        launchSingleTop = true
                                                                        restoreState = true
                                                                    }
                                                                }
                                                            }
                                                            .testTag("nav_item_${item.route}")
                                                    ) {
                                                        if (isSelected) {
                                                            // Active Tab: Glowing Cyan-Blue Frosted Capsule Circle with White Specular Edge
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(38.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        Brush.linearGradient(
                                                                            listOf(
                                                                                Color(0xFF06B6D4),
                                                                                Color(0xFF0284C7)
                                                                            )
                                                                        )
                                                                    )
                                                                    .border(
                                                                        1.2.dp,
                                                                        Brush.verticalGradient(
                                                                            listOf(
                                                                                Color.White.copy(alpha = 0.90f),
                                                                                Color(0xFF38BDF8).copy(alpha = 0.60f)
                                                                            )
                                                                        ),
                                                                        CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = item.selectedIcon,
                                                                    contentDescription = item.title,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        } else {
                                                            // Inactive Tab: Pure Muted Silver-Gray Symbol, No Text
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .clip(CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = item.unselectedIcon,
                                                                    contentDescription = item.title,
                                                                    tint = Color(0xFF94A3B8),
                                                                    modifier = Modifier.size(22.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Delicate Sparkle Effect on the right edge above settings
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8).copy(alpha = 0.75f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 2.dp, end = 26.dp)
                                            .size(12.dp)
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = innerPadding.calculateTopPadding())
                        ) {
                            if (showBottomBar && isWideScreen) {
                                NavigationRail(
                                    containerColor = Color(0xFF0B0F1B).copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .testTag("navigation_rail")
                                        .border(0.8.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    NavigationItem.entries.forEach { item ->
                                        val isSelected = currentRoute == item.route
                                        NavigationRailItem(
                                            selected = isSelected,
                                            onClick = {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = item.title
                                                )
                                            },
                                            label = { Text(text = item.title) },
                                            modifier = Modifier.testTag("rail_item_${item.route}")
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                NavHost(
                                    navController = navController,
                                    startDestination = NavigationItem.Schedule.route
                                ) { 
                                    composable(NavigationItem.Schedule.route) {
                                        ScheduleScreen(
                                            viewModel = viewModel,
                                            onNavigateToSearch = { navController.navigate(NavigationItem.Search.route) },
                                            onSelectAnime = { id ->
                                                viewModel.selectAnimeDetail(id)
                                                navController.navigate("detail/$id")
                                            },
                                            onBarsVisibilityChange = { isBarsVisible = it },
                                            onOpenPlayer = { id, ep -> navController.navigate("player/$id/$ep") }
                                        )
                                    }

                                    composable(NavigationItem.Search.route) {
                                        SearchScreen(
                                            viewModel = viewModel,
                                            onSelectAnime = { id ->
                                                viewModel.selectAnimeDetail(id)
                                                navController.navigate("detail/$id")
                                            },
                                            onBarsVisibilityChange = { isBarsVisible = it },
                                            onOpenPlayer = { id, ep -> navController.navigate("player/$id/$ep") }
                                        )
                                    }

                                    composable(NavigationItem.Wishlist.route) {
                                        WishlistScreen(
                                            viewModel = viewModel,
                                            onNavigateToSearch = { navController.navigate(NavigationItem.Search.route) },
                                            onSelectAnime = { id ->
                                                viewModel.selectAnimeDetail(id)
                                                navController.navigate("detail/$id")
                                            },
                                            onBarsVisibilityChange = { isBarsVisible = it },
                                            onOpenPlayer = { id, ep -> navController.navigate("player/$id/$ep") }
                                        )
                                    }

                                    composable("calendar") {
                                        CalendarScreen(
                                            viewModel = viewModel,
                                            onNavigateToSearch = { navController.navigate(NavigationItem.Search.route) },
                                            onSelectAnime = { id ->
                                                viewModel.selectAnimeDetail(id)
                                                navController.navigate("detail/$id")
                                            },
                                            onBarsVisibilityChange = { isBarsVisible = it },
                                            onOpenPlayer = { id, ep -> navController.navigate("player/$id/$ep") }
                                        )
                                    }

                                    composable(NavigationItem.Settings.route) {
                                        SettingsScreen(
                                            viewModel = viewModel
                                        )
                                    }

                                    composable(
                                        route = "detail/{animeId}",
                                        arguments = listOf(navArgument("animeId") { type = NavType.IntType })
                                    ) { backStackEntry ->
                                        val animeId = backStackEntry.arguments?.getInt("animeId") ?: 0
                                        AnimeDetailScreen(
                                            animeId = animeId,
                                            viewModel = viewModel,
                                            onBack = { navController.popBackStack() },
                                            onOpenPlayer = { id, ep, season -> navController.navigate("player/$id/$ep?seasonNumber=$season") }
                                        )
                                    }

                                    composable(
                                        route = "player/{animeId}/{episodeNumber}?seasonNumber={seasonNumber}",
                                        arguments = listOf(
                                            navArgument("animeId") { type = NavType.IntType },
                                            navArgument("episodeNumber") { type = NavType.IntType },
                                            navArgument("seasonNumber") { type = NavType.IntType; defaultValue = 1 }
                                        )
                                    ) { backStackEntry ->
                                        val animeId = backStackEntry.arguments?.getInt("animeId") ?: 0
                                        val episodeNumber = backStackEntry.arguments?.getInt("episodeNumber") ?: 1
                                        val seasonNumber = backStackEntry.arguments?.getInt("seasonNumber") ?: 1
                                        StreamingPlayerScreen(
                                            animeId = animeId,
                                            initialEpisode = episodeNumber,
                                            initialSeason = seasonNumber,
                                            viewModel = viewModel,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(400))
                    ) {
                        CinematicSplashOverlay(
                            onSplashFinished = { showSplash = false }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        android.util.Log.d("MainActivity", "handleDeepLinkIntent received uri: $uri")
        if (uri.scheme == "anitrack") {
            val error = uri.getQueryParameter("error")
            if (!error.isNullOrBlank()) {
                val errorDescription = uri.getQueryParameter("error_description") ?: error
                android.util.Log.e("MainActivity", "OAuth error from MAL redirect: $errorDescription")
                viewModel.setAuthErrorMessage("MyAnimeList login error: $errorDescription")
                return
            }
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                viewModel.handleMalOAuthCallback(code)
            } else {
                val fullUri = uri.toString()
                if (fullUri.contains("code=")) {
                    viewModel.handleMalOAuthCallback(fullUri)
                }
            }
        }
    }
}

@Composable
fun CinematicSplashOverlay(onSplashFinished: () -> Unit) {
    // Pure typography animations - 60/120fps hardware accelerated
    val containerAlpha = remember { Animatable(1f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleScale = remember { Animatable(0.88f) }
    val titleOffsetY = remember { Animatable(24f) }
    val dividerWidth = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val shimmerProgress = remember { Animatable(-300f) }

    LaunchedEffect(Unit) {
        val smoothDecel = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

        // Phase 1: Title smooth entrance
        launch {
            titleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = LinearEasing)
            )
        }
        launch {
            titleScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750, easing = smoothDecel)
            )
        }
        launch {
            titleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 750, easing = smoothDecel)
            )
        }

        // Phase 2: Elegant divider line expands & subtitle reveals
        delay(250)
        launch {
            dividerWidth.animateTo(
                targetValue = 72f,
                animationSpec = tween(durationMillis = 550, easing = smoothDecel)
            )
        }
        launch {
            subtitleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )
        }

        // Phase 3: Pristine light sweep reflection across typography
        shimmerProgress.animateTo(
            targetValue = 800f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )

        // Phase 4: Brief hold for visual impact
        delay(300)

        // Phase 5: Buttery smooth exit transition
        val exitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
        containerAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 380, easing = exitEasing)
        )
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = containerAlpha.value
            }
            .background(Color(0xFF090A10)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Main Typographic Wordmark
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    translationY = titleOffsetY.value
                    alpha = titleAlpha.value
                    scaleX = titleScale.value
                    scaleY = titleScale.value
                }
            ) {
                // Subtle back-glow aura
                Text(
                    text = "ANITRAK",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 46.sp,
                        letterSpacing = 10.sp,
                        color = Color(0xFF6366F1).copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1.08f
                        scaleY = 1.08f
                    }
                )

                // High-End Shimmer Gradient Typography
                val textGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE2E8F0),
                        Color(0xFF818CF8),
                        Color(0xFFC084FC),
                        Color(0xFFFFFFFF)
                    ),
                    start = Offset(shimmerProgress.value - 250f, 0f),
                    end = Offset(shimmerProgress.value + 50f, 100f)
                )

                Text(
                    text = "ANITRAK",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 46.sp,
                        letterSpacing = 10.sp,
                        brush = textGradient
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Minimalist Glowing Gradient Accent Line
            Box(
                modifier = Modifier
                    .width(dividerWidth.value.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF818CF8),
                                Color(0xFFC084FC),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Minimalist Tagline
            Text(
                text = "ANIME SCHEDULE & TRACKER",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 5.sp,
                    color = Color(0xFF94A3B8)
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = subtitleAlpha.value
                }
            )
        }
    }
}
