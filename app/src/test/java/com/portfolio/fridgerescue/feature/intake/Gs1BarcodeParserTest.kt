package com.portfolio.fridgerescue.feature.intake

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Gs1BarcodeParserTest {
    private val parser = Gs1BarcodeParser()

    @Test
    fun TC_BARCODE_001_parenthesized_gs1_extracts_gtin_date_and_batch() {
        val parsed = parser.parse("(01)08801234567890(17)270930(10)LOT-42")

        assertTrue(parsed.isGs1)
        assertEquals("08801234567890", parsed.gtin)
        assertEquals(LocalDate.of(2027, 9, 30), parsed.displayedDate)
        assertEquals("LOT-42", parsed.batchNumber)
    }

    @Test
    fun TC_BARCODE_002_compact_gs1_and_symbology_identifier_are_supported() {
        val parsed = parser.parse("]d2010880123456789015270105")

        assertEquals("08801234567890", parsed.gtin)
        assertEquals(LocalDate.of(2027, 1, 5), parsed.displayedDate)
    }

    @Test
    fun TC_BARCODE_003_plain_or_invalid_date_never_invents_a_date() {
        val plain = parser.parse("8801234567890")
        val invalidGs1 = parser.parse("(01)08801234567890(17)270231")

        assertFalse(plain.isGs1)
        assertNull(plain.displayedDate)
        assertTrue(invalidGs1.isGs1)
        assertNull(invalidGs1.displayedDate)
    }
}
