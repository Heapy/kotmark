package io.heapy.kotmark.benchmarks

import io.heapy.kotmark.autolink.linkExtractor
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

@State(Scope.Benchmark)
open class MarkdownBenchmark {
    @Param("1", "100")
    var repetitions: Int = 1

    private val parser = parser()
    private val renderer = htmlRenderer()
    private val extractor = linkExtractor()
    private lateinit var markdown: String
    private lateinit var document: Node
    private lateinit var links: String

    @Setup
    fun prepare() {
        markdown = ("""
            # Kotmark benchmark

            A paragraph with **strong**, *emphasis*, `code`, and [a link](https://example.com).

            > A block quote with another paragraph.

            - First item
            - Second item

        """.trimIndent() + "\n\n").repeat(repetitions)
        document = parser.parse(markdown)
        links = "Visit https://example.com/docs, www.example.org, or mail user@example.com. ".repeat(repetitions)
    }

    @Benchmark
    fun parse(): Node = parser.parse(markdown)

    @Benchmark
    fun render(): String = renderer.render(document)

    @Benchmark
    fun parseAndRender(): String = renderer.render(parser.parse(markdown))

    @Benchmark
    fun autolinks(): Int = extractor.extractLinks(links).count()
}
