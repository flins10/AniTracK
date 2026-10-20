package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.AnimeViewModel

/**
 * Settings Screen: MyAnimeList Account Login and Theme Color customization.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: AnimeViewModel,
    modifier: Modifier = Modifier
) {
    val isMalLoggedIn by viewModel.isMalLoggedIn.collectAsState()
    val malUserProfile by viewModel.malUserProfile.collectAsState()
    val isMalSyncing by viewModel.isMalSyncing.collectAsState()
    val currentTheme by viewModel.themeColor.collectAsState()

    var showMalConfigDialog by remember { mutableStateOf(false) }
    var showPasteCodeDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var pasteCodeInput by remember { mutableStateOf("") }
    var customClientIdInput by remember { mutableStateOf(viewModel.malClientId) }
    var customRedirectUriInput by remember { mutableStateOf(viewModel.malRedirectUri) }
    var selectedLoginTab by remember { mutableIntStateOf(0) }

    val isAniListLoggedIn by viewModel.isAniListLoggedIn.collectAsState()
    val anilistProfile by viewModel.anilistUserProfile.collectAsState()
    val isAniListSyncing by viewModel.isAniListSyncing.collectAsState()
    val isOptionalYouTubeApiEnabled by viewModel.isOptionalYouTubeApiEnabled.collectAsState()
    val youtubeRegionCode by viewModel.youtubeRegionCode.collectAsState()
    val isYouTubeKeyConfigured = viewModel.isYouTubeKeyConfigured

    var showAniListTokenDialog by remember { mutableStateOf(false) }
    var anilistTokenInput by remember { mutableStateOf("") }
    var showYtKeyDialog by remember { mutableStateOf(false) }
    var ytKeyInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val watchlist by viewModel.watchlist.collectAsState()
    val wishlist by viewModel.wishlist.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackupToUri(uri)
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBackupFromUri(uri)
        }
    }

    val apkUpdateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.validateAndInstallUpdateApk(uri)
        }
    }

    val themes = listOf(
        "LIGHT_BLUE" to Pair("Electric Blue", Color(0xFF38BDF8)),
        "OBSIDIAN" to Pair("Deep Obsidian", Color(0xFF94A3B8)),
        "RUBY_RED" to Pair("Ruby Red", Color(0xFFEF4444)),
        "EMERALD_GREEN" to Pair("Emerald Green", Color(0xFF10B981)),
        "SUNSET_AMBER" to Pair("Sunset Amber", Color(0xFFF59E0B))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 96.dp)
            .testTag("settings_screen")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "PREFERENCES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage your MyAnimeList account and personalize theme color",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ========================================================
        // SECTION 1: MYANIMELIST ACCOUNT LOGIN & SYNC (ENCRYPTED)
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    if (isMalLoggedIn) Color(0xFF2E51A2).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Section Title & Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E51A2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MAL",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MyAnimeList Account",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isMalLoggedIn) "Active Account Synchronized" else "Connect your personal MAL account",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isMalLoggedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isMalLoggedIn) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Connected",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Spacer(modifier = Modifier.height(14.dp))

                if (isMalLoggedIn) {
                    // Logged-in profile view
                    val user = malUserProfile
                    val username = user?.name ?: viewModel.connectedMalUsername ?: "MAL Member"

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!user?.picture.isNullOrBlank()) {
                                AsyncImage(
                                    model = user?.picture,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFF2E51A2), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    color = Color(0xFF2E51A2),
                                    shape = CircleShape,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (user?.id != null && user.id > 0) "MAL ID: #${user.id} • Encrypted" else "Account Linked • Encrypted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E51A2)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons when connected
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.fetchMalProfileAndSync(showToast = true) },
                            enabled = !isMalSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E51A2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_mal_pull_button")
                        ) {
                            if (isMalSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sync List",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.syncWatchlistToMal() },
                            enabled = !isMalSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_mal_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Push to MAL",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                customClientIdInput = viewModel.malClientId
                                customRedirectUriInput = viewModel.malRedirectUri
                                showMalConfigDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("API Config", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { showSignOutConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    // Not connected view: User can add their own account
                    TabRow(
                        selectedTabIndex = selectedLoginTab,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedLoginTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedLoginTab == 0,
                            onClick = { selectedLoginTab = 0 },
                            text = {
                                Text(
                                    "Username",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedLoginTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        )
                        Tab(
                            selected = selectedLoginTab == 1,
                            onClick = { selectedLoginTab = 1 },
                            text = {
                                Text(
                                    "OAuth 2.0 PKCE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedLoginTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedLoginTab == 0) {
                        // Quick username connect
                        Column {
                            Text(
                                text = "Enter your MyAnimeList username to instantly connect your public anime list, watch progress, and ratings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                placeholder = { Text("e.g. your_mal_username") },
                                label = { Text("MyAnimeList Username") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_mal_username_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (usernameInput.isNotBlank()) {
                                        viewModel.connectMalAccountByUsername(usernameInput.trim())
                                    }
                                },
                                enabled = !isMalSyncing && usernameInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E51A2)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_mal_connect_username_button")
                            ) {
                                if (isMalSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connecting...", style = MaterialTheme.typography.labelLarge)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Connect Account",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        // OAuth 2.0 PKCE tab
                        Column {
                            Text(
                                text = "Full two-way synchronization using official MyAnimeList OAuth 2.0 with PKCE authorization.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (viewModel.malClientId.isBlank()) {
                                        // Prompt for client ID
                                        showMalConfigDialog = true
                                    } else {
                                        viewModel.startMalOAuthFlow()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E51A2)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_mal_login_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Log In with MyAnimeList",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        customClientIdInput = viewModel.malClientId
                                        customRedirectUriInput = viewModel.malRedirectUri
                                        showMalConfigDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (viewModel.malClientId.isNotBlank()) "Client ID (Configured)" else "Add Client ID",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showPasteCodeDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Paste Code/URL", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========================================================
        // SECTION: ANILIST OAUTH 2.0 ACCOUNT INTEGRATION
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    Color(0xFF0284C7).copy(alpha = 0.35f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("AL", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AniList Integration",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "OAuth2 personal tracking & metadata sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (isAniListLoggedIn) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isAniListLoggedIn) "CONNECTED" else "OPTIONAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isAniListLoggedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAniListLoggedIn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.12f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (anilistProfile?.avatarUrl != null) {
                                AsyncImage(
                                    model = anilistProfile?.avatarUrl,
                                    contentDescription = "AniList Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = anilistProfile?.name ?: "AniList Member",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "User ID: ${anilistProfile?.id ?: "Authorized"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.fetchAniListProfileAndSync() },
                            enabled = !isAniListSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isAniListSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Lists", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.logoutAniList() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Sign Out", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Text(
                        text = "Connect with your official AniList account to synchronize your anime watch progress, scores, and lists automatically across devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val url = viewModel.anilistOAuthManager.getAuthorizationUrl()
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log In on AniList", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = { showAniListTokenDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Paste Token", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========================================================
        // SECTION: YOUTUBE DATA API V3 & VIDEO SEARCH
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    Color(0xFFEF4444).copy(alpha = 0.35f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFEF4444),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "YouTube Discovery & Playback",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isOptionalYouTubeApiEnabled) "Optional Data API v3 Mode" else "Keyless Mode (Default • No Key Needed)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (isOptionalYouTubeApiEnabled) {
                            if (isYouTubeKeyConfigured) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                        } else {
                            Color(0xFF10B981).copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isOptionalYouTubeApiEnabled) {
                                if (isYouTubeKeyConfigured) "OPTIONAL API (ACTIVE)" else "API KEY NEEDED"
                            } else "KEYLESS (ZERO KEY)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isOptionalYouTubeApiEnabled) {
                                    if (isYouTubeKeyConfigured) Color(0xFF10B981) else Color(0xFFF59E0B)
                                } else Color(0xFF10B981)
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Keyless Explanation Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isOptionalYouTubeApiEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color(0xFF10B981).copy(alpha = 0.1f),
                    border = BorderStroke(
                        1.dp,
                        if (isOptionalYouTubeApiEnabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isOptionalYouTubeApiEnabled) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOptionalYouTubeApiEnabled) "Optional Data API is enabled" else "Keyless Mode Active (Recommended)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isOptionalYouTubeApiEnabled) MaterialTheme.colorScheme.onSurface else Color(0xFF10B981)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isOptionalYouTubeApiEnabled) {
                                "Direct YouTube Data API v3 queries are enabled for search feeds. Quota limits apply per your Google Cloud account."
                            } else {
                                "Zero personal API key or configuration required! Search YouTube via official web queries derived from anime titles, and watch verified trailers through the official embedded player without API quotas or personal credentials."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle for Optional YouTube Data API v3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Optional YouTube Data API",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Disabled by default. Only enable if you want to use your personal YouTube Data API v3 key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isOptionalYouTubeApiEnabled,
                        onCheckedChange = { viewModel.setOptionalYouTubeApiEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFEF4444),
                            checkedTrackColor = Color(0xFFEF4444).copy(alpha = 0.4f)
                        )
                    )
                }

                // If Optional API is enabled, show key configuration
                if (isOptionalYouTubeApiEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Personal YouTube API Key (Optional)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isYouTubeKeyConfigured) "Custom key is active on this device" else "No key configured. Add your key or switch back to Keyless Mode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showYtKeyDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isYouTubeKeyConfigured) "Edit Key" else "Add Key", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                // Default Region Code Selector
                Text(
                    text = "SEARCH REGION PREFERENCE (CURRENT: $youtubeRegionCode)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Applies regional licensing preference (Default: IN - India) for queries and official anime distributors (e.g. Muse Asia, Ani-One).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                val regions = listOf("IN" to "India (IN)", "JP" to "Japan (JP)", "US" to "USA (US)", "GB" to "UK (GB)", "CA" to "Canada (CA)")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    regions.forEach { (code, label) ->
                        val isSelected = youtubeRegionCode == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setYouTubeRegionCode(code) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFFEF4444)
                            )
                        )
                    }
                }
            }
        }

        // ========================================================
        // SECTION 2: THEME COLOR CUSTOMIZATION
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Theme Color",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose the accent color and ambient glow for the app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    themes.forEach { (key, pair) ->
                        val (label, color) = pair
                        val isSelected = currentTheme == key
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.setThemeColor(key) }
                                .testTag("settings_theme_$key")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========================================================
        // SECTION 3: DATA PERSISTENCE, BACKUP & SEAMLESS UPDATES
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Saved Anime Persistence & Updates",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Durable SQLite storage • ${watchlist.size} Watchlist • ${wishlist.size} Wishlist • ${favorites.size} Favorites",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Saved anime, watchlists, favorites, watch history, and player preferences are permanently stored in versioned SQLite tables with non-destructive migrations. Clearing cache never removes saved entries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            exportBackupLauncher.launch("anitrack_backup_${System.currentTimeMillis() / 1000}.json")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_backup_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export Backup", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_backup_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Import Backup", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.clearMetadataAndApiCache() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_cache_safe_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear API & Image Cache (Keep Saved Anime)")
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdateAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Seamless In-Place APK Update Verifier",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val diagSummary = remember { viewModel.getSigningDiagnosticsSummary() }
                Text(
                    text = diagSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        apkUpdateLauncher.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_install_apk_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify & Install Downloaded Update APK")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lauma signature
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Lauma",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ========================================================
    // DIALOGS
    // ========================================================

    // Custom Client ID Dialog
    if (showMalConfigDialog) {
        AlertDialog(
            onDismissRequest = { showMalConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Your MAL Client ID")
                }
            },
            text = {
                Column {
                    Text(
                        text = "To log in via OAuth 2.0 PKCE, enter your own MyAnimeList Client ID (obtainable for free at myanimelist.net/apiconfig with App Type: Android).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Client ID:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customClientIdInput,
                        onValueChange = { customClientIdInput = it },
                        placeholder = { Text("Enter your 32-character Client ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Redirect URI:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customRedirectUriInput,
                        onValueChange = { customRedirectUriInput = it },
                        placeholder = { Text("anitrack://mal-callback") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomMalConfig(
                            customClientIdInput.takeIf { it.isNotBlank() },
                            customRedirectUriInput.takeIf { it.isNotBlank() }
                        )
                        showMalConfigDialog = false
                    }
                ) {
                    Text("Save Configuration")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMalConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Paste OAuth Code / URL Dialog
    if (showPasteCodeDialog) {
        AlertDialog(
            onDismissRequest = { showPasteCodeDialog = false },
            title = { Text("Paste Authorization Code or URL") },
            text = {
                Column {
                    Text(
                        text = "If your browser did not automatically redirect back to the app, copy the URL from your browser address bar or the 'code' parameter and paste it below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pasteCodeInput,
                        onValueChange = { pasteCodeInput = it },
                        placeholder = { Text("anitrack://mal-callback?code=... or code") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = pasteCodeInput.trim()
                        if (input.isNotBlank()) {
                            showPasteCodeDialog = false
                            viewModel.handleMalOAuthCallback(input)
                            pasteCodeInput = ""
                        }
                    }
                ) {
                    Text("Submit Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteCodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sign out confirmation dialog
    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = { Text("Disconnect Account?") },
            text = {
                Text("Are you sure you want to disconnect your MyAnimeList account? Stored account credentials and session tokens will be removed from this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logoutMal()
                        showSignOutConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AniList Token Dialog
    if (showAniListTokenDialog) {
        AlertDialog(
            onDismissRequest = { showAniListTokenDialog = false },
            title = { Text("Enter AniList Token") },
            text = {
                Column {
                    Text(
                        text = "Log in to AniList in your browser, copy your token or authorization PIN, and paste it below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = anilistTokenInput,
                        onValueChange = { anilistTokenInput = it },
                        placeholder = { Text("Bearer eyJhbGci...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val token = anilistTokenInput.trim()
                        if (token.isNotBlank()) {
                            viewModel.handleAniListToken(token)
                            showAniListTokenDialog = false
                            anilistTokenInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Save Token")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAniListTokenDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // YouTube API Key Dialog
    if (showYtKeyDialog) {
        AlertDialog(
            onDismissRequest = { showYtKeyDialog = false },
            title = { Text("Optional YouTube API Key") },
            text = {
                Column {
                    Text(
                        text = "The app works completely without any API key in Keyless Mode! If you wish to use the optional YouTube Data API v3 integration, you can provide your personal Google Cloud API key here. It is stored securely and only on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ytKeyInput,
                        onValueChange = { ytKeyInput = it },
                        placeholder = { Text("AIzaSy...") },
                        label = { Text("YouTube API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (viewModel.isYouTubeKeyConfigured) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setCustomYouTubeApiKey(null)
                                showYtKeyDialog = false
                                ytKeyInput = ""
                            }
                        ) {
                            Text("Reset")
                        }
                    }
                    Button(
                        onClick = {
                            val key = ytKeyInput.trim()
                            if (key.isNotBlank()) {
                                viewModel.setCustomYouTubeApiKey(key)
                                showYtKeyDialog = false
                                ytKeyInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Save Key")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showYtKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
