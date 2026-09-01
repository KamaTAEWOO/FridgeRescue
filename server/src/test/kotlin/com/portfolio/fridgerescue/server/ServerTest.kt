package com.portfolio.fridgerescue.server

import com.portfolio.fridgerescue.sync.AccountResponse
import com.portfolio.fridgerescue.sync.CreateAccountRequest
import com.portfolio.fridgerescue.sync.FamilyResponse
import com.portfolio.fridgerescue.sync.JoinFamilyRequest
import com.portfolio.fridgerescue.sync.SyncFoodItem
import com.portfolio.fridgerescue.sync.SyncRequest
import com.portfolio.fridgerescue.sync.SyncResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ServerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun twoAccountsJoinAndMergeByLatestTimestamp() = testApplication {
        application { module(FamilyStore()) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        suspend fun account(name: String): AccountResponse = client.post("/v1/accounts") {
            contentType(ContentType.Application.Json)
            setBody(CreateAccountRequest(name))
        }.body()

        val owner = account("민지")
        val member = account("태우")
        val joined = client.post("/v1/families/join") {
            bearerAuth(member.accessToken)
            contentType(ContentType.Application.Json)
            setBody(JoinFamilyRequest(owner.inviteCode))
        }
        assertEquals(HttpStatusCode.OK, joined.status)
        assertEquals(2, joined.body<FamilyResponse>().memberCount)

        val oldItem = item(name = "두부", updatedAt = 100)
        client.post("/v1/sync") {
            bearerAuth(owner.accessToken)
            contentType(ContentType.Application.Json)
            setBody(SyncRequest(0, listOf(oldItem)))
        }
        val response = client.post("/v1/sync") {
            bearerAuth(member.accessToken)
            contentType(ContentType.Application.Json)
            setBody(SyncRequest(0, listOf(oldItem.copy(name = "부침용 두부", updatedAtEpochMillis = 200))))
        }.body<SyncResponse>()

        assertEquals("부침용 두부", response.items.single().name)
        assertTrue(response.revision >= 2)
    }

    @Test
    fun protectedSyncRejectsMissingToken() = testApplication {
        application { module(FamilyStore()) }
        val response = client.post("/v1/sync") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun accountsAreRestoredFromPersistentState() {
        val stateFile = temporaryFolder.newFolder("state").toPath().resolve("family.json")
        val account = FamilyStore(stateFile = stateFile).createAccount("민지")

        val restored = FamilyStore(stateFile = stateFile)

        assertEquals(account.accountId, restored.accountIdForToken(account.accessToken))
    }

    private fun item(name: String, updatedAt: Long) = SyncFoodItem(
        id = "food-1",
        name = name,
        quantity = 1,
        storageLocation = "REFRIGERATED",
        isOpened = false,
        isPinned = false,
        status = "ACTIVE",
        updatedAtEpochMillis = updatedAt,
    )
}
