package com.portfolio.fridgerescue.feature.intake

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import com.portfolio.fridgerescue.core.data.repository.IntakeDraftRepository
import com.portfolio.fridgerescue.core.model.IntakeContentType
import com.portfolio.fridgerescue.core.model.IntakeDraft
import com.portfolio.fridgerescue.core.model.IntakeDraftStatus
import com.portfolio.fridgerescue.core.model.IntakeErrorCode
import java.io.File
import java.io.FileOutputStream
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedContentReceiver(
    private val context: Context,
    private val repository: IntakeDraftRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    suspend fun receive(intent: Intent): String? = withContext(Dispatchers.IO) {
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) {
            return@withContext null
        }
        val draftId = idFactory()
        val now = Instant.now(clock)
        val mimeType = intent.type?.lowercase()
        val contentType = when {
            mimeType == "text/plain" -> IntakeContentType.TEXT
            mimeType?.startsWith("image/") == true -> IntakeContentType.IMAGE
            mimeType == "application/pdf" -> IntakeContentType.PDF
            else -> null
        }

        val draft = when {
            intent.action == Intent.ACTION_SEND_MULTIPLE || (intent.clipData?.itemCount ?: 0) > 1 ->
                errorDraft(
                    draftId,
                    mimeType,
                    contentType,
                    IntakeErrorCode.SHARE_MULTIPLE_UNSUPPORTED,
                    now,
                )
            contentType == null -> errorDraft(
                draftId,
                mimeType,
                null,
                IntakeErrorCode.SHARE_TYPE_UNSUPPORTED,
                now,
            )
            contentType == IntakeContentType.TEXT -> receiveText(intent, draftId, mimeType, now)
            else -> receiveFile(intent, draftId, mimeType, contentType, now)
        }
        repository.save(draft)
        draftId
    }

    private fun receiveText(
        intent: Intent,
        id: String,
        mimeType: String?,
        now: Instant,
    ): IntakeDraft {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val error = when {
            text.isEmpty() -> IntakeErrorCode.SHARED_TEXT_EMPTY
            text.length > MAX_TEXT_LENGTH -> IntakeErrorCode.SHARED_FILE_TOO_LARGE
            else -> null
        }
        return if (error == null) {
            IntakeDraft(
                id = id,
                contentType = IntakeContentType.TEXT,
                mimeType = mimeType,
                textContent = text,
                cachedFilePath = null,
                status = IntakeDraftStatus.READY,
                errorCode = null,
                createdAt = now,
                updatedAt = now,
            )
        } else {
            errorDraft(id, mimeType, IntakeContentType.TEXT, error, now)
        }
    }

    private fun receiveFile(
        intent: Intent,
        id: String,
        mimeType: String?,
        contentType: IntakeContentType,
        now: Instant,
    ): IntakeDraft {
        val uri = intent.streamUri()
            ?: return errorDraft(id, mimeType, contentType, IntakeErrorCode.SHARED_URI_UNAVAILABLE, now)
        val cacheDirectory = File(context.cacheDir, "shared-intake").apply { mkdirs() }
        val cachedFile = File(cacheDirectory, "$id.${contentType.name.lowercase()}")
        val copyError = runCatching { copyBounded(uri, cachedFile) }.exceptionOrNull()
        if (copyError != null) {
            cachedFile.delete()
            val code = if (copyError is ContentTooLargeException) {
                IntakeErrorCode.SHARED_FILE_TOO_LARGE
            } else {
                IntakeErrorCode.SHARED_URI_UNAVAILABLE
            }
            return errorDraft(id, mimeType, contentType, code, now)
        }
        if (!cachedFile.hasExpectedSignature(contentType)) {
            cachedFile.delete()
            return errorDraft(
                id,
                mimeType,
                contentType,
                IntakeErrorCode.SHARED_FILE_SIGNATURE_INVALID,
                now,
            )
        }
        return IntakeDraft(
            id = id,
            contentType = contentType,
            mimeType = mimeType,
            textContent = null,
            cachedFilePath = cachedFile.absolutePath,
            status = IntakeDraftStatus.READY,
            errorCode = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun copyBounded(uri: Uri, target: File) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Shared URI cannot be opened")
        input.use { source ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count == -1) break
                    total += count
                    if (total > MAX_FILE_BYTES) throw ContentTooLargeException()
                    output.write(buffer, 0, count)
                }
            }
        }
    }

    private fun Intent.streamUri(): Uri? {
        if ((clipData?.itemCount ?: 0) > 1) return null
        val extra = parcelableExtra<Uri>(Intent.EXTRA_STREAM)
        return extra ?: clipData?.takeIf { it.itemCount == 1 }?.getItemAt(0)?.uri
    }

    private inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }

    private fun errorDraft(
        id: String,
        mimeType: String?,
        contentType: IntakeContentType?,
        errorCode: String,
        now: Instant,
    ) = IntakeDraft(
        id = id,
        contentType = contentType,
        mimeType = mimeType,
        textContent = null,
        cachedFilePath = null,
        status = IntakeDraftStatus.ERROR,
        errorCode = errorCode,
        createdAt = now,
        updatedAt = now,
    )

    private fun File.hasExpectedSignature(type: IntakeContentType): Boolean {
        val header = inputStream().use { input ->
            val buffer = ByteArray(16)
            val count = input.read(buffer).coerceAtLeast(0)
            buffer.copyOf(count)
        }
        return when (type) {
            IntakeContentType.PDF -> header.startsWith("%PDF-".encodeToByteArray())
            IntakeContentType.IMAGE -> header.isJpeg() || header.isPng() ||
                header.isWebp() || header.isGif() || header.isHeif()
            IntakeContentType.TEXT -> true
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private fun ByteArray.isJpeg() = size >= 3 &&
        this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte() && this[2] == 0xFF.toByte()
    private fun ByteArray.isPng() = startsWith(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
    private fun ByteArray.isWebp() = size >= 12 &&
        copyOfRange(0, 4).decodeToString() == "RIFF" && copyOfRange(8, 12).decodeToString() == "WEBP"
    private fun ByteArray.isGif() = size >= 6 && copyOfRange(0, 6).decodeToString() in setOf("GIF87a", "GIF89a")
    private fun ByteArray.isHeif() = size >= 12 && copyOfRange(4, 8).decodeToString() == "ftyp"

    private class ContentTooLargeException : Exception()

    private companion object {
        const val MAX_TEXT_LENGTH = 1_000_000
        const val MAX_FILE_BYTES = 15L * 1024 * 1024
    }
}
