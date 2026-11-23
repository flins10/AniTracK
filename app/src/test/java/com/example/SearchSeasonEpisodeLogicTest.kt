package com.example

import com.example.data.local.AnimeEntity
import com.example.data.remote.AniListFuzzyDate
import com.example.data.remote.AniListMedia
import com.example.data.remote.AniListRelationEdge
import com.example.data.remote.AniListRelationsConnection
import com.example.data.remote.AniListTitle
import com.example.model.SeasonRules
import com.example.service.SeasonChainResolver
import com.example.util.EnglishTitleMapper
import com.example.util.EpisodeDataParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSeasonEpisodeLogicTest {

    private fun media(
        id: Int,
        english: String? = null,
        romaji: String? = null,
        format: String = "ONA",
        status: String = "RELEASING",
        episodes: Int? = null,
        duration: Int? = 20,
        year: Int? = 2024,
        month: Int? = 1,
        day: Int? = 1,
        country: String? = "CN",
        relations: List<AniListRelationEdge> = emptyList()
    ): AniListMedia {
        return AniListMedia(
            id = id,
            type = "ANIME",
            title = AniListTitle(romaji = romaji, english = english, userPreferred = english ?: romaji),
            format = format,
            status = status,
            episodes = episodes,
            duration = duration,
            seasonYear = year,
            countryOfOrigin = country,
            startDate = year?.let { AniListFuzzyDate(it, month, day) },
            relations = AniListRelationsConnection(edges = relations)
        )
    }

    @Test
    fun ongoingDonghuaS1_withChibiMoviePrologueSpecial_doesNotInflateToSeason5() = runBlocking {
        val chibi = media(
            id = 202,
            english = "Tales of Herding Gods Mini Theater",
            romaji = "Mu Shen Ji Xiao Juchang",
            format = "TV_SHORT",
            status = "FINISHED",
            episodes = 20,
            duration = 3,
            year = 2024,
            month = 11
        )
        val prologue = media(
            id = 203,
            english = "Tales of Herding Gods: Prologue",
            romaji = "Mu Shen Ji Xian Dao Pian",
            format = "ONA",
            status = "FINISHED",
            episodes = 2,
            duration = 15,
            year = 2024,
            month = 9
        )
        val movie = media(
            id = 204,
            english = "Tales of Herding Gods: The Movie",
            romaji = "Mu Shen Ji Juchangban",
            format = "MOVIE",
            status = "FINISHED",
            episodes = 1,
            duration = 95,
            year = 2025,
            month = 5
        )
        val pvSpecial = media(
            id = 205,
            english = "Tales of Herding Gods PV Special",
            romaji = "Mu Shen Ji Tebie Pian",
            format = "SPECIAL",
            status = "FINISHED",
            episodes = 1,
            duration = 5,
            year = 2024,
            month = 10
        )
        val shortBonusOna = media(
            id = 206,
            english = "Tales of Herding Gods: Extra Tales",
            romaji = "Mu Shen Ji Fanwai",
            format = "ONA",
            status = "FINISHED",
            episodes = 2,
            duration = 14,
            year = 2025,
            month = 1
        )
        val mainS1 = media(
            id = 201,
            english = "Tales of Herding Gods",
            romaji = "Mu Shen Ji",
            format = "ONA",
            status = "RELEASING",
            episodes = null,
            duration = 22,
            year = 2024,
            month = 10,
            relations = listOf(
                AniListRelationEdge("PREQUEL", prologue),
                AniListRelationEdge("SEQUEL", chibi),
                AniListRelationEdge("SEQUEL", movie),
                AniListRelationEdge("SEQUEL", pvSpecial),
                AniListRelationEdge("SEQUEL", shortBonusOna)
            )
        )

        val graph = listOf(mainS1, chibi, prologue, movie, pvSpecial, shortBonusOna).associateBy { it.id }
        val resolved = SeasonChainResolver.resolve(mainS1) { ids -> ids.mapNotNull { graph[it] } }

        // Ongoing S1 must NOT inflate into Season 5+ — only 1 real season exists, so switcher stays empty
        assertTrue("Expected empty season switcher for single ongoing S1, got ${resolved.seasons}", resolved.seasons.isEmpty())
        // Side entries must remain discoverable in bridgeRelations
        assertTrue(resolved.bridgeRelations.any { it.id == movie.id })
        assertTrue(resolved.bridgeRelations.any { it.id == prologue.id })
    }

    @Test
    fun multiSeasonWithPartSplitsAndMovieBridge_numbersSeasonsAndPartsAccurately() = runBlocking {
        val s1p1 = media(
            id = 301, english = "Epic Series Season 1", format = "TV", status = "FINISHED",
            episodes = 12, duration = 24, year = 2021, month = 1, country = "JP",
            relations = listOf(AniListRelationEdge("SEQUEL", media(302)))
        )
        val s1p2 = media(
            id = 302, english = "Epic Series Season 1 Part 2", format = "TV", status = "FINISHED",
            episodes = 12, duration = 24, year = 2021, month = 7, country = "JP",
            relations = listOf(
                AniListRelationEdge("PREQUEL", media(301)),
                AniListRelationEdge("SEQUEL", media(303))
            )
        )
        val movieBridge = media(
            id = 303, english = "Epic Series: The Movie", format = "MOVIE", status = "FINISHED",
            episodes = 1, duration = 105, year = 2022, month = 4, country = "JP",
            relations = listOf(
                AniListRelationEdge("PREQUEL", media(302)),
                AniListRelationEdge("SEQUEL", media(304))
            )
        )
        val s2p1 = media(
            id = 304, english = "Epic Series Season 2", format = "TV", status = "FINISHED",
            episodes = 12, duration = 24, year = 2023, month = 1, country = "JP",
            relations = listOf(
                AniListRelationEdge("PREQUEL", media(303)),
                AniListRelationEdge("SEQUEL", media(305))
            )
        )
        val s2p2 = media(
            id = 305, english = "Epic Series Season 2 Part 2", format = "TV", status = "RELEASING",
            episodes = 12, duration = 24, year = 2024, month = 1, country = "JP",
            relations = listOf(AniListRelationEdge("PREQUEL", media(304)))
        )

        val graph = listOf(s1p1, s1p2, movieBridge, s2p1, s2p2).associateBy { it.id }
        val resolved = SeasonChainResolver.resolve(s2p2) { ids -> ids.mapNotNull { graph[it] } }

        assertEquals(4, resolved.seasons.size)
        assertEquals(listOf(1, 1, 2, 2), resolved.seasons.map { it.seasonNumber })
        assertEquals(listOf(null, 2, null, 2), resolved.seasons.map { it.partNumber })
        assertEquals(listOf("1", "1 Pt 2", "2", "2 Pt 2"), resolved.seasons.map { it.shortLabel })
        assertEquals("Season 2 • Part 2", resolved.seasons.last().displayLabel)
        assertTrue(resolved.seasons.last().isCurrent)
        // Movie bridge excluded from seasons, included in bridgeRelations
        assertFalse(resolved.seasons.any { it.id == movieBridge.id })
        assertTrue(resolved.bridgeRelations.any { it.id == movieBridge.id })

        // Opening the Movie itself must NOT show a season switcher pretending the movie is a season
        val resolvedFromMovie = SeasonChainResolver.resolve(movieBridge) { ids -> ids.mapNotNull { graph[it] } }
        assertTrue(resolvedFromMovie.seasons.isEmpty())
    }

    @Test
    fun seasonRules_avoidsFalsePositivesAndParsesExplicitAndPinyinSeasons() {
        assertEquals(1, SeasonRules.inferSeasonFromTitle("Kaiju No. 8", "TV"))
        assertEquals(1, SeasonRules.inferSeasonFromTitle("Mob Psycho 100", "TV"))
        assertEquals(1, SeasonRules.inferSeasonFromTitle("Attack on Titan Part 2", "TV"))
        assertEquals(1, SeasonRules.inferSeasonFromTitle("Detective Conan Movie 27", "MOVIE"))
        assertEquals(1, SeasonRules.inferSeasonFromTitle("Renegade Immortal", "ONA"))
        assertEquals(2, SeasonRules.inferSeasonFromTitle("Soul Land II", "ONA"))
        assertEquals(3, SeasonRules.inferSeasonFromTitle("Battle Through the Heavens Season 3", "ONA"))
        assertEquals(4, SeasonRules.inferSeasonFromTitle("Doupo Cangqiong Di Si Ji", "ONA"))
        assertEquals(1, SeasonRules.resolveInitialSeason(storedSeason = 5, title = "Demon Slayer Movie", format = "MOVIE"))
    }

    @Test
    fun episodeDataParser_handlesMoviesNotYetReleasedOngoingDonghuaAndStaleCaps() {
        val now = 1790000000L

        // 1. Finished Movie with null episodes -> 1/1 finished (never 400+)
        val finishedMovie = EpisodeDataParser.parseAniListEpisodes(
            apiEpisodes = null,
            statusStr = "FINISHED",
            nextAiringEpisodeNumber = null,
            nextAiringAtUnix = null,
            mediaId = 999999,
            title = "Perfect World: Ashes of Fire",
            isDonghua = true,
            startYear = 2021,
            format = "MOVIE",
            nowSeconds = now
        )
        assertEquals(1, finishedMovie.totalEpisodes)
        assertEquals(1, finishedMovie.airedEpisodeCount)
        assertNull(finishedMovie.nextEpisodeNumber)
        assertTrue(finishedMovie.isFinished)

        // 2. Upcoming Movie -> 0 aired, 1 total, not finished
        val upcomingMovie = EpisodeDataParser.parseAniListEpisodes(
            apiEpisodes = null,
            statusStr = "NOT_YET_RELEASED",
            nextAiringEpisodeNumber = null,
            nextAiringAtUnix = null,
            format = "MOVIE",
            nowSeconds = now
        )
        assertEquals(0, upcomingMovie.airedEpisodeCount)
        assertEquals(1, upcomingMovie.totalEpisodes)
        assertFalse(upcomingMovie.isFinished)

        // 3. Brand-new ongoing Donghua on Ep 1 (nextAiringEpisode = 1) -> 0 aired, next = 1
        val ep1Donghua = EpisodeDataParser.parseAniListEpisodes(
            apiEpisodes = 52,
            statusStr = "RELEASING",
            nextAiringEpisodeNumber = 1,
            nextAiringAtUnix = now + 86400L,
            isDonghua = true,
            format = "ONA",
            nowSeconds = now
        )
        assertEquals(0, ep1Donghua.airedEpisodeCount)
        assertEquals(1, ep1Donghua.nextEpisodeNumber)
        assertEquals(52, ep1Donghua.totalEpisodes)

        // 4. Ongoing Donghua with stale lower apiEpisodes cap (52) while Ep 79 is next -> uncapped (null total), 78 aired
        val staleCapDonghua = EpisodeDataParser.parseAniListEpisodes(
            apiEpisodes = 52,
            statusStr = "RELEASING",
            nextAiringEpisodeNumber = 79,
            nextAiringAtUnix = now + 86400L,
            isDonghua = true,
            format = "ONA",
            nowSeconds = now
        )
        assertEquals(78, staleCapDonghua.airedEpisodeCount)
        assertNull(staleCapDonghua.totalEpisodes)
        assertEquals(79, staleCapDonghua.nextEpisodeNumber)

        // 5. AnimeEntity movie progress display & NOT_YET_RELEASED aired count
        val upcomingEntity = AnimeEntity(
            id = 5001,
            titleRomaji = "Upcoming Show",
            coverImage = "",
            status = "NOT_YET_RELEASED",
            format = "TV",
            currentEpisode = 0,
            totalEpisodes = 12
        )
        assertEquals(0, upcomingEntity.getAiredEpisodeCount())

        val movieEntity = AnimeEntity(
            id = 5002,
            titleRomaji = "Great Movie",
            coverImage = "",
            status = "FINISHED",
            format = "MOVIE",
            totalEpisodes = null,
            watchedEpisodeCount = 0
        )
        assertTrue(movieEntity.isMovie)
        assertEquals("Movie • 0 / 1", movieEntity.effectiveProgressDisplay)
    }

    @Test
    fun englishTitleMapper_expandSearchQueries_handlesStopWordsForTalesOf() {
        val expanded = EnglishTitleMapper.expandSearchQueries("Tales of")
        assertTrue(
            "Should expand to Tales of Herding Gods / Demons and Gods / Dark River pinyin, got $expanded",
            expanded.any { it.equals("Mu Shen Ji", ignoreCase = true) || it.equals("Yao Shen Ji", ignoreCase = true) || it.equals("An He Chuan", ignoreCase = true) }
        )
        assertTrue("Should stay compact (<= 2 extra alias queries for single batched request)", expanded.size in 1..2)
    }
}
