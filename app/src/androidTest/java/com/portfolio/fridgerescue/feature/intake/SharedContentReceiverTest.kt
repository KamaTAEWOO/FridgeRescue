package com.portfolio.fridgerescue.feature.intake

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portfolio.fridgerescue.core.data.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.core.data.repository.RoomIntakeDraftRepository
import com.portfolio.fridgerescue.core.model.IntakeContentType
import com.portfolio.fridgerescue.core.model.IntakeDraftStatus
import com.portfolio.fridgerescue.core.model.IntakeErrorCode
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedContentReceiverTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: FridgeRescueDatabase
    private lateinit var repository: RoomIntakeDraftRepository
    private var nextId = 0

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, FridgeRescueDatabase::class.java).build()
        repository = RoomIntakeDraftRepository(database.intakeDraftDao(), fixedClock())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun TC_INTAKE_001_text_share_is_persisted_as_ready_draft() = runBlocking {
        receiver().receive(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "두부 2개\n시금치 1봉")
            },
        )

        val draft = repository.latestActiveDraft.first()!!
        assertEquals(IntakeDraftStatus.READY, draft.status)
        assertEquals(IntakeContentType.TEXT, draft.contentType)
        assertEquals("두부 2개\n시금치 1봉", draft.textContent)
    }

    @Test
    fun TC_INTAKE_002_image_share_is_copied_to_private_cache() = runBlocking {
        val source = File(context.cacheDir, "shared-source.png").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3))
        }
        receiver().receive(fileIntent("image/png", source))

        val draft = repository.latestActiveDraft.first()!!
        val cached = File(requireNotNull(draft.cachedFilePath))
        assertEquals(IntakeDraftStatus.READY, draft.status)
        assertTrue(cached.exists())
        assertNotEquals(source.absolutePath, cached.absolutePath)
    }

    @Test
    fun TC_INTAKE_003_pdf_share_is_signature_checked_and_cached() = runBlocking {
        val source = File(context.cacheDir, "shared-source.pdf").apply {
            writeBytes("%PDF-1.7 test".encodeToByteArray())
        }
        receiver().receive(fileIntent("application/pdf", source))

        val draft = repository.latestActiveDraft.first()!!
        assertEquals(IntakeContentType.PDF, draft.contentType)
        assertEquals(IntakeDraftStatus.READY, draft.status)
    }

    @Test
    fun TC_INTAKE_004_unsupported_mime_is_saved_as_actionable_error() = runBlocking {
        receiver().receive(Intent(Intent.ACTION_SEND).apply { type = "application/zip" })

        val draft = repository.latestActiveDraft.first()!!
        assertEquals(IntakeDraftStatus.ERROR, draft.status)
        assertEquals(IntakeErrorCode.SHARE_TYPE_UNSUPPORTED, draft.errorCode)
    }

    @Test
    fun TC_INTAKE_005_unavailable_uri_does_not_crash() = runBlocking {
        receiver().receive(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(context.cacheDir, "missing.png")))
            },
        )

        val draft = repository.latestActiveDraft.first()!!
        assertEquals(IntakeDraftStatus.ERROR, draft.status)
        assertEquals(IntakeErrorCode.SHARED_URI_UNAVAILABLE, draft.errorCode)
    }

    @Test
    fun TC_INTAKE_007_multiple_files_are_rejected_without_partial_processing() = runBlocking {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            clipData = ClipData.newRawUri("first", Uri.parse("content://test/first"))
                .apply { addItem(ClipData.Item(Uri.parse("content://test/second"))) }
        }

        receiver().receive(intent)

        val draft = repository.latestActiveDraft.first()!!
        assertEquals(IntakeDraftStatus.ERROR, draft.status)
        assertEquals(IntakeErrorCode.SHARE_MULTIPLE_UNSUPPORTED, draft.errorCode)
    }

    @Test
    fun TC_INTAKE_008_new_share_keeps_previous_draft_record() = runBlocking {
        receiver().receive(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "첫 번째 주문")
            },
        )
        receiver().receive(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "두 번째 주문")
            },
        )

        assertEquals("첫 번째 주문", database.intakeDraftDao().findById("draft-1")?.textContent)
        assertEquals("두 번째 주문", repository.latestActiveDraft.first()?.textContent)
    }

    @Test
    fun TC_INTAKE_009_mime_signature_mismatch_is_rejected() = runBlocking {
        val source = File(context.cacheDir, "fake.png").apply {
            writeBytes("not an image".encodeToByteArray())
        }

        receiver().receive(fileIntent("image/png", source))

        val draft = repository.latestActiveDraft.first()!!
        assertEquals(IntakeDraftStatus.ERROR, draft.status)
        assertEquals(IntakeErrorCode.SHARED_FILE_SIGNATURE_INVALID, draft.errorCode)
    }

    private fun receiver() = SharedContentReceiver(
        context = context,
        repository = repository,
        clock = fixedClock(),
        idFactory = { "draft-${++nextId}" },
    )

    private fun fileIntent(mimeType: String, file: File) = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
    }

    private fun fixedClock() = Clock.fixed(
        Instant.parse("2026-09-01T00:00:00Z"),
        ZoneOffset.UTC,
    )
}
