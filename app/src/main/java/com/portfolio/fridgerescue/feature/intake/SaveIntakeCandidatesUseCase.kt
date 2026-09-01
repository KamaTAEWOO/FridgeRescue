package com.portfolio.fridgerescue.feature.intake

import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.model.FoodDate
import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.core.model.IntakeCandidate
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
            FoodItem(
                id = idFactory(),
                name = candidate.normalizedName,
                quantity = candidate.quantity,
                storageLocation = candidate.storageLocation,
                dates = listOfNotNull(estimatedDate),
                status = if (estimatedDate == null) FoodStatus.NEEDS_REVIEW else FoodStatus.ACTIVE,
            )
        }
        if (items.isEmpty()) return 0
        return repository.saveAll(items, operationIdFactory())
    }
}
