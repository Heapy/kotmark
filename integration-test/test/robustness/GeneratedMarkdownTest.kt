package io.heapy.kotmark.robustness

import io.heapy.kotmark.testfixtures.example.ExampleReader
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratedMarkdownTest {
    @Test
    fun shrinkerKeepsTheFailureAndBoundsItsWork() {
        assertEquals("needle", shrink("prefix needle suffix") { "needle" in it })
        var attempts = 0
        val source = "x".repeat(4096)
        assertEquals(source, shrink(source) { attempts++; false })
        assertTrue(attempts <= 200)
    }

    @Test
    fun generatedDocumentsPreserveHtmlThroughMarkdown() = forInputs(
        seed = 0x4B4F54, iterations = 300, generate = MarkdownInputs::structured,
    ) { source ->
        MarkdownChecks().checkRoundTrip(source)
        MarkdownChecks(robustnessExtensions()).checkRoundTrip(source)
    }

    @Test
    fun arbitraryTextHasConsistentTreesSpansAndRenderers() {
        val core = MarkdownChecks()
        val extended = MarkdownChecks(robustnessExtensions())
        forInputs(seed = 0x46555A, iterations = 400, generate = MarkdownInputs::arbitrary) { source ->
            core.checkInput(source)
            extended.checkInput(source)
        }
    }

    @Test
    fun lineTerminatorsAndNulAreNormalized() {
        val core = MarkdownChecks()
        val extended = MarkdownChecks(robustnessExtensions())
        forInputs(seed = 0x4C494E, iterations = 300, generate = MarkdownInputs::arbitrary) { source ->
            core.checkLineEndings(source)
            extended.checkLineEndings(source)
        }
    }

    @Test
    fun corpusMutationsAndTruncationsRemainWellFormed() {
        val corpus = MarkdownInputs.seeds + ExampleReader.readExamples("commonmark-java/spec.txt").map { it.source }
        val core = MarkdownChecks()
        val extended = MarkdownChecks(robustnessExtensions())
        forInputs(seed = 0x434F52, iterations = 500, generate = { random ->
            MarkdownInputs.mutate(random, corpus.random(random))
        }) { source ->
            core.checkInput(source)
            extended.checkInput(source)
        }
        for (source in MarkdownInputs.seeds) {
            for (end in 0..source.length) {
                withReproducer("seed prefix $end", source.take(end)) {
                    core.checkInput(it)
                    extended.checkInput(it)
                }
            }
        }
    }
}

private fun forInputs(seed: Int, iterations: Int, generate: (Random) -> String, property: (String) -> Unit) {
    val random = Random(seed)
    repeat(iterations) { iteration ->
        withReproducer("seed=$seed iteration=$iteration", generate(random), property)
    }
}

private fun withReproducer(label: String, source: String, property: (String) -> Unit) {
    try {
        property(source)
    } catch (failure: Exception) {
        // Only ordinary contract failures are shrunk; VM errors must escape immediately.
        val minimal = shrink(source) { candidate ->
            try {
                property(candidate)
                false
            } catch (candidateFailure: Exception) {
                candidateFailure::class == failure::class && candidateFailure.message == failure.message
            }
        }
        throw AssertionError("$label\ninput=${escaped(source)}\nshrunk=${escaped(minimal)}", failure)
    }
}

/** Bounded deletion shrinking; diagnostics use UTF-16 escapes, including lone surrogates. */
internal fun shrink(source: String, fails: (String) -> Boolean): String {
    var result = source
    var chunk = maxOf(1, result.length / 2)
    var attempts = 0
    while (chunk > 0 && attempts < 200) {
        var start = 0
        while (start + chunk <= result.length && attempts++ < 200) {
            val candidate = result.removeRange(start, start + chunk)
            if (fails(candidate)) result = candidate else start += chunk
        }
        chunk /= 2
    }
    return result
}

private fun escaped(source: String) = source.toCharArray().joinToString("") { "\\u${it.code.toString(16).padStart(4, '0')}" }
