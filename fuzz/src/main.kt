package io.heapy.kotmark.fuzz

import com.code_intelligence.jazzer.Jazzer
import io.heapy.kotmark.robustness.MarkdownChecks
import io.heapy.kotmark.robustness.MarkdownInputs
import io.heapy.kotmark.robustness.robustnessExtensions
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val output = Path.of("build", "fuzz")
    val corpus = output.resolve("corpus")
    Files.createDirectories(corpus)
    // Do not overwrite the generated corpus; keep discoveries between campaigns.
    MarkdownInputs.seeds.forEachIndexed { index, source ->
        val seed = corpus.resolve("seed-$index")
        if (!Files.exists(seed)) Files.write(seed, source.toByteArray(Charsets.UTF_8))
    }
    Jazzer.main(arrayOf(
        "--target_class=io.heapy.kotmark.fuzz.MarkdownFuzzTarget",
        "--instrumentation_includes=io.heapy.kotmark.**",
        "--instrumentation_excludes=io.heapy.kotmark.robustness.**:io.heapy.kotmark.fuzz.**",
        "-max_len=4096", "-timeout=10", "-rss_limit_mb=2048",
        "-artifact_prefix=$output/", "--reproducer_path=$output",
        *args.ifEmpty { arrayOf("-max_total_time=60", corpus.toString()) },
    ))
}

object MarkdownFuzzTarget {
    private val core = MarkdownChecks()
    private val extended = MarkdownChecks(robustnessExtensions())

    @JvmStatic
    fun fuzzerTestOneInput(data: ByteArray) {
        val source = data.toString(Charsets.UTF_8)
        core.checkInput(source)
        extended.checkInput(source)
        core.checkLineEndings(source)
        extended.checkLineEndings(source)
    }
}
