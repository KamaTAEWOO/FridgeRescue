package com.portfolio.fridgerescue.feature.notification

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class NotificationSettings(
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 8,
)

interface NotificationSettingsRepository {
    val settings: Flow<NotificationSettings>
    suspend fun setQuietHoursEnabled(enabled: Boolean)
    suspend fun clear()
}

private val Context.notificationDataStore by preferencesDataStore(name = "notification_settings")

class DataStoreNotificationSettingsRepository(
    private val context: Context,
) : NotificationSettingsRepository {
    override val settings: Flow<NotificationSettings> = context.notificationDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences ->
            NotificationSettings(
                quietHoursEnabled = preferences[QUIET_HOURS_ENABLED] ?: true,
            )
        }

    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { it[QUIET_HOURS_ENABLED] = enabled }
    }

    override suspend fun clear() {
        context.notificationDataStore.edit { it.clear() }
    }

    private companion object {
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    }
}

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
