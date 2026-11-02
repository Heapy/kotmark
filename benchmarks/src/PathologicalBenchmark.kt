package io.heapy.kotmark.benchmarks

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.parser
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

/** Increasing input sizes expose changes in scaling as well as absolute latency. */
@State(Scope.Benchmark)
open class PathologicalBenchmark {
    @Param("1000", "10000")
    var size: Int = 1000

    private val parser = parser()
    private lateinit var unclosedLinks: String
    private lateinit var unmatchedEmphasis: String

    @Setup
    fun prepare() {
        unclosedLinks = "[](".repeat(size) + "\n"
        unmatchedEmphasis = "*a_ ".repeat(size)
    }

    @Benchmark
    fun unclosedLinks(): Node = parser.parse(unclosedLinks)

    @Benchmark
    fun unmatchedEmphasis(): Node = parser.parse(unmatchedEmphasis)
}
