package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import java.time.LocalDate

class ScheduleUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(context)
            val repository = AnimeRepository(
                animeDao = database.animeDao(),
                historyDao = database.episodeHistoryDao(),
                apiCacheDao = database.apiCacheDao(),
                deletedAnimeDao = database.deletedAnimeDao(),
                savedAnimeDao = database.savedAnimeDao()
            )
            val result = repository.refreshSchedule()
            if (result.isSuccess) {
                checkAndNotifyUpcoming(context, database)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private suspend fun checkAndNotifyUpcoming(context: Context, database: AppDatabase) {
        val todayValue = LocalDate.now().dayOfWeek.value
        val nowSeconds = System.currentTimeMillis() / 1000
        val animeList = database.animeDao().getAllAnimeSync()
        val todayAnime = animeList.filter { it.inWatchlist && it.notificationsEnabled && it.airingDayOfWeek == todayValue }

        createNotificationChannel(context)

        for (anime in todayAnime) {
            val airingTime = anime.getEffectiveAiringTime(nowSeconds)
            val nextEp = anime.getEffectiveNextEpisodeNumber(nowSeconds)
            if (airingTime != null && nextEp != null && (airingTime - nowSeconds) in -1800L..3600L) {
                sendReleaseNotification(
                    context = context,
                    animeId = anime.id,
                    title = anime.displayTitle,
                    episodeNumber = nextEp
                )
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "anime_universe_release_notifications"
        const val CHANNEL_NAME = "Anime Universe Releases"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications when a tracked airing anime episode releases"
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun sendReleaseNotification(
            context: Context,
            animeId: Int,
            title: String,
            episodeNumber: Int
        ) {
            createNotificationChannel(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("New Episode Alert")
                .setContentText("$title Episode $episodeNumber is airing soon!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(animeId, builder.build())
        }
    }
}
