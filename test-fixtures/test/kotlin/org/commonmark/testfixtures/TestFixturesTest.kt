package org.commonmark.testfixtures

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestFixturesTest {
    private val exampleCounts = mapOf(
        "commonmark-java/spec.txt" to 652,
        "commonmark-java/gfm-spec.txt" to 672,
        "commonmark-java/cmark-regression.txt" to 15,
        "commonmark-java/commonmark.js-regression.txt" to 20,
        "commonmark-java/alerts-spec.txt" to 33,
        "commonmark-spec/spec.txt" to 655,
    )

    @Test
    fun bundledBytesMatchPinnedUpstream() {
        val checksums = TestFixtures.readText("SHA256SUMS").lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val (checksum, path) = line.split("  ", limit = 2)
                path to checksum
            }
        assertEquals(exampleCounts.keys, checksums.keys)
        for ((path, expected) in checksums) {
            val bytes = TestFixtures.resource(path).openStream().use { it.readBytes() }
            assertEquals(expected, MessageDigest.getInstance("SHA-256").digest(bytes).toHexString(), path)
        }
    }

    @Test
    fun bundledCorporaRetainEveryExample() {
        for ((path, expected) in exampleCounts) {
            val count = TestFixtures.readText(path).lineSequence()
                .count { it.startsWith("`".repeat(32) + " example") }
            assertEquals(expected, count, path)
        }
    }
}
