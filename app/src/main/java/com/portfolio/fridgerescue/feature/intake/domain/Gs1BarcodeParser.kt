package com.portfolio.fridgerescue.feature.intake.domain

import java.time.DateTimeException
import java.time.LocalDate

data class ParsedBarcode(
    val rawValue: String,
    val gtin: String?,
    val displayedDate: LocalDate?,
    val batchNumber: String?,
    val isGs1: Boolean,
)

class Gs1BarcodeParser {
    fun parse(rawValue: String): ParsedBarcode {
        val value = rawValue.trim().removeSymbologyIdentifier()
        val fields = if ('(' in value) parseParenthesized(value) else parseCompact(value)
        val gtin = fields[AI_GTIN]
        val date = (fields[AI_EXPIRATION] ?: fields[AI_BEST_BEFORE])?.let(::parseDate)
        return ParsedBarcode(
            rawValue = rawValue,
            gtin = gtin,
            displayedDate = date,
            batchNumber = fields[AI_BATCH],
            isGs1 = gtin != null || date != null || fields.containsKey(AI_BATCH),
        )
    }

    private fun parseParenthesized(value: String): Map<String, String> = FIELD.findAll(value)
        .associate { match -> match.groupValues[1] to match.groupValues[2].trim() }

    private fun parseCompact(value: String): Map<String, String> {
        if (!value.startsWith(AI_GTIN) || value.length < 16) return emptyMap()
        val result = linkedMapOf(AI_GTIN to value.substring(2, 16))
        var cursor = 16
        while (cursor + 2 <= value.length) {
            val ai = value.substring(cursor, cursor + 2)
            cursor += 2
            when (ai) {
                AI_EXPIRATION, AI_BEST_BEFORE -> {
                    if (cursor + 6 > value.length) break
                    result[ai] = value.substring(cursor, cursor + 6)
                    cursor += 6
                }
                AI_BATCH -> {
                    val end = value.indexOf(GROUP_SEPARATOR, cursor).let {
                        if (it == -1) value.length else it
                    }
                    result[ai] = value.substring(cursor, end)
                    cursor = end + 1
                }
                else -> break
            }
        }
        return result
    }

    private fun parseDate(value: String): LocalDate? {
        if (value.length != 6 || value.any { !it.isDigit() }) return null
        return try {
            LocalDate.of(2000 + value.substring(0, 2).toInt(), value.substring(2, 4).toInt(), value.substring(4, 6).toInt())
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun String.removeSymbologyIdentifier(): String =
        if (length >= 3 && first() == ']' && this[1].isLetterOrDigit()) substring(3) else this

    private companion object {
        const val AI_GTIN = "01"
        const val AI_EXPIRATION = "17"
        const val AI_BEST_BEFORE = "15"
        const val AI_BATCH = "10"
        const val GROUP_SEPARATOR = '\u001D'
        val FIELD = Regex("\\((\\d{2,4})\\)([^()]*)")
    }
}
