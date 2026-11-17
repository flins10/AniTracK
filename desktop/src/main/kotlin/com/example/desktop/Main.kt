package com.example.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

val ObsidianBlack = Color(0xFF0C0E14)
val ObsidianSurface = Color(0xFF131620)
val ObsidianSurface2 = Color(0xFF1A1E2B)
val ChampagneGold = Color(0xFFE6C586)
val ChampagneGoldBright = Color(0xFFF6E6C4)
val IvoryText = Color(0xFFF4F6FA)
val AshText = Color(0xFFB9C0CF)
val HairlineSubtle = Color(0x22FFFFFF)

fun main() = application {
    val windowState = rememberWindowState(width = 1320.dp, height = 820.dp)
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Anime Universe — PC Edition"
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = ChampagneGold,
                background = ObsidianBlack,
                surface = ObsidianSurface,
                onBackground = IvoryText,
                onSurface = IvoryText
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack)
            ) {
                var currentTab by remember { mutableStateOf("home") }
                var userSession by remember { mutableStateOf<String?>(null) }
                var username by remember { mutableStateOf("Traveler") }
                var showAuthModal by remember { mutableStateOf(false) }

                Row(modifier = Modifier.fillMaxSize()) {
                    // Desktop Sidebar
                    Surface(
                        color = ObsidianSurface,
                        modifier = Modifier.width(260.dp).fillMaxFileHeightIfPossible()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.linearGradient(listOf(ChampagneGold, ChampagneGoldBright))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("A", color = ObsidianBlack, fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    }
                                    Column {
                                        Text("ANIME UNIVERSE", color = IvoryText, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                        Text("PC Edition v2.1.0", color = AshText, fontSize = 11.sp)
                                    }
                                }

                                HorizontalDivider(color = HairlineSubtle)

                                SidebarButton("Home", Icons.Filled.Home, currentTab == "home") { currentTab = "home" }
                                SidebarButton("Explore", Icons.Filled.Explore, currentTab == "discover") { currentTab = "discover" }
                                SidebarButton("Library", Icons.Filled.Bookmarks, currentTab == "mylist") { currentTab = "mylist" }
                                SidebarButton("Community", Icons.Filled.Group, currentTab == "community") { currentTab = "community" }
                            }

                            // Footer profile / auth
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianSurface2,
                                border = BorderStroke(1.dp, HairlineSubtle),
                                modifier = Modifier.fillMaxWidth().clickable { showAuthModal = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(ChampagneGold.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(username, color = IvoryText, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                        Text(if (userSession != null) "Cloud Synced" else "Sign In / Guest", color = AshText, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Main Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(ObsidianBlack)
                            .padding(28.dp)
                    ) {
                        when (currentTab) {
                            "home" -> DesktopHomeScreen()
                            "discover" -> DesktopDiscoverScreen()
                            "mylist" -> DesktopLibraryScreen()
                            "community" -> DesktopCommunityScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.fillMaxFileHeightIfPossible(): Modifier = this.fillMaxHeight()

@Composable
fun SidebarButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ChampagneGold.copy(alpha = 0.15f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f)) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) ChampagneGold else AshText, modifier = Modifier.size(20.dp))
            Text(title, color = if (selected) ChampagneGoldBright else IvoryText, fontSize = 14.sp, fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium)
        }
    }
}

@Composable
fun DesktopHomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Welcome to Anime Universe", color = IvoryText, fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text("Track your favorite anime, donghua, and release schedules on PC.", color = AshText, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, HairlineSubtle),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) {
            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.CenterStart) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Weekly Airing Schedule", color = ChampagneGold, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Text("Synchronized with AniList GraphQL & Supabase Cloud.", color = AshText, fontSize = 13.sp)
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack)
                    ) {
                        Text("Explore Catalog", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopDiscoverScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Explore Catalog", color = IvoryText, fontSize = 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Search top anime & donghua...", color = AshText, fontSize = 14.sp)
    }
}

@Composable
fun DesktopLibraryScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("My Library & Watchlist", color = IvoryText, fontSize = 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your saved watchlists and custom collections.", color = AshText, fontSize = 14.sp)
    }
}

@Composable
fun DesktopCommunityScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Community & Reviews", color = IvoryText, fontSize = 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Share reviews and connect with anime fans.", color = AshText, fontSize = 14.sp)
    }
}
