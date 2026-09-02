package com.portfolio.fridgerescue.feature.intake.domain

import com.portfolio.fridgerescue.core.domain.repository.FoodRepository
import com.portfolio.fridgerescue.core.domain.model.FoodDate
import com.portfolio.fridgerescue.core.domain.model.FoodDateSource
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidate
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

class SaveIntakeCandidatesUseCase(
    private val repository: FoodRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val idFactory: () -> FoodItemId = { FoodItemId(UUID.randomUUID().toString()) },
    private val operationIdFactory: () -> String = { UUID.randomUUID().toString() },
) {
    suspend operator fun invoke(candidates: List<IntakeCandidate>): Int {
        val today = LocalDate.now(clock)
        val items = candidates.filter(IntakeCandidate::isSelected).map { candidate ->
            val estimatedDate = candidate.estimatedShelfLifeDays?.let { days ->
                FoodDate(today.plusDays(days.toLong()), FoodDateSource.APP_ESTIMATED)
            }
            val displayedDate = candidate.displayedDate?.let { date ->
                FoodDate(date, FoodDateSource.MANUFACTURER_DISPLAYED)
            }
            FoodItem(
                id = idFactory(),
                name = candidate.normalizedName,
                quantity = candidate.quantity,
                storageLocation = candidate.storageLocation,
                dates = listOfNotNull(displayedDate, estimatedDate),
                status = if (displayedDate == null && estimatedDate == null) {
                    FoodStatus.NEEDS_REVIEW
                } else {
                    FoodStatus.ACTIVE
                },
            )
        }
        if (items.isEmpty()) return 0
        return repository.saveAll(items, operationIdFactory())
    }
}
