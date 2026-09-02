package com.portfolio.fridgerescue.feature.intake.domain

import com.portfolio.fridgerescue.core.domain.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.testing.InMemoryIntakeDraftRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiveBarcodeUseCaseTest {
    @Test
    fun TC_BARCODE_004_gs1_scan_creates_review_candidate_with_displayed_date() = runBlocking {
        val repository = InMemoryIntakeDraftRepository()
        val ids = listOf("draft", "candidate").iterator()
        val useCase = ReceiveBarcodeUseCase(repository, fixedClock(), idFactory = ids::next)

        val draftId = useCase("(01)08801234567890(17)270930", "DATA_MATRIX")
        val candidate = repository.candidates("draft").single()

        assertEquals("draft", draftId)
        assertEquals("GTIN 08801234567890", candidate.normalizedName)
        assertEquals(LocalDate.of(2027, 9, 30), candidate.displayedDate)
        assertEquals(IntakeCandidateGroup.REVIEW, candidate.group)
        assertFalse(candidate.isSelected)
        assertEquals("(01)08801234567890(17)270930", repository.latestActiveDraft.first()?.textContent)
    }

    @Test
    fun TC_BARCODE_005_plain_barcode_requires_product_name_review() = runBlocking {
        val repository = InMemoryIntakeDraftRepository()
        val ids = listOf("draft", "candidate").iterator()

        ReceiveBarcodeUseCase(repository, fixedClock(), idFactory = ids::next)("8801234567890", "EAN_13")

        val candidate = repository.candidates("draft").single()
        assertEquals("바코드 8801234567890", candidate.normalizedName)
        assertNull(candidate.displayedDate)
        assertFalse(candidate.isSelected)
    }

    private fun fixedClock() = Clock.fixed(
        Instant.parse("2026-09-01T00:00:00Z"),
        ZoneOffset.UTC,
    )
}
