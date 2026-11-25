package com.example

import com.example.data.remote.AniListFuzzyDate
import com.example.data.remote.AniListMedia
import com.example.data.remote.AniListRelationEdge
import com.example.data.remote.AniListRelationsConnection
import com.example.data.remote.AniListTitle
import com.example.model.FranchiseSeasonOverrideMode
import com.example.service.SeasonChainResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonChainResolverTest {

    private fun media(
        id: Int,
        title: String,
        format: String = "TV",
        status: String = "FINISHED",
        episodes: Int? = 12,
        year: Int? = null,
        month: Int? = null,
        day: Int? = 1,
        endYear: Int? = null,
        endMonth: Int? = null,
        endDay: Int? = null,
        edges: List<Pair<String, AniListMedia>> = emptyList()
    ): AniListMedia {
        return AniListMedia(
            id = id,
            type = "ANIME",
            title = AniListTitle(english = title, romaji = title, userPreferred = title),
            format = format,
            status = status,
            episodes = episodes,
            seasonYear = year,
            startDate = year?.let { AniListFuzzyDate(year = it, month = month ?: 1, day = day ?: 1) },
            endDate = endYear?.let { AniListFuzzyDate(year = it, month = endMonth ?: 1, day = endDay ?: 1) },
            relations = AniListRelationsConnection(
                edges = edges.map { (rel, target) ->
                    AniListRelationEdge(relationType = rel, node = target.copy(relations = null))
                }
            )
        )
    }

    @Test
    fun resolvesEightSeasonChainFromSeasonOneMiddleAndLast() = runBlocking {
        val rawNodes = (1..8).map { i ->
            media(id = 100 + i, title = "Hero Academy Season $i", year = 2016 + i)
        }
        val graph = rawNodes.mapIndexed { idx, node ->
            val edges = mutableListOf<Pair<String, AniListMedia>>()
            if (idx > 0) edges.add("PREQUEL" to rawNodes[idx - 1])
            if (idx < rawNodes.lastIndex) edges.add("SEQUEL" to rawNodes[idx + 1])
            node.id to node.copy(
                relations = AniListRelationsConnection(
                    edges.map { (rel, t) -> AniListRelationEdge(rel, t) }
                )
            )
        }.toMap()

        for (startIdx in listOf(0, 3, 7)) {
            val root = graph.getValue(101 + startIdx)
            val resolved = SeasonChainResolver.resolve(root) { ids ->
                ids.mapNotNull { graph[it] }
            }
            assertEquals(8, resolved.seasons.size)
            assertEquals((101..108).toList(), resolved.seasons.map { it.id })
            assertEquals((1..8).toList(), resolved.seasons.map { it.seasonNumber })
            assertTrue(resolved.seasons[startIdx].isCurrent)
        }
    }

    @Test
    fun traversesMultipleConsecutiveMovieBridgesWithoutLosingLaterSeasonsAndMergesMovies() = runBlocking {
        // Season 1 -> Movie1 -> Movie2 -> Movie3 -> Season 2
        val s1 = media(1, "Rascal S1", format = "TV", year = 2018)
        val m1 = media(2, "Dreaming Girl", format = "MOVIE", year = 2019)
        val m2 = media(3, "Sister Venturing Out", format = "MOVIE", year = 2023, month = 6)
        val m3 = media(4, "Knapsack Kid", format = "MOVIE", year = 2023, month = 12)
        val s2 = media(5, "Rascal Santa Claus", format = "TV", year = 2025)

        val graph = mapOf(
            1 to s1.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("SEQUEL", m1)))),
            2 to m1.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", s1), AniListRelationEdge("SEQUEL", m2)))),
            3 to m2.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", m1), AniListRelationEdge("SEQUEL", m3)))),
            4 to m3.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", m2), AniListRelationEdge("SEQUEL", s2)))),
            5 to s2.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", m3))))
        )

        val resolved = SeasonChainResolver.resolve(graph.getValue(1)) { ids -> ids.mapNotNull { graph[it] } }
        assertEquals(listOf(1, 5), resolved.seasons.map { it.id })
        assertEquals(listOf(1, 2), resolved.seasons.map { it.seasonNumber })
        assertEquals(listOf(2, 3, 4), resolved.moviesAndOvas.map { it.id })
        assertEquals(listOf("MOVIE", "MOVIE", "MOVIE"), resolved.moviesAndOvas.map { it.formatBadge })
    }

    @Test
    fun handlesBranchingMovieAndTvSequelsWithoutDroppingRestOfFranchise() = runBlocking {
        val s1 = media(10, "Demon Slayer", format = "TV", year = 2019)
        val movieDeadEnd = media(11, "Mugen Train Movie", format = "MOVIE", year = 2020)
        val s2 = media(12, "Mugen Train Arc", format = "TV", year = 2021, month = 10)
        val s3 = media(13, "Entertainment District", format = "TV", year = 2021, month = 12)
        val s4 = media(14, "Swordsmith Village", format = "TV", year = 2023)
        val s5 = media(15, "Hashira Training", format = "TV", year = 2024)

        val graph = mapOf(
            10 to s1.copy(
                relations = AniListRelationsConnection(
                    listOf(
                        AniListRelationEdge("SEQUEL", movieDeadEnd),
                        AniListRelationEdge("SEQUEL", s2)
                    )
                )
            ),
            11 to movieDeadEnd.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", s1)))),
            12 to s2.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", s1), AniListRelationEdge("SEQUEL", s3)))),
            13 to s3.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", s2), AniListRelationEdge("SEQUEL", s4)))),
            14 to s4.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", s3), AniListRelationEdge("SEQUEL", s5)))),
            15 to s5.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", s4))))
        )

        val resolved = SeasonChainResolver.resolve(graph.getValue(10)) { ids -> ids.mapNotNull { graph[it] } }
        assertEquals(listOf(10, 12, 13, 14, 15), resolved.seasons.map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 5), resolved.seasons.map { it.seasonNumber })
        assertEquals(listOf(11), resolved.moviesAndOvas.map { it.id })
    }

    @Test
    fun walksThroughSpecialBridgesToUpcomingSeasonWhilePlacingMoviesOvasSpecialsInDedicatedSection() = runBlocking {
        val s1 = media(21, "Titan S1", format = "TV", year = 2013)
        val s2 = media(22, "Titan S2", format = "TV", year = 2017)
        val finalSpecial = media(23, "Titan Final Season: The Final Chapters", format = "SPECIAL", year = 2023)
        val recapSpecial = media(24, "Titan Recap Special", format = "SPECIAL", year = 2018)
        val ovaEpisode = media(28, "Titan Lost Girls OVA", format = "OVA", year = 2017, month = 12)
        val upcomingTba = media(25, "Titan Season 5", format = "TV", status = "NOT_YET_RELEASED", year = null)
        val cancelled = media(26, "Titan Cancelled Project", format = "TV", status = "CANCELLED", year = 2026)
        val sideStory = media(27, "Titan Junior High", format = "TV", year = 2015)

        val graph = mapOf(
            21 to s1.copy(
                relations = AniListRelationsConnection(
                    listOf(
                        AniListRelationEdge("SEQUEL", s2),
                        AniListRelationEdge("SIDE_STORY", sideStory),
                        AniListRelationEdge("SIDE_STORY", ovaEpisode)
                    )
                )
            ),
            22 to s2.copy(
                relations = AniListRelationsConnection(
                    listOf(
                        AniListRelationEdge("PREQUEL", s1),
                        AniListRelationEdge("SEQUEL", recapSpecial),
                        AniListRelationEdge("SEQUEL", finalSpecial)
                    )
                )
            ),
            23 to finalSpecial.copy(
                relations = AniListRelationsConnection(
                    listOf(
                        AniListRelationEdge("PREQUEL", s2),
                        AniListRelationEdge("SEQUEL", upcomingTba),
                        AniListRelationEdge("SEQUEL", cancelled)
                    )
                )
            ),
            24 to recapSpecial.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", s2)))),
            25 to upcomingTba.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", finalSpecial)))),
            26 to cancelled.copy(relations = AniListRelationsConnection(listOf(AniListRelationEdge("PREQUEL", finalSpecial)))),
            27 to sideStory,
            28 to ovaEpisode
        )

        val resolved = SeasonChainResolver.resolve(graph.getValue(21)) { ids -> ids.mapNotNull { graph[it] } }
        // TV seasons 21, 22, and 25 (reached across finalSpecial bridge!) are numbered 1, 2, 3
        assertEquals(listOf(21, 22, 25), resolved.seasons.map { it.id })
        assertEquals(listOf(1, 2, 3), resolved.seasons.map { it.seasonNumber })
        // OVA and SPECIAL nodes are excluded from seasons and merged into moviesAndOvas sorted by release date
        assertEquals(listOf(28, 24, 23), resolved.moviesAndOvas.map { it.id })
        assertEquals(listOf("OVA", "SPECIAL", "SPECIAL"), resolved.moviesAndOvas.map { it.formatBadge })
        // TV side story (27) and cancelled (26) are excluded from seasons
        assertFalse(resolved.seasons.any { it.id == 26 || it.id == 27 })
    }

    @Test
    fun mergesArtificiallySplitContinuousRunsIntoOneSeasonAndSupportsManualSplitOverride() = runBlocking {
        // 5 split continuous entries of Tales of Herding Gods (26 eps each, <= 21 days gap) -> 1 Season with 130 eps
        val part1 = media(
            id = 501, title = "Tales of Herding Gods", format = "ONA", episodes = 26,
            year = 2024, month = 10, day = 27,
            endYear = 2025, endMonth = 4, endDay = 20
        )
        val part2 = media(
            id = 502, title = "Tales of Herding Gods 2", format = "ONA", episodes = 26,
            year = 2025, month = 4, day = 27,
            endYear = 2025, endMonth = 10, endDay = 19
        )
        val part3 = media(
            id = 503, title = "Tales of Herding Gods 3", format = "ONA", episodes = 26,
            year = 2025, month = 10, day = 26,
            endYear = 2026, endMonth = 4, endDay = 19
        )
        val part4 = media(
            id = 504, title = "Tales of Herding Gods 4", format = "ONA", episodes = 26,
            year = 2026, month = 4, day = 26,
            endYear = 2026, endMonth = 10, endDay = 18
        )
        val part5 = media(
            id = 505, title = "Tales of Herding Gods 5", format = "ONA", status = "RELEASING", episodes = 26,
            year = 2026, month = 10, day = 25
        )

        val rawList = listOf(part1, part2, part3, part4, part5)
        val graph = rawList.mapIndexed { idx, node ->
            val edges = mutableListOf<AniListRelationEdge>()
            if (idx > 0) edges.add(AniListRelationEdge("PREQUEL", rawList[idx - 1]))
            if (idx < rawList.lastIndex) edges.add(AniListRelationEdge("SEQUEL", rawList[idx + 1]))
            node.id to node.copy(relations = AniListRelationsConnection(edges))
        }.toMap()

        // 1. AUTO mode merges all 5 continuous parts into 1 single SEASON 1 pill with 130 episodes
        val autoResolved = SeasonChainResolver.resolve(
            rootMedia = graph.getValue(501),
            overrideMode = FranchiseSeasonOverrideMode.AUTO
        ) { ids -> ids.mapNotNull { graph[it] } }

        assertEquals(1, autoResolved.seasons.size)
        assertEquals(5, autoResolved.rawMainSeasonNodeCount)
        val mergedSeason = autoResolved.seasons.first()
        assertEquals(1, mergedSeason.seasonNumber)
        assertEquals("1", mergedSeason.shortLabel)
        assertEquals(130, mergedSeason.episodes)
        assertTrue(mergedSeason.isMergedContinuousRun)
        assertEquals(listOf(0, 26, 52, 78, 104), mergedSeason.subEntries.map { it.startEpisodeOffset })

        // 2. Manual override SPLIT_ALL splits them into 5 separate numbered seasons 1..5
        val splitResolved = SeasonChainResolver.resolve(
            rootMedia = graph.getValue(501),
            overrideMode = FranchiseSeasonOverrideMode.SPLIT_ALL
        ) { ids -> ids.mapNotNull { graph[it] } }

        assertEquals(5, splitResolved.seasons.size)
        assertEquals(listOf(1, 2, 3, 4, 5), splitResolved.seasons.map { it.seasonNumber })
    }
}
