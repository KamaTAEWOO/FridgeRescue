package com.portfolio.fridgerescue.feature.notification.worker

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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.portfolio.fridgerescue.core.domain.repository.FoodRepository
import com.portfolio.fridgerescue.app.MainActivity
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.feature.intake.data.SharedIntakeCacheCleaner
import com.portfolio.fridgerescue.feature.notification.domain.GetNotificationCandidatesUseCase
import com.portfolio.fridgerescue.feature.notification.domain.GetStaleFoodCandidatesUseCase
import com.portfolio.fridgerescue.feature.notification.domain.NotificationSettingsRepository
import com.portfolio.fridgerescue.feature.notification.domain.QuietHoursPolicy
import java.time.Instant
import java.util.UUID
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 임박 식재료를 하루 한 번 요약하고, 오래 확인하지 않은 항목은 재검토 상태로 바꾼다.
 * CoroutineWorker와 Hilt 주입을 사용해 백그라운드 작업도 UI와 동일한 Repository를 공유한다.
 */
@HiltWorker
class ExpiryNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: FoodRepository,
    private val settingsRepository: NotificationSettingsRepository,
    private val quietHoursPolicy: QuietHoursPolicy,
    private val getStaleFoods: GetStaleFoodCandidatesUseCase,
    private val getNotificationCandidates: GetNotificationCandidatesUseCase,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        SharedIntakeCacheCleaner.clean(applicationContext.cacheDir, Instant.now())
        val settings = settingsRepository.settings.first()
        val quietDelay = quietHoursPolicy.delayUntilAllowed(ZonedDateTime.now(), settings)
        if (quietDelay != null) {
            scheduleAfterQuietHours(quietDelay)
            return Result.success()
        }
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

        val initialFoods = repository.foodItems.first()
        val staleFoods = getStaleFoods(
            foods = initialFoods,
            events = repository.events.first(),
            now = Instant.now(),
        )
        if (staleFoods.isNotEmpty()) {
            repository.saveAll(
                foodItems = staleFoods.map { it.copy(status = com.portfolio.fridgerescue.core.domain.model.FoodStatus.NEEDS_REVIEW) },
                operationId = "stale-review:${LocalDate.now()}",
            )
        }
        val foods = if (staleFoods.isEmpty()) initialFoods else repository.foodItems.first()
        val today = LocalDate.now()
        val candidates = getNotificationCandidates(foods, today)
        if (candidates.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_ID)
            return Result.success()
        }

        createChannel()
        val names = candidates.take(3).joinToString(", ") { it.name }
        val remaining = candidates.size - 3
        val namesBody = if (remaining > 0) "$names 외 ${remaining}개" else names
        val unopenedOverdueCount = candidates.count { foodItem ->
            !foodItem.isOpened && foodItem.effectiveDate()?.value?.isBefore(today) == true
        }
        val body = if (unopenedOverdueCount > 0) {
            "$namesBody · ${applicationContext.getString(R.string.notification_unopened_overdue_short, unopenedOverdueCount)}"
        } else {
            namesBody
        }
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
            .addAction(
                0,
                applicationContext.getString(R.string.rescue_mark_consumed),
                foodActionIntent(candidates.first().id.value, NotificationFoodActionReceiver.ACTION_CONSUME, 1),
            )
            .addAction(
                0,
                applicationContext.getString(R.string.action_still_here),
                foodActionIntent(candidates.first().id.value, NotificationFoodActionReceiver.ACTION_STILL_HERE, 2),
            )
            .addAction(
                0,
                applicationContext.getString(R.string.action_discard),
                foodActionIntent(candidates.first().id.value, NotificationFoodActionReceiver.ACTION_DISCARD, 3),
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun foodActionIntent(foodId: String, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            Intent(applicationContext, NotificationFoodActionReceiver::class.java).apply {
                this.action = action
                putExtra(NotificationFoodActionReceiver.EXTRA_FOOD_ID, foodId)
                putExtra(NotificationFoodActionReceiver.EXTRA_OPERATION_ID, UUID.randomUUID().toString())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun scheduleAfterQuietHours(delay: java.time.Duration) {
        val request = OneTimeWorkRequestBuilder<ExpiryNotificationWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            QUIET_HOURS_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
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
        private const val QUIET_HOURS_WORK_NAME = "quiet-hours-expiry-summary"
        private const val CHANNEL_ID = "expiry-summary"
        internal const val NOTIFICATION_ID = 1001

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
