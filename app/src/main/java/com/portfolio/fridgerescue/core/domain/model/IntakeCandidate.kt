package com.portfolio.fridgerescue.core.domain.model

import java.time.LocalDate

enum class IntakeCandidateGroup { MANAGE, REVIEW, EXCLUDED }

data class IntakeCandidate(
    val id: String,
    val draftId: String,
    val originalName: String,
    val normalizedName: String,
    val quantity: Int?,
    val group: IntakeCandidateGroup,
    val isSelected: Boolean,
    val reason: String?,
    val position: Int,
    val storageLocation: StorageLocation,
    val estimatedShelfLifeDays: Int?,
    val displayedDate: LocalDate? = null,
)
