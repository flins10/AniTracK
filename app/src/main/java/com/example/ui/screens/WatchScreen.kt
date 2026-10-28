package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import com.example.data.remote.MovieBoxItem
import com.example.data.repository.ArchivePlayableMedia
import com.example.data.repository.ArchivePlayableMediaResult
import com.example.data.repository.ArchiveSearchRepository
import com.example.data.repository.InternetArchiveRepository
import com.example.data.repository.MovieBoxDownloadManager
import com.example.data.repository.MovieBoxRepository
import com.example.data.repository.PlaybackRepository
import com.example.ui.components.ArchiveDirectPlayerDialog
import com.example.ui.components.EpisodeGridSelector
import com.example.ui.components.MovieBoxDownloadsSheet
import com.example.ui.components.RecentLocalVideosSheet
import com.example.viewmodel.AnimeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cinema category for real-world live-action Movies & Series in the Watch Catalogue.
 */
enum class CinemaOrigin(val label: String) {
    BOLLYWOOD("Bollywood"),
    HOLLYWOOD("Hollywood")
}

/**
 * Model representing a real-world live-action movie or series in the Watch Catalogue.
 * Strictly excludes animation/anime and uses original theatrical posters.
 */
data class WatchCatalogueItem(
    val identifier: String,
    val title: String,
    val isSeries: Boolean,
    val genre: String,
    val origin: CinemaOrigin = CinemaOrigin.BOLLYWOOD,
    val releaseYear: String = "2024",
    val cast: String = "",
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val rating: String = "8.2",
    val director: String = "",
    val movieBoxId: Int = 0,
    val totalSeasons: Int = 1,
    val totalEpisodes: Int = 1,
    val runtimeMinutes: Int? = null,
    val isBookmarked: Boolean = false
)

private fun MovieBoxItem.toWatchCatalogueItem(): WatchCatalogueItem {
    val primaryGenre = genreArray.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: genre.substringBefore(",").trim().ifBlank { "Action" }
    val resolvedOrigin = if (language.equals("HI", ignoreCase = true) || language.equals("PA", ignoreCase = true)) {
        CinemaOrigin.BOLLYWOOD
    } else {
        CinemaOrigin.HOLLYWOOD
    }
    return WatchCatalogueItem(
        identifier = archiveIdentifier?.takeIf { it.isNotBlank() }
            ?: cleanTitle.lowercase().replace(Regex("[^a-z0-9]+"), "-"),
        title = cleanTitle,
        isSeries = isSeries,
        genre = primaryGenre,
        origin = resolvedOrigin,
        releaseYear = year.ifBlank { "2024" },
        cast = cast.joinToString(", ") { it.name },
        description = overview,
        posterUrl = poster,
        backdropUrl = backdrop.ifBlank { poster },
        rating = rating.ifBlank { "8.0" },
        director = director,
        movieBoxId = id,
        totalSeasons = totalSeasons,
        totalEpisodes = totalEpisodes,
        runtimeMinutes = runtime
    )
}

// Live-action genres only (no Animation or Anime in Watch Catalogue)
private val WATCH_GENRES = listOf(
    "All Genres",
    "Action",
    "Adventure",
    "Biography",
    "Comedy",
    "Crime",
    "Documentary",
    "Drama",
    "Fantasy",
    "Historical",
    "Horror",
    "Mystery",
    "Romance",
    "Sci-Fi",
    "Thriller"
)

/**
 * Curated real-world Bollywood & Hollywood live-action Movies and Series
 * with official original theatrical posters and real Internet Archive identifiers.
 */
private val CURATED_MOVIES_AND_SERIES = listOf(
    WatchCatalogueItem(
        identifier = "3-idiots_202108",
        title = "3 Idiots",
        isSeries = false,
        genre = "Comedy",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2009",
        cast = "Aamir Khan, R. Madhavan, Sharman Joshi",
        description = "Two friends are searching for their long lost companion. They revisit their college days and recall the memories of their friend who inspired them to think differently.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BNzc4ZWQ3NmYtODE0Ny00YTQ4LTlkZWItNTBkMGQ0MmUwMmJlXkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 170
    ),
    WatchCatalogueItem(
        identifier = "dangal-hindi-educational-movie-hd",
        title = "Dangal",
        isSeries = false,
        genre = "Biography",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2016",
        cast = "Aamir Khan, Sakshi Tanwar, Fatima Sana Shaikh",
        description = "Former wrestler Mahavir Singh Phogat and his two wrestler daughters struggle towards glory at the Commonwealth Games in the face of societal oppression.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMTQ4MzQzMzM2Nl5BMl5BanBnXkFtZTgwMTQ1NzU3MDI@._V1_.jpg",
        runtimeMinutes = 161
    ),
    WatchCatalogueItem(
        identifier = "Interstellar_2014",
        title = "Interstellar",
        isSeries = false,
        genre = "Sci-Fi",
        origin = CinemaOrigin.HOLLYWOOD,
        releaseYear = "2014",
        cast = "Matthew McConaughey, Anne Hathaway, Jessica Chastain",
        description = "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot, Joseph Cooper, is tasked to pilot a spacecraft, along with a team of researchers, to find a new planet for humans.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 169
    ),
    WatchCatalogueItem(
        identifier = "byomkesh-bakshi-all-episodes",
        title = "Byomkesh Bakshi",
        isSeries = true,
        genre = "Mystery",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "1993",
        cast = "Rajit Kapur, K.K. Raina, Sukanya Kulkarni",
        description = "Sharp and intelligent detective Byomkesh Bakshi and his chronicler friend Ajit solve baffling murder mysteries and intricate crimes in Kolkata.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BZjM3YjE2N2UtM2E4MC00NDg4LTk1MzgtY2I4M2U2ZTg4YjE0XkEyXkFqcGc@._V1_.jpg",
        totalSeasons = 2,
        totalEpisodes = 32
    ),
    WatchCatalogueItem(
        identifier = "gangs-of-wasseypur-part-1_202206",
        title = "Gangs of Wasseypur",
        isSeries = false,
        genre = "Crime",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2012",
        cast = "Manoj Bajpayee, Nawazuddin Siddiqui, Richa Chadha",
        description = "A clash between Sultan and Shahid Khan leads to the expulsion of Khan from Wasseypur, and ignites a deadly blood feud spanning three generations.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BNWQyMmJkYTItNDUyYS00MjgwLWFmYjUtOWQ1NmRjOGZlOTZhXkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 160
    ),
    WatchCatalogueItem(
        identifier = "TheDarkKnightRises_201810",
        title = "The Dark Knight",
        isSeries = false,
        genre = "Action",
        origin = CinemaOrigin.HOLLYWOOD,
        releaseYear = "2008",
        cast = "Christian Bale, Heath Ledger, Aaron Eckhart",
        description = "When a menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman, James Gordon and Harvey Dent must work together to put an end to the madness.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMTMxNTMwODM0NF5BMl5BanBnXkFtZTcwODAyMTk2Mw@@._V1_.jpg",
        runtimeMinutes = 152
    ),
    WatchCatalogueItem(
        identifier = "Lagaan2001",
        title = "Lagaan",
        isSeries = false,
        genre = "Drama",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2001",
        cast = "Aamir Khan, Gracy Singh, Rachel Shelley",
        description = "The people of a small village in Victorian India stake their future on a game of cricket against their ruthless British rulers.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BM2FmODM4OTktOTRjOS00ZTIzLWIzZjAtMDBhOGEzYThkNzMzXkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 224
    ),
    WatchCatalogueItem(
        identifier = "DekhBhaiDekh",
        title = "Dekh Bhai Dekh",
        isSeries = true,
        genre = "Comedy",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "1993",
        cast = "Shekhar Suman, Navin Nischol, Farida Jalal",
        description = "Three generations of the eccentric Diwan family live together in an ancestral bungalow, navigating hilarious misunderstandings and family bonds.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMmI2N2I5M2QtN2I3Zi00Y2Q0LWI0N2MtM2I4Y2I5ZGI3ZGY1XkEyXkFqcGc@._V1_.jpg",
        totalSeasons = 1,
        totalEpisodes = 65
    ),
    WatchCatalogueItem(
        identifier = "sholay-1975_202204",
        title = "Sholay",
        isSeries = false,
        genre = "Action",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "1975",
        cast = "Dharmendra, Amitabh Bachchan, Sanjeev Kumar",
        description = "After his family is murdered by a notorious and ruthless bandit, a former police officer enlists the services of two outlaws to capture him.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMmI1NTRmZTQtM2I2YS00YjA4LTk4ZTItM2I4Y2I5ZGI3ZGY1XkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 204
    ),
    WatchCatalogueItem(
        identifier = "Fight_Club_1999",
        title = "Fight Club",
        isSeries = false,
        genre = "Thriller",
        origin = CinemaOrigin.HOLLYWOOD,
        releaseYear = "1999",
        cast = "Brad Pitt, Edward Norton, Helena Bonham Carter",
        description = "An insomniac office worker and a devil-may-care soap maker form an underground fight club that evolves into much more.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BOTgyOGQ1NDItNGU3Ny00MjU3LTg2YWEtNmEyYjBiMjI1Y2M5XkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 139
    ),
    WatchCatalogueItem(
        identifier = "taare-zameen-par-2007",
        title = "Taare Zameen Par",
        isSeries = false,
        genre = "Drama",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2007",
        cast = "Darsheel Safary, Aamir Khan, Tisca Chopra",
        description = "An eight-year-old boy is thought to be a lazy trouble-maker, until the new art teacher has the patience and compassion to discover the real problem behind his struggles in school.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMDhjZWViN2MtNzgxOS00NmI4LThiZDQtZDI3MzM4MDE4NTc0XkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 165
    ),
    WatchCatalogueItem(
        identifier = "TheGodfather1972",
        title = "The Godfather",
        isSeries = false,
        genre = "Crime",
        origin = CinemaOrigin.HOLLYWOOD,
        releaseYear = "1972",
        cast = "Marlon Brando, Al Pacino, James Caan",
        description = "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant youngest son.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BNGEwYjgwOGQtYjg5ZS00Njc1LTk2ZGEtM2QwZWQ2NjdhZTE5XkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 175
    ),
    WatchCatalogueItem(
        identifier = "BharatEkKhoj",
        title = "Bharat Ek Khoj",
        isSeries = true,
        genre = "Historical",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "1988",
        cast = "Roshan Seth, Om Puri, Naseeruddin Shah",
        description = "Shyam Benegal's landmark historical drama series tracing the 5,000-year history of India from the Indus Valley Civilization to independence.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BN2EyODc1MDAtNTg0ZC00MjRhLTg1NzctM2NjYTlmOGMwYWNiXkEyXkFqcGc@._V1_.jpg",
        totalSeasons = 1,
        totalEpisodes = 53
    ),
    WatchCatalogueItem(
        identifier = "jawan-2023",
        title = "Jawan",
        isSeries = false,
        genre = "Action",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2023",
        cast = "Shah Rukh Khan, Nayanthara, Vijay Sethupathi",
        description = "A high-octane action thriller outlining the emotional journey of a man who is set to rectify the wrongs in the society.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMGExNGI1NDktOWI2Mi00NDM3LWIxMmQtNTQxYTgzMzI0MTA1XkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 169
    ),
    WatchCatalogueItem(
        identifier = "mirzapur-series",
        title = "Mirzapur",
        isSeries = true,
        genre = "Crime",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2018",
        cast = "Pankaj Tripathi, Ali Fazal, Divyenndu",
        description = "A shocking incident at a wedding procession ignites a series of events entangling the lives of two families in the lawless city of Mirzapur.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BZTFjMzMxZTUtYTMyNy00OWNhLTk4ODQtNGI1NjI1NjJhMzc3XkEyXkFqcGc@._V1_.jpg",
        totalSeasons = 3,
        totalEpisodes = 29
    ),
    WatchCatalogueItem(
        identifier = "oppenheimer-2023",
        title = "Oppenheimer",
        isSeries = false,
        genre = "Biography",
        origin = CinemaOrigin.HOLLYWOOD,
        releaseYear = "2023",
        cast = "Cillian Murphy, Emily Blunt, Matt Damon",
        description = "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BN2JkMDc5MGQtZjg3YS00NmFiLWIyZmQtZTJmNTM5MjVmYTQ4XkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 180
    ),
    WatchCatalogueItem(
        identifier = "the-family-man-series",
        title = "The Family Man",
        isSeries = true,
        genre = "Thriller",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2019",
        cast = "Manoj Bajpayee, Sharib Hashmi, Samantha Ruth Prabhu",
        description = "A working man from the National Investigation Agency tries to protect the nation from terrorism while also keeping his family safe from his secret job.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BZjM3YTczOWQtNjkzNi00ZTRlLThkZWEtZjgwZGVmODhkMDU3XkEyXkFqcGc@._V1_.jpg",
        totalSeasons = 2,
        totalEpisodes = 19
    ),
    WatchCatalogueItem(
        identifier = "dune-part-two-2024",
        title = "Dune: Part Two",
        isSeries = false,
        genre = "Sci-Fi",
        origin = CinemaOrigin.HOLLYWOOD,
        releaseYear = "2024",
        cast = "Timothée Chalamet, Zendaya, Rebecca Ferguson",
        description = "Paul Atreides unites with Chani and the Fremen while on a warpath of revenge against the conspirators who destroyed his family.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BNTc0YmQxMjEtODI5MC00NjFiLTlkMWUtOGQ5NjFmYWUyZGJhXkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 166
    ),
    WatchCatalogueItem(
        identifier = "panchayat-series",
        title = "Panchayat",
        isSeries = true,
        genre = "Comedy",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2020",
        cast = "Jitendra Kumar, Raghubir Yadav, Neena Gupta",
        description = "A comedy-drama, which captures the journey of an engineering graduate Abhishek, who for lack of a better job option joins as secretary of a Panchayat office in a remote village of Uttar Pradesh.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BNjMwYWMxNjYtY2I2NC00OWE0LTg4MmQtMmI5ZGI2NzU4ODhiXkEyXkFqcGc@._V1_.jpg",
        totalSeasons = 3,
        totalEpisodes = 24
    ),
    WatchCatalogueItem(
        identifier = "breaking-bad-series",
        title = "Breaking Bad",
        isSeries = true,
        genre = "Crime",
        origin = CinemaOrigin.HOLLYWOOD,
        releaseYear = "2008",
        cast = "Bryan Cranston, Aaron Paul, Anna Gunn",
        description = "A chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing and selling methamphetamine with a former student in order to secure his family's future.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMzU5ZGYzNmQtMTdhYy00OGRiLTg0NmQtYjVjNzliZTg1ZGE4XkEyXkFqcGc@._V1_.jpg",
        totalSeasons = 5,
        totalEpisodes = 62
    ),
    WatchCatalogueItem(
        identifier = "stree-2-2024",
        title = "Stree 2",
        isSeries = false,
        genre = "Horror",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2024",
        cast = "Rajkummar Rao, Shraddha Kapoor, Pankaj Tripathi",
        description = "After the events of Stree, the town of Chanderi is being haunted again. This time, women are mysteriously abducted by a terrifying headless entity.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMTA1NmUxYzItZmVmNy00YmQxLTk4Y2UtZjVkMWUwMWQ5N2IxXkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 147
    ),
    WatchCatalogueItem(
        identifier = "stranger-things-series",
        title = "Stranger Things",
        isSeries = true,
        genre = "Sci-Fi",
        origin = CinemaOrigin.HOLLYWOOD,
        releaseYear = "2016",
        cast = "Millie Bobby Brown, Finn Wolfhard, Winona Ryder",
        description = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMjEzMDAxOTUyMV5BMl5BanBnXkFtZTgwNzAxMzYzOTE@._V1_.jpg",
        totalSeasons = 4,
        totalEpisodes = 34
    ),
    WatchCatalogueItem(
        identifier = "inception-2010",
        title = "Inception",
        isSeries = false,
        genre = "Sci-Fi",
        origin = CinemaOrigin.HOLLYWOOD,
        releaseYear = "2010",
        cast = "Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page",
        description = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_.jpg",
        runtimeMinutes = 148
    ),
    WatchCatalogueItem(
        identifier = "rrr-2022",
        title = "RRR",
        isSeries = false,
        genre = "Action",
        origin = CinemaOrigin.BOLLYWOOD,
        releaseYear = "2022",
        cast = "N.T. Rama Rao Jr., Ram Charan, Ajay Devgn",
        description = "A fearless revolutionary and an officer in the British force, who once shared a deep bond, decide to join forces and chart out an inspirational path of freedom against the despotic rulers.",
        posterUrl = "https://m.media-amazon.com/images/M/MV5BNWMwODYyMjQtMTczMi00NTQ1LWFkYjItMGJhMWRkY2E3NDAyXkEyXkFqcGc@._V1_.jpg",
        runtimeMinutes = 187
    )
)

/**
 * Resolves official theatrical movie/series posters from IMDb suggestion API so search results
 * always display original movie/series posters and never Internet Archive placeholder images.
 */
private fun fetchOfficialPosterMetadata(queryTitle: String): List<WatchCatalogueItem> {
    val clean = queryTitle.trim()
    if (clean.isEmpty()) return emptyList()
    return try {
        val encoded = URLEncoder.encode(clean.lowercase().replace(" ", "_"), "UTF-8")
        val firstChar = encoded.firstOrNull { it.isLetterOrDigit() }?.lowercaseChar() ?: 'a'
        val url = URL("https://v3.sg.media-imdb.com/suggestion/$firstChar/$encoded.json")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
        }
        if (conn.responseCode != 200) return emptyList()
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(body)
        val arr = root.optJSONArray("d") ?: return emptyList()
        val results = mutableListOf<WatchCatalogueItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val qid = obj.optString("qid", "")
            if (qid != "movie" && qid != "tvSeries" && qid != "tvMiniSeries") continue
            val title = obj.optString("l", "").trim()
            val imageObj = obj.optJSONObject("i") ?: continue
            val posterUrl = imageObj.optString("imageUrl", "").trim()
            if (title.isEmpty() || posterUrl.isEmpty()) continue
            // Exclude animation/anime content
            val qLabel = obj.optString("q", "").lowercase()
            if (title.contains("anime", ignoreCase = true) ||
                title.contains("animated", ignoreCase = true) ||
                qLabel.contains("animation")
            ) continue

            val id = obj.optString("id", title.lowercase().replace(Regex("[^a-z0-9]+"), "-"))
            val year = obj.optInt("y", 2024).toString()
            val cast = obj.optString("s", "")
            val isSeries = (qid == "tvSeries" || qid == "tvMiniSeries")
            results.add(
                WatchCatalogueItem(
                    identifier = id,
                    title = title,
                    isSeries = isSeries,
                    genre = if (isSeries) "Drama" else "Action",
                    origin = CinemaOrigin.HOLLYWOOD,
                    releaseYear = year,
                    cast = cast,
                    description = if (cast.isNotBlank()) "Starring $cast. Released in $year." else "Live-action ${if (isSeries) "series" else "feature film"} ($year).",
                    posterUrl = posterUrl,
                    totalSeasons = if (isSeries) 2 else 1,
                    totalEpisodes = if (isSeries) 16 else 1,
                    runtimeMinutes = if (!isSeries) 135 else null
                )
            )
        }
        results
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Watch Catalogue Screen for real-world live-action Movies & Series (Bollywood & Hollywood).
 * Uses original theatrical posters, excludes animation, and plays strictly inside the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    viewModel: AnimeViewModel? = null,
    onNavigateToPlayer: ((animeId: Int, ep: Int, title: String, iaId: String) -> Unit)? = null,
    onNavigateToDetail: ((animeId: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val archiveRepo = remember { ArchiveSearchRepository() }
    val iaRepo = remember { InternetArchiveRepository() }
    val movieBoxRepo = remember { MovieBoxRepository(internetArchiveRepository = iaRepo) }
    val downloadManager = remember(context) { MovieBoxDownloadManager(context) }
    val downloadsList by downloadManager.downloads.collectAsState()
    val playbackRepo = remember(context) { PlaybackRepository(context) }

    // Primary Watch Navigation Sections (Real-world Movies, Series, Bollywood, Hollywood)
    var currentSection by remember { mutableStateOf("Home") }
    val sections = listOf("Home", "Movies", "Series", "Bollywood", "Hollywood", "Genres", "My List", "Recently Viewed")

    // Search & Filter State
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All Genres") }
    var selectedOrigin by remember { mutableStateOf<CinemaOrigin?>(null) }
    var isGridView by remember { mutableStateOf(true) }
    var sortOrder by remember { mutableStateOf("Newest") } // "Newest", "A-Z"
    var showFilterSheet by remember { mutableStateOf(false) }

    // Catalogue & MovieBox Pagination Data State
    val catalogueItems = remember { mutableStateListOf<WatchCatalogueItem>().apply { addAll(CURATED_MOVIES_AND_SERIES) } }
    val bookmarkedIds = remember { mutableStateListOf<String>() }
    val recentlyViewedItems = remember { mutableStateListOf<WatchCatalogueItem>() }
    var isLiveSearching by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var isLoadingMorePage by remember { mutableStateOf(false) }
    var canLoadMorePages by remember { mutableStateOf(true) }

    // MovieBox Downloads Sheet State
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var downloadStatusBanner by remember { mutableStateOf<String?>(null) }
    var isPreparingDownload by remember { mutableStateOf(false) }

    // In-App Playback and Detail State (Unified ExoPlayer opens immediately without blocking overlay)
    var selectedItemForDetail by remember { mutableStateOf<WatchCatalogueItem?>(null) }
    var activePlaybackItem by remember { mutableStateOf<WatchCatalogueItem?>(null) }
    var activePlaybackEpisode by remember { mutableIntStateOf(1) }
    var directPlayableMedia by remember { mutableStateOf<ArchivePlayableMedia?>(null) }
    var isResolvingPlayback by remember { mutableStateOf(false) }
    var playbackUnavailableReason by remember { mutableStateOf<String?>(null) }
    var playbackResolveJob by remember { mutableStateOf<Job?>(null) }

    // Local Device Video Playback State
    var showLocalVideosSheet by remember { mutableStateOf(false) }
    var recentLocalVideos by remember { mutableStateOf(playbackRepo.getRecentLocalVideos()) }
    var activeLocalVideoUri by remember { mutableStateOf<String?>(null) }
    var activeLocalVideoTitle by remember { mutableStateOf("") }
    var activeLocalInitialPosMs by remember { mutableLongStateOf(0L) }

    val openLocalVideoUri: (Uri, String?) -> Unit = { uri, fallbackTitle ->
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}
        var displayName = fallbackTitle
        if (displayName.isNullOrBlank()) {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) displayName = cursor.getString(idx)
                    }
                }
            } catch (_: Exception) {}
        }
        val resolvedTitle = displayName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "Local Video"
        val uriStr = uri.toString()
        val savedPos = playbackRepo.getLocalVideoSavedPositionMs(uriStr)
        playbackRepo.saveLocalVideoProgress(
            uriString = uriStr,
            displayName = resolvedTitle,
            positionMs = savedPos,
            durationMs = 0L,
            mimeType = context.contentResolver.getType(uri) ?: "video/*"
        )
        recentLocalVideos = playbackRepo.getRecentLocalVideos()
        activeLocalVideoTitle = resolvedTitle
        activeLocalInitialPosMs = savedPos
        activeLocalVideoUri = uriStr
    }

    val localVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            openLocalVideoUri(uri, null)
        }
    }

    // Debounce search query
    LaunchedEffect(searchQuery) {
        delay(350)
        debouncedQuery = searchQuery.trim()
    }

    // Initial & Section-based MovieBox API Discovery + Live Search
    LaunchedEffect(debouncedQuery, currentSection) {
        currentPage = 1
        canLoadMorePages = true
        if (debouncedQuery.isNotEmpty()) {
            isLiveSearching = true
            withContext(Dispatchers.IO) {
                try {
                    val localMatches = CURATED_MOVIES_AND_SERIES.filter {
                        it.title.contains(debouncedQuery, ignoreCase = true) ||
                            it.genre.contains(debouncedQuery, ignoreCase = true) ||
                            it.cast.contains(debouncedQuery, ignoreCase = true)
                    }
                    val movieBoxSearch = movieBoxRepo.search(debouncedQuery, page = 1)
                        .map { it.toWatchCatalogueItem() }
                    val remoteMatches = fetchOfficialPosterMetadata(debouncedQuery)
                    val merged = (localMatches + movieBoxSearch + remoteMatches)
                        .distinctBy { it.title.lowercase() }

                    withContext(Dispatchers.Main) {
                        catalogueItems.clear()
                        catalogueItems.addAll(merged)
                    }
                } finally {
                    withContext(Dispatchers.Main) { isLiveSearching = false }
                }
            }
        } else {
            val remoteList = withContext(Dispatchers.IO) {
                val mbItems = when (currentSection) {
                    "Movies" -> movieBoxRepo.getLatestMovies(page = 1)
                    "Series" -> movieBoxRepo.getTVSeries(page = 1)
                    else -> movieBoxRepo.getTrending(page = 1)
                }.map { it.toWatchCatalogueItem() }
                (CURATED_MOVIES_AND_SERIES + mbItems).distinctBy { it.title.lowercase() }
            }
            catalogueItems.clear()
            catalogueItems.addAll(remoteList)
        }
    }

    val handleLoadMorePage: () -> Unit = {
        if (!isLoadingMorePage && canLoadMorePages) {
            isLoadingMorePage = true
            val nextPage = currentPage + 1
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val moreItems = if (debouncedQuery.isNotEmpty()) {
                        movieBoxRepo.search(debouncedQuery, page = nextPage)
                    } else {
                        when (currentSection) {
                            "Movies" -> movieBoxRepo.getLatestMovies(page = nextPage)
                            "Series" -> movieBoxRepo.getTVSeries(page = nextPage)
                            else -> movieBoxRepo.getTrending(page = nextPage)
                        }
                    }.map { it.toWatchCatalogueItem() }

                    withContext(Dispatchers.Main) {
                        val newUnique = moreItems.filterNot { candidate ->
                            catalogueItems.any { it.title.equals(candidate.title, ignoreCase = true) }
                        }
                        if (newUnique.isNotEmpty()) {
                            catalogueItems.addAll(newUnique)
                            currentPage = nextPage
                        } else {
                            canLoadMorePages = false
                        }
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoadingMorePage = false
                    }
                }
            }
        }
    }

    val handleDownloadItem: (WatchCatalogueItem, Int) -> Unit = { item, episodeNum ->
        if (!isPreparingDownload) {
            isPreparingDownload = true
            downloadStatusBanner = "Verifying authorized download stream for \"${item.title}\"..."
            showDownloadsSheet = true
            coroutineScope.launch(Dispatchers.IO) {
                val streamData = movieBoxRepo.getStreamingUrls(
                    id = item.movieBoxId,
                    type = if (item.isSeries) "series" else "movie",
                    episodeNumber = if (item.isSeries) episodeNum else null,
                    titleHint = item.title,
                    archiveIdentifierHint = item.identifier
                )
                withContext(Dispatchers.Main) {
                    isPreparingDownload = false
                    if (streamData.isVerifiedPlayableMedia && !streamData.downloadUrl.isNullOrBlank()) {
                        val displayTitle = if (item.isSeries) "${item.title} - Ep $episodeNum" else item.title
                        val res = downloadManager.startOrResumeDownload(
                            contentId = item.movieBoxId.takeIf { it > 0 } ?: item.identifier.hashCode(),
                            episodeNumber = if (item.isSeries) episodeNum else null,
                            title = displayTitle,
                            quality = streamData.quality,
                            downloadUrl = streamData.downloadUrl
                        )
                        downloadStatusBanner = res.fold(
                            onSuccess = { "Download started: ${it.fileName}" },
                            onFailure = { it.localizedMessage ?: "Download unavailable" }
                        )
                    } else {
                        downloadStatusBanner = streamData.message.ifBlank {
                            "No authorized downloadable video stream is currently available for \"${item.title}\"."
                        }
                    }
                }
            }
        }
    }

    // Filter and Sort displayed items
    val displayedItems = remember(
        catalogueItems.toList(),
        currentSection,
        selectedGenre,
        selectedOrigin,
        sortOrder,
        bookmarkedIds.toList(),
        recentlyViewedItems.toList()
    ) {
        var list = when (currentSection) {
            "Home" -> catalogueItems.toList()
            "Movies" -> catalogueItems.filter { !it.isSeries }
            "Series" -> catalogueItems.filter { it.isSeries }
            "Bollywood" -> catalogueItems.filter { it.origin == CinemaOrigin.BOLLYWOOD }
            "Hollywood" -> catalogueItems.filter { it.origin == CinemaOrigin.HOLLYWOOD }
            "Genres" -> if (selectedGenre == "All Genres") catalogueItems.toList() else catalogueItems.filter { it.genre.equals(selectedGenre, ignoreCase = true) }
            "My List" -> catalogueItems.filter { bookmarkedIds.contains(it.identifier) }
            "Recently Viewed" -> recentlyViewedItems.toList()
            else -> catalogueItems.toList()
        }

        if (selectedGenre != "All Genres" && currentSection != "Genres") {
            list = list.filter { it.genre.equals(selectedGenre, ignoreCase = true) }
        }

        if (selectedOrigin != null) {
            list = list.filter { it.origin == selectedOrigin }
        }

        list = when (sortOrder) {
            "A-Z" -> list.sortedBy { it.title.lowercase() }
            else -> list.sortedByDescending { it.releaseYear }
        }

        list
    }

    // MovieBox In-App Playback Handler (Streams movies & series via MovieBox API inside the unified ExoPlayer)
    val handlePlayTitle: (WatchCatalogueItem, Int) -> Unit = { item, episodeNum ->
        if (!recentlyViewedItems.any { it.identifier == item.identifier }) {
            recentlyViewedItems.add(0, item)
        }
        playbackResolveJob?.cancel()
        activePlaybackItem = item
        activePlaybackEpisode = episodeNum
        directPlayableMedia = null
        playbackUnavailableReason = null
        isResolvingPlayback = true

        playbackResolveJob = coroutineScope.launch(Dispatchers.IO) {
            val resolvedTitle = if (item.isSeries) "${item.title} - Episode $episodeNum" else item.title

            val streamData = withTimeoutOrNull(16_000L) {
                movieBoxRepo.getStreamingUrls(
                    id = item.movieBoxId,
                    type = if (item.isSeries) "series" else "movie",
                    episodeNumber = if (item.isSeries) episodeNum else null,
                    titleHint = item.title,
                    archiveIdentifierHint = item.identifier
                )
            }

            val resolvedMedia = streamData?.playableMedia?.takeIf { streamData.isVerifiedPlayableMedia }
            val failReason = streamData?.message ?: "Can't find \"$resolvedTitle\" stream on MovieBox."

            withContext(Dispatchers.Main) {
                isResolvingPlayback = false
                if (resolvedMedia != null) {
                    directPlayableMedia = resolvedMedia
                    playbackUnavailableReason = null
                } else {
                    directPlayableMedia = null
                    playbackUnavailableReason = failReason
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060913),
                        Color(0xFF04060C),
                        Color(0xFF020408)
                    )
                )
            )
            .testTag("watch_catalogue_screen")
    ) {
        // Subtle Radial Frost Glow Ambient in top corner
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.12f),
                            Color(0xFF818CF8).copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .blur(50.dp)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MOVIES & SERIES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.2.sp
                        ),
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Watch",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Open Local Device Video Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clickable {
                                recentLocalVideos = playbackRepo.getRecentLocalVideos()
                                showLocalVideosSheet = true
                            }
                            .testTag("watch_open_local_video_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Open Local Video",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Local Video",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    // MovieBox Downloads Manager Button
                    IconButton(
                        onClick = {
                            downloadStatusBanner = null
                            showDownloadsSheet = true
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("watch_downloads_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "MovieBox Downloads",
                            tint = if (downloadsList.isNotEmpty()) Color(0xFF10B981) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // View Toggle (Grid / List)
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("toggle_display_mode")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Filter Button
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), CircleShape)
                            .testTag("filter_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter Catalogue",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Real-Time Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search Bollywood, Hollywood movies & series...",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (isLiveSearching) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0A0F1D).copy(alpha = 0.85f),
                    unfocusedContainerColor = Color(0xFF0A0F1D).copy(alpha = 0.65f),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("watch_catalogue_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.forEach { sec ->
                    val isSecSelected = sec == currentSection
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSecSelected) Color(0xFF0284C7) else Color(0xFF0D1527),
                        border = BorderStroke(
                            1.dp,
                            if (isSecSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .clickable { currentSection = sec }
                            .testTag("watch_nav_${sec.lowercase().replace(' ', '_')}")
                    ) {
                        Text(
                            text = sec,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSecSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = if (isSecSelected) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Content Area
            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Titles Found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentSection == "My List") "You have not saved any movies or series to My List yet."
                            else "No movies or series match the current filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                selectedGenre = "All Genres"
                                selectedOrigin = null
                                currentSection = "Home"
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Text("Reset Filters")
                        }
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag("watch_catalogue_grid")
                ) {
                    if (currentSection == "Home" && displayedItems.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            val heroItem = displayedItems.first()
                            FrostGlowHeroCard(
                                item = heroItem,
                                isBookmarked = bookmarkedIds.contains(heroItem.identifier),
                                onToggleBookmark = {
                                    if (bookmarkedIds.contains(heroItem.identifier)) bookmarkedIds.remove(heroItem.identifier)
                                    else bookmarkedIds.add(heroItem.identifier)
                                },
                                onWatchNow = { handlePlayTitle(heroItem, 1) },
                                onViewDetails = { selectedItemForDetail = heroItem }
                            )
                        }
                    }

                    items(if (currentSection == "Home") displayedItems.drop(1) else displayedItems, key = { it.identifier }) { item ->
                        FrostGlowCatalogueCard(
                            item = item,
                            isBookmarked = bookmarkedIds.contains(item.identifier),
                            onToggleBookmark = {
                                if (bookmarkedIds.contains(item.identifier)) bookmarkedIds.remove(item.identifier)
                                else bookmarkedIds.add(item.identifier)
                            },
                            onClick = { selectedItemForDetail = item },
                            onPlay = { handlePlayTitle(item, 1) }
                        )
                    }

                    if (canLoadMorePages && currentSection in listOf("Home", "Movies", "Series")) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = handleLoadMorePage,
                                    enabled = !isLoadingMorePage,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                    modifier = Modifier.testTag("watch_load_more_btn")
                                ) {
                                    if (isLoadingMorePage) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF38BDF8),
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Loading More...")
                                    } else {
                                        Text("Load More Titles (Page ${currentPage + 1})", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("watch_catalogue_list")
                ) {
                    items(displayedItems, key = { it.identifier }) { item ->
                        FrostGlowListRow(
                            item = item,
                            isBookmarked = bookmarkedIds.contains(item.identifier),
                            onToggleBookmark = {
                                if (bookmarkedIds.contains(item.identifier)) bookmarkedIds.remove(item.identifier)
                                else bookmarkedIds.add(item.identifier)
                            },
                            onClick = { selectedItemForDetail = item },
                            onPlay = { handlePlayTitle(item, 1) }
                        )
                    }
                }
            }
        }

        // Detail Bottom Sheet
        if (selectedItemForDetail != null) {
            val item = selectedItemForDetail!!
            val isSaved = bookmarkedIds.contains(item.identifier)
            ModalBottomSheet(
                onDismissRequest = { selectedItemForDetail = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF090E1A),
                scrimColor = Color.Black.copy(alpha = 0.70f)
            ) {
                TitleDetailSheetContent(
                    item = item,
                    isBookmarked = isSaved,
                    onToggleBookmark = {
                        if (isSaved) bookmarkedIds.remove(item.identifier)
                        else bookmarkedIds.add(item.identifier)
                    },
                    onWatchEpisode = { ep ->
                        selectedItemForDetail = null
                        handlePlayTitle(item, ep)
                    },
                    onDownloadEpisode = { ep ->
                        selectedItemForDetail = null
                        handleDownloadItem(item, ep)
                    },
                    onDismiss = { selectedItemForDetail = null }
                )
            }
        }

        // Filter Bottom Sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = Color(0xFF0A0F1D)
            ) {
                CatalogueFilterSheetContent(
                    selectedGenre = selectedGenre,
                    onSelectGenre = { selectedGenre = it },
                    selectedOrigin = selectedOrigin,
                    onSelectOrigin = { selectedOrigin = it },
                    sortOrder = sortOrder,
                    onSelectSort = { sortOrder = it },
                    onApply = { showFilterSheet = false },
                    onReset = {
                        selectedGenre = "All Genres"
                        selectedOrigin = null
                        sortOrder = "Newest"
                        showFilterSheet = false
                    }
                )
            }
        }

        // Unified Full-Featured In-App MovieBox ExoPlayer Dialog
        if (activePlaybackItem != null) {
            val item = activePlaybackItem!!
            val displayTitle = if (item.isSeries) "${item.title} - Episode $activePlaybackEpisode" else item.title
            ArchiveDirectPlayerDialog(
                playable = directPlayableMedia,
                isLoading = isResolvingPlayback,
                unavailableReason = playbackUnavailableReason,
                displayTitle = displayTitle,
                currentEpisode = if (item.isSeries) activePlaybackEpisode else null,
                totalEpisodes = if (item.isSeries) item.totalEpisodes else 1,
                sourceBadgeText = "MOVIEBOX STREAM",
                onSelectEpisode = if (item.isSeries) {
                    { newEp -> handlePlayTitle(item, newEp) }
                } else null,
                onRetry = { handlePlayTitle(item, activePlaybackEpisode) },
                onDismiss = {
                    playbackResolveJob?.cancel()
                    isResolvingPlayback = false
                    activePlaybackItem = null
                    directPlayableMedia = null
                    playbackUnavailableReason = null
                }
            )
        }

        // Local Device Video Player Dialog
        if (activeLocalVideoUri != null) {
            val localUriStr = activeLocalVideoUri!!
            val localPlayable = remember(localUriStr, activeLocalVideoTitle) {
                ArchivePlayableMedia(
                    identifier = "local_device_video",
                    title = activeLocalVideoTitle,
                    streamUrl = localUriStr,
                    fileName = activeLocalVideoTitle,
                    format = "LOCAL FILE",
                    sizeBytes = 0L,
                    durationSeconds = null,
                    licenseUrl = null,
                    description = "Playing local device video file",
                    creator = "Local Storage",
                    matchedEpisode = null,
                    allCompatibleFiles = emptyList()
                )
            }
            ArchiveDirectPlayerDialog(
                playable = localPlayable,
                isLoading = false,
                unavailableReason = null,
                displayTitle = activeLocalVideoTitle,
                currentEpisode = null,
                totalEpisodes = 1,
                initialPositionMs = activeLocalInitialPosMs,
                onPositionUpdate = { posMs ->
                    playbackRepo.saveLocalVideoProgress(
                        uriString = localUriStr,
                        displayName = activeLocalVideoTitle,
                        positionMs = posMs,
                        durationMs = 0L
                    )
                },
                onDismiss = {
                    recentLocalVideos = playbackRepo.getRecentLocalVideos()
                    activeLocalVideoUri = null
                }
            )
        }

        // Recently Played Local Videos & File Picker Sheet
        if (showLocalVideosSheet) {
            RecentLocalVideosSheet(
                recentVideos = recentLocalVideos,
                onOpenLocalFilePicker = {
                    showLocalVideosSheet = false
                    localVideoPickerLauncher.launch(
                        arrayOf("video/*", "application/x-mpegURL", "application/dash+xml")
                    )
                },
                onSelectRecentVideo = { item ->
                    showLocalVideosSheet = false
                    openLocalVideoUri(Uri.parse(item.uriString), item.displayName)
                },
                onRemoveRecentVideo = { uriString ->
                    playbackRepo.removeLocalVideoFromHistory(uriString)
                    recentLocalVideos = playbackRepo.getRecentLocalVideos()
                },
                onDismiss = { showLocalVideosSheet = false }
            )
        }

        // MovieBox Downloads Sheet (Progress, Pause, Resume, Cancel, and Verified Offline Playback)
        if (showDownloadsSheet) {
            MovieBoxDownloadsSheet(
                downloads = downloadsList,
                statusBannerMessage = downloadStatusBanner,
                onPauseDownload = { taskId -> downloadManager.pauseDownload(taskId) },
                onResumeDownload = { taskId -> downloadManager.resumeDownload(taskId) },
                onCancelDownload = { taskId -> downloadManager.cancelAndRemoveDownload(taskId) },
                onPlayOffline = { task ->
                    if (task.isVerifiedPlayableOffline) {
                        showDownloadsSheet = false
                        val offlineFileUri = Uri.fromFile(File(task.destinationPath))
                        openLocalVideoUri(offlineFileUri, task.title)
                    }
                },
                onDismiss = { showDownloadsSheet = false }
            )
        }
    }
}

/**
 * Reusable Original Movie/Series Poster Image with sleek dark fallback (never Internet Archive logo).
 */
@Composable
private fun OriginalPosterImage(
    posterUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = posterUrl,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF090D1A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF38BDF8),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF090D1A))
                        )
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    )
}

/**
 * Featured Hero Card with clean original movie/series poster (no "Featured Hindi Dub" or "Hindi Verified" text).
 */
@Composable
private fun FrostGlowHeroCard(
    item: WatchCatalogueItem,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onWatchNow: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527).copy(alpha = 0.85f)),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.8f),
                    Color(0xFF818CF8).copy(alpha = 0.4f),
                    Color(0xFF06B6D4).copy(alpha = 0.7f)
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("hero_featured_card")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                OriginalPosterImage(
                    posterUrl = item.posterUrl,
                    title = item.title,
                    modifier = Modifier.fillMaxSize()
                )
                // Subtle bottom gradient for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF0D1527).copy(alpha = 0.75f),
                                    Color(0xFF0D1527)
                                )
                            )
                        )
                )

                // Bookmark button in top-right corner only (no banner text)
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFF38BDF8) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.origin.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF38BDF8)
                    )
                    Text(text = " • ", color = Color(0xFF64748B))
                    Text(
                        text = item.genre,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFFCBD5E1)
                    )
                    Text(text = " • ", color = Color(0xFF64748B))
                    Text(
                        text = if (item.isSeries) "${item.totalSeasons} Seasons (${item.totalEpisodes} Ep)" else "${item.runtimeMinutes ?: 140} min",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                    Text(text = " • ", color = Color(0xFF64748B))
                    Text(
                        text = item.releaseYear,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onWatchNow,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("Details", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * 2-Column Grid Card showcasing the clean original Movie/Series Poster.
 */
@Composable
private fun FrostGlowCatalogueCard(
    item: WatchCatalogueItem,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1322).copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("catalogue_card_${item.identifier}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
            ) {
                OriginalPosterImage(
                    posterUrl = item.posterUrl,
                    title = item.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Bookmark Icon in Top Right
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.60f))
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFF38BDF8) else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Quick Play Action Overlay on Bottom Right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48))
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.isSeries) "Series" else "Movie",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF38BDF8)
                    )
                    Text(text = " • ", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text(
                        text = item.genre,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color(0xFFCBD5E1)
                    )
                    Text(text = " • ", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text(
                        text = item.releaseYear,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

/**
 * List View Row
 */
@Composable
private fun FrostGlowListRow(
    item: WatchCatalogueItem,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1322).copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 96.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                OriginalPosterImage(
                    posterUrl = item.posterUrl,
                    title = item.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.origin.label} • ${if (item.isSeries) "Series" else "Movie"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = Color(0xFF38BDF8)
                    )
                    Text(text = " • ", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text(
                        text = item.genre,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFFCBD5E1)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.isSeries) "${item.totalSeasons} Seasons • ${item.totalEpisodes} Episodes • ${item.releaseYear}"
                    else "${item.runtimeMinutes ?: 140} min • ${item.releaseYear}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF94A3B8)
                )

                if (item.cast.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.cast,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE11D48))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Watch",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Bottom Sheet Detail View for Movie & Series Details + Direct In-App Episode Selection
 */
@Composable
private fun TitleDetailSheetContent(
    item: WatchCatalogueItem,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onWatchEpisode: (Int) -> Unit,
    onDownloadEpisode: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedEpisodeToWatch by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("title_detail_sheet")
    ) {
        // Header with Original Poster
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(width = 82.dp, height = 118.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OriginalPosterImage(
                        posterUrl = item.posterUrl,
                        title = item.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${item.origin.label} • ${if (item.isSeries) "Series" else "Movie"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF38BDF8)
                            )
                        )
                        if (item.rating.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.rating,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color(0xFFFACC15)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${item.genre} • ${item.releaseYear}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFFCBD5E1)
                    )
                    if (item.cast.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.cast,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = Color(0xFF94A3B8),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Synopsis
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            color = Color(0xFFCBD5E1),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(14.dp))

        // If Series: Render Direct 5-Column Episode Grid Selector
        if (item.isSeries) {
            Text(
                text = "Episode Selector",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(6.dp))

            EpisodeGridSelector(
                totalEpisodes = item.totalEpisodes,
                selectedEpisode = selectedEpisodeToWatch,
                watchedEpisodeCount = 0,
                onEpisodeSelected = { ep ->
                    selectedEpisodeToWatch = ep
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // In-App Action Buttons Row (Plays strictly inside the app + Download option)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onWatchEpisode(selectedEpisodeToWatch) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                modifier = Modifier
                    .weight(1.25f)
                    .height(46.dp)
                    .testTag("detail_watch_now_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (item.isSeries) "Watch Ep $selectedEpisodeToWatch" else "Watch Movie",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            if (onDownloadEpisode != null) {
                OutlinedButton(
                    onClick = { onDownloadEpisode(selectedEpisodeToWatch) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(46.dp)
                        .testTag("detail_download_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Download",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onToggleBookmark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                modifier = Modifier
                    .weight(0.9f)
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isBookmarked) "Saved" else "My List",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

/**
 * Filter Bottom Sheet Content
 */
@Composable
private fun CatalogueFilterSheetContent(
    selectedGenre: String,
    onSelectGenre: (String) -> Unit,
    selectedOrigin: CinemaOrigin?,
    onSelectOrigin: (CinemaOrigin?) -> Unit,
    sortOrder: String,
    onSelectSort: (String) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Catalogue Filters",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            OutlinedButton(
                onClick = onReset,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Reset All", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Industry / Origin Filter (Bollywood / Hollywood)
        Text("Cinema", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CinemaOrigin.entries.forEach { origin ->
                val isSel = selectedOrigin == origin
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectOrigin(if (isSel) null else origin) },
                    label = { Text(origin.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sort By
        Text("Sort Order", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Newest", "A-Z").forEach { sort ->
                val isSel = sortOrder == sort
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectSort(sort) },
                    label = { Text(sort) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Genres
        Text("Genre", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(6.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
        ) {
            items(WATCH_GENRES) { g ->
                val isSel = selectedGenre == g
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSel) Color(0xFF38BDF8) else Color(0xFF334155)),
                    modifier = Modifier.clickable { onSelectGenre(g) }
                ) {
                    Text(
                        text = g,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        color = if (isSel) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onApply,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Text("Apply Filters", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
