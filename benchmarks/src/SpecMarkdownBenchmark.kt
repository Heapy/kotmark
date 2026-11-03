package io.heapy.kotmark.benchmarks

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.testfixtures.TestFixtures
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

@State(Scope.Benchmark)
open class SpecMarkdownBenchmark {
    private val renderer = markdownRenderer()
    private lateinit var document: Node

    @Setup
    fun prepare() {
        document = parser().parse(TestFixtures.readText("commonmark-java/spec.txt"))
    }

    @Benchmark
    fun renderMarkdownWholeSpec(): Int = renderer.render(document).length
}
