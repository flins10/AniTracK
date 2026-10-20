package com.example.data.repository

import com.example.data.local.AnimeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PlaybackType {
    DIRECT_EXO_PLAYER
}

data class StreamingServer(
    val id: String,
    val serverName: String,
    val language: String,
    val embedUrl: String,
    val playbackType: PlaybackType = PlaybackType.DIRECT_EXO_PLAYER,
    val archiveIdentifier: String? = null,
    val fileName: String? = null,
    val licenseUrl: String? = null,
    val format: String? = null,
    val matchConfidence: MatchConfidence = MatchConfidence.HIGH
)

data class StreamingSource(
    val providerName: String = "Internet Archive",
    val servers: List<StreamingServer>,
    val activeServer: StreamingServer?,
    val isAvailable: Boolean = true,
    val errorMessage: String? = null,
    val queriedTitles: List<String> = emptyList()
)

class StreamingSourceRepository(
    private val archiveRepository: InternetArchiveRepository = InternetArchiveRepository()
) {
    /**
     * Resolves genuine, compatible media streams from Internet Archive for the anime and episode.
     * All broken pirated web servers, placeholder embeds, and fake endpoints have been completely removed.
     */
    suspend fun getStreamingSource(anime: AnimeEntity, episodeNumber: Int): StreamingSource = withContext(Dispatchers.IO) {
        when (val matchResult = archiveRepository.findMatchesForAnime(anime, episodeNumber)) {
            is ArchiveAnimeMatchResult.Success -> {
                val servers = matchResult.allCandidates.mapIndexed { index, candidate ->
                    val epLabel = candidate.matchedEpisode?.let { "Ep $it" } ?: "Full Feature"
                    val formatLabel = candidate.bestPlayableFile.format ?: "MP4"
                    StreamingServer(
                        id = "archive_${candidate.identifier}_${candidate.bestPlayableFile.name}",
                        serverName = "Internet Archive ($formatLabel • $epLabel)",
                        language = if (candidate.isEpisodeSpecific) "Exact Episode Match" else "Archive Stream",
                        embedUrl = candidate.streamUrl,
                        playbackType = PlaybackType.DIRECT_EXO_PLAYER,
                        archiveIdentifier = candidate.identifier,
                        fileName = candidate.bestPlayableFile.name,
                        licenseUrl = candidate.licenseUrl,
                        format = formatLabel,
                        matchConfidence = candidate.matchConfidence
                    )
                }

                val activeServer = servers.firstOrNull()

                StreamingSource(
                    providerName = "Internet Archive",
                    servers = servers,
                    activeServer = activeServer,
                    isAvailable = true,
                    errorMessage = null
                )
            }
            is ArchiveAnimeMatchResult.NoMatch -> {
                StreamingSource(
                    providerName = "Internet Archive",
                    servers = emptyList(),
                    activeServer = null,
                    isAvailable = false,
                    errorMessage = matchResult.reason,
                    queriedTitles = matchResult.queriedTitles
                )
            }
            is ArchiveAnimeMatchResult.Error -> {
                StreamingSource(
                    providerName = "Internet Archive",
                    servers = emptyList(),
                    activeServer = null,
                    isAvailable = false,
                    errorMessage = matchResult.message
                )
            }
        }
    }
}
