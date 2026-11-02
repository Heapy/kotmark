package org.commonmark.testfixtures

import java.net.URL

/** Pinned resources shipped with the test suite; no filesystem or network fallback. */
object TestFixtures {
    fun resource(path: String): URL =
        requireNotNull(TestFixtures::class.java.getResource("/upstream/$path")) {
            "Missing bundled test fixture: $path"
        }

    fun readText(path: String): String =
        resource(path).openStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
}
