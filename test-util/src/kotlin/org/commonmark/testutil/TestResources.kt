package org.commonmark.testutil

import java.net.URL
import org.commonmark.testfixtures.TestFixtures

object TestResources {
    @JvmStatic
    fun getSpec(): URL =
        TestFixtures.resource("commonmark-java/spec.txt")

    @JvmStatic
    fun getGfmSpec(): URL =
        TestFixtures.resource("commonmark-java/gfm-spec.txt")

    @JvmStatic
    fun getRegressions(): List<URL> =
        listOf(
            TestFixtures.resource("commonmark-java/cmark-regression.txt"),
            TestFixtures.resource("commonmark-java/commonmark.js-regression.txt"),
        )

    @JvmStatic
    fun readAsString(url: URL): String =
        url.openStream().bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.joinToString(separator = "\n", postfix = "\n")
        }
}
