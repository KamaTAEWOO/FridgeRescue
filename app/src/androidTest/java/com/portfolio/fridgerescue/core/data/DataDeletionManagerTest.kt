package com.portfolio.fridgerescue.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portfolio.fridgerescue.core.data.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.core.data.database.IntakeCandidateEntity
import com.portfolio.fridgerescue.feature.notification.NotificationSettings
import com.portfolio.fridgerescue.feature.notification.NotificationSettingsRepository
import com.portfolio.fridgerescue.feature.family.FamilySyncSettings
import com.portfolio.fridgerescue.feature.family.FamilySyncSettingsRepository
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataDeletionManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: FridgeRescueDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, FridgeRescueDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun TC_PRIVACY_005_confirmed_deletion_clears_room_settings_and_managed_cache() = runBlocking {
        database.intakeCandidateDao().insertAll(listOf(candidate()))
        val settings = FakeNotificationSettingsRepository()
        val familySettings = FakeFamilySettingsRepository()
        val cacheFile = File(context.cacheDir, "shared-intake/deletion-test.jpg").apply {
            parentFile?.mkdirs()
            writeText("private receipt")
        }

        LocalDataDeletionManager(context, database, settings, familySettings).deleteAll()

        assertEquals(emptyList<Any>(), database.intakeCandidateDao().findForDraft("draft"))
        assertTrue(settings.cleared)
        assertTrue(familySettings.cleared)
        assertFalse(cacheFile.exists())
    }

    private class FakeFamilySettingsRepository : FamilySyncSettingsRepository {
        override val settings: Flow<FamilySyncSettings> = MutableStateFlow(FamilySyncSettings())
        var cleared = false
        override suspend fun saveAccount(settings: FamilySyncSettings) = Unit
        override suspend fun saveFamily(familyId: String, familyName: String, inviteCode: String) = Unit
        override suspend fun saveSync(revision: Long, syncedAtEpochMillis: Long) = Unit
        override suspend fun clear() { cleared = true }
    }

    private fun candidate() = IntakeCandidateEntity(
        id = "candidate",
        draftId = "draft",
        originalName = "두부",
        normalizedName = "두부",
        quantity = 1,
        candidateGroup = "MANAGE",
        isSelected = true,
        reason = null,
        position = 0,
        storageLocation = "REFRIGERATED",
        estimatedShelfLifeDays = 5,
        displayedDate = null,
    )

    private class FakeNotificationSettingsRepository : NotificationSettingsRepository {
        override val settings: Flow<NotificationSettings> = MutableStateFlow(NotificationSettings())
        var cleared = false

        override suspend fun setQuietHoursEnabled(enabled: Boolean) = Unit

        override suspend fun clear() {
            cleared = true
        }
    }
}
