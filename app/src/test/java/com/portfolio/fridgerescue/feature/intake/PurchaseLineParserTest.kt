package com.portfolio.fridgerescue.feature.intake

import com.portfolio.fridgerescue.core.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.model.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseLineParserTest {
    private var nextId = 0
    private val parser = PurchaseLineParser(idFactory = { "candidate-${++nextId}" })

    @Test
    fun `TC-INTAKE-011 extracts product names and explicit quantities but ignores totals`() {
        val candidates = parser.parse(
            "draft",
            """
            주문번호 123456
            친환경 두부 2개 5,000원
            시금치 1봉 2,900원
            결제금액 7,900원
            """.trimIndent(),
        )

        assertEquals(listOf("친환경 두부", "시금치"), candidates.map { it.normalizedName })
        assertEquals(listOf(2, 1), candidates.map { it.quantity })
        assertTrue(candidates.all { it.group == IntakeCandidateGroup.MANAGE && it.isSelected })
    }

    @Test
    fun `TC-FILTER-001 fresh food is selected and household goods are excluded`() {
        val candidates = parser.parse("draft", "냉동 닭가슴살 2팩\n물티슈 1개")

        assertEquals(IntakeCandidateGroup.MANAGE, candidates[0].group)
        assertEquals(StorageLocation.FROZEN, candidates[0].storageLocation)
        assertTrue(candidates[0].isSelected)
        assertEquals(IntakeCandidateGroup.EXCLUDED, candidates[1].group)
        assertFalse(candidates[1].isSelected)
    }

    @Test
    fun `TC-OCR-002 bundle offer is sent to review without guessed quantity`() {
        val candidate = parser.parse("draft", "요거트 2+1 행사").single()

        assertEquals(IntakeCandidateGroup.REVIEW, candidate.group)
        assertFalse(candidate.isSelected)
        assertNull(candidate.quantity)
        assertEquals("묶음 수량 확인 필요", candidate.reason)
    }

    @Test
    fun `TC-INTAKE-014 canceled item is not auto selected`() {
        val candidate = parser.parse("draft", "주문취소 두부 1개").single()

        assertEquals(IntakeCandidateGroup.REVIEW, candidate.group)
        assertFalse(candidate.isSelected)
    }
}
