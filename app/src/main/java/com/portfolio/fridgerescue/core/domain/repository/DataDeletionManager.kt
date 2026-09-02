package com.portfolio.fridgerescue.core.domain.repository

/** 사용자의 로컬 데이터를 한 번에 삭제하는 도메인 계약이다. */
fun interface DataDeletionManager {
    suspend fun deleteAll()
}
