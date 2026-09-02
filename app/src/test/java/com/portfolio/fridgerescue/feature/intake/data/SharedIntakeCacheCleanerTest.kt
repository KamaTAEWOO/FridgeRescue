package com.portfolio.fridgerescue.feature.intake.data

import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedIntakeCacheCleanerTest {
    @Test
    fun TC_PRIVACY_003_only_expired_shared_files_are_deleted() {
        val root = Files.createTempDirectory("fridge-rescue-cache").toFile()
        try {
            val directory = File(root, "shared-intake").apply { mkdirs() }
            val now = Instant.parse("2026-09-02T12:00:00Z")
            val expired = File(directory, "expired.image").apply {
                writeText("old")
                setLastModified(now.minus(Duration.ofHours(25)).toEpochMilli())
            }
            val recent = File(directory, "recent.image").apply {
                writeText("new")
                setLastModified(now.minus(Duration.ofHours(23)).toEpochMilli())
            }
            val unrelated = File(root, "keep.txt").apply { writeText("keep") }
            val oldCapture = File(root, "receipt-capture").apply { mkdirs() }
                .resolve("expired.jpg").apply {
                    writeText("old capture")
                    setLastModified(now.minus(Duration.ofHours(25)).toEpochMilli())
                }

            assertEquals(2, SharedIntakeCacheCleaner.clean(root, now))
            assertFalse(expired.exists())
            assertFalse(oldCapture.exists())
            assertTrue(recent.exists())
            assertTrue(unrelated.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun TC_PRIVACY_004_clear_all_removes_managed_cache_only() {
        val root = Files.createTempDirectory("fridge-rescue-clear-cache").toFile()
        try {
            val shared = File(root, "shared-intake/item.jpg").apply {
                parentFile?.mkdirs()
                writeText("shared")
            }
            val capture = File(root, "receipt-capture/item.jpg").apply {
                parentFile?.mkdirs()
                writeText("capture")
            }
            val unrelated = File(root, "keep.txt").apply { writeText("keep") }

            assertEquals(2, SharedIntakeCacheCleaner.clearAll(root))
            assertFalse(shared.exists())
            assertFalse(capture.exists())
            assertTrue(unrelated.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
