package com.portfolio.fridgerescue.feature.family

import androidx.room.withTransaction
import com.portfolio.fridgerescue.core.data.database.FoodItemDao
import com.portfolio.fridgerescue.core.data.database.FoodItemEntity
import com.portfolio.fridgerescue.core.data.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.sync.SyncFoodItem
import com.portfolio.fridgerescue.sync.SyncRequest
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FamilySyncManager(
    private val database: FridgeRescueDatabase,
    private val foodItemDao: FoodItemDao,
    private val settingsRepository: FamilySyncSettingsRepository,
    private val gateway: FamilySyncGateway = HttpFamilySyncGateway(),
    private val clock: Clock = Clock.systemUTC(),
) {
    val settings: Flow<FamilySyncSettings> = settingsRepository.settings

    suspend fun createAccount(baseUrl: String, displayName: String) {
        val response = gateway.createAccount(baseUrl.trim().trimEnd('/'), displayName.trim())
        settingsRepository.saveAccount(
            FamilySyncSettings(
                serverBaseUrl = baseUrl.trim().trimEnd('/'),
                accountId = response.accountId,
                accessToken = response.accessToken,
                displayName = response.displayName,
                familyId = response.familyId,
                familyName = response.familyName,
                inviteCode = response.inviteCode,
            ),
        )
        syncNow()
    }

    suspend fun joinFamily(inviteCode: String) {
        val settings = settingsRepository.settings.first()
        require(settings.isConnected)
        val family = gateway.joinFamily(
            settings.serverBaseUrl,
            requireNotNull(settings.accessToken),
            inviteCode.trim(),
        )
        settingsRepository.saveFamily(family.familyId, family.familyName, family.inviteCode)
        syncNow()
    }

    suspend fun syncNow(): Int {
        val settings = settingsRepository.settings.first()
        require(settings.isConnected)
        val local = foodItemDao.loadAll().map(FoodItemEntity::toSync)
        val response = gateway.sync(
            settings.serverBaseUrl,
            requireNotNull(settings.accessToken),
            SyncRequest(settings.revision, local),
        )
        database.withTransaction {
            foodItemDao.upsertAll(response.items.map(SyncFoodItem::toEntity))
        }
        settingsRepository.saveSync(response.revision, clock.millis())
        return response.items.size
    }

    suspend fun clear() = settingsRepository.clear()
}

private fun FoodItemEntity.toSync() = SyncFoodItem(
    id = id,
    name = name,
    quantity = quantity,
    storageLocation = storageLocation,
    manufacturerDisplayedDate = manufacturerDisplayedDate,
    appEstimatedDate = appEstimatedDate,
    userConfirmedDate = userConfirmedDate,
    isOpened = isOpened,
    isPinned = isPinned,
    status = status,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun SyncFoodItem.toEntity() = FoodItemEntity(
    id = id,
    name = name,
    quantity = quantity,
    storageLocation = storageLocation,
    manufacturerDisplayedDate = manufacturerDisplayedDate,
    appEstimatedDate = appEstimatedDate,
    userConfirmedDate = userConfirmedDate,
    isOpened = isOpened,
    isPinned = isPinned,
    status = status,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
