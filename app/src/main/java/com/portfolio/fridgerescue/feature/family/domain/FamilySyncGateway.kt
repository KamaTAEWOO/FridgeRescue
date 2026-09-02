package com.portfolio.fridgerescue.feature.family.domain

import com.portfolio.fridgerescue.sync.AccountResponse
import com.portfolio.fridgerescue.sync.FamilyResponse
import com.portfolio.fridgerescue.sync.SyncRequest
import com.portfolio.fridgerescue.sync.SyncResponse

/** 가족 서버와 통신하는 기능을 추상화한 도메인 계약이다. */
interface FamilySyncGateway {
    suspend fun createAccount(baseUrl: String, displayName: String): AccountResponse
    suspend fun joinFamily(baseUrl: String, token: String, inviteCode: String): FamilyResponse
    suspend fun sync(baseUrl: String, token: String, request: SyncRequest): SyncResponse
}
