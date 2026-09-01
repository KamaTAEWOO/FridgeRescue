package com.portfolio.fridgerescue.feature.intake

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

            assertEquals(1, SharedIntakeCacheCleaner.clean(root, now))
            assertFalse(expired.exists())
            assertTrue(recent.exists())
            assertTrue(unrelated.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
