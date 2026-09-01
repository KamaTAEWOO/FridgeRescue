package com.portfolio.fridgerescue.server

import com.portfolio.fridgerescue.sync.ApiError
import com.portfolio.fridgerescue.sync.CreateAccountRequest
import com.portfolio.fridgerescue.sync.JoinFamilyRequest
import com.portfolio.fridgerescue.sync.SyncRequest
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import java.nio.file.Path

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val dataFile = Path.of(
        System.getenv("FRIDGE_RESCUE_DATA_FILE") ?: "data/family-store.json",
    )
    embeddedServer(Netty, host = "0.0.0.0", port = port) {
        module(FamilyStore(stateFile = dataFile))
    }.start(wait = true)
}

fun Application.module(store: FamilyStore = FamilyStore()) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Authentication) {
        bearer("family-token") {
            realm = "FridgeRescue family sync"
            authenticate { credential ->
                store.accountIdForToken(credential.token)?.let(::UserIdPrincipal)
            }
        }
    }
    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }
        route("/v1") {
            post("/accounts") {
                val request = call.receive<CreateAccountRequest>()
                if (request.displayName.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("INVALID_NAME", "Name is required"))
                } else {
                    call.respond(HttpStatusCode.Created, store.createAccount(request.displayName))
                }
            }
            authenticate("family-token") {
                post("/families/join") {
                    val accountId = call.principal<UserIdPrincipal>()!!.name
                    val request = call.receive<JoinFamilyRequest>()
                    val family = store.joinFamily(accountId, request.inviteCode)
                    if (family == null) {
                        call.respond(HttpStatusCode.NotFound, ApiError("INVITE_NOT_FOUND", "Invite code not found"))
                    } else call.respond(family)
                }
                post("/sync") {
                    val accountId = call.principal<UserIdPrincipal>()!!.name
                    val request = call.receive<SyncRequest>()
                    val response = store.sync(accountId, request.knownRevision, request.items)
                    if (response == null) {
                        call.respond(HttpStatusCode.NotFound, ApiError("FAMILY_NOT_FOUND", "Family not found"))
                    } else call.respond(response)
                }
            }
        }
    }
}
