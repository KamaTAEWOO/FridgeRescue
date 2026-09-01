package com.portfolio.fridgerescue.feature.intake

import java.io.File
import java.time.Duration
import java.time.Instant

object SharedIntakeCacheCleaner {
    private val retention = Duration.ofHours(24)

    fun clean(cacheRoot: File, now: Instant = Instant.now()): Int {
        val intakeDirectory = File(cacheRoot, "shared-intake")
        val cutoff = now.minus(retention).toEpochMilli()
        return intakeDirectory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.lastModified() < cutoff }
            .count(File::delete)
    }
}
