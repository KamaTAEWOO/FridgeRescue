package com.portfolio.fridgerescue.feature.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.portfolio.fridgerescue.FridgeRescueApplication
import com.portfolio.fridgerescue.MainActivity
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.feature.intake.SharedIntakeCacheCleaner
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class ExpiryNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        SharedIntakeCacheCleaner.clean(applicationContext.cacheDir, Instant.now())
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val application = applicationContext as FridgeRescueApplication
        val foods = application.container.foodRepository.foodItems.first()
        val candidates = GetNotificationCandidatesUseCase()(foods, LocalDate.now())
        if (candidates.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_ID)
            return Result.success()
        }

        createChannel()
        val names = candidates.take(3).joinToString(", ") { it.name }
        val remaining = candidates.size - 3
        val body = if (remaining > 0) "$names 외 ${remaining}개" else names
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.notification_title, candidates.size))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = applicationContext.getString(R.string.notification_channel_description)
            },
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "daily-expiry-summary"
        private const val CHANNEL_ID = "expiry-summary"
        private const val NOTIFICATION_ID = 1001

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
