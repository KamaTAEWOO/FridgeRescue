package com.portfolio.fridgerescue.server

import com.portfolio.fridgerescue.sync.AccountResponse
import com.portfolio.fridgerescue.sync.FamilyResponse
import com.portfolio.fridgerescue.sync.SyncFoodItem
import com.portfolio.fridgerescue.sync.SyncResponse
import java.security.SecureRandom
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FamilyStore(
    private val clock: Clock = Clock.systemUTC(),
    private val stateFile: Path? = null,
) {
    @Serializable
    private data class Account(
        val id: String,
        val token: String,
        val displayName: String,
        var familyId: String,
    )

    @Serializable
    private data class Family(
        val id: String,
        val name: String,
        val inviteCode: String,
        val memberIds: MutableSet<String> = linkedSetOf(),
        val items: MutableMap<String, SyncFoodItem> = linkedMapOf(),
        var revision: Long = 0,
    )

    @Serializable
    private data class Snapshot(
        val accounts: List<Account> = emptyList(),
        val families: List<Family> = emptyList(),
    )

    private val accountsByToken = ConcurrentHashMap<String, Account>()
    private val families = ConcurrentHashMap<String, Family>()
    private val random = SecureRandom()
    private val json = Json { prettyPrint = true }

    init {
        stateFile?.takeIf(Files::exists)?.let { file ->
            val snapshot = json.decodeFromString<Snapshot>(Files.readString(file))
            snapshot.accounts.forEach { accountsByToken[it.token] = it }
            snapshot.families.forEach { families[it.id] = it }
        }
    }

    @Synchronized
    fun createAccount(displayName: String): AccountResponse {
        val cleanName = displayName.trim().take(30)
        require(cleanName.isNotEmpty())
        val accountId = UUID.randomUUID().toString()
        val family = Family(
            id = UUID.randomUUID().toString(),
            name = "$cleanName 가족",
            inviteCode = uniqueInviteCode(),
        )
        val token = UUID.randomUUID().toString() + UUID.randomUUID().toString()
        val account = Account(accountId, token, cleanName, family.id)
        family.memberIds += accountId
        families[family.id] = family
        accountsByToken[token] = account
        persist()
        return account.toResponse(family)
    }

    fun accountIdForToken(token: String): String? = accountsByToken[token]?.id

    @Synchronized
    fun joinFamily(accountId: String, inviteCode: String): FamilyResponse? {
        val account = accountsByToken.values.firstOrNull { it.id == accountId } ?: return null
        val target = families.values.firstOrNull {
            it.inviteCode.equals(inviteCode.trim(), ignoreCase = true)
        } ?: return null
        families[account.familyId]?.memberIds?.remove(account.id)
        account.familyId = target.id
        target.memberIds += account.id
        persist()
        return target.toResponse()
    }

    @Synchronized
    fun sync(accountId: String, knownRevision: Long, submitted: List<SyncFoodItem>): SyncResponse? {
        val account = accountsByToken.values.firstOrNull { it.id == accountId } ?: return null
        val family = families[account.familyId] ?: return null
        submitted.forEach { incoming ->
            val normalized = incoming.copy(updatedByAccountId = accountId)
            val current = family.items[incoming.id]
            val sameContent = current?.copy(updatedByAccountId = "") ==
                normalized.copy(updatedByAccountId = "")
            val shouldReplace = current == null ||
                normalized.updatedAtEpochMillis > current.updatedAtEpochMillis ||
                (normalized.updatedAtEpochMillis == current.updatedAtEpochMillis &&
                    !sameContent && normalized.updatedByAccountId > current.updatedByAccountId)
            if (shouldReplace && current != normalized) {
                family.items[incoming.id] = normalized
                family.revision++
            }
        }
        persist()
        return SyncResponse(
            familyId = family.id,
            revision = family.revision,
            serverTimeEpochMillis = clock.millis(),
            items = family.items.values.sortedBy { it.id },
        )
    }

    private fun uniqueInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        while (true) {
            val code = buildString { repeat(6) { append(alphabet[random.nextInt(alphabet.length)]) } }
            if (families.values.none { it.inviteCode == code }) return code
        }
    }

    private fun Account.toResponse(family: Family) = AccountResponse(
        accountId = id,
        accessToken = token,
        displayName = displayName,
        familyId = family.id,
        familyName = family.name,
        inviteCode = family.inviteCode,
    )

    private fun Family.toResponse() = FamilyResponse(id, name, inviteCode, memberIds.size)

    private fun persist() {
        val target = stateFile ?: return
        target.parent?.let(Files::createDirectories)
        val temporary = target.resolveSibling(target.fileName.toString() + ".tmp")
        Files.writeString(
            temporary,
            json.encodeToString(Snapshot(accountsByToken.values.toList(), families.values.toList())),
        )
        try {
            Files.move(
                temporary,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: Exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
