package com.portfolio.fridgerescue.feature.notification.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.portfolio.fridgerescue.feature.notification.domain.NotificationSettings
import com.portfolio.fridgerescue.feature.notification.domain.NotificationSettingsRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "notification_settings")

/** 알림 설정을 DataStore에 보관하는 데이터 계층 구현이다. */
class DataStoreNotificationSettingsRepository(
    private val context: Context,
) : NotificationSettingsRepository {
    override val settings: Flow<NotificationSettings> = context.notificationDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
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
