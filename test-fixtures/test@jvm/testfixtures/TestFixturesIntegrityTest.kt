package io.heapy.kotmark.testfixtures

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestFixturesIntegrityTest {
    @Test
    fun embeddedBytesMatchEveryPinnedUpstreamResource() {
        val checksums = resource("SHA256SUMS").decodeToString().lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val (checksum, path) = line.split("  ", limit = 2)
                path to checksum
            }
        assertEquals(
            setOf("commonmark-java/spec.txt", "commonmark-java/gfm-spec.txt",
                "commonmark-java/cmark-regression.txt", "commonmark-java/commonmark.js-regression.txt",
                "commonmark-java/alerts-spec.txt", "commonmark-spec/spec.txt"),
            checksums.keys,
        )
        for ((path, expected) in checksums) {
            val original = resource(path)
            assertEquals(expected, MessageDigest.getInstance("SHA-256").digest(original).toHexString(), path)
            assertContentEquals(original, TestFixtures.readText(path).encodeToByteArray(), path)
        }
    }

    private fun resource(path: String): ByteArray =
        requireNotNull(TestFixturesIntegrityTest::class.java.getResourceAsStream("/upstream/$path")) {
            "Missing original fixture: $path"
        }.use { it.readBytes() }
}
