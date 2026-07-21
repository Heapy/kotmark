package org.commonmark.ext.autolink

import java.io.File
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.test.Test
import kotlin.test.fail

class AutolinkSpecTest {
    @Test
    fun upstreamGfmExtendedAutolinks() {
        val extension = AutolinkExtension.create()
        val parser = Parser.builder().extensions(listOf(extension)).build()
        val renderer = HtmlRenderer.builder().extensions(listOf(extension)).build()
        val fixture =
            sequenceOf(
                File("projects/commonmark-java/commonmark-test-util/src/main/resources/gfm-spec.txt"),
                File("../projects/commonmark-java/commonmark-test-util/src/main/resources/gfm-spec.txt"),
            ).firstOrNull(File::isFile)
                ?: error("Unable to locate gfm-spec.txt from ${File(".").absolutePath}")
        val failures = mutableListOf<String>()
        var count = 0
        for ((source, expected) in readAutolinkExamples(fixture.readText())) {
            count++
            val actual = renderer.render(parser.parse(source))
            if (actual != expected) {
                failures +=
                    "example $count\nsource:\n$source\nexpected:\n$expected\nactual:\n$actual"
            }
        }
        if (failures.isNotEmpty()) {
            fail("${failures.size}/$count GFM autolink examples failed:\n\n${failures.joinToString("\n---\n")}")
        }
    }

    private fun readAutolinkExamples(spec: String): List<Pair<String, String>> {
        val examples = mutableListOf<Pair<String, String>>()
        var inSection = false
        var state = 0
        var source = StringBuilder()
        var html = StringBuilder()
        for (line in spec.lineSequence()) {
            if (state == 0 && line.startsWith("## ")) {
                inSection = line == "## Autolinks (extension)"
            }
            when (state) {
                0 ->
                    if (inSection && line.startsWith(EXAMPLE_START)) {
                        state = 1
                    }
                1 ->
                    if (line == ".") {
                        state = 2
                    } else {
                        source.append(line.replace('\u2192', '\t')).append('\n')
                    }
                2 ->
                    if (line == EXAMPLE_END) {
                        examples += source.toString() to html.toString()
                        source = StringBuilder()
                        html = StringBuilder()
                        state = 0
                    } else {
                        html.append(line).append('\n')
                    }
            }
        }
        return examples
    }

    companion object {
        private const val EXAMPLE_START = "```````````````````````````````` example"
        private const val EXAMPLE_END = "````````````````````````````````"
    }
}
