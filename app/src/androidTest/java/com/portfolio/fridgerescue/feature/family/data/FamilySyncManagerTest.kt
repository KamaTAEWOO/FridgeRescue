package com.portfolio.fridgerescue.feature.family.data

import com.portfolio.fridgerescue.feature.family.domain.FamilySyncGateway
import com.portfolio.fridgerescue.feature.family.domain.FamilySyncSettings
import com.portfolio.fridgerescue.feature.family.domain.FamilySyncSettingsRepository
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portfolio.fridgerescue.core.data.local.database.FoodItemEntity
import com.portfolio.fridgerescue.core.data.local.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.sync.AccountResponse
import com.portfolio.fridgerescue.sync.FamilyResponse
import com.portfolio.fridgerescue.sync.SyncFoodItem
import com.portfolio.fridgerescue.sync.SyncRequest
import com.portfolio.fridgerescue.sync.SyncResponse
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilySyncManagerTest {
    private lateinit var database: FridgeRescueDatabase
    private lateinit var settings: FakeSettingsRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FridgeRescueDatabase::class.java,
        ).build()
        settings = FakeSettingsRepository()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun newerRemoteItemReplacesLocalAndRevisionIsStored() = runBlocking {
        database.foodItemDao().upsert(food("로컬 두부", 100))
        val gateway = FakeGateway(
            syncResponse = SyncResponse(
                familyId = "family",
                revision = 4,
                serverTimeEpochMillis = 300,
                items = listOf(food("가족 두부", 200).toSync()),
            ),
        )
        val manager = manager(gateway)

        assertEquals(1, manager.syncNow())

        assertEquals("가족 두부", database.foodItemDao().findById("food")?.name)
        assertEquals(4, settings.value.value.revision)
        assertEquals(1_000L, settings.value.value.lastSyncedAtEpochMillis)
    }

    @Test
    fun accountCreationStoresCredentialsAndPerformsInitialSync() = runBlocking {
        val gateway = FakeGateway()
        val manager = manager(gateway)

        manager.createAccount("https://family.example", "민지")

        assertEquals("token", settings.value.value.accessToken)
        assertEquals("민지", settings.value.value.displayName)
        assertEquals(1, gateway.syncCalls)
    }

    private fun manager(gateway: FakeGateway) = FamilySyncManager(
        database = database,
        foodItemDao = database.foodItemDao(),
        settingsRepository = settings,
        gateway = gateway,
        clock = Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC),
    )

    private fun food(name: String, updatedAt: Long) = FoodItemEntity(
        id = "food",
        name = name,
        quantity = 1,
        storageLocation = "REFRIGERATED",
        manufacturerDisplayedDate = null,
        appEstimatedDate = "2026-09-03",
        userConfirmedDate = null,
        isOpened = false,
        isPinned = false,
        status = "ACTIVE",
        updatedAtEpochMillis = updatedAt,
    )
}

private class FakeSettingsRepository : FamilySyncSettingsRepository {
    val value = MutableStateFlow(
        FamilySyncSettings(
            serverBaseUrl = "https://family.example",
            accountId = "account",
            accessToken = "token",
            displayName = "민지",
            familyId = "family",
            familyName = "민지 가족",
            inviteCode = "ABC123",
        ),
    )
    override val settings: Flow<FamilySyncSettings> = value

    override suspend fun saveAccount(settings: FamilySyncSettings) { value.value = settings }
    override suspend fun saveFamily(familyId: String, familyName: String, inviteCode: String) {
        value.value = value.value.copy(familyId = familyId, familyName = familyName, inviteCode = inviteCode)
    }
    override suspend fun saveSync(revision: Long, syncedAtEpochMillis: Long) {
        value.value = value.value.copy(revision = revision, lastSyncedAtEpochMillis = syncedAtEpochMillis)
    }
    override suspend fun clear() { value.value = FamilySyncSettings() }
}

private class FakeGateway(
    private val syncResponse: SyncResponse = SyncResponse("family", 1, 1, emptyList()),
) : FamilySyncGateway {
    var syncCalls = 0
    override suspend fun createAccount(baseUrl: String, displayName: String) = AccountResponse(
        "account", "token", displayName, "family", "$displayName 가족", "ABC123",
    )
    override suspend fun joinFamily(baseUrl: String, token: String, inviteCode: String) =
        FamilyResponse("joined", "함께 가족", inviteCode, 2)
    override suspend fun sync(baseUrl: String, token: String, request: SyncRequest): SyncResponse {
        syncCalls++
        return syncResponse
    }
}

private fun FoodItemEntity.toSync() = SyncFoodItem(
    id, name, quantity, storageLocation, manufacturerDisplayedDate, appEstimatedDate,
    userConfirmedDate, isOpened, isPinned, status, updatedAtEpochMillis,
)
