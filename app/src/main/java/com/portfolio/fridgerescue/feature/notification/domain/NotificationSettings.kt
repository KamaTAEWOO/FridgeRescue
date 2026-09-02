package com.portfolio.fridgerescue.feature.notification.domain

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow

data class NotificationSettings(
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 8,
)

/** 알림 설정 저장 방식과 무관하게 기능 계층이 사용하는 계약이다. */
interface NotificationSettingsRepository {
    val settings: Flow<NotificationSettings>
    suspend fun setQuietHoursEnabled(enabled: Boolean)
    suspend fun clear()
}

/** 현재 시각이 방해 금지 시간이라면 알림을 미룰 시간을 계산한다. */
class QuietHoursPolicy {
    fun delayUntilAllowed(
        now: ZonedDateTime,
        settings: NotificationSettings,
    ): Duration? {
        if (!settings.quietHoursEnabled || settings.quietStartHour == settings.quietEndHour) {
            return null
        }
        val start = LocalTime.of(settings.quietStartHour, 0)
        val end = LocalTime.of(settings.quietEndHour, 0)
        val current = now.toLocalTime()
        val isQuiet = if (start < end) {
            current >= start && current < end
        } else {
            current >= start || current < end
        }
        if (!isQuiet) return null

        val endDate = when {
            start < end -> now.toLocalDate()
            current >= start -> now.toLocalDate().plusDays(1)
            else -> now.toLocalDate()
        }
        return Duration.between(now, endDate.atTime(end).atZone(now.zone))
            .coerceAtLeast(Duration.ZERO)
    }
}
