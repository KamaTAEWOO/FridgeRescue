package com.portfolio.fridgerescue.feature.intake.domain

import com.portfolio.fridgerescue.core.domain.model.IntakeCandidate
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import java.util.UUID

class PurchaseLineParser(
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun parse(draftId: String, text: String): List<IntakeCandidate> = text
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .filterNot(::isMetadataLine)
        .mapIndexedNotNull { position, line -> parseLine(draftId, line, position) }
        .toList()

    private fun parseLine(draftId: String, line: String, position: Int): IntakeCandidate? {
        val quantity = QUANTITY_PATTERNS.firstNotNullOfOrNull { regex ->
            regex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
        val cleanedName = QUANTITY_PATTERNS
            .fold(line) { value, regex -> value.replace(regex, " ") }
            .replace(PRICE_PATTERN, " ")
            .replace(LEADING_MARKER, "")
            .replace(WHITESPACE, " ")
            .trim(' ', '-', '·', '|')
        if (cleanedName.length < 2 || cleanedName.all(Char::isDigit)) return null

        val normalized = cleanedName
            .replace(OPTION_PATTERN, "")
            .replace(WHITESPACE, " ")
            .trim()
        val lower = line.lowercase()
        val group = when {
            REVIEW_KEYWORDS.any(lower::contains) || BUNDLE_PATTERN.containsMatchIn(lower) ->
                IntakeCandidateGroup.REVIEW
            EXCLUDED_KEYWORDS.any(lower::contains) -> IntakeCandidateGroup.EXCLUDED
            MANAGED_KEYWORDS.any(lower::contains) -> IntakeCandidateGroup.MANAGE
            else -> IntakeCandidateGroup.REVIEW
        }
        val reason = when (group) {
            IntakeCandidateGroup.MANAGE -> null
            IntakeCandidateGroup.REVIEW -> if (BUNDLE_PATTERN.containsMatchIn(lower)) {
                "묶음 수량 확인 필요"
            } else {
                "식재료 여부 확인 필요"
            }
            IntakeCandidateGroup.EXCLUDED -> "장기보관 또는 비식품"
        }
        return IntakeCandidate(
            id = idFactory(),
            draftId = draftId,
            originalName = line,
            normalizedName = normalized,
            quantity = quantity,
            group = group,
            isSelected = group == IntakeCandidateGroup.MANAGE,
            reason = reason,
            position = position,
            storageLocation = if (lower.contains("냉동")) {
                StorageLocation.FROZEN
            } else {
                StorageLocation.REFRIGERATED
            },
            estimatedShelfLifeDays = estimateShelfLifeDays(lower),
        )
    }

    private fun estimateShelfLifeDays(value: String): Int? = when {
        listOf("시금치", "상추", "깻잎", "샐러드").any(value::contains) -> 3
        listOf("고기", "돼지", "소고기", "닭").any(value::contains) -> 2
        listOf("두부", "우유", "버섯", "요거트").any(value::contains) -> 5
        listOf("계란", "달걀").any(value::contains) -> 14
        else -> null
    }

    private fun isMetadataLine(line: String): Boolean {
        val normalized = line.replace(" ", "").lowercase()
        return METADATA_KEYWORDS.any(normalized::contains) &&
            MANAGED_KEYWORDS.none(normalized::contains)
    }

    private companion object {
        val QUANTITY_PATTERNS = listOf(
            Regex("(?i)(?:수량\\s*[:x×]?\\s*)?(\\d+)\\s*(?:개|봉|팩|병|입|ea)"),
            Regex("(?i)[x×]\\s*(\\d+)"),
        )
        val PRICE_PATTERN = Regex("\\d{1,3}(?:,\\d{3})+\\s*원?")
        val LEADING_MARKER = Regex("^[•·*\\-\\d.)\\s]+")
        val OPTION_PATTERN = Regex("\\s*\\[[^]]+]\\s*")
        val WHITESPACE = Regex("\\s+")
        val BUNDLE_PATTERN = Regex("\\d+\\s*[+]\\s*\\d+")
        val METADATA_KEYWORDS = listOf("주문번호", "결제금액", "총금액", "합계", "배송비", "할인", "쿠폰", "카드", "주소")
        val REVIEW_KEYWORDS = listOf("주문취소", "취소상품", "대체상품", "품절")
        val EXCLUDED_KEYWORDS = listOf("휴지", "세제", "샴푸", "물티슈", "비누", "치약", "생수", "라면", "통조림", "쌀")
        val MANAGED_KEYWORDS = listOf(
            "두부", "우유", "시금치", "상추", "깻잎", "샐러드", "고기", "돼지", "소고기",
            "닭", "계란", "달걀", "요거트", "버섯", "토마토", "오이", "과일", "채소", "김치",
        )
    }
}
