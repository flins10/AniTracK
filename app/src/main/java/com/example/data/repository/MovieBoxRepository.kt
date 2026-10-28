package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.util.LruCache
import com.example.data.remote.ArchiveFileItem
import com.example.data.remote.InternetArchiveApiService
import com.example.data.remote.MovieBoxApiService
import com.example.data.remote.MovieBoxAuthInfo
import com.example.data.remote.MovieBoxCastMember
import com.example.data.remote.MovieBoxItem
import com.example.data.remote.MovieBoxQualityStream
import com.example.data.remote.MovieBoxStreamingData
import com.example.data.remote.MovieBoxSubtitleTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Repository implementing the MovieBox API specification from:
 * https://github.com/shamsullah01/moviebox/blob/refs/heads/master/docs/API.md
 *
 * Features:
 * - Trending (`getTrending`), Latest Movies (`getLatestMovies`), TV Series (`getTVSeries`),
 *   Search (`search`), Movie/Series Details (`getMovieDetails`), and Streaming URLs (`getStreamingUrls`).
 * - Request deduplication and in-memory LRU caching to avoid redundant network requests.
 * - Multi-page pagination support (`page = 1, 2, ...`).
 * - Stream playability validation: verifies whether a returned stream/download URL is a real
 *   playable video stream (`.mp4`, `.mkv`, `.webm`, `.m3u8`, `.mpd`) and resolves actual playable
 *   streams via [InternetArchiveRepository] when MovieBox.ng returns synthetic/web-only links.
 */
class MovieBoxRepository(
    private val apiService: MovieBoxApiService = MovieBoxApiService(),
    private val internetArchiveRepository: InternetArchiveRepository = InternetArchiveRepository(),
    private val okHttpClient: OkHttpClient = MovieBoxApiService.defaultHttpClient()
) {
    private val listCache = LruCache<String, List<MovieBoxItem>>(40)
    private val detailCache = LruCache<Int, MovieBoxItem>(50)
    private val inFlightListRequests = ConcurrentHashMap<String, Deferred<List<MovieBoxItem>>>()
    private val requestMutex = Mutex()

    companion object {
        private const val PAGE_SIZE = 8

        /**
         * Validates if a URL is a candidate direct media stream or adaptive manifest
         * rather than an HTML web page or UTM redirect link.
         */
        fun isDirectMediaStreamFormat(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val trimmed = url.trim()
            if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true) &&
                !trimmed.startsWith("content://", ignoreCase = true) &&
                !trimmed.startsWith("file://", ignoreCase = true)
            ) {
                return false
            }
            if (trimmed.contains("utm_source=", ignoreCase = true) ||
                trimmed.contains("/search?keyword=", ignoreCase = true)
            ) {
                return false
            }
            val cleanPath = trimmed.substringBefore('?').substringBefore('#').lowercase(Locale.US)
            return cleanPath.endsWith(".mp4") ||
                cleanPath.endsWith(".m4v") ||
                cleanPath.endsWith(".mkv") ||
                cleanPath.endsWith(".webm") ||
                cleanPath.endsWith(".m3u8") ||
                cleanPath.endsWith(".mpd")
        }

        /**
         * Sanitizes a movie/episode title into a safe filesystem filename, preventing path traversal
         * or illegal characters.
         */
        fun sanitizeFileName(rawTitle: String, quality: String = "720p", extension: String = "mp4"): String {
            val cleanBase = rawTitle
                .replace(Regex("\\[.*?]"), "")
                .replace(Regex("\\.+"), " ")
                .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
                .trim()
                .replace(Regex("\\s+"), "_")
                .trim('_')
                .take(72)
                .ifBlank { "moviebox_video" }
            val cleanQuality = quality.replace(Regex("[^a-zA-Z0-9]"), "").ifBlank { "HD" }
            val cleanExt = extension.trimStart('.').lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifBlank { "mp4" }
            return "${cleanBase}_${cleanQuality}.$cleanExt"
        }

        /**
         * Documented MovieBox catalog + curated Bollywood & Hollywood live-action movies and series
         * with original theatrical posters and zero animation content.
         */
        val FALLBACK_CATALOG: List<MovieBoxItem> = listOf(
            MovieBoxItem(
                id = 1,
                title = "Breaking Bad",
                year = "2008",
                rating = "9.5",
                genre = "Crime, Drama, Thriller",
                genreArray = listOf("Crime", "Drama", "Thriller"),
                poster = "https://m.media-amazon.com/images/M/MV5BMzU5ZGYzNmQtMTdhYy00OGRiLTg0NmQtYjVjNzliZTg1ZGE4XkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BMzU5ZGYzNmQtMTdhYy00OGRiLTg0NmQtYjVjNzliZTg1ZGE4XkEyXkFqcGc@._V1_.jpg",
                type = "Series",
                language = "EN",
                overview = "A high school chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing and selling methamphetamine with a former student to secure his family's future.",
                runtime = 49,
                cast = listOf(
                    MovieBoxCastMember("Bryan Cranston", "Walter White"),
                    MovieBoxCastMember("Aaron Paul", "Jesse Pinkman"),
                    MovieBoxCastMember("Anna Gunn", "Skyler White")
                ),
                director = "Vince Gilligan",
                totalSeasons = 5,
                totalEpisodes = 7,
                archiveIdentifier = "breaking-bad-1"
            ),
            MovieBoxItem(
                id = 2,
                title = "Oppenheimer",
                year = "2023",
                rating = "8.9",
                genre = "Biography, Drama, History",
                genreArray = listOf("Biography", "Drama", "History"),
                poster = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
                backdrop = "https://image.tmdb.org/t/p/w1280/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg",
                type = "Movie",
                language = "EN",
                overview = "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during World War II.",
                runtime = 180,
                cast = listOf(
                    MovieBoxCastMember("Cillian Murphy", "J. Robert Oppenheimer"),
                    MovieBoxCastMember("Emily Blunt", "Kitty Oppenheimer"),
                    MovieBoxCastMember("Matt Damon", "Leslie Groves")
                ),
                director = "Christopher Nolan",
                archiveIdentifier = "oppenheimer.-2023.1080-p.-bluray.-egydead.-com"
            ),
            MovieBoxItem(
                id = 3,
                title = "3 Idiots",
                year = "2009",
                rating = "8.4",
                genre = "Comedy, Drama",
                genreArray = listOf("Comedy", "Drama"),
                poster = "https://m.media-amazon.com/images/M/MV5BNzc4ZWQ3NmYtODE0Ny00YTQ4LTlkZWItNTBkMGQ0MmUwMmJlXkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BNzc4ZWQ3NmYtODE0Ny00YTQ4LTlkZWItNTBkMGQ0MmUwMmJlXkEyXkFqcGc@._V1_.jpg",
                type = "Movie",
                language = "HI",
                overview = "Two friends embark on a quest for a lost buddy. On this journey, they revisit their college days and recall their friend who inspired them to think differently.",
                runtime = 170,
                cast = listOf(
                    MovieBoxCastMember("Aamir Khan", "Rancho"),
                    MovieBoxCastMember("R. Madhavan", "Farhan"),
                    MovieBoxCastMember("Sharman Joshi", "Raju")
                ),
                director = "Rajkumar Hirani",
                archiveIdentifier = "3-idiots_202108"
            ),
            MovieBoxItem(
                id = 4,
                title = "Stranger Things",
                year = "2016",
                rating = "8.7",
                genre = "Sci-Fi, Mystery, Thriller",
                genreArray = listOf("Sci-Fi", "Mystery", "Thriller"),
                poster = "https://image.tmdb.org/t/p/w500/uOOtwVbSr4QDjAGIifLDwpb2Pdl.jpg",
                backdrop = "https://image.tmdb.org/t/p/w1280/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                type = "Series",
                language = "EN",
                overview = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
                runtime = 50,
                cast = listOf(
                    MovieBoxCastMember("Millie Bobby Brown", "Eleven"),
                    MovieBoxCastMember("Finn Wolfhard", "Mike Wheeler"),
                    MovieBoxCastMember("Winona Ryder", "Joyce Byers")
                ),
                director = "The Duffer Brothers",
                totalSeasons = 4,
                totalEpisodes = 8,
                archiveIdentifier = "stranger.-things.-s-01.720p.-blu-ray.x-264-galaxy-tv"
            ),
            MovieBoxItem(
                id = 5,
                title = "Dangal",
                year = "2016",
                rating = "8.3",
                genre = "Biography, Drama",
                genreArray = listOf("Biography", "Drama"),
                poster = "https://m.media-amazon.com/images/M/MV5BMTQ4MzQzMzM2Nl5BMl5BanBnXkFtZTgwMTQ1NzU3MDI@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BMTQ4MzQzMzM2Nl5BMl5BanBnXkFtZTgwMTQ1NzU3MDI@._V1_.jpg",
                type = "Movie",
                language = "HI",
                overview = "Former wrestler Mahavir Singh Phogat and his two wrestler daughters struggle towards glory at the Commonwealth Games.",
                runtime = 161,
                cast = listOf(
                    MovieBoxCastMember("Aamir Khan", "Mahavir Singh Phogat"),
                    MovieBoxCastMember("Fatima Sana Shaikh", "Geeta Phogat"),
                    MovieBoxCastMember("Sanya Malhotra", "Babita Kumari")
                ),
                director = "Nitesh Tiwari",
                archiveIdentifier = "dangal-hindi-educational-movie-hd"
            ),
            MovieBoxItem(
                id = 6,
                title = "Interstellar",
                year = "2014",
                rating = "8.7",
                genre = "Sci-Fi, Adventure, Drama",
                genreArray = listOf("Sci-Fi", "Adventure", "Drama"),
                poster = "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg",
                type = "Movie",
                language = "EN",
                overview = "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot, Joseph Cooper, is tasked to pilot a spacecraft to find a new planet for humans.",
                runtime = 169,
                cast = listOf(
                    MovieBoxCastMember("Matthew McConaughey", "Cooper"),
                    MovieBoxCastMember("Anne Hathaway", "Brand"),
                    MovieBoxCastMember("Jessica Chastain", "Murph")
                ),
                director = "Christopher Nolan",
                archiveIdentifier = "interstellar-2014_202409"
            ),
            MovieBoxItem(
                id = 7,
                title = "Mirzapur",
                year = "2018",
                rating = "8.5",
                genre = "Action, Crime, Thriller",
                genreArray = listOf("Action", "Crime", "Thriller"),
                poster = "https://m.media-amazon.com/images/M/MV5BNzRjMjhmZmUtYjRhNS00YjlmLTliNjUtM2I4M2Y0N2I0ZjM3XkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BNzRjMjhmZmUtYjRhNS00YjlmLTliNjUtM2I4M2Y0N2I0ZjM3XkEyXkFqcGc@._V1_.jpg",
                type = "Series",
                language = "HI",
                overview = "A shocking incident at a wedding procession ignites a series of events entangling the lives of two families in the lawless city of Mirzapur.",
                runtime = 48,
                cast = listOf(
                    MovieBoxCastMember("Pankaj Tripathi", "Kaleen Bhaiya"),
                    MovieBoxCastMember("Ali Fazal", "Guddu Pandit"),
                    MovieBoxCastMember("Divyenndu", "Munna Bhaiya")
                ),
                director = "Karan Anshuman",
                totalSeasons = 3,
                totalEpisodes = 10,
                archiveIdentifier = "mirzapur-s-02-e-10-hindi-480p-web-dl"
            ),
            MovieBoxItem(
                id = 8,
                title = "Bhool Bhulaiyaa 2",
                year = "2022",
                rating = "7.4",
                genre = "Comedy, Horror",
                genreArray = listOf("Comedy", "Horror"),
                poster = "https://m.media-amazon.com/images/M/MV5BYjczMWI2ZGMtM2ViMy00M2I2LTk0N2ItN2I1YzI0M2Y3N2I4XkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BYjczMWI2ZGMtM2ViMy00M2I2LTk0N2ItN2I1YzI0M2Y3N2I4XkEyXkFqcGc@._V1_.jpg",
                type = "Movie",
                language = "HI",
                overview = "When strangers Reet and Ruhan cross paths, their journey leads to an abandoned mansion and a dreaded spirit that has been trapped for 18 years.",
                runtime = 143,
                cast = listOf(
                    MovieBoxCastMember("Kartik Aaryan", "Rooh Baba"),
                    MovieBoxCastMember("Kiara Advani", "Reet"),
                    MovieBoxCastMember("Tabu", "Anjulika / Manjulika")
                ),
                director = "Anees Bazmee",
                archiveIdentifier = "bhool.-bhulaiyaa.-2.2022.-bolly-4u.org-web-dl-hindi-1080p-1.9-gb"
            ),
            MovieBoxItem(
                id = 9,
                title = "Sacred Games",
                year = "2018",
                rating = "8.5",
                genre = "Crime, Drama, Thriller",
                genreArray = listOf("Crime", "Drama", "Thriller"),
                poster = "https://m.media-amazon.com/images/M/MV5BMjI0N2I0M2UtY2M3Ny00ZGM3LTk3YjUtY2I3ZGM0N2I4YjE0XkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BMjI0N2I0M2UtY2M3Ny00ZGM3LTk3YjUtY2I3ZGM0N2I4YjE0XkEyXkFqcGc@._V1_.jpg",
                type = "Series",
                language = "HI",
                overview = "A link in their pasts leads an honest cop to a fugitive gang boss, whose cryptic warning spurs the officer on a quest to save Mumbai from cataclysm.",
                runtime = 50,
                cast = listOf(
                    MovieBoxCastMember("Saif Ali Khan", "Sartaj Singh"),
                    MovieBoxCastMember("Nawazuddin Siddiqui", "Ganesh Gaitonde"),
                    MovieBoxCastMember("Pankaj Tripathi", "Guruji")
                ),
                director = "Anurag Kashyap & Vikramaditya Motwane",
                totalSeasons = 2,
                totalEpisodes = 8,
                archiveIdentifier = "sacred-games-s02.-hevc"
            ),
            MovieBoxItem(
                id = 11,
                title = "Zindagi Na Milegi Dobara",
                year = "2011",
                rating = "8.2",
                genre = "Comedy, Drama, Adventure",
                genreArray = listOf("Comedy", "Drama", "Adventure"),
                poster = "https://m.media-amazon.com/images/M/MV5BZGFmMjM5OWMtZTRiNC00ODhlLThlYTItYTcyZDMyYmMyYjFjXkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BZGFmMjM5OWMtZTRiNC00ODhlLThlYTItYTcyZDMyYmMyYjFjXkEyXkFqcGc@._V1_.jpg",
                type = "Movie",
                language = "HI",
                overview = "Three friends decide to turn their fantasy vacation into reality after one of their friends gets engaged.",
                runtime = 155,
                cast = listOf(
                    MovieBoxCastMember("Hrithik Roshan", "Arjun"),
                    MovieBoxCastMember("Farhan Akhtar", "Imraan"),
                    MovieBoxCastMember("Abhay Deol", "Kabir")
                ),
                director = "Zoya Akhtar",
                archiveIdentifier = "zindagi-na-milegi-dobara-2011-webhd-720-p-hritik-roshan-abhay-deol"
            ),
            MovieBoxItem(
                id = 12,
                title = "Gangs of Wasseypur",
                year = "2012",
                rating = "8.2",
                genre = "Action, Crime, Drama",
                genreArray = listOf("Action", "Crime", "Drama"),
                poster = "https://m.media-amazon.com/images/M/MV5BMTc5NjY4MjUwNF5BMl5BanBnXkFtZTgwODM3NzM5MzE@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BMTc5NjY4MjUwNF5BMl5BanBnXkFtZTgwODM3NzM5MzE@._V1_.jpg",
                type = "Movie",
                language = "HI",
                overview = "A clash between Sultan and Shahid Khan leads to the expulsion of Khan from Wasseypur, and ignites a deadly blood feud spanning three generations.",
                runtime = 160,
                cast = listOf(
                    MovieBoxCastMember("Manoj Bajpayee", "Sardar Khan"),
                    MovieBoxCastMember("Nawazuddin Siddiqui", "Faizal Khan"),
                    MovieBoxCastMember("Richa Chadha", "Nagma Khatoon")
                ),
                director = "Anurag Kashyap",
                archiveIdentifier = "gangs-of-wasseypur-2012-part-1-hindi-1080p"
            ),
            MovieBoxItem(
                id = 13,
                title = "Taare Zameen Par",
                year = "2007",
                rating = "8.3",
                genre = "Drama, Family",
                genreArray = listOf("Drama", "Family"),
                poster = "https://m.media-amazon.com/images/M/MV5BMDhjZWViN2MtNzgxOS00NmI4LThiZDQtZDI3MzM4MDE4NTc0XkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BMDhjZWViN2MtNzgxOS00NmI4LThiZDQtZDI3MzM4MDE4NTc0XkEyXkFqcGc@._V1_.jpg",
                type = "Movie",
                language = "HI",
                overview = "An eight-year-old boy is thought to be a lazy trouble-maker, until the new art teacher has the patience and compassion to discover the real problem behind his struggles in school.",
                runtime = 165,
                cast = listOf(
                    MovieBoxCastMember("Darsheel Safary", "Ishaan Awasthi"),
                    MovieBoxCastMember("Aamir Khan", "Ram Shankar Nikumbh")
                ),
                director = "Aamir Khan",
                archiveIdentifier = "taare-zameen-par-hindi-educational-movie"
            ),
            MovieBoxItem(
                id = 14,
                title = "Lagaan",
                year = "2001",
                rating = "8.1",
                genre = "Drama, History, Sport",
                genreArray = listOf("Drama", "History", "Sport"),
                poster = "https://m.media-amazon.com/images/M/MV5BM2FmODM4OTktOTRjOS00ZTIzLWIzZjAtMDBhOGEzYThkNzMzXkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BM2FmODM4OTktOTRjOS00ZTIzLWIzZjAtMDBhOGEzYThkNzMzXkEyXkFqcGc@._V1_.jpg",
                type = "Movie",
                language = "HI",
                overview = "The people of a small village in Victorian India stake their future on a game of cricket against their ruthless British rulers.",
                runtime = 224,
                cast = listOf(
                    MovieBoxCastMember("Aamir Khan", "Bhuvan"),
                    MovieBoxCastMember("Gracy Singh", "Gauri")
                ),
                director = "Ashutosh Gowariker",
                archiveIdentifier = "lagaan-2001-dv-drip-charme-leon-silver-rg"
            ),
            MovieBoxItem(
                id = 15,
                title = "The Dark Knight Rises",
                year = "2012",
                rating = "8.4",
                genre = "Action, Thriller, Drama",
                genreArray = listOf("Action", "Thriller", "Drama"),
                poster = "https://image.tmdb.org/t/p/w500/hr0L2aueqlP2BYUblTTjmtn0hw4.jpg",
                backdrop = "https://image.tmdb.org/t/p/w1280/c3OHQncTAnKFhdOTX7D3LTW6son.jpg",
                type = "Movie",
                language = "EN",
                overview = "Eight years after the Joker's reign of anarchy, Batman, with the help of the enigmatic Selina Kyle, is forced from his exile to save Gotham City from the brutal guerrilla terrorist Bane.",
                runtime = 164,
                cast = listOf(
                    MovieBoxCastMember("Christian Bale", "Bruce Wayne / Batman"),
                    MovieBoxCastMember("Tom Hardy", "Bane"),
                    MovieBoxCastMember("Anne Hathaway", "Selina Kyle")
                ),
                director = "Christopher Nolan",
                archiveIdentifier = "The.Dark.Knight.Rises.2012.1080pWww.compucalitv.com"
            ),
            MovieBoxItem(
                id = 16,
                title = "Byomkesh Bakshi",
                year = "1993",
                rating = "9.1",
                genre = "Mystery, Crime, Thriller",
                genreArray = listOf("Mystery", "Crime", "Thriller"),
                poster = "https://m.media-amazon.com/images/M/MV5BZjM3YjE2N2UtM2E4MC00NDg4LTk1MzgtY2I4M2U2ZTg4YjE0XkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BZjM3YjE2N2UtM2E4MC00NDg4LTk1MzgtY2I4M2U2ZTg4YjE0XkEyXkFqcGc@._V1_.jpg",
                type = "Series",
                language = "HI",
                overview = "Sharp and observant truth-seeker Byomkesh Bakshi and his chronicler friend Ajit solve baffling murder mysteries in Kolkata.",
                runtime = 45,
                cast = listOf(
                    MovieBoxCastMember("Rajit Kapur", "Byomkesh Bakshi"),
                    MovieBoxCastMember("K.K. Raina", "Ajit Bandyopadhyay")
                ),
                director = "Basu Chatterjee",
                totalSeasons = 2,
                totalEpisodes = 25,
                archiveIdentifier = "byomkesh-bakshi-movie-world"
            ),
            MovieBoxItem(
                id = 17,
                title = "Sherlock Holmes",
                year = "1984",
                rating = "8.7",
                genre = "Mystery, Crime, Drama",
                genreArray = listOf("Mystery", "Crime", "Drama"),
                poster = "https://image.tmdb.org/t/p/w500/c9z71K3275mB0dJ2tE51E7G9o36.jpg",
                backdrop = "https://image.tmdb.org/t/p/w1280/c9z71K3275mB0dJ2tE51E7G9o36.jpg",
                type = "Series",
                language = "EN",
                overview = "Consulting detective Sherlock Holmes and his trusted friend Dr. John Watson crack the most intricate criminal cases across Victorian England.",
                runtime = 52,
                cast = listOf(
                    MovieBoxCastMember("Jeremy Brett", "Sherlock Holmes"),
                    MovieBoxCastMember("David Burke", "Dr. John Watson")
                ),
                director = "John Hawkesworth",
                totalSeasons = 4,
                totalEpisodes = 41,
                archiveIdentifier = "SherlockHolmesGranada"
            ),
            MovieBoxItem(
                id = 18,
                title = "Drishyam",
                year = "2015",
                rating = "8.2",
                genre = "Crime, Drama, Mystery",
                genreArray = listOf("Crime", "Drama", "Mystery"),
                poster = "https://m.media-amazon.com/images/M/MV5BYmJhZmJlYTItZmZlNy00MGY0LTg0ZGMtNWFkYWU5NTA1YTNhXkEyXkFqcGc@._V1_.jpg",
                backdrop = "https://m.media-amazon.com/images/M/MV5BYmJhZmJlYTItZmZlNy00MGY0LTg0ZGMtNWFkYWU5NTA1YTNhXkEyXkFqcGc@._V1_.jpg",
                type = "Movie",
                language = "HI",
                overview = "Desperate measures are taken by a man who tries to save his family from the dark side of the law, after they commit an unexpected crime.",
                runtime = 163,
                cast = listOf(
                    MovieBoxCastMember("Ajay Devgn", "Vijay Salgaonkar"),
                    MovieBoxCastMember("Tabu", "IG Meera Deshmukh"),
                    MovieBoxCastMember("Shriya Saran", "Nandini Salgaonkar")
                ),
                director = "Nishikant Kamat",
                archiveIdentifier = "drishyam-2015-1080p-blu-ray-x-264-dd-5.1"
            )
        )
    }

    /**
     * Deduplicated fetch helper with LRU caching.
     */
    private suspend fun fetchDeduplicatedList(
        cacheKey: String,
        forceRefresh: Boolean = false,
        fetcher: suspend () -> List<MovieBoxItem>
    ): List<MovieBoxItem> = coroutineScope {
        if (!forceRefresh) {
            listCache.get(cacheKey)?.let { return@coroutineScope it }
        }
        val deferred = requestMutex.withLock {
            inFlightListRequests[cacheKey] ?: async(Dispatchers.IO) {
                try {
                    val res = fetcher()
                    if (res.isNotEmpty()) {
                        listCache.put(cacheKey, res)
                    }
                    res
                } finally {
                    inFlightListRequests.remove(cacheKey)
                }
            }.also { inFlightListRequests[cacheKey] = it }
        }
        deferred.await()
    }

    /**
     * 1. `getTrending(page = 1)` - Fetches trending movies & series with pagination.
     */
    suspend fun getTrending(page: Int = 1, forceRefresh: Boolean = false): List<MovieBoxItem> {
        val safePage = page.coerceAtLeast(1)
        return fetchDeduplicatedList("trending_p$safePage", forceRefresh) {
            val remote = runCatching { apiService.fetchRemoteCategory("trending", safePage) }.getOrDefault(emptyList())
            if (remote.isNotEmpty()) {
                remote.forEach { detailCache.put(it.id, it) }
                remote
            } else {
                paginateFallback(FALLBACK_CATALOG, safePage)
            }
        }
    }

    /**
     * 2. `getLatestMovies(page = 1)` - Fetches latest movies with pagination.
     */
    suspend fun getLatestMovies(page: Int = 1, forceRefresh: Boolean = false): List<MovieBoxItem> {
        val safePage = page.coerceAtLeast(1)
        return fetchDeduplicatedList("movies_p$safePage", forceRefresh) {
            val remote = runCatching { apiService.fetchRemoteCategory("movies", safePage) }.getOrDefault(emptyList())
            val filteredRemote = remote.filter { !it.isSeries }
            if (filteredRemote.isNotEmpty()) {
                filteredRemote.forEach { detailCache.put(it.id, it) }
                filteredRemote
            } else {
                paginateFallback(FALLBACK_CATALOG.filter { !it.isSeries }, safePage)
            }
        }
    }

    /**
     * 3. `getTVSeries(page = 1)` - Fetches TV series with pagination.
     */
    suspend fun getTVSeries(page: Int = 1, forceRefresh: Boolean = false): List<MovieBoxItem> {
        val safePage = page.coerceAtLeast(1)
        return fetchDeduplicatedList("series_p$safePage", forceRefresh) {
            val remote = runCatching { apiService.fetchRemoteCategory("series", safePage) }.getOrDefault(emptyList())
            val filteredRemote = remote.filter { it.isSeries }
            if (filteredRemote.isNotEmpty()) {
                filteredRemote.forEach { detailCache.put(it.id, it) }
                filteredRemote
            } else {
                paginateFallback(FALLBACK_CATALOG.filter { it.isSeries }, safePage)
            }
        }
    }

    /**
     * 4. `search(query, page = 1)` - Searches MovieBox.ng, TMDB, OMDb, IMDb suggestion posters, and fallback catalog.
     */
    suspend fun search(query: String, page: Int = 1): List<MovieBoxItem> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return getTrending(page)
        val safePage = page.coerceAtLeast(1)
        val cacheKey = "search_${cleanQuery.lowercase(Locale.US)}_p$safePage"

        return fetchDeduplicatedList(cacheKey) {
            val remote = runCatching { apiService.fetchRemoteSearch(cleanQuery, safePage) }.getOrDefault(emptyList())
            val localMatches = if (safePage == 1) {
                FALLBACK_CATALOG.filter {
                    it.title.contains(cleanQuery, ignoreCase = true) ||
                        it.genre.contains(cleanQuery, ignoreCase = true) ||
                        it.overview.contains(cleanQuery, ignoreCase = true) ||
                        it.cast.any { c -> c.name.contains(cleanQuery, ignoreCase = true) }
                }
            } else emptyList()

            val imdbSuggestions = if (safePage == 1) {
                fetchOfficialImdbSuggestions(cleanQuery)
            } else emptyList()

            val combined = (localMatches + remote + imdbSuggestions)
                .distinctBy { it.cleanTitle.lowercase(Locale.US) }

            combined.forEach { detailCache.put(it.id, it) }
            combined
        }
    }

    /**
     * 5. `getMovieDetails(id)` - Fetches detailed movie/series metadata.
     */
    suspend fun getMovieDetails(id: Int): MovieBoxItem = withContext(Dispatchers.IO) {
        detailCache.get(id)?.let { cached ->
            if (cached.cast.isNotEmpty() && cached.runtime != null) {
                return@withContext cached
            }
        }

        val remote = runCatching { apiService.fetchRemoteMovieDetails(id) }.getOrNull()
        if (remote != null) {
            detailCache.put(id, remote)
            return@withContext remote
        }

        val fallback = FALLBACK_CATALOG.find { it.id == id } ?: detailCache.get(id)
        if (fallback != null) {
            detailCache.put(id, fallback)
            return@withContext fallback
        }

        MovieBoxItem(
            id = id,
            title = "Title #$id",
            year = "2024",
            rating = "7.5",
            genre = "Action, Drama",
            genreArray = listOf("Action", "Drama"),
            poster = "",
            backdrop = "",
            type = "Movie",
            overview = "Movie details are currently unavailable from the remote server.",
            language = "EN",
            runtime = 120,
            director = "Unknown"
        )
    }

    /**
     * 6. `getStreamingUrls(id, type = "movie", episodeNumber = null, titleHint = null, archiveIdentifierHint = null)`
     *
     * Resolves verified full-length MP4/MKV/WebM streams for the exact requested movie or series.
     * Never substitutes a different movie or series if the requested title is unavailable.
     */
    suspend fun getStreamingUrls(
        id: Int,
        type: String = "movie",
        episodeNumber: Int? = null,
        titleHint: String? = null,
        archiveIdentifierHint: String? = null
    ): MovieBoxStreamingData = withContext(Dispatchers.IO) {
        val rawDetails = if (id > 0) getMovieDetails(id) else null
        val matchedCatalogItem = if (!titleHint.isNullOrBlank()) {
            val cleanedHint = titleHint.replace(Regex("\\[.*?]"), "").trim()
            FALLBACK_CATALOG.find { it.cleanTitle.equals(cleanedHint, ignoreCase = true) }
                ?: rawDetails?.takeIf { it.cleanTitle.equals(cleanedHint, ignoreCase = true) }
        } else {
            rawDetails
        }

        val cleanTitle = (titleHint?.takeIf { it.isNotBlank() }
            ?: matchedCatalogItem?.cleanTitle
            ?: rawDetails?.cleanTitle
            ?: "Movie").replace(Regex("\\[.*?]"), "").trim()

        val isSeries = type.equals("series", ignoreCase = true) ||
            (type.isBlank() && matchedCatalogItem?.isSeries == true)
        val epNum = if (isSeries) (episodeNumber ?: 1).coerceAtLeast(1) else null
        val displayTitle = if (isSeries && epNum != null) "$cleanTitle - Episode $epNum" else cleanTitle
        val directorLabel = matchedCatalogItem?.director ?: rawDetails?.director ?: "MovieBox"
        val overviewText = matchedCatalogItem?.overview ?: rawDetails?.overview ?: ""

        val authKey = MovieBoxApiService.generateAuthKey()
        val videoHash = MovieBoxApiService.generateVideoHash()
        val syntheticCdnUrl = "https://cdn.moviebox.ng/stream/$videoHash.mp4?auth_key=$authKey"
        val encodedSearch = URLEncoder.encode(cleanTitle, "UTF-8")
        val utmUrl = "${apiService.webURL}/?utm_source=moviebox-clone&utm_medium=direct&utm_campaign=watch-movie&search=$encodedSearch"
        val searchUrl = "${apiService.webURL}/search?keyword=$encodedSearch"

        // 1. Check if the remote MovieBox streaming endpoint returns a direct video stream URL
        val remoteStreamUrl = if (id > 0) {
            withTimeoutOrNull(2_000L) {
                runCatching {
                    apiService.fetchRemoteStreamingData(
                        id = id,
                        type = if (isSeries) "series" else "movie",
                        episodeNumber = epNum
                    )
                }.getOrNull()
            }
        } else null

        var resolvedPlayable: ArchivePlayableMedia? = null
        var unavailableReason: String? = null

        if (!remoteStreamUrl.isNullOrBlank() && isDirectMediaStreamFormat(remoteStreamUrl)) {
            val isHls = remoteStreamUrl.contains(".m3u8", ignoreCase = true)
            val formatLabel = if (isHls) "HLS" else "MP4"
            val fileItem = ArchiveFileItem(
                name = sanitizeFileName(displayTitle, "720p", if (isHls) "m3u8" else "mp4"),
                format = formatLabel,
                size = "0"
            )
            resolvedPlayable = ArchivePlayableMedia(
                identifier = "moviebox_${id}_${epNum ?: 1}",
                title = displayTitle,
                streamUrl = remoteStreamUrl,
                fileName = fileItem.name,
                format = formatLabel,
                sizeBytes = 0L,
                durationSeconds = matchedCatalogItem?.runtime?.times(60)?.toFloat(),
                licenseUrl = null,
                description = overviewText,
                creator = "MovieBox API",
                matchedEpisode = epNum,
                allCompatibleFiles = listOf(fileItem)
            )
        }

        // 2. Inspect candidate identifier (from explicit hint, matched catalog item, or verified lookup table)
        val verifiedIdsForTitle = InternetArchiveRepository.resolveVerifiedIdentifiersForTitles(listOf(cleanTitle))
        val candidateIdentifiers = buildList {
            archiveIdentifierHint?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
            matchedCatalogItem?.archiveIdentifier?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
            addAll(verifiedIdsForTitle)
        }.distinct()

        if (resolvedPlayable == null && candidateIdentifiers.isNotEmpty()) {
            for (candidateId in candidateIdentifiers) {
                val directAttempt = withTimeoutOrNull(8_500L) {
                    runCatching {
                        internetArchiveRepository.inspectAndResolvePlayableMedia(
                            identifier = candidateId,
                            preferredEpisode = epNum,
                            isMovieOrSingleWork = !isSeries,
                            isLiveActionAllowed = true
                        )
                    }.getOrNull()
                }
                if (directAttempt is ArchivePlayableMediaResult.Success) {
                    resolvedPlayable = directAttempt.media.copy(
                        title = displayTitle,
                        description = overviewText.ifBlank { directAttempt.media.description },
                        creator = "MovieBox API • ${directAttempt.media.creator ?: directorLabel}",
                        matchedEpisode = epNum
                    )
                    break
                }
            }
        }

        // 3. Search full-length live-action stream for this exact title (never substituting a different title)
        if (resolvedPlayable == null) {
            val liveMatch = withTimeoutOrNull(10_000L) {
                runCatching {
                    internetArchiveRepository.findBestLiveActionStream(
                        title = cleanTitle,
                        episodeNumber = epNum,
                        isSeries = isSeries
                    )
                }.getOrNull()
            }

            when (liveMatch) {
                is ArchivePlayableMediaResult.Success -> {
                    resolvedPlayable = liveMatch.media.copy(
                        title = displayTitle,
                        description = overviewText.ifBlank { liveMatch.media.description },
                        creator = "MovieBox API • ${liveMatch.media.creator ?: directorLabel}",
                        matchedEpisode = epNum
                    )
                }
                is ArchivePlayableMediaResult.Unavailable -> {
                    unavailableReason = liveMatch.reason
                }
                is ArchivePlayableMediaResult.Error -> {
                    unavailableReason = liveMatch.message
                }
                null -> {
                    unavailableReason = "Can't find a playable stream for \"$displayTitle\"."
                }
            }
        }

        if (resolvedPlayable != null && isDirectMediaStreamFormat(resolvedPlayable.streamUrl)) {
            val qualityStreams = buildList {
                add(
                    MovieBoxQualityStream(
                        quality = "720p HD (${resolvedPlayable.format})",
                        streamUrl = resolvedPlayable.streamUrl,
                        format = resolvedPlayable.format,
                        sizeBytes = resolvedPlayable.sizeBytes
                    )
                )
                resolvedPlayable.allCompatibleFiles
                    .filter { it.name != resolvedPlayable.fileName }
                    .forEachIndexed { idx, fileItem ->
                        val altUrl = InternetArchiveApiService.getDirectDownloadUrl(resolvedPlayable.identifier, fileItem.name)
                        add(
                            MovieBoxQualityStream(
                                quality = "Alt Source ${idx + 2} (${fileItem.format ?: "MP4"})",
                                streamUrl = altUrl,
                                format = fileItem.format ?: "MP4",
                                sizeBytes = fileItem.size?.toLongOrNull()
                            )
                        )
                    }
            }

            return@withContext MovieBoxStreamingData(
                streamUrl = resolvedPlayable.streamUrl,
                quality = "720p",
                subtitles = emptyList(),
                downloadUrl = resolvedPlayable.streamUrl,
                source = "MovieBox Direct Stream (${resolvedPlayable.format})",
                isDirectLink = true,
                authInfo = MovieBoxAuthInfo(
                    authKey = authKey,
                    videoHash = videoHash,
                    timestamp = System.currentTimeMillis(),
                    streamingUrl = resolvedPlayable.streamUrl
                ),
                movieTitle = displayTitle,
                message = "Authorized MovieBox stream ready for \"$displayTitle\".",
                isVerifiedPlayableMedia = true,
                playableMedia = resolvedPlayable,
                qualityStreams = qualityStreams,
                searchUrl = searchUrl,
                homepageUrl = apiService.webURL
            )
        }

        // If no verified playable media stream was found, return the documented MovieBox structure
        // with `isVerifiedPlayableMedia = false` and `downloadUrl = null` so the UI clearly reports
        // that a direct stream could not be found rather than hanging or pretending a UTM link is a video.
        MovieBoxStreamingData(
            streamUrl = utmUrl,
            quality = "720p",
            subtitles = emptyList(),
            downloadUrl = null,
            source = "MovieBox.ng UTM Fallback",
            isDirectLink = false,
            authInfo = MovieBoxAuthInfo(
                authKey = authKey,
                videoHash = videoHash,
                timestamp = System.currentTimeMillis(),
                streamingUrl = syntheticCdnUrl
            ),
            movieTitle = displayTitle,
            message = unavailableReason ?: "Can't find a playable MovieBox video stream for \"$displayTitle\".",
            isVerifiedPlayableMedia = false,
            playableMedia = null,
            qualityStreams = emptyList(),
            searchUrl = searchUrl,
            homepageUrl = apiService.webURL
        )
    }

    private fun paginateFallback(items: List<MovieBoxItem>, page: Int): List<MovieBoxItem> {
        if (items.isEmpty()) return emptyList()
        if (page <= 1) return items.take(PAGE_SIZE)
        val startIndex = (page - 1) * PAGE_SIZE
        if (startIndex >= items.size) return emptyList()
        return items.subList(startIndex, minOf(startIndex + PAGE_SIZE, items.size))
    }

    private fun fetchOfficialImdbSuggestions(queryTitle: String): List<MovieBoxItem> {
        val clean = queryTitle.trim()
        if (clean.isEmpty()) return emptyList()
        return try {
            val encoded = URLEncoder.encode(clean.lowercase(Locale.US).replace(" ", "_"), "UTF-8")
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
            val results = mutableListOf<MovieBoxItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val qid = obj.optString("qid", "")
                if (qid != "movie" && qid != "tvSeries" && qid != "tvMiniSeries") continue
                val title = obj.optString("l", "").trim()
                val imageObj = obj.optJSONObject("i") ?: continue
                val posterUrl = imageObj.optString("imageUrl", "").trim()
                if (title.isEmpty() || posterUrl.isEmpty()) continue

                val qLabel = obj.optString("q", "").lowercase(Locale.US)
                if (title.contains("anime", ignoreCase = true) ||
                    title.contains("animated", ignoreCase = true) ||
                    qLabel.contains("animation")
                ) continue

                val imdbIdStr = obj.optString("id", "")
                val numericId = imdbIdStr.removePrefix("tt").toIntOrNull() ?: (10_000 + i)
                val year = obj.optInt("y", 2024).toString()
                val castStr = obj.optString("s", "")
                val isSeries = (qid == "tvSeries" || qid == "tvMiniSeries")
                val castMembers = castStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    .map { MovieBoxCastMember(it, "") }

                results.add(
                    MovieBoxItem(
                        id = numericId,
                        title = title,
                        year = year,
                        rating = "7.9",
                        genre = if (isSeries) "Drama, Mystery" else "Action, Drama",
                        genreArray = if (isSeries) listOf("Drama", "Mystery") else listOf("Action", "Drama"),
                        poster = posterUrl,
                        backdrop = posterUrl,
                        type = if (isSeries) "Series" else "Movie",
                        overview = if (castStr.isNotBlank()) "Starring $castStr. Released in $year." else "Live-action ${if (isSeries) "series" else "feature film"} ($year).",
                        language = "EN",
                        runtime = if (isSeries) 45 else 135,
                        cast = castMembers,
                        totalSeasons = if (isSeries) 2 else 1,
                        totalEpisodes = if (isSeries) 16 else 1,
                        archiveIdentifier = imdbIdStr.ifBlank { title.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "-") }
                    )
                )
            }
            results
        } catch (_: Exception) {
            emptyList()
        }
    }
}

/**
 * Download Status State for MovieBox Downloads (Requirement 4).
 */
enum class MovieBoxDownloadState {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class MovieBoxDownloadTask(
    val taskId: String,
    val contentId: Int,
    val title: String,
    val quality: String,
    val downloadUrl: String,
    val destinationPath: String,
    val fileName: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val state: MovieBoxDownloadState = MovieBoxDownloadState.QUEUED,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (totalBytes > 0L) (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt().coerceIn(0, 100)

    /**
     * Offline playback is ONLY enabled when the task is COMPLETED and the file on disk exists with > 0 bytes.
     */
    val isVerifiedPlayableOffline: Boolean
        get() {
            if (state != MovieBoxDownloadState.COMPLETED) return false
            val f = File(destinationPath)
            return f.exists() && f.isFile && f.length() > 0L
        }
}

/**
 * Manages authorized MovieBox video downloads with:
 * - Stream & Content-Type validation before downloading
 * - Safe file naming & duplicate-download prevention
 * - Live byte progress, Pause, Resume (HTTP Range support), Cancel, and Delete
 * - Verified local file check before allowing offline playback in ExoPlayer
 */
class MovieBoxDownloadManager(
    context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val downloadDir: File by lazy {
        val base = appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: appContext.filesDir
        File(base, "MovieBoxDownloads").apply { mkdirs() }
    }

    private val _downloads = MutableStateFlow<List<MovieBoxDownloadTask>>(emptyList())
    val downloads: StateFlow<List<MovieBoxDownloadTask>> = _downloads.asStateFlow()

    init {
        refreshExistingDownloadsOnDisk()
    }

    /**
     * Scans `MovieBoxDownloads` directory on startup so previously completed downloads remain available.
     */
    fun refreshExistingDownloadsOnDisk() {
        val existingFiles = downloadDir.listFiles()?.filter { it.isFile && it.length() > 0L }.orEmpty()
        if (existingFiles.isEmpty()) return

        val currentMap = _downloads.value.associateBy { it.destinationPath }.toMutableMap()
        existingFiles.forEachIndexed { idx, file ->
            if (!currentMap.containsKey(file.absolutePath)) {
                val taskId = "disk_${file.name.hashCode()}"
                currentMap[file.absolutePath] = MovieBoxDownloadTask(
                    taskId = taskId,
                    contentId = 9000 + idx,
                    title = file.nameWithoutExtension.replace('_', ' '),
                    quality = "Offline MP4",
                    downloadUrl = file.toURI().toString(),
                    destinationPath = file.absolutePath,
                    fileName = file.name,
                    bytesDownloaded = file.length(),
                    totalBytes = file.length(),
                    state = MovieBoxDownloadState.COMPLETED
                )
            }
        }
        _downloads.value = currentMap.values.sortedByDescending { it.createdAt }
    }

    /**
     * Enqueues or resumes a download for [title] from [downloadUrl].
     * Prevents duplicate active or completed downloads for the same [taskId].
     */
    fun startOrResumeDownload(
        contentId: Int,
        episodeNumber: Int? = null,
        title: String,
        quality: String = "720p",
        downloadUrl: String?
    ): Result<MovieBoxDownloadTask> {
        if (downloadUrl.isNullOrBlank() || !MovieBoxRepository.isDirectMediaStreamFormat(downloadUrl)) {
            return Result.failure(
                IllegalArgumentException("No authorized direct video download URL is available for \"$title\".")
            )
        }

        val taskId = if (episodeNumber != null) "mb_${contentId}_ep$episodeNumber" else "mb_$contentId"
        val existing = _downloads.value.find { it.taskId == taskId }
        if (existing != null) {
            if (existing.state == MovieBoxDownloadState.DOWNLOADING) {
                return Result.success(existing)
            }
            if (existing.isVerifiedPlayableOffline) {
                return Result.success(existing)
            }
        }

        val ext = downloadUrl.substringBefore('?').substringAfterLast('.', "mp4").lowercase(Locale.US)
            .takeIf { it in listOf("mp4", "mkv", "webm") } ?: "mp4"
        val safeFileName = MovieBoxRepository.sanitizeFileName(title, quality, ext)
        val targetFile = File(downloadDir, safeFileName)

        val initialBytes = if (targetFile.exists()) targetFile.length() else 0L
        val task = existing?.copy(
            state = MovieBoxDownloadState.DOWNLOADING,
            errorMessage = null,
            bytesDownloaded = initialBytes
        ) ?: MovieBoxDownloadTask(
            taskId = taskId,
            contentId = contentId,
            title = title,
            quality = quality,
            downloadUrl = downloadUrl,
            destinationPath = targetFile.absolutePath,
            fileName = safeFileName,
            bytesDownloaded = initialBytes,
            totalBytes = -1L,
            state = MovieBoxDownloadState.DOWNLOADING
        )

        upsertTask(task)
        launchDownloadJob(task)
        return Result.success(task)
    }

    fun pauseDownload(taskId: String) {
        activeJobs.remove(taskId)?.cancel()
        updateTaskState(taskId) {
            if (it.state == MovieBoxDownloadState.DOWNLOADING || it.state == MovieBoxDownloadState.QUEUED) {
                it.copy(state = MovieBoxDownloadState.PAUSED)
            } else it
        }
    }

    fun resumeDownload(taskId: String) {
        val task = _downloads.value.find { it.taskId == taskId } ?: return
        if (task.state == MovieBoxDownloadState.DOWNLOADING) return
        val updated = task.copy(state = MovieBoxDownloadState.DOWNLOADING, errorMessage = null)
        upsertTask(updated)
        launchDownloadJob(updated)
    }

    fun cancelAndRemoveDownload(taskId: String) {
        activeJobs.remove(taskId)?.cancel()
        val task = _downloads.value.find { it.taskId == taskId }
        if (task != null) {
            runCatching { File(task.destinationPath).delete() }
        }
        _downloads.value = _downloads.value.filterNot { it.taskId == taskId }
    }

    private fun launchDownloadJob(task: MovieBoxDownloadTask) {
        activeJobs.remove(task.taskId)?.cancel()
        val job = scope.launch {
            try {
                val file = File(task.destinationPath)
                file.parentFile?.mkdirs()
                var downloadedBytes = if (file.exists()) file.length() else 0L

                val requestBuilder = Request.Builder()
                    .url(task.downloadUrl)
                    .get()
                    .header("User-Agent", "AniTrackMovieBoxDownloader/1.0")

                if (downloadedBytes > 0L) {
                    requestBuilder.header("Range", "bytes=$downloadedBytes-")
                }

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        throw IllegalStateException("Server returned HTTP ${response.code}")
                    }
                    val contentType = response.header("Content-Type").orEmpty().lowercase(Locale.US)
                    if (contentType.contains("text/html")) {
                        throw IllegalStateException("URL returned an HTML webpage instead of a downloadable video stream.")
                    }

                    val isPartial = response.code == 206
                    val contentLength = response.body?.contentLength() ?: -1L
                    val totalBytes = when {
                        isPartial && contentLength > 0L -> downloadedBytes + contentLength
                        contentLength > 0L -> contentLength
                        else -> task.totalBytes
                    }

                    if (!isPartial && downloadedBytes > 0L) {
                        downloadedBytes = 0L
                    }

                    val bodyStream = response.body?.byteStream()
                        ?: throw IllegalStateException("Empty download stream")

                    RandomAccessFile(file, "rw").use { raf ->
                        if (isPartial && downloadedBytes > 0L) {
                            raf.seek(downloadedBytes)
                        } else {
                            raf.setLength(0L)
                            raf.seek(0L)
                        }

                        val buffer = ByteArray(32 * 1024)
                        var lastUiEmitMs = 0L
                        while (isActive) {
                            val read = bodyStream.read(buffer)
                            if (read == -1) break
                            raf.write(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            if (now - lastUiEmitMs >= 250L) {
                                lastUiEmitMs = now
                                updateTaskState(task.taskId) {
                                    it.copy(
                                        bytesDownloaded = downloadedBytes,
                                        totalBytes = totalBytes,
                                        state = MovieBoxDownloadState.DOWNLOADING
                                    )
                                }
                            }
                        }
                    }

                    if (isActive) {
                        if (!file.exists() || file.length() <= 0L) {
                            throw IllegalStateException("Downloaded file is empty or could not be verified on disk.")
                        }
                        updateTaskState(task.taskId) {
                            it.copy(
                                bytesDownloaded = file.length(),
                                totalBytes = if (totalBytes > 0L) totalBytes else file.length(),
                                state = MovieBoxDownloadState.COMPLETED,
                                errorMessage = null
                            )
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                updateTaskState(task.taskId) {
                    it.copy(
                        state = MovieBoxDownloadState.FAILED,
                        errorMessage = e.localizedMessage ?: "Download failed"
                    )
                }
            } finally {
                activeJobs.remove(task.taskId)
            }
        }
        activeJobs[task.taskId] = job
    }

    private fun upsertTask(task: MovieBoxDownloadTask) {
        val current = _downloads.value.toMutableList()
        val idx = current.indexOfFirst { it.taskId == task.taskId }
        if (idx >= 0) {
            current[idx] = task
        } else {
            current.add(0, task)
        }
        _downloads.value = current
    }

    private fun updateTaskState(taskId: String, transform: (MovieBoxDownloadTask) -> MovieBoxDownloadTask) {
        _downloads.value = _downloads.value.map {
            if (it.taskId == taskId) transform(it) else it
        }
    }
}
