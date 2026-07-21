package org.commonmark.testutil

import java.io.File
import java.net.URL

object TestResources {
    @JvmStatic
    fun getSpec(): URL =
        locate(
            "spec.txt",
            "projects/commonmark-java/commonmark-test-util/src/main/resources/spec.txt",
        )

    @JvmStatic
    fun getGfmSpec(): URL =
        locate(
            "gfm-spec.txt",
            "projects/commonmark-java/commonmark-test-util/src/main/resources/gfm-spec.txt",
        )

    @JvmStatic
    fun getRegressions(): List<URL> =
        listOf(
            locate(
                "cmark-regression.txt",
                "projects/commonmark-java/commonmark-test-util/src/main/resources/cmark-regression.txt",
            ),
            locate(
                "commonmark.js-regression.txt",
                "projects/commonmark-java/commonmark-test-util/src/main/resources/commonmark.js-regression.txt",
            ),
        )

    @JvmStatic
    fun readAsString(url: URL): String =
        url.openStream().bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.joinToString(separator = "\n", postfix = "\n")
        }

    private fun locate(resourceName: String, workspacePath: String): URL =
        TestResources::class.java.getResource("/$resourceName")
            ?: sequenceOf(File(workspacePath), File("../$workspacePath"))
                .firstOrNull(File::isFile)
                ?.toURI()
                ?.toURL()
            ?: error("Unable to locate $resourceName from ${File(".").absolutePath}")
}
