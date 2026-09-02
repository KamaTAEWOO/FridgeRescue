package com.portfolio.fridgerescue.feature.family.domain

import kotlinx.coroutines.flow.Flow

/** 화면이 데이터 구현을 알지 않고 가족 동기화를 요청하도록 제공하는 기능 계약이다. */
interface FamilySyncService {
    val settings: Flow<FamilySyncSettings>
    suspend fun createAccount(baseUrl: String, displayName: String)
    suspend fun joinFamily(inviteCode: String)
    suspend fun syncNow(): Int
    suspend fun clear()
}
