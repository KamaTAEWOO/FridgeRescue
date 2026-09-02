package com.portfolio.fridgerescue.core.data.repository

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.portfolio.fridgerescue.core.data.local.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.core.domain.repository.DataDeletionManager
import com.portfolio.fridgerescue.feature.family.domain.FamilySyncSettingsRepository
import com.portfolio.fridgerescue.feature.intake.data.SharedIntakeCacheCleaner
import com.portfolio.fridgerescue.feature.notification.domain.NotificationSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** DB, 설정, 임시 파일, 알림을 포함한 로컬 데이터를 실제로 제거한다. */
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
