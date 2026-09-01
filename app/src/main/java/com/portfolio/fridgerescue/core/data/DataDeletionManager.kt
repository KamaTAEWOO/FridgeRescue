package com.portfolio.fridgerescue.core.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.portfolio.fridgerescue.core.data.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.feature.intake.SharedIntakeCacheCleaner
import com.portfolio.fridgerescue.feature.notification.NotificationSettingsRepository
import com.portfolio.fridgerescue.feature.family.FamilySyncSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface DataDeletionManager {
    suspend fun deleteAll()
}

class LocalDataDeletionManager(
    private val context: Context,
    private val database: FridgeRescueDatabase,
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val familySyncSettingsRepository: FamilySyncSettingsRepository? = null,
) : DataDeletionManager {
    override suspend fun deleteAll() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        notificationSettingsRepository.clear()
        familySyncSettingsRepository?.clear()
        SharedIntakeCacheCleaner.clearAll(context.cacheDir)
        NotificationManagerCompat.from(context).cancelAll()
    }
}
