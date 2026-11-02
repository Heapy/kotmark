package io.heapy.kotmark.testutil

import io.heapy.kotmark.testfixtures.TestFixtures

object TestResources {
    fun getSpec(): String = "commonmark-java/spec.txt"

    fun getGfmSpec(): String = "commonmark-java/gfm-spec.txt"

    fun getRegressions(): List<String> =
        listOf(
            "commonmark-java/cmark-regression.txt",
            "commonmark-java/commonmark.js-regression.txt",
        )

    fun readAsString(path: String): String =
        TestFixtures.readText(path).replace("\r\n", "\n").replace('\r', '\n')
            .let { if (it.endsWith('\n')) it else "$it\n" }
}
