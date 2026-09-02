package com.portfolio.fridgerescue.feature.family.data

import com.portfolio.fridgerescue.sync.AccountResponse
import com.portfolio.fridgerescue.sync.CreateAccountRequest
import com.portfolio.fridgerescue.sync.FamilyResponse
import com.portfolio.fridgerescue.sync.JoinFamilyRequest
import com.portfolio.fridgerescue.sync.SyncRequest
import com.portfolio.fridgerescue.sync.SyncResponse
import com.portfolio.fridgerescue.feature.family.domain.FamilySyncGateway
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HttpFamilySyncGateway(
    private val json: Json = Json { ignoreUnknownKeys = true },
) : FamilySyncGateway {
    override suspend fun createAccount(baseUrl: String, displayName: String): AccountResponse =
        post(baseUrl, "/v1/accounts", null, json.encodeToString(CreateAccountRequest(displayName)))

    override suspend fun joinFamily(
        baseUrl: String,
        token: String,
        inviteCode: String,
    ): FamilyResponse = post(
        baseUrl,
        "/v1/families/join",
        token,
        json.encodeToString(JoinFamilyRequest(inviteCode)),
    )

    override suspend fun sync(
        baseUrl: String,
        token: String,
        request: SyncRequest,
    ): SyncResponse = post(baseUrl, "/v1/sync", token, json.encodeToString(request))

    private suspend inline fun <reified T> post(
        baseUrl: String,
        path: String,
        token: String?,
        body: String,
    ): T = withContext(Dispatchers.IO) {
        val normalized = baseUrl.trim().trimEnd('/')
        require(normalized.startsWith("https://") || normalized.startsWith("http://")) {
            "Server URL must use HTTP or HTTPS"
        }
        val connection = URL(normalized + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            val status = connection.responseCode
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("Family server HTTP $status: $responseBody")
            json.decodeFromString<T>(responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
