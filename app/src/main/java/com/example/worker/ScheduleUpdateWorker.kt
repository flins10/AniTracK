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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ScheduleUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(context)
            val repository = AnimeRepository(database.animeDao(), database.episodeHistoryDao(), database.apiCacheDao(), database.deletedAnimeDao())
            // Execute live schedule & countdown refresh
            val result = repository.refreshSchedule()
            if (result.isSuccess) {
                // Check and trigger notifications for today's releases
                checkAndNotifyUpcoming(context, database)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun checkAndNotifyUpcoming(context: Context, database: AppDatabase) {
        val todayValue = LocalDate.now().dayOfWeek.value
        val nowSeconds = System.currentTimeMillis() / 1000
        val animeList = database.animeDao().getAllAnimeSync()
        val todayAnime = animeList.filter { it.inWatchlist && it.airingDayOfWeek == todayValue }

        createNotificationChannel(context)

        for (anime in todayAnime) {
            val airingTime = anime.getEffectiveAiringTime(nowSeconds)
            val nextEp = anime.getEffectiveNextEpisodeNumber(nowSeconds)
            // If episode airs within 1 hour or aired within last 30 minutes, notify user
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
        const val CHANNEL_ID = "anitrack_release_notifications"
        const val CHANNEL_NAME = "AniTrAk Episode Releases"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications when a tracked seasonal anime episode is released"
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
                .setContentTitle("New Episode Release Alert!")
                .setContentText("$title Episode $episodeNumber is now airing!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(animeId, builder.build())
        }
    }
}
