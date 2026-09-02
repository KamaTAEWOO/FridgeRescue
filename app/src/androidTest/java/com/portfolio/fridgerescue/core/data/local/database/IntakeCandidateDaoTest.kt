package com.portfolio.fridgerescue.core.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portfolio.fridgerescue.core.data.repository.RoomIntakeDraftRepository
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidate
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import com.portfolio.fridgerescue.feature.intake.domain.UpdateIntakeCandidateUseCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntakeCandidateDaoTest {
    private lateinit var database: FridgeRescueDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FridgeRescueDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun TC_DATA_016_candidate_edit_survives_repository_recreation() = runBlocking {
        val firstRepository = repository()
        firstRepository.replaceCandidates("draft", listOf(candidate()))
        UpdateIntakeCandidateUseCase(firstRepository)("candidate", "부침용 두부", "4")

        val restored = repository().candidates("draft").single()

        assertEquals("부침용 두부", restored.normalizedName)
        assertEquals(4, restored.quantity)
    }

    private fun repository() = RoomIntakeDraftRepository(
        dao = database.intakeDraftDao(),
        candidateDao = database.intakeCandidateDao(),
    )

    private fun candidate() = IntakeCandidate(
        id = "candidate",
        draftId = "draft",
        originalName = "두부 1개",
        normalizedName = "두부",
        quantity = 1,
        group = IntakeCandidateGroup.MANAGE,
        isSelected = true,
        reason = null,
        position = 0,
        storageLocation = StorageLocation.REFRIGERATED,
        estimatedShelfLifeDays = 5,
    )
}
