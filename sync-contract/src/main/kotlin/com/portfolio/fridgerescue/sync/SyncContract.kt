package com.portfolio.fridgerescue.sync

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(val displayName: String)

@Serializable
data class AccountResponse(
    val accountId: String,
    val accessToken: String,
    val displayName: String,
    val familyId: String,
    val familyName: String,
    val inviteCode: String,
)

@Serializable
data class JoinFamilyRequest(val inviteCode: String)

@Serializable
data class FamilyResponse(
    val familyId: String,
    val familyName: String,
    val inviteCode: String,
    val memberCount: Int,
)

@Serializable
data class SyncFoodItem(
    val id: String,
    val name: String,
    val quantity: Int? = null,
    val storageLocation: String,
    val manufacturerDisplayedDate: String? = null,
    val appEstimatedDate: String? = null,
    val userConfirmedDate: String? = null,
    val isOpened: Boolean,
    val isPinned: Boolean,
    val status: String,
    val updatedAtEpochMillis: Long,
    val updatedByAccountId: String = "",
)

@Serializable
data class SyncRequest(
    val knownRevision: Long,
    val items: List<SyncFoodItem>,
)

@Serializable
data class SyncResponse(
    val familyId: String,
    val revision: Long,
    val serverTimeEpochMillis: Long,
    val items: List<SyncFoodItem>,
)

@Serializable
data class ApiError(val code: String, val message: String)
