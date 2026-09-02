package com.portfolio.fridgerescue.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** 패키지 이동 이후에도 Clean Architecture 의존 방향이 되돌아가지 않도록 검사한다. */
class CleanArchitectureDependencyTest {
    private val sourceRoot: File by lazy {
        listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory)
            ?: error("메인 소스 디렉터리를 찾을 수 없습니다.")
    }

    @Test
    fun `domain 계층은 Android data presentation 계층에 의존하지 않는다`() {
        assertNoForbiddenImports(
            layer = "domain",
            forbidden = listOf(
                "import android.",
                "import androidx.",
                ".data.",
                ".presentation.",
            ),
        )
    }

    @Test
    fun `data 계층은 presentation 계층에 의존하지 않는다`() {
        assertNoForbiddenImports(layer = "data", forbidden = listOf(".presentation."))
    }

    @Test
    fun `presentation 계층은 data 구현에 직접 의존하지 않는다`() {
        assertNoForbiddenImports(layer = "presentation", forbidden = listOf(".data."))
    }

    private fun assertNoForbiddenImports(layer: String, forbidden: List<String>) {
        val violations = sourceRoot.walkTopDown()
            .filter { file ->
                file.isFile && file.extension == "kt" &&
                    "/$layer/" in file.invariantSeparatorsPath
            }
            .flatMap { file ->
                file.readLines().asSequence()
                    .filter { line -> line.isImport() }
                    .filter { import -> forbidden.any(import::contains) }
                    .map { import -> "${file.relativeTo(sourceRoot).path}: $import" }
            }
            .toList()

        assertTrue(
            "$layer 계층 의존 규칙 위반:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    private fun String.isImport(): Boolean = trimStart().startsWith("import ")
}
