package com.example

import com.example.auth.SupabaseAuthService
import com.example.config.AppConfig
import com.example.data.local.AnimeEntity
import com.example.model.BuiltInAvatar
import com.example.model.MemberStatus
import com.example.model.ProgressRules
import com.example.model.SeasonRules
import com.example.model.SentimentCategory
import com.example.model.StarRatingUtils
import com.example.model.VisibilityLevel
import com.example.model.WatchStatus
import com.example.util.ErrorSanitizer
import com.example.util.SecureStorageCodec
import com.example.viewmodel.DiscoverFilterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun englishAndRomajiBothExist_displaysEnglish() {
        val entity = AnimeEntity(
            id = 100,
            titleEnglish = "Frieren: Beyond Journey's End",
            titleRomaji = "Sousou no Frieren",
            coverImage = "",
            airingDayOfWeek = 1
        )
        assertEquals("Frieren: Beyond Journey's End", entity.displayTitle)
    }

    @Test
    fun englishIsNullOrBlank_displaysRomaji() {
        val nullEnglish = AnimeEntity(
            id = 101,
            titleEnglish = null,
            titleRomaji = "Unlocalized Show",
            coverImage = "",
            airingDayOfWeek = 1
        )
        val blankEnglish = AnimeEntity(
            id = 102,
            titleEnglish = "  ",
            titleRomaji = "Unlocalized Show",
            coverImage = "",
            airingDayOfWeek = 1
        )
        assertEquals("Unlocalized Show", nullEnglish.displayTitle)
        assertEquals("Unlocalized Show", blankEnglish.displayTitle)
    }

    @Test
    fun donghuaWithEnglishTitle_displaysEnglish() {
        val donghua = AnimeEntity(
            id = 104,
            titleEnglish = "Soul Land 2",
            titleRomaji = "Douluo Dalu II",
            titlePinyin = "Douluo Dalu 2",
            countryOfOrigin = "CN",
            coverImage = "",
            airingDayOfWeek = 1
        )
        assertEquals("Soul Land 2", donghua.displayTitle)
    }

    @Test
    fun progressRules_clampsEpisodeBoundsAndDetectsCompletion() {
        assertEquals(0, ProgressRules.clampEpisodeProgress(-4, 12))
        assertEquals(12, ProgressRules.clampEpisodeProgress(99, 12))
        assertEquals(15, ProgressRules.clampEpisodeProgress(15, null))

        val (ep5, done5) = ProgressRules.incrementEpisode(4, 12)
        assertEquals(5, ep5)
        assertFalse(done5)

        val (ep12, done12) = ProgressRules.incrementEpisode(11, 12)
        assertEquals(12, ep12)
        assertTrue(done12)

        val (epStill12, doneStill12) = ProgressRules.incrementEpisode(12, 12)
        assertEquals(12, epStill12)
        assertTrue(doneStill12)
    }

    @Test
    fun builtInAvatars_hasNinePresetsAndFallback() {
        assertEquals(9, BuiltInAvatar.entries.size)
        assertEquals(BuiltInAvatar.AVATAR_1, BuiltInAvatar.fromKey("unknown_key"))
        assertEquals(BuiltInAvatar.AVATAR_5, BuiltInAvatar.fromKey("avatar_5"))
    }

    @Test
    fun watchStatusAndVisibility_mapDbValuesSafely() {
        assertEquals(WatchStatus.WATCHING, WatchStatus.fromDbValue("watching"))
        assertEquals(WatchStatus.COMPLETED, WatchStatus.fromDbValue("completed"))
        assertEquals(WatchStatus.PLAN_TO_WATCH, WatchStatus.fromDbValue("plan_to_watch"))
        assertEquals(WatchStatus.ON_HOLD, WatchStatus.fromDbValue("on_hold"))
        assertEquals(WatchStatus.DROPPED, WatchStatus.fromDbValue("dropped"))
        assertEquals(VisibilityLevel.PRIVATE, VisibilityLevel.fromDbValue(null))
        assertEquals(VisibilityLevel.COMMUNITY, VisibilityLevel.fromDbValue("community"))
        assertEquals(MemberStatus.NONE, MemberStatus.fromDbValue(null))
        assertEquals(MemberStatus.ACTIVE, MemberStatus.fromDbValue("active"))
        assertEquals(MemberStatus.LEFT, MemberStatus.fromDbValue("left"))
        assertEquals(MemberStatus.REMOVED, MemberStatus.fromDbValue("removed"))
    }

    @Test
    fun discoverFilterState_countsActiveFiltersAccurately() {
        val defaultState = DiscoverFilterState()
        assertEquals(0, defaultState.activeFilterCount)

        val filtered = DiscoverFilterState(
            status = "RELEASING",
            season = "SPRING",
            selectedGenres = setOf("Action", "Sci-Fi")
        )
        assertEquals(3, filtered.activeFilterCount)
    }

    @Test
    fun appConfig_communityCapIsFifty() {
        assertEquals(50, AppConfig.MAX_COMMUNITY_MEMBERS)
        assertEquals("https://graphql.anilist.co", AppConfig.ANILIST_GRAPHQL_URL)
    }

    @Test
    fun appConfig_runtimeSupabaseConfigValidation() {
        assertTrue(AppConfig.isValidUrl("https://exampleproj.supabase.co"))
        assertFalse(AppConfig.isValidUrl("http://insecure.supabase.co"))
        assertFalse(AppConfig.isValidUrl("https://your-project.supabase.co"))
        assertTrue(AppConfig.isValidKey("sb_publishable_1234567890abcdef"))
        assertFalse(AppConfig.isValidKey("short"))

        val prevUrl = AppConfig.runtimeSupabaseUrl
        val prevKey = AppConfig.runtimeSupabaseAnonKey
        try {
            AppConfig.updateRuntimeSupabaseConfig("https://exampleproj.supabase.co/", "sb_publishable_1234567890abcdef")
            assertEquals("https://exampleproj.supabase.co", AppConfig.runtimeSupabaseUrl)
            assertEquals("sb_publishable_1234567890abcdef", AppConfig.runtimeSupabaseAnonKey)
            assertTrue(AppConfig.isSupabaseConfigured)

            if (!AppConfig.hasBuiltInVaultConfig) {
                AppConfig.updateRuntimeSupabaseConfig("http://insecure.supabase.co", "short")
                assertFalse(AppConfig.isSupabaseConfigured)
            }
        } finally {
            AppConfig.updateRuntimeSupabaseConfig(prevUrl, prevKey)
        }
    }

    @Test
    fun supabaseAuth_usesDeepLinkRedirectUri() {
        assertEquals("animeuniverse://auth-callback", SupabaseAuthService.AUTH_REDIRECT_URI)
    }

    @Test
    fun starRatingUtils_convertsBetweenScoreRatingAndFiveStars() {
        assertEquals(0f, StarRatingUtils.score100ToStars(null), 0.001f)
        assertEquals(0f, StarRatingUtils.score100ToStars(0), 0.001f)
        assertEquals(5.0f, StarRatingUtils.score100ToStars(100), 0.001f)
        assertEquals(4.5f, StarRatingUtils.score100ToStars(90), 0.001f)
        assertEquals(4.0f, StarRatingUtils.rating10ToStars(8.0), 0.001f)
        assertEquals(3.5f, StarRatingUtils.rating10ToStars(7.0), 0.001f)
        assertEquals(90, StarRatingUtils.starsToScore100(4.5f))
        assertEquals(100, StarRatingUtils.starsToScore100(5.0f))
        assertEquals(SentimentCategory.CINEMA, SentimentCategory.fromStars(4.5f))
        assertEquals(SentimentCategory.WORTH, SentimentCategory.fromStars(4.0f))
        assertEquals("4.5", StarRatingUtils.formatStars(4.5f))
    }

    @Test
    fun errorSanitizer_stripsHttpCodesAndTechnicalNoise() {
        val http300 = ErrorSanitizer.sanitize("Request failed with HTTP 300 done", "Unable to sign in.")
        assertFalse(http300.contains("300"))
        assertFalse(http300.contains("HTTP", ignoreCase = true))

        val anilist502 = ErrorSanitizer.sanitize("AniList HTTP 502 Bad Gateway", "Unable to load catalog.")
        assertFalse(anilist502.contains("502"))
        assertFalse(anilist502.contains("HTTP", ignoreCase = true))

        val duplicateUser = ErrorSanitizer.sanitize("duplicate key value violates unique constraint profiles_username_key (23505)")
        assertTrue(duplicateUser.contains("already taken", ignoreCase = true))

        val timeoutMsg = ErrorSanitizer.sanitize("java.net.SocketTimeoutException: failed to connect to https://example.com")
        assertFalse(timeoutMsg.contains("Exception"))
        assertFalse(timeoutMsg.contains("https://"))

        val jsonFallback = ErrorSanitizer.sanitize("{\"code\":400,\"msg\":\"error_description\"}", "Could not complete request.")
        assertEquals("Could not complete request.", jsonFallback)
    }

    @Test
    fun secureStorageCodec_encryptsAndDecryptsRoundTrip() {
        val secret = "sb_publishable_very_secret_sample_token_12345"
        val encoded = SecureStorageCodec.encrypt(secret)
        assertNotEquals(secret, encoded)
        assertFalse(encoded.contains("sample_token"))
        assertEquals(secret, SecureStorageCodec.decrypt(encoded))
    }

    @Test
    fun errorSanitizer_masksEmailPreservingDomainAndPrefix() {
        assertEquals("ws*******@gmail.com", ErrorSanitizer.maskEmail("wslogin69@gmail.com"))
        assertEquals("x****@gmail.com", ErrorSanitizer.maskEmail("xy@gmail.com"))
        assertEquals("", ErrorSanitizer.maskEmail(""))
        assertEquals("", ErrorSanitizer.maskEmail("notanemail"))
    }

    @Test
    fun deletionCooldownHelpers_normalizeHashAndFormatMinutes() {
        assertEquals(5L * 60L * 1000L, SupabaseAuthService.EMAIL_COOLDOWN_MILLIS)
        assertEquals(24L * 60L * 60L * 1000L, SupabaseAuthService.USERNAME_COOLDOWN_MILLIS)

        assertEquals(
            SupabaseAuthService.sha256Hex("WsLogin69@Gmail.com"),
            SupabaseAuthService.sha256Hex("  wslogin69@gmail.com ")
        )
        assertEquals("1 minute", SupabaseAuthService.formatRemainingMinutes(1L))
        assertEquals("1 minute", SupabaseAuthService.formatRemainingMinutes(60L))
        assertEquals("5 minutes", SupabaseAuthService.formatRemainingMinutes(299L))
    }

    @Test
    fun seasonRules_infersAndClampsSeasonsCorrectly() {
        assertEquals(3, SeasonRules.inferSeasonFromTitle("A Will Eternal 3"))
        assertEquals(2, SeasonRules.inferSeasonFromTitle("Demon Slayer: Entertainment District Season 2"))
        assertEquals(3, SeasonRules.inferSeasonFromTitle("Mob Psycho 100 III"))
        assertEquals(2, SeasonRules.inferSeasonFromTitle("All Saints Street Part 2"))
        assertEquals(1, SeasonRules.inferSeasonFromTitle("Solo Leveling (2024)"))
        assertEquals(1, SeasonRules.inferSeasonFromTitle("Steins;Gate"))

        assertEquals(1, SeasonRules.clampSeason(0))
        assertEquals(1, SeasonRules.clampSeason(-5))
        assertEquals(99, SeasonRules.clampSeason(250))

        val season3Entity = AnimeEntity(
            id = 555,
            titleEnglish = "Kingdom 3",
            titleRomaji = "Kingdom 3rd Season",
            coverImage = "",
            userSeason = 3,
            watchedEpisodeCount = 12,
            totalEpisodes = 26,
            inWatchlist = true
        )
        assertEquals(3, season3Entity.effectiveSeasonNumber)
        assertEquals("S3 • 12 / 26 eps", season3Entity.effectiveProgressDisplay)

        val season1Entity = season3Entity.copy(
            titleEnglish = "Kingdom",
            titleRomaji = "Kingdom",
            userSeason = 1
        )
        assertEquals(1, season1Entity.effectiveSeasonNumber)
        assertEquals("12 / 26 eps", season1Entity.effectiveProgressDisplay)
    }

    @Test
    fun animePartCategory_classifiesAndFormatsOtherPartsCorrectly() {
        assertEquals(
            com.example.model.AnimePartCategory.MOVIE,
            com.example.model.AnimePartCategory.classify("SEQUEL", "MOVIE")
        )
        assertEquals(
            com.example.model.AnimePartCategory.SPIN_OFF,
            com.example.model.AnimePartCategory.classify("SPIN_OFF", "TV")
        )
        assertEquals(
            com.example.model.AnimePartCategory.SPIN_OFF,
            com.example.model.AnimePartCategory.classify("CHARACTER", "TV_SHORT")
        )
        assertEquals(
            com.example.model.AnimePartCategory.SIDE_STORY,
            com.example.model.AnimePartCategory.classify("SIDE_STORY", "ONA")
        )
        assertEquals(
            com.example.model.AnimePartCategory.OVA_SPECIAL,
            com.example.model.AnimePartCategory.classify("SIDE_STORY", "SIDE_STORY")
                .let { com.example.model.AnimePartCategory.classify("ALTERNATIVE", "OVA") }
        )
        assertEquals(
            com.example.model.AnimePartCategory.OTHER,
            com.example.model.AnimePartCategory.classify("SUMMARY", "TV")
        )

        assertEquals("MOVIE", com.example.model.AnimePartCategory.badgeLabel("SEQUEL", "MOVIE"))
        assertEquals("SPIN-OFF", com.example.model.AnimePartCategory.badgeLabel("SPIN_OFF", "TV"))
        assertEquals("SIDE STORY", com.example.model.AnimePartCategory.badgeLabel("SIDE_STORY", "TV"))
        assertEquals("OVA", com.example.model.AnimePartCategory.badgeLabel("OTHER", "OVA"))
        assertEquals("SPECIAL", com.example.model.AnimePartCategory.badgeLabel("OTHER", "SPECIAL"))
        assertEquals("SUMMARY", com.example.model.AnimePartCategory.badgeLabel("SUMMARY", "TV"))
        assertEquals("ALTERNATIVE", com.example.model.AnimePartCategory.badgeLabel("ALTERNATIVE", "TV"))
    }

    @Test
    fun getRichDetailWithSeasons_returnsEmptyForStandaloneAndNumberedChainForMultiSeason() = kotlinx.coroutines.runBlocking {
        fun mediaNode(
            id: Int,
            title: String,
            format: String = "TV",
            type: String = "ANIME",
            edges: List<com.example.data.remote.AniListRelationEdge> = emptyList()
        ) = com.example.data.remote.AniListMedia(
            id = id,
            type = type,
            title = com.example.data.remote.AniListTitle(english = title, romaji = title),
            coverImage = com.example.data.remote.AniListCoverImage(large = "https://img/$id.jpg"),
            format = format,
            status = "FINISHED",
            episodes = if (format == "MOVIE") 1 else 12,
            seasonYear = 2020 + (id % 5),
            relations = com.example.data.remote.AniListRelationsConnection(edges = edges)
        )

        fun edge(relType: String, target: com.example.data.remote.AniListMedia) =
            com.example.data.remote.AniListRelationEdge(relationType = relType, node = target)

        // Graph A: Standalone single-season anime with a spin-off and movie, but no TV sequel/prequel
        val standaloneMovie = mediaNode(201, "Solo Side Movie", format = "MOVIE")
        val standaloneSpinOff = mediaNode(202, "Solo Spin-Off", format = "TV_SHORT")
        val standaloneRoot = mediaNode(
            200,
            "Solo Series",
            format = "TV",
            edges = listOf(
                edge("SIDE_STORY", standaloneMovie),
                edge("SPIN_OFF", standaloneSpinOff)
            )
        )

        // Graph B: Multi-season chain: S1 -> Bridge Movie -> S2 -> S3, plus a MANGA adaptation
        val mangaRef = mediaNode(999, "Manga Adaptation", format = "MANGA", type = "MANGA")
        val s1Stub = mediaNode(301, "Saga Season 1", format = "TV")
        val movieBridgeStub = mediaNode(302, "Saga The Movie", format = "MOVIE")
        val s2Stub = mediaNode(303, "Saga Season 2", format = "TV")
        val s3Stub = mediaNode(304, "Saga Season 3", format = "TV")
        val ovaPart = mediaNode(305, "Saga OVA", format = "OVA")

        val s1Full = mediaNode(301, "Saga Season 1", format = "TV", edges = listOf(edge("SEQUEL", movieBridgeStub), edge("ADAPTATION", mangaRef)))
        val movieFull = mediaNode(302, "Saga The Movie", format = "MOVIE", edges = listOf(edge("PREQUEL", s1Stub), edge("SEQUEL", s2Stub)))
        val s2Full = mediaNode(303, "Saga Season 2", format = "TV", edges = listOf(edge("PREQUEL", movieBridgeStub), edge("SEQUEL", s3Stub), edge("SIDE_STORY", ovaPart)))
        val s3Full = mediaNode(304, "Saga Season 3", format = "TV", edges = listOf(edge("PREQUEL", s2Stub)))

        val nodeMap = mapOf(
            200 to standaloneRoot,
            201 to standaloneMovie,
            202 to standaloneSpinOff,
            301 to s1Full,
            302 to movieFull,
            303 to s2Full,
            304 to s3Full,
            305 to ovaPart
        )

        val fakeService = object : com.example.data.remote.AniListApiService {
            override suspend fun queryMedia(request: com.example.data.remote.AniListRequest) =
                throw UnsupportedOperationException()

            override suspend fun querySchedule(request: com.example.data.remote.AniListRequest) =
                throw UnsupportedOperationException()

            override suspend fun querySingleMedia(request: com.example.data.remote.AniListRequest):
                retrofit2.Response<com.example.data.remote.AniListResponseData<com.example.data.remote.AniListSingleMediaData>> {
                val id = (request.variables["id"] as? Number)?.toInt()
                return retrofit2.Response.success(
                    com.example.data.remote.AniListResponseData(
                        data = com.example.data.remote.AniListSingleMediaData(Media = id?.let { nodeMap[it] })
                    )
                )
            }
        }

        val provider = com.example.service.AniListMetadataProvider(fakeService)

        // 1. Standalone anime with zero seasons -> empty seasons list, relations kept
        val soloDetail = provider.getRichDetailWithSeasons(standaloneRoot)
        assertTrue(soloDetail.seasons.isEmpty())
        assertEquals(listOf(201, 202), soloDetail.relations.map { it.id })

        // 2. Opening S2 resolves [1, 2, 3] with S2 marked isCurrent = true, Movie bridge included, MANGA filtered out
        val s2Detail = provider.getRichDetailWithSeasons(s2Full)
        assertEquals(listOf(1, 2, 3), s2Detail.seasons.map { it.seasonNumber })
        assertEquals(listOf(301, 303, 304), s2Detail.seasons.map { it.id })
        assertFalse(s2Detail.seasons[0].isCurrent)
        assertTrue(s2Detail.seasons[1].isCurrent)
        assertFalse(s2Detail.seasons[2].isCurrent)
        assertTrue(s2Detail.relations.any { it.id == 302 })
        assertTrue(s2Detail.relations.any { it.id == 305 })
        assertFalse(s2Detail.relations.any { it.id == 999 })
    }

    @Test
    fun appUpdateInfo_comparesVersionCodeAndSemanticVersionAccurately() {
        assertTrue(
            com.example.service.AppUpdateInfo.isRemoteNewer(
                currentVersionCode = 3,
                currentVersionName = "2.1.0",
                remoteVersionCode = 4,
                remoteVersionName = "2.2.0"
            )
        )
        assertFalse(
            com.example.service.AppUpdateInfo.isRemoteNewer(
                currentVersionCode = 3,
                currentVersionName = "2.1.0",
                remoteVersionCode = 3,
                remoteVersionName = "2.1.0"
            )
        )
        assertTrue(
            com.example.service.AppUpdateInfo.isRemoteNewer(
                currentVersionCode = 3,
                currentVersionName = "2.1.0",
                remoteVersionCode = 3,
                remoteVersionName = "2.1.1"
            )
        )
        assertFalse(
            com.example.service.AppUpdateInfo.isRemoteNewer(
                currentVersionCode = 3,
                currentVersionName = "2.1.0",
                remoteVersionCode = 2,
                remoteVersionName = "2.0.0"
            )
        )
    }

    @Test
    fun buildGoogleOAuthUrl_encodesCustomRedirectSchemeProperly() {
        val url = com.example.auth.SupabaseAuthService.buildGoogleOAuthUrl(
            baseUrl = "https://demo.supabase.co/",
            redirectUri = "animeuniverse://auth-callback"
        )
        assertEquals(
            "https://demo.supabase.co/auth/v1/authorize?provider=google&redirect_to=animeuniverse%3A%2F%2Fauth-callback",
            url
        )
    }
}

