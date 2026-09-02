package com.portfolio.fridgerescue.feature.family.domain

import kotlinx.coroutines.flow.Flow

data class FamilySyncSettings(
    val serverBaseUrl: String = "http://10.0.2.2:8080",
    val accountId: String? = null,
    val accessToken: String? = null,
    val displayName: String? = null,
    val familyId: String? = null,
    val familyName: String? = null,
    val inviteCode: String? = null,
    val revision: Long = 0,
    val lastSyncedAtEpochMillis: Long? = null,
) {
    val isConnected: Boolean get() = !accountId.isNullOrBlank() && !accessToken.isNullOrBlank()
}

/** 가족 동기화 연결 정보를 저장하고 관찰하는 도메인 계약이다. */
interface FamilySyncSettingsRepository {
    val settings: Flow<FamilySyncSettings>
    suspend fun saveAccount(settings: FamilySyncSettings)
    suspend fun saveFamily(familyId: String, familyName: String, inviteCode: String)
    suspend fun saveSync(revision: Long, syncedAtEpochMillis: Long)
    suspend fun clear()
}
